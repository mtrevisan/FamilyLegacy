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
 * Generates a single {@link ReportDocument} covering several individuals,
 * one self-contained sub-report per person.
 *
 * <p>Optional opening sections (typically a {@link PedigreeTreeSection})
 * are rendered between the cover and the index of persons, so the reader
 * sees the roster of individuals at a glance before diving into the
 * per-person sub-reports.</p>
 */
public final class MultiPersonReportGenerator{

	private final FLEFModel model;
	private final List<FLEFRecord> persons;
	/** The groups that have a page after the persons. */
	private final List<FLEFRecord> groups;
	private final ReportConfig config;
	private final String title;
	private final List<SectionBuilder> opening;


	MultiPersonReportGenerator(final FLEFModel model, final List<FLEFRecord> persons,
		final ReportConfig config, final String title){
		this(model, persons, config, title, List.of());
	}


	MultiPersonReportGenerator(final FLEFModel model, final List<FLEFRecord> persons, final ReportConfig config,
			final String title, final List<SectionBuilder> opening){
		this(model, persons, List.of(), config, title, opening);
	}


	/**
	 * @param groups	The groups that have a page after the one of the persons.
	 */
	MultiPersonReportGenerator(final FLEFModel model, final List<FLEFRecord> persons, final List<FLEFRecord> groups,
			final ReportConfig config, final String title, final List<SectionBuilder> opening){
		this.model = model;
		this.persons = List.copyOf(persons);
		this.groups = List.copyOf(groups);
		this.config = config;
		this.title = title;
		this.opening = List.copyOf(opening);
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}


	public ReportDocument generate(final ReportProgressListener listener){
		if(persons.isEmpty() && groups.isEmpty())
			return placeholder();

		final RelationIndex sharedIndex = RelationIndex.build(model, r -> true);

		final ReportContext first = ReportContext.build(model, (persons.isEmpty()? groups.getFirst(): persons.getFirst()),
			config, sharedIndex);
		final List<ReportSection> sections = new ArrayList<>();

		sections.addAll(new HeaderSection(first).build());
		sections.add(new ReportSection.Heading(1, title));
		sections.add(new ReportSection.Paragraph(first.labels.sections().multiPersonCover(persons.size())));

		for(final SectionBuilder b : opening)
			sections.addAll(b.build());

		if(config.indexIndividuals()){
			sections.add(new ReportSection.Heading(1, first.labels.sections().indexOfIndividuals()));
			final List<List<String>> rows = new ArrayList<>(persons.size());
			for(final FLEFRecord person : persons)
				rows.add(List.of(ReportFormatters.escape(first.displayText(person))));
			sections.add(new ReportSection.Table(
				List.of(first.labels.sections().columnIndividual()), rows));
		}

		//the groups have an index of their own, after the one of the persons (the same option decides both)
		if(config.indexIndividuals() && !groups.isEmpty()){
			sections.add(new ReportSection.Heading(1, first.labels.sections().indexOfGroups()));
			final List<List<String>> rows = new ArrayList<>(groups.size());
			for(final FLEFRecord group : groups)
				rows.add(List.of(ReportFormatters.escape(first.displayText(group))));
			sections.add(new ReportSection.Table(List.of(first.labels.sections().columnGroup()), rows));
		}

		// Reserve a small slice for the cover, give the rest to the persons.
		final int coverPercent = 5;
		final int perPerson = (100 - coverPercent) / (persons.size() + groups.size());
		listener.onProgress(coverPercent, null);

		for(int i = 0; i < persons.size(); i ++){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			final FLEFRecord person = persons.get(i);
			final ReportContext ctx = ReportContext.build(model, person, config, sharedIndex);
			sections.add(new ReportSection.PageBreak());
			sections.add(new ReportSection.Heading(1, ctx.displayText(person)));
			sections.addAll(new IntroductionSection(ctx).build());
			sections.addAll(new IndividualLifeStorySection(ctx).build());

			listener.onProgress(coverPercent + (i + 1) * perPerson,
				ctx.displayText(person));
		}

		for(int i = 0; i < groups.size(); i ++){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			final FLEFRecord group = groups.get(i);
			final ReportContext ctx = ReportContext.build(model, group, config, sharedIndex);
			sections.add(new ReportSection.PageBreak());
			sections.add(new ReportSection.Heading(1, ctx.displayText(group)));
			sections.addAll(new GroupIntroductionSection(ctx).build());
			sections.addAll(new GroupStorySection(ctx).build());

			listener.onProgress(coverPercent + (persons.size() + i + 1) * perPerson, ctx.displayText(group));
		}

		//what the persons and the groups cite together, once: the repositories, the sources, the media, the places and their index (the
		//index of the persons is already above)
		final List<FLEFRecord> subjects = new ArrayList<>(persons);
		subjects.addAll(groups);
		final ReportContext all = ReportContext.buildForSubjects(model, config.withIndexIndividuals(false), sharedIndex, subjects);
		sections.addAll(new RepositorySection(all).build());
		sections.addAll(new SourcesSection(all).build());
		sections.addAll(new MediaSection(all).build());
		sections.addAll(new PlacesSection(all).build());
		sections.addAll(new IndexSection(all).build());

		return new ReportDocument(title, null, sections, first.labels.language().locale().toLanguageTag(),
			first.labels.sections().contents());
	}


	private ReportDocument placeholder(){
		final List<ReportSection> sections = new ArrayList<>();
		sections.add(new ReportSection.Heading(1, title));
		sections.add(new ReportSection.Paragraph(
			"No individual is available for this report. "
				+ "Select a person in the editor and try again."));
		final ReportLabels labels = new ReportLabels(config.language());
		return new ReportDocument(title, null, sections, labels.language().locale().toLanguageTag(), labels.sections().contents());
	}

}
