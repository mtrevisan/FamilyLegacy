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
package io.github.mtrevisan.calendars.gregorian;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * The Gregorian reforms by historical jurisdiction.
 * <p>
 * Several adoptions happened at the level of a land, a province, or a confession, not of a modern state (the Austrian
 * lands, the Dutch provinces, the Southern Netherlands...). A jurisdiction is therefore a path
 * {@code "Country/Subdivision"} (for example {@code "Austria/Tyrol"}), and {@link #resolve(String)} picks the most
 * specific reform that applies: an exact match first, then the parent path. A country that has no single reform date
 * (Austria, the Netherlands, Belgium) has <em>no</em> national fallback, so no historically inaccurate "national" date is
 * ever invented; {@link #subdivisions(String)} lists what the caller can ask the user instead.
 * <p>
 * Source: "List of adoption dates of the Gregorian calendar by country" (Wikipedia), which cites Steinmetz and Fruin
 * (1934) among others. The Swiss rows were read verbatim from that page, but there is no second source to check them
 * against.
 */
public final class CalendarReforms{

	/** The Protestant parts of Switzerland: 31 December 1700 was followed by 12 January 1701. */
	private static final CalendarReform PROTESTANT_SWITZERLAND = new CalendarReform(1700, 12, 31, 1701, 1, 12);

	private static final Map<String, CalendarReform> REGISTRY = new LinkedHashMap<>();
	private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();

	static{
		//countries with a single reform
		register("Italy", CalendarReform.ROME);
		register("Spain", CalendarReform.ROME);
		register("Portugal", CalendarReform.ROME);
		register("Poland", CalendarReform.ROME);
		register("Papal States", CalendarReform.ROME);
		register("France", CalendarReform.FRANCE);
		register("Great Britain", CalendarReform.BRITAIN);
		register("Sweden", CalendarReform.SWEDEN);
		register("Finland", CalendarReform.SWEDEN);
		register("Denmark", CalendarReform.DENMARK);
		register("Bulgaria", CalendarReform.BULGARIA);
		register("Russia", CalendarReform.RUSSIA);
		register("Romania", CalendarReform.ROMANIA);
		register("Greece", CalendarReform.GREECE);
		register("Hungary", new CalendarReform(1587, 10, 21, 1587, 11, 1));
		register("Luxembourg", new CalendarReform(1582, 12, 20, 1582, 12, 31));

		//Austrian lands (no national date)
		register("Austria/Carinthia", new CalendarReform(1583, 12, 14, 1583, 12, 25));
		register("Austria/Lower Austria", new CalendarReform(1583, 10, 20, 1583, 10, 31));
		register("Austria/Upper Austria", new CalendarReform(1583, 10, 20, 1583, 10, 31));
		register("Austria/Salzburg", new CalendarReform(1583, 2, 10, 1583, 2, 21));
		register("Austria/Styria", new CalendarReform(1583, 12, 11, 1583, 12, 22));
		register("Austria/Tyrol", CalendarReform.ROME_1583);
		register("Austria/Further Austria", CalendarReform.ROME_1583);
		register("Slovenia/Carniola", CalendarReform.ROME_1583);

		//Bohemian lands
		register("Bohemia", new CalendarReform(1584, 1, 6, 1584, 1, 17));
		register("Bohemia/Moravia", new CalendarReform(1584, 10, 15, 1584, 10, 26));
		register("Bohemia/Silesia", new CalendarReform(1584, 1, 6, 1584, 1, 17));

		//Dutch provinces (no national date)
		register("Netherlands/Zeeland", CalendarReform.NETHERLANDS_BRABANT);
		register("Netherlands/Brabant", CalendarReform.NETHERLANDS_BRABANT);
		register("Netherlands/States General", CalendarReform.NETHERLANDS_BRABANT);
		register("Netherlands/Holland", new CalendarReform(1583, 1, 1, 1583, 1, 12));
		register("Netherlands/Gelderland", CalendarReform.NETHERLANDS_GELDERLAND);
		register("Netherlands/Overijssel", CalendarReform.NETHERLANDS_UTRECHT);
		register("Netherlands/Utrecht", CalendarReform.NETHERLANDS_UTRECHT);
		register("Netherlands/Friesland", CalendarReform.NETHERLANDS_FRISIA);
		register("Netherlands/Groningen", CalendarReform.NETHERLANDS_FRISIA);
		register("Netherlands/Drenthe", new CalendarReform(1701, 4, 30, 1701, 5, 12));

		//Belgium (no national date). The Southern Netherlands under Spanish rule: "or one day later" according to the source
		register("Belgium/Flanders", CalendarReform.NETHERLANDS_BRABANT);
		register("Belgium/Liège", new CalendarReform(1583, 2, 10, 1583, 2, 21));
		register("Belgium/Southern Netherlands", new CalendarReform(1582, 12, 20, 1582, 12, 31));

		//Switzerland (no national date). Rows without published days are not registered: Thurgau and Appenzell Innerrhoden
		//(1584), Lower Valais (1623), the Catholic parts of Graubünden (1623-1624), the city of St. Gallen (1724), and the
		//Protestant communes of Graubünden (1783-1811, except Schiers and Grüsch). Appenzell Ausserrhoden adopted the
		//Gregorian calendar in 1584, went back to the Julian one in 1597, and changed again in 1798: only the last, dated,
		//switch is registered (a single switch is all CalendarReform can express)
		register("Switzerland/Diocese of Basel", new CalendarReform(1583, 10, 20, 1583, 10, 31));
		for(final String canton : new String[]{"Luzern", "Uri", "Schwyz", "Zug", "Freiburg", "Solothurn", "Le Landeron"})
			register("Switzerland/" + canton, new CalendarReform(1584, 1, 11, 1584, 1, 22));
		register("Switzerland/Obwalden", new CalendarReform(1584, 2, 11, 1584, 2, 22));
		register("Switzerland/Nidwalden", new CalendarReform(1584, 2, 11, 1584, 2, 22));
		register("Switzerland/Valais", new CalendarReform(1655, 2, 28, 1655, 3, 11));
		register("Switzerland/Protestant", PROTESTANT_SWITZERLAND);
		for(final String canton : new String[]{"Basel", "Bern", "Neuchâtel", "Sargans", "Schaffhausen", "Geneva", "Zürich"})
			register("Switzerland/" + canton, PROTESTANT_SWITZERLAND);
		register("Switzerland/Glarus/Catholic", PROTESTANT_SWITZERLAND);
		register("Switzerland/Glarus/Protestant", new CalendarReform(1798, 6, 22, 1798, 7, 4));
		register("Switzerland/Appenzell Ausserrhoden", new CalendarReform(1798, 12, 13, 1798, 12, 25));
		register("Switzerland/Graubünden/Schiers", new CalendarReform(1811, 12, 25, 1812, 1, 7));
		register("Switzerland/Graubünden/Grüsch", new CalendarReform(1811, 12, 25, 1812, 1, 7));

		//Germany (the Catholic states adopted at different dates; only the Protestant block is a single reform)
		register("Germany/Protestant", CalendarReform.GERMANY_PROTESTANT);
		register("Germany/Bavaria", new CalendarReform(1583, 10, 5, 1583, 10, 16));
	}


	private CalendarReforms(){}

	private static void register(final String jurisdiction, final CalendarReform reform){
		REGISTRY.put(normalize(jurisdiction), reform);
		DISPLAY_NAMES.put(normalize(jurisdiction), jurisdiction);
	}

	/**
	 * Finds the reform of a jurisdiction: the exact path, or else the nearest registered ancestor.
	 *
	 * @param jurisdiction	a path such as {@code "Austria/Tyrol"} or just {@code "Austria"} (case, accents, spaces, and
	 *		underscores are ignored)
	 * @return	the most specific reform that applies, empty if none is known (for a country with several reforms, ask the user
	 *		to choose among {@link #subdivisions(String)})
	 */
	public static Optional<CalendarReform> resolve(final String jurisdiction){
		if(jurisdiction == null)
			return Optional.empty();

		String key = normalize(jurisdiction);
		while(!key.isEmpty()){
			final CalendarReform reform = REGISTRY.get(key);
			if(reform != null)
				return Optional.of(reform);

			final int separator = key.lastIndexOf('/');
			if(separator < 0)
				break;
			key = key.substring(0, separator);
		}
		return Optional.empty();
	}

	/**
	 * Finds the reform that applies to a place, given the names of the place and of the places that contain it.
	 * <p>
	 * The nearest level that names a registered jurisdiction wins, so a town in Tyrol resolves to Tyrol even if Austria is
	 * also in the chain. A jurisdiction with a path ({@code "Austria/Tyrol"}) is accepted only if the names of its outer
	 * segments also appear in the outer levels, so that a canton named "Basel" is not confused with another place of the same
	 * name. If two registered jurisdictions end with the same name at the same level, the one with the longer path wins.
	 *
	 * @param levels	the names of the place itself (all of them: primary and alternative), then of its containers, nearest
	 *		first; a level has more than one name when the place has alternative names or several containers
	 * @return	the reform, or empty if no level matches a jurisdiction that has a single reform (the caller can then ask
	 *		the user)
	 */
	public static Optional<CalendarReform> resolveByNames(final List<List<String>> levels){
		for(int level = 0; level < levels.size(); level ++){
			String bestKey = null;
			for(final String name : levels.get(level)){
				final String last = normalize(name);
				if(last.isEmpty())
					continue;

				for(final String key : REGISTRY.keySet())
					if(lastSegment(key).equals(last) && outerSegmentsPresent(key, levels, level)
							&& (bestKey == null || depth(key) > depth(bestKey)))
						bestKey = key;
			}
			if(bestKey != null)
				return Optional.of(REGISTRY.get(bestKey));
		}
		return Optional.empty();
	}

	private static boolean outerSegmentsPresent(final String key, final List<List<String>> levels, final int level){
		String outer = key;
		int separator = outer.lastIndexOf('/');
		while(separator >= 0){
			outer = outer.substring(0, separator);
			final String segment = lastSegment(outer);
			boolean found = false;
			for(int i = level + 1; i < levels.size() && !found; i ++)
				for(final String name : levels.get(i))
					if(normalize(name).equals(segment)){
						found = true;
						break;
					}
			if(!found)
				return false;

			separator = outer.lastIndexOf('/');
		}
		return true;
	}

	private static String lastSegment(final String key){
		return key.substring(key.lastIndexOf('/') + 1);
	}

	private static int depth(final String key){
		int depth = 0;
		for(int i = 0; i < key.length(); i ++)
			if(key.charAt(i) == '/')
				depth ++;
		return depth;
	}

	/**
	 * @param country	the country (or any path prefix)
	 * @return	the registered jurisdictions below the given path, in display form, in registration order
	 */
	public static List<String> subdivisions(final String country){
		final String prefix = normalize(country) + "/";
		final List<String> result = new ArrayList<>();
		for(final Map.Entry<String, String> entry : DISPLAY_NAMES.entrySet())
			if(entry.getKey().startsWith(prefix))
				result.add(entry.getValue());
		return Collections.unmodifiableList(result);
	}

	/** @return all the registered jurisdictions, in display form. */
	public static List<String> jurisdictions(){
		return List.copyOf(DISPLAY_NAMES.values());
	}

	private static String normalize(final String jurisdiction){
		final String lower = jurisdiction.toLowerCase(Locale.ROOT);
		final String decomposed = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD);
		final StringBuilder sb = new StringBuilder(decomposed.length());
		for(int i = 0; i < decomposed.length(); i ++){
			final char c = decomposed.charAt(i);
			if(c == '/' || Character.isLetterOrDigit(c))
				sb.append(c);
		}
		return sb.toString();
	}

}
