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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;


/**
 * Structure:
 * <pre>
 * record HistoricEventRecord {
 *   type?: enum { war, epidemic, famine, migration, legal_reform, political_change, territorial_change, natural_disaster, economic_crisis, scientific_discovery, religious_reform, social_movement, pandemic } | Text
 *   title?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   confidence?: Confidence
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): type, title, date, place, confidence
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
	private final BoundComboBox<String> confidenceCombo;


	public static HistoricEventRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, HistoricEventRecordDialog::new);
	}

	public static HistoricEventRecordDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, HistoricEventRecordDialog::new);
	}


	private HistoricEventRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HistoricEventHandler.getInstance());

		typeCombo = new BoundComboBox<>(HistoricEventReader.TAG_TYPE, GUIHelper.fillCombo(HistoricEventReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.historic_event.type");
		typeCombo.setEditable(true);
		titleField = new BoundTextField(HistoricEventReader.TAG_TITLE);
		dateField = DateField.createWithWrapperTag(HistoricEventReader.TAG_DATE, this, "Date", model);
		placeField = EntityField.createForStructureWithReference(HistoricEventReader.TAG_PLACE, this, model, PlaceCitationHandler.class);
		confidenceCombo = new BoundComboBox<>(HistoricEventReader.TAG_CONFIDENCE, GUIHelper.fillCombo(ConfidenceReader.CONFIDENCES, null));
		confidenceCombo.setI18NPrefix("enum.confidence");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, ContextImpactHandler.TYPE, I18N.t("dialog.component.context.impacts"))
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, ConclusionHandler.TYPE, I18N.t("dialog.component.conclusions"))
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, ResearchQuestionHandler.TYPE, I18N.t("dialog.component.research.questions"))
			.withComponent(PanelKey.SOURCE, HistoricEventReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.NOTE, HistoricEventReader.TAG_NOTE, null)
			.withComponent(PanelKey.AUDIT, HistoricEventReader.TAG_AUDIT, null)
			.build();

		components.bind(typeCombo);
		components.bind(titleField);
		components.bind(confidenceCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	/**
	 * Fills the editor with what the Context Assistant suggests. Nothing is saved unless the user confirms the editor.
	 *
	 * @param type	The type of historic event (can be {@code null}).
	 * @param title	The title, used only where still empty (can be {@code null}).
	 * @return	This dialog.
	 */
	public HistoricEventRecordDialog withDraft(final String type, final String title){
		if(type != null)
			typeCombo.setSelectedItem(type);
		if(title != null && !title.isBlank() && titleField.isEmpty())
			titleField.setText(title);

		return this;
	}

	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]");

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic_event.type") + ":", typeCombo);

		// title
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic_event.title") + ":", titleField);

		// date
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic_event.date") + ":", dateField);

		// place
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic_event.place") + ":", placeField);

		// confidence
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.historic_event.confidence") + ":", confidenceCombo);

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
