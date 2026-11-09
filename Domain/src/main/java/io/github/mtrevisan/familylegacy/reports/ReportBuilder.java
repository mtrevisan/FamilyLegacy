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
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;
import io.github.mtrevisan.reports.ReportDocument;

import java.util.EnumSet;
import java.util.Set;


/**
 * Builds the document of each {@link ReportType}: which generator runs, and how the options chosen by the user are adapted to the
 * perspective of the report.
 * <p>
 * The generator itself (see {@link ReportGenerator}) dispatches by the kind of the root record, so passing a group, a source, a place,
 * etc. as root is supported without any change here. A report of the types {@link ReportType#BIBLIOGRAPHY} and
 * {@link ReportType#RESEARCH_PROGRESS} is about the record it is given, not about the whole file.
 * </p>
 */
public final class ReportBuilder{

	private ReportBuilder(){}


	/**
	 * Assembles the document of a report.
	 *
	 * @param type	The type of the report.
	 * @param model	The model to report on.
	 * @param root	The record the report is about.
	 * @param userConfig	The options selected by the user.
	 * @param listener	Told how far the generation is.
	 * @return	The document, never {@code null}.
	 */
	public static ReportDocument build(final ReportType type, final FLEFModel model, final FLEFRecord root, final ReportConfig userConfig,
			final ReportProgressListener listener){
		final ReportConfig tailored = tailorConfigFor(type, userConfig);

		return switch(type){
			case INDIVIDUAL, GROUP, RELATIONSHIP, PLACE, SOURCE, RESEARCH_QUESTION, BIBLIOGRAPHY, RESEARCH_PROGRESS ->
				new ReportGenerator(model, root, tailored)
				.generate(listener);
			case FULL_BIBLIOGRAPHY -> new FullBibliographyGenerator(model, tailored).generate(listener);
			case FAMILY_BOOK -> new FamilyBookGenerator(model, tailored).generate(listener);
			case TO_VERIFY -> new ToVerifyGenerator(model, tailored).generate(listener);
			case SURNAMES -> new SurnamesGenerator(model, tailored).generate(listener);
			case ANCESTOR -> new AncestorReportGenerator(model, root, tailored).generate(listener);
			case DESCENDANT -> new DescendantReportGenerator(model, root, tailored).generate(listener);
		};
	}


	/**
	 * Adapts the user's {@link ReportConfig} to the perspective requested
	 * by the report type.
	 *
	 * <p>Only the <em>narrative</em> flags are toggled: privacy, audit,
	 * sources, evidence, media, cultural norms, groups, indexes and style
	 * are left exactly as the user chose them, because they are
	 * perspective-independent.</p>
	 *
	 * <ul>
	 *   <li><b>INDIVIDUAL</b> — user config unchanged.</li>
	 *   <li><b>GROUP</b> — user config unchanged; the group sections are
	 *       selected automatically by the generator's dispatch.</li>
	 *   <li><b>ANCESTOR</b>, <b>DESCENDANT</b> — introduction on for every person; ancestry, descendants
	 *       and relations disabled (the persons are the ancestors or the descendants).</li>
	 *   <li><b>RELATIONSHIP</b> — direct and indirect relations; ancestry
	 *       and descendants disabled.</li>
	 *   <li><b>BIBLIOGRAPHY</b> — sources, media, repositories and evidence;
	 *       everything else minimized.</li>
	 *   <li><b>RESEARCH_PROGRESS</b> — context and research, evidence,
	 *       coverage report; narrative sections disabled.</li>
	 * </ul>
	 */
	public static ReportConfig tailorConfigFor(final ReportType type, final ReportConfig base){
		return switch(type){
			case INDIVIDUAL, GROUP, PLACE, SOURCE, RESEARCH_QUESTION, FULL_BIBLIOGRAPHY, TO_VERIFY, SURNAMES -> base;
			//one self-contained sub-report per person: the introduction and the life story, no ancestry, descendants or relations (the
			//persons are the ancestors or the descendants)
			case ANCESTOR, DESCENDANT, FAMILY_BOOK -> withNarrative(base, true, false, false, false, false, false);
			case RELATIONSHIP -> withNarrative(base, base.introduction(), false, false, false, true, true);
			case BIBLIOGRAPHY -> withSourcesFocus(base);
			case RESEARCH_PROGRESS -> withResearchFocus(base);
		};
	}


