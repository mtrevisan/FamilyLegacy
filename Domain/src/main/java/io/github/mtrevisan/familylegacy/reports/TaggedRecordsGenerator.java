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
import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Generates the "Tagged Records" report of the whole file: for each free tag in use (see {@link RecordTags}), the records that carry it,
 * with their kind. It is the report to take a cross-cutting concern ("Brick Wall", "Needs Translation", ...) out of the file as a list.
 * <p>
 * The records the privacy policy of the report hides are left out.
 * </p>
 */
public final class TaggedRecordsGenerator{

	private final FLEFModel model;
	private final ReportConfig config;


	public TaggedRecordsGenerator(final FLEFModel model, final ReportConfig config){
		this.model = model;
		this.config = config;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language(), config.dateFormat());
		final String title = labels.sections().taggedRecordsTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config);
		listener.onProgress(5, null);

		//the records by tag (the spelling of a tag is the first met, the case is ignored)
		final Map<String, List<FLEFRecord>> byTag = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for(final FLEFRecord record : all){
			if(!RecordTags.isTaggable(record.getTag()) || !ctx.isVisible(record))
				continue;

			for(final String tag : model.tags().get(record.getId()))
				byTag.computeIfAbsent(tag, k -> new ArrayList<>())
					.add(record);
		}
		listener.onProgress(50, null);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, ctx.labels.sections().overview()));
		if(byTag.isEmpty())
			sections.add(new ReportSection.Paragraph(ctx.labels.sections().taggedRecordsNothing()));
		else{
			final int records = (int)byTag.values()
				.stream()
				.flatMap(List::stream)
				.distinct()
				.count();
			sections.add(new ReportSection.Paragraph(String.format(ctx.labels.sections().taggedRecordsIntro(),
				ctx.formatInt(records), ctx.formatInt(byTag.size()))));
			sections.add(summary(ctx, byTag));
			for(final Map.Entry<String, List<FLEFRecord>> entry : byTag.entrySet()){
				if(Thread.currentThread().isInterrupted())
					throw new RuntimeException("Report generation cancelled");

				sections.add(new ReportSection.Heading(2, ReportFormatters.escape(entry.getKey()) + " (" + ctx.formatInt(entry.getValue().size()) + ")"));
				sections.add(table(ctx, entry.getValue()));
			}
		}
		sections.add(new ReportSection.Paragraph("*" + ctx.labels.sections().taggedRecordsNotInFile() + "*"));
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	/** The table of the tags: which and how many records. */
	private static ReportSection summary(final ReportContext ctx, final Map<String, List<FLEFRecord>> byTag){
		final List<List<String>> rows = new ArrayList<>(byTag.size());
		for(final Map.Entry<String, List<FLEFRecord>> entry : byTag.entrySet())
			rows.add(List.of(ReportFormatters.escape(entry.getKey()), ctx.formatInt(entry.getValue().size())));
		return new ReportSection.Table(List.of(ctx.labels.sections().taggedRecordsColumn("TAG"),
			ctx.labels.sections().taggedRecordsColumn("COUNT")), rows);
	}

	/** The records of a tag, by what is shown of each one, with their kind. */
	private static ReportSection table(final ReportContext ctx, final List<FLEFRecord> records){
		final Locale locale = ctx.labels.language().locale();
		final List<FLEFRecord> ordered = new ArrayList<>(records);
		ordered.sort(Comparator.comparing(FLEFRecord::getTag)
			.thenComparing(ctx::displayText, ReportCollation.of(locale))
			.thenComparing(FLEFRecord::getId));
		final List<List<String>> rows = new ArrayList<>(ordered.size());
		for(final FLEFRecord record : ordered)
			rows.add(List.of(ctx.labels.sections().taggedRecordsKind(record.getTag()), ReportFormatters.escape(ctx.displayText(record))));
		return new ReportSection.Table(List.of(ctx.labels.sections().taggedRecordsColumn("KIND"),
			ctx.labels.sections().taggedRecordsColumn("RECORD")), rows);
	}

}
