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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;

import java.util.ArrayList;
import java.util.List;


/**
 * Opens a conclusion up: which evidence it rests on, how the researcher got from the evidence to the conclusion, how confident the
 * result is, which other readings were on the table and which evidence goes against it. It answers <em>why</em>, <em>how</em> and
 * <em>on which evidence</em>, with no black box: everything is read from the records (the competing assertions the conclusion
 * resolves, their citations with the evidence qualifiers, their confidence, the research questions, the narrative), and the weights
 * are the ones of {@link SourceCitationReader#citationWeight(FLEFRecord)}, a rule of thumb that never replaces the researcher's
 * judgement.
 * <p>
 * The result is data, with no text of the user interface: see {@link InferenceInspectionText} for how it is written.
 */
public final class InferenceInspection{

	/** The remarks on the reasoning that the data allow to make. */
	public enum FindingKind{
		/** The conclusion does not say which research question it answers. */
		NO_QUESTION,
		/** There is no narrative that explains the reasoning. */
		NO_NARRATIVE,
		/** Fewer than two assertions are resolved: there is nothing to choose between. */
		SINGLE_ASSERTION,
		/** A target of the conclusion is not in the model (or not visible). */
		UNRESOLVED_TARGET,
		/** The status says the preferred assertion is supported or proven, but none is preferred. */
		STATUS_WITHOUT_PREFERRED,
		/** The preferred assertion has no citation that supports it. */
		PREFERRED_UNSUPPORTED,
		/** The preferred assertion is contradicted by a citation, or its evidence is negative. */
		PREFERRED_CONTRADICTED,
		/** An alternative has more weight of evidence than the preferred assertion. */
		PREFERRED_OUTWEIGHED,
		/** The status is proven but the preferred assertion is not backed by direct evidence from an original source. */
		PROVEN_WITHOUT_DIRECT_ORIGINAL,
		/** An alternative assertion is supported by evidence too. */
		RIVAL_SUPPORTED
	}

	/**
	 * @param kind    what the remark is about
	 * @param subject the assertion it is about (its label), or {@code null}
	 */
	public record Finding(FindingKind kind, String subject){}

	/**
	 * One citation that backs, or goes against, an assertion.
	 *
	 * @param sourceId        the identifier of the cited source
	 * @param source          the title of the source (its identifier if it has none)
	 * @param locator         where in the source, or {@code null}
	 * @param contradicting   whether the citation contradicts the assertion
	 * @param sourceType      original or derived, or {@code null}
	 * @param informationType primary, secondary or undetermined, or {@code null}
	 * @param evidenceType    direct, indirect or negative, or {@code null}
	 * @param weight          the weight of the citation as support (negative if it goes against the assertion)
	 * @param extracts        how many extracts it cites
	 */
	public record Citation(String sourceId, String source, String locator, boolean contradicting, String sourceType,
			String informationType, String evidenceType, int weight, int extracts){

		/** @return whether it goes against the assertion: it contradicts it, or it is negative evidence (the absence of the assertion) */
		public boolean opposing(){
			return (contradicting || EvidenceQualifiersReader.EVIDENCE_TYPE_NEGATIVE.equals(evidenceType));
		}

		/** @return whether the citation is an original source with direct evidence that supports the assertion */
		public boolean directOriginal(){
			return (!opposing() && EvidenceQualifiersReader.ENUM_SOURCE_TYPE_ORIGINAL.equals(sourceType)
				&& EvidenceQualifiersReader.ENUM_EVIDENCE_TYPE_DIRECT.equals(evidenceType));
		}
	}

