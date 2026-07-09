package io.github.mtrevisan.familylegacy.v2.ui.panels;

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.ui.MainFrame;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Pannello base per la visualizzazione e modifica di un'entità FLEF.
 * @param <T> tipo di record (Map<String, Object>)
 */
public abstract class EntityPanel<T extends Map<String, Object>> extends JPanel {

	protected final Map<String, TreeMap<Integer, T>> store;
	protected final MainFrame mainFrame;
	protected T currentRecord;
	protected Integer currentId;

	// Componenti comuni
	protected final JTabbedPane tabbedPane = new JTabbedPane();
	protected boolean ignoreEvents = false;

	public EntityPanel(final Map<String, TreeMap<Integer, T>> store, final MainFrame mainFrame) {
		this.store = store;
		this.mainFrame = mainFrame;
		setLayout(new MigLayout("insets 0, fill", "[grow]", "[grow]"));
		initUI();
	}

	private void initUI() {
		// Pannello principale con schede
		add(tabbedPane, "grow");

		// Crea le schede
		createTabs();
	}

	/**
	 * Crea le schede del pannello. Deve essere implementato dalle sottoclassi.
	 * Tipicamente: "Base", "Details", "Links", "History", "Research".
	 */
	protected abstract void createTabs();

	/**
	 * Carica un'entità esistente.
	 * @param id ID dell'entità
	 */
	public void loadEntity(final Integer id) {
		this.currentId = id;
		final String tableName = getTableName();
		final TreeMap<Integer, T> table = store.get(tableName);
		if (table != null && table.containsKey(id)) {
			this.currentRecord = table.get(id);
			fillData();
		} else {
			// Gestione errore: entità non trovata
			loadNewEntity();
		}
	}

	/**
	 * Crea una nuova entità.
	 */
	public void loadNewEntity() {
		this.currentId = null;
		this.currentRecord = createNewRecord();
		clearData();
	}

	/**
	 * Salva l'entità corrente.
	 * @return true se il salvataggio è riuscito
	 */
	public boolean saveEntity() {
		if (currentRecord == null) {
			return false;
		}
		if (!validateData()) {
			return false;
		}
		final boolean saved = saveData();
		if (saved && currentId == null) {
			// Assegna un nuovo ID
			currentId = EntityManager.extractRecordID(currentRecord);
		}
		return saved;
	}

	/**
	 * Salva i dati dal form al record.
	 * @return true se il salvataggio è riuscito
	 */
	protected abstract boolean saveData();

	/**
	 * Riempie il form con i dati del record corrente.
	 */
	protected abstract void fillData();

	/**
	 * Pulisce il form per un nuovo record.
	 */
	protected abstract void clearData();

	/**
	 * Valida i dati prima del salvataggio.
	 * @return true se i dati sono validi
	 */
	protected abstract boolean validateData();

	/**
	 * Crea un nuovo record vuoto.
	 * @return il nuovo record
	 */
	protected abstract T createNewRecord();

	/**
	 * Restituisce il nome della tabella/entità.
	 * @return nome della tabella (es. "individual", "place")
	 */
	protected abstract String getTableName();

	/**
	 * Metodo factory per creare un pannello lista per questa entità.
	 * @param onSelection azione da eseguire quando viene selezionato un elemento
	 * @return il pannello lista
	 */
	public static <T extends Map<String, Object>> JPanel createListPanel(
		final Map<String, TreeMap<Integer, T>> store,
		final MainFrame mainFrame,
		final Consumer<Integer> onSelection) {
		// Implementazione di default: una semplice lista
		final JPanel panel = new JPanel(new MigLayout("insets 0, fill", "[grow]", "[grow]"));
		panel.add(new javax.swing.JLabel("Lista non implementata per questa entità"), "grow");
		return panel;
	}

	// Metodi di utilità per le sottoclassi
	protected void setIgnoreEvents(final boolean ignore) {
		this.ignoreEvents = ignore;
	}

	protected boolean isValidText(final String text) {
		return text != null && !text.trim().isEmpty();
	}
}
