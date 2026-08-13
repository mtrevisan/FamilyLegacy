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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/* DONE */
/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupRecord {
 *   id: LocalID
 *   name*: ClassifiedName
 *   type?: enum { family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe } | Text
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropRect
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public class GroupRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_GROUP_ATTRIBUTE = "GROUP_ATTRIBUTE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";


	static{
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new ClassifiedNameHandler());
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ConclusionTargetHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final EntityReferenceListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourcePanel;
	private final PreferredImagePanel preferredImagePanel;
	private final RestrictionPanel privacyPanel;
	private final ModificationPanel auditPanel;

	// Other
	private final EntityReferenceListPanel culturalNormPanel;
	private final EntityReferenceListPanel conclusionPanel;
	private final EntityReferenceListPanel memberPanel;
	private final EntityReferenceListPanel attributePanel;
	private final EntityReferenceListPanel relationshipPanel;


	public static GroupRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, GroupRecordDialog::new);
	}

	public static GroupRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, GroupRecordDialog::new);
	}


	private GroupRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupHandler.TYPE));

		namePanel = EntityReferenceListPanel.createForStructure(TAG_NAME, this, "Names", model, ClassifiedNameHandler.TYPE);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"family", "household", "neighbourhood", "fraternity", "club", "literary_society", "association",
			"organisation", "tribe"});
		typeCombo.setEditable(true);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		privacyPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		auditPanel = new ModificationPanel(this);

		culturalNormPanel = EntityReferenceListPanel.createForRecord(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);
		conclusionPanel = EntityReferenceListPanel.createForRecord(TAG_CONCLUSION, this, "Conclusions", model, ConclusionTargetHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);
		memberPanel = EntityReferenceListPanel.createForRecord(TAG_RELATIONSHIP, this, "Members", model, RelationshipHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);
		attributePanel = EntityReferenceListPanel.createForRecord(TAG_GROUP_ATTRIBUTE, this, "Group Attributes", model, GroupAttributeHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);
		relationshipPanel = EntityReferenceListPanel.createForRecord(TAG_RELATIONSHIP, this, "Relationships", model, RelationshipHandler.TYPE)
			.withParentEntity(this.record.getId(), GroupHandler.TYPE);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Privacy", privacyPanel);
		tabbedPane.addTab("Audit", auditPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]15[]10[]10[]10[]"));

		panel.add(preferredImagePanel, "growx,align center");

		// type
		final JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// names
		panel.add(namePanel, "growx");

		// members
		panel.add(memberPanel, "growx");

		// attributes
		panel.add(attributePanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]10[]10[]"));
		panel.add(culturalNormPanel, "growx");
		panel.add(conclusionPanel, "growx");
		panel.add(relationshipPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		namePanel.load(record);
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
		preferredImagePanel.load(record);
		privacyPanel.load(record);
		auditPanel.load(record);

		conclusionPanel.loadReference(record.getId());
		memberPanel.loadReference(record.getId());
		attributePanel.loadReference(record.getId());
		relationshipPanel.loadReference(record.getId());
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		namePanel.save(record);
		culturalNormPanel.save(record);
		notePanel.save(record);
		sourcePanel.save(record);
		preferredImagePanel.save(record);
		privacyPanel.save(record);
		auditPanel.save(record);

		conclusionPanel.save(record);
		memberPanel.save(record);
		attributePanel.save(record);
		relationshipPanel.save(record);
	}


	public static void main(final String[] args){
//		GUIHelper.launch(GroupRecordDialog::createNew, modelFiller);

		final FLEFRecord groupAttribute = FLEFRecord.createMainRecord("GA1", "GROUP_ATTRIBUTE");
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"));
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "residence"));
		final FLEFRecord relationship = FLEFRecord.createMainRecord("RL1", "RELATIONSHIP");
		relationship.addChild(FLEFRecord.createChildWithTag("SUBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"))
		);
		relationship.addChild(FLEFRecord.createChildWithTag("OBJECT")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G2@"))
		);
		relationship.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "associate"));
		final FLEFRecord group1 = FLEFRecord.createMainRecord("G1", "GROUP");
		final FLEFRecord group2 = FLEFRecord.createMainRecord("G2", "GROUP");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(groupAttribute);
			model.addRecord(relationship);
			model.addRecord(group1);
			model.addRecord(group2);
		};
		GUIHelper.launch(GroupRecordDialog::createEdit, modelFiller, group1);
	}

}
