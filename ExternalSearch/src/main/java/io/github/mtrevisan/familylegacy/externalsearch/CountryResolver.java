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

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * Tells the country of a place from the names of the place and of the places that contain it: "Italia", "Italy", "Italie" are
 * {@code IT}.
 * <p>
 * The names of the countries are the ones the Java platform knows in the main languages of Europe and of the Americas (the countries
 * of today), plus a few historical or common names whose country of today is not in doubt ("England", "Prussia", "Bohemia",
 * "Holland"). A name that is the one of several countries of today ("Austria-Hungary", "Galicia") tells nothing. The comparison
 * ignores case, accents and punctuation.
 */
public final class CountryResolver{

	/** The languages the names of the countries are taken in. */
	private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.ITALIAN, Locale.FRENCH, Locale.GERMAN,
		Locale.of("es"), Locale.of("pt"), Locale.of("nl"), Locale.of("pl"), Locale.of("cs"), Locale.of("sk"), Locale.of("hu"),
		Locale.of("ro"), Locale.of("hr"), Locale.of("sl"), Locale.of("sv"), Locale.of("da"), Locale.of("nb"), Locale.of("fi"),
		Locale.of("is"), Locale.of("ga"), Locale.of("el"), Locale.of("ru"), Locale.of("uk"), Locale.of("lt"), Locale.of("lv"),
		Locale.of("et"), Locale.of("la"), Locale.of("ca"));

	/** Names that are not the name of a country of today, or that the platform does not give. */
	private static final Map<String, String> ALIASES = Map.ofEntries(
		Map.entry("England", "GB"), Map.entry("Scotland", "GB"), Map.entry("Wales", "GB"), Map.entry("Northern Ireland", "GB"),
		Map.entry("Great Britain", "GB"), Map.entry("Britain", "GB"), Map.entry("UK", "GB"),
		Map.entry("USA", "US"), Map.entry("U.S.A.", "US"), Map.entry("United States of America", "US"), Map.entry("America", "US"),
		Map.entry("Stati Uniti d'America", "US"),
		Map.entry("Holland", "NL"), Map.entry("Olanda", "NL"),
		Map.entry("Prussia", "DE"), Map.entry("Preussen", "DE"), Map.entry("Preußen", "DE"), Map.entry("Bavaria", "DE"),
		Map.entry("Bayern", "DE"), Map.entry("Baviera", "DE"), Map.entry("Württemberg", "DE"), Map.entry("Saxony", "DE"),
		Map.entry("Sachsen", "DE"), Map.entry("Hannover", "DE"), Map.entry("Deutsches Reich", "DE"), Map.entry("German Empire", "DE"),
		Map.entry("Bohemia", "CZ"), Map.entry("Böhmen", "CZ"), Map.entry("Boemia", "CZ"), Map.entry("Moravia", "CZ"),
		Map.entry("Mähren", "CZ"),
		Map.entry("Regno d'Italia", "IT"), Map.entry("Kingdom of Italy", "IT"), Map.entry("Regno di Sardegna", "IT"),
		Map.entry("Regno delle Due Sicilie", "IT"), Map.entry("Granducato di Toscana", "IT"), Map.entry("Repubblica di Venezia", "IT"),
		Map.entry("Serenissima", "IT"),
		Map.entry("Russian Federation", "RU"),
		Map.entry("Eire", "IE"));

	private static volatile Map<String, String> index;


	private CountryResolver(){}


	/**
	 * @param name	The name of a place.
	 * @return	The ISO 3166-1 alpha-2 code of the country it names, empty if it does not name one.
	 */
	public static Optional<String> resolve(final String name){
		final String key = normalize(name);
		return (key.isEmpty()? Optional.empty(): Optional.ofNullable(index().get(key)));
	}

	/**
	 * @param levels	The names of a place and of the places that contain it, one level at a time, the place first (each level may have
	 * 	several names: the alternative ones, or several containers).
	 * @return	The country of the outermost level that names one, empty if none does.
	 */
	public static Optional<String> resolveLevels(final List<List<String>> levels){
		for(int i = levels.size() - 1; i >= 0; i --)
			for(final String name : levels.get(i)){
				final Optional<String> country = resolve(name);
				if(country.isPresent())
					return country;
			}
		return Optional.empty();
	}

	private static Map<String, String> index(){
		Map<String, String> result = index;
		if(result == null){
			synchronized(CountryResolver.class){
				result = index;
				if(result == null){
					result = buildIndex();
					index = result;
				}
			}
		}
		return result;
	}

	private static Map<String, String> buildIndex(){
		final Map<String, String> names = new HashMap<>(8192);
		for(final String code : Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2)){
			final Locale country = Locale.of(StringUtils.EMPTY, code);
			for(final Locale language : LANGUAGES){
				final String name = normalize(country.getDisplayCountry(language));
				//a name the platform does not have comes back as the code itself: not a name
				if(!name.isEmpty() && !name.equalsIgnoreCase(code))
					names.putIfAbsent(name, code);
			}
		}
		for(final Map.Entry<String, String> alias : ALIASES.entrySet())
			names.put(normalize(alias.getKey()), alias.getValue());
		return Map.copyOf(names);
	}

	/** @return	The name in lower case, without accents, with only letters and digits separated by one space. */
	static String normalize(final String name){
		if(name == null)
			return StringUtils.EMPTY;

		final String decomposed = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		final StringBuilder sb = new StringBuilder(decomposed.length());
		boolean space = false;
		for(int i = 0; i < decomposed.length(); i ++){
			final char c = decomposed.charAt(i);
			if(Character.isLetterOrDigit(c)){
				if(space && !sb.isEmpty())
					sb.append(' ');
				sb.append(c);
				space = false;
			}
			else if(Character.getType(c) != Character.NON_SPACING_MARK)
				space = true;
		}
		//"ß" has no decomposition: "Preußen" and "Preussen" are the same name
		return StringUtils.replace(sb.toString(), "ß", "ss");
	}

}
