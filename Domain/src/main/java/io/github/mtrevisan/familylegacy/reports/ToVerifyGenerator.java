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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Generates the "To Verify" report of the whole file: what the researcher has still to look at (see {@link ToVerifyFinder}), a summary
 * of the groups of findings and, for each group, a table of the records.
 * <p>
 * The records the privacy policy of the report hides are left out.
 * </p>
 */
public final class ToVerifyGenerator{

	private final FLEFModel model;
	private final ReportConfig config;
	private final String mediaBaseDirectory;


	public ToVerifyGenerator(final FLEFModel model, final ReportConfig config){
		this(model, config, MediaPathDisplay.baseDirectory());
	}

	/** @param mediaBaseDirectory	The directory that the relative locations of the media are relative to ({@code null}: they are not judged). */
	public ToVerifyGenerator(final FLEFModel model, final ReportConfig config, final String mediaBaseDirectory){
		this.model = model;
		this.config = config;
		this.mediaBaseDirectory = mediaBaseDirectory;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language());
		final String title = labels.sections().toVerifyTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config);
		listener.onProgress(5, null);

		final List<ToVerifyFinder.Finding> findings = ToVerifyFinder.find(model, ctx::isVisible, mediaBaseDirectory);
		listener.onProgress(70, null);

		final Map<ToVerifyFinder.Kind, List<ToVerifyFinder.Finding>> byKind = new EnumMap<>(ToVerifyFinder.Kind.class);
		for(final ToVerifyFinder.Finding finding : findings)
			byKind.computeIfAbsent(finding.kind(), k -> new ArrayList<>())
				.add(finding);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, ctx.labels.sections().overview()));
		if(findings.isEmpty())
			sections.add(new ReportSection.Paragraph(ctx.labels.sections().toVerifyNothing()));
		else{
			sections.add(new ReportSection.Paragraph(String.format(ctx.labels.sections().toVerifyIntro(),
				ctx.formatInt(findings.size()), ctx.formatInt(byKind.size()))));
			sections.add(summary(ctx, byKind));
			for(final Map.Entry<ToVerifyFinder.Kind, List<ToVerifyFinder.Finding>> entry : byKind.entrySet()){
				if(Thread.currentThread().isInterrupted())
					throw new RuntimeException("Report generation cancelled");

				sections.add(new ReportSection.Heading(2, ctx.labels.sections().toVerifyKind(entry.getKey().key())
					+ " (" + ctx.formatInt(entry.getValue().size()) + ")"));
				sections.add(table(ctx, entry.getValue()));
			}
		}
		sections.add(new ReportSection.Paragraph("*" + ctx.labels.sections().toVerifyNotIncluded() + "*"));
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	/** The table of the groups: what and how many. */
	private static ReportSection summary(final ReportContext ctx, final Map<ToVerifyFinder.Kind, List<ToVerifyFinder.Finding>> byKind){
		final List<List<String>> rows = new ArrayList<>(byKind.size());
		for(final Map.Entry<ToVerifyFinder.Kind, List<ToVerifyFinder.Finding>> entry : byKind.entrySet())
			rows.add(List.of(ctx.labels.sections().toVerifyKind(entry.getKey().key()), ctx.formatInt(entry.getValue().size())));
		return new ReportSection.Table(List.of(ctx.labels.sections().toVerifyColumn("GROUP"),
			ctx.labels.sections().toVerifyColumn("COUNT")), rows);
	}

	/** The records of a group, by what is shown of each one, with what is to say about it. */
	private ReportSection table(final ReportContext ctx, final List<ToVerifyFinder.Finding> findings){
		final Locale locale = ctx.labels.language().locale();
		final List<List<String>> rows = new ArrayList<>(findings.size());
		final List<ToVerifyFinder.Finding> ordered = new ArrayList<>(findings);
		ordered.sort(Comparator.comparing((final ToVerifyFinder.Finding finding) -> subject(ctx, finding), ReportCollation.of(locale))
			.thenComparing(finding -> StringUtils.defaultString(finding.recordId())));
		//the column of the detail is left out when no finding has anything to say (most groups are only a list of records)
		final boolean withDetail = ordered.stream()
			.anyMatch(finding -> !finding.detail().isBlank());
		for(final ToVerifyFinder.Finding finding : ordered)
			rows.add(withDetail
				? List.of(ReportFormatters.escape(subject(ctx, finding)), ReportFormatters.escape(finding.detail()))
				: List.of(ReportFormatters.escape(subject(ctx, finding))));
		return new ReportSection.Table((withDetail
			? List.of(ctx.labels.sections().toVerifyColumn("RECORD"), ctx.labels.sections().toVerifyColumn("DETAIL"))
			: List.of(ctx.labels.sections().toVerifyColumn("RECORD"))), rows);
	}

	/** @return	What is shown of the record the finding is about: its text, or its identifier if it is not there. */
	private String subject(final ReportContext ctx, final ToVerifyFinder.Finding finding){
		if(finding.recordId() == null)
			return StringUtils.EMPTY;

		final FLEFRecord record = model.getRecordById(finding.recordId());
		return (record != null? ctx.displayText(record): finding.recordId());
	}

}
