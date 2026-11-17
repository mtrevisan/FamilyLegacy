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

package io.github.mtrevisan.familylegacy.externalsearch;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;


/**
 * What is searched: the values of the placeholders of a person (a value never known is simply absent).
 * <p>
 * Immutable; the values are stripped and a blank value counts as absent.
 */
public final class SearchSubject{

	/** A subject with no values at all. */
	public static final SearchSubject EMPTY = new SearchSubject(new EnumMap<>(Placeholder.class));


	private final Map<Placeholder, String> values;


	private SearchSubject(final Map<Placeholder, String> values){
		this.values = Collections.unmodifiableMap(values);
	}

	/**
	 * @param values	The values (blank or {@code null} ones are left out).
	 * @return	The subject.
	 */
	public static SearchSubject of(final Map<Placeholder, String> values){
		final Map<Placeholder, String> copy = new EnumMap<>(Placeholder.class);
		if(values != null)
			for(final Map.Entry<Placeholder, String> entry : values.entrySet())
				putIfPresent(copy, entry.getKey(), entry.getValue());
		return new SearchSubject(copy);
	}

	private static void putIfPresent(final Map<Placeholder, String> values, final Placeholder placeholder, final String value){
		if(placeholder != null && StringUtils.isNotBlank(value))
			values.put(placeholder, StringUtils.normalizeSpace(value));
	}


	/**
	 * @param placeholder	The placeholder.
	 * @return	Its value, the empty string if it is not known.
	 */
	public String value(final Placeholder placeholder){
		return values.getOrDefault(placeholder, StringUtils.EMPTY);
	}

	/**
	 * @param placeholder	The placeholder.
	 * @return	Whether its value is known.
	 */
	public boolean has(final Placeholder placeholder){
		return values.containsKey(placeholder);
	}

	/**
	 * @param placeholder	The placeholder of a year.
	 * @return	The year, empty if not known or not a number.
	 */
	public OptionalInt year(final Placeholder placeholder){
		final String value = values.get(placeholder);
		if(value == null)
			return OptionalInt.empty();

		try{
			return OptionalInt.of(Integer.parseInt(value));
		}
		catch(final NumberFormatException ignored){
			return OptionalInt.empty();
		}
	}

	/** @return	The known values (unmodifiable). */
	public Map<Placeholder, String> values(){
		return values;
	}

	/**
	 * @param placeholder	The placeholder.
	 * @param value	The new value (blank or {@code null} removes it).
	 * @return	A copy of this subject with the value changed.
	 */
	public SearchSubject with(final Placeholder placeholder, final String value){
		final Map<Placeholder, String> copy = new EnumMap<>(Placeholder.class);
		copy.putAll(values);
		copy.remove(placeholder);
		putIfPresent(copy, placeholder, value);
		return new SearchSubject(copy);
	}

	/**
	 * @param other	Values that take the place of the ones of this subject (only the placeholders the other has).
	 * @return	A copy of this subject with the values of the other one.
	 */
	public SearchSubject withAll(final SearchSubject other){
		final Map<Placeholder, String> copy = new EnumMap<>(Placeholder.class);
		copy.putAll(values);
		copy.putAll(other.values);
		return new SearchSubject(copy);
	}

	/** @return	Whether at least one part of the name is known. */
	public boolean hasName(){
		return (has(Placeholder.NAME_GIVEN) || has(Placeholder.SURNAME) || has(Placeholder.NAME_FULL));
	}


	@Override
	public boolean equals(final Object obj){
		return (this == obj || obj instanceof final SearchSubject other && values.equals(other.values));
	}

	@Override
	public int hashCode(){
		return Objects.hash(values);
	}

	@Override
	public String toString(){
		return values.toString();
	}

}
