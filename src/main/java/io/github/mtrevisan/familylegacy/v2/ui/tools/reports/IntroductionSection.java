package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;


/**
 * Builds the introduction section with individual-scoped statistics
 * (lifespan, event count, sources cited, relation counts).
 *
 * <p>The lifespan is exact (year, month, day) when both the birth and the
 * death dates are stored with {@code DatePrecision.EXACT}. When only the
 * years are known, the age is reported with a {@code ~} prefix to signal
 * that it is approximate: a year-only difference can be off by one because
 * month and day are lost.</p>
 *
 * <p>Plural-sensitive lines (age, relation counts) go through
 * {@link ReportLabels} plural helpers, which use ICU4J to select the correct
 * form for the target language.</p>
 */
final class IntroductionSection implements SectionBuilder{

	private static final String TAG_TYPE = "type";
	private static final String TYPE_BIRTH = "birth";
	private static final String TYPE_DEATH = "death";


	private final ReportContext ctx;


	IntroductionSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.introduction())
			return List.of();

		final List<ReportSection> out = new ArrayList<>();
		out.add(new ReportSection.Heading(1, ctx.labels.introduction()));
		out.add(new ReportSection.Paragraph(String.format(
			ctx.labels.introductionBody(),
			"**" + ReportFormatters.escape(ctx.displayText(ctx.root)) + "**")));

		final List<FLEFRecord> events = ctx.index.eventsOf(ctx.root);
		out.add(new ReportSection.Paragraph(ctx.labels.statistics()));
		out.add(new ReportSection.BulletList(List.of(
			String.format(ctx.labels.lifespan(), lifespanOf(events)),
			String.format(ctx.labels.eventsCount(), ctx.formatInt(events.size())),
			String.format(ctx.labels.sourcesCount(), ctx.formatInt(sourcesCount())),
			ctx.labels.relationsCount(
				ctx.index.parentsOf(ctx.root).size(),
				ctx.index.spousesOf(ctx.root).size(),
				ctx.index.childrenOf(ctx.root).size())
		)));
		out.add(new ReportSection.Paragraph(String.format(
			ctx.labels.narrativeNote(),
			"**" + ReportFormatters.escape(ctx.displayText(ctx.root)) + "**")));
		return out;
	}


	/**
	 * Builds the lifespan string, e.g. {@code "1926–2018 (91 years)"} when
	 * both dates are full dates, or {@code "1926–2018 (~92 years)"} when
	 * only the years are known.
	 *
	 * <p>The {@code ~} prefix marks the approximate age: the year-only
	 * difference can be off by one because month and day are missing.</p>
	 */
	private String lifespanOf(final List<FLEFRecord> events){
		final FLEFRecord birthEvent = findEvent(events, TYPE_BIRTH);
		final FLEFRecord deathEvent = findEvent(events, TYPE_DEATH);

		final Integer birthYear = (birthEvent != null? GenealogicalDateHelper.yearOrNull(birthEvent): null);
		final Integer deathYear = (deathEvent != null? GenealogicalDateHelper.yearOrNull(deathEvent): null);

		if(birthYear == null && deathYear == null)
			return "—";

		final String span = (birthYear != null? birthYear.toString(): "?")
			+ "–"
			+ (deathYear != null? deathYear.toString(): "?");

		// 1. Exact age when both dates are full (year + month + day).
		final LocalDate birthDate = (birthEvent != null
			? GenealogicalDateHelper.exactDateOrNull(birthEvent): null);
		final LocalDate deathDate = (deathEvent != null
			? GenealogicalDateHelper.exactDateOrNull(deathEvent): null);
		if(birthDate != null && deathDate != null && !deathDate.isBefore(birthDate)){
			final int years = Period.between(birthDate, deathDate).getYears();
			return span + " (" + ctx.labels.ageYears(years) + ")";
		}

		// 2. Approximate age from year-only difference.
		if(birthYear != null && deathYear != null && deathYear >= birthYear){
			final int years = deathYear - birthYear;
			return span + " (~" + ctx.labels.ageYears(years) + ")";
		}

		return span;
	}


	private static FLEFRecord findEvent(final List<FLEFRecord> events, final String type){
		for(final FLEFRecord e : events){
			final String t = FLEFRecordHelper.getChildValue(e, TAG_TYPE);
			if(type.equalsIgnoreCase(t))
				return e;
		}
		return null;
	}


	private int sourcesCount(){
		final List<String> ids = new ArrayList<>();
		SourceCollector.collect(ctx, ids);
		return ids.size();
	}

}
