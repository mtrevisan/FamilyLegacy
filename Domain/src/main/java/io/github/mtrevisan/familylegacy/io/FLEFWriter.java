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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Serializer class to output a {@link FLEFModel} or {@link FLEFRecord} tree into FLEF format.
 * Supports two output modes:
 * <ul>
 *   <li><b>Expanded mode</b> (default) – each nested record is written with its own {@code { }},</li>
 *   <li><b>Compact mode</b> – chains of single‑child, no‑value records are flattened using dot notation.</li>
 * </ul>
 */
public class FLEFWriter{

	private static final String TAG_OPEN_CURLY_BRACE = "{";
	private static final String TAG_CLOSE_CURLY_BRACE = "}";


	private static final String TAG_VALUE_MULTILINE = "\"\"\"";


	private final String indentSequence;
	private final boolean compactMode;

	private ProgressListener progressListener = ProgressListener.NOOP;
	private int lastReportedPercent = -1;


	/**
	 * Helper class to keep track of the traversal state during non-recursive iteration.
	 */
	private static class Frame{
		final FLEFRecord record;
		final int indentLevel;
		int childIndex;

		Frame(final FLEFRecord record, final int indentLevel){
			this.record = record;
			this.indentLevel = indentLevel;
			this.childIndex = 0;
		}
	}


	/**
	 * Creates a writer with default tab indentation and expanded output mode.
	 */
	public static FLEFWriter create(){
		return new FLEFWriter("\t", false);
	}

	/**
	 * Creates a writer with custom indentation and compact output mode.
	 */
	public static FLEFWriter createCompact(){
		return new FLEFWriter("\t", true);
	}

	/**
	 * Creates a writer with a custom indentation sequence and compact mode.
	 *
	 * @param indent string used for each level of indentation
	 * @param compactMode    if {@code true}, compress single‑child chains into dot‑path notation
	 */
	public static FLEFWriter createWithIndent(final String indent, final boolean compactMode){
		return new FLEFWriter(indent, compactMode);
	}


	private FLEFWriter(final String indent, final boolean compactMode){
		this.indentSequence = indent;
		this.compactMode = compactMode;
	}


	/**
	 * Writes the FLEF model to a file.
	 * If {@code compress} is {@code true}, the output is wrapped in a GZIP stream.
	 *
	 * @param model    the model to write
	 * @param path     the destination file path
	 * @param compress whether to compress with GZIP
	 * @throws IOException if writing fails
	 */
	public void write(final FLEFModel model, final Path path, final boolean compress) throws IOException{
		try(final OutputStream os = Files.newOutputStream(path)){
			write(model, os, compress);
		}
	}

