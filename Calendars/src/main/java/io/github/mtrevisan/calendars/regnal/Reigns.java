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
package io.github.mtrevisan.calendars.regnal;

import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The reigns and pontificates used to date the documents.
 */
public final class Reigns{

	private static final String POPES_RESOURCE = "popes.csv";

	/** Variant spellings of the Latin, Italian, and English names of the popes. */
	private static final Map<String, List<String>> NAME_VARIANTS = Map.ofEntries(
		Map.entry("ioannes", List.of("johannes", "joannes", "iohannes")),
		Map.entry("iulius", List.of("julius")),
		Map.entry("hadrianus", List.of("adrianus")),
		Map.entry("caelestinus", List.of("coelestinus", "celestinus")),
		Map.entry("callistus", List.of("calixtus", "callixtus")),
		Map.entry("nicolaus", List.of("nicholaus")),
		Map.entry("bonifacius", List.of("bonifatius")),
		Map.entry("niccolo", List.of("nicolo", "nicola")),
		Map.entry("callixtus", List.of("calixtus", "callistus")),
		Map.entry("eugene", List.of("eugenius")),
		Map.entry("giovanni paolo", List.of("giovanpaolo")));

	/** Latin genitives that do not follow the "-us" -> "-i" and "-es" -> "-is" rules. */
	private static final Map<String, String> IRREGULAR_GENITIVES = Map.of("clemens", "clementis", "leo", "leonis",
		"alexander", "alexandri", "felix", "felicis");