	/* ----- What the user can choose --------------------------------------- */

	/**
	 * Tells what a report does with an option, so that the dialog shows only what has an effect.
	 * <p>
	 * An option the report forces is told by comparing what {@link #tailorConfigFor(ReportType, ReportConfig)} makes of a configuration with
	 * everything on and one with everything off; one that does not apply is told by the kind of the record the report is about.
	 * </p>
	 *
	 * @param type	The type of the report.
	 * @param root	The record the report is about; {@code null} if not chosen yet.
	 * @param option	The option.
	 * @return	How the option is shown.
	 */
	public static OptionState optionState(final ReportType type, final FLEFRecord root, final ReportOption option){
		if(notApplicable(type, root).contains(option))
			return OptionState.HIDDEN;

		final ReportConfig on = ReportConfig.uniform(true, ReportLanguage.ENGLISH, PrivacyPolicy.HIDE_CONFIDENTIAL);
		final ReportConfig off = ReportConfig.uniform(false, ReportLanguage.ENGLISH, PrivacyPolicy.HIDE_CONFIDENTIAL);
		final Object onValue = option.valueIn(on);
		final Object offValue = option.valueIn(off);
		final Object tailoredOn = option.valueIn(tailorConfigFor(type, on));
		final Object tailoredOff = option.valueIn(tailorConfigFor(type, off));

		if(tailoredOn.equals(onValue) && tailoredOff.equals(offValue))
			return OptionState.EDITABLE;
		if(tailoredOn.equals(onValue) && tailoredOff.equals(onValue))
			return OptionState.ALWAYS_ON;
		//the report always leaves it out
		return OptionState.HIDDEN;
	}

	/** The options that change nothing in a report of the type about the record. */
	private static Set<ReportOption> notApplicable(final ReportType type, final FLEFRecord root){
		final Set<ReportOption> out = EnumSet.noneOf(ReportOption.class);
		if(type == ReportType.FULL_BIBLIOGRAPHY){
			//a catalogue of the sources: the front matter, the notes, the citations and the evidence of the sources
			out.addAll(EnumSet.complementOf(EnumSet.of(ReportOption.SHOW_PRIVACY_DETAILS, ReportOption.AUDIT, ReportOption.HEADER,
				ReportOption.NOTES, ReportOption.SOURCES, ReportOption.EVIDENCE)));
			return out;
		}
		if(type == ReportType.TO_VERIFY || type == ReportType.SURNAMES){
			//a list on the whole file: only the front matter has a meaning (the privacy policy hides what it hides)
			out.addAll(EnumSet.complementOf(EnumSet.of(ReportOption.HEADER)));
			return out;
		}
		if(type == ReportType.ANCESTOR || type == ReportType.DESCENDANT || type == ReportType.FAMILY_BOOK){
			//a sub-report for each person, then what the persons cite together: no context, norms, historic events, timeline, coverage
			//or group sheet
			out.addAll(Set.of(ReportOption.CONTEXT_AND_RESEARCH, ReportOption.CULTURAL_NORMS, ReportOption.HISTORIC_EVENTS,
				ReportOption.TIMELINE, ReportOption.COVERAGE_REPORT, ReportOption.GROUP_MEMBERS, ReportOption.GROUP_SUBGROUPS,
				ReportOption.GROUP_ATTRIBUTES));
			//the order of the persons is the choice of the book
			if(type != ReportType.FAMILY_BOOK)
				out.add(ReportOption.PERSON_ORDER);
			else{
				//and a book has everybody, not a line, and a page for each group, as a group sheet has
				out.add(ReportOption.COLLATERAL_SCOPE);
				out.add(ReportOption.GENERATIONS);
				out.removeAll(Set.of(ReportOption.GROUP_MEMBERS, ReportOption.GROUP_SUBGROUPS, ReportOption.GROUP_ATTRIBUTES));
			}
			return out;
		}

		//only the reports of the lines of a person have a scope for the collaterals and a number of generations
		out.add(ReportOption.COLLATERAL_SCOPE);
		out.add(ReportOption.GENERATIONS);
		out.add(ReportOption.PERSON_ORDER);

		final ReportContext.RootKind kind = (root != null? ReportContext.rootKindOf(root):
			(type == ReportType.GROUP? ReportContext.RootKind.GROUP: ReportContext.RootKind.INDIVIDUAL));
		final Set<ReportOption> person = Set.of(ReportOption.PATERNAL_ANCESTRY, ReportOption.MATERNAL_ANCESTRY, ReportOption.DESCENDANTS,
			ReportOption.DIRECT_RELATIONS, ReportOption.INDIRECT_RELATIONS, ReportOption.GROUPS, ReportOption.EVENT_FULL_PARTICIPANTS);
		final Set<ReportOption> group = Set.of(ReportOption.GROUP_MEMBERS, ReportOption.GROUP_SUBGROUPS, ReportOption.GROUP_ATTRIBUTES);
		switch(kind){
			case INDIVIDUAL -> out.addAll(group);
			case GROUP -> out.addAll(person);
			default -> {
				//a source, a place, an event...: neither a person nor a group
				out.addAll(person);
				out.addAll(group);
				out.add(ReportOption.INTRODUCTION);
			}
		}
		return out;
	}


