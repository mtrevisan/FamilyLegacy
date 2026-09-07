package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.JPanel;
import java.util.function.Consumer;


/**
 * Factory for creating type‑specific filter panels.
 */
public class FilterPanelFactory{

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
		// Based on handler type, return appropriate panel.
		// For simplicity, we'll implement only for Individual.
		if(handler.getType().equals("individual")){
			return new IndividualFilterPanel(onChanged);
		}
		// Add other types similarly.
		return new JPanel(); // empty
	}

}
