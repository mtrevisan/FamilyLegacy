/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordChangeSummary;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.ImageCarouselPanel;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.features.ContextualSuggestions;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.undo.ComboBoxUndoSelectionListener;
import io.github.mtrevisan.familylegacy.ui.undo.EditableComboAdapter;
import io.github.mtrevisan.familylegacy.ui.undo.UndoController;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Abstract base class for all record editing dialogs.
 * Provides common functionality and utility methods.
 */
public abstract class BaseRecordDialog extends JDialog implements RecordDialog{

	private static final int DIALOG_MARGIN = GUIHelper.DIALOG_MARGIN;

	static final String PANEL_PROPERTY_NAME = "name";
	/** The records that belong to the research workflow and to the historical context: their editors show every tab at every level. */
	private static final Set<String> ADVANCED_RECORD_TYPES = Set.of(ConclusionHandler.TYPE, IdentityHypothesisHandler.TYPE,
		ResearchQuestionHandler.TYPE, ResearchActivityHandler.TYPE, ResearchTaskHandler.TYPE, CulturalNormHandler.TYPE,
		HistoricEventHandler.TYPE, ContextImpactHandler.TYPE);

	public static final String TAB_NAME_PROPERTIES = "properties";
	public static final String TAB_NAME_CAUSE = "cause";
	public static final String TAB_NAME_LOCATION = "location";
	public static final String TAB_NAME_ATTRIBUTES = "attributes";
	public static final String TAB_NAME_RELATIONSHIPS = "relationships";
	public static final String TAB_NAME_PARTICIPATIONS = "participations";
	public static final String TAB_NAME_CONTEXT = "context";
	public static final String TAB_NAME_RESEARCH = "research";
	public static final String TAB_NAME_FINDINGS = "findings";
	public static final String TAB_NAME_REFERENCES = "references";
	public static final String TAB_NAME_SOURCES = "sources";
	public static final String TAB_NAME_NOTES = "notes";
	public static final String TAB_NAME_PRIVACY = "privacy";
	public static final String TAB_NAME_AUDIT = "audit";
	public static final String TAB_NAME_SUBMITTER = "submitter";

	private static final String ACTION_GLOBAL_UNDO = "globalUndo";
	private static final String ACTION_GLOBAL_REDO = "globalRedo";


	protected final RecordTypeHandler<?> handler;
	protected final FLEFModel model;
	protected final FLEFRecord record;
	protected final boolean isNew;
	protected boolean isSaved;

	private final UndoController undoController = new UndoController();

	protected BoundTextField parentEntity;

	protected RecordDialogComponents components;

	protected final JTabbedPane tabbedPane = new JTabbedPane();

	protected final ImageCarouselPanel imageCarouselPanel = new ImageCarouselPanel();
	/** The quiet strip where the advanced concepts that could help with the record are suggested (only in the editors with tabs). */
	private SuggestionStrip suggestionStrip;


