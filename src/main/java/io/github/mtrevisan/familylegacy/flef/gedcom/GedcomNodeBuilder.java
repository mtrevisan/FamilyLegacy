/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.gedcom;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class GedcomNodeBuilder{

	/** NOTE: {@link Pattern#DOTALL} is for unicode line separator. */
	private static final Pattern GEDCOM_LINE = Pattern.compile("^\\s*(\\d)\\s+(@([^@ ]+)@\\s+)?([a-zA-Z_0-9.]+)(\\s+@([^@ ]+)@)?(\\s(.*))?$",
		Pattern.DOTALL);
	private static final int GEDCOM_LINE_LEVEL = 1;
	private static final int GEDCOM_LINE_ID = 3;
	private static final int GEDCOM_LINE_TAG = 4;
	private static final int GEDCOM_LINE_XREF = 6;
	private static final int GEDCOM_LINE_VALUE = 8;


	public static GedcomNode createRoot(){
		final GedcomNode root = createEmpty();
		root.level = -1;
		return root;
	}

	public static GedcomNode createEmpty(){
		return new GedcomNodeGedcom();
	}

	public static GedcomNode create(final String tag){
		if(tag == null || tag.isEmpty())
			throw new IllegalArgumentException("Tag must be present");

		return createEmpty()
			.withTag(tag);
	}

	public static GedcomNode createWithID(final String tag, final String id){
		if(id == null || id.isEmpty())
			throw new IllegalArgumentException("ID must be present");

		return create(tag)
			.withID(id);
	}

	public static GedcomNode createWithValue(final String tag, final String value){
		return create(tag)
			.withValue(value);
	}

	public static GedcomNode createWithIDValue(final String tag, final String id, final String value){
		if(id == null || id.isEmpty())
			throw new IllegalArgumentException("ID must be present");

		return create(tag)
			.withID(id)
			.withValue(value);
	}

	public static GedcomNode createWithReference(final String tag, final String xref){
		if(xref == null || xref.isEmpty())
			throw new IllegalArgumentException("XRef must be present");

		return create(tag)
			.withXRef(xref);
	}

	public static GedcomNode parse(final CharSequence line){
		final Matcher m = GEDCOM_LINE.matcher(line);
		if(!m.find())
			return null;

		return createEmpty()
			.withLevel(m.group(GEDCOM_LINE_LEVEL))
			.withID(m.group(GEDCOM_LINE_ID))
			.withTag(m.group(GEDCOM_LINE_TAG))
			.withXRef(m.group(GEDCOM_LINE_XREF))
			.withValue(StringUtils.replace(m.group(GEDCOM_LINE_VALUE), "@@", "@"));
	}

}