	/**
	 * One of the competing assertions the conclusion evaluates.
	 *
	 * @param id         the identifier of the record
	 * @param label      how the record reads to the user
	 * @param preferred  whether the conclusion prefers it
	 * @param confidence the confidence recorded on the assertion ({@code low}, {@code medium}, {@code high}), or {@code null}
	 * @param weight     the sum of the weights of its citations
	 * @param citations  the citations that back it or go against it
	 * @param known      whether the record is in the model and visible
	 */
	public record Assertion(String id, String label, boolean preferred, String confidence, int weight, List<Citation> citations,
			boolean known){

		/** @return the citations that support the assertion */
		public List<Citation> supporting(){
			return citations.stream()
				.filter(citation -> !citation.opposing())
				.toList();
		}

		/** @return the citations that go against the assertion */
		public List<Citation> opposing(){
			return citations.stream()
				.filter(Citation::opposing)
				.toList();
		}
	}

	/**
	 * @param conclusionId the identifier of the conclusion
	 * @param issue        the issue it resolves, or {@code null}
	 * @param proofStatus  the proof status, or {@code null}
	 * @param narrative    the narrative written by the researcher, or {@code null}
	 * @param questions    the titles of the research questions that led to it
	 * @param assertions   the assertions it evaluates, in the order of the record
	 * @param methodology  the citations of the conclusion itself (a treatise, a secondary source that already settled the conflict)
	 * @param findings     the remarks on the reasoning
	 */
	public record Inspection(String conclusionId, String issue, String proofStatus, String narrative, List<String> questions,
			List<Assertion> assertions, List<Citation> methodology, List<Finding> findings){

		/** @return the preferred assertion, or {@code null} */
		public Assertion preferred(){
			return assertions.stream()
				.filter(Assertion::preferred)
				.findFirst()
				.orElse(null);
		}

		/** @return the assertions that are not the preferred one */
		public List<Assertion> alternatives(){
			return assertions.stream()
				.filter(assertion -> !assertion.preferred())
				.toList();
		}

		/** @return the confidence of the conclusion: the one recorded on the preferred assertion, or {@code null} */
		public String confidence(){
			final Assertion preferred = preferred();
			return (preferred != null? preferred.confidence(): null);
		}
	}


	private InferenceInspection(){}


