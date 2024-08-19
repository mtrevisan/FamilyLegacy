/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


public class SearchParser{

	public static final String PARAMETER_SEARCH_TEXT = "searchText";
	public static final String PARAMETER_ADDITIONAL_FIELDS = "additionalFields";

	private static final String ADDITIONAL_FIELD_ORDER_BY = "orderby";
	private static final String SORT_ORDER_DESCENDING = " DESC";
	private static final String SORT_ORDER_ASCENDING = " ASC";

	private static final String PARAMETER_SEPARATOR = ":";


	private SearchParser(){}


	public static Map<String, Object> parseSearchQuery(final String query){
		//map to store the parsing results
		final Map<String, Object> result = new HashMap<>();
		//list to store additional fields in X:Y format
		final List<Map.Entry<String, String>> additionalFields = new ArrayList<>();
		//hold the main search text
		final StringJoiner searchText = new StringJoiner(StringUtils.SPACE);

		//split the input string based on spaces
		final String[] parts = StringUtils.split(query);

		String currentKey = null;
		final StringBuilder currentFieldValue = new StringBuilder();
		boolean isProcessingField = false;
		for(int i = 0, length = parts.length; i < length; i ++){
			final String part = parts[i];

			final boolean isAdditionalField = part.contains(PARAMETER_SEPARATOR);
			if(isAdditionalField){
				if(!currentFieldValue.isEmpty())
					//add the previous key-value pair to the list
					additionalFields.add(Map.entry(currentKey, currentFieldValue.toString()));

				//start a new additional field
				final String[] keyValue = StringUtils.split(part, ':');
				currentKey = keyValue[0].trim();
				currentFieldValue.setLength(0);
				currentFieldValue.append(keyValue[1].trim());
				isProcessingField = true;
			}
			else if(isProcessingField)
				//continue appending parts to additional field
				currentFieldValue.append(StringUtils.SPACE)
					.append(part);
			else
				//part of the main search text
				searchText.add(part);
		}

		//add the last additional field found, if any
		if(!currentFieldValue.isEmpty())
			additionalFields.add(Map.entry(currentKey, currentFieldValue.toString()));

		//add the main search text and additional fields to the result map
		result.put(PARAMETER_SEARCH_TEXT, searchText.toString());
		result.put(PARAMETER_ADDITIONAL_FIELDS, additionalFields);

		return result;
	}


	public static List<RowSorter.SortKey> getSortKeys(final List<Map.Entry<String, String>> additionalFields, final String[] columnNames){
		final List<RowSorter.SortKey> sortKeys = new ArrayList<>(0);
		for(int i = 0, length = additionalFields.size(); i < length; i ++){
			final Map.Entry<String, String> additionalField = additionalFields.get(i);

			if(additionalField.getKey().equals(SearchParser.ADDITIONAL_FIELD_ORDER_BY)){
				final String value = additionalField.getValue();
				final SortOrder sortOrder = SearchParser.detectSortOrder(value);
				final String columnName = SearchParser.stripSortOrder(value);

				for(int columnIndex = 0, columnCount = columnNames.length; columnIndex < columnCount; columnIndex ++)
					if(columnNames[columnIndex].equalsIgnoreCase(columnName)){
						final RowSorter.SortKey sortKey = new RowSorter.SortKey(columnIndex, sortOrder);
						sortKeys.add(sortKey);
						break;
					}
			}
		}
		return sortKeys;
	}

	private static SortOrder detectSortOrder(final String key){
		return (key.endsWith(SORT_ORDER_DESCENDING)? SortOrder.DESCENDING: SortOrder.ASCENDING);
	}

	private static String stripSortOrder(final String key){
		if(key.endsWith(SORT_ORDER_DESCENDING))
			return key.substring(0, key.length() - 5);
		else if(key.endsWith(SORT_ORDER_ASCENDING))
			return key.substring(0, key.length() - 4);
		return key;
	}

}
