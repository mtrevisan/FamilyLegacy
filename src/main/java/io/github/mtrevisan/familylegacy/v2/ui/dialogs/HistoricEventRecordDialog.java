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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Structure:
 * <pre>
 * record HistoricEventRecord {
 *   id: LocalID
 *   type?: enum { war, epidemic, famine, migration, legal_reform, political_change, territorial_change, natural_disaster, economic_crisis, scientific_discovery, religious_reform, social_movement, pandemic } | Text
 *   title?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 */
public class HistoricEventRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3544157573805016620L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";


	static{
		HandlerRegistry.register(new HistoricEventHandler());
		HandlerRegistry.register(new NoteHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField titleField;
	private final DateField dateField;
	private final PlaceField placeField;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel evidencePanel;
	private final ModificationPanel auditPanel;


	public static HistoricEventRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, HistoricEventRecordDialog::new);
	}

	public static HistoricEventRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, HistoricEventRecordDialog::new);
	}


	private HistoricEventRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(HistoricEventHandler.TYPE));

		setTitle(record == null? "Add Historic Event": "Edit Historic Event");

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"war", "epidemic", "famine", "migration", "legal_reform", "political_change", "territorial_change",
			"natural_disaster", "economic_crisis", "scientific_discovery", "religious_reform", "social_movement",
			"pandemic"
		});
		typeCombo.setEditable(true);
		titleField = new BoundTextField(TAG_TITLE);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Date", model);
		placeField = PlaceField.create(TAG_PLACE, this, model);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), HistoricEventHandler.TYPE);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		auditPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(titleField);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Audit", auditPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]10[]10[]"));

		// type
		mainPanel.add(new JLabel("Type:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// title
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// place
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeField, "growx,wrap");

		// evidence
		mainPanel.add(evidencePanel, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		dateField.load(record);
		placeField.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
		evidencePanel.load(record);
		auditPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		dateField.save(record);
		placeField.saveReferences(record);
		notePanel.save(record);
		sourcePanel.save(record);
		evidencePanel.save(record);
		auditPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(HistoricEventRecordDialog::createNew);
	}

}
