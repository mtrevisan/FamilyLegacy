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

import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
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
 * Builds the Media section: preferred image, the media attached to the record (see MediaCitation) and every document reachable
 * from the individual's citations and extracts. Documents whose record is
 * hidden by the privacy policy are skipped.
 */
public final class MediaSection implements SectionBuilder{

	private final ReportContext ctx;
	/** Whether the section is an appendix of the report (it comes after the indexes). */
	private final boolean appendix;


	public MediaSection(final ReportContext ctx){
		this(ctx, false);
	}

	/** @param appendix	Whether the section is an appendix of the report: the title says so. */
	MediaSection(final ReportContext ctx, final boolean appendix){
		this.ctx = ctx;
		this.appendix = appendix;
	}


	@Override
	public List<ReportSection> build(){
		//(what the chapters of the persons already have is not told again)
		if(!ctx.config.media() || toTheChapters())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, (appendix? ctx.labels.sections().appendix() + ": " + ctx.labels.media(): ctx.labels.media())));

		appendPreferredImage(out);
		appendDocuments(out);

		if(out.size() == 1)
			out.add(new ReportSection.Paragraph(ctx.labels.sections().empty()));
		return out;
	}

	/**
	 * @return	The section of the media of the person the context is about, for the chapter of the person: nothing if the report does not
	 * 	put the media in the chapters, or the person has none.
	 */
	List<ReportSection> inline(){
		if(!ctx.config.media() || ctx.config.mediaPlacement() != MediaPlacement.INLINE || !onlyPersons())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(2, ctx.labels.media()));
		appendDocuments(out);
		return (out.size() > 1? out: List.of());
	}

	/** @return	Whether the media of the persons are in their chapters (so that the section of the media has nothing of theirs). */
	private boolean toTheChapters(){
		return ctx.config.mediaPlacement() == MediaPlacement.INLINE && !appendix && onlyPersons();
	}

	private boolean onlyPersons(){
		for(final FLEFRecord subject : ctx.subjects())
			if(ReportContext.rootKindOf(subject) != ReportContext.RootKind.INDIVIDUAL)
				return false;
		return true;
	}


	private void appendPreferredImage(final List<ReportSection> out){
		//(in a multi-person report each person shows the own image)
		if(!ctx.config.includePictures() || ctx.subjects().size() > 1 || ctx.isRedacted(ctx.root))
			return;

		final FLEFRecord preferred = FLEFRecordHelper.findChild(ctx.root, IndividualReader.TAG_PREFERRED_IMAGE);
		if(preferred == null)
			return;
		final String uri = MediaReader.extractUri(preferred);
		if(uri == null)
			return;

		try{
			out.add(Portraits.image(ctx, ctx.root, preferred, uri, ctx.labels.sections().preferredImage()));
		}
		catch(final Exception ignored){ /* skip */ }
	}


	private void appendDocuments(final List<ReportSection> out){
		final int perPage = (ctx.config.mediaPlacement() == MediaPlacement.SEPARATE_PAGES? 1: ctx.config.imagesPerPage());
		int shown = 0;
		for(final String docId : collectDocumentIds()){
			final FLEFRecord doc = ctx.visible(ctx.model.getRecordById(docId));
			if(doc == null)
				continue;
			final String uri = MediaReader.extractUri(doc);
			if(uri == null)
				continue;

			//(a new page after as many images as a page holds)
			if(perPage > 0 && shown > 0 && shown % perPage == 0)
				out.add(ReportSection.PageBreak.INSTANCE);
			shown ++;

			final String description = FLEFRecordHelper.getChildValue(doc, MediaReader.TAG_DESCRIPTION);
			final String mapping = FLEFRecordHelper.getChildValue(doc, MediaReader.TAG_MAPPING);

			//without pictures, the document is listed as a link to its file
			final String link = "[" + ReportFormatters.escape(docId) + "](" + ReportFormatters.linkTarget(uri) + ")";
			if(ctx.config.includePictures()){
				try{
					out.add(new ReportSection.Image(ReportImages.prepare(Path.of(uri), null, 0, ctx.config.maxImageMegapixels()),
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
				//(the name of the file is not told if the description is that name already)
				final String fileName = ReportFormatters.fileNameOf(uri);
				if(!ReportFormatters.isDescribedBy(description, fileName))
					ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentUri(), fileName);
				ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentDescription(), description);
				ReportFormatters.appendIfPresent(rows, ctx.labels.sections().documentMapping(),
					ctx.labels.enumValue("enum.media.mapping", mapping));
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

		for(final FLEFRecord individual : individualRecords()){
			//(an individual that may be living, shown with the name only, has no documents)
			if(ctx.isRedacted(individual))
				continue;

			//the media attached to the record (the subject, its events and attributes) come first
			for(final MediaCitationReader.MediaUse use : MediaCitationReader.extractAll(individual, ctx.model))
				docIds.add(use.mediaId());
			for(final FLEFRecord sourceCitation : FLEFRecordHelper.findChildren(individual, IndividualReader.TAG_SOURCE))
				for(final FLEFRecord extract : ctx.visible(ExtractReader.extractsOf(sourceCitation, ctx.model)))
					for(final FLEFRecord docRef : FLEFRecordHelper.findChildren(extract, ExtractReader.TAG_MEDIA_PART_MEDIA))
						if(docRef.getValue() != null)
							docIds.add(docRef.getValue());
		}

		//the picture a portrait is cut from, if only the portrait is wanted
		for(final FLEFRecord subject : ctx.subjects())
			if(ReportContext.rootKindOf(subject) == ReportContext.RootKind.INDIVIDUAL && !ctx.isRedacted(subject)){
				final String original = Portraits.omittedOriginalOf(ctx, subject);
				if(original != null)
					docIds.remove(original);
			}
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