	/**
	 * Writes the FLEF model to an output stream.
	 * If {@code compress} is {@code true}, the output is wrapped in a GZIP stream.
	 *
	 * @param model    the model to write
	 * @param out      the output stream
	 * @param compress whether to compress with GZIP
	 * @throws IOException if writing fails
	 */
	public void write(final FLEFModel model, final OutputStream out, final boolean compress) throws IOException{
		final OutputStream os = (compress? new GZIPOutputStream(out): out);
		try(final OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)){
			write(model, writer);
		}
		finally{
			if(compress)
				// GZIPOutputStream must be closed to flush final bytes
				os.close();
		}
	}

	/**
	 * Serializes a {@link FLEFRecord} into a formatted string.
	 *
	 * @param record	record to serialize
	 * @return FLEF formatted string representation
	 */
	public String writeToString(final FLEFRecord record){
		final StringWriter sw = new StringWriter();
		try{
			write(record, sw);
		}
		catch(final IOException e){
			throw new IllegalStateException("Unexpected error writing to StringWriter", e);
		}
		return sw.toString();
	}

	/**
	 * Serializes a {@link FLEFModel} into a formatted string.
	 *
	 * @param model	model to serialize
	 * @return FLEF formatted string representation
	 */
	public String writeToString(final FLEFModel model){
		return writeToString(model, null);
	}

	public String writeToString(final FLEFModel model, final ProgressListener listener){
		final StringWriter sw = new StringWriter();
		try{
			write(model, sw, listener);
		}
		catch(final IOException e){
			throw new IllegalStateException("Unexpected error writing to StringWriter", e);
		}
		return sw.toString();
	}

	/**
	 * Writes a {@link FLEFModel} to an output {@link Writer}.
	 *
	 * @param model	Model to write.
	 * @param writer	Target output stream writer.
	 * @throws	IOException	If writing fails.
	 */
	public void write(final FLEFModel model, final Writer writer) throws IOException{
		write(model, writer, null);
	}

	public void write(final FLEFModel model, final Writer writer, final ProgressListener listener) throws IOException{
		this.progressListener = (listener != null? listener: ProgressListener.NOOP);
		this.lastReportedPercent = -1;

		final List<FLEFRecord> records = model.getRecords();
		final boolean hasHeader = model.getHeader().hasData();
		// Each top-level record is one unit of work; the header counts too.
		final int total = records.size() + (hasHeader? 1: 0);
		int done = 0;

		// Write header record if present
		if(hasHeader){
			progressListener.onProgress(0, I18N.t("writer.writing_header"));

			writeRecord(model.getHeader(), writer, 0);

			done ++;
		}

		// Write the records, each as a top-level block `<record_kind> <Id> { … }`, as defined by the protocol
		for(int i = 0, size = records.size(); i < size; i ++){
			if(i > 0 || hasHeader)
				writer.write(StringUtils.LF);

			writeRecord(records.get(i), writer, 0);

			done ++;
			reportProgress(done, total);
		}

		writer.flush();

		progressListener.onProgress(100, I18N.t("writer.done"));
	}

	/**
	 * Reports the serialization progress when the integer percentage
	 * changes. Called once per top-level record, so it must be cheap.
	 */
	private void reportProgress(final int done, final int total){
		if(total == 0)
			return;

		final int percent = (int)(done * 100. / total);
		if(percent != lastReportedPercent){
			lastReportedPercent = percent;
			progressListener.onProgress(percent, I18N.tf("writer.serializing", done, total));
		}
	}

	/**
	 * Writes a {@link FLEFRecord} to an output {@link Writer}.
	 *
	 * @param record	Record to write.
	 * @param writer	Target output stream writer.
	 * @throws	IOException	If writing fails.
	 */
	public void write(final FLEFRecord record, final Writer writer) throws IOException{
		writeRecord(record, writer, 1);

		writer.flush();
	}

	/**
	 * Iteratively writes a {@link FLEFRecord} structure without recursion.
	 * In compact mode, single‑child chains with no value are written as a single
	 * dotted‑path tag.
	 *
	 * @param rootRecord	Record to serialize.
	 * @param writer	Target output writer.
	 * @param indentLevel	Current tree nesting level.
	 * @throws	IOException	If writing fails.
	 */
	public void writeRecord(final FLEFRecord rootRecord, final Writer writer, final int indentLevel) throws IOException{
		if(rootRecord == null || !rootRecord.hasData())
			return;

		final Deque<Frame> stack = new ArrayDeque<>();

		// For the root, compute effective record and path if compact mode is enabled
		processRecordNode(rootRecord, writer, indentLevel, stack);

		while(!stack.isEmpty()){
			final Frame current = stack.peek();
			final FLEFRecord rec = current.record;
			final List<FLEFRecord> children = rec.getChildren();

			if(rec.hasChildren() && current.childIndex < children.size()){
				final FLEFRecord child = children.get(current.childIndex ++);

				if(child != null && (StringUtils.isNotEmpty(child.getTag()) || child.hasData())){
					final int nextIndent = current.indentLevel;
					processRecordNode(child, writer, nextIndent, stack);
				}
			}
			else{
				// All children processed or leaf node reached
				if(rec.hasChildren() || StringUtils.isNotEmpty(rec.getFormattedId())){
					writeIndent(writer, current.indentLevel - 1);
					writer.write(TAG_CLOSE_CURLY_BRACE);
					writer.write(StringUtils.LF);
				}

				stack.pop();
			}
		}
	}

	/**
	 * Processes a record node by determining its effective structure (based on compact mode),
	 * writing its header, and pushing it onto the traversal stack if it has children.
	 *
	 * @param record      the record to process
	 * @param indentLevel current indentation level
	 * @param writer      the target output writer
	 * @param stack       the stack tracking traversal frames
	 * @throws IOException if writing fails
	 */
	private void processRecordNode(final FLEFRecord record, final Writer writer, final int indentLevel,
			final Deque<Frame> stack) throws IOException{
		if(compactMode){
			// In compact mode, check if the child is compressible: the chain is followed in place, and its tags are written one by one
			final FLEFRecord effective = getEffectiveRecord(record);
			writeRecordHeader(effective, writer, indentLevel, record);
			if(opensBlock(effective))
				stack.push(new Frame(effective, indentLevel + 1));
		}
		else{
			// Expanded mode: write the child as a separate block
			writeRecordHeader(record, writer, indentLevel, record);
			if(opensBlock(record))
				stack.push(new Frame(record, indentLevel + 1));
		}
	}

	/** A record with children, or with an id (even with no children), is written as a block, that must be closed. */
	private static boolean opensBlock(final FLEFRecord record){
		return (record.hasChildren() || StringUtils.isNotEmpty(record.getFormattedId()));
	}

	/**
	 * Writes a record header (tag, optional ID, optional value, and opening brace).
	 * <p>
	 * The file format cannot hold both an id and a scalar value on the same record: the text between the tag and the opening brace
	 * is read back as the id. In the model, records with an id are top-level records, which have no value; the other records have a
	 * value and no id.
	 *
	 * @param record      the record to write
	 * @param writer      the output writer
	 * @param indentLevel the current indentation level
	 * @param chainStart  the record where the chain of single-child records that ends with {@code record} starts (the record itself
	 *                    if it is not in a chain): the tags of the chain are written with the dots between them
	 * @throws IOException if writing fails
	 */
	private void writeRecordHeader(final FLEFRecord record, final Writer writer, final int indentLevel,
			final FLEFRecord chainStart) throws IOException{
		writeIndent(writer, indentLevel);

		// Record tag/type – the tags of the chain, if any, are written as a dotted path
		FLEFRecord link = chainStart;
		while(true){
			final String tag = link.getTag();
			if(tag != null)
				writer.write(tag);
			if(link == record)
				break;

			link = link.getChildren().getFirst();
			writer.write('.');
		}

		// Optional record ID
		final String formattedId = record.getFormattedId();
		if(StringUtils.isNotEmpty(formattedId)){
			writer.write(StringUtils.SPACE);
			writer.write(formattedId);
		}

		// Record value (supports single-line and multiline formatting)
		final String value = record.getValue();
		if(value != null){
			if(isMultiline(value) || needsTextBlock(value, record.hasChildren() && StringUtils.isEmpty(formattedId))){
				writer.write(StringUtils.SPACE);
				writer.write(TAG_VALUE_MULTILINE);
				// Always start the content on a new line
				writer.write(StringUtils.LF);

				// Write content lines with zero indentation (no extra indent)
				writeMultilineValue(value, writer, 0);

				writer.write(TAG_VALUE_MULTILINE);
			}
			else{
				writer.write(StringUtils.SPACE);
				writer.write(value);
			}
		}

		// Child block hierarchy start
		if(record.hasChildren() || StringUtils.isNotEmpty(formattedId)){
			writer.write(StringUtils.SPACE);
			writer.write(TAG_OPEN_CURLY_BRACE);
		}
		writer.write(StringUtils.LF);
	}

	/**
	 * Writes a multiline text value without extra indentation. The lines are read in place and written one by one, without
	 * normalizing the line breaks nor splitting the text first: the value can be long, and each copy of it is memory.
	 *
	 * @param text        the text to write
	 * @param writer      the output writer
	 * @param indentLevel the indentation level for each line (usually 0)
	 * @throws IOException if writing fails
	 */
	private void writeMultilineValue(final String text, final Writer writer, final int indentLevel) throws IOException{
		final int length = text.length();
		if(length == 0)
			return;

		int start = 0;
		while(true){
			//a line ends at \n, \r\n or \r
			int end = start;
			while(end < length && text.charAt(end) != '\n' && text.charAt(end) != '\r')
				end ++;

			final boolean last = (end == length);
			writeIndent(writer, indentLevel);
			writeEscapedTextBlockLine(text, start, end, last, writer);
			if(last)
				return;

			writer.write(StringUtils.LF);

			start = end + 1;
			if(text.charAt(end) == '\r' && start < length && text.charAt(start) == '\n')
				start ++;
		}
	}

	/**
	 * Writes a line of a text block, escaping the bare minimum: the backslashes (the parser translates the escape sequences), the
	 * quotes that would form triple quotes (or that would touch the closing ones), and, on the last line only, the leading whitespace
	 * (the closing triple quotes follow the last line, whose indentation the parser would otherwise take as incidental indentation of
	 * the whole text). Everything else, spaces and quotes included, is written as is.
	 * <p>
	 * The line is the range of a text, and it is written straight to the writer, in runs of the characters that need no escape.
	 *
	 * @param text	The text.
	 * @param start	The start of the line (inclusive).
	 * @param end	The end of the line (exclusive).
	 * @param last	Whether the line is the last one (followed by the closing triple quotes).
	 * @param writer	The output writer.
	 */
	private static void writeEscapedTextBlockLine(final String text, final int start, final int end, final boolean last,
			final Writer writer) throws IOException{
		final int length = end - start;
		//as in a Java text block, the trailing whitespace of a line is stripped (so the last one is escaped), and a line made of
		//whitespace only is blank, so it neither counts for the incidental indentation nor survives (so all of it is escaped)
		int lastNonWhitespace = -1;
		for(int i = length - 1; i >= 0; i --)
			if(!Character.isWhitespace(text.charAt(start + i))){
				lastNonWhitespace = i;

				break;
			}
		final boolean blank = (lastNonWhitespace < 0);
		int runLength = 0;
		boolean runAtEnd = false;
		//the start of the run of characters written as they are, or -1
		int plainStart = -1;
		for(int i = 0; i < length; i ++){
			final char chr = text.charAt(start + i);
			final String escaped;
			if((chr == ' ' || chr == '\t') && (blank || i == length - 1 && lastNonWhitespace < i || i == 0 && last))
				escaped = (chr == ' '? "\\s": "\\t");
			else if(chr == '"'){
				int quoteStart = i;
				while(quoteStart > 0 && text.charAt(start + quoteStart - 1) == '"')
					quoteStart --;
				if(quoteStart == i){
					runLength = 1;
					while(i + runLength < length && text.charAt(start + i + runLength) == '"')
						runLength ++;
					runAtEnd = (last && i + runLength == length);
				}
				//as in a Java text block, the first quote of every group of three is escaped; the quotes that touch the closing
				//triple quotes are all escaped
				escaped = (runAtEnd || runLength >= 3 && (i - quoteStart) % 3 == 0? "\\\"": null);
			}
			else if(chr == '\\')
				escaped = "\\\\";
			else
				escaped = null;

			if(escaped == null){
				if(plainStart < 0)
					plainStart = start + i;
			}
			else{
				if(plainStart >= 0){
					writer.write(text, plainStart, start + i - plainStart);
					plainStart = -1;
				}
				writer.write(escaped);
			}
		}
		if(plainStart >= 0)
			writer.write(text, plainStart, end - plainStart);
	}

	/**
	 * Determines if a string contains line breaks and should be written as a
	 * multiline text block.
	 *
	 * @param str the string to check
	 * @return {@code true} if the string contains a newline or carriage return
	 */
	private boolean isMultiline(final String str){
		return (str.contains(StringUtils.LF) || str.contains(StringUtils.CR));
	}

	/**
	 * Determines if a single-line value cannot be written as is, because the parser would read it differently: an empty value
	 * (read as no value), a value with leading or trailing whitespace, a value starting with a brace or with triple quotes, a value
	 * ending in a brace (read as an id followed by a block), or a value followed by a block. Such values are written as a text
	 * block, where the escape sequences make them unambiguous.
	 *
	 * @param value	the value, without line breaks
	 * @param block	whether the record is written with a block
	 * @return	{@code true} if the value is to be written as a text block
	 */
	private static boolean needsTextBlock(final String value, final boolean block){
		if(block || value.isEmpty())
			return true;

		final char first = value.charAt(0);
		final char last = value.charAt(value.length() - 1);
		return (first == ' ' || first == '\t' || last == ' ' || last == '\t'
			|| first == '{' || last == '{' || value.startsWith(TAG_VALUE_MULTILINE));
	}

	/**
	 * Writes the indentation sequence a given number of times.
	 *
	 * @param writer the output writer
	 * @param level  the number of indentation levels
	 * @throws IOException if writing fails
	 */
	private void writeIndent(final Writer writer, final int level) throws IOException{
		for(int i = 0; i < level; i ++)
			writer.write(indentSequence);
	}

	/**
	 * Traverses a chain of records where each record has exactly one child and no value, and returns the deepest record.
	 * <p>
	 * If the record is not compressible (has multiple children, a value or an id), the record itself is returned.
	 *
	 * @param record the starting record
	 * @return the deepest record of the chain
	 */
	private FLEFRecord getEffectiveRecord(final FLEFRecord record){
		if(!compactMode)
			return record;

		// If the record has an ID, it cannot be compressed (the ID must be preserved)
		if(StringUtils.isNotEmpty(record.getFormattedId()))
			return record;

		FLEFRecord current = record;
		// A record is compressible if it has exactly one child, no value, and no ID
		while(current.getChildren().size() == 1 && current.getValue() == null
				&& StringUtils.isEmpty(current.getFormattedId()))
			current = current.getChildren().getFirst();

		return current;
	}

}
