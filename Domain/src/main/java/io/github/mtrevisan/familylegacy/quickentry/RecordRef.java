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

import org.apache.commons.lang3.StringUtils;


/**
 * A record that a quick entry refers to: one that is in the file, or one that is not in the file yet and is made from its name (a name
 * always makes a new record: looking for an existing one is up to the caller, which lets the user choose).
 *
 * @param existingId	The id of a record of the file, {@code null} if none.
 * @param newName	The name of a new record, {@code null} if none.
 */
public record RecordRef(String existingId, String newName){

	/** No record. */
	public static final RecordRef NONE = new RecordRef(null, null);


	/** @return	The record of the file with the given id ({@link #NONE} if the id is blank). */
	public static RecordRef existing(final String id){
		return (StringUtils.isNotBlank(id)? new RecordRef(id, null): NONE);
	}

	/** @return	A new record with the given name ({@link #NONE} if the name is blank). */
	public static RecordRef named(final String name){
		return (StringUtils.isNotBlank(name)? new RecordRef(null, StringUtils.normalizeSpace(name)): NONE);
	}


	/** @return	Whether there is no record. */
	public boolean isNone(){
		return (existingId == null && newName == null);
	}

}
