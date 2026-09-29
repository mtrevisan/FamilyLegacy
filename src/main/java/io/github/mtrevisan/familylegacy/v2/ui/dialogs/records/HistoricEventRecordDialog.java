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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Structure:
 * <pre>
 * record HistoricEventRecord {
 *   id: LocalID
 *   type?: enum { war, epidemic, famine, migration, legal_reform, political_change, territorial_change, natural_disaster, economic_crisis, scientific_discovery, religious_reform, social_movement, pandemic } | Text
 *   title?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): type, title, date, place, evidence
 * Tab 5 (Context): ContextImpactRecord (context[historic_event] = this historic event)
 * Tab 6 (Research): ConclusionRecord (resolves = this historic event), ResearchQuestionRecord (target[historic_event] = this historic event)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 10 (Audit): audit
 */
public class HistoricEventRecordDialog extends BaseRecordDialog{

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField titleField;
	private final DateField dateField;
	private final EntityField placeField;


	public static HistoricEventRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, HistoricEventRecordDialog::new);
	}

	public static HistoricEventRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, HistoricEventRecordDialog::new);
	}


	private HistoricEventRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HistoricEventHandler.getInstance());

		typeCombo = new BoundComboBox<>(HistoricEventHandler.TAG_TYPE, HistoricEventHandler.TYPES);
		typeCombo.setEditable(true);
		titleField = new BoundTextField(HistoricEventHandler.TAG_TITLE);
		dateField = DateField.createWithWrapperTag(HistoricEventHandler.TAG_DATE, this, "Date", model);
		placeField = EntityField.createForStructureWithReference(PlaceHandler.TYPE, this, model, PlaceCitationHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impact"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, HistoricEventHandler.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, HistoricEventHandler.TAG_NOTE, null)
			.withComponent(PanelKey.EVIDENCE, HistoricEventHandler.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.AUDIT, HistoricEventHandler.TAG_AUDIT, null)
			.build();

		components.bind(typeCombo);
		components.bind(titleField);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]");

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic.event.type") + ":", typeCombo);

		// title
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic.event.title") + ":", titleField);

		// date
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic.event.date") + ":", dateField);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.place") + ":", placeField);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT_ON_CONTEXT);
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
	protected void loadData(){
		components.load(record);

		dateField.load(record);
		placeField.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected void saveData(){
		components.save(record);

		dateField.save(record);
		placeField.saveReferences(record);
	}


	public static void main(final String[] args) throws IOException{
		GUIHelper.launch(HistoricEventRecordDialog::createEdit, "/tests/test.flef", "HE1");
	}

}
