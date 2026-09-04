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
package io.github.mtrevisan.familylegacy.v2.io;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
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
import java.util.Objects;
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

	private static final String FIELD_RECORDS = "records";

	private static final String TAG_VALUE_MULTILINE = "\"\"\"";


	private final String indentSequence;
	private final boolean compactMode;


	/**
	 * Helper class to keep track of the traversal state during non-recursive iteration.
	 */
	private static class Frame{
		final FLEFRecord record;
		final int indentLevel;
		final String tagOverride; // if not null, use this instead of record.getTag()
		int childIndex;

		Frame(final FLEFRecord record, final int indentLevel){
			this(record, indentLevel, null);
		}

		Frame(final FLEFRecord record, final int indentLevel, final String tagOverride){
			this.record = record;
			this.indentLevel = indentLevel;
			this.tagOverride = tagOverride;
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
		this.indentSequence = Objects.requireNonNull(indent, "Indent cannot be null");
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
		final StringWriter sw = new StringWriter();
		try{
			write(model, sw);
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
		Objects.requireNonNull(model, "model cannot be null");
		Objects.requireNonNull(writer, "writer cannot be null");

		// Write header record if present
		if(model.getHeader() != null)
			writeRecord(model.getHeader(), writer, 0);

		// Write top-level records
		final List<FLEFRecord> records = model.getRecords();
		if(!records.isEmpty()){
			if(model.getHeader() != null)
				writer.write(StringUtils.LF);
			writer.write(FIELD_RECORDS);
			writer.write(StringUtils.SPACE);
			writer.write(TAG_OPEN_CURLY_BRACE);
			writer.write(StringUtils.LF);
			for(int i = 0; i < records.size(); i ++){
				final FLEFRecord record = records.get(i);

				writeRecord(record, writer, 1);
				if(i < records.size() - 1)
					writer.write(StringUtils.LF);
			}
			writer.write(TAG_CLOSE_CURLY_BRACE);
			writer.write(StringUtils.LF);
		}

		writer.flush();
	}

	/**
	 * Writes a {@link FLEFRecord} to an output {@link Writer}.
	 *
	 * @param record	Record to write.
	 * @param writer	Target output stream writer.
	 * @throws	IOException	If writing fails.
	 */
	public void write(final FLEFRecord record, final Writer writer) throws IOException{
		Objects.requireNonNull(record, "record cannot be null");
		Objects.requireNonNull(writer, "writer cannot be null");

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
		if(compactMode){
			final Effective effective = getEffectiveRecord(rootRecord);
			// Write the header with the compressed path
			writeRecordHeader(effective.record, writer, indentLevel, effective.path);
			if(effective.record.hasChildren())
				stack.push(new Frame(effective.record, indentLevel + 1, effective.path));
		}
		else{
			// Expanded mode: just write the root header normally
			writeRecordHeader(rootRecord, writer, indentLevel, null);
			if(rootRecord.hasChildren())
				stack.push(new Frame(rootRecord, indentLevel + 1));
		}

		while(!stack.isEmpty()){
			final Frame current = stack.peek();
			final FLEFRecord rec = current.record;
			final List<FLEFRecord> children = rec.getChildren();

			if(rec.hasChildren() && current.childIndex < children.size()){
				final FLEFRecord child = children.get(current.childIndex ++);

				if(child != null && (StringUtils.isNotEmpty(child.getTag()) || child.hasData())){
					final int nextIndent = current.indentLevel;
					if(compactMode){
						// In compact mode, check if the child is compressible
						final Effective effective = getEffectiveRecord(child);
						writeRecordHeader(effective.record, writer, nextIndent, effective.path);
						if(effective.record.hasChildren())
							stack.push(new Frame(effective.record, nextIndent + 1, effective.path));
					}
					else{
						// Expanded mode: write the child as a separate block
						writeRecordHeader(child, writer, nextIndent, null);
						if(child.hasChildren())
							stack.push(new Frame(child, nextIndent + 1));
					}
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
	 * Writes a record header (tag, optional ID, optional value, and opening brace).
	 *
	 * @param record      the record to write
	 * @param writer      the output writer
	 * @param indentLevel the current indentation level
	 * @param tagOverride if not {@code null}, use this tag instead of {@code record.getTag()}
	 * @throws IOException if writing fails
	 */
	private void writeRecordHeader(final FLEFRecord record, final Writer writer, final int indentLevel,
			final String tagOverride) throws IOException{
		writeIndent(writer, indentLevel);

		// Record tag/type – use tagOverride if provided
		final String tag = (tagOverride != null) ? tagOverride : record.getTag();
		if(tag != null)
			writer.write(tag);

		// Optional record ID
		final String formattedId = record.getFormattedId();
		if(StringUtils.isNotEmpty(formattedId)){
			writer.write(StringUtils.SPACE);
			writer.write(formattedId);
		}

		// Record value (supports single-line and multiline formatting)
		final String value = record.getValue();
		if(value != null){
			if(isMultiline(value)){
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
	 * Writes a multiline text value without extra indentation.
	 *
	 * @param text        the text to write
	 * @param writer      the output writer
	 * @param indentLevel the indentation level for each line (usually 0)
	 * @throws IOException if writing fails
	 */
	private void writeMultilineValue(final String text, final Writer writer, final int indentLevel) throws IOException{
		final String[] lines = text.split("\r?\n", -1);
		for(int i = 0, length = lines.length; i < length; i ++){
			final String line = lines[i];

			writeIndent(writer, indentLevel);
			writer.write(line);
			if(i < lines.length - 1)
				writer.write(StringUtils.LF);
		}
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
	 * Holds the result of compressing a chain of single‑child records.
	 *
	 * @param record the deepest (effective) record in the chain
	 * @param path   the dotted‑path tag (e.g., "valid_from.value.point.full_date")
	 */
	private record Effective(FLEFRecord record, String path){}

	/**
	 * Traverses a chain of records where each record has exactly one child
	 * and no value, and returns the deepest record together with the
	 * accumulated dotted‑path.
	 * <p>
	 * If the record is not compressible (has multiple children or a value),
	 * the record itself is returned with its own tag as the path.
	 *
	 * @param record the starting record
	 * @return an {@link Effective} containing the deepest record and its dot‑path
	 */
	private Effective getEffectiveRecord(final FLEFRecord record){
		if(!compactMode)
			return new Effective(record, record.getTag());

		// If the record has an ID, it cannot be compressed (the ID must be preserved)
		if(StringUtils.isNotEmpty(record.getFormattedId()))
			return new Effective(record, record.getTag());

		final StringBuilder path = new StringBuilder(record.getTag());
		FLEFRecord current = record;

		// A record is compressible if it has exactly one child, no value, and no ID
		while(current.getChildren().size() == 1 && current.getValue() == null
				&& StringUtils.isEmpty(current.getFormattedId())){
			current = current.getChildren().getFirst();
			path.append('.')
				.append(current.getTag());
		}

		return new Effective(current, path.toString());
	}

}
