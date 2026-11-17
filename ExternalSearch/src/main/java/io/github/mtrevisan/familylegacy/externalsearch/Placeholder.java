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

import io.github.mtrevisan.familylegacy.i18n.I18N;

import java.util.Locale;
import java.util.Optional;


/**
 * A value of a person that a URL template can contain, written between braces: {@code {name_given}}, {@code {birth_place}}, ...
 * <p>
 * The years are Gregorian; a range ({@code _from}, {@code _to}) is the window a search should look in (the uncertainty of the date,
 * widened a little around an exact year: records often disagree by a year or two). A place is the town (or the nearest place that
 * is not a building, a street, a parish or a cemetery), the {@code _full} variant the whole hierarchy, the {@code _country} variant
 * the ISO 3166 code of the country, when it can be told.
 */
public enum Placeholder{

	NAME_GIVEN("name_given"),
	SURNAME("surname"),
	NAME_FULL("name_full"),
	SEX("sex"),

	BIRTH_YEAR("birth_year"),
	BIRTH_YEAR_FROM("birth_year_from"),
	BIRTH_YEAR_TO("birth_year_to"),
	BIRTH_PLACE("birth_place"),
	BIRTH_PLACE_FULL("birth_place_full"),
	BIRTH_COUNTRY("birth_country"),

	DEATH_YEAR("death_year"),
	DEATH_YEAR_FROM("death_year_from"),
	DEATH_YEAR_TO("death_year_to"),
	DEATH_PLACE("death_place"),
	DEATH_PLACE_FULL("death_place_full"),
	DEATH_COUNTRY("death_country"),

	FATHER_GIVEN("father_given"),
	FATHER_SURNAME("father_surname"),
	MOTHER_GIVEN("mother_given"),
	MOTHER_SURNAME("mother_surname"),
	SPOUSE_GIVEN("spouse_given"),
	SPOUSE_SURNAME("spouse_surname");


	private final String token;


	Placeholder(final String token){
		this.token = token;
	}


	/**
	 * @param token	The name written between braces in a template (the case does not matter).
	 * @return	The placeholder, empty if there is none with that name.
	 */
	public static Optional<Placeholder> of(final String token){
		if(token == null)
			return Optional.empty();

		final String name = token.strip().toLowerCase(Locale.ROOT);
		for(final Placeholder placeholder : values())
			if(placeholder.token.equals(name))
				return Optional.of(placeholder);
		return Optional.empty();
	}


	/** @return	The name written between braces in a template. */
	public String token(){
		return token;
	}

	/** @return	The placeholder as written in a template: {@code {token}}. */
	public String written(){
		return "{" + token + "}";
	}

	/** @return	Whether it is a part of the name of the person searched (the values that change from one name of the person to another). */
	public boolean isName(){
		return (this == NAME_GIVEN || this == SURNAME || this == NAME_FULL);
	}

	/** @return	The (localized) description of the value. */
	public String label(){
		return I18N.t("external_search.placeholder." + token, token);
	}

}
