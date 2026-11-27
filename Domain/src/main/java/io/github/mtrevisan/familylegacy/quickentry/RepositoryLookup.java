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

import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.List;


/** Finds the repositories of a file by the name that the user typed, the way {@link PlaceLookup} finds the places. */
public final class RepositoryLookup{

	private RepositoryLookup(){}


	/**
	 * @param model	The model.
	 * @param text	The name that was typed.
	 * @param limit	The maximum number of repositories to return.
	 * @return	The repositories named like the text, then the ones whose name starts with it, in the order of the file.
	 */
	public static List<FLEFRecord> find(final RecordLookup model, final String text, final int limit){
		return NameLookup.find(model, RepositoryReader.TYPE, RepositoryReader.TAG_NAME, NameReader.TAG_VALUE, text, limit);
	}

}
