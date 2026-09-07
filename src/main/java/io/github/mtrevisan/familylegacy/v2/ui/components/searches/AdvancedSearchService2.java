package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


/**
 * Service that executes an advanced search using a strategy registry and text utilities.
 */
public class AdvancedSearchService2{

	private final FLEFModel model;
	private final StrategyRegistry registry;
	private final double fuzzyThreshold;

	public AdvancedSearchService2(final FLEFModel model, final StrategyRegistry registry){
		this(model, registry, 0.05);
	}

	public AdvancedSearchService2(final FLEFModel model, final StrategyRegistry registry, final double fuzzyThreshold){
		this.model = model;
		this.registry = registry;
		this.fuzzyThreshold = fuzzyThreshold;
	}

	/**
	 * Performs the search according to the given criteria.
	 *
	 * @param criteria the search criteria
	 * @return a list of search results
	 */
	public List<SearchResult> search(final SearchCriteria criteria){
		final List<SearchResult> results = new ArrayList<>();

		if(criteria == null || criteria.getHandlerType() == null){
			return results;
		}

		final SearchStrategy strategy = registry.getStrategy(criteria.getHandlerType());
		if(strategy == null){
			// Fallback: use generic text matching only
			return searchWithGenericText(criteria);
		}

		// Build predicate using the strategy
		final Predicate<FLEFRecord> predicate = strategy.buildPredicate(criteria);

		// Apply text matching first (for performance)
		final Predicate<FLEFRecord> textPredicate = buildTextPredicate(criteria);
		final Predicate<FLEFRecord> finalPredicate = textPredicate.and(predicate);

		final List<FLEFRecord> records = model.getRecordsByType(criteria.getHandlerType().getType());
		for(final FLEFRecord record : records){
			if(finalPredicate.test(record)){
				results.add(new SearchResult(record));
			}
		}

		return results;
	}

	private Predicate<FLEFRecord> buildTextPredicate(final SearchCriteria criteria){
		final String searchText = criteria.getSearchText();
		if(searchText == null || searchText.isEmpty()){
			return record -> true;
		}
		return record -> TextSearchUtils.matchesText(
			criteria.getHandlerType().getDisplayText(record, model),
			searchText,
			criteria.isFuzzy(),
			criteria.isWholeWord(),
			fuzzyThreshold
		);
	}

	private List<SearchResult> searchWithGenericText(final SearchCriteria criteria){
		final List<SearchResult> results = new ArrayList<>();
		final Predicate<FLEFRecord> predicate = buildTextPredicate(criteria);
		final List<FLEFRecord> records = model.getRecordsByType(criteria.getHandlerType().getType());
		for(final FLEFRecord record : records){
			if(predicate.test(record)){
				results.add(new SearchResult(record));
			}
		}
		return results;
	}

}
