package io.github.mtrevisan.familylegacy.v2.ui2;

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.v2.ui2.panels.EntityPanel;
import io.github.mtrevisan.familylegacy.v2.ui2.panels.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui2.panels.PlacePanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Finestra principale dell'applicazione FLEF.
 * Gestisce la navigazione tra le diverse entità e mantiene il contesto.
 */
public class MainFrame extends JFrame {

	@Serial
	private static final long serialVersionUID = 1L;

	// Store dei dati (condiviso)
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	// Componenti UI
	private final JPanel breadcrumbPanel = new JPanel(new MigLayout("insets 2", "[][grow]"));
	private final JPanel centerPanel = new JPanel(new CardLayout());
	private final JPanel entityListPanel = new JPanel(new MigLayout("insets 0, fill", "[grow]", "[grow]"));

	// Stato di navigazione
	private String currentEntityType;
	private Integer currentEntityId;
	private final Map<String, EntityPanel<?>> entityPanels = new HashMap<>();
	private final Map<String, Runnable> entityListLoaders = new HashMap<>();

	public MainFrame(final Map<String, TreeMap<Integer, Map<String, Object>>> store) {
		super("FLEF - Family Legacy Format");
		this.store = store;
		initUI();
		registerEntityTypes();
		loadEntityList("individual");
	}

	private void initUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		// Barra superiore: navigazione + breadcrumb
		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(createNavBar(), BorderLayout.NORTH);
		topPanel.add(breadcrumbPanel, BorderLayout.SOUTH);
		add(topPanel, BorderLayout.NORTH);

