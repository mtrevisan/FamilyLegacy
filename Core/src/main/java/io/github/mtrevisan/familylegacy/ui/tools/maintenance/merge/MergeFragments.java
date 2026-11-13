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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance.merge;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The text of a field of a record, as the merge dialogs show it and the user edits it: how a field is written as FLEF text and how
 * the edited text is read back.
 */
final class MergeFragments{

	/** The id of the record that wraps a fragment to be parsed. */
	private static final String SYNTHETIC_ID = "PARSER_TMP";


	private MergeFragments(){}


	/** The FLEF text of the field, indented, one line per tag. */
	static String serialize(final FLEFRecord record){
		final StringBuilder sb = new StringBuilder();
		serializeInto(record, 0, sb);
		return sb.toString();
	}

	/** The FLEF text of the occurrences of a field, one after the other; empty if there are none. */
	static String serialize(final List<FLEFRecord> records){
		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord record : records)
			serializeInto(record, 0, sb);
		return sb.toString();
	}

	private static void serializeInto(final FLEFRecord record, final int indent, final StringBuilder sb){
		final String prefix = "  ".repeat(indent);
		sb.append(prefix)
			.append(record.getTag() != null? record.getTag(): "?");
		if(record.getId() != null)
			sb.append(' ').append(record.getId());
		if(record.getValue() != null)
			sb.append(' ').append(record.getValue());
		if(record.hasChildren()){
			sb.append(" {")
				.append(StringUtils.LF);
			for(final FLEFRecord child : record.getChildren())
				serializeInto(child, indent + 1, sb);
			sb.append(prefix)
				.append('}')
				.append(StringUtils.LF);
		}
		else
			sb.append(StringUtils.LF);
	}

	/**
	 * Parses the text as the occurrences of a field.
	 * <p>
	 * The fragment is wrapped in a synthetic record of the same type as the one that holds the field (e.g. {@code individual}), with a
	 * placeholder id, so that the parser sees a document that looks like a real file. The synthetic record's children are the
	 * occurrences. If the user has broken the structure the wrong children are returned, and the validation of the file rejects them.
	 *
	 * @param recordTag the tag of the record that holds the field
	 * @param text      the text to read
	 * @return the occurrences, never {@code null}
	 * @throws IOException on a syntax error in the text
	 */
	static List<FLEFRecord> parse(final String recordTag, final String text) throws IOException{
		if(StringUtils.isBlank(text))
			return List.of();

		final String wrapped = "records {" + StringUtils.LF
			+ recordTag + StringUtils.SPACE + SYNTHETIC_ID + " {" + StringUtils.LF
			+ text + StringUtils.LF
			+ "}" + StringUtils.LF
			+ "}" + StringUtils.LF;

		final FLEFModel model = new FLEFParser().parse(wrapped);
		final FLEFRecord synthetic = model.getRecordById(SYNTHETIC_ID);
		if(synthetic == null)
			return List.of();

		return new ArrayList<>(synthetic.getChildren());
	}

}
