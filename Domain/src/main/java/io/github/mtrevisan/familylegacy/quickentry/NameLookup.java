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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Finds the records of a kind (places, repositories...) by a name that the user typed, so that a record is reused instead of being
 * created again.
 * <p>
 * The comparison ignores case, accents and extra spaces, and looks at every name of the record. The records whose name is the text come
 * first, then the ones whose name starts with it. What the user typed is never replaced silently by one of them: it is the user who picks.
 */
final class NameLookup{

	private NameLookup(){}


	/**
	 * @param model	The model.
	 * @param recordType	The kind of record.
	 * @param nameTag	The tag of the names of the record.
	 * @param valueTag	The tag of the text inside a name.
	 * @param text	The name that was typed.
	 * @param limit	The maximum number of records to return.
	 * @return	The records named like the text, then the ones whose name starts with it, in the order of the file (empty if the text is blank).
	 */
	static List<FLEFRecord> find(final RecordLookup model, final String recordType, final String nameTag, final String valueTag,
			final String text, final int limit){
		final String key = fold(text);
		if(key.isEmpty() || limit <= 0)
			return List.of();

		final List<FLEFRecord> exact = new ArrayList<>();
		final List<FLEFRecord> prefixed = new ArrayList<>();
		for(final FLEFRecord record : model.getRecordsByType(recordType)){
			boolean isExact = false;
			boolean isPrefixed = false;
			for(final FLEFRecord name : FLEFRecordHelper.findChildren(record, nameTag)){
				final String candidate = fold(FLEFRecordHelper.getChildValue(name, valueTag));
				if(candidate.equals(key))
					isExact = true;
				else if(candidate.startsWith(key))
					isPrefixed = true;
			}
			if(isExact)
				exact.add(record);
			else if(isPrefixed)
				prefixed.add(record);
		}

		exact.addAll(prefixed);
		return (exact.size() > limit? List.copyOf(exact.subList(0, limit)): List.copyOf(exact));
	}

	/** @return	The name as it is compared: lower case, without accents, with the spaces normalized. */
	static String fold(final String name){
		if(name == null)
			return StringUtils.EMPTY;

		return StringUtils.stripAccents(StringUtils.normalizeSpace(name).toLowerCase(Locale.ROOT));
	}

}
