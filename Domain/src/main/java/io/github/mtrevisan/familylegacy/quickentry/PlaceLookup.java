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

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.List;


/**
 * Finds the places of a file by the name that the user typed, so that a place is reused instead of being created again.
 * <p>
 * The comparison ignores case, accents and extra spaces, and looks at every name of the place. The places whose name is the text come
 * first, then the ones whose name starts with it. What the user typed is never replaced silently by one of them: it is the user who
 * picks (a name may be the one of different places, or of the same place under different jurisdictions in different eras).
 */
public final class PlaceLookup{

	private PlaceLookup(){}


	/**
	 * @param model	The model.
	 * @param text	The name that was typed.
	 * @param limit	The maximum number of places to return.
	 * @return	The places named like the text, then the ones whose name starts with it, in the order of the file (empty if the text is blank).
	 */
	public static List<FLEFRecord> find(final RecordLookup model, final String text, final int limit){
		return NameLookup.find(model, PlaceReader.TYPE, PlaceReader.TAG_NAME, PlaceReader.TAG_NAME_VALUE, text, limit);
	}

	/** @return	The name as it is compared: lower case, without accents, with the spaces normalized. */
	static String fold(final String name){
		return NameLookup.fold(name);
	}

}
