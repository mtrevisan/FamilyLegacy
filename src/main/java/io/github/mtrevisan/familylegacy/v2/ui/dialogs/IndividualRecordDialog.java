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
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.MemberRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
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
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record IndividualRecord {
 *   id: LocalID
 *   name*: PersonalNameStructure
 *   sex?: enum { male, female, unknown }
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropRect
 *   }
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class IndividualRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212974L;


	private static final String TAG_PERSONAL_NAME = "NAME";
	private static final String TAG_SEX = "SEX";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_INDIVIDUAL_ATTRIBUTE = "INDIVIDUAL_ATTRIBUTE";
	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new PersonalNameHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ConclusionTargetHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final EntityReferenceListPanel personalNamePanel;
	private final BoundComboBox<String> sexCombo;
	private final EntityReferenceListPanel culturalNormPanel;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourceCitationPanel;
	private final PreferredImagePanel preferredImagePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final EntityReferenceListPanel conclusionPanel;
	//TODO ONGOING
	private final MemberRelationshipListPanel memberPanel;
	private final EntityReferenceListPanel attributePanel;
	private final EntityReferenceListPanel relationshipPanel;


	public static IndividualRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualHandler.TYPE));

		personalNamePanel = EntityReferenceListPanel.createForStructure(TAG_PERSONAL_NAME, this, "Personal Names*", model, PersonalNameHandler.TYPE);
		sexCombo = new BoundComboBox<>(TAG_SEX, new String[]{
			StringUtils.EMPTY,
			"male", "female", "unknown"});
		culturalNormPanel = EntityReferenceListPanel.createForRecord(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		sourceCitationPanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		conclusionPanel = EntityReferenceListPanel.createForRecord(TAG_CONCLUSION, this, "Conclusions", model, ConclusionTargetHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		memberPanel = new MemberRelationshipListPanel(this, "Members", model, this.record.getId(), IndividualHandler.TYPE);
		attributePanel = EntityReferenceListPanel.createForRecord(TAG_INDIVIDUAL_ATTRIBUTE, this, "Individual Attributes", model, IndividualAttributeHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		relationshipPanel = EntityReferenceListPanel.createForRecord(TAG_RELATIONSHIP, this, "Relationships", model, RelationshipHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(sexCombo);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]15[]10[]10[]"));

		panel.add(preferredImagePanel, "growx,align center");

		// names
		panel.add(personalNamePanel, "growx");

		// Sex – now using the bound combo box
		final JPanel sexPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		sexPanel.add(new JLabel("Sex:"), "align label");
		sexPanel.add(sexCombo, "growx");
		panel.add(sexPanel, "growx");

		// members
		panel.add(memberPanel, "growx");

		// attributes
		panel.add(attributePanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]10[]10[]"));
		panel.add(conclusionPanel, "growx");
		panel.add(relationshipPanel, "growx");
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		personalNamePanel.load(record);
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourceCitationPanel.load(record);
		preferredImagePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);

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

		personalNamePanel.save(record);
		culturalNormPanel.save(record);
		notePanel.save(record);
		sourceCitationPanel.save(record);
		preferredImagePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);

		conclusionPanel.save(record);
		memberPanel.save(record);
		attributePanel.save(record);
		relationshipPanel.save(record);
	}


	public static void main(final String[] args){
//		GUIHelper.launch(IndividualRecordDialog::createNew, modelFiller);

		final FLEFRecord individualAttribute = FLEFRecord.createMainRecord("IA1", "INDIVIDUAL_ATTRIBUTE");
		individualAttribute.addChild(FLEFRecord.createChildWithValue("INDIVIDUAL", "@I1@"));
		individualAttribute.addChild(FLEFRecord.createChildWithValue("TYPE", "residence"));
		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(individualAttribute);
			model.addRecord(individual);
		};
		GUIHelper.launch(IndividualRecordDialog::createEdit, modelFiller, individual);
	}

}
