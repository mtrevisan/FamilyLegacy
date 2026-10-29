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
package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Reads an archival reference as it is written in a book, in a finding aid or on a label (for example "vol. 3, p. 45, n. 12", "FHL
 * film 1234567, frame 245", "ASVe, busta 123, fol. 12v", "Registro dei nati 1850, atto 12/1850") and puts it in the form the locator of a
 * citation has: label and value pairs ("Volume: 3, Page: 45, Entry: 12").
 * <p>
 * It understands the labels in Italian, English, Latin, French, German and Spanish, in the abbreviations they are written with (vol.,
 * p., pp., fol., f., c., n., no., busta, fasc., ...). It is deliberately cautious: it does not guess. A word it does not know, or a number
 * without a label, is not put in the locator but given back as not understood, so that the researcher decides. What it does is only
 * rewriting; whether the reference is right is for the researcher to check against the source.
 * </p>
 */
final class ArchivalReferenceParser{

	private static final Pattern LEADING_CONNECTIVE = Pattern.compile("^(?i:e|and|et|und|y)\\s+");
	private static final Pattern DASH_SPACED = Pattern.compile("\\s*-\\s*");
	private static final Pattern DASH_LONG = Pattern.compile("\\s*[–—]\\s*");
	private static final Pattern LEADING_ZEROS = Pattern.compile("(?<![\\d])0+(?=\\d)");
	private static final Pattern VERSO_MARK = Pattern.compile("\\s*(?:verso)\\b");
	private static final Pattern RECTO_MARK = Pattern.compile("\\s*(?:recto)\\b");
	/** A label of the locator and its value. */
	record Part(String label, String value){

		@Override
		public String toString(){
			return label + ": " + value;
		}

	}

	/**
	 * What was read.
	 *
	 * @param parts        the labels and values, in the order of the locator (from the general to the particular)
	 * @param unrecognized what was not understood, in the order it was written (an archive, a series, a word)
	 * @param notes        what the researcher should know about how it was read (an ambiguous abbreviation, a label written twice)
	 */
	record Result(List<Part> parts, List<String> unrecognized, List<String> notes){

		/** @return the locator: the parts as "Label: value", separated by commas (empty if nothing was understood) */
		String locator(){
			final StringBuilder sb = new StringBuilder();
			for(final Part part : parts){
				if(!sb.isEmpty())
					sb.append(", ");
				sb.append(part);
			}
			return sb.toString();
		}

	}


	private enum Kind{
		/** A number, a range, a number with a letter (12v), a number and a year (12/1850). */
		NUMBER,
		/** Free text up to the next comma. */
		TEXT,
		/** A year, or a range of years. */
		YEAR
	}

	private record Spec(String label, Kind kind, boolean keepKeyword, String... words){}

	/** The labels, in the order they have in the locator. */
	private static final List<Spec> SPECS = List.of(
		new Spec("Film", Kind.NUMBER, false, "film", "microfilm", "rullino", "rotolo", "roll", "reel", "dgs", "mf"),
		new Spec("Shelf Mark", Kind.TEXT, false, "segnatura", "collocazione", "shelfmark", "shelf mark", "signature", "cote", "signatur",
			"call number", "callnumber", "inventario", "inv"),
		new Spec("Shelf Mark", Kind.TEXT, true, "ms", "mss", "cod", "codex"),
		new Spec("Box", Kind.NUMBER, false, "busta", "buste", "box", "scatola", "faldone", "mazzo", "cassa", "carton", "caja"),
		new Spec("File", Kind.NUMBER, false, "fascicolo", "fasc", "filza", "cartella", "dossier", "file", "fascicule", "legajo"),
		new Spec("Register", Kind.TEXT, false, "registro", "registri", "reg", "register", "registre", "registers"),
		new Spec("Year", Kind.YEAR, false, "anno", "year", "jahr", "année", "annee"),
		new Spec("Volume", Kind.NUMBER, false, "vol", "vols", "volume", "volumen", "volumes", "tomo", "tome", "tom", "libro", "band",
			"bd"),
		new Spec("Item", Kind.NUMBER, false, "item", "pezzo", "pezzi", "unit", "unità"),
		new Spec("Page", Kind.NUMBER, false, "p", "pp", "pg", "pag", "pagg", "pagina", "pagine", "page", "pages", "seite", "seiten",
			"pág", "página"),
		new Spec("Folio", Kind.NUMBER, false, "fol", "fols", "folio", "folios", "foglio", "f", "c", "cc", "carta", "carte", "blatt",
			"bl", "feuillet", "hoja"),
		new Spec("Frame", Kind.NUMBER, false, "frame", "fotogramma", "fotogrammi", "image", "images", "img", "immagine", "immagini",
			"imm", "foto", "bild", "vue"),
		new Spec("Entry", Kind.NUMBER, false, "n", "nr", "no", "nos", "num", "numero", "número", "number", "atto", "atti", "entry",
			"acte", "akte", "eintrag"),
		new Spec("Line", Kind.NUMBER, false, "line", "lines", "riga", "rigo", "ligne", "zeile", "linea")
	);

	/** The abbreviations that are written with one letter: they need a dot or a colon, or a number right after them. */
	private static final int SINGLE_LETTER = 1;

	private static final String NUMBER_UNIT = "\\d+(?:\\s*(?:recto|verso|bis|ter)\\b|\\s?[a-zA-Z](?![\\p{L}]))?";
	private static final Pattern NUMBER = Pattern.compile("^(" + NUMBER_UNIT + "(?:\\s*[-–—/]\\s*" + NUMBER_UNIT + ")?)");
	private static final Pattern YEAR_VALUE = Pattern.compile("^((?:1\\d{3}|20\\d{2})(?:\\s*[-–—/]\\s*(?:1\\d{3}|20\\d{2}))?)\\b");
	private static final Pattern BARE_YEAR = Pattern.compile("^(?:1\\d{3}|20\\d{2})(?:\\s*[-–—/]\\s*(?:1\\d{3}|20\\d{2}))?$");
	private static final Pattern LEADING_PARTICLE = Pattern.compile("^(?:dei|degli|delle|dello|della|del|di|of|des|der|de|the|d')\\s+",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern SEPARATORS = Pattern.compile("^[\\s,;:.]+|[\\s,;:]+$");
	private static final Pattern CIRCA_YEAR = Pattern.compile("^(?:1\\d{3}|20\\d{2})$");

	private record Keyword(int start, int end, Spec spec, String word){}


	private ArchivalReferenceParser(){}


	/**
	 * @param text the reference as it is written
	 * @return what was understood of it
	 */
	static Result parse(final String text){
		final List<String> notes = new ArrayList<>();
		final Set<String> unrecognized = new LinkedHashSet<>();
		final List<Part> found = new ArrayList<>();
		if(text == null || text.isBlank())
			return new Result(List.of(), List.of(), List.of());

		final String input = StringUtils.normalizeSpace(text);
		final List<Keyword> keywords = keywords(input);
		leftover(input.substring(0, (keywords.isEmpty()? input.length(): keywords.getFirst().start())), unrecognized, found);

		for(int i = 0; i < keywords.size(); i ++){
			final Keyword keyword = keywords.get(i);
			final int end = (i + 1 < keywords.size()? keywords.get(i + 1).start(): input.length());
			read(keyword, input.substring(keyword.end(), end), found, unrecognized, notes);
		}

		final List<Part> parts = merge(found, notes);
		return new Result(parts, List.copyOf(unrecognized), List.copyOf(notes));
	}


	/** The labels in the text, in the order they are written; where two overlap the longest one wins. */
	private static List<Keyword> keywords(final String input){
		final List<Keyword> all = new ArrayList<>();
		for(final Spec spec : SPECS)
			for(final String word : spec.words()){
				final String escaped = Pattern.quote(word).replace(StringUtils.SPACE, "\\E\\s*\\Q");
				final String pattern = (word.length() == SINGLE_LETTER
					? "(?<![\\p{L}\\p{N}])" + escaped + "(?:\\s*[.:]\\s*|\\s*(?=\\d))"
					: "(?<![\\p{L}\\p{N}])" + escaped + "(?![\\p{L}])\\s*[.:]?\\s*");
				final Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(input);
				while(matcher.find())
					all.add(new Keyword(matcher.start(), matcher.end(), spec, word));
			}

		all.sort(Comparator.comparingInt(Keyword::start).thenComparing(k -> -k.end()));
		final List<Keyword> keywords = new ArrayList<>();
		int last = -1;
		for(final Keyword keyword : all)
			if(keyword.start() >= last){
				keywords.add(keyword);
				last = keyword.end();
			}
		return keywords;
	}

	/** What comes before the first label: an archive, a series, a year. */
	private static void leftover(final String text, final Set<String> unrecognized, final List<Part> found){
		for(final String segment : StringUtils.split(text, ",;")){
			final String clean = clean(segment);
			if(clean.isEmpty())
				continue;

			if(BARE_YEAR.matcher(clean).matches())
				found.add(new Part("Year", normalizeRange(clean)));
			else
				unrecognized.add(clean);
		}
	}

	/** Reads the value that follows a label. */
	private static void read(final Keyword keyword, final String raw, final List<Part> found, final Set<String> unrecognized,
			final List<String> notes){
		final String value = clean(raw);
		final String label = keyword.spec().label();
		switch(keyword.spec().kind()){
			case TEXT -> {
				final int cut = StringUtils.indexOfAny(value, ",;");
				final String first = (cut >= 0? value.substring(0, cut): value);
				final String rest = (cut >= 0? value.substring(cut + 1): null);
				String own = LEADING_PARTICLE.matcher(clean(first)).replaceFirst("");
				if(keyword.spec().keepKeyword())
					own = keyword.word().substring(0, 1).toUpperCase(Locale.ROOT) + keyword.word().substring(1) + ". " + own;
				if(clean(first).isEmpty())
					notes.add(I18Nless.noValue(label));
				else
					found.add(new Part(label, own));
				if(rest != null)
					leftover(rest, unrecognized, found);
			}
			case YEAR -> {
				final Matcher matcher = YEAR_VALUE.matcher(value);
				if(matcher.find()){
					found.add(new Part(label, normalizeRange(matcher.group(1))));
					leftover(value.substring(matcher.end()), unrecognized, found);
				}
				else
					unrecognized.add(clean(keyword.word() + StringUtils.SPACE + value));
			}
			case NUMBER -> {
				final Matcher matcher = NUMBER.matcher(value);
				if(!matcher.find()){
					if(!value.isEmpty())
						unrecognized.add(clean(keyword.word() + StringUtils.SPACE + value));
					return;
				}

				final String number = matcher.group(1);
				//"c. 1850" is the year, circa, not the folio 1850
				if("c".equalsIgnoreCase(keyword.word()) && CIRCA_YEAR.matcher(number).matches()){
					notes.add(I18Nless.circa(number));
					unrecognized.add(clean(keyword.word() + ". " + number));
				}
				else{
					found.add(new Part(label, normalize(label, number)));
					if(keyword.word().length() == SINGLE_LETTER && "cf".contains(keyword.word().toLowerCase(Locale.ROOT)))
						notes.add(I18Nless.abbreviation(keyword.word(), label));
				}
				for(final String rest : StringUtils.split(value.substring(matcher.end()), ",;"))
					leftover(stripConnective(rest), unrecognized, found);
			}
		}
	}

	/** "12 verso" is "12v", "045" is "45" (not for a film: its number is a code), "12 – 13" is "12-13". */
	private static String normalize(final String label, final String number){
		String value = StringUtils.deleteWhitespace(VERSO_MARK.matcher(RECTO_MARK.matcher(number).replaceAll("r")).replaceAll("v"));
		value = normalizeRange(value);
		if(!"Film".equals(label) && !"Box".equals(label) && !"File".equals(label))
			value = LEADING_ZEROS.matcher(value).replaceAll(StringUtils.EMPTY);
		return value;
	}

	private static String normalizeRange(final String value){
		return DASH_SPACED.matcher(DASH_LONG.matcher(value).replaceAll("-")).replaceAll("-").strip();
	}

	private static String stripConnective(final String text){
		return LEADING_CONNECTIVE.matcher(clean(text)).replaceFirst(StringUtils.EMPTY);
	}

	private static String clean(final String text){
		return SEPARATORS.matcher(text.strip()).replaceAll("").strip();
	}

	/** Puts the parts in the order of the locator and keeps the first of the labels that are written twice. */
	private static List<Part> merge(final List<Part> found, final List<String> notes){
		final List<Part> parts = new ArrayList<>();
		final Set<String> labels = new LinkedHashSet<>();
		for(final Part part : found)
			if(labels.add(part.label()))
				parts.add(part);
			else
				notes.add(I18Nless.twice(part.label(), part.value()));

		parts.sort(Comparator.comparingInt(part -> order(part.label())));
		return parts;
	}

	private static int order(final String label){
		for(int i = 0; i < SPECS.size(); i ++)
			if(SPECS.get(i).label().equals(label))
				return i;
		return SPECS.size();
	}


	/** The notes, as text that does not need the application's translations (they are explanations for the researcher). */
	private static final class I18Nless{

		private I18Nless(){}

		static String noValue(final String label){
			return label + ": no value after the label";
		}

		static String circa(final String year){
			return "\"c. " + year + "\" is read as circa " + year + ", not as the folio " + year;
		}

		static String abbreviation(final String word, final String label){
			return "\"" + word + ".\" is read as " + label.toLowerCase(Locale.ROOT) + (("c".equalsIgnoreCase(word))
				? " (carta): it can also be \"circa\""
				: " (foglio, folio): it can also be a file or a following");
		}

		static String twice(final String label, final String value){
			return label + " is written twice: the first is kept, \"" + value + "\" is left out";
		}

	}

}
