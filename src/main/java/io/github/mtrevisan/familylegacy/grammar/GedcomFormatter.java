/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mtrevisan.familylegacy.grammar;

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
	public static StringBuilder makeOrList(final Collection<String> list, final String itemPrefix, final String itemSuffix){
		final StringBuilder sb = new StringBuilder();
		for(final String item : list){
			if(sb.length() > 0)
				sb.append("|");
			if(item == null || "NULL".equals(item))
				sb.append("<");
			else if(itemPrefix != null)
				sb.append(itemPrefix);
			sb.append(item);
			if(item == null || "NULL".equals(item))
				sb.append(">");
			else if(itemSuffix != null)
				sb.append(itemSuffix);
		}
		if(list.size() > 1)
			sb.insert(0, "[");
		if(list.size() > 1)
			sb.append("]");
		return sb;
	}

}
