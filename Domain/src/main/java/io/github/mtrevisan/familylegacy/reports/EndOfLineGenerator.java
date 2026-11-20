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
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import io.github.mtrevisan.familylegacy.reports.index.ReportFormatters;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


/**
 * Generates the "End of Line" report of an individual: the ancestors with no known parents (the "brick walls"), by generation, with
 * the dates and the places of birth and death, to see where the research of the line has to continue.
 * <p>
 * The ancestors are followed as far as the number of generations of the configuration, but an ancestor is at the end of the line only if
 * the file has no parent of theirs at all, so the last generation shown is not listed for being the last. An individual the privacy policy
 * hides is not an ancestor, one it shows with the name and the role only has no dates and places.
 * </p>
 */
public final class EndOfLineGenerator{

	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public EndOfLineGenerator(final FLEFModel model, final FLEFRecord root, final ReportConfig config){
		this.model = Objects.requireNonNull(model, "model");
		this.root = Objects.requireNonNull(root, "root");
		this.config = Objects.requireNonNull(config, "config");
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportContext ctx = ReportContext.build(model, root, config);
		final ReportLabels labels = ctx.labels;
		final RelationIndex index = ctx.index;
		final String title = labels.sections().endOfLineTitle();
		final String language = labels.language().locale().toLanguageTag();

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, labels.sections().overview()));
		listener.onProgress(10, null);

		final List<List<FLEFRecord>> levels = index.ancestorLevels(root, config.generations());
		int ancestors = 0;
		final List<List<String>> rows = new ArrayList<>();
		//(the root is the first generation: it is not an ancestor of itself)
		for(int generation = 1; generation < levels.size(); generation ++){
			final List<FLEFRecord> level = levels.get(generation);
			ancestors += level.size();

			final List<FLEFRecord> ends = new ArrayList<>(level.stream()
				.filter(person -> index.parentsOf(person).isEmpty())
				.toList());
			ends.sort(Comparator.comparing(ctx::displayText, ReportCollation.of(labels.language().locale())));
			for(final FLEFRecord person : ends){
				if(Thread.currentThread().isInterrupted())
					throw new RuntimeException("Report generation cancelled");

				rows.add(row(ctx, person, generation));
			}
		}
		listener.onProgress(80, null);

		if(ancestors == 0)
			sections.add(new ReportSection.Paragraph(labels.sections().endOfLineNoAncestors()));
		else if(rows.isEmpty())
			sections.add(new ReportSection.Paragraph(String.format(labels.sections().endOfLineNone(), ctx.formatInt(ancestors))));
		else{
			sections.add(new ReportSection.Paragraph(String.format(labels.sections().endOfLineIntro(), ctx.formatInt(rows.size()),
				ctx.formatInt(ancestors), ctx.formatInt(levels.size() - 1))));
			sections.add(new ReportSection.Table(List.of(labels.sections().endOfLineColumn("GENERATION"),
				labels.sections().endOfLineColumn("NAME"), labels.sections().endOfLineColumn("BORN"),
				labels.sections().endOfLineColumn("BIRTH_PLACE"), labels.sections().endOfLineColumn("DIED"),
				labels.sections().endOfLineColumn("DEATH_PLACE")), rows));
			sections.add(new ReportSection.Paragraph("*" + labels.sections().endOfLineNote() + "*"));
		}
		listener.onProgress(100, null);

		return new ReportDocument(title, ReportFormatters.escape(ctx.displayText(root)), sections, language, labels.sections().contents());
	}


	private List<String> row(final ReportContext ctx, final FLEFRecord person, final int generation){
		//a person shown with the name and the role only has nothing else
		final boolean redacted = ctx.isRedacted(person);
		final FLEFRecord birth = (redacted? null: FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_BIRTH));
		final FLEFRecord death = (redacted? null: FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_DEATH));
		return List.of(ctx.formatInt(generation), ReportFormatters.escape(ctx.displayText(person)),
			FamilyGroupSheetGenerator.date(ctx, birth), FamilyGroupSheetGenerator.place(model, birth),
			FamilyGroupSheetGenerator.date(ctx, death), FamilyGroupSheetGenerator.place(model, death));
	}

}
