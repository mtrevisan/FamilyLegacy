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
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionResolution;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * The dashboard of the conflicts of the research, all in one place: dates and places that do not agree, conflicting relationships,
 * attributes and names, identity hypotheses nobody has judged and conclusions still to be made.
 * <p>
 * The conflicts are the ones the {@link ConflictFinder} finds (the ones of the Conflicts report: it is the same knowledge, seen as a
 * list to work on instead of a document to read), with the confidence of each competing assertion, how many citations support it
 * ({@code SourceCitation.stance}) or contradict it, and the assertion that the conclusion prefers. A conclusion that evaluates
 * records but takes no position (see {@link ConclusionResolution#isOpen}) is listed too, as a conclusion to complete.
 * <p>
 * It reads the model and decides nothing; whatever concerns an individual hidden by the privacy policy is left out.
 *
 * @param entries	Everything that is in conflict or open, the unresolved first.
 */
public record ConflictDashboard(List<Entry> entries){

	/** What an entry is about, as the user thinks of it. */
	public enum Category{

		/** Several births, baptisms, deaths or burials of a person whose dates do not agree (or are not compared yet). */
		DATES,
		/** Events whose places cannot all be right. */
		PLACES,
		/** Parentage and relationships that exclude each other. */
		RELATIONSHIPS,
		/** Attributes and names with several values. */
		VALUES,
		/** Identity hypotheses. */
		IDENTITIES,
		/** Conclusions that evaluate records but decide nothing yet. */
		CONCLUSIONS;


		/** @return	The key of the labels of the category. */
		public String key(){
			return name().toLowerCase(Locale.ROOT);
		}

	}

	/**
	 * One thing to look at.
	 *
	 * @param category	What it is about.
	 * @param conflict	The conflict ({@code null} for a conclusion that is still open).
	 * @param conclusion	The conclusion that is still open ({@code null} for a conflict).
	 * @param personIds	The individuals it concerns.
	 * @param assertions	The competing assertions, or the records an open conclusion evaluates.
	 * @param types	The kinds of record involved.
	 */
	public record Entry(Category category, ReportConflict conflict, FLEFRecord conclusion, List<String> personIds,
			List<ReportConflict.Assertion> assertions, Set<ConflictRecordType> types){

		/** @return	Whether a conclusion settles it (an open conclusion never does). */
		public boolean resolved(){
			return (conflict != null && conflict.resolved());
		}

		/** @return	The identifier of the conclusion that deals with it (the one that settles the conflict, or the open one), {@code null} if none. */
		public String conclusionId(){
			if(conclusion != null)
				return conclusion.getId();
			return (conflict != null && conflict.settlement() != null? conflict.settlement().conclusionId(): null);
		}

		/** @return	Whether a conclusion can be written for it: not for a name (a conclusion cannot point to one), nor for an open conclusion. */
		public boolean canBeConcluded(){
			return (conflict != null && conflict.kind() != ReportConflict.Kind.NAME);
		}

		/** @return	Whether it is listed with the record type chosen. */
		public boolean accepts(final ConflictRecordType type){
			return (type == ConflictRecordType.ALL || types.contains(type));
		}

		/**
		 * @return	The issue a conclusion that resolves the conflict would address ({@code birth_date}, {@code parentage}, {@code sex}...),
		 * 	empty if no conclusion can.
		 */
		public String issue(){
			if(!canBeConcluded())
				return StringUtils.EMPTY;

			return switch(conflict.kind()){
				case BIRTH, BAPTISM, DEATH, BURIAL -> conflict.kind().key() + (conflict.problem() == ReportConflict.Problem.SEVERAL_RECORDS
					? StringUtils.EMPTY: "_date");
				case PARENTAGE -> "parentage";
				case RELATIONSHIP -> "relationship_type";
				case ATTRIBUTE -> StringUtils.substringBefore(conflict.assertions().getFirst().value(), ':');
				case IDENTITY -> "identity";
				case NAME -> StringUtils.EMPTY;
			};
		}

	}


	/**
	 * @param model	The model.
	 * @param finder	What finds the conflicts ({@code null}: none is found, only the open conclusions are listed).
	 * @return	The dashboard.
	 */
	public static ConflictDashboard analyze(final FLEFModel model, final ConflictFinder finder){
		final List<Entry> entries = new ArrayList<>();

		if(finder != null)
			for(final ReportConflict conflict : finder.find(model)){
				if(!isVisible(model, conflict))
					continue;

				entries.add(new Entry(categoryOf(conflict), conflict, null, conflict.personIds(), conflict.assertions(),
					EnumSet.of(typeOf(conflict.kind()))));
			}

		for(final FLEFRecord conclusion : PrivacyService.visible(model, model.getRecordsByType(ConclusionReader.TYPE))){
			final Set<String> resolves = new LinkedHashSet<>();
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				resolves.add(refOf(target));
			resolves.remove(null);

			final String preferred = refOf(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
			final String proofStatus = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS);
			if(!ConclusionResolution.isOpen(new ConclusionResolution.Evaluation(resolves, preferred,
					(StringUtils.isBlank(proofStatus)? null: proofStatus))))
				continue;

			final List<ReportConflict.Assertion> assertions = new ArrayList<>(resolves.size());
			final Set<ConflictRecordType> types = EnumSet.noneOf(ConflictRecordType.class);
			for(final String id : resolves){
				final FLEFRecord record = model.getRecordById(id);
				if(record == null)
					continue;

				assertions.add(new ReportConflict.Assertion(id, null, null, FLEFRecordHelper.getChildValue(record, EventReader.TAG_CONFIDENCE)));
				final ConflictRecordType type = typeOf(record);
				if(type != null)
					types.add(type);
			}
			entries.add(new Entry(Category.CONCLUSIONS, null, conclusion, List.of(), assertions, types));
		}

		entries.sort(Comparator.comparing(Entry::resolved)
			.thenComparing(Entry::category));
		return new ConflictDashboard(List.copyOf(entries));
	}

	/**
	 * @param filter	Settled, unresolved or both.
	 * @param confidence	The confidence an assertion must have.
	 * @param type	The kind of record.
	 * @return	The entries that pass all the filters, in the same order.
	 */
	public List<Entry> filter(final ConflictFilter filter, final ConflictConfidence confidence, final ConflictRecordType type){
		return entries.stream()
			.filter(entry -> filter.accepts(entry.resolved()))
			.filter(entry -> entry.accepts(type))
			.filter(entry -> confidence.accepts(entry.assertions()))
			.toList();
	}

	/** @return	How many entries a conclusion has not settled. */
	public int unresolved(){
		return (int)entries.stream()
			.filter(entry -> !entry.resolved())
			.count();
	}

	/** @return	How many entries are of the category. */
	public int count(final Category category){
		return (int)entries.stream()
			.filter(entry -> entry.category() == category)
			.count();
	}


	/**
	 * @param model	The model.
	 * @param assertion	The assertion.
	 * @return	How many citations of the assertion support it and how many contradict it (see {@code SourceCitation.stance}); it is the
	 * 	count the finder gave, if it gave one.
	 */
	public static int[] citations(final FLEFModel model, final ReportConflict.Assertion assertion){
		if(assertion.supporting() != null && assertion.contradicting() != null)
			return new int[]{assertion.supporting(), assertion.contradicting()};

		final FLEFRecord record = model.getRecordById(assertion.recordId());
		int supporting = 0;
		int contradicting = 0;
		if(record != null)
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_SOURCE))
				if(SourceCitationReader.isContradicting(citation))
					contradicting ++;
				else
					supporting ++;
		return new int[]{supporting, contradicting};
	}


	private static boolean isVisible(final FLEFModel model, final ReportConflict conflict){
		for(final String personId : conflict.personIds()){
			final FLEFRecord person = model.getRecordById(personId);
			if(PrivacyService.isHidden(model, person))
				return false;
		}
		return true;
	}

	private static Category categoryOf(final ReportConflict conflict){
		return switch(conflict.kind()){
			case BIRTH, BAPTISM, DEATH, BURIAL -> (conflict.problem() == ReportConflict.Problem.DIFFERENT_PLACES
				? Category.PLACES: Category.DATES);
			case PARENTAGE, RELATIONSHIP -> Category.RELATIONSHIPS;
			case ATTRIBUTE, NAME -> Category.VALUES;
			case IDENTITY -> Category.IDENTITIES;
		};
	}

	private static ConflictRecordType typeOf(final ReportConflict.Kind kind){
		for(final ConflictRecordType type : ConflictRecordType.values())
			if(type != ConflictRecordType.ALL && type.accepts(kind))
				return type;
		throw new IllegalArgumentException("No record type for " + kind);
	}

	/** @return	The kind of record, {@code null} if it is not one of the ones that conflict. */
	private static ConflictRecordType typeOf(final FLEFRecord record){
		if(EventReader.isOfType(record) || EventParticipationReader.isOfType(record))
			return ConflictRecordType.EVENT;
		if(RelationshipReader.isOfType(record))
			return ConflictRecordType.RELATIONSHIP;
		if(IndividualAttributeReader.isOfType(record))
			return ConflictRecordType.ATTRIBUTE;
		if(IdentityHypothesisReader.isOfType(record))
			return ConflictRecordType.IDENTITY;
		return null;
	}

	/** The identifier a field of a conclusion points to. */
	private static String refOf(final FLEFRecord field){
		if(field == null)
			return null;

		final FLEFRecord child = field.getTheOnlyChild();
		if(child != null && child.getValue() != null)
			return child.getValue();
		return field.getValue();
	}

}
