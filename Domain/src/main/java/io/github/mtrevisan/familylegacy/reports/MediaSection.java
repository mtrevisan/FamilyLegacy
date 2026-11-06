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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportSection;

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
public final class MediaSection implements SectionBuilder{

	private final ReportContext ctx;


	public MediaSection(final ReportContext ctx){
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
		//(in a multi-person report each person shows the own image)
		if(!ctx.config.includePictures() || ctx.subjects().size() > 1)
			return;

		final FLEFRecord preferred = FLEFRecordHelper.findChild(ctx.root, IndividualReader.TAG_PREFERRED_IMAGE);
		if(preferred == null)
			return;
		final String uri = MediaReader.extractUri(preferred);
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
			final String uri = MediaReader.extractUri(doc);
			if(uri == null)
				continue;

			final String description = FLEFRecordHelper.getChildValue(doc, MediaReader.TAG_DESCRIPTION);
			final String mapping = FLEFRecordHelper.getChildValue(doc, MediaReader.TAG_MAPPING);

			//without pictures, the document is listed as a link to its file
			final String link = "[" + ReportFormatters.escape(docId) + "](" + uri + ")";
			if(ctx.config.includePictures()){
				try{
					out.add(new ReportSection.Image(Path.of(uri),
						description != null? description: docId));
				}
				catch(final Exception ignored){
					out.add(new ReportSection.Paragraph(link));
				}
			}
			else
				out.add(new ReportSection.Paragraph(link));

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
			for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(src, SourceReader.TAG_MEDIA))
				if(docRef.getValue() != null)
					docIds.add(docRef.getValue());
		}

		for(final FLEFRecord individual : individualRecords())
			for(final FLEFRecord sourceCitation : FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_SOURCE))
				for(final FLEFRecord extract : FLEFRecordHelper.findChildren(sourceCitation, SourceCitationReader.TAG_EXTRACT))
					for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(extract, SourceCitationReader.TAG_MEDIA_PART_MEDIA))
						if(docRef.getValue() != null)
							docIds.add(docRef.getValue());
		return docIds;
	}


	private List<FLEFRecord> individualRecords(){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord subject : ctx.subjects()){
			out.add(subject);
			if(ReportContext.rootKindOf(subject) == ReportContext.RootKind.GROUP){
				out.addAll(ctx.index.eventsOfGroup(subject));
				out.addAll(ctx.index.attributesOfGroup(subject));
			}
			else{
				out.addAll(ctx.index.eventsOf(subject));
				out.addAll(ctx.index.attributesOf(subject));
			}
		}
		return out;
	}

}
