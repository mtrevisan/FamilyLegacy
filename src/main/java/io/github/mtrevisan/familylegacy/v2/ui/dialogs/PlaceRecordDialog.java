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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code PLACE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record PlaceRecord {
 *   id: LocalID
 *   name+: ClassifiedNameStructure
 *   type?: enum {
 *     address, building, street, hamlet, village, town, municipality, city,
 *     metropolitan_area, county, province, department, district, region,
 *     macro_region, country, empire, parish, diocese, cemetery, archive, unknown
 *   } | Text
 *   map?: struct {
 *     coordinates: Coord
 *     evidence?: EvidenceQualifiers
 *   }
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): name, type, map, evidence
 * Tab 3 (Relationships): PlaceRelationshipRecord (subject = this place), PlaceRelationshipRecord (object = this place)
 * Tab 4 (Participations): EventParticipationRecord (participant.place = this place)
 * Tab 5 (Context): ContextImpactRecord (target.place = this place)
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this place), IdentityHypothesisRecord (subject/candidate = this place), ResearchQuestionRecord (target.place = this place)
 * Tab 7 (Sources): source
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class PlaceRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2581031991500033899L;


	private static final String DOT = ".";

	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_MAP = "MAP";
	private static final String TAG_COORDINATES = "COORDINATES";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";

	private static final String TAG_PLACE_RELATIONSHIP = "PLACE_RELATIONSHIP";
	private static final String TAG_EVENT_PARTICIPATION = "EVENT_PARTICIPATION";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_IDENTITY_HYPOTHESIS = "IDENTITY_HYPOTHESIS";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final EntityReferenceListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final BoundTextField mapCoordinatesField;
	private final EvidenceQualifiersPanel mapEvidencePanel;


	public static PlaceRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PlaceRecordDialog::new);
	}

	public static PlaceRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PlaceRecordDialog::new);
	}


	private PlaceRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PlaceHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]");

		namePanel = EntityReferenceListPanel.createForStructure(TAG_NAME, this, "Names*", model, ClassifiedNameHandler.class);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"address", "building", "street", "hamlet", "village", "town",
			"municipality", "city", "metropolitan_area", "county", "province",
			"department", "district", "region", "macro_region", "country",
			"empire", "parish", "diocese", "cemetery", "archive", "unknown"
		});
		typeCombo.setEditable(true);
		mapCoordinatesField = new BoundTextField(TAG_MAP + DOT + TAG_COORDINATES);
		mapEvidencePanel = new EvidenceQualifiersPanel(TAG_MAP + DOT + TAG_EVIDENCE, this, "Map Evidence", model, null);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT, TAG_PLACE_RELATIONSHIP, "Members", PlaceRelationshipHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.PLACE_RELATIONSHIP_ON_OBJECT, TAG_PLACE_RELATIONSHIP, "Relationships", PlaceRelationshipHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, TAG_EVENT_PARTICIPATION, "Participations", EventParticipationHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_TARGET, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE, TAG_IDENTITY_HYPOTHESIS, "Identity Hypotheses", IdentityHypothesisHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, PlaceHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, GroupAttributeHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(typeCombo);
		components.bind(mapCoordinatesField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// name
		GUIHelper.addComponent(propertiesPanel, namePanel);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Type:", typeCombo);

		// map panel:
		final JPanel mapPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]");
		mapPanel.setBorder(new TitledBorder("Map"));
		GUIHelper.addLabeledComponent(mapPanel, "Coordinates:", mapCoordinatesField);
		GUIHelper.addComponent(mapPanel, mapEvidencePanel);
		GUIHelper.addComponent(propertiesPanel, mapPanel);

		// place evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
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
		final JPanel identityHypothesisPanel = components.getPanel(PanelKey.IDENTITY_HYPOTHESIS_ON_SUBJECT_OR_CANDIDATE);
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
		mapEvidencePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one name is required.",
				tabbedPane, propertiesPanel, namePanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		namePanel.save(record);

		components.save(record);

		mapEvidencePanel.save(record);
	}


	public static void main(final String[] args){
//		GUIHelper.launch(PlaceRecordDialog::createNew, modelFiller);

		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "fdgh"));
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("PLACE", "@P1@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("GROUP", "@G2@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("PREFERRED", "@P1@"));
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("PROOF_STATUS", "supported"));
		final FLEFRecord place = FLEFRecord.createMainRecord("P1", "PLACE");
		place.addChild(FLEFRecord.createChildWithTag("NAME")
			.addChild(FLEFRecord.createChildWithTag("TEXT")
				.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "f"))
			)
		);
		place.addChild(FLEFRecord.createChildWithTagAndValue("CONCLUSION", conclusion.getId()));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(conclusion);
			model.addRecord(place);
		};
		GUIHelper.launch(PlaceRecordDialog::createEdit, modelFiller, place);
	}

}
