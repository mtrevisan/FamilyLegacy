package io.github.mtrevisan.familylegacy.v2.ui.panels;

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.ui.MainFrame;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.4.extractRecordLatitude;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLongitude;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLatitude;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLongitude;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordName;

/**
 * Pannello per la gestione dei luoghi (PLACE_RECORD).
 */
public class PlacePanel extends EntityPanel<Map<String, Object>> {

	// Componenti UI
	private final JTextField nameField = new JTextField(30);
	private final JTextField addressField = new JTextField(40);
	private final JTextField hierarchyField = new JTextField(30);
	private final JTextField latitudeField = new JTextField(15);
	private final JTextField longitudeField = new JTextField(15);
	private final JComboBox<PlaceItem> subordinateComboBox = new JComboBox<>();

	private final JButton notesButton = new JButton("Notes");
	private final JButton mediaButton = new JButton("Media");
	private final JButton sourceButton = new JButton("Sources");
	private final JCheckBox confidentialCheckBox = new JCheckBox("Confidential");

	public PlacePanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final MainFrame mainFrame) {
		super(store, mainFrame);
	}

	@Override
	protected void createTabs() {
		// Scheda "Base"
		final JPanel basePanel = new JPanel(new MigLayout("insets 10", "[right]rel[grow]", ""));
		basePanel.add(new JLabel("Name:"), "align label");
		basePanel.add(nameField, "grow,wrap");
		basePanel.add(new JLabel("Address:"), "align label");
		basePanel.add(addressField, "grow,wrap");
		basePanel.add(new JLabel("Hierarchy:"), "align label");
		basePanel.add(hierarchyField, "grow,wrap");
		basePanel.add(new JLabel("Latitude:"), "align label");
		basePanel.add(latitudeField, "grow,wrap");
		basePanel.add(new JLabel("Longitude:"), "align label");
		basePanel.add(longitudeField, "grow,wrap");
		basePanel.add(new JLabel("Subordinate:"), "align label");
		basePanel.add(subordinateComboBox, "grow");

		// Scheda "Other"
		final JPanel otherPanel = new JPanel(new MigLayout("insets 10", "[center]", ""));
		otherPanel.add(notesButton, "split 3, gap 10");
		otherPanel.add(mediaButton);
		otherPanel.add(sourceButton, "wrap");
		otherPanel.add(confidentialCheckBox, "gapy 10");

		tabbedPane.addTab("Base", basePanel);
		tabbedPane.addTab("Other", otherPanel);

		// Popola il combo dei subordinate
		populateSubordinateComboBox();
	}

	private void populateSubordinateComboBox() {
		subordinateComboBox.removeAllItems();
		subordinateComboBox.addItem(null);
		final TreeMap<Integer, Map<String, Object>> places = store.get(EntityManager.TABLE_NAME_PLACE);
		if (places != null) {
			for (final Map.Entry<Integer, Map<String, Object>> entry : places.entrySet()) {
				final Integer id = entry.getKey();
				final String name = extractRecordName(entry.getValue());
				final String label = (name != null ? name : "Place " + id);
				subordinateComboBox.addItem(new PlaceItem(id, label));
			}
		}
	}

	@Override
	public void loadEntity(final Integer id) {
		super.loadEntity(id);
		// Se il subordinateComboBox è stato popolato, seleziona il valore corretto
		if (currentRecord != null) {
			final Integer subId = (Integer) currentRecord.get("subordinate_id");
			if (subId != null) {
				for (int i = 0; i < subordinateComboBox.getItemCount(); i++) {
					final PlaceItem item = subordinateComboBox.getItemAt(i);
					if (item != null && Objects.equals(item.id, subId)) {
						subordinateComboBox.setSelectedItem(item);
						break;
					}
				}
			}
		}
	}

	@Override
	protected void fillData() {
		if (currentRecord == null) {
			return;
		}
		setIgnoreEvents(true);
		try {
			nameField.setText(extractRecordName(currentRecord));
			addressField.setText((String) currentRecord.get("address"));
			hierarchyField.setText((String) currentRecord.get("hierarchy"));
			latitudeField.setText(extractRecordLatitude(currentRecord));
			longitudeField.setText(extractRecordLongitude(currentRecord));

			// Subordinate (gestito in loadEntity)
			// Confidential
			final String restriction = getRestriction();
			confidentialCheckBox.setSelected(EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
		} finally {
			setIgnoreEvents(false);
		}
	}

	@Override
	protected void clearData() {
		setIgnoreEvents(true);
		try {
			nameField.setText("");
			addressField.setText("");
			hierarchyField.setText("");
			latitudeField.setText("");
			longitudeField.setText("");
			subordinateComboBox.setSelectedItem(null);
			confidentialCheckBox.setSelected(false);
		} finally {
			setIgnoreEvents(false);
		}
	}

	@Override
	protected boolean validateData() {
		final String name = nameField.getText().trim();
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Name is required", "Validation Error", JOptionPane.ERROR_MESSAGE);
			nameField.requestFocusInWindow();
			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData() {
		if (currentRecord == null) {
			return false;
		}
		setIgnoreEvents(true);
		try {
			// Campi base
			insertRecordName(currentRecord, nameField.getText().trim());
			currentRecord.put("address", addressField.getText().trim());
			currentRecord.put("hierarchy", hierarchyField.getText().trim());
			insertRecordLatitude(currentRecord, latitudeField.getText().trim());
			insertRecordLongitude(currentRecord, longitudeField.getText().trim());

			// Subordinate
			final PlaceItem selectedSub = (PlaceItem) subordinateComboBox.getSelectedItem();
			currentRecord.put("subordinate_id", selectedSub != null ? selectedSub.id : null);

			// Se è un nuovo record, assegna un ID
			if (currentId == null) {
				final TreeMap<Integer, Map<String, Object>> table = store.get(EntityManager.TABLE_NAME_PLACE);
				int newId = 1;
				if (table != null && !table.isEmpty()) {
					newId = table.lastKey() + 1;
				}
				insertRecordID(currentRecord, newId);
				currentId = newId;
			}

			// Salva nel store
			final TreeMap<Integer, Map<String, Object>> table = store.get(EntityManager.TABLE_NAME_PLACE);
			if (table != null) {
				table.put(currentId, currentRecord);
			}

			// Gestisci la restrizione
			final String restriction = confidentialCheckBox.isSelected() ? EntityManager.RESTRICTION_CONFIDENTIAL : null;
			// TODO: salvare la restrizione in una tabella separata

			// Ricarica il combo dei subordinate (perché potrebbe essere cambiato)
			populateSubordinateComboBox();

			// Notifica il MainFrame per aggiornare la lista
			mainFrame.refreshEntityList(getTableName());

			return true;
		} finally {
			setIgnoreEvents(false);
		}
	}

	@Override
	protected Map<String, Object> createNewRecord() {
		return new HashMap<>();
	}

	@Override
	protected String getTableName() {
		return EntityManager.TABLE_NAME_PLACE;
	}

	private String getRestriction() {
		// TODO: implementare la lettura dalla tabella restriction
		return null;
	}

	// Classe interna per gli item del combo
	private static class PlaceItem {
		final Integer id;
		final String label;

		PlaceItem(final Integer id, final String label) {
			this.id = id;
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	// Metodo factory per creare il pannello lista (da usare nel MainFrame)
	public static JPanel createListPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store,
		final MainFrame mainFrame,
		final java.util.function.Consumer<Integer> onSelection) {
		// Implementazione semplice: una lista con JList
		final JPanel panel = new JPanel(new MigLayout("insets 0, fill", "[grow]", "[grow]"));
		final TreeMap<Integer, Map<String, Object>> places = store.get(EntityManager.TABLE_NAME_PLACE);
		final String[] items;
		if (places != null && !places.isEmpty()) {
			items = places.entrySet().stream()
				.map(e -> e.getKey() + ": " + extractRecordName(e.getValue()))
				.toArray(String[]::new);
		} else {
			items = new String[]{"No places"};
		}
		final javax.swing.JList<String> list = new javax.swing.JList<>(items);
		list.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && onSelection != null) {
				final int idx = list.getSelectedIndex();
				if (idx >= 0 && places != null) {
					final Integer id = (Integer) places.keySet().toArray()[idx];
					onSelection.accept(id);
				}
			}
		});
		panel.add(new javax.swing.JScrollPane(list), "grow");
		return panel;
	}
}
