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
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Dialog for editing a {@code PLACE_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record PlaceRecord {
 *   name+: NameStructure
 *   type?: enum {
 *     // settlements and built environments
 *     address,             // specific postal or civic address
 *     building,            // individual building or structure
 *     street,              // street, road, avenue, lane, or similar thoroughfare
 *     hamlet,              // very small settlement, usually smaller than a village
 *     village,             // small rural settlement
 *     town,                // populated settlement larger than a village
 *     municipality,        // local administrative unit with self-government
 *     city,                // major urban settlement
 *     metropolitan_area,   // urban area consisting of a central city and surrounding communities
 *     // administrative jurisdictions
 *     county,              // county-level administrative division
 *     province,            // province-level administrative division
 *     department,          // department-level administrative division
 *     district,            // district-level administrative division
 *     region,              // region-level administrative division
 *     macro_region,        // supra-regional grouping of multiple regions
 *     country,             // sovereign state or nation
 *     empire,              // multi-ethnic or multi-national imperial state
 *     // ecclesiastical jurisdictions
 *     parish,              // ecclesiastical parish
 *     diocese,             // ecclesiastical diocese or equivalent jurisdiction
 *     // sites and institutions
 *     cemetery             // burial ground or cemetery
 *   } | Text
 *   map?: struct {              // Contains a representative geographic location for the place.
 *     coordinates: Coord        // geographic coordinates of the place, representative point, not necessarily the full territorial extent of the place
 *     source*: SourceCitation   // the evidence that this location was true for the place
 *     confidence?: Confidence   // confidence that this location was true for the place
 *   }
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   confidence?: Confidence
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): name, type, map, confidence
 * Tab 3 (Relationships): PlaceRelationshipRecord (subject = this place), PlaceRelationshipRecord (target = this place)
 * Tab 4 (Participations): EventParticipationRecord (participant[place] = this place)
 * Tab 5 (Context): ContextImpactRecord (target[place] = this place)
 * Tab 6 (Research): ConclusionRecord (resolves = this place), IdentityHypothesisRecord (identity = this place), ResearchQuestionRecord (target[place] = this place)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class PlaceRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final BoundTextField locationCoordinatesField;
	private final BoundComboBox<String> locationConfidenceCombo;
	private final BoundComboBox<String> confidenceCombo;


	public static PlaceRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, PlaceRecordDialog::new);
	}

	public static PlaceRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PlaceRecordDialog::new);
	}


	private PlaceRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PlaceHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		namePanel = EntityListPanel.createForStructure(PlaceReader.TAG_NAME, this, GUIHelper.requiredTitle("dialog.place.names"), model, NameHandler.class);
		typeCombo = new BoundComboBox<>(PlaceReader.TAG_TYPE, GUIHelper.fillCombo(PlaceReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.place.type");
		typeCombo.setEditable(true);
		locationCoordinatesField = new BoundTextField(PlaceReader.TAG_LOCATION_COORDINATES);
		locationConfidenceCombo = new BoundComboBox<>(PlaceReader.TAG_LOCATION_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		locationConfidenceCombo.setI18NPrefix("enum.confidence");
		confidenceCombo = new BoundComboBox<>(PlaceReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT, PlaceRelationshipHandler.TYPE, I18N.t("dialog.component.place_relationships_on_subject"))
			.withComponent(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT, PlaceRelationshipHandler.TYPE, I18N.t("dialog.component.place_relationships_on_target"))
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, EventParticipationHandler.TYPE, I18N.t("dialog.component.event.participations"))
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impacts"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY, IdentityHypothesisHandler.TYPE, I18N.t("dialog.component.identity.hypotheses"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, PlaceReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.SOURCE_FOR_LOCATION, PlaceReader.TAG_LOCATION_SOURCE, I18N.t("dialog.component.location.sources_with_citations"))
			.withComponent(PanelKey.NOTE, PlaceReader.TAG_NOTE, null)
			.withComponent(PanelKey.PRIVACY, PlaceReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, PlaceReader.TAG_AUDIT, null)
			.build();

		components.bind(typeCombo);
		components.bind(locationCoordinatesField);
		components.bind(locationConfidenceCombo);
		components.bind(confidenceCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// name
		GUIHelper.addComponent(propertiesPanel, namePanel);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.place.type"), typeCombo);

		// place evidence
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.place.confidence"), confidenceCombo);

		return propertiesPanel;
	}

	@Override
	protected JPanel createLocationPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(5, "[]5[]5[]");

		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.place.location.coordinates"), locationCoordinatesField);

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE_FOR_LOCATION);
		GUIHelper.addComponent(panel, sourcePanel);

		GUIHelper.addLabeledComponent(panel, I18N.l("dialog.place.location.confidence"), locationConfidenceCombo);

		return panel;
	}

	@Override
	protected JPanel createRelationshipsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		final JPanel placeRelationshipAsSubjectPanel = components.getPanel(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT);
		GUIHelper.addComponent(panel, placeRelationshipAsSubjectPanel);

		final JPanel placeRelationshipAsObjectPanel = components.getPanel(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT);
		GUIHelper.addComponent(panel, placeRelationshipAsObjectPanel);

		return panel;
	}

	@Override
	protected JPanel createParticipationsPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel eventParticipationPanel = components.getPanel(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT);
		GUIHelper.addComponent(panel, eventParticipationPanel);

		return panel;
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
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		// conclusion
		final JPanel conclusionPanel = components.getPanel(PanelKey.CONCLUSION_ON_RESOLVES);
		GUIHelper.addComponent(panel, conclusionPanel);

		// identity hypothesis
		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY);
		GUIHelper.addComponent(panel, identityHypothesisPanel);

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
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		components.load(record);

		namePanel.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.place.names")),
				tabbedPane, propertiesPanel, namePanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		namePanel.save(record);

		components.save(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(PlaceRecordDialog::createEdit, "/tests/test.flef", "P1");
	}

}
