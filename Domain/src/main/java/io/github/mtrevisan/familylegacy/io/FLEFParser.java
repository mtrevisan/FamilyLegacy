/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarParser;
import io.github.mtrevisan.familylegacy.io.grammar.FLEFGrammarValidator;
import io.github.mtrevisan.familylegacy.io.grammar.SingularFieldIndex;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.Strings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class FLEFParser{

	private static final char TAG_OPEN_CURLY_BRACE = '{';
	private static final char TAG_CLOSE_CURLY_BRACE = '}';
	private static final char BYTE_ORDER_MARK = '﻿';

	private static final String TAG_VALUE_MULTILINE = "\"\"\"";


	/**
	 * Helper to hold both the root and the deepest node of a nested chain.
	 */
	private record NestedPair(FLEFRecord root, FLEFRecord current){}


	private String text;
	private int length;
	private int position;

	private ProgressListener progressListener = ProgressListener.NOOP;
	private int lastReportedPercent = -1;
	private final List<String> warnings = new ArrayList<>();
	/** The tags are the same few words repeated for every node: a single instance of each is kept. */
	private final StringPool tags = new StringPool(2048);
	/** The short values repeat too (the types, the calendars, ...). */
	private final StringPool values = new StringPool(4096);
	private static final int MAX_POOLED_VALUE_LENGTH = 32;


	/**
	 * Parses a FLEF file from the given path.
	 * If the file begins with the GZIP magic bytes, it is automatically decompressed.
	 *
	 * @param path the path to the FLEF file (may be plain text or gzip‑compressed)
	 * @return the parsed FLEF model
	 * @throws IOException if reading fails
	 */
	public FLEFModel parse(final Path path) throws IOException{
		return parse(path, null);
	}

	public FLEFModel parse(final Path path, final ProgressListener listener) throws IOException{
		final ProgressListener l = (listener != null? listener: ProgressListener.NOOP);
		l.onProgress(0, I18N.t("parser.reading.file"));

		return parse(readContent(path, l), l);
	}

	/**
	 * Reads a file as text. The buffers are kept as few and as short-lived as possible: the bytes are not kept while the text is parsed,
	 * and a compressed file is read in a buffer of exactly its uncompressed size (written at its end), instead of the chunks that
	 * {@link InputStream#readAllBytes()} joins in a second array.
	 */
	private static String readContent(final Path path, final ProgressListener listener) throws IOException{
		byte[] data = Files.readAllBytes(path);
		if(!isGzipCompressed(data))
			return new String(data, StandardCharsets.UTF_8);

		listener.onProgress(-1, I18N.t("parser.decompressing"));

		//the uncompressed size modulo 2^32 is in the last four bytes (little endian)
		final int trailer = (data.length >= 18
			? (data[data.length - 4] & 0xFF) | (data[data.length - 3] & 0xFF) << 8 | (data[data.length - 2] & 0xFF) << 16
				| (data[data.length - 1] & 0xFF) << 24
			: -1);
		//a size that cannot be (deflate does not compress more than about 1000 times) is not trusted
		final int declaredSize = (trailer > 0 && trailer <= data.length * 1100L? trailer: -1);
		try(final ByteArrayInputStream bais = new ByteArrayInputStream(data);
			 final GZIPInputStream gis = new GZIPInputStream(bais)){
			if(declaredSize > 0){
				final byte[] expanded = new byte[declaredSize];
				final int read = gis.readNBytes(expanded, 0, declaredSize);
				//a size that does not match (e.g. a file with more than one member) is not trusted
				final int next = (read == declaredSize? gis.read(): -1);
				if(read == declaredSize && next < 0)
					return new String(expanded, StandardCharsets.UTF_8);

				final byte[] rest = gis.readAllBytes();
				final int nextLength = (next >= 0? 1: 0);
				final byte[] all = new byte[read + nextLength + rest.length];
				System.arraycopy(expanded, 0, all, 0, read);
				if(nextLength == 1)
					all[read] = (byte)next;
				System.arraycopy(rest, 0, all, read + nextLength, rest.length);
				return new String(all, StandardCharsets.UTF_8);
			}

			return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Parses a FLEF file from an input stream.
	 * If the stream starts with the GZIP magic bytes, it is automatically decompressed.
	 *
	 * @param inputStream the input stream (may be plain or gzip‑compressed)
	 * @return the parsed FLEF model
	 * @throws IOException if reading fails
	 */
	public FLEFModel parse(final InputStream inputStream) throws IOException{
		return parse(inputStream, null);
	}

	public FLEFModel parse(final InputStream inputStream, final ProgressListener listener) throws IOException{
		final ProgressListener l = (listener != null? listener: ProgressListener.NOOP);
		l.onProgress(0, I18N.t("parser.reading.stream"));

		final String content;

		// Read the first few bytes to test for GZIP magic
		final byte[] header = new byte[2];
		final PushbackInputStream pushback = new PushbackInputStream(inputStream, 2);
		//(readNBytes does not stop at a short read; an empty stream gives 0, not -1)
		final int read = pushback.readNBytes(header, 0, 2);
		if(read == 2 && isGzipMagic(header)){
			pushback.unread(header, 0, 2);

			l.onProgress(-1, I18N.t("parser.decompressing"));

			try(final GZIPInputStream gis = new GZIPInputStream(pushback)){
				content = new String(gis.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		else{
			pushback.unread(header, 0, read);
			content = new String(pushback.readAllBytes(), StandardCharsets.UTF_8);
		}

		return parse(content, l);
	}

	/**
	 * Checks if the given byte array starts with the GZIP magic header (1F 8B).
	 */
	private static boolean isGzipCompressed(final byte[] data){
		return (data.length >= 2 && isGzipMagic(data));
	}

	private static boolean isGzipMagic(final byte[] header){
		return (header.length >= 2 && header[0] == (byte)0x1F && header[1] == (byte)0x8B);
	}


	public FLEFModel parse(final String text){
		return parse(text, null);
	}

	public FLEFModel parse(final String text, final ProgressListener listener){
		this.progressListener = (listener != null? listener: ProgressListener.NOOP);
		this.lastReportedPercent = -1;

		this.text = text;
		length = text.length();
		position = 0;

		warnings.clear();
		final FLEFModel root = new FLEFModel();

		// A byte order mark at the start of the file may be present, and is ignored
		if(!eof() && peek() == BYTE_ORDER_MARK)
			advance();
		skipIgnored();

		while(!eof()){
			final int recordPosition = position;
			final FLEFRecord record = parseRecord();

			final String tag = record.getTag();
			if(Strings.CI.equals(FLEFGrammarParser.FIELD_HEADER, tag)){
				SingularFieldIndex.mergeSingularBlocks(record);
				root.setHeader(record);
			}
			else if(Strings.CI.equals(FLEFGrammarParser.FIELD_RECORDS, tag) && record.getId() == null){
				// legacy layout, with every record inside a `records { … }` block
				final List<FLEFRecord> children = record.getChildren();
				for(final FLEFRecord child : children){
					if(child.getId() == null){
						warnings.add("Record '" + child.getTag() + "' without an id inside `records { }` at position " + recordPosition
							+ " skipped");
						continue;
					}

					SingularFieldIndex.mergeSingularBlocks(child);
					addParsedRecord(root, child);
				}
			}
			else if(record.getId() != null){
				// `<record_kind> <Id> { … }`, as defined by the protocol
				SingularFieldIndex.mergeSingularBlocks(record);
				addParsedRecord(root, record);
			}
			else
				throw new RuntimeException("Unexpected tag: " + tag + " at position " + recordPosition);

			skipIgnored();
		}

		//the files written before the sex became an individual attribute
		SexMigration.migrate(root);
		//the files written with the enum values that the grammar renamed or removed
		EnumValuesMigration.migrate(root);

		return root;
	}

	/**
	 * The problems found by the last parse that do not stop it (a record without an id skipped, an id found twice), in order.
	 */
	public List<String> getWarnings(){
		return List.copyOf(warnings);
	}

	private void addParsedRecord(final FLEFModel model, final FLEFRecord record){
		final String id = record.getId();
		if(model.hasRecord(id)){
			model.markDuplicateId(id);
			warnings.add("Duplicate record ID '" + id + "': the last one replaces the previous");
		}
		model.addRecord(record);
	}

	private FLEFRecord parseRecord(){
		final int tagStart = position;
		skipIdentifier();
		final int tagEnd = position;

		FLEFRecord record;
		FLEFRecord currentRecord;
		final int firstDot = text.indexOf('.', tagStart);
		if(firstDot >= 0 && firstDot < tagEnd){
			final NestedPair pair = createNestedRecord(tagStart, tagEnd);
			record = pair.root;
			currentRecord = pair.current;
		}
		else{
			record = FLEFRecord.createChildWithTag(tags.lowercase(text, tagStart, tagEnd));
			currentRecord = record;
		}

		skipSpaces();

		// Block
		if(!eof()){
			final char chr = peek();
			// Child block with no scalar value
			if(chr == TAG_OPEN_CURLY_BRACE){
				consume(TAG_OPEN_CURLY_BRACE);
				parseBlock(currentRecord);

				return record;
			}

			// Multi-line string as value
			if(startsWithTripleQuotes()){
				currentRecord.setValue(readMultilineString());
				skipSpaces();
				if(!eof() && peek() == TAG_OPEN_CURLY_BRACE){
					consume(TAG_OPEN_CURLY_BRACE);
					parseBlock(currentRecord);
				}

				return record;
			}

			// Inline value until EOL or block '{'
			String value = readToEndOfLine();
			// If there is an inline id followed by a block
			if(!value.isEmpty() && value.charAt(value.length() - 1) == TAG_OPEN_CURLY_BRACE){
				value = value.substring(0, value.length() - 1).trim();
				if(!value.isEmpty())
					currentRecord.setId(value);
				parseBlock(currentRecord);

				return record;
			}

			// Simple value
			if(!value.isEmpty())
				currentRecord.setValue(value);

			skipSpaces();

			if(!eof() && peek() == TAG_OPEN_CURLY_BRACE){
				consume(TAG_OPEN_CURLY_BRACE);
				parseBlock(currentRecord);
			}
		}

		return record;
	}

	private void parseBlock(final FLEFRecord parent){
		skipIgnored();

		while(!eof()){
			if(peek() == TAG_CLOSE_CURLY_BRACE){
				consume(TAG_CLOSE_CURLY_BRACE);

				return;
			}

			parent.addChild(parseRecord());

			skipIgnored();

			reportProgress();
		}

		throw error("Missing closing brace");
	}

	/**
	 * Reports the parse progress to the listener when the integer
	 * percentage has changed. Called from the hot loops
	 * ({@link #parse(String, ProgressListener)} and {@link #parseBlock}),
	 * so it must be cheap: it only does an integer division and a
	 * comparison, and it fires the listener at most 100 times per parse.
	 */
	private void reportProgress(){
		if(length == 0)
			return;

		final int percent = (int)(position * 100L / length);
		if(percent != lastReportedPercent){
			lastReportedPercent = percent;

			progressListener.onProgress(percent, I18N.t("parser.parsing"));
		}
	}

	/**
	 * Creates a chain of nested records for a dotted tag path.
	 * Example: "valid_from.value.point.full_date" produces:
	 * valid_from { value { point { full_date {} } } }
	 * Returns the deepest record (the last segment).
	 */
	private NestedPair createNestedRecord(final int tagStart, final int tagEnd){
		//the segments are read in place, without splitting the path (the empty ones are ignored)
		FLEFRecord root = null;
		FLEFRecord current = null;
		int start = tagStart;
		while(start < tagEnd){
			int end = text.indexOf('.', start);
			if(end < 0 || end > tagEnd)
				end = tagEnd;
			if(end > start){
				final FLEFRecord child = FLEFRecord.createChildWithTag(tags.lowercase(text, start, end));
				if(root == null)
					root = child;
				else
					current.addChild(child);

				current = child;
			}
			start = end + 1;
		}
		if(root == null)
			throw error("Expected identifier");

		return new NestedPair(root, current);
	}


	/**
	 * Moves past an identifier.
	 */
	private void skipIdentifier(){
		final int start = position;
		while(!eof()){
			final char c = peek();
			if(Character.isWhitespace(c) || c == TAG_OPEN_CURLY_BRACE || c == TAG_CLOSE_CURLY_BRACE)
				break;

			advance();
		}
		if(start == position)
			throw error("Expected identifier");
	}

	/**
	 * Reads everything until end-of-line.
	 */
	private String readToEndOfLine(){
		final int start = position;
		while(!eof()){
			final char c = peek();
			if(c == '\n' || c == '\r')
				break;

			advance();
		}
		final String res = (position - start <= MAX_POOLED_VALUE_LENGTH
			? values.exact(text, start, position)
			: text.substring(start, position));

		skipLineBreak();

		return res;
	}

	/**
	 * Finds the triple quotes that close the text block: the first ones that are not escaped, as in a Java text block (an escaped quote
	 * that comes right before the closing ones is part of the text).
	 *
	 * @return	The index of the closing triple quotes, or {@code -1} if there are none.
	 */
	private int indexOfClosingTripleQuotes(){
		for(int i = position, length = text.length(); i < length; i ++){
			final char chr = text.charAt(i);
			if(chr == '\\')
				//the escaped character is never a delimiter
				i ++;
			else if(text.startsWith(TAG_VALUE_MULTILINE, i))
				return i;
		}
		return -1;
	}

	/**
	 * Reads a string delimited by triple quotes.
	 */
	private String readMultilineString(){
		expectTripleQuotes();
		advanceBy(3);

		// Find the closing triple quotes
		final int end = indexOfClosingTripleQuotes();
		if(end < 0)
			throw error("Unterminated multiline string");

		int startOfCloseLine = end;
		// Find the start of the line that contains the closing quotes
		while(startOfCloseLine > 0 && text.charAt(startOfCloseLine - 1) != '\n')
			startOfCloseLine --;
		// Compute the leading whitespace (indent) of the line containing the closing delimiter (from its start to 'end')
		int closeIndent = 0;
		while(startOfCloseLine + closeIndent < end && (text.charAt(startOfCloseLine + closeIndent) == ' '
				|| text.charAt(startOfCloseLine + closeIndent) == '\t'))
			closeIndent ++;

		// The raw content is between the delimiters
		final String block = normalizeTextBlock(position, end, closeIndent);

		// Move position past the closing delimiter and normalize the trailing newline
		position = end + 3;

		skipLineBreak();

		return block;
	}

	/**
	 * Normalizes a raw text block content (enclosed in """) according to Java text block rules, working on a single array of
	 * characters (the text block can be long, and every copy of it is memory).
	 * <p>
	 * This method performs the following transformations:
	 * <ol>
	 *   <li>Normalizes line endings: all {@code \r\n} and {@code \r} are converted to {@code \n}.</li>
	 *   <li>Strips the leading newline if the content starts with one (the opening delimiter
	 *       is typically followed by a newline).</li>
	 *   <li>Determines the minimum indentation (incidental indentation) across all non-blank lines,
	 *       including the indentation of the closing delimiter line.</li>
	 *   <li>Removes this minimum indentation from every line, and the trailing whitespace.</li>
	 *   <li>Translates standard escape sequences: {@code \\}, {@code \"}, {@code \n}, {@code \r}, {@code \t}, {@code \s}.</li>
	 * </ol>
	 *
	 * @param from        the start of the raw string in the text (just after the opening triple quotes)
	 * @param to          the end of the raw string in the text (the closing triple quotes)
	 * @param closeIndent the number of leading whitespace characters on the closing delimiter line
	 * @return the normalized content, ready to be stored as the record value
	 */
	private String normalizeTextBlock(final int from, final int to, final int closeIndent){
		final char[] buffer = new char[to - from];
		text.getChars(from, to, buffer, 0);

		// Step 1: Normalize line endings to LF only
		int size = 0;
		for(int i = 0, length = buffer.length; i < length; i ++){
			final char c = buffer[i];
			if(c == '\r'){
				buffer[size ++] = '\n';
				if(i + 1 < length && buffer[i + 1] == '\n')
					i ++;
			}
			else
				buffer[size ++] = c;
		}

		// Step 2: Remove the leading newline if present (the opening """ is usually followed by \n)
		final int begin = (size > 0 && buffer[0] == '\n'? 1: 0);

		// Step 3: Calculate the minimum indentation among all non-blank content lines
		// and also consider the indentation of the closing delimiter line.
		int minIndent = closeIndent;
		for(int lineStart = begin; lineStart <= size; ){
			int lineEnd = lineStart;
			while(lineEnd < size && buffer[lineEnd] != '\n')
				lineEnd ++;

			// Ignore lines that consist solely of whitespace when computing the minimum indent
			if(!isBlank(buffer, lineStart, lineEnd)){
				int leading = 0;
				while(lineStart + leading < lineEnd && (buffer[lineStart + leading] == ' ' || buffer[lineStart + leading] == '\t'))
					leading ++;
				if(leading < minIndent)
					minIndent = leading;
			}
			lineStart = lineEnd + 1;
		}

		// Step 4: Strip the minimum indentation and the trailing whitespace from each line (in place: the text only shrinks)
		int written = 0;
		for(int lineStart = begin; lineStart <= size; ){
			int lineEnd = lineStart;
			while(lineEnd < size && buffer[lineEnd] != '\n')
				lineEnd ++;

			if(lineStart > begin)
				buffer[written ++] = '\n';
			// Remove the minIndent only if the line is long enough
			final int contentStart = (lineEnd > lineStart && lineEnd - lineStart >= minIndent? lineStart + minIndent: lineStart);
			// As in a Java text block, the trailing whitespace of each line is removed (before the escapes are translated)
			int contentEnd = lineEnd;
			while(contentEnd > contentStart && Character.isWhitespace(buffer[contentEnd - 1]))
				contentEnd --;
			for(int i = contentStart; i < contentEnd; i ++)
				buffer[written ++] = buffer[i];
			lineStart = lineEnd + 1;
		}

		// Step 5: Translate escape sequences (handle \", \\, \n, \r, \t, \s)
		return new String(buffer, 0, translateEscapes(buffer, written));
	}

	/** @return whether the characters have nothing but whitespace (as the lines that {@link String#trim()} makes empty) */
	private static boolean isBlank(final char[] buffer, final int start, final int end){
		for(int i = start; i < end; i ++)
			if(buffer[i] > ' ')
				return false;
		return true;
	}

	/**
	 * Translates standard escape sequences found in a text block, in place (a translation is never longer than the sequence).
	 * <p>
	 * Supports: {@code \\} -> {@code \}, {@code \"} -> {@code "}, {@code \n} -> newline,
	 * {@code \r} -> carriage return, {@code \t} -> tab, {@code \s} -> space.
	 * Unrecognized escapes are left unchanged (e.g., {@code \x} remains {@code \x}).
	 *
	 * @param buffer the characters potentially containing escape sequences
	 * @param length the number of characters
	 * @return the number of characters once the escapes are translated
	 */
	private static int translateEscapes(final char[] buffer, final int length){
		int written = 0;
		for(int i = 0; i < length; i ++){
			final char c = buffer[i];
			if(c == '\\' && i + 1 < length){
				final char next = buffer[i + 1];
				switch(next){
					case 'n' -> {
						buffer[written ++] = '\n';
						i ++;
					}
					case 'r' -> {
						buffer[written ++] = '\r';
						i ++;
					}
					case 't' -> {
						buffer[written ++] = '\t';
						i ++;
					}
					case 's' -> {
						buffer[written ++] = ' ';
						i ++;
					}
					case 'b' -> {
						buffer[written ++] = '\b';
						i ++;
					}
					case 'f' -> {
						buffer[written ++] = '\f';
						i ++;
					}
					case '\'' -> {
						buffer[written ++] = '\'';
						i ++;
					}
					//line continuation: the escaped line break is removed
					case '\n' ->
						i ++;
					//octal escapes, \0 to \377
					case '0', '1', '2', '3', '4', '5', '6', '7' -> {
						int value = next - '0';
						int digits = 1;
						final int maxDigits = (next <= '3'? 3: 2);
						while(digits < maxDigits && i + 1 + digits < length && buffer[i + 1 + digits] >= '0'
								&& buffer[i + 1 + digits] <= '7'){
							value = value * 8 + (buffer[i + 1 + digits] - '0');
							digits ++;
						}
						buffer[written ++] = (char)value;
						i += digits;
					}
					case '"' -> {
						buffer[written ++] = '"';
						i ++;
					}
					case '\\' -> {
						buffer[written ++] = '\\';
						i ++;
					}
					default ->
						// Unknown escape: leave both characters as-is
						buffer[written ++] = c;
				}
			}
			else
				buffer[written ++] = c;
		}
		return written;
	}


	private void skipIgnored(){
		while(!eof()){
			final char c = peek();
			if(Character.isWhitespace(c))
				advance();
			else if(c == '#' || (c == '/' && position + 1 < length && text.charAt(position + 1) == '/')){
				// Skip single-line comments (# or //)
				while(!eof() && peek() != '\n' && peek() != '\r')
					advance();
			}
			else
				break;
		}
	}

	/**
	 * Skip only spaces and tabs.
	 * Newlines are preserved because they terminate values.
	 */
	private void skipSpaces(){
		while(!eof()){
			final char c = peek();
			if(c != ' ' && c != '\t')
				break;

			advance();
		}
	}

	private void skipLineBreak(){
		if(!eof() && peek() == '\r')
			advance();
		if(!eof() && peek() == '\n')
			advance();
	}

	private boolean startsWithTripleQuotes(){
		return text.startsWith(TAG_VALUE_MULTILINE, position);
	}

	private void expectTripleQuotes(){
		if(!startsWithTripleQuotes())
			throw error("Expected triple quotes");
	}

	private boolean eof(){
		return (position >= length);
	}

	private char peek(){
		return text.charAt(position);
	}

	private void consume(char expected){
		if(eof() || peek() != expected)
			throw error("Expected '" + expected + "'");

		advance();
	}

	private void advance(){
		position ++;
	}

	private void advanceBy(final int count){
		position += count;
	}

	private IllegalArgumentException error(final String message){
		return new IllegalArgumentException(message + " at position " + position);
	}


	static void main(final String[] args) throws IOException{
		//with errors
		String text2 = """
			header {
			  date 2026-07-31
			  submitter.name Mario Rossi
			}
			records {
			  individual I1 {
			    side INVALID_VALUE
			    audit.creation.date 2026-07-31
			  }
			  event_participation EP1 {
			    event.event E999
			    participant.individual I1
			    role CHILD
			    audit.creation.date 2026-07-31
			  }
			}
		""";

		//without errors
		String text = """
			header {
			  protocol {
			    name Family LEgacy Format
			    version 0.2.0
			  }
			  source {
			    name My Genealogy Software
			    version 1.0.0
			  }
			  date 2026-07-31
			  submitter {
			    name Mario Rossi
			    contact {
			      value mario.rossi@example.com
			      type personal
			      audit.creation.date 2026-07-31
			    }
			  }
			  scope Example family
			}
			records {
			  individual I1 {
			    name {
			      part {
			        type given
			        value Mario
			      }
			      part {
			        type family
			        value ""\"
Rossi
Bianchi
""\"
			      }
			    }
			    side MALE
             note {
               text Individuo presente nel registro di nascita.
               audit.creation.date 2026-07-31
             }
			    audit.creation.date 2026-07-31
			  }
			  event E1 {
			    type BIRTH
			    detail {
			      date.value.point.single_date.full_date {
			        value 1894-03-17
			        calendar gregorian
			      }
			      audit.creation.date 2026-07-31
			    }
			    audit.creation.date 2026-07-31
			  }
			  event_participation EP1 {
			    event E1
			    participant.individual I1
			    role CHILD
			    audit.creation.date 2026-07-31
			  }
			}
			""";

		final Path path = Paths.get("src/main/resources/gedg/flef_0.2.0.gedg");
		final FLEFGrammar grammar = FLEFGrammarParser.parse(path);
		for(final String warning : grammar.getParseWarnings())
			System.out.println(warning);


		System.out.println();


		final FLEFGrammarValidator.ValidationResult validationResult = FLEFGrammarValidator.validate(grammar);
		System.out.println("Grammar check (errors):");
		for(final String error : validationResult.errors())
			System.out.println(error);
		System.out.println("Grammar check (warnings):");
		for(final String warning : validationResult.warnings())
			System.out.println(warning);


		System.out.println();


		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(text);

		System.out.println(model);


		System.out.println();


		final FLEFWriter writer = FLEFWriter.createCompact();
		System.out.println(writer.writeToString(model));


		System.out.println();


		final FLEFValidator validator = new FLEFValidator(grammar);
		final List<String> errorsSchema = validator.validateSchema(model);
		System.out.println("Schema check:");
		for(final String error : errorsSchema)
			System.out.println(error);


		System.out.println();


		final List<String> errorsIntegrity = validator.validateIntegrity(model);
		System.out.println("Integrity check:");
		for(final String error : errorsIntegrity)
			System.out.println(error);
	}

}
