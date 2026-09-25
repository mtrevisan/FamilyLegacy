package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main body of a source report (root = {@code source} record).
 *
 * <p>Renders:</p>
 * <ul>
 *   <li>title, author, publisher, date, place, media type;</li>
 *   <li>repositories holding the source;</li>
 *   <li>attached documents;</li>
 *   <li>every citation that references this source, grouped by owner;</li>
 *   <li>notes, evidence, privacy and audit.</li>
 * </ul>
 */
final class SourceRootSection implements SectionBuilder{

	private static final String TAG_AUTHOR = "author";
	private static final String TAG_PUBLISHER = "publisher";
	private static final String TAG_MEDIA_TYPE = "media_type";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_URI = "uri";
	private static final String TAG_DESCRIPTION = "description";

	private static final String TYPE_REPOSITORY = "repository";


	private final ReportContext ctx;


	SourceRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isSourceRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.sourceOf(),
			ReportFormatters.escape(ctx.displayText(ctx.root)))));

		writeBasicInfo(out);
		writeRepositories(out);
		writeDocuments(out);
		writeCitations(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(ctx.root, out);
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.sourceAuthor(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_AUTHOR));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sourcePublisher(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_PUBLISHER));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sourceDate(),
			FLEFRecordHelper.extractDate(ctx.root));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sourcePlace(),
			FLEFRecordHelper.extractPlace(ctx.root, ctx.model));
		ReportFormatters.appendIfPresent(rows, ctx.labels.sourceMediaType(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_MEDIA_TYPE));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeRepositories(final List<ReportSection> out){
		final List<FLEFRecord> repos = ctx.index.repositoriesOfSource(ctx.root);
		if(repos.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sourceRepository()));
		for(final FLEFRecord rc : repos){
			final String rid = rc.extractReferencedId(TYPE_REPOSITORY, TYPE_REPOSITORY);
			final FLEFRecord repo = (rid != null? ctx.visible(ctx.model.getRecordById(rid)): null);
			final String name = (repo != null
				? FLEFRecordHelper.getChildValue(repo, TAG_NAME + "." + TAG_VALUE)
				: rid);
			final String locator = FLEFRecordHelper.getChildValue(rc, TAG_LOCATOR);

			final StringBuilder line = new StringBuilder("- ")
				.append(ReportFormatters.escape(ReportFormatters.orEmpty(name)));
			if(locator != null)
				line.append(" — *").append(ReportFormatters.escape(locator)).append('*');
			out.add(new ReportSection.Paragraph(line.toString()));

			if(repo != null){
				for(final FLEFRecord contact : ctx.visibleChildren(repo, "contact")){
					final String rendered = ReportFormatters.renderContact(contact, ctx.labels);
					if(rendered != null)
						out.add(new ReportSection.Paragraph("  " + rendered));
				}
			}
		}
	}


	private void writeDocuments(final List<ReportSection> out){
		final List<FLEFRecord> docs = ctx.index.documentsOfSource(ctx.root);
		if(docs.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sourceDocuments()));
		for(final FLEFRecord docRef : docs){
			final String docId = docRef.getValue();
			if(docId == null)
				continue;
			final FLEFRecord doc = ctx.visible(ctx.model.getRecordById(docId));
			if(doc == null)
				continue;
			final String uri = FLEFRecordHelper.getChildValue(doc, TAG_URI);
			final String descr = FLEFRecordHelper.getChildValue(doc, TAG_DESCRIPTION);
			final StringBuilder sb = new StringBuilder("- ");
			if(descr != null && !descr.isBlank())
				sb.append(ReportFormatters.escape(descr));
			if(uri != null && !uri.isBlank()){
				if(sb.length() > 2)
					sb.append(" — ");
				sb.append(uri);
			}
			out.add(new ReportSection.Paragraph(sb.toString()));
		}
	}


	private void writeCitations(final List<ReportSection> out){
		final List<FLEFRecord> citations = ctx.index.citationsOfSource(ctx.root);
		if(citations.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.sourceCitedBy()));
		final List<String> items = new ArrayList<>(citations.size());
		for(final FLEFRecord c : citations){
			final FLEFRecord owner = findOwnerOf(c);
			final String ownerLabel = (owner != null? ctx.displayText(owner): "?");
			final String locator = FLEFRecordHelper.getChildValue(c, TAG_LOCATOR);
			final StringBuilder sb = new StringBuilder(ReportFormatters.escape(ownerLabel));
			if(locator != null)
				sb.append(" — *").append(ReportFormatters.escape(locator)).append('*');
			items.add(sb.toString());
		}
		out.add(new ReportSection.BulletList(items));
	}


	/**
	 * Finds the record that contains the given {@code source} citation.
	 * Because the citation is an embedded structure, this method relies on
	 * identity comparison across all top-level records.
	 */
	private FLEFRecord findOwnerOf(final FLEFRecord citation){
		for(final FLEFRecord rec : ctx.model.getRecords()){
			for(final FLEFRecord c : FLEFRecordHelper.findChildren(rec, "source"))
				if(c == citation)
					return rec;
		}
		return null;
	}

}
