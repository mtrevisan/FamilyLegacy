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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;


/**
 * Merges FLEF files, and finds the duplicates inside one file, following the Merge and Deletion rules of the protocol.
 * <p>
 * The purpose is to bring together the research of two files (or two imports) without losing evidence and without asserting
 * identities the evidence does not support:
 * <ul>
 *   <li>ids are local to a file: the incoming records whose id is already used by the base are renamed, and every reference and
 *     {@code flef:} link is updated;</li>
 *   <li>only the kinds that can be the subject of an identity hypothesis (individuals, groups, places, sources, repositories, media,
 *     cultural norms, historic events) are compared; a pair is merged only above the automatic threshold, while an uncertain pair
 *     becomes an IdentityHypothesisRecord for the researcher to evaluate;</li>
 *   <li>a merge keeps the survivor, unites the repeated fields, never assigns a singular field twice, keeps the most restrictive
 *     privacy, redirects every reference to the absorbed record, and records the absorbed id and the evidence in an
 *     {@code audit.update} of the survivor;</li>
 *   <li>the association records that end up stating the same assertion are collapsed uniting their evidence; competing
 *     assertions are preserved and reported, to be resolved with ConclusionRecords;</li>
 *   <li>the identity hypotheses accepted by a merge are deleted, with the cascade the protocol prescribes.</li>
 * </ul>
 * The input models are never modified.
 */
public final class FLEFMerger{

	/** Pairs at or above this probability are merged automatically. */
	public static final double DEFAULT_AUTO_THRESHOLD = 0.95;
	/** Pairs at or above this probability, and below the automatic threshold, become identity hypotheses. */
	public static final double DEFAULT_REVIEW_THRESHOLD = 0.5;

	/** Rounds of comparison of the relational kinds, each using the merges of the previous one. */
	private static final int MAX_RELATIONAL_ROUNDS = 5;


	private final FLEFGrammar grammar;
	private final GrammarSupport grammarSupport;
	private final double autoThreshold;
	private final double reviewThreshold;
	private final Clock clock;


	public FLEFMerger(final FLEFGrammar grammar){
		this(grammar, DEFAULT_AUTO_THRESHOLD, DEFAULT_REVIEW_THRESHOLD, Clock.systemDefaultZone());
	}

	/**
	 * @param grammar         the FLEF grammar, which drives references and field cardinalities
	 * @param autoThreshold   probability at or above which two records are merged; above {@code 1} nothing is merged automatically
	 *                        and every candidate becomes a hypothesis
	 * @param reviewThreshold probability at or above which a pair becomes an identity hypothesis
	 * @param clock           the clock of the audit entries
	 */
	public FLEFMerger(final FLEFGrammar grammar, final double autoThreshold, final double reviewThreshold, final Clock clock){
		if(reviewThreshold < 0. || reviewThreshold > autoThreshold)
			throw new IllegalArgumentException("Thresholds must satisfy 0 <= review <= auto, found review " + reviewThreshold
				+ ", auto " + autoThreshold);

		this.grammar = grammar;
		grammarSupport = new GrammarSupport(grammar);
		this.autoThreshold = autoThreshold;
		this.reviewThreshold = reviewThreshold;
		this.clock = clock;
	}


	/**
	 * Merges an incoming model into a base model.
	 * <p>
	 * The base wins: its ids never change, its records survive the merges, and its header is kept (gaining the submitter contacts of
	 * the incoming file). Records of the same model are never compared with each other.
	 *
	 * @param base     the model to merge into
	 * @param incoming the model to merge
	 * @return the merged model and the decisions taken; {@link MergeReport#idMapping()} tells where each incoming record ended
	 */
	public MergeReport merge(final FLEFModel base, final FLEFModel incoming){
		return run(base, incoming);
	}

	/**
	 * Finds and merges the records of a model that describe the same entity.
	 *
	 * @param model the model to deduplicate
	 * @return the deduplicated model and the decisions taken
	 */
	public MergeReport deduplicate(final FLEFModel model){
		return run(model, null);
	}

	private MergeReport run(final FLEFModel base, final FLEFModel incoming){
		final MergeSession session = new MergeSession(grammar, grammarSupport, now(clock), incoming != null);
		session.load(base, incoming);

		final EntityLinker linker = new EntityLinker(session, autoThreshold, reviewThreshold);
		int totalMerges = 0;
		final Map<String, Integer> mergesAtLastLink = new HashMap<>();
		for(final String kind : EntityMatcher.MATCHABLE_KINDS){
			totalMerges += linker.link(kind);
			mergesAtLastLink.put(kind, totalMerges);
		}
		// individuals, groups, places, and sources are compared also through the records related to them: each round of merges is
		// new evidence for the next one (once the parents are merged their children have the same parents, once the individuals
		// are merged the places of their births are the same use)
		for(int round = 0; round < MAX_RELATIONAL_ROUNDS; round ++){
			final int mergesBeforeRound = totalMerges;
			for(final String kind : EntityMatcher.RELATIONAL_KINDS)
				// nothing changed since the last comparison of this kind, so nothing new would be found
				if(mergesAtLastLink.get(kind) < totalMerges){
					totalMerges += linker.link(kind);
					mergesAtLastLink.put(kind, totalMerges);
				}
			if(totalMerges == mergesBeforeRound)
				break;
		}

		final ProtocolRules rules = new ProtocolRules(grammarSupport, session.references, session.fields);
		new AssertionDeduplicator(session, session.references, session.signature, session.fields, rules, session.now,
			AssertionDeduplicator.sessionPolicy(session))
			.run();
		new ProtocolCleanup(session, rules, reviewThreshold)
			.run(linker.reviewCandidates());

		return session.report();
	}

	/**
	 * The current date and time, as a FLEF {@code DateTime}.
	 */
	static String now(final Clock clock){
		return OffsetDateTime.now(clock)
			.truncatedTo(ChronoUnit.SECONDS)
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

}
