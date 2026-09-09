package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ConclusionSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.CulturalNormSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.DocumentSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.EventSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.GroupSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.HistoricEventSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.IdentityHypothesisSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.IndividualSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.PlaceSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.RepositorySearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchActivitySearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchQuestionSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchTaskSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.SourceSearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * Service that executes an advanced search using a strategy registry and text utilities.
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


	private final FLEFModel model;
	private final double fuzzyThreshold;

	private SearchStrategy strategy;


	public SearchService(final FLEFModel model){
		this(model, 0.05);
	}

	public SearchService(final FLEFModel model, final double fuzzyThreshold){
		this.model = model;
		this.fuzzyThreshold = fuzzyThreshold;
	}


	/**
	 * Performs the search according to the given criteria.
	 *
	 * @param criteria the search criteria
	 * @return a list of search results
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria){
		return search(criteria, null);
	}

	/**
	 * Performs the search according to the given criteria and reports progress.
	 *
	 * @param criteria         the search criteria
	 * @param progressCallback consumer for reporting progress percentage (0-100)
	 * @return a list of search results
	 */
	public List<FLEFRecord> search(final SearchCriteria criteria, final Consumer<Integer> progressCallback){
		if(criteria == null || criteria.getHandler() == null)
			return List.of();

		strategy = REGISTRY.get(criteria.getHandler().getClassType());

		// Build predicate using the strategy
		// Apply text matching first (for performance)
		final Predicate<FLEFRecord> predicate = buildTextPredicate(criteria)
			.and(strategy.buildPredicate(criteria, model));

		final List<FLEFRecord> records = model.getRecordsByType(criteria.getHandler().getType());
		return executeSearch(records, predicate, progressCallback);
	}

	private static List<FLEFRecord> executeSearch(final List<FLEFRecord> records,
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

	private Predicate<FLEFRecord> buildTextPredicate(final SearchCriteria criteria){
		final String searchText = criteria.getSearchText();
		if(searchText == null || searchText.isEmpty())
			return record -> true;

		return record -> TextSearchHelper.matchesText(
			criteria.getHandler().getDisplayText(record, model),
			searchText,
			criteria.isFuzzy(),
			criteria.isWholeWord(),
			fuzzyThreshold
		);
	}


	public String getDisplayText(final FLEFRecord record){
		return strategy.getDisplayText(record, model);
	}

}
