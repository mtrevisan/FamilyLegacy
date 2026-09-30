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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Renders the "extra info" blocks attached to any record: notes, source
 * citations, evidence qualifiers, date provenance, privacy block and the
 * audit trail.
 *
 * <p>Every method respects the report's {@link PrivacyPolicy} — including
 * the privacy declared on the citation node itself — and the
 * {@link ReportConfig} flags.</p>
 */
final class CitationRenderer{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_SOURCE = "source";
	private static final String TAG_EXTRACT = "extract";
	private static final String TAG_TEXT = "text";
	private static final String TAG_TYPE = "type";
	private static final String TAG_LOCALE = "locale";
	private static final String TAG_MIME = "mime";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_NOTE = "note";
	private static final String TAG_EVIDENCE = "evidence";

	private static final String TAG_AUDIT = "audit";
	private static final String TAG_CREATION = "creation";
	private static final String TAG_UPDATE = "update";
	private static final String TAG_DATE = "date";
	private static final String TAG_COMMENT = "comment";

	private static final String TAG_PRIVACY = "privacy";


	private final ReportContext ctx;


	CitationRenderer(final ReportContext ctx){
		this.ctx = ctx;
	}


	/* ======================================================================
	 *                          Notes
	 * ====================================================================== */

	/**
	 * Note blocks for a record. Each note is rendered as its text, followed
	 * by:
	 * <ul>
	 *   <li>an optional metadata suffix with the note's {@code mime} and
	 *       {@code locale};</li>
	 *   <li>its title (when present);</li>
	 *   <li>its translations (when present);</li>
	 *   <li>the sources cited by the note itself (when present).</li>
	 * </ul>
	 *
	 * <p>Notes whose privacy level is hidden by the current policy are
	 * silently skipped.</p>
	 */
	List<ReportSection> notes(final FLEFRecord rec){
		final List<ReportSection> out = new ArrayList<>();
		for(final FLEFRecord n : ctx.visibleChildren(rec, TAG_NOTE)){
			if(!ctx.isVisible(n))
				continue;

			final String text = ReportFormatters.noteText(n);
			final String suffix = noteMetadataSuffix(n);
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().note() + ":** " + ReportFormatters.escape(text) + suffix));

			final String title = ReportFormatters.noteTitle(n);
			if(title != null)
				out.add(new ReportSection.Paragraph(
					"*" + ctx.labels.sections().noteTitle() + ":* " + ReportFormatters.escape(title)));

			for(final String tr : ReportFormatters.noteTranslations(n, ctx.labels))
				out.add(new ReportSection.Paragraph(tr));

