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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code PLACE_RELATIONSHIP_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record PlaceRelationshipRecord {
 *   subject: struct {
 *     place: Xref<PlaceRecord>   // the place for which the relationship is asserted. The SUBJECT is always the place whose role is described by TYPE relative to the TARGET.
 *   }
 *   target: struct {
 *     place: Xref<PlaceRecord>   // the place to which the subject is related
 *   }
 *   type: enum {
 *     administrative_part_of,    // subject is part of an administrative jurisdiction, e.g., Treviso → Veneto
 *     geographic_part_of,        // subject is geographically contained within another place
 *     ecclesiastical_part_of,    // subject belongs to an ecclesiastical jurisdiction such as a parish, diocese, or archdiocese
 *     judicial_part_of,          // subject belongs to a court, district, or judicial jurisdiction
 *     cadastral_part_of,         // subject belongs to a cadastral or land-records district
 *     successor_of,              // subject replaced or inherited the identity or function of the TARGET place, other than by merger or division
 *     merged_from,               // subject was created, wholly or partially, by the merger of the TARGET place (one record per merged place)
 *     split_from,                // subject was created from the division of the TARGET place
 *     received_territory_from    // subject received part or all of the territory of the TARGET place, which continued to exist
 *   } | Text
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   confidence?: Confidence      // confidence that this place relationship was true
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): subject, target, type, valid_from, valid_to
 * Tab 5 (Context): ContextImpactRecord (target[place_relationship] = this relationship)
 * Tab 6 (Research): ConclusionRecord (resolves = this relationship), ResearchQuestionRecord (target[place_relationship] = this relationship)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 10 (Audit): audit
 */
public class PlaceRelationshipRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityField subjectField;
	private final EntityField targetField;
	private final BoundComboBox<String> typeCombo;
	private final DateField validFromField;
	private final DateField validToField;
	private final BoundComboBox<String> confidenceCombo;


	public static PlaceRelationshipRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, PlaceRelationshipRecordDialog::new);
	}

	public static PlaceRelationshipRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, PlaceRelationshipRecordDialog::new);
	}


	private PlaceRelationshipRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PlaceRelationshipHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		subjectField = EntityField.createForRecordFromOneofReference(PlaceRelationshipReader.TAG_SUBJECT, this, model)
			.withHandlerTypes(PlaceHandler.class);
		targetField = EntityField.createForRecordFromOneofReference(PlaceRelationshipReader.TAG_TARGET, this, model)
			.withHandlerTypes(PlaceHandler.class);
		typeCombo = new BoundComboBox<>(PlaceRelationshipReader.TAG_TYPE, GUIHelper.fillCombo(PlaceRelationshipReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.place_relationship.type");
		typeCombo.setEditable(true);
		validFromField = DateField.createWithWrapperTag(PlaceRelationshipReader.TAG_VALID_FROM, this, I18N.t("dialog.date.valid_from"), model);
		validToField = DateField.createWithWrapperTag(PlaceRelationshipReader.TAG_VALID_TO, this, I18N.t("dialog.date.valid_to"), model);
		confidenceCombo = new BoundComboBox<>(PlaceRelationshipReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impacts"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, PlaceRelationshipReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.NOTE, PlaceRelationshipReader.TAG_NOTE, null)
			.withComponent(PanelKey.AUDIT, PlaceRelationshipReader.TAG_AUDIT, null)
			.build();

		components.bind(typeCombo);
		components.bind(confidenceCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// subject
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place_relationship.subject") + "*:", subjectField);

		// target
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place_relationship.target") + "*:", targetField);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place_relationship.part.type") + "*:", typeCombo);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]5[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.validity.range")));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid_from") + ":", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid_to") + ":", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		// confidence
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place_relationship.confidence") + ":", confidenceCombo);

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

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

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
	public BaseRecordDialog withParentEntity(final FLEFRecord parent){
		JOptionPane.showMessageDialog(this,
			I18N.tf("error.cannot_set_parent.message", PlaceRelationshipHandler.getInstance().getLabel()),
			I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

		return this;
	}

	public PlaceRelationshipRecordDialog withSubject(final FLEFRecord subject){
		super.withParentEntity(subject);

		if(parentEntity != null && !parentEntity.isEmpty()){
			subjectField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentEnabled(subjectField, showAll);
			GUIHelper.setComponentEnabled(targetField, true);
		}

		return this;
	}

	public PlaceRelationshipRecordDialog withTarget(final FLEFRecord target){
		super.withParentEntity(target);

		if(parentEntity != null && !parentEntity.isEmpty()){
			targetField.setEntity(FLEFRecord.createMainRecord(parentEntity.getText(), parentEntity.getPath()));

			final boolean showAll = (parentEntity == null || parentEntity.isEmpty());
			GUIHelper.setComponentEnabled(subjectField, true);
			GUIHelper.setComponentEnabled(targetField, showAll);
		}

		return this;
	}


	@Override
	protected void loadData(){
		subjectField.load(record);
		targetField.load(record);

		components.load(record);

		validFromField.load(record);
		validToField.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(!subjectField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.place_relationship.subject")),
				tabbedPane, propertiesPanel, subjectField);

			return false;
		}

		if(!targetField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.place_relationship.target")),
				tabbedPane, propertiesPanel, targetField);

			return false;
		}

		if(!typeCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.place_relationship.part.type")),
				tabbedPane, propertiesPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		subjectField.saveReferences(record);
		targetField.saveReferences(record);

		components.save(record);

		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(PlaceRelationshipRecordDialog::createEdit, "/tests/test.flef", "PR1");
	}

}