	/* ----- Config copy helpers -------------------------------------------- */

	/**
	 * Returns a copy of {@code base} with the six narrative flags replaced. Everything else, including {@code collateralScope}, is the
	 * choice of the user and is kept.
	 */
	private static ReportConfig withNarrative(final ReportConfig base, final boolean introduction, final boolean paternal,
			final boolean maternal, final boolean descendants, final boolean direct, final boolean indirect){
		return base.with(ReportOption.INTRODUCTION, introduction)
			.with(ReportOption.PATERNAL_ANCESTRY, paternal)
			.with(ReportOption.MATERNAL_ANCESTRY, maternal)
			.with(ReportOption.DESCENDANTS, descendants)
			.with(ReportOption.DIRECT_RELATIONS, direct)
			.with(ReportOption.INDIRECT_RELATIONS, indirect);
	}


	/**
	 * Config for a bibliography report: sources, media, repositories and evidence in focus, with every narrative section disabled.
	 * {@code collateralScope} is forced to {@link CollateralScope#NONE} because a bibliography does not follow a direct line.
	 */
	private static ReportConfig withSourcesFocus(final ReportConfig base){
		return withNarrative(base, false, false, false, false, false, false)
			.with(ReportOption.COLLATERAL_SCOPE, CollateralScope.NONE)
			.with(ReportOption.SOURCES, true)
			.with(ReportOption.EVIDENCE, true)
			.with(ReportOption.CONTEXT_AND_RESEARCH, false)
			.with(ReportOption.CULTURAL_NORMS, false)
			.with(ReportOption.GROUPS, false)
			.with(ReportOption.GROUP_MEMBERS, false)
			.with(ReportOption.GROUP_SUBGROUPS, false)
			.with(ReportOption.GROUP_ATTRIBUTES, false)
			.with(ReportOption.HISTORIC_EVENTS, false)
			.with(ReportOption.REPOSITORIES, true)
			.with(ReportOption.EVENT_FULL_PARTICIPANTS, false)
			.with(ReportOption.TIMELINE, false)
			.with(ReportOption.INDEX_INDIVIDUALS, false)
			.with(ReportOption.INDEX_PLACES, true);
	}


	/**
	 * Config for a research-progress report: research questions, context impacts, evidence and coverage in focus; narrative sections
	 * disabled. {@code collateralScope} is forced to {@link CollateralScope#NONE} because a research-progress report does not follow a
	 * direct line.
	 */
	private static ReportConfig withResearchFocus(final ReportConfig base){
		return withNarrative(base, true, false, false, false, false, false)
			.with(ReportOption.COLLATERAL_SCOPE, CollateralScope.NONE)
			.with(ReportOption.SOURCES, true)
			.with(ReportOption.MEDIA, false)
			.with(ReportOption.EVIDENCE, true)
			.with(ReportOption.CONTEXT_AND_RESEARCH, true)
			.with(ReportOption.GROUPS, false)
			.with(ReportOption.GROUP_MEMBERS, false)
			.with(ReportOption.GROUP_SUBGROUPS, false)
			.with(ReportOption.GROUP_ATTRIBUTES, false)
			.with(ReportOption.HISTORIC_EVENTS, true)
			.with(ReportOption.REPOSITORIES, true)
			.with(ReportOption.EVENT_FULL_PARTICIPANTS, false)
			.with(ReportOption.COVERAGE_REPORT, true)
			.with(ReportOption.INDEX_INDIVIDUALS, true)
			.with(ReportOption.INDEX_PLACES, false);
	}

}
