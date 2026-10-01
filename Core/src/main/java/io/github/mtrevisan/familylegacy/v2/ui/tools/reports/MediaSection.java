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
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
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
			preferred, ctx.labels.sections().preferredImage(), ctx.labels);
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
				ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentUri(), uri);
				ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentDescription(), description);
				ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentMapping(),
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
