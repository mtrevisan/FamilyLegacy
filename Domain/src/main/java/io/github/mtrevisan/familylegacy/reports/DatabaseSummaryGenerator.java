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

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HeaderReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


/**
 * Generates the "Database Summary" report of the whole file: how many records there are of each kind, how complete the individuals are
 * (how many have a surname, a sex, a birth date and place, a death date, known parents, a source) and the period the events cover.
 * <p>
 * It counts what the privacy policy of the report shows, and it judges nothing: it tells what the file has, not whether it is right (the
 * coverage report and the "To Verify" report are for that).
 * </p>
 */
public final class DatabaseSummaryGenerator{

	private static final String NO_VALUE = "—";


	/**
	 * What the file has.
	 *
	 * @param byKind	the number of records of each kind (the tag of the records), the most numerous first
	 * @param individuals	the individuals shown
	 * @param withSurname	how many have a surname
	 * @param withSex	how many have a known sex
	 * @param withBirthDate	how many have a birth with a year
	 * @param withBirthPlace	how many have a birth with a place
	 * @param withDeathDate	how many have a death with a year
	 * @param withParents	how many have at least one known parent
	 * @param withSource	how many are backed by a source citation
	 * @param events	the events shown
	 * @param datedEvents	how many events have a year
	 * @param firstEvent	the first year of an event, {@code null} if no event has one
	 * @param lastEvent	the last year of an event
	 * @param firstBirth	the first year of a birth
	 * @param lastBirth	the last year of a birth
	 */
	record Summary(Map<String, Integer> byKind, int individuals, int withSurname, int withSex, int withBirthDate, int withBirthPlace,
			int withDeathDate, int withParents, int withSource, int events, int datedEvents, Integer firstEvent, Integer lastEvent,
			Integer firstBirth, Integer lastBirth){

		int records(){
			return byKind.values().stream()
				.mapToInt(Integer::intValue)
				.sum();
		}
	}


	private final FLEFModel model;
	private final ReportConfig config;


	public DatabaseSummaryGenerator(final FLEFModel model, final ReportConfig config){
		this.model = Objects.requireNonNull(model, "model");
		this.config = Objects.requireNonNull(config, "config");
	}


	public ReportDocument generate(){
		return generate(ReportProgressListener.NOOP);
	}

