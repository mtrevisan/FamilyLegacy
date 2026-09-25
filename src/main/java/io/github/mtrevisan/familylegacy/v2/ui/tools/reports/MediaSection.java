package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Builds the Media section: preferred image plus every document reachable
 * from the individual's citations and extracts. Documents whose record is
 * hidden by the privacy policy are skipped.
 */
final class MediaSection implements SectionBuilder{

	private static final String TAG_URI = "uri";
	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_MAPPING = "mapping";
	private static final String TAG_DOCUMENT = "document";
	private static final String TAG_EXTRACT = "extract";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_DOC_PART = "document_part";


	private final ReportContext ctx;


	MediaSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.media())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.media()));

		appendPreferredImage(out);
		appendDocuments(out);

		if(out.size() == 1)
			out.add(new ReportSection.Paragraph(ctx.labels.empty()));
		return out;
	}


	private void appendPreferredImage(final List<ReportSection> out){
		final FLEFRecord preferred = FLEFRecordHelper.findChild(ctx.root, "preferred_image");
		if(preferred == null)
			return;
		final String uri = FLEFRecordHelper.getChildValue(preferred, TAG_URI);
		if(uri == null)
			return;

		final String caption = ReportFormatters.imageCaption(
			preferred, ctx.labels.preferredImage(), ctx.labels);
		try{
			out.add(new ReportSection.Image(Path.of(uri), caption));
		}
		catch(final Exception ignored){ /* skip */ }
	}


	private void appendDocuments(final List<ReportSection> out){
		for(final String docId : collectDocumentIds()){
			final FLEFRecord doc = ctx.visible(ctx.model.getRecordById(docId));
			if(doc == null)
				continue;
			final String uri = FLEFRecordHelper.getChildValue(doc, TAG_URI);
			if(uri == null)
				continue;

			final String description = FLEFRecordHelper.getChildValue(doc, TAG_DESCRIPTION);
			final String mapping = FLEFRecordHelper.getChildValue(doc, TAG_MAPPING);

			try{
				out.add(new ReportSection.Image(Path.of(uri),
					description != null? description: docId));
			}
			catch(final Exception ignored){
				out.add(new ReportSection.Paragraph(
					"[" + ReportFormatters.escape(docId) + "](" + uri + ")"));
			}

			if(ctx.config.includeDescriptions() && (description != null || mapping != null)){
				final List<String> rows = new ArrayList<>();
				ReportFormatters.appendIfPresent(rows, ctx.labels.documentUri(), uri);
				ReportFormatters.appendIfPresent(rows, ctx.labels.documentDescription(), description);
				ReportFormatters.appendIfPresent(rows, ctx.labels.documentMapping(),
					ReportFormatters.enumLabel(mapping));
				if(!rows.isEmpty())
					out.add(new ReportSection.BulletList(rows));
			}
		}
	}


	private Set<String> collectDocumentIds(){
		final Set<String> docIds = new LinkedHashSet<>();

		for(final String sid : SourceCollector.collect(ctx)){
			final FLEFRecord src = ctx.visible(ctx.model.getRecordById(sid));
			if(src == null)
				continue;
			for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(src, TAG_DOCUMENT))
				if(docRef.getValue() != null)
					docIds.add(docRef.getValue());
		}

		for(final FLEFRecord rec : individualRecords()){
			for(final FLEFRecord extract : FLEFRecordHelper.findChildren(rec,
				TAG_SOURCE + "." + TAG_EXTRACT)){
				for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(extract,
					TAG_DOC_PART + "." + TAG_DOCUMENT))
					if(docRef.getValue() != null)
						docIds.add(docRef.getValue());
			}
		}
		return docIds;
	}


	private List<FLEFRecord> individualRecords(){
		final List<FLEFRecord> out = new ArrayList<>();
		out.add(ctx.root);
		out.addAll(ctx.index.eventsOf(ctx.root));
		out.addAll(ctx.index.attributesOf(ctx.root));
		return out;
	}

}
