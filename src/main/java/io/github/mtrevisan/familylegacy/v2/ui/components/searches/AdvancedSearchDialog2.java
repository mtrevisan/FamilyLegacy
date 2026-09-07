package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class AdvancedSearchDialog2 extends JDialog {

	// ... fields, similar to previous version, but with dynamic filter panel
	// We'll use the same pattern as before, but replace the static event filters with a dynamic panel.

	// The filter panel will be recreated when the type changes.
	private JPanel filterPanelHolder = new JPanel(new BorderLayout());
	private IndividualFilterPanel individualFilterPanel; // for now, we'll hard-code for individuals
	// In a full implementation, we'd use a factory and store a reference to the current panel.

	// The rest is similar: type combo, search field, checkboxes, results list, status label, buttons.

	// We'll show the integration code in the constructor.

	public AdvancedSearchDialog2(final Window parent, final FLEFModel model,
		final Class<? extends RecordTypeHandler<?>>[] handlerTypes,
		final Consumer<FLEFRecord> onSelect) {
		super(parent, "Advanced Search", ModalityType.APPLICATION_MODAL);
		// ... initialization

		// Register strategies
		final StrategyRegistry registry = new StrategyRegistry();
		registry.register(IndividualHandler.getInstance(), new IndividualSearchStrategy());
		// Register other strategies...

		final AdvancedSearchService searchService = new AdvancedSearchService(model, registry);

		// ... build UI

		// When type changes, update the filter panel
		typeCombo.addActionListener(e -> updateFilterPanel());
		updateFilterPanel(); // initial

		// Search button and debouncing as before
		// ...

		// When the user changes any filter, we perform a search
		// We can call a method that reads the current values and builds criteria.
	}

	private void updateFilterPanel() {
		final RecordTypeHandler<?> handler = (RecordTypeHandler<?>) typeCombo.getSelectedItem();
		filterPanelHolder.removeAll();
		if (handler != null) {
			// Use factory to create panel
			final JPanel panel = FilterPanelFactory.createPanel(handler, criteria -> performSearch());
			filterPanelHolder.add(panel, BorderLayout.CENTER);
		}
		filterPanelHolder.revalidate();
		filterPanelHolder.repaint();
	}

	private void performSearch() {
		final RecordTypeHandler<?> handler = (RecordTypeHandler<?>) typeCombo.getSelectedItem();
		if (handler == null) return;

		final SearchCriteria criteria = new SearchCriteria(
			handler,
			searchField.getText().trim(),
			fuzzyCheckBox.isSelected(),
			wholeWordCheckBox.isSelected()
		);

		// Read specific filters from the current panel
		// For simplicity, we read from IndividualFilterPanel if it's the current one.
		if (filterPanelHolder.getComponent(0) instanceof IndividualFilterPanel) {
			final IndividualFilterPanel ifp = (IndividualFilterPanel) filterPanelHolder.getComponent(0);
			criteria.withFilter("eventType", ifp.getEventType())
				.withFilter("dateFrom", ifp.getDateFrom())
				.withFilter("dateTo", ifp.getDateTo())
				.withFilter("locationContains", ifp.getLocationContains());
		}

		// Execute search via service and update results
		// ...
	}

}