			// Sources cited by the note itself.
			if(ctx.config.sources())
				out.addAll(citations(n));
		}
		return out;
	}

	/**
	 * Builds a small suffix such as {@code " — (text/markdown, it)"} that
	 * records the note's declared MIME type and locale. Returns an empty
	 * string when neither field is present.
	 */
	private String noteMetadataSuffix(final FLEFRecord note){
		final String mime = FLEFRecordHelper.getChildValue(note, TAG_MIME);
		final String locale = FLEFRecordHelper.getChildValue(note, TAG_LOCALE);
		final List<String> tags = new ArrayList<>(2);
		if(mime != null && !mime.isBlank()) tags.add(mime.trim());
		if(locale != null && !locale.isBlank()) tags.add(locale.trim());
		if(tags.isEmpty())
			return StringUtils.EMPTY;
		return " — *(" + ReportFormatters.escape(String.join(", ", tags)) + ")*";
	}


	/* ======================================================================
	 *                          Citations
	 * ====================================================================== */

	/**
	 * Citation blocks for a record. Each citation shows the source title,
	 * the locator (if any) and the extracted text (if any). Citations whose
	 * own privacy level is hidden by the current policy are skipped, even
	 * when the referenced source is visible.
	 */
	List<ReportSection> citations(final FLEFRecord rec){
		final List<ReportSection> out = new ArrayList<>();
		final List<FLEFRecord> cites = ctx.visibleChildren(rec, TAG_SOURCE);
		if(cites.isEmpty())
			return out;

		for(final FLEFRecord c : cites){
			// Filter on the citation's own privacy block (item 6).
			if(!ctx.isVisible(c))
				continue;

			final String sid = ReportFormatters.extractSourceId(c);
			if(sid == null)
				continue;
			if(!ctx.isVisible(ctx.model.getRecordById(sid)))
				continue;

			final StringBuilder sb = new StringBuilder("• ")
				.append(ReportFormatters.escape(ctx.sourceTitle(sid)));

			final String locator = FLEFRecordHelper.getChildValue(c, TAG_LOCATOR);
			if(locator != null)
				sb.append(" — *").append(ReportFormatters.escape(locator)).append('*');

			final String extractText = FLEFRecordHelper.getChildValue(c,
				TAG_EXTRACT + "." + TAG_TEXT);
			if(extractText != null){
				sb.append(" — *").append(ctx.labels.sections().extractText()).append(":* \"")
					.append(ReportFormatters.escape(extractText)).append('"');

				final String extractType = FLEFRecordHelper.getChildValue(c,
					TAG_EXTRACT + "." + TAG_TYPE);
				final String extractLocale = FLEFRecordHelper.getChildValue(c,
					TAG_EXTRACT + "." + TAG_LOCALE);
				final List<String> quals = new ArrayList<>();
				if(extractType != null && !extractType.isBlank())
					quals.add(ReportFormatters.orEmpty(
						ReportFormatters.enumLabel(extractType)));
				if(extractLocale != null && !extractLocale.isBlank())
					quals.add(extractLocale);
				if(!quals.isEmpty())
					sb.append(" *(").append(ReportFormatters.escape(
						String.join(", ", quals))).append(")*");
			}

			// Extract note.
			final String extractNote = ReportFormatters.extractNote(c);
			if(extractNote != null)
				sb.append(" — *").append(ctx.labels.sections().extractNote()).append(":* ")
					.append(ReportFormatters.escape(extractNote));

			// Extract crop.
			final String crop = ReportFormatters.extractCrop(c);
			if(crop != null)
				sb.append(" — *").append(ctx.labels.sections().extractCrop()).append(":* ")
					.append(ReportFormatters.escape(crop));

			out.add(new ReportSection.Paragraph(sb.toString()));

			if(ctx.config.evidence())
				addEvidence(c, out);
		}
		return out;
	}


	/* ======================================================================
	 *                          Evidence
	 * ====================================================================== */

	void addEvidence(final FLEFRecord owner, final List<ReportSection> out){
		final FLEFRecord ev = FLEFRecordHelper.findChild(owner, TAG_EVIDENCE);
		if(ev == null)
			return;

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceSourceType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersPanel.TAG_SOURCE_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceInformationType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersPanel.TAG_INFORMATION_TYPE));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().evidenceType(),
			FLEFRecordHelper.getChildValue(ev, EvidenceQualifiersPanel.TAG_EVIDENCE_TYPE));

		if(!rows.isEmpty())
			out.add(new ReportSection.Paragraph(
				"*" + ctx.labels.sections().evidence() + ":* " + String.join(", ", rows)));
	}


	/* ======================================================================
	 *                          Date provenance
	 * ====================================================================== */

	void addDateProvenance(final FLEFRecord rec, final String fieldTag,
		final List<ReportSection> out){
		if(!ctx.config.dateProvenance())
			return;

		final FLEFRecord dateField = FLEFRecordHelper.findChild(rec, fieldTag);
		if(dateField == null)
			return;

		final List<FLEFRecord> cites = ctx.visibleChildren(dateField, TAG_SOURCE);
		final boolean hasEvidence = FLEFRecordHelper.findChild(dateField, TAG_EVIDENCE) != null;
		if(cites.isEmpty() && !hasEvidence)
			return;

		final List<String> items = new ArrayList<>();
		for(final FLEFRecord c : cites){
			if(!ctx.isVisible(c))
				continue;
			final String sid = ReportFormatters.extractSourceId(c);
			if(sid == null)
				continue;
			if(!ctx.isVisible(ctx.model.getRecordById(sid)))
				continue;
			final String locator = FLEFRecordHelper.getChildValue(c, TAG_LOCATOR);
			final StringBuilder sb = new StringBuilder(ctx.sourceTitle(sid));
			if(locator != null)
				sb.append(" — *").append(ReportFormatters.escape(locator)).append('*');
			items.add(sb.toString());
		}

		if(!items.isEmpty()){
			out.add(new ReportSection.Paragraph("*" + ctx.labels.sections().dateProvenance() + ":*"));
			out.add(new ReportSection.BulletList(items));
		}

		if(hasEvidence){
			final List<ReportSection> ev = new ArrayList<>();
			addEvidence(dateField, ev);
			out.addAll(ev);
		}
	}


	/* ======================================================================
	 *                          Participation details
	 * ====================================================================== */

	List<ReportSection> participationDetails(final FLEFRecord participation){
		final List<ReportSection> out = new ArrayList<>();
		if(participation == null)
			return out;

		// Participation can carry its own note, independent from the event.
		final String note = FLEFRecordHelper.getChildValue(participation, TAG_NOTE);
		if(note != null && !note.isBlank())
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().note() + ":** " + ReportFormatters.escape(note)));

		if(ctx.config.notes())
			out.addAll(notes(participation));
		if(ctx.config.sources())
			out.addAll(citations(participation));
		if(ctx.config.evidence())
			addEvidence(participation, out);
		out.addAll(audit(participation));
		out.addAll(privacyInfo(participation));
		return out;
	}


	/* ======================================================================
	 *                          Privacy details
	 * ====================================================================== */

	List<ReportSection> privacyInfo(final FLEFRecord rec){
		if(!ctx.config.showPrivacyDetails())
			return List.of();

		final FLEFRecord privacy = FLEFRecordHelper.findChild(rec, TAG_PRIVACY);
		if(privacy == null)
			return List.of();

		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyLevel(),
			PrivacyReader.extractLevel(privacy));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyReason(),
			PrivacyReader.extractReason(privacy));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().privacyExpires(),
			PrivacyReader.extractExpires(privacy));
		if(rows.isEmpty())
			return List.of();

		return List.of(new ReportSection.Paragraph(
			"*" + ctx.labels.sections().privacy() + ":* " + String.join(", ", rows)));
	}


	/* ======================================================================
	 *                          Audit
	 * ====================================================================== */

	List<ReportSection> audit(final FLEFRecord rec){
		if(!ctx.config.audit())
			return List.of();

		final FLEFRecord auditNode = FLEFRecordHelper.findChild(rec, TAG_AUDIT);
		if(auditNode == null)
			return List.of();

		final List<String> rows = new ArrayList<>();

		final FLEFRecord creation = FLEFRecordHelper.findChild(auditNode, TAG_CREATION);
		if(creation != null)
			appendAuditRow(rows, ctx.labels.sections().auditCreated(), creation);

		for(final FLEFRecord update : FLEFRecordHelper.findChildren(auditNode, TAG_UPDATE))
			appendAuditRow(rows, ctx.labels.sections().auditUpdated(), update);

		if(rows.isEmpty())
			return List.of();
		return List.of(new ReportSection.BulletList(rows));
	}

	private void appendAuditRow(final List<String> rows, final String label,
		final FLEFRecord entry){
		final String date = FLEFRecordHelper.getChildValue(entry, TAG_DATE);
		if(date == null || date.isBlank())
			return;

		final StringBuilder sb = new StringBuilder("**").append(label).append(":** ")
			.append(ReportFormatters.escape(date.trim()));
		final String comment = FLEFRecordHelper.getChildValue(entry, TAG_COMMENT);
		if(comment != null && !comment.isBlank())
			sb.append(" — ").append(ReportFormatters.escape(comment.trim()));
		rows.add(sb.toString());
	}

}
