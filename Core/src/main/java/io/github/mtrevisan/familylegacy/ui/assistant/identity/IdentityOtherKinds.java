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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.RecordAbsorber;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticKeys;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;


/**
 * The records that are not individuals and may be the same entity: groups, places, sources, repositories, media, cultural norms and
 * historic events, the other kinds an identity hypothesis can be about. The comparison is the one of the automatic merge
 * ({@link RecordAbsorber#duplicatePairs}: names, jurisdictions, coordinates, titles, authors, files...), which has its own for each kind;
 * the pairs that a conclusion has disproven are not proposed, and neither are those with a hypothesis already.
 * <p>
 * Like the finder of individuals, it only suggests: nothing is merged, and the researcher records the possibility as a hypothesis (a
 * draft to confirm), compares the two records, or says that they are different.
 * </p>
 */
final class IdentityOtherKinds{

	/** The kinds, in the order they are listed. */
	static final List<String> KINDS = Stream.of(GroupReader.TYPE, PlaceReader.TYPE, SourceReader.TYPE, RepositoryReader.TYPE, MediaReader.TYPE,
			CulturalNormReader.TYPE, HistoricEventReader.TYPE)
		.map(kind -> kind.toLowerCase(Locale.ROOT))
		.toList();
	/** The probability from which a pair is a strong candidate (a warning), and the confidence proposed is high. */
	static final double HIGH_PROBABILITY = 0.9;


	private IdentityOtherKinds(){}


	/** @return	The findings with the draft of the hypothesis that would record each one; none if the grammar of the merge is not available. */
	static List<IdentityFinding> findings(final FLEFModel model){
		return findings(model, IdentitySettings.DEFAULTS);
	}

	/**
	 * @param model	The model.
	 * @param settings	What the user decided: the probability from which a pair is proposed (the merge reviews from 50 %).
	 * @return	The findings; none if the grammar of the merge is not available.
	 */
	static List<IdentityFinding> findings(final FLEFModel model, final IdentitySettings settings){
		final FLEFGrammar grammar = MergeGrammar.instance();
		if(model == null || grammar == null)
			return List.of();

		final Set<String> hypotheses = existingHypotheses(model);
		final List<IdentityFinding> findings = new ArrayList<>();
		for(final RecordAbsorber.DuplicatePair pair : new RecordAbsorber(grammar)
				.duplicatePairs(model, KINDS, settings.otherProbability(), Integer.MAX_VALUE)){
			final FLEFRecord first = model.getRecordById(pair.firstId());
			final FLEFRecord second = model.getRecordById(pair.secondId());
			if(first == null || second == null || PrivacyService.isHidden(model, first) || PrivacyService.isHidden(model, second)
					|| hypotheses.contains(pairKey(pair.firstId(), pair.secondId())))
				continue;

			findings.add(finding(model, pair, first, second));
		}
		return findings;
	}

	static IdentityFinding finding(final FLEFModel model, final RecordAbsorber.DuplicatePair pair, final FLEFRecord first,
			final FLEFRecord second){
		final String kind = I18N.t("confirmation.exist_record." + pair.kind().toLowerCase(Locale.ROOT), pair.kind());
		final String left = RecordDisplay.describe(first, model);
		final String right = RecordDisplay.describe(second, model);
		final int percent = (int)Math.round(pair.probability() * 100.);
		final String confidence = confidence(pair.probability());
		final List<String> evidence = List.of(StringUtils.splitByWholeSeparator(pair.rationale(), "; "));

		final Diagnostic diagnostic = new Diagnostic((pair.probability() >= HIGH_PROBABILITY? Diagnostic.Severity.WARNING
			: Diagnostic.Severity.INFORMATION), AssistantCategory.IDENTITY,
			I18N.tf("identity.possible_duplicate_kind.title", kind, left, right, percent),
			I18N.tf("identity.possible_duplicate_kind.explanation", kind, left, right, percent, I18N.t("enum.confidence." + confidence),
				pair.rationale()),
			List.of(pair.firstId(), pair.secondId()), null,
			DiagnosticKeys.of("identity.possible_duplicate", List.of(pair.firstId(), pair.secondId())), List.of(), percent);
		return new IdentityFinding(diagnostic, new IdentityDraft(pair.firstId(), pair.secondId(),
			I18N.tf("identity.rationale_kind", percent, pair.rationale()), confidence, evidence, List.of(), List.of()));
	}

	/** @return	The confidence proposed for the hypothesis of a pair with that probability. */
	static String confidence(final double probability){
		if(probability >= HIGH_PROBABILITY)
			return ConfidenceReader.ENUM_HIGH;
		return (probability >= 0.8? ConfidenceReader.ENUM_MEDIUM: ConfidenceReader.ENUM_LOW);
	}

	/** @return	The pairs (the smaller identifier first, then {@code |}, then the other) that an identity hypothesis already covers. */
	private static Set<String> existingHypotheses(final FLEFModel model){
		final Set<String> pairs = new HashSet<>();
		for(final FLEFRecord hypothesis : model.getRecordsByType(IdentityHypothesisReader.TYPE)){
			final List<String> ids = new ArrayList<>(2);
			for(final FLEFRecord identity : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY)){
				final FLEFRecord reference = identity.getTheOnlyChild();
				if(reference != null && reference.getValue() != null)
					ids.add(reference.getValue());
			}
			if(ids.size() >= 2)
				pairs.add(pairKey(ids.get(0), ids.get(1)));
		}
		return pairs;
	}

	//the pair is the same whatever the order of its two records
	private static String pairKey(final String first, final String second){
		return (first.compareTo(second) <= 0? first + "|" + second: second + "|" + first);
	}

}
