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
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;
import java.util.Collections;
import java.util.List;


/* DONE */
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
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 *
 * EntityParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 * }
 * </pre>
 */
public class RelationshipRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6390551689993360839L;


	protected enum ActorType{
		SUBJECT,
		OBJECT
	}


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_STATUS = "STATUS";
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
		HandlerRegistry.register(new NoteHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel();

	private final BindingManager bindingManager = new BindingManager();

	private ActorType actorType;

	private final JLabel subjectLabel;
	private final ParticipantField subjectField;
	private final BoundComboBox<String> subjectTypeCombo;
	private final ParticipantField objectField;
	private final BoundTextField subjectRoleField;
	private final BoundComboBox<String> statusCombo;
	private final DateField validFromField;
	private final DateField validToField;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel evidencePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static RelationshipRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, RelationshipRecordDialog::new);
	}

	public static RelationshipRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, RelationshipRecordDialog::new);
	}

	private RelationshipRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(RelationshipHandler.TYPE));

		subjectLabel = new JLabel("Subject*:");
		subjectField = ParticipantField.create(TAG_SUBJECT, this, model);
		subjectField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE));
		subjectField.addPropertyChangeListener(ParticipantField.PROPERTY_PARTICIPANT, e -> {
			updateTypeCombo();

			refreshLayout();
		});
		objectField = ParticipantField.create(TAG_OBJECT, this, model);
		objectField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE));
		objectField.addPropertyChangeListener(ParticipantField.PROPERTY_PARTICIPANT, e -> {
			updateTypeCombo();

			refreshLayout();
		});
		subjectTypeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child",
			"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
			"group_member", "associate"
		});
		subjectTypeCombo.setEditable(true);
		subjectRoleField = new BoundTextField(TAG_ROLE);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			StringUtils.EMPTY,
			"active", "ended", "unknown"
		});
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), RelationshipHandler.TYPE);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void updateTypeCombo(){
		final FLEFRecord subjectRecord = subjectField.getParticipantRecord();
		if(subjectRecord == null)
			return;

		final FLEFRecord objectRecord = objectField.getParticipantRecord();
		if(objectRecord == null)
			return;

		final String subjectType = subjectRecord.getTag();
		final String objectType = objectRecord.getTag();
		final List<String> validTypes = getValidTypes(subjectType, objectType);

		final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(StringUtils.EMPTY);
		for(final String type : validTypes)
			model.addElement(type);
		final String selectedItem = (String)subjectTypeCombo.getSelectedItem();
		final int selectedIndex = subjectTypeCombo.getSelectedIndex();
		subjectTypeCombo.setModel(model);
		if(selectedIndex < 0)
			subjectTypeCombo.setSelectedItem(selectedItem);
		else if(!validTypes.contains(selectedItem))
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


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
		mainPanel.setLayout(new MigLayout("ins 10,fillx,top", "[right]rel[grow]",
			(showAll
				? "[]5[]10[]5[]10[]10[]10[]"
				: "[]5[]10[]10[]10[]10[]")));

		if(showAll || actorType != ActorType.SUBJECT){
			// subject
			mainPanel.add(subjectLabel, "align label");
			mainPanel.add(subjectField, "growx,wrap");
		}

		// (subject) type
		mainPanel.add(new JLabel("Subject Type*:"), "align label");
		mainPanel.add(subjectTypeCombo, "growx,wrap");

		// (subject) role
		mainPanel.add(new JLabel("Subject Role:"), "align label");
		mainPanel.add(subjectRoleField, "growx,wrap");

		if(showAll || actorType != ActorType.OBJECT){
			// object
			mainPanel.add(new JLabel("Object*:"), "align label");
			mainPanel.add(objectField, "growx,wrap");
		}

		// status
		mainPanel.add(new JLabel("Status:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid from
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx,wrap");
		// valid to
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx");
		mainPanel.add(validityPanel, "span 2,growx,wrap");

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


	private void refreshLayout(){
		recreateMainPanel();

		mainPanel.revalidate();
		mainPanel.repaint();

		pack();
	}

	private void recreateMainPanel(){
		mainPanel.removeAll();
		createMainPanel();
	}

	@Override
	public BaseRecordDialog withParentEntity(final String parentEntityId, final String parentEntityHandlerType){
		JOptionPane.showMessageDialog(this, "Cannot set parent on Relationship Record Dialog.",
			"Error", JOptionPane.ERROR_MESSAGE);

		return this;
	}

	public BaseRecordDialog withSubject(final String parentEntityId, final String parentEntityHandlerType){
		super.withParentEntity(parentEntityId, parentEntityHandlerType);

		if(parentEntity != null && !parentEntity.isEmpty()){
			subjectField.setParticipant(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));
			actorType = ActorType.SUBJECT;
		}

		refreshLayout();

		return this;
	}

	public BaseRecordDialog withObject(final String parentEntityId, final String parentEntityHandlerType){
		super.withParentEntity(parentEntityId, parentEntityHandlerType);

		if(parentEntity != null && !parentEntity.isEmpty()){
			objectField.setParticipant(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));
			actorType = ActorType.OBJECT;
		}

		refreshLayout();

		return this;
	}


	@Override
	protected void loadData(){
		subjectField.load(record);
		objectField.load(record);

		bindingManager.load(record);

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

		if(subjectField.equals(objectField)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject and Object must not be the same entity.",
				tabbedPane, mainPanel, subjectField);
			return false;
		}

		if(!subjectTypeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type is required.",
				tabbedPane, mainPanel, subjectTypeCombo);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.saveReferences(record);
		objectField.saveReferences(record);

		bindingManager.save(record);

		validFromField.save(record);
		validToField.save(record);
		notePanel.save(record);
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
			final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
			model.addRecord(individual);

			final RelationshipRecordDialog dialog = RelationshipRecordDialog.createNew(null, model);
//			dialog.setSubject("I1", IndividualHandler.TYPE);
			dialog.setVisible(true);
		});
	}

}
