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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.CulturalNormListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing an {@code EVENT_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record EventRecord {
 *   id: LocalID
 *   type: enum {
 *     birth, death, adoption, graduation, immigration, naturalization, bankruptcy,
 *     guardianship, coroner_report, cremation, burial, education, retirement,
 *     military_induction, military_muster_roll, military_service, military_award,
 *     military_release, military_discharge, military_resignation, military_retirement,
 *     prison, pardon, jury_duty, illness, hospitalization, medical_procedure, honor,
 *     deportation, internment, liberation, emancipation, relocation, emigration,
 *     census, deed, escrow, chancery, will, probate,
 *     engagement, marriage_bann, marriage_contract, marriage_license, marriage_settlement,
 *     marriage, divorce_filed, divorce_decree, divorce, annulment
 *   } | Text
 *   detail: EventStructure
 * }
 * </pre>
 */
public class EventRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -9191829528682252778L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_DESCRIPTION = "DESCRIPTION";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_AGENCY = "AGENCY";
	private static final String TAG_CAUSE = "CAUSE";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new EventHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]10[]10[]10[]"));

	private final BoundComboBox<String> typeCombo;
	private final BoundTextArea descriptionArea;
	private final DateField dateField;
	private final PlaceCitationField placeCitationField;
	private final BoundTextField agencyField;
// *   cause?: struct {
// *     value: Text
//	*     evidence?: EvidenceQualifiers
//	*   }
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final EvidenceQualifiersPanel qualifiers;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static EventRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new EventRecordDialog(parent, model, null);
	}

	public static EventRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new EventRecordDialog(parent, model, record);
	}


	private EventRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(EventHandler.TYPE));

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			"birth", "death", "adoption", "graduation", "immigration", "naturalization", "bankruptcy", "guardianship",
			"coroner_report", "cremation", "burial", "education", "retirement", "military_induction",
			"military_muster_roll", "military_service", "military_award", "military_release", "military_discharge",
			"military_resignation", "military_retirement", "prison", "pardon", "jury_duty", "illness", "hospitalization",
			"medical_procedure", "honor", "deportation", "internment", "liberation", "emancipation", "relocation",
			"emigration", "census", "deed", "escrow", "chancery", "will", "probate", "engagement", "marriage_bann",
			"marriage_contract", "marriage_license", "marriage_settlement", "marriage", "divorce_filed", "divorce_decree",
			"divorce", "annulment"
		});
		typeCombo.setEditable(true);
		descriptionArea = new BoundTextArea(TAG_DESCRIPTION, 3, 25);
		dateField = DateField.createWithWrapperTag(TAG_DATE, parent, "Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, parent, model);
		agencyField = new BoundTextField(TAG_AGENCY, 30);
		//TODO cause
		culturalNormPanel = new CulturalNormListPanel(TAG_CULTURAL_NORM, parent, model);
		notePanel = new NoteListPanel(TAG_NOTE, parent, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, parent, model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(descriptionArea);
		bindingManager.bind(agencyField);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// type
		mainPanel.add(new JLabel("Type:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// description
		mainPanel.add(new JLabel("Description:"), "align label");
		mainPanel.add(descriptionArea, "growx,wrap");

		// date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// place
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeCitationField, "growx,wrap");

		// agency
		mainPanel.add(new JLabel("Agency:"), "align label");
		mainPanel.add(agencyField, "growx,wrap");

		// cause
		mainPanel.add(new JLabel("Cause:"), "align label");
		//TODO
		mainPanel.add(new JTextField(), "growx,wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]"));
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		dateField.load(record);
		placeCitationField.load(record);
		//TODO cause
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourceCitationPanel.load(record);
		qualifiers.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
//		if(typeCombo.isEmpty()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Note value is required.",
//				tabbedPane, mainPanel, valueArea);
//
//			return false;
//		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		dateField.save(record);
		placeCitationField.save(record);
		//TODO cause
		culturalNormPanel.save(record);
		notePanel.saveReferences(record);
		sourceCitationPanel.save(record);
		qualifiers.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final EventRecordDialog dialog = EventRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