	/**
	 * @param model      the model the conclusion belongs to
	 * @param conclusion a conclusion record
	 * @return what the conclusion rests on and how it was reached
	 */
	public static Inspection inspect(final FLEFModel model, final FLEFRecord conclusion){
		final String preferredId = armValue(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED));
		final List<Assertion> assertions = new ArrayList<>();
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES)){
			final String id = armValue(target);
			if(id != null && assertions.stream().noneMatch(assertion -> assertion.id().equals(id)))
				assertions.add(assertion(model, id, id.equals(preferredId)));
		}
		//the preferred one must be among the resolved ones, but if it is not it is shown nevertheless: it is part of the reasoning
		if(preferredId != null && assertions.stream().noneMatch(assertion -> assertion.id().equals(preferredId)))
			assertions.add(assertion(model, preferredId, true));

		final List<String> questions = new ArrayList<>();
		for(final FLEFRecord reference : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESEARCH)){
			final String id = referenceValue(reference);
			final FLEFRecord question = (id != null? model.getRecordById(id): null);
			if(ResearchQuestionReader.isOfType(question))
				questions.add(ResearchHelper.questionTitle(question));
			else if(id != null)
				questions.add(id);
		}

		final String status = ResearchHelper.proofStatus(conclusion);
		final String narrative = ResearchHelper.firstTextValue(conclusion, ConclusionReader.TAG_NARRATIVE);
		final List<Citation> methodology = citations(model, conclusion);
		return new Inspection(conclusion.getId(), ResearchHelper.conclusionIssue(conclusion), status, narrative, List.copyOf(questions),
			List.copyOf(assertions), methodology, findings(status, narrative, questions, assertions));
	}

	private static Assertion assertion(final FLEFModel model, final String id, final boolean preferred){
		final FLEFRecord record = model.getRecordById(id);
		if(record == null || PrivacyService.isHidden(model, record))
			return new Assertion(id, id, preferred, null, 0, List.of(), false);

		final List<Citation> citations = citations(model, record);
		final int weight = citations.stream()
			.mapToInt(Citation::weight)
			.sum();
		final String confidence = FLEFRecordHelper.getChildValue(record, "confidence");
		return new Assertion(id, RecordDisplay.describe(record, model), preferred, (confidence != null && !confidence.isBlank()? confidence: null),
			weight, List.copyOf(citations), true);
	}

	private static List<Citation> citations(final FLEFModel model, final FLEFRecord holder){
		final List<Citation> citations = new ArrayList<>();
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(holder, SourceCitationReader.TAG_SOURCE)){
			final String sourceId = SourceCitationReader.extractSource(citation);
			final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);
			final String title = (source != null? ResearchHelper.firstTextValue(source, SourceReader.TAG_TITLE): null);
			final FLEFRecord evidence = FLEFRecordHelper.findChild(citation, SourceCitationReader.TAG_EVIDENCE);
			final String locator = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR);
			citations.add(new Citation(sourceId, (title != null && !title.isBlank()? title: sourceId),
				(locator != null && !locator.isBlank()? locator: null), SourceCitationReader.isContradicting(citation),
				(evidence != null? EvidenceQualifiersReader.extractSourceType(evidence): null),
				(evidence != null? EvidenceQualifiersReader.extractInformationType(evidence): null),
				(evidence != null? EvidenceQualifiersReader.extractEvidenceType(evidence): null),
				SourceCitationReader.citationWeight(citation), FLEFRecordHelper.findChildren(citation, SourceCitationReader.TAG_EXTRACT).size()));
		}
		return List.copyOf(citations);
	}

	private static List<Finding> findings(final String status, final String narrative, final List<String> questions,
			final List<Assertion> assertions){
		final List<Finding> findings = new ArrayList<>();
		if(questions.isEmpty())
			findings.add(new Finding(FindingKind.NO_QUESTION, null));
		if(narrative == null || narrative.isBlank())
			findings.add(new Finding(FindingKind.NO_NARRATIVE, null));
		if(assertions.size() < 2)
			findings.add(new Finding(FindingKind.SINGLE_ASSERTION, null));
		for(final Assertion assertion : assertions)
			if(!assertion.known())
				findings.add(new Finding(FindingKind.UNRESOLVED_TARGET, assertion.id()));

		final Assertion preferred = assertions.stream()
			.filter(Assertion::preferred)
			.findFirst()
			.orElse(null);
		final boolean positive = (ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED.equals(status)
			|| ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(status));
		if(preferred == null){
			if(positive)
				findings.add(new Finding(FindingKind.STATUS_WITHOUT_PREFERRED, null));
		}
		else if(preferred.known()){
			if(preferred.supporting().isEmpty())
				findings.add(new Finding(FindingKind.PREFERRED_UNSUPPORTED, preferred.label()));
			if(!preferred.opposing().isEmpty())
				findings.add(new Finding(FindingKind.PREFERRED_CONTRADICTED, preferred.label()));
			for(final Assertion alternative : assertions)
				if(!alternative.preferred() && alternative.known()){
					if(alternative.weight() > preferred.weight())
						findings.add(new Finding(FindingKind.PREFERRED_OUTWEIGHED, alternative.label()));
					if(!alternative.supporting().isEmpty())
						findings.add(new Finding(FindingKind.RIVAL_SUPPORTED, alternative.label()));
				}
			if(ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(status) && preferred.citations().stream().noneMatch(Citation::directOriginal))
				findings.add(new Finding(FindingKind.PROVEN_WITHOUT_DIRECT_ORIGINAL, preferred.label()));
		}
		return List.copyOf(findings);
	}

	/** @return the value of the alternative a "oneof" holds (an Xref), or {@code null} */
	private static String armValue(final FLEFRecord union){
		return (union == null || union.getChildren().isEmpty()? null: union.getChildren().getFirst().getValue());
	}

	private static String referenceValue(final FLEFRecord reference){
		String id = reference.getValue();
		if(id == null || id.isBlank()){
			final FLEFRecord onlyChild = reference.getTheOnlyChild();
			id = (onlyChild != null? onlyChild.getValue(): null);
		}
		return (id != null && !id.isBlank()? id: null);
	}

}
