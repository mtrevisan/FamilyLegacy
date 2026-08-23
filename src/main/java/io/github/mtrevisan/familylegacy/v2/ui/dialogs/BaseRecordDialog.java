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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Abstract base class for all record editing dialogs.
 * Provides common functionality and utility methods.
 */
public abstract class BaseRecordDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 6460878052412992481L;


	protected final RecordTypeHandler<?> handler;
	protected final FLEFModel model;
	protected final FLEFRecord record;
	protected final boolean isNew;
	protected boolean isSaved;

	protected BoundTextField parentEntity;

	protected final JTabbedPane tabbedPane = new JTabbedPane();


	@Deprecated
	protected BaseRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record,
			final RecordTypeHandler<?> handler){
		super(parent, ModalityType.APPLICATION_MODAL);

		this.handler = handler;
		this.model = model;
		this.record = (record != null? record: createNewRecord());
		this.isNew = (record == null);

		setTitle(buildTitle(handler, (record == null)));
	}

	protected <T extends Class<? extends RecordTypeHandler<?>>> BaseRecordDialog(final Dialog parent,
			final FLEFModel model, final FLEFRecord record, final T handler){
		super(parent, ModalityType.APPLICATION_MODAL);

		this.handler = HandlerRegistry.getHandler(handler);
		this.model = model;
		this.record = (record != null? record: createNewRecord());
		this.isNew = (record == null);

		setTitle(buildTitle(this.handler, (record == null)));
	}

	protected void finalizeDialog(final Dialog parent){
		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		final JPanel propertiesPanel = createPropertiesPanel();
		final JPanel attributesPanel = createAttributesPanel();
		final JPanel relationshipsPanel = createRelationshipsPanel();
		final JPanel participationsPanel = createParticipationsPanel();
		final JPanel contextPanel = createContextPanel();
		final JPanel researchPanel = createResearchPanel();
		final JPanel findingsPanel = createFindingsPanel();
		final JPanel referencesPanel = createReferencesPanel();
		final JPanel sourcesPanel = createSourcesPanel();
		final JPanel notesPanel = createNotesPanel();
		final JPanel privacyPanel = createPrivacyPanel();
		final JPanel auditPanel = createAuditPanel();

		if(attributesPanel == null &&  relationshipsPanel == null && participationsPanel == null && contextPanel == null
				&& researchPanel == null && findingsPanel == null && referencesPanel == null && sourcesPanel == null
				&& notesPanel == null && privacyPanel == null &&  auditPanel == null){
			// No optional panels, just show the properties panel
			setLayout(GUIHelper.createLabelFieldLayout(0, "[]"));
			GUIHelper.addComponent(this, propertiesPanel);
		}
		else{
			addTabIfNotNull(tabbedPane, "Properties", propertiesPanel);
			addTabIfNotNull(tabbedPane, "Attributes", attributesPanel);
			addTabIfNotNull(tabbedPane, "Relationships", relationshipsPanel);
			addTabIfNotNull(tabbedPane, "Participations", participationsPanel);
			addTabIfNotNull(tabbedPane, "Context", contextPanel);
			addTabIfNotNull(tabbedPane, "Research", researchPanel);
			addTabIfNotNull(tabbedPane, "Findings", findingsPanel);
			addTabIfNotNull(tabbedPane, "References", referencesPanel);
			addTabIfNotNull(tabbedPane, "Sources", sourcesPanel);
			addTabIfNotNull(tabbedPane, "Notes", notesPanel);
			addTabIfNotNull(tabbedPane, "Privacy", privacyPanel);
			addTabIfNotNull(tabbedPane, "Audit", auditPanel);
		}

		finalizeLayout(tabbedPane);
	}

	/**
	 * Adds a tab to the tabbed pane only if the panel is not {@code null}.
	 *
	 * @param tabbedPane the tabbed pane
	 * @param title      the tab title
	 * @param panel      the panel to add (may be {@code null})
	 */
	private void addTabIfNotNull(final JTabbedPane tabbedPane, final String title, final JPanel panel){
		if(panel != null)
			tabbedPane.addTab(title, panel);
	}

	protected JPanel createPropertiesPanel(){
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
		final String dialogType = (isNew? "New": "Edit");
		final String label = handler.getLabel();
		final StringBuilder sb = new StringBuilder(dialogType)
			.append(StringUtils.SPACE)
			.append(label);

		String id = record.getId();
		if(id == null)
			// in case of a citation
			id = FLEFRecordHelper.getChildValue(record, handler.getCitedType());

		if(id != null)
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
	 * public static NoteRecordDialog createNew(final Dialog parent, final FLEFModel model){
	 *     return createNew(parent, model, NoteRecordDialog::new);
	 * }
	 * </pre>
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param factory	Reference to the subclass's private constructor (e.g. {@code NoteRecordDialog::new}).
	 * @return	A new dialog instance, for a new record.
	 */
	protected static <T extends BaseRecordDialog> T createNew(final Dialog parent, final FLEFModel model,
			final DialogFactory<T> factory){
		return factory.create(parent, model, null);
	}

	/**
	 * Collapses the {@code createEdit(Dialog, FLEFModel, FLEFRecord)} boilerplate (including the "record cannot be null"
	 * guard) that used to be duplicated, near-verbatim, in every subclass. Call it from the subclass's own
	 * {@code createEdit}, passing its constructor as a method reference:
	 * <pre>
	 * public static NoteRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
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
	protected static <T extends BaseRecordDialog> T createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record, final DialogFactory<T> factory){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return factory.create(parent, model, record);
	}


	/**
	 * Wires up the standard dialog chrome that used to be repeated at the end of every {@code initComponents()}:
	 * the frame's layout, the tabbed pane, and the Save/Cancel button row. Call it last, from the subclass's own
	 * {@code initComponents()}, after all tabs have been added:
	 * <pre>
	 * tabbedPane.addTab("Main", createMainPanel());
	 * tabbedPane.addTab("Audit", auditPanel);
	 *
	 * finalizeLayout(tabbedPane);
	 * </pre>
	 *
	 * @param tabbedPane	The dialog's fully-populated tabbed pane.
	 */
	protected void finalizeLayout(final JTabbedPane tabbedPane){
		setLayout(new MigLayout("ins 10,fillx,top"));

		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createSaveCancelButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	public BaseRecordDialog withParentEntity(final String parentEntityId, final String path){
		parentEntity = new BoundTextField(path);
		parentEntity.setText(parentEntityId);

		return this;
	}

	protected abstract void loadData();

	/**
	 * Public save method that performs validation and then saves.
	 * Called by the Save button.
	 */
	public final void save(){
		if(validData()){
			record.clear();

			saveData();

			if(isNew && handler.isTopLevelEntity())
				model.addRecord(record);
			isSaved = true;

			dispose();
		}
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

		return FLEFRecord.createMainRecord(generateNewId(), handler.getType());
	}

	private String generateNewId(){
		return XRefHelper.generateNewId(model, handler.getType(), handler.getIdPrefix());
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


	protected void removeChildren(final String tag){
		FLEFRecordHelper.removeChildren(record, tag);
	}


	protected void showError(final String title, final String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	protected void showInfo(final String title, final String message){
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	protected boolean showConfirm(final String title, final String message){
		final int selectedOption = JOptionPane.showConfirmDialog(this, message, title,
			JOptionPane.YES_NO_OPTION);
		return (selectedOption == JOptionPane.YES_OPTION);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public FLEFRecord getRecord(){
		return (!record.isEmpty()? record: null);
	}


	protected boolean confirmRecordExistsForType(final String participantId,
			final Class<? extends RecordTypeHandler<?>> participantHandlerClass){
		if(model.getRecordById(participantId) == null){
			final RecordTypeHandler<?> participantHandler = HandlerRegistry.getHandler(participantHandlerClass);
			JOptionPane.showMessageDialog(this, "Unknown " + participantHandler.getLabel() + " ID.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

}
