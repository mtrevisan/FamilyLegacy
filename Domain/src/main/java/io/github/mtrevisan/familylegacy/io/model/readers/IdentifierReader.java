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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Handler for IDENTIFIER structures: how an item (a source, or a copy of it in a repository) is identified.
 * <p>
 * Three things are kept apart:
 * <ul>
 *   <li>an identifier of a {@code SourceRecord} identifies the source itself (an ISBN, a DOI, a microfilm number);</li>
 *   <li>an identifier of a {@code RepositoryCitation} says where the source is found in that repository (a shelf mark, a call number, a
 *   classification number);</li>
 *   <li>a <em>locator</em> is not an identifier: it says where a piece of information is found inside the source (page, entry, folio,
 *   frame).</li>
 * </ul>
 * Structure:
 * <pre>
 * struct IdentifierStructure {
 *   scheme: enum {
 *     isbn, issn, doi, url, call_number, dewey, lcc, udc, colon, garside, fhl_film, fhl_digital_folder, fhl_ancestral_file, index_card
 *   } | Text
 *   value: Text   // Identifier value.
 *
 *   require not_blank(value)
 * }
 * </pre>
 * Whether a value is valid for its scheme is a matter of {@link #validity(String, String)}: a value that fails the check is a warning,
 * never an error, because a printed number can be wrong and an old edition can predate the check.
 */
public final class IdentifierReader{

	public static final String TYPE = "IDENTIFIER";

	public static final String TAG_IDENTIFIER = "identifier";
	public static final String TAG_SCHEME = "scheme";
	public static final String TAG_VALUE = "value";

	public static final String ENUM_SCHEME_ISBN = "isbn";
	public static final String ENUM_SCHEME_ISSN = "issn";
	public static final String ENUM_SCHEME_DOI = "doi";
	public static final String ENUM_SCHEME_URL = "url";
	public static final String ENUM_SCHEME_CALL_NUMBER = "call_number";
	public static final String ENUM_SCHEME_DEWEY = "dewey";
	public static final String ENUM_SCHEME_LCC = "lcc";
	public static final String ENUM_SCHEME_UDC = "udc";
	public static final String ENUM_SCHEME_COLON = "colon";
	public static final String ENUM_SCHEME_GARSIDE = "garside";
	public static final String ENUM_SCHEME_FHL_FILM = "fhl_film";
	public static final String ENUM_SCHEME_FHL_DIGITAL_FOLDER = "fhl_digital_folder";
	public static final String ENUM_SCHEME_FHL_ANCESTRAL_FILE = "fhl_ancestral_file";
	public static final String ENUM_SCHEME_INDEX_CARD = "index_card";
	public static final String[] SCHEMES = {
		ENUM_SCHEME_ISBN, ENUM_SCHEME_ISSN, ENUM_SCHEME_DOI, ENUM_SCHEME_URL, ENUM_SCHEME_CALL_NUMBER, ENUM_SCHEME_DEWEY, ENUM_SCHEME_LCC,
		ENUM_SCHEME_UDC, ENUM_SCHEME_COLON, ENUM_SCHEME_GARSIDE, ENUM_SCHEME_FHL_FILM, ENUM_SCHEME_FHL_DIGITAL_FOLDER,
		ENUM_SCHEME_FHL_ANCESTRAL_FILE, ENUM_SCHEME_INDEX_CARD
	};
	/** The schemes that identify a source itself, wherever it is kept (the ones offered first for a source). */
	public static final String[] SOURCE_SCHEMES = {
		ENUM_SCHEME_ISBN, ENUM_SCHEME_ISSN, ENUM_SCHEME_DOI, ENUM_SCHEME_URL, ENUM_SCHEME_FHL_FILM, ENUM_SCHEME_FHL_DIGITAL_FOLDER,
		ENUM_SCHEME_FHL_ANCESTRAL_FILE
	};
	/** The schemes that say where a source is found in a repository (the ones offered first for a repository citation). */
	public static final String[] REPOSITORY_SCHEMES = {
		ENUM_SCHEME_CALL_NUMBER, ENUM_SCHEME_DEWEY, ENUM_SCHEME_LCC, ENUM_SCHEME_UDC, ENUM_SCHEME_COLON, ENUM_SCHEME_GARSIDE,
		ENUM_SCHEME_INDEX_CARD, ENUM_SCHEME_FHL_FILM, ENUM_SCHEME_FHL_DIGITAL_FOLDER
	};

	/** The result of the check of a value against the rules of its scheme. */
	public enum Validity{
		/** The value passes the check of its scheme. */
		VALID,
		/** The value fails the check of its scheme (a wrong check digit, a malformed URL). */
		INVALID,
		/** The scheme has no check (a shelf mark, a Dewey number, a custom scheme) or the value is empty: nothing can be said. */
		UNCHECKED
	}

	/** What {@link #compact(String)} gives for a text that has characters that no ISBN or ISSN has. */
	private static final String NOT_COMPACTABLE = "?";
	private static final int ISBN10_LENGTH = 10;
	private static final int ISBN13_LENGTH = 13;
	private static final int ISSN_LENGTH = 8;

	/** A DOI: the registrant code, a slash and the suffix (ISO 26324, Crossref and DataCite). */
	private static final Pattern DOI = Pattern.compile("10\\.\\d{4,9}(?:\\.\\d+)*/\\S+");
	/** The ISSN as it is written: four digits, a hyphen, three digits and a digit or {@code X}. */
	private static final Pattern ISSN = Pattern.compile("\\d{4}-?\\d{3}[\\dXx]");
	private static final String[] DOI_PREFIXES = {"https://doi.org/", "http://doi.org/", "https://dx.doi.org/", "http://dx.doi.org/", "doi:"};

	/** The labels (lowercase, with no separators) that the files of other programs give to a scheme. */
	private static final Map<String, String> LABELS = Map.ofEntries(
		Map.entry("isbn", ENUM_SCHEME_ISBN), Map.entry("isbn10", ENUM_SCHEME_ISBN), Map.entry("isbn13", ENUM_SCHEME_ISBN),
		Map.entry("issn", ENUM_SCHEME_ISSN),
		Map.entry("doi", ENUM_SCHEME_DOI),
		Map.entry("url", ENUM_SCHEME_URL), Map.entry("uri", ENUM_SCHEME_URL), Map.entry("link", ENUM_SCHEME_URL),
		Map.entry("weblink", ENUM_SCHEME_URL), Map.entry("website", ENUM_SCHEME_URL),
		Map.entry("callnumber", ENUM_SCHEME_CALL_NUMBER), Map.entry("callno", ENUM_SCHEME_CALL_NUMBER),
		Map.entry("shelfmark", ENUM_SCHEME_CALL_NUMBER), Map.entry("shelfnumber", ENUM_SCHEME_CALL_NUMBER),
		Map.entry("caln", ENUM_SCHEME_CALL_NUMBER),
		Map.entry("dewey", ENUM_SCHEME_DEWEY), Map.entry("ddc", ENUM_SCHEME_DEWEY), Map.entry("deweydecimal", ENUM_SCHEME_DEWEY),
		Map.entry("deweydecimalclassification", ENUM_SCHEME_DEWEY),
		Map.entry("lcc", ENUM_SCHEME_LCC), Map.entry("loc", ENUM_SCHEME_LCC), Map.entry("libraryofcongressclassification", ENUM_SCHEME_LCC),
		Map.entry("udc", ENUM_SCHEME_UDC), Map.entry("universaldecimalclassification", ENUM_SCHEME_UDC),
		Map.entry("colon", ENUM_SCHEME_COLON), Map.entry("coloncclassification", ENUM_SCHEME_COLON),
		Map.entry("garside", ENUM_SCHEME_GARSIDE), Map.entry("garsideclassification", ENUM_SCHEME_GARSIDE),
		Map.entry("fhlfilm", ENUM_SCHEME_FHL_FILM), Map.entry("fhlmicrofilm", ENUM_SCHEME_FHL_FILM),
		Map.entry("fhlfilmnumber", ENUM_SCHEME_FHL_FILM), Map.entry("fhlmicrofilmnumber", ENUM_SCHEME_FHL_FILM),
		Map.entry("filmnumber", ENUM_SCHEME_FHL_FILM), Map.entry("microfilmnumber", ENUM_SCHEME_FHL_FILM),
		Map.entry("fhldigitalfolder", ENUM_SCHEME_FHL_DIGITAL_FOLDER), Map.entry("digitalfoldernumber", ENUM_SCHEME_FHL_DIGITAL_FOLDER),
		Map.entry("dgs", ENUM_SCHEME_FHL_DIGITAL_FOLDER),
		Map.entry("fhlancestralfile", ENUM_SCHEME_FHL_ANCESTRAL_FILE), Map.entry("ancestralfile", ENUM_SCHEME_FHL_ANCESTRAL_FILE),
		Map.entry("ancestralfilenumber", ENUM_SCHEME_FHL_ANCESTRAL_FILE), Map.entry("af", ENUM_SCHEME_FHL_ANCESTRAL_FILE),
		Map.entry("indexcard", ENUM_SCHEME_INDEX_CARD), Map.entry("cardcatalog", ENUM_SCHEME_INDEX_CARD),
		Map.entry("cardcatalogue", ENUM_SCHEME_INDEX_CARD)
	);


	private IdentifierReader(){}


	/** @return the scheme of an identifier (one of {@link #SCHEMES} or free text), or {@code null} if not set */
	public static String extractScheme(final FLEFRecord identifier){
		return FLEFRecordHelper.getChildValue(identifier, TAG_SCHEME);
	}

	/** @return the value of an identifier, or {@code null} if not set */
	public static String extractValue(final FLEFRecord identifier){
		return FLEFRecordHelper.getChildValue(identifier, TAG_VALUE);
	}

	/**
	 * @param parent	A record or a structure that has {@code identifier} children (a SourceRecord, a RepositoryCitation).
	 * @return	The identifiers it has, in the order of the file.
	 */
	public static List<FLEFRecord> extractIdentifiers(final FLEFRecord parent){
		return FLEFRecordHelper.findChildren(parent, TAG_IDENTIFIER);
	}

	/**
	 * @param parent	A record or a structure that has {@code identifier} children.
	 * @param scheme	The scheme to look for (one of {@link #SCHEMES}, or free text, ignoring case).
	 * @return	The values of its identifiers of that scheme, in the order of the file; the blank ones are left out.
	 */
	public static List<String> extractValues(final FLEFRecord parent, final String scheme){
		final List<String> values = new ArrayList<>(0);
		for(final FLEFRecord identifier : extractIdentifiers(parent)){
			final String value = StringUtils.trimToNull(extractValue(identifier));
			if(value != null && StringUtils.equalsIgnoreCase(StringUtils.trim(extractScheme(identifier)), scheme))
				values.add(value);
		}
		return values;
	}

	/** @return a new {@code identifier} child with the scheme and the value ({@code scheme} is written before {@code value}) */
	public static FLEFRecord create(final String scheme, final String value){
		return FLEFRecord.createChildWithTag(TAG_IDENTIFIER)
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_SCHEME, scheme))
			.addChild(FLEFRecord.createChildWithTagAndValue(TAG_VALUE, value));
	}

	/** @return the text of an identifier to show to the user: {@code scheme: value}, or just the value if it has no scheme */
	public static String displayText(final FLEFRecord identifier){
		final String scheme = StringUtils.trimToEmpty(extractScheme(identifier));
		final String value = StringUtils.trimToEmpty(extractValue(identifier));
		return (scheme.isEmpty()? value: value.isEmpty()? scheme + ":": scheme + ": " + value);
	}


	/**
	 * Reads the name that another program, or a person, gives to a kind of identifier ("ISBN", "Call number", "Shelf mark", "FHL film
	 * number", "DDC", ...).
	 *
	 * @param label	The name.
	 * @return	The scheme of the vocabulary (one of {@link #SCHEMES}), or {@code null} if the name is not one that is known: the caller
	 * 	keeps then the name itself as a custom scheme, which loses nothing.
	 */
	public static String schemeOfLabel(final String label){
		if(StringUtils.isBlank(label))
			return null;

		final StringBuilder key = new StringBuilder(label.length());
		for(int i = 0; i < label.length(); i ++){
			final char c = label.charAt(i);
			if(Character.isLetterOrDigit(c))
				key.append(Character.toLowerCase(c));
		}
		return LABELS.get(key.toString());
	}

	/**
	 * Guesses the scheme of a value from how it is written: an ISBN (with a correct check digit), an ISSN (with a correct check digit), a
	 * DOI, a URL. A value that could be many things (a number) has no scheme: the check digit is what tells an ISBN from any other
	 * string of digits.
	 *
	 * @param value	The value.
	 * @return	The scheme (one of {@link #SCHEMES}), or {@code null} if the value is not recognized.
	 */
	public static String inferScheme(final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(text.isEmpty())
			return null;

		final String lower = text.toLowerCase(Locale.ROOT);
		if(lower.startsWith("doi:") || isDoi(text))
			return ENUM_SCHEME_DOI;
		if(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("ftp://") || lower.startsWith("urn:"))
			return ENUM_SCHEME_URL;
		if(isIsbn(text))
			return ENUM_SCHEME_ISBN;
		if(isIssn(text))
			return ENUM_SCHEME_ISSN;
		return null;
	}


	/**
	 * Checks a value against the rules of its scheme: the check digit of an ISBN or an ISSN, the syntax of a URL (RFC 3986). The other
	 * schemes (and the custom ones) have no rule.
	 *
	 * @param scheme	The scheme (ignoring case); {@code null} or blank means a scheme that is not known.
	 * @param value	The value.
	 * @return	Whether the value is valid, not valid, or cannot be judged.
	 */
	public static Validity validity(final String scheme, final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(text.isEmpty())
			return Validity.UNCHECKED;

		final String key = StringUtils.trimToEmpty(scheme).toLowerCase(Locale.ROOT);
		return switch(key){
			case ENUM_SCHEME_ISBN -> (isIsbn(text)? Validity.VALID: Validity.INVALID);
			case ENUM_SCHEME_ISSN -> (isIssn(text)? Validity.VALID: Validity.INVALID);
			case ENUM_SCHEME_URL -> (isUrl(text)? Validity.VALID: Validity.INVALID);
			default -> Validity.UNCHECKED;
		};
	}

	/**
	 * @param value	The text, with or without hyphens and spaces ("0-306-40615-2", "978 0 306 40615 7").
	 * @return	Whether it is an ISBN-10 or an ISBN-13 whose check digit is right.
	 */
	public static boolean isIsbn(final String value){
		final String digits = compact(value);
		if(digits.length() == ISBN10_LENGTH)
			return isIsbn10(digits);
		return (digits.length() == ISBN13_LENGTH && isIsbn13(digits));
	}

	private static boolean isIsbn10(final String digits){
		int sum = 0;
		for(int i = 0; i < ISBN10_LENGTH; i ++){
			final char c = digits.charAt(i);
			final int digit;
			if(c == 'X' && i == ISBN10_LENGTH - 1)
				digit = 10;
			else if(c >= '0' && c <= '9')
				digit = c - '0';
			else
				return false;
			sum += (ISBN10_LENGTH - i) * digit;
		}
		return (sum % 11 == 0);
	}

	private static boolean isIsbn13(final String digits){
		int sum = 0;
		for(int i = 0; i < ISBN13_LENGTH; i ++){
			final char c = digits.charAt(i);
			if(c < '0' || c > '9')
				return false;

			sum += (c - '0') * (i % 2 == 0? 1: 3);
		}
		return (sum % 10 == 0);
	}

	/**
	 * @param value	The text, with or without the hyphen ("0378-5955").
	 * @return	Whether it is an ISSN whose check digit is right.
	 */
	public static boolean isIssn(final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(!ISSN.matcher(text).matches())
			return false;

		final String digits = compact(text);
		if(digits.length() != ISSN_LENGTH)
			return false;

		int sum = 0;
		for(int i = 0; i < ISSN_LENGTH; i ++){
			final char c = digits.charAt(i);
			final int digit = (c == 'X'? 10: c - '0');
			sum += (ISSN_LENGTH - i) * digit;
		}
		return (sum % 11 == 0);
	}

	/** @return whether the text is a DOI, with or without the {@code doi:} or {@code https://doi.org/} prefix */
	public static boolean isDoi(final String value){
		return DOI.matcher(withoutDoiPrefix(StringUtils.trimToEmpty(value))).matches();
	}

	/**
	 * @param value	The text.
	 * @return	Whether it is a URI according to RFC 3986: it has a scheme, no space and no character that the syntax does not allow.
	 */
	public static boolean isUrl(final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(text.isEmpty() || StringUtils.containsWhitespace(text))
			return false;

		try{
			final URI uri = new URI(text);
			return (uri.isAbsolute() && StringUtils.isNotEmpty(uri.getScheme())
				&& (uri.getRawSchemeSpecificPart() != null && !uri.getRawSchemeSpecificPart().isEmpty()));
		}
		catch(final URISyntaxException ignored){
			return false;
		}
	}


	/**
	 * A key that is the same for two writings of the same identifier ("ISBN 0-306-40615-2" and "0306406152"; "doi:10.1000/182" and
	 * "https://doi.org/10.1000/182"): the merge and the duplicate checks compare the identifiers with it.
	 *
	 * @param scheme	The scheme of the identifier.
	 * @param value	The value of the identifier.
	 * @return	The key, {@code scheme|normalized value} in lowercase; empty if there is no value.
	 */
	public static String normalizedKey(final String scheme, final String value){
		final String text = StringUtils.trimToEmpty(value);
		if(text.isEmpty())
			return StringUtils.EMPTY;

		final String key = StringUtils.trimToEmpty(scheme).toLowerCase(Locale.ROOT);
		final String normalized = switch(key){
			case ENUM_SCHEME_ISBN -> isbnKey(text);
			case ENUM_SCHEME_ISSN -> (NOT_COMPACTABLE.equals(compact(text))? text.toLowerCase(Locale.ROOT): compact(text));
			case ENUM_SCHEME_DOI -> withoutDoiPrefix(text).toLowerCase(Locale.ROOT);
			case ENUM_SCHEME_URL -> StringUtils.removeEnd(text.toLowerCase(Locale.ROOT), "/");
			default -> StringUtils.normalizeSpace(text).toLowerCase(Locale.ROOT);
		};
		return key + "|" + normalized;
	}

	/**
	 * @return the ISBN-13 that the text stands for: an ISBN-10 with a right check digit is the ISBN-13 that begins with {@code 978} (the
	 * same book, written in the old way); what is not an ISBN is kept as it is, so that two different garbled texts do not collide
	 */
	private static String isbnKey(final String text){
		final String digits = compact(text);
		if(NOT_COMPACTABLE.equals(digits))
			return text.toLowerCase(Locale.ROOT);
		if(digits.length() != ISBN10_LENGTH || !isIsbn10(digits))
			return digits;

		final String body = "978" + digits.substring(0, ISBN10_LENGTH - 1);
		int sum = 0;
		for(int i = 0; i < body.length(); i ++)
			sum += (body.charAt(i) - '0') * (i % 2 == 0? 1: 3);
		return body + (10 - sum % 10) % 10;
	}

	/** @return the text without a leading {@code ISBN}/{@code ISSN} label and its colon */
	private static String withoutLabel(final String text){
		final String lower = text.toLowerCase(Locale.ROOT);
		for(final String label : new String[]{"isbn-13", "isbn-10", "isbn", "issn"})
			if(lower.startsWith(label))
				return StringUtils.stripStart(text.substring(label.length()), ": ");
		return text;
	}

	private static String withoutDoiPrefix(final String text){
		final String lower = text.toLowerCase(Locale.ROOT);
		for(final String prefix : DOI_PREFIXES)
			if(lower.startsWith(prefix))
				return text.substring(prefix.length()).trim();
		return text;
	}

	/** @return the digits (and the letter {@code X}, upper case) of the text, with the hyphens, the spaces and any label dropped */
	private static String compact(final String text){
		final String source = withoutLabel(StringUtils.trimToEmpty(text));
		final StringBuilder sb = new StringBuilder(source.length());
		for(int i = 0; i < source.length(); i ++){
			final char c = source.charAt(i);
			if(c >= '0' && c <= '9' || c == 'X' || c == 'x')
				sb.append(Character.toUpperCase(c));
			else if(c != '-' && c != ' ' && c != '‐' && c != '‑' && c != '–')
				return NOT_COMPACTABLE;
		}
		return sb.toString();
	}

}
