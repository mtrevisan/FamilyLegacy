package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import java.util.Map;


/**
 * Interface implemented by all UI filter panels to populate search criteria dynamically.
 */
public interface RecordFilterPanel{

	/**
	 * Extracts UI input controls state into key-value search filter pairs.
	 *
	 * @return A map containing active filter keys and their corresponding values.
	 */
	Map<String, String> getFilters();

}
