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
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;


/**
 * Serializer class to output a {@link FLEFModel} or {@link FLEFRecord} tree into FLEF format.
 */
public class FLEFWriter{

	private static final String TAG_OPEN_CURLY_BRACE = "{";
	private static final String TAG_CLOSE_CURLY_BRACE = "}";

	private static final String FIELD_RECORDS = "records";

	private static final String TAG_VALUE_MULTILINE = "\"\"\"";


	private final String indentSequence;


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
	 * Creates a writer with default 2-space indentation.
	 */
	public static FLEFWriter create(){
		return new FLEFWriter("  ");
	}

	/**
	 * Creates a writer with custom indentation sequence.
	 *
	 * @param indentSequence	string used for each level of indentation
	 */
	public static FLEFWriter createWithIndentSequence(final String indentSequence){
		return new FLEFWriter(indentSequence);
	}


	private FLEFWriter(final String indentSequence){
		this.indentSequence = Objects.requireNonNull(indentSequence);
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
			for(int i = 0; i < records.size(); i++){
				writeRecord(records.get(i), writer, 1);
				if(i < records.size() - 1)
					writer.write(StringUtils.LF);
			}
			writer.write(TAG_CLOSE_CURLY_BRACE);
			writer.write(StringUtils.LF);
		}
		writer.flush();
	}

	/**
	 * Iteratively writes a {@link FLEFRecord} structure without recursion.
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
		stack.push(new Frame(rootRecord, indentLevel));

		// Start opening sequence for the root record
		writeRecordHeader(rootRecord, writer, indentLevel);

		while(!stack.isEmpty()){
			final Frame current = stack.peek();
			final FLEFRecord rec = current.record;
			final List<FLEFRecord> children = rec.getChildren();

			if(rec.hasChildren() && current.childIndex < children.size()){
				final FLEFRecord child = children.get(current.childIndex++);
				if(child != null && child.hasData()){
					final int nextIndent = current.indentLevel + 1;
					writeRecordHeader(child, writer, nextIndent);
					if(child.hasChildren())
						stack.push(new Frame(child, nextIndent));
				}
			}
			else{
				// All children processed or leaf node reached
				if(rec.hasChildren()){
					writeIndent(writer, current.indentLevel);
					writer.write(TAG_CLOSE_CURLY_BRACE);
					writer.write(StringUtils.LF);
				}

				stack.pop();
			}
		}
	}

	private void writeRecordHeader(final FLEFRecord record, final Writer writer, final int indentLevel)
			throws IOException{
		writeIndent(writer, indentLevel);

		// Record tag/type
		if(record.getTag() != null)
			writer.write(record.getTag());

		// Optional record ID
		final String formattedId = record.getFormattedId();
		if(formattedId != null && !formattedId.isEmpty()){
			writer.write(StringUtils.SPACE);
			writer.write(formattedId);
		}

		// Record value (supports single-line and multiline formatting)
		final String val = record.getValue();
		if(val != null){
			if(isMultiline(val)){
				writer.write(StringUtils.SPACE);
				writer.write(TAG_VALUE_MULTILINE);
				writer.write(StringUtils.LF);
				writeMultilineValue(val, writer, indentLevel + 1);
				writeIndent(writer, indentLevel);
				writer.write(TAG_VALUE_MULTILINE);
			}
			else{
				writer.write(StringUtils.SPACE);
				writer.write(val);
			}
		}

		// Child block hierarchy start
		if(record.hasChildren()){
			writer.write(StringUtils.SPACE);
			writer.write(TAG_OPEN_CURLY_BRACE);
		}
		writer.write(StringUtils.LF);
	}

	private void writeMultilineValue(final String text, final Writer writer, final int indentLevel) throws IOException{
		final String[] lines = text.split("\r?\n", -1);
		for(final String line : lines){
			writeIndent(writer, indentLevel);
			writer.write(line);
			writer.write(StringUtils.LF);
		}
	}

	private boolean isMultiline(final String str){
		return (str.contains(StringUtils.LF) || str.contains(StringUtils.CR));
	}

	private void writeIndent(final Writer writer, final int level) throws IOException{
		for(int i = 0; i < level; i ++)
			writer.write(indentSequence);
	}

}
