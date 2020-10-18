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
package io.github.mtrevisan.familylegacy.gedcom2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GedcomLine{

	//NOTE: DOTALL is for unicode line separator
	private static final Pattern GEDCOM_LINE = Pattern.compile("^\\s*(\\d)\\s+(@([^@ ]+)@\\s+)?([a-zA-Z_0-9.]+)(\\s+@([^@ ]+)@)?(\\s(.*))?$", Pattern.DOTALL);
	private static final int GEDCOM_LINE_LEVEL = 1;
	private static final int GEDCOM_LINE_ID = 3;
	private static final int GEDCOM_LINE_TAG = 4;
	private static final int GEDCOM_LINE_XREF = 6;
	private static final int GEDCOM_LINE_VALUE = 8;


	private int level;
	private String tag;
	private String id;
	private String xref;
	private String value;


	public static GedcomLine parse(final String line){
		final Matcher m = GEDCOM_LINE.matcher(line);
		if(!m.find())
			return null;

		final GedcomLine gedcomLine = new GedcomLine();
		gedcomLine.level = Integer.parseInt(m.group(GEDCOM_LINE_LEVEL));
		gedcomLine.tag = m.group(GEDCOM_LINE_TAG);
		gedcomLine.id = m.group(GEDCOM_LINE_ID);
		gedcomLine.xref = m.group(GEDCOM_LINE_XREF);
		gedcomLine.value = m.group(GEDCOM_LINE_VALUE);
		return gedcomLine;
	}

	public int getLevel(){
		return level;
	}

	public String getTag(){
		return tag;
	}

	public String getID(){
		return id;
	}

	public String getXRef(){
		return xref;
	}

	public String getValue(){
		return value;
	}

}