		// Split pane: lista a sinistra, dettaglio a destra
		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			entityListPanel, centerPanel);
		splitPane.setDividerLocation(300);
		splitPane.setResizeWeight(0.3);
		add(splitPane, BorderLayout.CENTER);

		// Status bar in basso (opzionale)
		add(createStatusBar(), BorderLayout.SOUTH);
	}

	private Component createNavBar() {
		final JPanel navPanel = new JPanel(new MigLayout("insets 2", "[][][][][]push[]"));
		navPanel.add(new JLabel("Entity:"));

		final JComboBox<String> entitySelector = new JComboBox<>(new String[]{
			"individual", "family", "group", "event", "place", "note",
			"repository", "source", "cultural_norm", "calendar",
			"historic_event", "research_status", "dna_match"
		});
		entitySelector.addActionListener(e -> {
			final String type = (String) entitySelector.getSelectedItem();
			loadEntityList(type);
		});
		navPanel.add(entitySelector, "w 150");

		final JButton addButton = new JButton("+ Add");
		addButton.addActionListener(e -> {
			final String type = (String) entitySelector.getSelectedItem();
			createNewEntity(type);
		});
		navPanel.add(addButton);

		// Pulsante per salvare tutto (opzionale)
		final JButton saveAllButton = new JButton("Save All");
		saveAllButton.addActionListener(e -> saveAll());
		navPanel.add(saveAllButton, "gapleft 30");

		// Pulsante per esportare (opzionale)
		final JButton exportButton = new JButton("Export GEDCOM");
		exportButton.addActionListener(e -> exportGedcom());
		navPanel.add(exportButton);

		return navPanel;
	}

	private Component createStatusBar() {
		final JPanel statusBar = new JPanel(new MigLayout("insets 2", "[left]push[right]"));
		statusBar.add(new JLabel("Ready"));
		statusBar.add(new JLabel("FLEF 0.0.9"));
		return statusBar;
	}

	private void registerEntityTypes() {
		// Registra i loader per la lista delle entità
		entityListLoaders.put("individual", () -> showEntityList("individual", IndividualPanel::createListPanel));
		entityListLoaders.put("place", () -> showEntityList("place", PlacePanel::createListPanel));
		// TODO: aggiungere gli altri tipi
	}

	private void loadEntityList(final String entityType) {
		currentEntityType = entityType;
		final Runnable loader = entityListLoaders.get(entityType);
		if (loader != null) {
			loader.run();
		} else {
			// Fallback: mostra un pannello vuoto
			entityListPanel.removeAll();
			entityListPanel.add(new JLabel("No loader for " + entityType), "grow");
			entityListPanel.revalidate();
			entityListPanel.repaint();
		}
		// Carica il primo elemento (o nessuno)
		loadEntityDetail(entityType, null);
	}

	private void showEntityList(final String entityType, final Consumer<Component> listCreator) {
		entityListPanel.removeAll();
		final Component listComponent = (Component) listCreator;
		// Questo è un placeholder: in realtà il pannello lista sarà creato da una factory
		// Per ora usiamo un approccio semplice
		final JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(new JLabel("List of " + entityType + " (coming)"), BorderLayout.CENTER);
		entityListPanel.add(listPanel, "grow");
		entityListPanel.revalidate();
		entityListPanel.repaint();
	}

	/**
	 * Carica il dettaglio di un'entità nel pannello centrale.
	 * @param entityType tipo di entità (es. "individual")
	 * @param entityId ID dell'entità (null per nuovo record)
	 */
	public void loadEntityDetail(final String entityType, final Integer entityId) {
		currentEntityType = entityType;
		currentEntityId = entityId;

		// Ottieni o crea il pannello dell'entità
		EntityPanel<?> panel = entityPanels.get(entityType);
		if (panel == null) {
			panel = createEntityPanel(entityType);
			if (panel == null) {
				return;
			}
			entityPanels.put(entityType, panel);
			centerPanel.add(panel, entityType);
		}

		// Carica i dati
		if (entityId != null) {
			panel.loadEntity(entityId);
		} else {
			panel.loadNewEntity();
		}

		// Mostra il pannello
		final CardLayout cl = (CardLayout) centerPanel.getLayout();
		cl.show(centerPanel, entityType);

		// Aggiorna il breadcrumb
		updateBreadcrumb(entityType, entityId);
	}

	private EntityPanel<?> createEntityPanel(final String entityType) {
		return switch (entityType) {
			case "individual" -> new IndividualPanel(store, this);
			case "place" -> new PlacePanel(store, this);
			// TODO: aggiungere gli altri tipi
			default -> null;
		};
	}

	private void updateBreadcrumb(final String entityType, final Integer entityId) {
		breadcrumbPanel.removeAll();
		breadcrumbPanel.add(new JLabel("Home"), "split 2");
		final JLabel separator = new JLabel("›");
		breadcrumbPanel.add(separator);
		final JLabel entityLabel = new JLabel(entityType + (entityId != null ? " #" + entityId : " (new)"));
		breadcrumbPanel.add(entityLabel);
		breadcrumbPanel.revalidate();
		breadcrumbPanel.repaint();
	}

	private void createNewEntity(final String entityType) {
		loadEntityDetail(entityType, null);
	}

	private void saveAll() {
		for (final EntityPanel<?> panel : entityPanels.values()) {
			panel.saveEntity();
		}
		// Opzionale: notifica all'utente
	}

	private void exportGedcom() {
		// TODO: implementare esportazione GEDCOM
		System.out.println("Export not yet implemented");
	}

	// Metodo per ottenere lo store (usato dai pannelli)
	public Map<String, TreeMap<Integer, Map<String, Object>>> getStore() {
		return store;
	}

	// Metodo per aggiornare la lista dopo un salvataggio
	public void refreshEntityList(final String entityType) {
		// Ricarica la lista
		loadEntityList(entityType);
		// Se c'è un'entità selezionata, ricarica il dettaglio
		if (currentEntityId != null && entityType.equals(currentEntityType)) {
			loadEntityDetail(entityType, currentEntityId);
		}
	}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> {
			final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();
			// Inizializza lo store con dati di esempio (per test)
			initTestStore(store);
			final MainFrame frame = new MainFrame(store);
			frame.setVisible(true);
		});
	}

	private static void initTestStore(final Map<String, TreeMap<Integer, Map<String, Object>>> store) {
		// Place
		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("name", "Rome");
		place1.put("address", "Piazza Venezia");
		places.put(1, place1);
		store.put(EntityManager.TABLE_NAME_PLACE, places);

		// Individual (semplice)
		final TreeMap<Integer, Map<String, Object>> individuals = new TreeMap<>();
		final Map<String, Object> ind1 = new HashMap<>();
		ind1.put("id", 1);
		ind1.put("sex", "MALE");
		// Per semplicità, i nomi sono in una tabella separata, ma per test li mettiamo qui
		ind1.put("personal_name", "Mario");
		ind1.put("family_name", "Rossi");
		individuals.put(1, ind1);
		store.put(EntityManager.TABLE_NAME_INDIVIDUAL, individuals);
	}
}
