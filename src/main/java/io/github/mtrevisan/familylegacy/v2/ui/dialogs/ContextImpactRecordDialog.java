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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/**
 * Dialog for editing a {@code CONTEXT_IMPACT_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ContextImpactRecord {
 *   id: LocalID
 *   context: ContextSource
 *   target: ImpactTarget
 *   impact_type?: enum { explains, influences, constrains, motivates, causes } | Text
 *   significance?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): context, target, impact_type, significance, evidence
 * Tab 7 (Sources): source
 * Tab 10 (Audit): audit
 */
public class ContextImpactRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2171561180026896666L;


	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_IMPACT_TYPE = "IMPACT_TYPE";
	private static final String TAG_SIGNIFICANCE = "SIGNIFICANCE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final ParticipantField contextField;
	private final ParticipantField targetField;
	private final BoundComboBox<String> impactTypeCombo;
	private final BoundTextArea significanceArea;


	private final JPanel propertiesPanel;


	public static ContextImpactRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ContextImpactRecordDialog::new);
	}

	public static ContextImpactRecordDialog createEdit(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		return createEdit(parent, model, record, ContextImpactRecordDialog::new);
	}


	private ContextImpactRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ContextImpactHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]");

		contextField = ParticipantField.create(TAG_CONTEXT, this, model);
		contextField.setHandlerTypes(CulturalNormHandler.class, HistoricEventHandler.class);

		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(IndividualHandler.class, GroupHandler.class, PlaceHandler.class, EventHandler.class,
			RelationshipHandler.class, IndividualAttributeHandler.class, GroupAttributeHandler.class,
			ConclusionHandler.class, EventParticipationHandler.class, PlaceRelationshipHandler.class,
			IdentityHypothesisHandler.class);

		impactTypeCombo = new BoundComboBox<>(TAG_IMPACT_TYPE, new String[]{
			StringUtils.EMPTY,
			"explains", "influences", "constrains", "motivates", "causes"
		});
		impactTypeCombo.setEditable(true);

		significanceArea = new BoundTextArea(TAG_SIGNIFICANCE, 3, 30);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, IndividualAttributeHandler.class)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(impactTypeCombo);
		components.bind(significanceArea);

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// context
		GUIHelper.addLabeledComponent(propertiesPanel, "Context*:", contextField);

		// target
		GUIHelper.addLabeledComponent(propertiesPanel, "Target*:", targetField);

		// impact type
		GUIHelper.addLabeledComponent(propertiesPanel, "Impact Type:", impactTypeCombo);

		// significance
		GUIHelper.addLabeledComponent(propertiesPanel, "Significance:", significanceArea);

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
	}

	@Override
	protected boolean validData(){
		if(!contextField.hasData()){
			JOptionPane.showMessageDialog(this,
				"Context is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(!targetField.hasData()){
			JOptionPane.showMessageDialog(this,
				"Target is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

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
		final FLEFRecord culturalNorm = FLEFRecord.createMainRecord("CN1", "CULTURAL_NORM");
		culturalNorm.addChild(FLEFRecord.createChildWithTagAndValue("NAME", "Primogeniture"));

		final FLEFRecord individual = FLEFRecord.createMainRecord("I1", "INDIVIDUAL");
		individual.addChild(FLEFRecord.createChildWithTagAndValue("SEX", "male"));

		final FLEFRecord contextImpact = FLEFRecord.createMainRecord("CI1", "CONTEXT_IMPACT");
		contextImpact.addChild(FLEFRecord.createChildWithTag("CONTEXT")
			.addChild(FLEFRecord.createChildWithTagAndValue("CULTURAL_NORM", "@CN1@")));
		contextImpact.addChild(FLEFRecord.createChildWithTag("TARGET")
			.addChild(FLEFRecord.createChildWithTagAndValue("INDIVIDUAL", "@I1@")));
		contextImpact.addChild(FLEFRecord.createChildWithTagAndValue("IMPACT_TYPE", "constrains"));
		contextImpact.addChild(FLEFRecord.createChildWithTagAndValue("SIGNIFICANCE", "Inheritance limited to eldest son"));

		final FLEFRecord source1 = FLEFRecord.createMainRecord("S1", "SOURCE");
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

}
