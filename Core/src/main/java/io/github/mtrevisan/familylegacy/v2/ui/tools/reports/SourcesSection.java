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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.date.DateService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Builds the Sources section.
 *
 * <p>Collects every {@code source} record cited by the root — directly, or
 * through its events, attributes, relationships, participations or
 * memberships — and renders each source with:</p>
 * <ul>
 *   <li>its title, author, publisher, date, place and media type;</li>
 *   <li>every repository citation, with locator, contacts and note;</li>
 *   <li>every document attached to the source, with URI, description and
 *       mapping;</li>
 *   <li>its notes, evidence qualifiers, audit trail and privacy block.</li>
 * </ul>
 *
 * <p>The section is skipped entirely when the root is itself a source, in
 * which case {@link SourceRootSection} produces a richer report.</p>
 *
 * <p>Emitted only when {@code config.sources()} is enabled and at least one
 * source is reachable.</p>
 */
final class SourcesSection implements SectionBuilder{

	/* ======================================================================
	 *                          Tags
	 * ====================================================================== */

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_AUTHOR = "author";
	private static final String TAG_PUBLISHER = "publisher";
	private static final String TAG_MEDIA_TYPE = "media_type";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_CONTACT = "contact";
	private static final String TAG_NOTE = "note";
	private static final String TAG_URI = "uri";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_MAPPING = "mapping";
	private static final String TAG_DOCUMENT = "document";

	private static final String TYPE_REPOSITORY = "repository";


	private final ReportContext ctx;


	SourcesSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	/* ======================================================================
	 *                          Build
	 * ====================================================================== */

	@Override
	public List<ReportSection> build(){
		if(!ctx.config.sources())
			return List.of();

		// A source report has its own dedicated section; skip this one to
		// avoid duplication.
		if(ctx.isSourceRoot())
			return List.of();

		final Set<String> ids = SourceCollector.collect(ctx);
		if(ids.isEmpty())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.sources()));

		for(final String sid : ids){
			final FLEFRecord src = ctx.visible(ctx.model.getRecordById(sid));
			if(src != null)
				appendSource(out, src);
		}
		return out;
	}


	/* ======================================================================
	 *                          Rendering
	 * ====================================================================== */

	private void appendSource(final List<ReportSection> out, final FLEFRecord src){
		out.add(new ReportSection.Heading(2, ReportFormatters.escape(
			ctx.sourceTitle(src.getId()))));

		appendBasicInfo(out, src);
		appendRepositories(out, src);
		appendDocuments(out, src);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(src));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(src, out);
		out.addAll(ctx.citations().audit(src));
		out.addAll(ctx.citations().privacyInfo(src));
	}


	/* ----- Basic info ------------------------------------------------------ */

	private void appendBasicInfo(final List<ReportSection> out, final FLEFRecord src){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourceAuthor(),
			FLEFRecordHelper.getChildValue(src, TAG_AUTHOR));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourcePublisher(),
			FLEFRecordHelper.getChildValue(src, TAG_PUBLISHER));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourceDate(),
			DateService.getDateDisplayText(src));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourcePlace(),
			FLEFRecordHelper.extractPlace(src, ctx.model));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sections().sourceMediaType(),
			FLEFRecordHelper.getChildValue(src, TAG_MEDIA_TYPE));

		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	/* ----- Repositories ---------------------------------------------------- */

	private void appendRepositories(final List<ReportSection> out, final FLEFRecord src){
		final List<FLEFRecord> repoRefs = ctx.visibleChildren(src, TYPE_REPOSITORY);
		if(repoRefs.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().sourceRepository()));
		for(final FLEFRecord rc : repoRefs)
			appendRepository(out, rc);
	}

	private void appendRepository(final List<ReportSection> out, final FLEFRecord rc){
		final String rid = rc.extractReferencedId(TYPE_REPOSITORY, TYPE_REPOSITORY);
		final FLEFRecord repo = (rid != null? ctx.visible(ctx.model.getRecordById(rid)): null);

		// Repository name (or raw ID when the reference cannot be resolved).
		final String name = (repo != null
			? RepositoryReader.extractPrimaryName(repo)
			: rid);
		final String locator = FLEFRecordHelper.getChildValue(rc, TAG_LOCATOR);

		final StringBuilder line = new StringBuilder("**")
			.append(ctx.labels.sections().sourceRepository()).append(":** ")
			.append(ReportFormatters.escape(ReportFormatters.orEmpty(name)));
		if(locator != null && !locator.isBlank())
			line.append(" — *").append(ReportFormatters.escape(locator)).append('*');
		out.add(new ReportSection.Paragraph(line.toString()));

		// Repository citation note.
		final String repoNote = FLEFRecordHelper.getChildValue(rc, TAG_NOTE);
		if(repoNote != null && !repoNote.isBlank())
			out.add(new ReportSection.Paragraph(
				"  *" + ctx.labels.sections().sourceRepositoryNote() + ":* "
					+ ReportFormatters.escape(repoNote)));

		// Repository contacts.
		if(repo != null){
			for(final FLEFRecord contact : ctx.visibleChildren(repo, RepositoryReader.TAG_CONTACT)){
				final String rendered = ReportFormatters.renderContact(contact);
				if(rendered != null)
					out.add(new ReportSection.Paragraph("  " + rendered));

				// Contact privacy, when the flag is on.
				if(ctx.config.showPrivacyDetails())
					out.addAll(ctx.citations().privacyInfo(contact));
			}
		}

		// Repository citation evidence.
		if(ctx.config.evidence())
			ctx.citations().addEvidence(rc, out);
	}


	/* ----- Documents ------------------------------------------------------- */

	private void appendDocuments(final List<ReportSection> out, final FLEFRecord src){
		final List<FLEFRecord> docRefs = ctx.visibleChildren(src, TAG_DOCUMENT);
		if(docRefs.isEmpty())
			return;

		out.add(new ReportSection.Heading(3, ctx.labels.sections().sourceDocuments()));
		for(final FLEFRecord docRef : docRefs){
			final String label = documentSummary(docRef.getValue());
			if(label != null)
				out.add(new ReportSection.Paragraph("- " + label));
		}
	}

	/**
	 * Builds a one-line summary of a document: description, URI and mapping,
	 * in that order, separated by {@code —}. Returns {@code null} when the
	 * document cannot be resolved or is hidden.
	 */
	private String documentSummary(final String docId){
		if(docId == null)
			return null;
		final FLEFRecord doc = ctx.visible(ctx.model.getRecordById(docId));
		if(doc == null)
			return ReportFormatters.escape(docId);

		final String uri = FLEFRecordHelper.getChildValue(doc, TAG_URI);
		final String desc = FLEFRecordHelper.getChildValue(doc, TAG_DESCRIPTION);
		final String mapping = FLEFRecordHelper.getChildValue(doc, TAG_MAPPING);

		final StringBuilder sb = new StringBuilder();
		if(desc != null && !desc.isBlank())
			sb.append(ReportFormatters.escape(desc));
		if(uri != null && !uri.isBlank()){
			if(!sb.isEmpty())
				sb.append(" — ");
			sb.append(ReportFormatters.escape(uri));
		}
		if(mapping != null && !mapping.isBlank()){
			if(!sb.isEmpty())
				sb.append(" — ");
			sb.append("*").append(ReportFormatters.escape(
					ReportFormatters.orEmpty(ReportFormatters.enumLabel(mapping))))
				.append("*");
		}
		if(sb.isEmpty())
			sb.append(ReportFormatters.escape(docId));
		return sb.toString();
	}

}
