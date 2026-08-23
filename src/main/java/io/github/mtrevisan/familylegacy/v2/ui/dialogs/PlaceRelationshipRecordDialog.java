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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/**
 * Dialog for editing a {@code PLACE_RELATIONSHIP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record PlaceRelationshipRecord {
 *   id: LocalID
 *   subject: Xref&lt;PlaceRecord&gt;
 *   object: Xref&lt;PlaceRecord&gt;
 *   type: enum { administrative_part_of, geographic_part_of, ecclesiastical_part_of, judicial_part_of, cadastral_part_of } | Text
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): subject, object, type, valid_from, valid_to
 * Tab 5 (Context): ContextImpactRecord (target[place_relationship] = this relationship)
 * Tab 6 (Research): ConclusionRecord (resolves = this relationship), ResearchQuestionRecord (target[place_relationship] = this relationship)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 10 (Audit): audit
 */
public class PlaceRelationshipRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4588274864438851179L;


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_AUDIT = "AUDIT";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";


	private final JPanel propertiesPanel;

	private final RecordDialogComponents components;

	private final ParticipantField subjectField;
	private final ParticipantField objectField;
	private final BoundComboBox<String> typeCombo;
	private final DateField validFromField;
	private final DateField validToField;


	public static PlaceRelationshipRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PlaceRelationshipRecordDialog::new);
	}

	public static PlaceRelationshipRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, PlaceRelationshipRecordDialog::new);
	}


	private PlaceRelationshipRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PlaceRelationshipHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		subjectField = ParticipantField.create(TAG_SUBJECT, this, model)
			.withHandlerTypes(PlaceHandler.class);
		objectField = ParticipantField.create(TAG_OBJECT, this, model)
			.withHandlerTypes(PlaceHandler.class);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"administrative_part_of", "geographic_part_of", "ecclesiastical_part_of", "judicial_part_of",
			"cadastral_part_of"
		});
		typeCombo.setEditable(true);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, PlaceRelationshipHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, PlaceRelationshipHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, PlaceRelationshipHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, PlaceRelationshipHandler.class)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// subject
		GUIHelper.addLabeledComponent(propertiesPanel, "Subject*:", subjectField);

		// object
		GUIHelper.addLabeledComponent(propertiesPanel, "Object*:", objectField);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Part Type*:", typeCombo);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, "Valid From:", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, "Valid To:", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

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

		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

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
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	public BaseRecordDialog withParentEntity(final String parentEntityId, final String parentEntityPath){
		JOptionPane.showMessageDialog(this, "Cannot set parent on a Place Relationship Record.",
			"Error", JOptionPane.ERROR_MESSAGE);

		return this;
	}

	public PlaceRelationshipRecordDialog withSubject(final String placeId, final String placePath){
		super.withParentEntity(placeId, placePath);

		if(parentEntity != null && !parentEntity.isEmpty()){
			subjectField.setParticipant(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, showAll);
			GUIHelper.setComponentVisible(objectField, true);
		}

		return this;
	}

	public PlaceRelationshipRecordDialog withObject(final String parentEntityId, final String parentEntityPath){
		super.withParentEntity(parentEntityId, parentEntityPath);

		if(parentEntity != null && !parentEntity.isEmpty()){
			objectField.setParticipant(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentVisible(subjectField, true);
			GUIHelper.setComponentVisible(objectField, showAll);
		}

		return this;
	}


	@Override
	protected void loadData(){
		subjectField.load(record);
		objectField.load(record);

		components.load(record);

		validFromField.load(record);
		validToField.load(record);
	}

	@Override
	protected boolean validData(){
		if(!subjectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject cannot be empty.",
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!objectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Object cannot be empty.",
				tabbedPane, propertiesPanel, objectField);

			return false;
		}

		if(!typeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type cannot be empty.",
				tabbedPane, propertiesPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.saveReferences(record);
		objectField.saveReferences(record);

		components.save(record);

		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		try(final InputStream is = PlaceRelationshipRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(PlaceRelationshipRecordDialog::createEdit, content, "PR1");
		}
	}

}
