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

package io.github.mtrevisan.familylegacy.externalsearch.flef;

import io.github.mtrevisan.familylegacy.externalsearch.Placeholder;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.Map;


/**
 * One of the names a person can be searched by: a name of the individual (birth, married, religious, alias, ...), or one of its
 * transcriptions (a romanization, a Latin form), or a name the user writes.
 *
 * @param label	What the name is (its type, and the system of a transcription), for the user.
 * @param given	The given names, separated by a space; may be empty.
 * @param surname	The surname (with its prefix: "de", "van"), or what takes its place when there is none (a patronymic, a toponymic);
 * 	may be empty.
 * @param full	The whole name, in the order of its parts; may be empty.
 */
public record NameCandidate(String label, String given, String surname, String full){

	public NameCandidate{
		label = StringUtils.defaultString(StringUtils.strip(label));
		given = StringUtils.normalizeSpace(StringUtils.defaultString(given));
		surname = StringUtils.normalizeSpace(StringUtils.defaultString(surname));
		full = StringUtils.normalizeSpace(StringUtils.defaultString(full));
		if(full.isEmpty())
			full = StringUtils.normalizeSpace(given + StringUtils.SPACE + surname);
	}

	/** @return	Whether no part of the name is known. */
	public boolean isEmpty(){
		return (given.isEmpty() && surname.isEmpty() && full.isEmpty());
	}

	/** @return	The values of the name for the placeholders ({@link Placeholder#isName()}). */
	public SearchSubject asSubject(){
		final Map<Placeholder, String> values = new EnumMap<>(Placeholder.class);
		values.put(Placeholder.NAME_GIVEN, given);
		values.put(Placeholder.SURNAME, surname);
		values.put(Placeholder.NAME_FULL, full);
		return SearchSubject.of(values);
	}

}
