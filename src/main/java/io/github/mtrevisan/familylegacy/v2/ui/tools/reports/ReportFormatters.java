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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.PlaceReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Pure static helpers shared by section builders. No model state, no
 * dependencies on the report configuration.
 */
public final class ReportFormatters{

	private static final String TAG_PART = "part";
	private static final String TAG_VALUE = "value";
	private static final String TAG_NAME = "name";
	private static final String TAG_PLACE = "place";
	private static final String TAG_SOURCE = "source";


	private ReportFormatters(){
	}


	/* ----- Basic escaping / null-safety ------------------------------------ */

	/** Null-safe identity (used before markdown escaping). */
	static String esc(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}

	/** Null-safe identity (used for values already markdown-safe). */
	static String escape(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}

	/** Null-safe identity for values that may legitimately be empty. */
	static String orEmpty(final String s){
		return (s == null? StringUtils.EMPTY: s);
	}


	/* ----- Names ----------------------------------------------------------- */

	/**
	 * Concatenates the ordered {@code part} values of a {@code name} structure
	 * into a single display string, preserving the original part order.
	 */
	static String buildName(final FLEFRecord nameRec){
		final StringBuilder sb = new StringBuilder();
		for(final FLEFRecord child : nameRec.getChildren()){
			if(!TAG_PART.equalsIgnoreCase(child.getTag()))
				continue;
			final String v = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
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
	static String placeName(final FLEFRecord place){
		for(final FLEFRecord n : FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME)){
			final String v = FLEFRecordHelper.getChildValue(n, NameReader.TAG_VALUE);
			if(v != null && !v.isBlank())
				return v;
		}
		return null;
	}

	/** Resolves the display name of a {@code place.place} reference, or null. */
	static String resolvePlaceName(final FLEFModel model, final FLEFRecord rec){
		final String ref = FLEFRecordHelper.getChildValue(rec, TAG_PLACE + "." + TAG_PLACE);
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
	static String renderNameVariant(final FLEFRecord variantNode){
		if(variantNode == null)
			return null;
		final FLEFRecord branch = variantNode.getTheOnlyChild();
		if(branch == null)
			return null;
		final String tag = branch.getTag();
		if(tag == null)
			return null;

		final String value = FLEFRecordHelper.getChildValue(branch, "value");
		if(value == null || value.isBlank())
			return null;

		if("phonetic".equalsIgnoreCase(tag)){
			final String system = FLEFRecordHelper.getChildValue(branch, "system");
			return (system != null && !system.isBlank()
				? "[" + system + "] ": StringUtils.EMPTY) + value;
		}

		if("transcription".equalsIgnoreCase(tag)){
			final String system = FLEFRecordHelper.getChildValue(branch, "system");
			final String type = FLEFRecordHelper.getChildValue(branch, "type");
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
	static String noteText(final FLEFRecord note){
		final String v = note.getValue();
		if(v != null)
			return v;
		final FLEFRecord only = FLEFRecordHelper.findChild(note, "text");
		return (only != null? only.getValue(): StringUtils.EMPTY);
	}

	/** Extracts the referenced source ID from a {@code source} citation. */
	public static String extractSourceId(final FLEFRecord sourceChild){
		final String direct = FLEFRecordHelper.getChildValue(sourceChild, TAG_SOURCE);
		if(direct != null)
			return direct;
		final FLEFRecord inner = FLEFRecordHelper.findChild(sourceChild, TAG_SOURCE);
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
	static String renderContact(final FLEFRecord contact){
		if(contact == null)
			return null;

		final String value = FLEFRecordHelper.getChildValue(contact, "value");
		if(value == null || value.isBlank())
			return null;

		final String type = FLEFRecordHelper.getChildValue(contact, "type");
		final String name = FLEFRecordHelper.getChildValue(contact, "name.value");
		final String note = FLEFRecordHelper.getChildValue(contact, "note");

		final StringBuilder sb = new StringBuilder();
		if(type != null && !type.isBlank())
			sb.append("**").append(escape(enumLabel(type))).append(":** ");
		sb.append(escape(value));
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
	static List<String> noteTranslations(final FLEFRecord note, final ReportLabels labels){
		final List<String> out = new ArrayList<>();
		for(final FLEFRecord tr : FLEFRecordHelper.findChildren(note, "translation")){
			final String text = FLEFRecordHelper.getChildValue(tr, "text");
			if(text == null || text.isBlank())
				continue;
			final String locale = FLEFRecordHelper.getChildValue(tr, "locale");

			final StringBuilder sb = new StringBuilder("*")
				.append(labels.sections().noteTranslation()).append('*');
			if(locale != null && !locale.isBlank())
				sb.append(" (").append(escape(locale)).append(')');
			sb.append(": ").append(escape(text));
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
	static String imageCaption(final FLEFRecord imageNode, final String base,
		final ReportLabels labels){
		if(imageNode == null)
			return base;

		final FLEFRecord cropNode = FLEFRecordHelper.findChild(imageNode, "crop");
		if(cropNode == null)
			return base;

		final String x = FLEFRecordHelper.getChildValue(cropNode, "x");
		final String y = FLEFRecordHelper.getChildValue(cropNode, "y");
		final String w = FLEFRecordHelper.getChildValue(cropNode, "width");
		final String h = FLEFRecordHelper.getChildValue(cropNode, "height");
		if(x == null || y == null || w == null || h == null)
			return base;

		return base + " — " + labels.sections().imageCroppedRegion()
			+ ": " + x + "," + y + "," + w + "," + h;
	}


	/* ======================================================================
	 *                          Note title
	 * ====================================================================== */

	/** Returns the trimmed title of a {@code note} node, or {@code null}. */
	static String noteTitle(final FLEFRecord note){
		final String title = FLEFRecordHelper.getChildValue(note, "title");
		return (title != null && !title.isBlank()? title.trim(): null);
	}


	/* ----- Misc ------------------------------------------------------------ */

	/** Whether a kinship term is meaningful for display (not "self" / "unrelated"). */
	static boolean isUsefulKinshipTerm(final String term){
		return (term != null
			&& !term.isBlank()
			&& !"self".equals(term)
			&& !"unrelated".equals(term));
	}

	/** Appends a {@code **label:** value} row only when the value is non-blank. */
	static void appendIfPresent(final List<String> rows, final String label, final String value){
		if(value != null && !value.isBlank())
			rows.add("**" + label + ":** " + escape(value));
	}

	/* ----- Enum labels ----------------------------------------------------- */

	/**
	 * Turns an enum-like code ({@code activity_type}, {@code status},
	 * {@code result}, {@code proof_status}, {@code priority}, ...) into a
	 * human-readable label by replacing underscores with spaces. Returns
	 * {@code null} for null or blank input.
	 */
	static String enumLabel(final String code){
		if(code == null || code.isBlank())
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
	static String noteBody(final FLEFRecord note){
		final String text = noteText(note);
		final String mime = FLEFRecordHelper.getChildValue(note, "mime");
		if(mime == null || mime.isBlank())
			return text;
		if(mime.contains("markdown"))
			return text;
		if(mime.contains("html"))
			return stripHtml(text);
		return text;
	}

	/** Very small HTML-to-text converter: keeps <br>/<p> as newlines. */
	private static String stripHtml(final String html){
		return html
			.replaceAll("(?i)<br\\s*/?>", StringUtils.LF)
			.replaceAll("(?i)</p>", StringUtils.LF + StringUtils.LF)
			.replaceAll("(?i)<p[^>]*>", StringUtils.EMPTY)
			.replaceAll("<[^>]+>", StringUtils.EMPTY)
			.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"");
	}

	/**
	 * Renders an {@code ExtractStructure.note} as a one-line string. Returns
	 * {@code null} when the note is empty.
	 */
	static String extractNote(final FLEFRecord citation){
		final String note = FLEFRecordHelper.getChildValue(citation,
			"extract.note");
		return (note != null && !note.isBlank()? note.trim(): null);
	}

	/**
	 * Renders an {@code ExtractStructure.document_part.crop} as a compact
	 * string for the citation. Returns {@code null} when the crop is absent.
	 */
	static String extractCrop(final FLEFRecord citation){
		final FLEFRecord crop = FLEFRecordHelper.findChild(citation,
			"extract.document_part.crop");
		if(crop == null)
			return null;
		final String x = FLEFRecordHelper.getChildValue(crop, "x");
		final String y = FLEFRecordHelper.getChildValue(crop, "y");
		final String w = FLEFRecordHelper.getChildValue(crop, "width");
		final String h = FLEFRecordHelper.getChildValue(crop, "height");
		if(x == null || y == null || w == null || h == null)
			return null;
		return x + "," + y + "," + w + "," + h;
	}

}