	/**
	 * The kings and queens of England, of Great Britain (1707), and of the United Kingdom (1801), with the day from which
	 * their regnal years are counted (Julian before 14 September 1752), after the tables of C. R. Cheney, "A Handbook of
	 * Dates for Students of British History".
	 */
	public static final List<Reign> ENGLISH = List.of(
		monarch("william_i", 1, "1066-12-25", "1087-09-09", "William", "Will", "Wm", "Willelmi", "Gulielmi", "Guglielmo"),
		monarch("william_ii", 2, "1087-09-26", "1100-08-02", "William", "Will", "Wm", "Willelmi", "Gulielmi", "Guglielmo"),
		monarch("henry_i", 1, "1100-08-05", "1135-12-01", "Henry", "Hen", "Henrici", "Enrico"),
		// the date of the coronation is disputed (22 or 26 December): Cheney gives 22 December
		monarch("stephen", 0, "1135-12-22", "1154-10-25", "Stephen", "Steph", "Stephani", "Stefano"),
		monarch("henry_ii", 2, "1154-12-19", "1189-07-06", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("richard_i", 1, "1189-09-03", "1199-04-06", "Richard", "Ric", "Ricardi", "Riccardo"),
		new Reign("john", names("John", "Joh", "Johannis", "Iohannis", "Giovanni"), 0, date("1199-05-27"), date("1199-05-27"),
			date("1216-10-19"), Reign.YearRule.ASCENSION),
		monarch("henry_iii", 3, "1216-10-28", "1272-11-16", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("edward_i", 1, "1272-11-20", "1307-07-07", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("edward_ii", 2, "1307-07-08", "1327-01-20", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("edward_iii", 3, "1327-01-25", "1377-06-21", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("richard_ii", 2, "1377-06-22", "1399-09-29", "Richard", "Ric", "Ricardi", "Riccardo"),
		monarch("henry_iv", 4, "1399-09-30", "1413-03-20", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("henry_v", 5, "1413-03-21", "1422-08-31", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("henry_vi", 6, "1422-09-01", "1461-03-04", "Henry", "Hen", "Henrici", "Enrico"),
		// the readeption of Henry VI was dated in his 49th year
		new Reign("henry_vi_readeption", names("Henry", "Hen", "Henrici", "Enrico"), 6, date("1422-09-01"),
			date("1470-10-09"), date("1471-04-11"), Reign.YearRule.ANNIVERSARY),
		monarch("edward_iv", 4, "1461-03-04", "1483-04-09", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("edward_v", 5, "1483-04-09", "1483-06-25", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("richard_iii", 3, "1483-06-26", "1485-08-22", "Richard", "Ric", "Ricardi", "Riccardo"),
		monarch("henry_vii", 7, "1485-08-22", "1509-04-21", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("henry_viii", 8, "1509-04-22", "1547-01-28", "Henry", "Hen", "Henrici", "Enrico"),
		monarch("edward_vi", 6, "1547-01-28", "1553-07-06", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("jane", 0, "1553-07-06", "1553-07-19", "Jane", "Janae", "Giovanna"),
		// the first year of Mary I starts on 19 July 1553, after Jane, the following ones on 6 July, ignoring Jane
		new Reign("mary_i", names("Mary", "Mariae", "Maria"), 1, date("1553-07-06"), date("1553-07-19"), date("1554-07-24"),
			Reign.YearRule.ANNIVERSARY),
		// "1 & 2 Philip and Mary": Philip's year first, Mary's (counted from 6 July 1553) second
		new Reign("philip_and_mary", names("Philip and Mary", "Phil and Mary", "Philippi et Mariae", "Filippo e Maria"), 0,
			date("1553-07-06"), date("1554-07-25"), date("1558-11-17"), Reign.YearRule.DOUBLE_ANNIVERSARY),
		monarch("elizabeth_i", 1, "1558-11-17", "1603-03-24", "Elizabeth", "Eliz", "Elizabethae", "Elisabetta"),
		monarch("james_i", 1, "1603-03-24", "1625-03-27", "James", "Jac", "Jacobi", "Iacobi", "Giacomo"),
		monarch("charles_i", 1, "1625-03-27", "1649-01-30", "Charles", "Car", "Caroli", "Carlo"),
		// the regnal years of Charles II were counted from the death of his father
		monarch("charles_ii", 2, "1649-01-30", "1685-02-06", "Charles", "Car", "Caroli", "Carlo"),
		monarch("james_ii", 2, "1685-02-06", "1688-12-11", "James", "Jac", "Jacobi", "Iacobi", "Giacomo"),
		// after the death of Mary II (28 December 1694) William III continued the same count from 13 February, as in
		// Cheney (other tables restart the years of William III on 28 December)
		new Reign("william_and_mary", names("William and Mary", "Will and Mary", "W and M", "Gulielmi et Mariae",
			"Guglielmo e Maria"), 0, date("1689-02-13"), date("1689-02-13"), date("1702-03-08"), Reign.YearRule.ANNIVERSARY),
		monarch("william_iii", 3, "1689-02-13", "1702-03-08", "William", "Will", "Wm", "Gulielmi", "Guglielmo"),
		monarch("anne", 0, "1702-03-08", "1714-08-01", "Anne", "Annae", "Anna"),
		monarch("george_i", 1, "1714-08-01", "1727-06-11", "George", "Geo", "Georgii", "Giorgio"),
		monarch("george_ii", 2, "1727-06-11", "1760-10-25", "George", "Geo", "Georgii", "Giorgio"),
		monarch("george_iii", 3, "1760-10-25", "1820-01-29", "George", "Geo", "Georgii", "Giorgio"),
		monarch("george_iv", 4, "1820-01-29", "1830-06-26", "George", "Geo", "Georgii", "Giorgio"),
		monarch("william_iv", 4, "1830-06-26", "1837-06-20", "William", "Will", "Wm", "Gulielmi", "Guglielmo"),
		monarch("victoria", 0, "1837-06-20", "1901-01-22", "Victoria", "Vict", "Vic", "Victoriae", "Vittoria"),
		monarch("edward_vii", 7, "1901-01-22", "1910-05-06", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("george_v", 5, "1910-05-06", "1936-01-20", "George", "Geo", "Georgii", "Giorgio"),
		monarch("edward_viii", 8, "1936-01-20", "1936-12-11", "Edward", "Edw", "Edwardi", "Edoardo"),
		monarch("george_vi", 6, "1936-12-11", "1952-02-06", "George", "Geo", "Georgii", "Giorgio"),
		monarch("elizabeth_ii", 2, "1952-02-06", "2022-09-08", "Elizabeth", "Eliz", "Elizabethae", "Elisabetta"),
		monarch("charles_iii", 3, "2022-09-08", null, "Charles", "Car", "Caroli", "Carlo")
	);

	/**
	 * The popes from Innocent III (1198) to Leo XIV, and the antipopes of the Western Schism and Felix V, with the day
	 * from which their pontifical years are counted (coronation, or consecration, or inauguration; the election for the
	 * three popes never crowned), Julian before 15 October 1582.
	 */
	public static final List<Reign> POPES = loadPopes();


	private Reigns(){}


	private static Reign monarch(final String id, final int ordinal, final String start, final String end,
			final String... names){
		return new Reign(id, names(names), ordinal, date(start), date(start), (end != null? date(end): null),
			Reign.YearRule.ANNIVERSARY);
	}

	private static List<String> names(final String... names){
		final Set<String> normalized = new LinkedHashSet<>();
		for(final String name : names)
			normalized.add(TextNormalizer.normalize(name));
		return List.copyOf(normalized);
	}

	private static Reign.CivilDate date(final String iso){
		return Reign.CivilDate.parse(iso);
	}

	private static List<Reign> loadPopes(){
		final List<Reign> popes = new ArrayList<>();
		try(final InputStream is = Reigns.class.getResourceAsStream(POPES_RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + POPES_RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			boolean header = true;
			while((line = reader.readLine()) != null){
				if(line.isBlank() || line.startsWith("#"))
					continue;
				if(header){
					header = false;
					continue;
				}

				// number;name_it;name_la;name_en;ordinal;antipope;election;start;start_kind;end;source
				final String[] fields = line.split(";", -1);
				final int ordinal = Integer.parseInt(fields[4]);
				final Set<String> names = new LinkedHashSet<>();
				for(final String name : new String[]{fields[1], fields[2], fields[3]})
					addPopeNames(names, stripOrdinal(TextNormalizer.normalize(name)));
				final Reign.CivilDate election = date(fields[6]);
				final Reign.CivilDate start = (StringUtils.isNotBlank(fields[7])? date(fields[7]): election);
				final Reign.CivilDate end = (StringUtils.isNotBlank(fields[9])? date(fields[9]): null);
				final String id = (Boolean.parseBoolean(fields[5])? "antipope_": "pope_") + fields[3].toLowerCase()
					.replace(' ', '_');
				popes.add(new Reign(id, List.copyOf(names), ordinal, start, election, end, Reign.YearRule.ANNIVERSARY));
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
		return List.copyOf(popes);
	}

	/** Removes the trailing Roman numeral of a regnal name (e.g. "pius v" -> "pius"). */
	private static String stripOrdinal(final String name){
		final String[] words = StringUtils.split(name);
		if(words.length > 1 && words[words.length - 1].matches("[ivxlc]+"))
			return String.join(" ", Arrays.copyOf(words, words.length - 1));
		return name;
	}

	private static void addPopeNames(final Set<String> names, final String name){
		final List<String> forms = new ArrayList<>();
		forms.add(name);
		forms.addAll(NAME_VARIANTS.getOrDefault(name, List.of()));
		// variants of the single words (e.g. "ioannes paulus" -> "johannes paulus")
		final String[] words = StringUtils.split(name);
		if(words.length > 1)
			for(final Map.Entry<String, List<String>> variant : NAME_VARIANTS.entrySet())
				if(name.contains(variant.getKey()))
					for(final String replacement : variant.getValue())
						forms.add(name.replace(variant.getKey(), replacement));

		for(final String form : forms){
			names.add(form);
			// the Latin genitive (e.g. "pontificatus Pii", "Clementis", "Ioannis Pauli")
			final String[] formWords = StringUtils.split(form);
			final StringBuilder genitive = new StringBuilder();
			for(final String word : formWords){
				if(!genitive.isEmpty())
					genitive.append(' ');
				genitive.append(genitive(word));
			}
			names.add(genitive.toString());
		}
	}

	private static String genitive(final String word){
		final String irregular = IRREGULAR_GENITIVES.get(word);
		if(irregular != null)
			return irregular;
		if(word.endsWith("us"))
			return word.substring(0, word.length() - 2) + "i";
		if(word.endsWith("es"))
			return word.substring(0, word.length() - 2) + "is";
		return word;
	}

}
