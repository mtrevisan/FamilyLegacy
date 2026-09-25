package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Builds the main section for a document report (root = {@code document} record).
 *
 * <p>Renders:</p>
 * <ul>
 *   <li>the document's URI, description and mapping;</li>
 *   <li>an embedded preview when the URI points to a local image;</li>
 *   <li>every source that references this document directly;</li>
 *   <li>every citation that references this document through an
 *       {@code extract.document_part.document} node;</li>
 *   <li>notes, audit and privacy of the document itself.</li>
 * </ul>
 */
final class DocumentRootSection implements SectionBuilder{

	private static final String TAG_URI = "uri";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_MAPPING = "mapping";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_LOCATOR = "locator";


	private final ReportContext ctx;


	DocumentRootSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.isDocumentRoot())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		final String title = documentTitle(ctx.root);
		out.add(new ReportSection.Heading(1, String.format(
			ctx.labels.documentOf(), ReportFormatters.escape(title))));

		writePreview(out);
		writeBasicInfo(out);
		writeReferencingSources(out);
		writeReferencingCitations(out);

		if(ctx.config.notes())
			out.addAll(ctx.citations().notes(ctx.root));
		out.addAll(ctx.citations().audit(ctx.root));
		out.addAll(ctx.citations().privacyInfo(ctx.root));
		return out;
	}


	private void writePreview(final List<ReportSection> out){
		final String uri = FLEFRecordHelper.getChildValue(ctx.root, TAG_URI);
		if(uri == null || uri.isBlank())
			return;
		try{
			out.add(new ReportSection.Image(Path.of(uri), documentTitle(ctx.root)));
		}
		catch(final Exception ignored){ /* not a local file */ }
	}


	private void writeBasicInfo(final List<ReportSection> out){
		final List<String> rows = new ArrayList<>();
		ReportFormatters.appendIfPresent(rows, ctx.labels.documentUri(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_URI));
		ReportFormatters.appendIfPresent(rows, ctx.labels.documentDescription(),
			FLEFRecordHelper.getChildValue(ctx.root, TAG_DESCRIPTION));
		ReportFormatters.appendIfPresent(rows, ctx.labels.documentMapping(),
			ReportFormatters.enumLabel(
				FLEFRecordHelper.getChildValue(ctx.root, TAG_MAPPING)));
		if(!rows.isEmpty())
			out.add(new ReportSection.BulletList(rows));
	}


	private void writeReferencingSources(final List<ReportSection> out){
		final List<FLEFRecord> sources = ctx.index.sourcesOfDocument(ctx.root);
		if(sources.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.documentReferencedBySources()));
		final List<String> items = new ArrayList<>(sources.size());
		for(final FLEFRecord src : sources)
			items.add(ReportFormatters.escape(ctx.sourceTitle(src.getId())));
		out.add(new ReportSection.BulletList(items));
	}


	private void writeReferencingCitations(final List<ReportSection> out){
		final List<FLEFRecord> citations = ctx.index.citationsOfDocument(ctx.root);
		if(citations.isEmpty())
			return;

		out.add(new ReportSection.Heading(2, ctx.labels.documentReferencedByCitations()));
		final List<String> items = new ArrayList<>(citations.size());
		for(final FLEFRecord cit : citations){
			if(!ctx.isVisible(cit))
				continue;
			final String locator = FLEFRecordHelper.getChildValue(cit, TAG_LOCATOR);
			final StringBuilder sb = new StringBuilder();
			sb.append(ReportFormatters.escape(ReportFormatters.orEmpty(cit.getId())));
			if(locator != null)
				sb.append(" — *").append(ReportFormatters.escape(locator)).append('*');
			items.add(sb.toString());
		}
		if(!items.isEmpty())
			out.add(new ReportSection.BulletList(items));
	}


	private static String documentTitle(final FLEFRecord doc){
		final String desc = FLEFRecordHelper.getChildValue(doc, TAG_DESCRIPTION);
		if(desc != null && !desc.isBlank())
			return desc.trim();
		final String uri = FLEFRecordHelper.getChildValue(doc, TAG_URI);
		if(uri != null && !uri.isBlank()){
			final int slash = uri.lastIndexOf('/');
			return (slash >= 0? uri.substring(slash + 1): uri);
		}
		return ReportFormatters.orEmpty(doc.getId());
	}

}
