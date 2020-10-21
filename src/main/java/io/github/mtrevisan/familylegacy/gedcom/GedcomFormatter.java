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
package io.github.mtrevisan.familylegacy.gedcom;

import java.util.Collection;


/**
 * This class contains some static methods which help with formatting data to be used in gedcom files.<br />
 * <br />
 * All the methods are synchronized for thread save access since many objects (like the date formats) are shared.
 */
final class GedcomFormatter{

	private GedcomFormatter(){}

	/**
	 * Makes a or-list out of the given list, adding the given pre- and suffixes
	 * to each or-item.<br>
	 * A generated list could look like:<br>
	 * [ITEM1|ITEM2|ITEM3]<br>
	 * or with prefix &lt; and suffix &gt;:<br>
	 * [&lt;ITEM1&gt;|&lt;ITEM2&gt;|&lt;ITEM3&gt;]
	 */
	public static StringBuilder makeOrList(final Collection<String> list){
		final StringBuilder sb = new StringBuilder();
		for(final String item : list){
			if(sb.length() > 0)
				sb.append("|");
			if(item == null || "NULL".equals(item))
				sb.append("<");
			sb.append(item);
			if(item == null || "NULL".equals(item))
				sb.append(">");
		}
		if(list.size() > 1)
			sb.insert(0, "[");
		if(list.size() > 1)
			sb.append("]");
		return sb;
	}

}
