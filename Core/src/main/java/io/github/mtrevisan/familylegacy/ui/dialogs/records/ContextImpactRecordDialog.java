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
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code CONTEXT_IMPACT_RECORD} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * record ContextImpactRecord {
 *   id: LocalID
 *   context: ContextSource
 *   target: ImpactTarget
 *   impact_type?: enum { explains, influences, constrains, motivates, causes } | Text
 *   explanation?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): context, target, impact_type, explanation, evidence
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class ContextImpactRecordDialog extends BaseRecordDialog{

	private final EntityField contextField;
	private final EntityField targetField;
	private final BoundComboBox<String> impactTypeCombo;
	private final BoundTextArea rationaleArea;
	private final BoundComboBox<String> confidenceCombo;

	private final JPanel propertiesPanel;


	public static ContextImpactRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ContextImpactRecordDialog::new);
	}

	public static ContextImpactRecordDialog createEdit(final Window parent, final FLEFModel model,
		final FLEFRecord record){
		return createEdit(parent, model, record, ContextImpactRecordDialog::new);
	}


	private ContextImpactRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ContextImpactHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]10[]");

		contextField = EntityField.createForRecordFromOneofReference(ContextImpactReader.TAG_CONTEXT, this, model)
			.withHandlerTypes(CulturalNormHandler.class, HistoricEventHandler.class);

		targetField = EntityField.createForRecordFromOneofReference(ContextImpactReader.TAG_TARGET, this, model)
			.withHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class, EventHandler.class,
				RelationshipHandler.class, IndividualAttributeHandler.class, GroupAttributeHandler.class,
				ConclusionHandler.class, EventParticipationHandler.class, PlaceRelationshipHandler.class,
				IdentityHypothesisHandler.class);

		impactTypeCombo = new BoundComboBox<>(ContextImpactReader.TAG_IMPACT_TYPE, GUIHelper.fillCombo(ContextImpactReader.IMPACT_TYPES, null));
		impactTypeCombo.setI18NPrefix("enum.context.impact.type");
		impactTypeCombo.setEditable(true);

		rationaleArea = new BoundTextArea(ContextImpactReader.TAG_RATIONALE, 3, 30);

		confidenceCombo = new BoundComboBox<>(ContextImpactReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ContextImpactReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE, ContextImpactReader.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.EVIDENCE, ContextImpactReader.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.AUDIT, ContextImpactReader.TAG_AUDIT, null)
			.build();

		components.bind(impactTypeCombo);
		components.bind(rationaleArea);
		components.bind(confidenceCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// context
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.context.impact.context") + "*:", contextField);

		// target
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.context.impact.target") + "*:", targetField);

		// impact type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.context.impact.impact.type") + ":", impactTypeCombo);

		// explanation
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.context.impact.explanation") + ":", rationaleArea);

		// confidence
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.context.impact.confidence") + ":", confidenceCombo);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
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
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		contextField.load(record);
		targetField.load(record);

		components.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(contextField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.context.impact.context")),
				tabbedPane, propertiesPanel, contextField);

			return false;
		}

		if(targetField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.context.impact.target")),
				tabbedPane, propertiesPanel, targetField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		contextField.saveReferences(record);
		targetField.saveReferences(record);

		components.save(record);
	}


	public static void main(final String[] args){
		final FLEFRecord culturalNorm = FLEFRecord.createMainRecord("CN1", CulturalNormHandler.TYPE);
		culturalNorm.addChild(FLEFRecord.createChildWithTagAndValue("NAME", "Primogeniture"));

		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", IndividualHandler.TYPE);
		individual.addChild(FLEFRecord.createChildWithTagAndValue("SEX", "male"));

		final FLEFRecord contextImpact = FLEFRecord.createMainRecord("CI1", ContextImpactHandler.TYPE);
		contextImpact.addChild(FLEFRecord.createChildWithTag("CONTEXT")
			.addChild(FLEFRecord.createChildWithTagAndValue("CULTURAL_NORM", "@CN1@")));
		contextImpact.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@")));
		contextImpact.addChild(FLEFRecord.createChildWithTagAndValue("IMPACT_TYPE", "constrains"));
		contextImpact.addChild(FLEFRecord.createChildWithTagAndValue("RATIONALE", "rationale"));
		contextImpact.addChild(FLEFRecord.createChildWithTagAndValue("CONFIDENCE", "medium"));

		final FLEFRecord source1 = FLEFRecord.createMainRecord("S1", SourceHandler.TYPE);
		source1.addChild(FLEFRecord.createChildWithTag("TITLE")
			.addChild(FLEFRecord.createChildWithTagAndValue("VALUE", "Historical study")));

		final Consumer<FLEFModel> modelFiller = model -> {
			model.addRecord(culturalNorm);
			model.addRecord(individual);
			model.addRecord(contextImpact);
			model.addRecord(source1);
		};
		GUIHelper.launch(ContextImpactRecordDialog::createEdit, modelFiller, contextImpact);
	}


	public static void main2(final String[] args) throws IOException{
		GUIHelper.launch(ConclusionRecordDialog::createEdit, "/tests/test.flef", "CI1");
	}

}
