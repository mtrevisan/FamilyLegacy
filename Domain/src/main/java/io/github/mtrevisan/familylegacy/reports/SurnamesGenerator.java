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
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.List;


/**
 * Generates the "Surnames by Place" report of the whole file: for each place the surnames of the people who come from it, how many
 * they are and the years they were born in (see {@link SurnameDistribution}), to take to the archive of the place.
 * <p>
 * The individuals the privacy policy of the report hides are left out.
 * </p>
 */
public final class SurnamesGenerator{

	/** The en dash that joins the first and the last year. */
	private static final String RANGE_SEPARATOR = "–";
	private static final String NO_VALUE = "—";

	private final FLEFModel model;
	private final ReportConfig config;


	public SurnamesGenerator(final FLEFModel model, final ReportConfig config){
		this.model = model;
		this.config = config;
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language());
		final String title = labels.sections().surnamesTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().empty())), language, labels.sections().contents());

		final RelationIndex index = RelationIndex.build(model, r -> true);
		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config, index);
		listener.onProgress(10, null);

		final SurnameDistribution.Result distribution = SurnameDistribution.of(model, index, ctx::isVisible, labels.language().locale());
		listener.onProgress(80, null);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, ctx.labels.sections().overview()));
		if(distribution.individuals() == 0)
			sections.add(new ReportSection.Paragraph(ctx.labels.sections().surnamesEmpty()));
		else{
			final int places = (int)distribution.places().stream()
				.filter(group -> group.placeId() != null)
				.count();
			sections.add(new ReportSection.Paragraph(String.format(ctx.labels.sections().surnamesIntro(),
				ctx.formatInt(distribution.individuals()), ctx.formatInt(distribution.surnames()), ctx.formatInt(places))));

			for(final SurnameDistribution.PlaceGroup group : distribution.places()){
				if(Thread.currentThread().isInterrupted())
					throw new RuntimeException("Report generation cancelled");

				final String name = (group.placeId() != null? group.placeName(): ctx.labels.sections().surnamesNoPlace());
				sections.add(new ReportSection.Heading(2, ReportFormatters.escape(name) + " (" + ctx.formatInt(group.individuals()) + ")"));
				sections.add(table(ctx, group));
			}
			sections.add(new ReportSection.Paragraph("*" + ctx.labels.sections().surnamesNote() + "*"));
		}
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	private static ReportSection table(final ReportContext ctx, final SurnameDistribution.PlaceGroup group){
		final List<List<String>> rows = new ArrayList<>(group.rows().size());
		for(final SurnameDistribution.Row row : group.rows())
			rows.add(List.of(ReportFormatters.escape(row.surname()), ctx.formatInt(row.individuals()), period(row),
				(row.undated() > 0? ctx.formatInt(row.undated()): NO_VALUE)));
		return new ReportSection.Table(List.of(ctx.labels.sections().surnamesColumn("SURNAME"), ctx.labels.sections().surnamesColumn("COUNT"),
			ctx.labels.sections().surnamesColumn("PERIOD"), ctx.labels.sections().surnamesColumn("UNDATED")), rows);
	}

	/** @return	The first and the last year ("1780–1850"), the year if they are the same, a dash if no one has a date. */
	private static String period(final SurnameDistribution.Row row){
		if(row.firstYear() == null)
			return NO_VALUE;
		return (row.firstYear().equals(row.lastYear())? row.firstYear().toString(): row.firstYear() + RANGE_SEPARATOR + row.lastYear());
	}

}
