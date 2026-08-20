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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code GROUP_ATTRIBUTE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupAttributeRecord {
 *   id: LocalID
 *   group: Xref&lt;GroupRecord&gt;
 *   type: enum {
 *     residence,
 *     member_count,
 *     children_count,
 *     social_class,
 *     ethnicity,
 *     religion,
 *     language,
 *     wealth,
 *     land_holding,
 *     primary_income_source
 *   } | Text
 *   value?: Text
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   place?: PlaceCitation
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): group, type, value, valid_from, valid_to, place, evidence
 * Tab 5 (Context): ContextImpactRecord (target.group_attribute = this attribute)
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this attribute), ResearchQuestionRecord (target.group_attribute = this attribute)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class GroupAttributeRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -5939902730413020982L;


	private static final String TAG_GROUP = "GROUP";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final DateField validFromField;
	private final DateField validToField;
	private final PlaceCitationField placeField;


	public static GroupAttributeRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, GroupAttributeRecordDialog::new);
	}

	public static GroupAttributeRecordDialog createEdit(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		return createEdit(parent, model, record, GroupAttributeRecordDialog::new);
	}


	private GroupAttributeRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, GroupAttributeHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"residence", "member_count", "children_count", "social_class", "ethnicity", "religion", "language",
			"wealth", "land_holding", "primary_income_source"
		});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "Valid From", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "Valid To", model);
		placeField = PlaceCitationField.create(TAG_PLACE, this, model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, GroupAttributeHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, GroupAttributeHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, GroupAttributeHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, GroupAttributeHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);
		components.bind(valueField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// group
		//parentEntity

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Type*:", typeCombo);

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, "Value:", valueField);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, "Valid From:", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, "Valid To:", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, "Place:", placeField);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_TARGET);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createResearchPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// research question
		final JPanel researchQuestionPanel = components.getPanel(PanelKey.RESEARCH_QUESTION_ON_TARGET);
		GUIHelper.addComponent(panel, researchQuestionPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	public void setGroup(final String groupId){
		if(StringUtils.isNotEmpty(groupId)){
			if(!confirmRecordExistsForType(groupId, GroupHandler.class))
				return;

			withParentEntity(groupId, GroupHandler.TYPE);
			refreshLayout();
		}
	}

	private void refreshLayout(){
		propertiesPanel.revalidate();
		propertiesPanel.repaint();

		pack();
	}


	@Override
	protected void loadData(){
		validFromField.load(record);
		validToField.load(record);
		placeField.load(record);

		components.load(record);


		// load parent group reference
		final String groupId = FLEFRecordHelper.getChildValue(record, TAG_GROUP);
		withParentEntity(groupId, GroupHandler.TYPE);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(parentEntity.getPath()) || StringUtils.isEmpty(parentEntity.getText())){
			JOptionPane.showMessageDialog(null,
				"Parent Group is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(StringUtils.isEmpty((String)typeCombo.getSelectedItem())){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type cannot be empty.",
				tabbedPane, propertiesPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		validFromField.save(record);
		validToField.save(record);
		placeField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord groupAttribute = FLEFRecord.createMainRecord("GA1", "GROUP_ATTRIBUTE");
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G1@"));
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("TYPE", "residence"));
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "123 Main St"));
		groupAttribute.addChild(FLEFRecord.createChildWithTag("SOURCE")
			.addChild(FLEFRecord.createChildWithTagAndValue("SOURCE", "@S1@"))
		);
		groupAttribute.addChild(FLEFRecord.createChildWithTagAndValue("NOTE", "@N1@"));

		final FLEFRecord group1 = FLEFRecord.createMainRecord("G1", "GROUP");

		final FLEFRecord source1 = FLEFRecord.createMainRecord("S1", "SOURCE");
		source1.addChild(FLEFRecord.createChildWithTag("TITLE")
			.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Attribute source")));

		final FLEFRecord contextImpact1 = FLEFRecord.createMainRecord("CI1", "CONTEXT_IMPACT");
		contextImpact1.addChild(FLEFRecord.createChildWithTag("CONTEXT")
			.addChild(FLEFRecord.createChildWithTagAndValue("CULTURAL_NORM", "@CN1@"))
		);
		contextImpact1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP_ATTRIBUTE", "@GA1@"))
		);

		final FLEFRecord conclusion1 = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion1.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "death cause"));
		conclusion1.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP_ATTRIBUTE", "@GA1@"))
		);

		final FLEFRecord researchQuestion1 = FLEFRecord.createMainRecord("RQ1", "RESEARCH_QUESTION");
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("TITLE", "rq title"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("QUESTION", "is?"));
		researchQuestion1.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP_ATTRIBUTE", "@GA1@"))
		);
		researchQuestion1.addChild(FLEFRecord.createChildWithTagAndValue("STATUS", "open"));

		final FLEFRecord note1 = FLEFRecord.createMainRecord("N1", "NOTE");
		note1.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Ind attr note"));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(groupAttribute);
			model.addRecord(group1);
			model.addRecord(source1);
			model.addRecord(contextImpact1);
			model.addRecord(conclusion1);
			model.addRecord(researchQuestion1);
			model.addRecord(note1);
		};
		GUIHelper.launch(GroupAttributeRecordDialog::createEdit, modelFiller, groupAttribute);
	}

}
