package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index.RelationIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Generates a single {@link ReportDocument} covering several individuals,
 * one self-contained sub-report per person.
 *
 * <p>Optional opening sections (typically a {@link PedigreeTreeSection})
 * are rendered between the cover and the index of persons, so the reader
 * sees the roster of individuals at a glance before diving into the
 * per-person sub-reports.</p>
 */
final class MultiPersonReportGenerator{

	private final FLEFModel model;
	private final List<FLEFRecord> persons;
	private final ReportConfig config;
	private final String title;
	private final List<SectionBuilder> opening;


	MultiPersonReportGenerator(final FLEFModel model, final List<FLEFRecord> persons,
		final ReportConfig config, final String title){
		this(model, persons, config, title, List.of());
	}


	MultiPersonReportGenerator(final FLEFModel model, final List<FLEFRecord> persons,
		final ReportConfig config, final String title, final List<SectionBuilder> opening){
		this.model = Objects.requireNonNull(model);
		this.persons = List.copyOf(persons);
		this.config = Objects.requireNonNull(config);
		this.title = Objects.requireNonNull(title);
		this.opening = List.copyOf(opening);
	}


	ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}


	ReportDocument generate(final ReportProgressListener listener){
		if(persons.isEmpty())
			return placeholder();

		final RelationIndex sharedIndex = RelationIndex.build(model, r -> true);

		final ReportContext first = ReportContext.build(model, persons.getFirst(),
			config, sharedIndex);
		final List<ReportSection> sections = new ArrayList<>();

		sections.addAll(new HeaderSection(first).build());
		sections.add(new ReportSection.Heading(1, title));
		sections.add(new ReportSection.Paragraph(String.format(
			first.labels.sections().multiPersonCover(),
			first.formatInt(persons.size()))));

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

		// Reserve a small slice for the cover, give the rest to the persons.
		final int coverPercent = 5;
		final int perPerson = (100 - coverPercent) / Math.max(1, persons.size());
		listener.onProgress(coverPercent, null);

		for(int i = 0; i < persons.size(); i++){
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

		return new ReportDocument(title, null, sections);
	}


	private ReportDocument placeholder(){
		final List<ReportSection> sections = new ArrayList<>();
		sections.add(new ReportSection.Heading(1, title));
		sections.add(new ReportSection.Paragraph(
			"No individual is available for this report. "
				+ "Select a person in the editor and try again."));
		return new ReportDocument(title, null, sections);
	}

}
