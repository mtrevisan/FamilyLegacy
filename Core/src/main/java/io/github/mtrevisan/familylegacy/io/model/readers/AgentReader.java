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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;


/**
 * Handler for AGENT records.
 * <p>
 * Structure:
 * <pre>
 * Agent = oneof {
 *  individual: Xref&lt;IndividualRecord&gt;   // the author is an individual described in this file
 *  group: Xref&lt;GroupRecord&gt;             // the author is a group or organization described in this file (e.g. a parish, a confraternity)
 *  text: Text                           // the author is not described in this file (e.g. a publisher's editor, an unidentified clerk)
 * }
 * </pre>
 */
public final class AgentReader{

	public static final String TAG_INDIVIDUAL = "individual";
	public static final String TAG_GROUP = "group";
	public static final String TAG_TEXT = "text";


	private AgentReader(){}


	public static String extractAgent(final FLEFRecord record){
		final String individual = FLEFRecordHelper.getChildValue(record, TAG_INDIVIDUAL);
		if(individual != null)
			return individual;

		final String group = FLEFRecordHelper.getChildValue(record, TAG_GROUP);
		if(group != null)
			return group;

		final String text = FLEFRecordHelper.getChildValue(record, TAG_TEXT);
		return text;
	}

}
