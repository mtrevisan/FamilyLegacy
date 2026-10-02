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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.ConclusionSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.CulturalNormSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.DocumentSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.EventSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.GroupSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.HistoricEventSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.IdentityHypothesisSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.IndividualSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.PlaceSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.RepositorySearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.ResearchActivitySearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.ResearchQuestionSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.ResearchTaskSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.SourceSearchStrategy;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Service that executes an advanced search using a strategy registry and
 * a word-level text matcher.
 * <p>
 * The search has two stages:
 * <ol>
 *   <li>the <b>structural filters</b> (sex, event type, date range, place,
 *       ...) are applied by the per-type {@link SearchStrategy};</li>
 *   <li>the <b>text query</b> is scored against the display text of each
 *       surviving record by {@link SearchMatcher}.</li>
 * </ol>
 * The results are ranked by the number of matched query tokens first, by
 * the sum of the per-token fuzzy scores second, and by display text third
 * (stable, alphabetical tie-breaker). A record that matches both words of
 * a two-word query always ranks above a record that matches only one.
 * <p>
 * When the query is blank, the text stage is skipped and the results are
 * the structurally-filtered records in their original order.
 */
public class SearchService{

	private static final Map<Class<? extends RecordTypeHandler<?>>, SearchStrategy> REGISTRY = new HashMap<>();

	static{
		REGISTRY.put(ConclusionHandler.class, new ConclusionSearchStrategy());
		REGISTRY.put(CulturalNormHandler.class, new CulturalNormSearchStrategy());
		REGISTRY.put(DocumentHandler.class, new DocumentSearchStrategy());
		REGISTRY.put(EventHandler.class, new EventSearchStrategy());
		REGISTRY.put(GroupHandler.class, new GroupSearchStrategy());
		REGISTRY.put(HistoricEventHandler.class, new HistoricEventSearchStrategy());
		REGISTRY.put(IdentityHypothesisHandler.class, new IdentityHypothesisSearchStrategy());
		REGISTRY.put(IndividualHandler.class, new IndividualSearchStrategy());
		REGISTRY.put(PlaceHandler.class, new PlaceSearchStrategy());
		REGISTRY.put(RepositoryHandler.class, new RepositorySearchStrategy());
		REGISTRY.put(ResearchActivityHandler.class, new ResearchActivitySearchStrategy());
		REGISTRY.put(ResearchQuestionHandler.class, new ResearchQuestionSearchStrategy());
		REGISTRY.put(ResearchTaskHandler.class, new ResearchTaskSearchStrategy());
		REGISTRY.put(SourceHandler.class, new SourceSearchStrategy());
	}


	/**
	 * A record that passed the text stage, together with the score that
	 * determines its position in the result list.
	 */
	private record ScoredRecord(FLEFRecord record, String displayText, SearchMatcher.MatchScore match){}


	private final FLEFModel model;

	private SearchStrategy strategy;


	public SearchService(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Performs the search according to the given criteria.
	 *
	 * @param criteria the search criteria
	 * @return a list of search results, ordered by match quality
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria){
		return search(criteria, null);
	}

	/**
	 * Performs the search according to the given criteria and reports progress.
	 *
	 * @param criteria         the search criteria
	 * @param progressCallback consumer for reporting progress percentage (0-100)
	 * @return a list of search results, ordered by match quality
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria, final Consumer<Integer> progressCallback){
		if(criteria == null)
			return List.of();

		final RecordTypeHandler<?> handler = criteria.handler();
		strategy = REGISTRY.get(handler.getClassType());

		// Stage 1: structural filters (sex, event type, date range, place, ...).
		final Predicate<FLEFRecord> structuralFilter = strategy.buildPredicate(criteria, model);

		final List<FLEFRecord> records = model.getRecordsByType(handler.getType());

		// Fast path: no text query. Structural filters only, no ranking.
		final String query = criteria.query();
		if(query.isEmpty())
			return executeStructuralSearch(records, structuralFilter, progressCallback);

		// Stage 2: text scoring and ranking. The display text used for
		// scoring is the one returned by the handler, which is what the
		// old TextSearchHelper path used; the enriched display text of
		// the strategy (with the "[M] " prefix and the birth/death
		// details) is only used for the final rendering, not for the
		// text match, so that the behaviour of the search is unchanged.
		final SearchMode mode = criteria.mode();
		final List<ScoredRecord> scored = new ArrayList<>();
		final int totalRecords = records.size();
		for(int i = 0; i < totalRecords; i ++){
			if(Thread.currentThread().isInterrupted())
				break;

			final FLEFRecord record = records.get(i);
			if(structuralFilter.test(record)){
				final String displayText = handler.getDisplayText(record, model);
				final SearchMatcher.MatchScore match = SearchMatcher.score(query, displayText, mode);
				if(match.passes())
					scored.add(new ScoredRecord(record, displayText, match));
			}

			if(progressCallback != null){
				final int progressPercent = (int)(((i + 1) / (double)totalRecords) * 100);
				progressCallback.accept(progressPercent);
			}
		}

		// Ranking: number of matched query tokens first (a record matching
		// "bort" and "gall" beats a record matching only one of the two),
		// then the sum of the per-token fuzzy scores (a full match beats
		// a fuzzy match), then display text for a stable, alphabetical
		// tie-break.
		scored.sort(Comparator
			.comparingInt((ScoredRecord sr) -> sr.match().matchedTokens()).reversed()
			.thenComparing(Comparator.comparingDouble(
				(ScoredRecord sr) -> sr.match().score()).reversed())
			.thenComparing(ScoredRecord::displayText, String.CASE_INSENSITIVE_ORDER));

		final List<FLEFRecord> results = new ArrayList<>(scored.size());
		for(final ScoredRecord sr : scored)
			results.add(sr.record());
		return results;
	}

	/**
	 * Structural search without a text query: applies the filters and
	 * preserves the original order of the records.
	 */
	private static List<FLEFRecord> executeStructuralSearch(final List<FLEFRecord> records,
			final Predicate<FLEFRecord> predicate, final Consumer<Integer> progressCallback){
		final List<FLEFRecord> results = new ArrayList<>();
		final int totalRecords = records.size();
		for(int i = 0; i < totalRecords; i ++){
			if(Thread.currentThread().isInterrupted())
				break;

			final FLEFRecord record = records.get(i);
			if(predicate.test(record))
				results.add(record);

			if(progressCallback != null){
				final int progressPercent = (int)(((i + 1) / (double)totalRecords) * 100);
				progressCallback.accept(progressPercent);
			}
		}

		return results;
	}

	public String getDisplayText(final FLEFRecord record){
		return strategy.getDisplayText(record, model);
	}

}