	protected BaseRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record,
			final RecordTypeHandler<?> handler){
		super(parent, ModalityType.APPLICATION_MODAL);

		this.handler = handler;
		this.model = model;
		this.record = (record != null? record: createNewRecord());
		this.isNew = (record == null);

		setTitle(buildTitle(this.handler, (record == null)));
	}


	protected void finalizeDialog(final Window parent){
		initComponents();
		installUsageTooltip();

		loadData();

		// Clear any edits generated during loadData() before binding listeners
		undoController.discardAllEdits();
		registerUndoSupportRecursively(getContentPane());

		setupKeyboardShortcuts();

		pack();

		revalidate();
		repaint();

		setLocationRelativeTo(parent);

		installSuggestions();
	}

	/**
	 * Asks, away from the event dispatch thread, which advanced concepts could help with the record, and shows them in the strip of the
	 * editor when they are known. Nothing is asked for a new record (the assistant knows only what is saved), and nothing is shown if
	 * there is nothing to suggest: the editor is not interrupted.
	 */
	private void installSuggestions(){
		if(suggestionStrip == null || isNew || record.getId() == null)
			return;

		ContextualSuggestions.find()
			.ifPresent(provider -> provider.suggest(this, model, record.getId(), this::showSuggestions));
	}

	private void showSuggestions(final List<Suggestion> suggestions){
		//the user may have closed the editor meanwhile
		if(!isDisplayable())
			return;

		suggestionStrip.show(suggestions);
		//the editor grows by what the strip needs, instead of the strip taking room from the fields
		setSize(getWidth(), getHeight() + suggestionStrip.getPreferredSize().height);
		validate();
	}

	/** Explains, on the first tab, when the record is to be used (if the translations define it). */
	private void installUsageTooltip(){
		final String html = GUIHelper.usageTooltip(handler.getType());
		if(html == null)
			return;

		if(tabbedPane.getTabCount() > 0)
			tabbedPane.setToolTipTextAt(0, html);
		else if(getContentPane() instanceof JComponent content)
			content.setToolTipText(html);
	}

	protected void initComponents(){
		final JPanel propertiesPanel = createPropertiesPanel();
		final JPanel causePanel = createCausePanel();
		final JPanel mapPanel = createLocationPanel();
		final JPanel attributesPanel = createAttributesPanel();
		final JPanel relationshipsPanel = createRelationshipsPanel();
		final JPanel participationsPanel = createParticipationsPanel();
		//the tabs of the research workflow and of the historical context are offered from the standard and advanced levels (the panels are
		//built all the same: only the tab is not shown), except in the editors of those very records
		final JPanel contextPanel = visibleAt(handler.getType(), UxLevel.ADVANCED, createContextPanel(), FeatureVisibilityService.shared());
		final JPanel researchPanel = visibleAt(handler.getType(), UxLevel.STANDARD, createResearchPanel(), FeatureVisibilityService.shared());
		final JPanel findingsPanel = createFindingsPanel();
		final JPanel referencesPanel = createReferencesPanel();
		final JPanel sourcesPanel = createSourcesPanel();
		final JPanel notesPanel = createNotesPanel();
		final JPanel privacyPanel = createPrivacyPanel();
		final JPanel auditPanel = createAuditPanel();

		if(attributesPanel == null && relationshipsPanel == null && participationsPanel == null && contextPanel == null
				&& researchPanel == null && findingsPanel == null && referencesPanel == null && sourcesPanel == null
				&& notesPanel == null && privacyPanel == null && auditPanel == null){
			// No optional panels, just show the properties panel (with the same margin the tabbed dialogs have: no content touches the edge)
			setLayout(GUIHelper.createLabelFieldLayout(DIALOG_MARGIN, "[]"));
			GUIHelper.addComponent(this, propertiesPanel);
		}
		else{
			addTabIfNotNull(tabbedPane, TAB_NAME_PROPERTIES, propertiesPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_CAUSE, causePanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_LOCATION, mapPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_ATTRIBUTES, attributesPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_RELATIONSHIPS, relationshipsPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_PARTICIPATIONS, participationsPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_CONTEXT, contextPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_RESEARCH, researchPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_FINDINGS, findingsPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_REFERENCES, referencesPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_SOURCES, sourcesPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_NOTES, notesPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_PRIVACY, privacyPanel);
			addTabIfNotNull(tabbedPane, TAB_NAME_AUDIT, auditPanel);
		}

		finalizeLayout(tabbedPane);
	}

	/**
	 * @param recordType the type of the record the editor is about
	 * @param minimum    the lowest level the tab is shown at
	 * @param panel      the panel of the tab ({@code null} if the editor has none)
	 * @param features   what tells the level of the user
	 * @return the panel if its tab is shown, otherwise {@code null}: the record of the research workflow and of the context are edited
	 * 	with all their tabs at every level (the user got there on purpose), the other records show those tabs only from {@code minimum}
	 */
	static JPanel visibleAt(final String recordType, final UxLevel minimum, final JPanel panel, final FeatureVisibilityService features){
		return (panel == null || ADVANCED_RECORD_TYPES.contains(recordType) || features.isVisible(minimum)? panel: null);
	}

	protected void setupKeyboardShortcuts(){
		final JComponent rootPane = getRootPane();
		final InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(ShortcutRegistry.EDIT_UNDO.keyStroke(), ACTION_GLOBAL_UNDO);
		inputMap.put(ShortcutRegistry.EDIT_REDO.keyStroke(), ACTION_GLOBAL_REDO);

		final ActionMap actionMap = rootPane.getActionMap();
		actionMap.put(ACTION_GLOBAL_UNDO, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				performUndo();
			}
		});
		actionMap.put(ACTION_GLOBAL_REDO, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				performRedo();
			}
		});
	}

	protected void registerUndoSupport(final JTextComponent textComponent){
		if(textComponent == null)
			return;

		//record only what the user types: programmatic `setText` calls (loading data, placeholders "(right-click to set)", refresh
		//after a nested dialog) would otherwise pollute the stack, requiring several undos or altering untouched components
		textComponent.getDocument()
			.addUndoableEditListener(e -> {
				if(textComponent.isEditable() && textComponent.hasFocus())
					undoController.addEdit(e.getEdit());
			});

		// Remove component-level shortcuts so they route through the dialog's RootPane
		final InputMap inputMap = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(ShortcutRegistry.EDIT_UNDO.keyStroke(), "none");
		inputMap.put(ShortcutRegistry.EDIT_REDO.keyStroke(), "none");
	}

	protected void registerUndoSupport(final JComboBox<?> comboBox){
		if(comboBox == null)
			return;

		if(comboBox.isEditable()){
			final Component editorComp = comboBox.getEditor().getEditorComponent();
			if(editorComp instanceof JTextComponent textComp){
				// Remove local text component shortcuts
				final InputMap compInputMap = textComp.getInputMap(JComponent.WHEN_FOCUSED);
				compInputMap.put(ShortcutRegistry.EDIT_UNDO.keyStroke(), "none");
				compInputMap.put(ShortcutRegistry.EDIT_REDO.keyStroke(), "none");

				final EditableComboAdapter adapter = new EditableComboAdapter(comboBox, textComp, undoController);
				textComp.addFocusListener(adapter);
				textComp.getDocument()
					.addDocumentListener(adapter);
				comboBox.addActionListener(adapter);
			}
		}
		else{
			final ComboBoxUndoSelectionListener listener = new ComboBoxUndoSelectionListener(comboBox, undoController);
			comboBox.addActionListener(listener);
		}
	}

	protected void registerUndoSupportRecursively(final Container container){
		if(container == null)
			return;

		for(final Component component : container.getComponents()){
			if(component instanceof JComboBox<?> comboBox)
				registerUndoSupport(comboBox);
			else if(component instanceof JTextComponent textComponent)
				registerUndoSupport(textComponent);
			else if(component instanceof Container subContainer)
				registerUndoSupportRecursively(subContainer);
		}
	}

	/**
	 * Records a change made in place to a record by a nested dialog, so that it can be undone from this one.
	 *
	 * @param target	The record that was modified.
	 * @param before	A copy of the record before the change.
	 * @param after	A copy of the record after the change.
	 * @param refresh	Refreshes the components showing the record.
	 */
	@Override
	public void addNestedEdit(final FLEFRecord target, final FLEFRecord before, final FLEFRecord after, final Runnable refresh){
		undoController.addEdit(new AbstractUndoableEdit(){
			@Override
			public void undo() throws CannotUndoException{
				super.undo();

				restore(before);
			}

			@Override
			public void redo() throws CannotRedoException{
				super.redo();

				restore(after);
			}

			private void restore(final FLEFRecord state){
				target.clear();
				state.deepCopyTo(target);

				refresh.run();
			}
		});
	}

	public void performUndo(){
		if(undoController.canUndo())
			undoController.undo();
	}

	public void performRedo(){
		if(undoController.canRedo())
			undoController.redo();
	}

	/**
	 * Adds a tab to the tabbed pane only if the panel is not {@code null}.
	 *
	 * @param tabbedPane the tabbed pane
	 * @param tabName    the tab name
	 * @param panel      the panel to add (maybe {@code null})
	 */
	void addTabIfNotNull(final JTabbedPane tabbedPane, final String tabName, final JPanel panel){
		if(panel != null){
			panel.putClientProperty(PANEL_PROPERTY_NAME, tabName);

			tabbedPane.addTab(I18N.t("tab.title." + tabName), panel);
		}
	}

	public boolean showSelectedTab(final String tabName){
		if(tabName != null && tabbedPane != null)
			for(int i = 0, count = tabbedPane.getTabCount(); i < count; i ++)
				if(tabbedPane.getComponentAt(i) instanceof JPanel panel
						&& tabName.equals(panel.getClientProperty(PANEL_PROPERTY_NAME))){
					tabbedPane.setSelectedIndex(i);

					return true;
				}
		return false;
	}

	@Override
	public void showRelationshipsAsObject(){
		showSelectedTabAndComponent(TAB_NAME_RELATIONSHIPS, PanelKey.RELATIONSHIP_ON_OBJECT);
	}

	public boolean showSelectedTabAndComponent(final String tabName, final PanelKey componentKey){
		if(tabName != null && tabbedPane != null)
			for(int i = 0, count = tabbedPane.getTabCount(); i < count; i ++)
				if(tabbedPane.getComponentAt(i) instanceof JPanel panel
						&& tabName.equals(panel.getClientProperty(PANEL_PROPERTY_NAME))){
					tabbedPane.setSelectedIndex(i);

					if(componentKey != null && panel.getComponentCount() > 0){
						final JPanel jpanel = components.getPanel(componentKey);
						if(jpanel != null)
							jpanel.requestFocusInWindow();
					}

					return true;
				}
		return false;
	}

	protected JPanel createPropertiesPanel(){
		return null;
	}

	protected JPanel createCausePanel(){
		return null;
	}

	protected JPanel createLocationPanel(){
		return null;
	}

	protected JPanel createAttributesPanel(){
		return null;
	}

	protected JPanel createRelationshipsPanel(){
		return null;
	}

	protected JPanel createParticipationsPanel(){
		return null;
	}

	protected JPanel createContextPanel(){
		return null;
	}

	protected JPanel createResearchPanel(){
		return null;
	}

	protected JPanel createFindingsPanel(){
		return null;
	}

	protected JPanel createReferencesPanel(){
		return null;
	}

	protected JPanel createSourcesPanel(){
		return null;
	}

	protected JPanel createNotesPanel(){
		return null;
	}

	protected JPanel createPrivacyPanel(){
		return null;
	}

	protected JPanel createAuditPanel(){
		return null;
	}

	private String buildTitle(final RecordTypeHandler<?> handler, final boolean isNew){
		final String dialogType = I18N.t(isNew? "dialog.title.new": "dialog.title.edit");
		final String label = handler.getLabel();
		final StringBuilder sb = new StringBuilder(dialogType)
			.append(StringUtils.SPACE)
			.append(label);

		String id = record.getId();
		if(id == null)
			// in case of a citation
			id = FLEFRecordHelper.getChildValue(record, handler.getCitedType());

		if(StringUtils.isNotEmpty(id))
			sb.append(StringUtils.SPACE)
				.append('[')
				.append(id)
				.append(']');
		return sb.toString();
	}


	/**
	 * Collapses the {@code createNew(Dialog, FLEFModel)} boilerplate that used to be duplicated, near-verbatim, in every
	 * subclass. Call it from the subclass's own {@code createNew}, passing its constructor as a method reference:
	 * <pre>
	 * public static NoteRecordDialog createNew(final Window parent, final FLEFModel model){
	 *     return createNew(parent, model, NoteRecordDialog::new);
	 * }
	 * </pre>
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param factory	Reference to the subclass's private constructor (e.g. {@code NoteRecordDialog::new}).
	 * @return	A new dialog instance, for a new record.
	 */
	protected static <T extends BaseRecordDialog> T createNew(final Window parent, final FLEFModel model,
			final DialogFactory<T> factory){
		return factory.create(parent, model, null);
	}

	/**
	 * Collapses the {@code createEdit(Dialog, FLEFModel, FLEFRecord)} boilerplate (including the "record cannot be null"
	 * guard) that used to be duplicated, near-verbatim, in every subclass. Call it from the subclass's own
	 * {@code createEdit}, passing its constructor as a method reference:
	 * <pre>
	 * public static NoteRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
	 *     return createEdit(parent, model, record, NoteRecordDialog::new);
	 * }
	 * </pre>
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @param factory	Reference to the subclass's private constructor (e.g. {@code NoteRecordDialog::new}).
	 * @return	A new dialog instance, for editing an existing record.
	 */
	protected static <T extends BaseRecordDialog> T createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record, final DialogFactory<T> factory){
		return factory.create(parent, model, record);
	}


	/**
	 * Wires up the standard dialog chrome that used to be repeated at the end of every {@code initComponents()}:
	 * the frame's layout, the tabbed pane, and the Save button row (ESC cancels). Call it last, from the subclass's own
	 * {@code initComponents()}, after all tabs have been added:
	 * <pre>
	 * tabbedPane.addTab("Main", createMainPanel());
	 * tabbedPane.addTab("Audit", auditPanel);
	 *
	 * finalizeLayout(tabbedPane);
	 * </pre>
	 *
	 * @param tabbedPane	The dialog's fully populated tabbed pane.
	 */
	protected void finalizeLayout(final JTabbedPane tabbedPane){
		if(tabbedPane.getTabCount() > 0){
			setLayout(new MigLayout("ins " + DIALOG_MARGIN + ",fillx,top"));

			add(tabbedPane, "growx");

			suggestionStrip = new SuggestionStrip();
			add(suggestionStrip, "newline,growx,hidemode 3");
		}

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);

		//the reserved ID is released on any close without save (Cancel, ESC, window X, programmatic dispose), once
		addWindowListener(new WindowAdapter(){
			private boolean released;

			@Override
			public void windowClosed(final WindowEvent e){
				if(!released && isNew && !isSaved){
					released = true;
					FLEFRecord.releaseReservedId(handler.getIdPrefix());
				}
			}
		});
	}


	/**
	 * Sets up a listener on the source list to update the image carousel when a source is selected.
	 */
	protected void setupSourceListSelection(){
		// Find the JList inside the source panel (assume it's an EntityListPanel or a panel with a list)
		final EntityListPanel sourcePanel = (EntityListPanel)components.getPanel(PanelKey.SOURCE);
		if(sourcePanel == null)
			return;

		final JList<?> sourceList = sourcePanel.getList();
		final ListSelectionListener sourceSelectionListener = e -> {
			if(!e.getValueIsAdjusting())
				updateCarouselFromSelectedSource();
		};
		sourceList.addListSelectionListener(sourceSelectionListener);
	}

	/**
	 * Updates the image carousel based on the currently selected source citation.
	 * Extracts URI from the selected source record's structure.
	 */
	protected void updateCarouselFromSelectedSource(){
		final EntityListPanel sourcePanel = (EntityListPanel)components.getPanel(PanelKey.SOURCE);
		if(sourcePanel == null)
			return;

		final FLEFRecord selected = sourcePanel.getSelectedItem();
		List<String> uris = null;
		if(selected != null)
			uris = extractUrisFromSource(selected);
		imageCarouselPanel.setImageUris(uris);

		imageCarouselPanel.revalidate();
		imageCarouselPanel.repaint();
	}

	/**
	 * Extracts image URIs from a source citation record.
	 * Assumes the source record has children with tag "URI" or references a media record.
	 */
	private List<String> extractUrisFromSource(final FLEFRecord sourceCitation){
		final List<String> uris = new ArrayList<>();

		final String sourceId = FLEFRecordHelper.getChildValue(sourceCitation, SourceHandler.TYPE);
		final FLEFRecord source = model.getRecordById(sourceId);
		final List<FLEFRecord> documents = FLEFRecordHelper.findChildren(source, SourceReader.TAG_MEDIA);
		for(final FLEFRecord documentRef : documents){
			final String documentId = documentRef.getValue();

			final FLEFRecord doc = model.getRecordById(documentId);
			final String uri = FLEFRecordHelper.getChildValue(doc, MediaReader.TAG_URI);
			if(StringUtils.isNotEmpty(uri))
				uris.add(uri);
		}

		return uris;
	}


	public BaseRecordDialog withParentEntity(final FLEFRecord parent){
		parentEntity = new BoundTextField(parent.getTag());
		parentEntity.setText(parent.getId());

		return this;
	}

	protected abstract void loadData();

	/**
	 * Public save method that performs validation and then saves.
	 * Called by the Save button.
	 */
	public final void save(){
		if(validData()){
			//a new record has no previous version to compare with: its creation is already recorded by the audit
			final FLEFRecord previous = (!isNew && handler.isTopLevelEntity()? copyOf(record): null);

			saveData();

			if(previous != null)
				recordChanges(previous);

			if(isNew && handler.isTopLevelEntity())
				model.addRecord(record);
			isSaved = true;
			//an edit may change the privacy of the records
			PrivacyService.invalidate();

			dispose();
		}
	}

	private static FLEFRecord copyOf(final FLEFRecord source){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		source.deepCopyTo(copy);
		return copy;
	}

	/**
	 * Adds an update to the audit of the record, whose comment tells what was changed, added or removed.
	 * <p>
	 * Nothing is added if nothing changed, or if the record has no audit.
	 * </p>
	 *
	 * @param previous	The record as it was before the save.
	 */
	private void recordChanges(final FLEFRecord previous){
		final FLEFRecord audit = FLEFRecordHelper.findChild(record, AuditReader.TAG_AUDIT);
		if(audit == null)
			return;

		final List<String> changes = RecordChangeSummary.describe(previous, record);
		if(changes.isEmpty())
			return;

		final String date = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		audit.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_UPDATE)
			.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, date))
			.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, String.join("\n", changes))));
	}

	/**
	 * Validates the data before saving.
	 * Subclasses must implement this method to check required fields.
	 *
	 * @return	Whether the data is valid.
	 */
	protected boolean validData(){
		return true;
	}

	/**
	 * Saves the record data to the model.
	 * Subclasses must call validateData() at the beginning of this method.
	 */
	protected abstract void saveData();

	private FLEFRecord createNewRecord(){
		if(!handler.isTopLevelEntity())
			return FLEFRecord.createEmpty();

		return FLEFRecord.createMainRecord(handler.getType(), handler.getIdPrefix(), model);
	}


	protected String getChildValue(final String tag){
		return FLEFRecordHelper.getChildValue(record, tag);
	}

	protected FLEFRecord findChild(final String tag){
		return FLEFRecordHelper.findChild(record, tag);
	}

	protected List<FLEFRecord> findChildren(final String tag){
		return FLEFRecordHelper.findChildren(record, tag);
	}

	protected void updateChildValue(final String tag, final String value){
		FLEFRecordHelper.updateChildValue(record, tag, value);
	}

	protected void addChild(final String tag, final String value){
		FLEFRecordHelper.addChildValue(record, tag, value);
	}


	@Override
	public boolean isSaved(){
		return isSaved;
	}

	@Override
	public FLEFRecord getRecord(){
		return (!record.isEmpty()? record: null);
	}

	@Override
	public Window window(){
		return this;
	}


	protected boolean confirmRecordExistsForType(final String participantId, final String participantLabel){
		if(model.getRecordById(participantId) == null){
			JOptionPane.showMessageDialog(this,
				I18N.tf("message.unknown_participant_id", participantLabel),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

}
