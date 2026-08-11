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
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.MemberRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.RelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.StructureListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
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
	}


	private final BindingManager bindingManager = new BindingManager();

	private final StructureListPanel personalNamePanel;
	private final BoundComboBox<String> sexCombo;
	private final EntityReferenceListPanel culturalNormPanel;
	private final EntityReferenceListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final PreferredImagePanel preferredImagePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final EntityReferenceListPanel conclusionPanel;
	private final MemberRelationshipListPanel memberPanel;
	private final EntityReferenceListPanel attributePanel;
	private final RelationshipListPanel relationshipPanel;


	public static IndividualRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, IndividualRecordDialog::new);
	}

	public static IndividualRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, IndividualRecordDialog::new);
	}


	private IndividualRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualHandler.TYPE));

		personalNamePanel = new StructureListPanel(TAG_PERSONAL_NAME, this, "Personal Names*", model, PersonalNameHandler.TYPE);
		sexCombo = new BoundComboBox<>(TAG_SEX,
			new String[]{StringUtils.EMPTY, "male", "female", "unknown"});
		culturalNormPanel = new EntityReferenceListPanel(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		notePanel = new EntityReferenceListPanel(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, "Sources", model);
		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		conclusionPanel = new EntityReferenceListPanel(TAG_CONCLUSION, this, "Conclusions", model, ConclusionHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		memberPanel = new MemberRelationshipListPanel(this, model, this.record.getId());
		attributePanel = new EntityReferenceListPanel(TAG_INDIVIDUAL_ATTRIBUTE, this, "Individual Attributes", model, IndividualAttributeHandler.TYPE)
			.withParentEntity(this.record.getId(), IndividualHandler.TYPE);
		relationshipPanel = new RelationshipListPanel(TAG_RELATIONSHIP, this, "Relationships", model);


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

		conclusionPanel.load(record);
		memberPanel.load(record);
		attributePanel.load(record);
		relationshipPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		personalNamePanel.save(record);
		culturalNormPanel.saveReferences(record);
		notePanel.saveReferences(record);
		sourceCitationPanel.save(record);
		preferredImagePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);

		conclusionPanel.saveReferences(record);
		memberPanel.save(record);
		attributePanel.saveReferences(record);
		relationshipPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(IndividualRecordDialog::createNew);
	}

}
