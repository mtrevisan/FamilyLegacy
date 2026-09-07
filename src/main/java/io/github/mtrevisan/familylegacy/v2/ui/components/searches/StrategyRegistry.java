package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import java.util.HashMap;
import java.util.Map;


/**
 * Registry that maps record type handlers to their search strategies.
 */
public class StrategyRegistry{

	private final Map<String, SearchStrategy> strategies = new HashMap<>();

	public void register(final RecordTypeHandler<?> handler, final SearchStrategy strategy){
		strategies.put(handler.getType(), strategy);
	}

	public SearchStrategy getStrategy(final RecordTypeHandler<?> handler){
		return strategies.get(handler.getType());
	}

	public boolean isRegistered(final RecordTypeHandler<?> handler){
		return strategies.containsKey(handler.getType());
	}

}