	public ReportDocument generate(final ReportProgressListener listener){
		final ReportLabels labels = new ReportLabels(config.language(), config.dateFormat());
		final String title = labels.sections().databaseSummaryTitle();
		final Locale locale = labels.language().locale();
		final String language = locale.toLanguageTag();

		final List<FLEFRecord> all = model.getRecords();
		if(all.isEmpty())
			return new ReportDocument(title, null, List.of(new ReportSection.Heading(1, title),
				new ReportSection.Paragraph(labels.sections().databaseSummaryEmpty())), language, labels.sections().contents());

		final ReportContext ctx = ReportContext.build(model, all.getFirst(), config);
		listener.onProgress(10, null);

		final Summary summary = summarize(ctx);
		listener.onProgress(80, null);

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, labels.sections().overview()));
		if(summary.records() == 0)
			sections.add(new ReportSection.Paragraph(labels.sections().databaseSummaryEmpty()));
		else{
			sections.add(new ReportSection.Paragraph(String.format(labels.sections().databaseSummaryIntro(),
				ctx.formatInt(summary.records()), ctx.formatInt(summary.byKind().size()), ctx.formatInt(summary.individuals()))));

			sections.add(new ReportSection.Heading(2, labels.sections().databaseSummaryKinds()));
			final List<List<String>> kinds = new ArrayList<>(summary.byKind().size());
			for(final Map.Entry<String, Integer> entry : summary.byKind().entrySet())
				kinds.add(List.of(StringUtils.capitalize(labels.recordKind(entry.getKey())), ctx.formatInt(entry.getValue())));
			sections.add(new ReportSection.Table(List.of(labels.sections().databaseSummaryColumn("KIND"),
				labels.sections().databaseSummaryColumn("RECORDS")), kinds));

			if(summary.individuals() > 0){
				sections.add(new ReportSection.Heading(2, labels.sections().databaseSummaryCompleteness()));
				final int total = summary.individuals();
				sections.add(new ReportSection.Table(List.of(labels.sections().databaseSummaryColumn("MEASURE"),
					labels.sections().databaseSummaryColumn("INDIVIDUALS"), labels.sections().databaseSummaryColumn("SHARE")),
					List.of(
						completeness(ctx, "SURNAME", summary.withSurname(), total),
						completeness(ctx, "SEX", summary.withSex(), total),
						completeness(ctx, "BIRTH_DATE", summary.withBirthDate(), total),
						completeness(ctx, "BIRTH_PLACE", summary.withBirthPlace(), total),
						completeness(ctx, "DEATH_DATE", summary.withDeathDate(), total),
						completeness(ctx, "PARENTS", summary.withParents(), total),
						completeness(ctx, "SOURCE", summary.withSource(), total))));
			}

			sections.add(new ReportSection.Heading(2, labels.sections().databaseSummaryPeriod()));
			sections.add(new ReportSection.Table(List.of(labels.sections().databaseSummaryColumn("MEASURE"),
				labels.sections().databaseSummaryColumn("VALUE")),
				List.of(
					List.of(labels.sections().databaseSummaryMeasure("EVENTS_DATED"), ctx.formatInt(summary.datedEvents()) + " / "
						+ ctx.formatInt(summary.events()) + " (" + PedigreeCollapseGenerator.percent(locale, summary.datedEvents(),
						summary.events()) + ")"),
					List.of(labels.sections().databaseSummaryMeasure("FIRST_EVENT"), year(summary.firstEvent())),
					List.of(labels.sections().databaseSummaryMeasure("LAST_EVENT"), year(summary.lastEvent())),
					List.of(labels.sections().databaseSummaryMeasure("FIRST_BIRTH"), year(summary.firstBirth())),
					List.of(labels.sections().databaseSummaryMeasure("LAST_BIRTH"), year(summary.lastBirth())))));
			sections.add(new ReportSection.Paragraph("*" + labels.sections().databaseSummaryNote() + "*"));
		}
		listener.onProgress(100, null);

		return new ReportDocument(title, null, sections, language, labels.sections().contents());
	}


	private static List<String> completeness(final ReportContext ctx, final String measure, final int count, final int total){
		return List.of(ctx.labels.sections().databaseSummaryMeasure(measure), ctx.formatInt(count),
			PedigreeCollapseGenerator.percent(ctx.labels.language().locale(), count, total));
	}

	private static String year(final Integer year){
		return (year != null? year.toString(): NO_VALUE);
	}


	/** @return	What the file has, as far as the privacy policy of the report shows it. */
	static Summary summarize(final ReportContext ctx){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final FLEFRecord record : ctx.model.getRecords())
			if(!HeaderReader.TYPE.equals(record.getTag()) && ctx.isVisible(record))
				counts.merge(record.getTag(), 1, Integer::sum);
		final Map<String, Integer> byKind = new LinkedHashMap<>();
		counts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
				.thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
			.forEach(entry -> byKind.put(entry.getKey(), entry.getValue()));

		int individuals = 0;
		int withSurname = 0;
		int withSex = 0;
		int withBirthDate = 0;
		int withBirthPlace = 0;
		int withDeathDate = 0;
		int withParents = 0;
		int withSource = 0;
		Integer firstBirth = null;
		Integer lastBirth = null;
		for(final FLEFRecord person : ctx.model.getRecordsByType(IndividualReader.TYPE)){
			if(!ctx.isVisible(person))
				continue;

			individuals ++;
			if(SurnameDistribution.surnameOf(person) != null)
				withSurname ++;
			if(PreferredSex.of(person, ctx.model) != SexType.UNKNOWN)
				withSex ++;
			if(!ctx.index.parentsOf(person).isEmpty())
				withParents ++;
			if(SourceCitationReader.hasSupportingCitation(person))
				withSource ++;

			final FLEFRecord birth = FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_BIRTH);
			if(birth != null){
				final Integer year = GenealogicalDateHelper.yearOrNull(birth);
				if(year != null){
					withBirthDate ++;
					firstBirth = (firstBirth != null? Math.min(firstBirth, year): year);
					lastBirth = (lastBirth != null? Math.max(lastBirth, year): year);
				}
				if(EventHelper.eventPlaceId(birth) != null)
					withBirthPlace ++;
			}
			final FLEFRecord death = FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_DEATH);
			if(death != null && GenealogicalDateHelper.yearOrNull(death) != null)
				withDeathDate ++;
		}

		int events = 0;
		int datedEvents = 0;
		Integer firstEvent = null;
		Integer lastEvent = null;
		for(final FLEFRecord event : ctx.model.getRecordsByType(EventReader.TYPE)){
			if(!ctx.isVisible(event))
				continue;

			events ++;
			final Integer year = GenealogicalDateHelper.yearOrNull(event);
			if(year == null)
				continue;

			datedEvents ++;
			firstEvent = (firstEvent != null? Math.min(firstEvent, year): year);
			lastEvent = (lastEvent != null? Math.max(lastEvent, year): year);
		}

		return new Summary(byKind, individuals, withSurname, withSex, withBirthDate, withBirthPlace, withDeathDate, withParents, withSource,
			events, datedEvents, firstEvent, lastEvent, firstBirth, lastBirth);
	}

}
