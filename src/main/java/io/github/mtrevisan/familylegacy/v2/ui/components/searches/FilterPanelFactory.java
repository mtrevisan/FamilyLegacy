package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ConclusionFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.CulturalNormFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.DocumentFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.EventFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.GroupFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.HistoricEventFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.IdentityHypothesisFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.IndividualFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.PlaceFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.RepositoryFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchActivityFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchQuestionFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.ResearchTaskFilterPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies.SourceFilterPanel;
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

import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Factory for creating type‑specific filter panels.
 */
public class FilterPanelFactory{

	private static final Map<Class<? extends RecordTypeHandler<?>>, Function<Consumer<SearchCriteria>, RecordFilterPanel>> REGISTRY = new HashMap<>();
	static {
		REGISTRY.put(ConclusionHandler.class, ConclusionFilterPanel::new);
		REGISTRY.put(CulturalNormHandler.class, CulturalNormFilterPanel::new);
		REGISTRY.put(DocumentHandler.class, DocumentFilterPanel::new);
		REGISTRY.put(EventHandler.class, EventFilterPanel::new);
		REGISTRY.put(GroupHandler.class, GroupFilterPanel::new);
		REGISTRY.put(HistoricEventHandler.class, HistoricEventFilterPanel::new);
		REGISTRY.put(IdentityHypothesisHandler.class, IdentityHypothesisFilterPanel::new);
		REGISTRY.put(IndividualHandler.class, IndividualFilterPanel::new);
		REGISTRY.put(PlaceHandler.class, PlaceFilterPanel::new);
		REGISTRY.put(RepositoryHandler.class, RepositoryFilterPanel::new);
		REGISTRY.put(ResearchActivityHandler.class, ResearchActivityFilterPanel::new);
		REGISTRY.put(ResearchQuestionHandler.class, ResearchQuestionFilterPanel::new);
		REGISTRY.put(ResearchTaskHandler.class, ResearchTaskFilterPanel::new);
		REGISTRY.put(SourceHandler.class, SourceFilterPanel::new);
	}


	private FilterPanelFactory(){}


	/**
	 * Creates a panel with input fields for the given record type.
	 * The consumer is called whenever a filter value changes.
	 *
	 * @param handler   the record type handler
	 * @param onChanged callback to update the search criteria
	 * @return a JPanel containing the filters
	 */
	public static JPanel createPanel(final RecordTypeHandler<?> handler,
			final Consumer<SearchCriteria> onChanged){
		if(handler == null)
			return null;

		final Function<Consumer<SearchCriteria>, RecordFilterPanel> builder = REGISTRY.get(handler.getClass());
		return (builder != null? (JPanel)builder.apply(onChanged): null);
	}

}
