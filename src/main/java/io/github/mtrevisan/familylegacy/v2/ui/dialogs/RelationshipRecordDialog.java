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
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.Collections;
import java.util.List;


/* ONGOING */
/**
 * Dialog for editing a {@code RELATIONSHIP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record RelationshipRecord {
 *   id: LocalID
 *   subject: EntityParticipant
 *   object: EntityParticipant
 *   type: enum { biological_child, adoptive_child, foster_child, guarded_child, step_child, civil_spouse, religious_spouse, customary_spouse, cohabiting_partner, engaged_partner, group_member, associate } | Text
 *   role?: Text
 *   status?: enum { active, ended, unknown }
 *   valid_on?: DateStructure
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class RelationshipRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6390551689993360839L;


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_VALID_ON = "VALID_ON";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	private static final List<String> INDIVIDUAL_TO_INDIVIDUAL_TYPES = List.of(
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child",
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	);
	private static final List<String> INDIVIDUAL_TO_GROUP_TYPES = List.of(
		"group_member", "associate"
	);
	private static final List<String> GROUP_TO_GROUP_TYPES = List.of(
		"associate"
	);
	private static final List<String> GROUP_TO_INDIVIDUAL_TYPES = Collections.emptyList();


	static{
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]10[]10[]5[]5[]10[]"));

	private final ParticipantField subjectField;
	private final ParticipantField objectField;
	private final BoundComboBox<String> subjectTypeCombo;
	private final BoundTextField subjectRoleField;
	private final BoundComboBox<String> statusCombo;
	private final DateField validOnField;
	private final DateField validFromField;
	private final DateField validToField;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel evidencePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static RelationshipRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new RelationshipRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static RelationshipRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new RelationshipRecordDialog(parent, model, record);
	}

	private RelationshipRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RelationshipHandler.TYPE));

		subjectField = ParticipantField.create(TAG_SUBJECT, this, model,
			List.of(IndividualHandler.TYPE, GroupHandler.TYPE));
		subjectField.addPropertyChangeListener(e -> {
			if(ParticipantField.PROPERTY_PARTICIPANT.equals(e.getPropertyName()))
				updateTypeCombo();
		});
		objectField = ParticipantField.create(TAG_OBJECT, this, model,
			List.of(IndividualHandler.TYPE, GroupHandler.TYPE));
		objectField.addPropertyChangeListener(e -> {
			if(ParticipantField.PROPERTY_PARTICIPANT.equals(e.getPropertyName()))
				updateTypeCombo();
		});
		subjectTypeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child",
			"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
			"group_member", "associate"
		});
		subjectTypeCombo.setEditable(true);
		subjectRoleField = new BoundTextField(TAG_ROLE, 20);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			StringUtils.EMPTY,
			"active", "ended", "unknown"
		});
		validOnField = DateField.createWithWrapperTag(TAG_VALID_ON, this, "Date", model);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);
		notePanel = new NoteListPanel(TAG_NOTE, parent, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, parent, model);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, parent);
		modificationPanel = new ModificationPanel(parent);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void updateTypeCombo(){
		final String subjectType = subjectField.getParticipantType();
		final String objectType = objectField.getParticipantType();
		final List<String> validTypes = getValidTypes(subjectType, objectType);

		final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(StringUtils.EMPTY);
		for(final String type : validTypes)
			model.addElement(type);
		subjectTypeCombo.setModel(model);

		final String currentSelection = (String)subjectTypeCombo.getSelectedItem();
		if(currentSelection == null || !validTypes.contains(currentSelection))
			subjectTypeCombo.clear();
	}

	private List<String> getValidTypes(final String subjectType, final String objectType){
		if(subjectType == null || objectType == null)
			return Collections.emptyList();

		if(IndividualHandler.TYPE.equalsIgnoreCase(subjectType) && IndividualHandler.TYPE.equalsIgnoreCase(objectType))
			return INDIVIDUAL_TO_INDIVIDUAL_TYPES;

		if(IndividualHandler.TYPE.equalsIgnoreCase(subjectType) && GroupHandler.TYPE.equalsIgnoreCase(objectType))
			return INDIVIDUAL_TO_GROUP_TYPES;

		if(GroupHandler.TYPE.equalsIgnoreCase(subjectType) && GroupHandler.TYPE.equalsIgnoreCase(objectType))
			return GROUP_TO_GROUP_TYPES;

		return GROUP_TO_INDIVIDUAL_TYPES;
	}

	private void initComponents(){
		bindingManager.bind(subjectTypeCombo);
		bindingManager.bind(subjectRoleField);
		bindingManager.bind(statusCombo);

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
		// subject
		mainPanel.add(new JLabel("Subject*:"), "align label");
		mainPanel.add(subjectField, "growx,wrap");

		// object
		mainPanel.add(new JLabel("Object*:"), "align label");
		mainPanel.add(objectField, "growx,wrap");

		// (subject) type
		mainPanel.add(new JLabel("Subject Type*:"), "align label");
		mainPanel.add(subjectTypeCombo, "growx,wrap");

		// (subject) role
		mainPanel.add(new JLabel("Subject Role:"), "align label");
		mainPanel.add(subjectRoleField, "growx,wrap");

		// status
		mainPanel.add(new JLabel("Status:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// valid on
		mainPanel.add(new JLabel("Valid On:"), "align label");
		mainPanel.add(validOnField, "growx,wrap");

		// valid from
		mainPanel.add(new JLabel("Valid From:"), "align label");
		mainPanel.add(validFromField, "growx,wrap");

		// valid to
		mainPanel.add(new JLabel("Valid To:"), "align label");
		mainPanel.add(validToField, "growx,wrap");

		// evidence
		mainPanel.add(evidencePanel, "span 2,growx,wrap");

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
		subjectField.load(record);
		objectField.load(record);

		bindingManager.load(record);

		validOnField.load(record);
		validFromField.load(record);
		validToField.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
		evidencePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);

		updateTypeCombo();
	}

	@Override
	protected boolean validData(){
		if(!subjectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject is required.",
				tabbedPane, mainPanel, subjectField);
			return false;
		}

		if(!objectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Object is required.",
				tabbedPane, mainPanel, objectField);
			return false;
		}

		String type = (String)subjectTypeCombo.getSelectedItem();
		if(StringUtils.isEmpty(type)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type is required.",
				tabbedPane, mainPanel, subjectTypeCombo);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.save(record);
		objectField.save(record);

		bindingManager.save(record);

		validOnField.save(record);
		validFromField.save(record);
		validToField.save(record);
		notePanel.saveReferences(record);
		sourcePanel.save(record);
		evidencePanel.save(record);
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
			final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
