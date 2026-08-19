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
package io.github.mtrevisan.familylegacy.v2.io.model;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public class XRefHelper{

	private static final Map<String, Integer> RESERVED_IDS = new ConcurrentHashMap<>();


	private XRefHelper(){}


	/**
	 * Adds a child record representing a reference (XREF) to another record.
	 * Automatically wraps the target ID with '@' delimiters.
	 *
	 * @param parent	The parent record.
	 * @param tag	The tag for the child record (e.g., "NOTE", "INDIVIDUAL").
	 * @param targetId	The raw target ID to reference.
	 * @return	The created child record, or {@code null} if input parameters are invalid.
	 */
	public static FLEFRecord addReferenceChild(final FLEFRecord parent, final String tag, final String targetId){
		if(parent == null || tag == null || targetId == null)
			return null;

		final FLEFRecord child = FLEFRecord.createChildWithTagAndValue(tag, targetId);
		parent.addChild(child);
		return child;
	}


	/**
	 * Generates and reserves a new unique ID.
	 * <p>
	 * The generated ID is guaranteed to be unique among both persisted records and IDs already reserved by open dialogs.
	 *
	 * @param model	The FLEF model.
	 * @param type	The record type (e.g., "INDIVIDUAL", "EVENT").
	 * @param prefix	The ID prefix (e.g., "I", "E").
	 * @return	A new unique ID.
	 */
	public static synchronized String generateNewId(final FLEFModel model, final String type, final String prefix){
		Integer next = RESERVED_IDS.get(prefix);
		if(next == null)
			next = model.getRecordsByType(type).stream()
				.map(FLEFRecord::getId)
				.filter(Objects::nonNull)
				.filter(id -> id.startsWith(prefix))
				.mapToInt(id -> {
					try{
						return Integer.parseInt(id.substring(prefix.length()));
					}
					catch(NumberFormatException ignored){
						return 0;
					}
				})
				.max()
				.orElse(0);
		next ++;

		RESERVED_IDS.put(prefix, next);

		return prefix + next;
	}

}
