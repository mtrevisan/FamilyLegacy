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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateService;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Generates the bibliography of the whole file: a table of every source (title, author, date and how many times it is cited), then an
 * entry for each source, in alphabetical order, with what it is (author, publisher, date, place), the repositories that hold it, its
 * documents and the records that cite it.
 * <p>
 * The sources the privacy policy hides are left out.
 * </p>
 */
public final class FullBibliographyGenerator{

	/** The deepest heading the renderers write. */
	private static final int MAX_HEADING_LEVEL = 6;

	private final FLEFModel model;
	private final ReportConfig config;


	public FullBibliographyGenerator(final FLEFModel model, final ReportConfig config){
		this.model = model;
		this.config = config;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language());
		final String title = labels.sections().fullBibliographyTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecordsByType(SourceReader.TYPE);
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final ReportContext first = ReportContext.build(model, all.getFirst(), config);
		final RelationIndex index = first.index;
		final List<FLEFRecord> sources = new ArrayList<>();
		for(final FLEFRecord source : all)
			if(first.isVisible(source))
				sources.add(source);
		sources.sort(Comparator.comparing((final FLEFRecord source) -> SourceReader.displayText(source).toLowerCase(first.labels.language().locale()))
			.thenComparing(FLEFRecord::getId));

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(first).build());
		sections.add(new ReportSection.Heading(1, first.labels.sections().overview()));
		sections.add(new ReportSection.Paragraph(String.format(first.labels.sections().sourcesCount(), first.formatInt(sources.size()))));
		sections.add(summary(first, index, sources));
		listener.onProgress(5, null);

		for(int i = 0; i < sources.size(); i ++){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			final FLEFRecord source = sources.get(i);
			for(final ReportSection section : new SourceRootSection(first.withRoot(source)).build())
				sections.add(demoted(section));
			listener.onProgress(5 + (i + 1) * 95 / sources.size(), SourceReader.displayText(source));
		}

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	/** The table of every source: title, author, date and number of citations. */
	private static ReportSection summary(final ReportContext ctx, final RelationIndex index, final List<FLEFRecord> sources){
		final List<List<String>> rows = new ArrayList<>(sources.size());
		for(final FLEFRecord source : sources)
			rows.add(List.of(
				ReportFormatters.escape(SourceReader.displayText(source)),
				ReportFormatters.escape(ReportFormatters.orEmpty(ReportFormatters.firstValue(source, SourceReader.TAG_AUTHOR))),
				ReportFormatters.escape(ReportFormatters.orEmpty(DateService.getDateDisplayText(source))),
				ctx.formatInt(index.citationsOfSource(source).size())));
		return new ReportSection.Table(List.of(ctx.labels.sections().title(), ctx.labels.sections().sourceAuthor(),
			ctx.labels.sections().sourceDate(), ctx.labels.sections().citations()), rows);
	}

	/** A heading one level lower, because each source is a part of the bibliography. */
	private static ReportSection demoted(final ReportSection section){
		return (section instanceof ReportSection.Heading(int level, String text)
			? new ReportSection.Heading(Math.min(level + 1, MAX_HEADING_LEVEL), text): section);
	}

}
