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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


/**
 * Generates the "Kinship" report of an individual: all the blood relatives of the subject, in order of the civil (Roman) degree, each one
 * with the kinship term (father, aunt, first cousin once removed, ...), the dates of birth and death.
 * <p>
 * A relative is found through a common ancestor: the ancestors of the subject, as far as the generations of the configuration, and
 * everybody who descends from them within the same number of generations (0 stands for {@value #DEFAULT_GENERATIONS}, because "all of
 * them" is the whole connected file). A relative with more than one common ancestor is listed once, with the nearest one, as the kinship
 * calculator does. Only the blood links count (adoptive, foster and step relatives are not listed), and an individual the privacy policy
 * hides is not listed.
 * </p>
 */
public final class KinshipReportGenerator{

	/** The generations followed up and down from a common ancestor when the configuration asks for all of them. */
	static final int DEFAULT_GENERATIONS = 4;
	/** The generations followed at most (a corrupt file may have a loop of ancestors). */
	static final int MAX_GENERATIONS = 12;


	/**
	 * A blood relative of the subject.
	 *
	 * @param person	the relative
	 * @param fromAncestor	the generations between the relative and the nearest common ancestor (0: the relative is that ancestor)
	 * @param subjectFromAncestor	the generations between the subject and the nearest common ancestor (0: the subject is that ancestor)
	 */
	record Relative(FLEFRecord person, int fromAncestor, int subjectFromAncestor){

		/** @return	The civil (Roman) degree: the generations in a direct line, the steps up and down in a collateral one. */
		int civilDegree(){
			return (fromAncestor == 0 || subjectFromAncestor == 0
				? Math.max(fromAncestor, subjectFromAncestor)
				: fromAncestor + subjectFromAncestor);
		}
	}


	private final FLEFModel model;
	private final FLEFRecord root;
	private final ReportConfig config;


	public KinshipReportGenerator(final FLEFModel model, final FLEFRecord root, final ReportConfig config){
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
		final Locale locale = labels.language().locale();
		final String title = labels.sections().kinshipTitle();
		final String language = locale.toLanguageTag();

		final List<ReportSection> sections = new ArrayList<>(new HeaderSection(ctx).build());
		sections.add(new ReportSection.Heading(1, labels.sections().overview()));
		listener.onProgress(10, null);

		final int generations = generationsOf(config);
		final List<Relative> relatives = compute(ctx.index, root, generations);
		listener.onProgress(50, null);

		//the term of each one from the subject ("aunt"); a relative that is not a blood one has none and is not listed
		final Map<Integer, List<List<String>>> rowsByDegree = new TreeMap<>();
		final Comparator<String> collator = ReportCollation.of(locale);
		final List<Relative> listed = new ArrayList<>(relatives.size());
		for(final Relative relative : relatives){
			if(Thread.currentThread().isInterrupted())
				throw new RuntimeException("Report generation cancelled");

			final String term = ctx.kinship().shortTerm(relative.person().getId(), root.getId());
			if(ReportFormatters.isUsefulKinshipTerm(term))
				listed.add(relative);
		}
		listed.sort(Comparator.comparingInt(Relative::civilDegree)
			.thenComparingInt(Relative::fromAncestor)
			.thenComparing(relative -> ctx.displayText(relative.person()), collator));
		for(final Relative relative : listed)
			rowsByDegree.computeIfAbsent(relative.civilDegree(), degree -> new ArrayList<>())
				.add(row(ctx, relative));
		listener.onProgress(80, null);

		if(listed.isEmpty())
			sections.add(new ReportSection.Paragraph(labels.sections().kinshipNone()));
		else{
			sections.add(new ReportSection.Paragraph(String.format(labels.sections().kinshipIntro(), ctx.formatInt(listed.size()),
				ctx.formatInt(rowsByDegree.size()), ctx.formatInt(generations))));
			final List<String> headers = List.of(labels.sections().kinshipColumn("KINSHIP"), labels.sections().kinshipColumn("NAME"),
				labels.sections().kinshipColumn("BORN"), labels.sections().kinshipColumn("DIED"));
			for(final Map.Entry<Integer, List<List<String>>> entry : rowsByDegree.entrySet()){
				sections.add(new ReportSection.Heading(2, String.format(labels.sections().kinshipDegree(), ctx.formatInt(entry.getKey()))));
				sections.add(new ReportSection.Table(headers, entry.getValue()));
			}
			sections.add(new ReportSection.Paragraph("*" + labels.sections().kinshipNote() + "*"));
		}
		listener.onProgress(100, null);

		return new ReportDocument(title, ReportFormatters.escape(ctx.displayText(root)), sections, language, labels.sections().contents());
	}


	/** @return	The generations to follow from the configuration (0 stands for {@link #DEFAULT_GENERATIONS}). */
	static int generationsOf(final ReportConfig config){
		return (config.generations() > 0? Math.min(config.generations(), MAX_GENERATIONS): DEFAULT_GENERATIONS);
	}

	/**
	 * Finds the relatives through the common ancestors.
	 *
	 * @param index	the relations of the model (they hide what the privacy policy hides)
	 * @param root	the subject
	 * @param generations	how many generations to follow up from the subject and down from each ancestor (1 is the parents, and the children)
	 * @return	The relatives, each one once, with the nearest common ancestor; the subject is not one.
	 */
	static List<Relative> compute(final RelationIndex index, final FLEFRecord root, final int generations){
		final Map<String, Relative> relatives = new LinkedHashMap<>();
		final List<List<FLEFRecord>> ancestors = index.ancestorLevels(root, generations);
		for(int up = 0; up < ancestors.size(); up ++)
			for(final FLEFRecord ancestor : ancestors.get(up)){
				final List<List<FLEFRecord>> descendants = index.descendantLevels(ancestor, generations);
				for(int down = 0; down < descendants.size(); down ++)
					for(final FLEFRecord person : descendants.get(down)){
						if(person.getId().equals(root.getId()))
							continue;

						final Relative candidate = new Relative(person, down, up);
						final Relative known = relatives.get(person.getId());
						//(the nearest ancestor wins, then the one with the relative closer to it, as the kinship calculator does)
						if(known == null || candidate.fromAncestor() + candidate.subjectFromAncestor()
								< known.fromAncestor() + known.subjectFromAncestor()
							|| (candidate.fromAncestor() + candidate.subjectFromAncestor()
								== known.fromAncestor() + known.subjectFromAncestor() && candidate.fromAncestor() < known.fromAncestor()))
							relatives.put(person.getId(), candidate);
					}
			}
		return new ArrayList<>(relatives.values());
	}


	private List<String> row(final ReportContext ctx, final Relative relative){
		final FLEFRecord person = relative.person();
		//a person shown with the name and the role only has nothing else
		final boolean redacted = ctx.isRedacted(person);
		final FLEFRecord birth = (redacted? null: FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_BIRTH));
		final FLEFRecord death = (redacted? null: FamilyGroupSheetGenerator.event(ctx, person, EventReader.ENUM_TYPE_DEATH));
		return List.of(ReportFormatters.escape(ctx.kinship().shortTerm(person.getId(), root.getId())),
			ReportFormatters.escape(ctx.displayText(person)),
			FamilyGroupSheetGenerator.date(ctx, birth), FamilyGroupSheetGenerator.date(ctx, death));
	}

}
