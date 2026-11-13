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
package io.github.mtrevisan.familylegacy.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ContactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Pure static helpers shared by section builders. No model state, no
 * dependencies on the report configuration.
 */
public final class ReportFormatters{

	private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
	private static final Pattern HTML_P_OPEN = Pattern.compile("(?i)<p[^>]*>");
	private static final Pattern HTML_P_CLOSE = Pattern.compile("(?i)</p>");
	private static final Pattern HTML_BR = Pattern.compile("(?i)<br\\s*/?>");
	private static final String TAG_PLACE = EventReader.TAG_PLACE;
	private static final String TAG_SOURCE = SourceCitationReader.TAG_SOURCE;


	private ReportFormatters(){
	}


	/* ----- Basic escaping / null-safety ------------------------------------ */

	/** Null-safe identity (used before markdown escaping). */
	public static String esc(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}

	/** Null-safe identity (used for values already markdown-safe). */
	public static String escape(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}

	/** @return	The first non-blank value among the children of the record with the tag (a tag that may be repeated), {@code null} if none. */
	public static String firstValue(final FLEFRecord record, final String tag){
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag))
			if(child.getValue() != null && !child.getValue().isBlank())
				return child.getValue();
		return null;
	}

	/** Null-safe identity for values that may legitimately be empty. */
	public static String orEmpty(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}


	/* ----- Names ----------------------------------------------------------- */

	/**
	 * Concatenates the ordered {@code part} values of a {@code name} structure
	 * into a single display string, preserving the original part order.
	 */
	public static String buildName(final FLEFRecord nameRec){
		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord child : nameRec.getChildren()){
			if(!NameReader.TAG_PART.equalsIgnoreCase(child.getTag()))
				continue;
			final String v = FLEFRecordHelper.getChildValue(child, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank()){
				if(!sb.isEmpty())
					sb.append(' ');
				sb.append(v.trim());
			}
		}
		return sb.toString();
	}


	/* ----- Places ---------------------------------------------------------- */

	/** Returns the display name of a {@code place} record, or null. */
	public static String placeName(final FLEFRecord place){
		for(final FLEFRecord n : FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME)){
			final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank())
				return v;
		}
		return null;
	}

	/** Resolves the display name of a {@code place.place} reference, or null. */
	public static String resolvePlaceName(final FLEFModel model, final FLEFRecord rec){
		final String ref = FLEFRecordHelper.getChildValue(rec, FLEFRecordHelper.composePath(TAG_PLACE, PlaceReader.TYPE));
		if(ref == null)
			return null;

		final FLEFRecord place = model.getRecordById(ref);
		return (place != null? placeName(place): null);
	}

	/* ----- Name variants --------------------------------------------------- */

	/**
	 * Renders a {@code TextValueVariant} (oneof {@code phonetic} or
	 * {@code transcription}) as a single human-readable string.
	 *
	 * <p>Format:</p>
	 * <ul>
	 *   <li>{@code [IPA] tarˈviːzo}</li>
	 *   <li>{@code [rōmaji → romanized] Tokyo}</li>
	 *   <li>{@code [pinyin] Beijing}</li>
	 * </ul>
	 *
	 * <p>Returns {@code null} when the variant is empty or unrecognized.</p>
	 */
	public static String renderNameVariant(final FLEFRecord variantNode){
		if(variantNode == null)
			return null;
		final FLEFRecord branch = variantNode.getTheOnlyChild();
		if(branch == null)
			return null;
		final String tag = branch.getTag();
		if(tag == null)
			return null;

		final String value = FLEFRecordHelper.getChildValue(branch, TextValueVariantReader.TAG_PHONETIC_VALUE);
		if(StringUtils.isEmpty(value))
			return null;

		if(TextValueVariantReader.TAG_PHONETIC.equalsIgnoreCase(tag)){
			final String system = FLEFRecordHelper.getChildValue(branch, TextValueVariantReader.TAG_PHONETIC_SYSTEM);
			return (system != null && !system.isBlank()
				? "[" + system + "] ": StringUtils.EMPTY) + value;
		}

		if(TextValueVariantReader.TAG_TRANSCRIPTION.equalsIgnoreCase(tag)){
			final String system = FLEFRecordHelper.getChildValue(branch, TextValueVariantReader.TAG_TRANSCRIPTION_SYSTEM);
			final String type = FLEFRecordHelper.getChildValue(branch, TextValueVariantReader.TAG_TRANSCRIPTION_TYPE);
			final StringBuilder tagText = new StringBuilder();
			if(system != null && !system.isBlank())
				tagText.append(system);
			if(type != null && !type.isBlank()){
				if(!tagText.isEmpty())
					tagText.append(" → ");
				tagText.append(type);
			}
			return (!tagText.isEmpty()? "[" + tagText + "] ": StringUtils.EMPTY) + value;
		}

		return value;
	}


	/* ----- Notes and citations -------------------------------------------- */

	/** Text of a {@code note} record, whether stored as a value or single child. */
	public static String noteText(final FLEFRecord note){
		final String v = note.getValue();
		if(v != null)
			return v;
		final FLEFRecord only = FLEFRecordHelper.findChild(note, NoteReader.TAG_TEXT);
		return (only != null? only.getValue(): StringUtils.EMPTY);
	}

	/** @return the descendants of a record (at any depth) with the given tag, in the order of the tree */
	public static List<FLEFRecord> collectDescendantsWithTag(final FLEFRecord rec, final String tag){
		final List<FLEFRecord> out = new ArrayList<>();
		collectDescendantsWithTag(rec, tag, out);
		return out;
	}

	private static void collectDescendantsWithTag(final FLEFRecord rec, final String tag, final List<FLEFRecord> out){
		for(final FLEFRecord child : rec.getChildren()){
			if(tag.equalsIgnoreCase(child.getTag()))
				out.add(child);
			collectDescendantsWithTag(child, tag, out);
		}
	}

	/** Extracts the referenced source ID from a {@code source} citation. */
	public static String extractSourceId(final FLEFRecord sourceChild){
		final String direct = FLEFRecordHelper.getChildValue(sourceChild, TAG_SOURCE);
		if(direct != null)
			return direct;
		final FLEFRecord inner = FLEFRecordHelper.findChild(sourceChild, SourceReader.TYPE);
		if(inner != null)
			return (inner.getTheOnlyChild() != null
				? inner.getTheOnlyChild().getValue()
				: inner.getValue());
		return null;
	}


	/* ======================================================================
	 *                          Contacts
	 * ====================================================================== */

	/**
	 * Renders a single {@code ContactStructure} as a one-line string.
	 *
	 * <p>Format: {@code **<type>:** <value> — <name> — *<note>*}. The type,
	 * name and note are omitted when absent. Returns {@code null} when the
	 * contact has no value.</p>
	 */
	public static String renderContact(final FLEFRecord contact){
		if(contact == null)
			return null;

		final String value = FLEFRecordHelper.getChildValue(contact, ContactReader.TAG_VALUE);
		if(StringUtils.isEmpty(value))
			return null;

		final String type = FLEFRecordHelper.getChildValue(contact, ContactReader.TAG_TYPE);
		final String name = FLEFRecordHelper.getChildValue(contact, FLEFRecordHelper.composePath(ContactReader.TAG_NAME, NameReader.TAG_VALUE));
		final String note = FLEFRecordHelper.getChildValue(contact, ContactReader.TAG_REMARK);

		final StringBuilder sb = new StringBuilder();
		if(type != null && !type.isBlank())
			sb.append("**").append(escape(enumLabel(type))).append(":** ");
		//a postal address is written on several lines: in a list item it would run together, so its lines are joined by commas
		sb.append(escape(String.join(", ", value.lines()
			.map(String::strip)
			.filter(line -> !line.isEmpty())
			.toList())));
		if(name != null && !name.isBlank())
			sb.append(" — ").append(escape(name));
		if(note != null && !note.isBlank())
			sb.append(" — *").append(escape(note)).append('*');
		return sb.toString();
	}


	/* ======================================================================
	 *                          Note translations
	 * ====================================================================== */

	/**
	 * Extracts every {@code translation} of the given {@code note} node as
	 * a one-line string of the form {@code *translation* (locale): text}.
	 * Translations without text are skipped.
	 */
	public static List<String> noteTranslations(final FLEFRecord note, final ReportLabels labels){
		final List<String> out = new ArrayList<>();
		for(final FLEFRecord tr : FLEFRecordHelper.findChildren(note, NoteReader.TAG_TRANSLATION)){
			final String text = FLEFRecordHelper.getChildValue(tr, NoteReader.TAG_TEXT);
			if(StringUtils.isEmpty(text))
				continue;
			final String locale = FLEFRecordHelper.getChildValue(tr, NoteReader.TAG_LOCALE);

			final StringBuilder sb = new StringBuilder("*")
				.append(labels.sections().noteTranslation())
				.append('*');
			if(locale != null && !locale.isBlank())
				sb.append(StringUtils.SPACE)
					.append('(')
					.append(escape(locale))
					.append(')');
			sb.append(": ")
				.append(escape(text));
			out.add(sb.toString());
		}
		return out;
	}


	/* ======================================================================
	 *                          Image crop
	 * ====================================================================== */

	/**
	 * Builds a caption for an image, appending the crop rectangle when
	 * present. The crop is rendered as text (not applied to the image),
	 * because no output format supports crop uniformly — this keeps the
	 * information available to the reader without losing it.
	 */
	public static String imageCaption(final FLEFRecord imageNode, final String base,
		final ReportLabels labels){
		if(imageNode == null)
			return base;

		final FLEFRecord cropNode = FLEFRecordHelper.findChild(imageNode, CropReader.TAG_CROP);
		if(cropNode == null)
			return base;

		final String x = FLEFRecordHelper.getChildValue(cropNode, CropReader.TAG_X);
		final String y = FLEFRecordHelper.getChildValue(cropNode, CropReader.TAG_Y);
		final String w = FLEFRecordHelper.getChildValue(cropNode, CropReader.TAG_WIDTH);
		final String h = FLEFRecordHelper.getChildValue(cropNode, CropReader.TAG_HEIGHT);
		if(x == null || y == null || w == null || h == null)
			return base;

		return base + " — " + labels.sections().imageCroppedRegion()
			+ ": x=" + x + ", y=" + y + ", " + w + "×" + h;
	}


	/* ======================================================================
	 *                          Note title
	 * ====================================================================== */

	/** Returns the trimmed title of a {@code note} node, or {@code null}. */
	public static String noteTitle(final FLEFRecord note){
		final String title = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_TITLE);
		return (title != null && !title.isBlank()? title.trim(): null);
	}


	/* ----- Misc ------------------------------------------------------------ */

	/** Whether a kinship term is meaningful for display (not "self" / "unrelated"). */
	public static boolean isUsefulKinshipTerm(final String term){
		return (term != null
			&& !term.isBlank()
			&& !"self".equals(term)
			&& !"unrelated".equals(term));
	}

	/** Appends a {@code **label:** value} row only when the value is non-blank. */
	public static void appendIfPresent(final List<String> rows, final String label, final String value){
		if(value != null && !value.isBlank())
			rows.add("**" + label + ":** " + escape(value));
	}

	/**
	 * Appends the validity of a record: a single {@code Date} row when it starts and ends on the same date (the date of the document
	 * that states it, not a period), otherwise the {@code From} and {@code To} rows.
	 */
	public static void appendValidity(final List<String> rows, final ReportLabels labels, final String from, final String to){
		final boolean hasFrom = (from != null && !from.isBlank());
		final boolean hasTo = (to != null && !to.isBlank());
		if(hasFrom && hasTo && from.trim().equals(to.trim())){
			appendIfPresent(rows, labels.sections().date(), from);
			return;
		}

		if(hasFrom)
			appendIfPresent(rows, labels.sections().validFrom(), from);
		if(hasTo)
			appendIfPresent(rows, labels.sections().validTo(), to);
	}

	/**
	 * Writes the coordinates of a place, kept as an ISO 6709 string ({@code +45.65056+012.21333/}), as {@code 45.65056° N, 12.21333° E}.
	 *
	 * @param iso	The coordinates, {@code null} if there are none.
	 * @return	The coordinates for a reader; the text as it is if it is not in that form; {@code null} if there are none.
	 */
	public static String coordinates(final String iso){
		if(iso == null || iso.isBlank())
			return null;

		final String text = StringUtils.removeEnd(iso.strip(), "/");
		//the sign of the latitude, its digits, the sign of the longitude, its digits (an altitude may follow: it is not told)
		final int second = Math.max(text.indexOf('+', 1), text.indexOf('-', 1));
		if(text.isEmpty() || (text.charAt(0) != '+' && text.charAt(0) != '-') || second < 0)
			return iso;
		int third = text.length();
		for(int i = second + 1; i < text.length(); i ++)
			if(text.charAt(i) == '+' || text.charAt(i) == '-'){
				third = i;
				break;
			}

		try{
			final double latitude = Double.parseDouble(text.substring(0, second));
			final double longitude = Double.parseDouble(text.substring(second, third));
			return Math.abs(latitude) + "° " + (latitude >= 0? "N": "S") + ", " + Math.abs(longitude) + "° " + (longitude >= 0? "E": "W");
		}
		catch(final NumberFormatException ignored){
			return iso;
		}
	}

	/** The chapters of a report that make the table of contents when there are many of them (a book about everybody has a chapter for each person). */
	private static final int MANY_CHAPTERS = 40;
	/** A chapter about a record: its title ends with the identifier of the record ({@code Antonia Pastrelo [I1430]}). */
	private static final Pattern RECORD_CHAPTER = Pattern.compile("\\[[A-Za-z]+\\d+\\]\\s*$");

	/**
	 * The chapters the table of contents lists. When there are many, the chapters about a record (a person, a group) are left out: the
	 * index of the individuals leads to them, and a contents of hundreds of lines is of no use.
	 *
	 * @param chapters	The headings of the first level, in order.
	 * @return	The headings that are listed.
	 */
	public static List<ReportSection.Heading> contentsOf(final List<ReportSection.Heading> chapters){
		if(chapters.size() <= MANY_CHAPTERS)
			return chapters;

		return chapters.stream()
			.filter(chapter -> !RECORD_CHAPTER.matcher(chapter.text()).find())
			.toList();
	}

	/* ----- Files ----------------------------------------------------------- */

	/** @return	Whether the location is an address on the net ({@code https://...}), not a file of the disk. */
	private static boolean isUrl(final String location){
		return location.contains("://");
	}

	/**
	 * What a report says of the location of a file: its name, not the whole path of the disk of who made the report (that is of no
	 * use to who reads it, and tells where the files of the family are); an address on the net is kept as it is.
	 *
	 * @param location	A path or an address, {@code null} if there is none.
	 * @return	The name of the file, or the address; {@code null} if there is no location.
	 */
	public static String fileNameOf(final String location){
		if(location == null || location.isBlank())
			return null;

		final String trimmed = location.strip();
		if(isUrl(trimmed))
			return trimmed;
		final int separator = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
		return (separator >= 0? trimmed.substring(separator + 1): trimmed);
	}

	/**
	 * The target of a link to a file, written so that every viewer follows it: each segment of the path is percent-encoded (a space, a
	 * parenthesis or an accent break a link otherwise), and the path of a disk of Windows is a {@code file:///} address.
	 *
	 * @param path	A path or an address, the separators either {@code /} or {@code \}.
	 * @return	The target of the link.
	 */
	public static String linkTarget(final String path){
		if(path == null || path.isBlank())
			return StringUtils.EMPTY;

		final String trimmed = path.strip();
		if(isUrl(trimmed))
			return Strings.CS.replace(trimmed, StringUtils.SPACE, "%20");

		final String[] segments = StringUtils.split(trimmed.replace('\\', '/'), '/');
		final StringBuilder sb = new StringBuilder();
		if(trimmed.startsWith("/") || trimmed.startsWith("\\"))
			sb.append('/');
		for(int i = 0; i < segments.length; i ++){
			final String segment = segments[i];
			if(i == 0 && segment.length() == 2 && Character.isLetter(segment.charAt(0)) && segment.charAt(1) == ':')
				sb.append("file:///").append(segment);
			else{
				if(i > 0)
					sb.append('/');
				sb.append(".".equals(segment) || "..".equals(segment)
					? segment: URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
			}
		}
		return sb.toString();
	}

	/** @return	Whether the description of a file is the name of the file (without the extension): it is not told twice. */
	public static boolean isDescribedBy(final String description, final String location){
		if(description == null || description.isBlank() || location == null)
			return false;

		final int dot = location.lastIndexOf('.');
		return description.strip().equalsIgnoreCase(dot > 0? location.substring(0, dot): location);
	}

	/* ----- Enum labels ----------------------------------------------------- */

	/**
	 * Turns an enum-like code ({@code activity_type}, {@code status},
	 * {@code result}, {@code proof_status}, {@code priority}, ...) into a
	 * human-readable label by replacing underscores with spaces. Returns
	 * {@code null} for null or blank input.
	 */
	public static String enumLabel(final String code){
		if(StringUtils.isEmpty(code))
			return null;
		return code.trim().replace('_', ' ');
	}

	/**
	 * Converts a note body to a renderer-agnostic string. Markdown notes
	 * are returned unchanged (renderers that support markdown handle them
	 * natively); HTML notes are stripped of tags for plain-text renderers.
	 * The mime is looked up from the note node itself; when absent, the
	 * text is returned as-is.
	 */
	public static String noteBody(final FLEFRecord note){
		final String text = noteText(note);
		final String mime = FLEFRecordHelper.getChildValue(note, NoteReader.TAG_MIME);
		if(StringUtils.isEmpty(mime))
			return text;
		if(mime.contains("markdown"))
			return text;
		if(mime.contains("html"))
			return stripHtml(text);
		return text;
	}

	/** Very small HTML-to-text converter: keeps <br>/<p> as newlines. */
	private static String stripHtml(final String html){
		final String text = HTML_TAG.matcher(HTML_P_OPEN.matcher(HTML_P_CLOSE.matcher(HTML_BR.matcher(html)
			.replaceAll(StringUtils.LF))
			.replaceAll(StringUtils.LF + StringUtils.LF))
			.replaceAll(StringUtils.EMPTY))
			.replaceAll(StringUtils.EMPTY);
		return text
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&amp;", "&");
	}

	/**
	 * Renders the {@code ExtractStructure.remark} entries of a citation as a one-line string. Returns
	 * {@code null} when there is no remark.
	 */
	public static String extractNote(final FLEFRecord citation){
		final List<String> remarks = new ArrayList<>();
		for(final FLEFRecord extract : FLEFRecordHelper.findChildren(citation, SourceCitationReader.TAG_EXTRACT))
			for(final FLEFRecord remark : FLEFRecordHelper.findChildren(extract, SourceCitationReader.TAG_REMARK)){
				final String value = remark.getValue();
				if(StringUtils.isNotBlank(value))
					remarks.add(value.trim());
			}
		return (remarks.isEmpty()? null: String.join("; ", remarks));
	}

	/**
	 * Renders an {@code ExtractStructure.media_part.crop} as a compact
	 * string for the citation. Returns {@code null} when the crop is absent.
	 */
	public static String extractCrop(final FLEFRecord citation){
		final FLEFRecord crop = FLEFRecordHelper.findChild(citation,
			FLEFRecordHelper.composePath(SourceCitationReader.TAG_EXTRACT, SourceCitationReader.TAG_MEDIA_PART, SourceCitationReader.TAG_CROP));
		if(crop == null)
			return null;
		final String x = FLEFRecordHelper.getChildValue(crop, CropReader.TAG_X);
		final String y = FLEFRecordHelper.getChildValue(crop, CropReader.TAG_Y);
		final String w = FLEFRecordHelper.getChildValue(crop, CropReader.TAG_WIDTH);
		final String h = FLEFRecordHelper.getChildValue(crop, CropReader.TAG_HEIGHT);
		if(x == null || y == null || w == null || h == null)
			return null;
		return "x=" + x + ", y=" + y + ", " + w + "×" + h;
	}

}
