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
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/* DONE */
/**
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record CulturalNormRecord {
 *   id: LocalID
 *   title?: Text
 *   rule_type?: enum {
 *     age_of_majority, marriage_minimum_age, baptism_age, confirmation_age, military_service_age, retirement_age,
 *     naming_convention, surname_transmission, patronymic_system, matronymic_system, title_usage,
 *     inheritance_rule, succession_rule, dowry_practice, guardianship_rule, adoption_practice,
 *     marriage_practice, marriage_prohibited_degree, widowhood_rule,
 *     residence_pattern, household_structure, social_classification,
 *     religious_practice, burial_practice,
 *     citizenship_rule, legitimacy_rule,
 *     age_difference_convention, generational_interval
 *   } | Text
 *   place?: PlaceCitation
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
 * Tab 1 (Properties): title, rule_type, place, valid_from, valid_to, evidence
 * Tab 5 (Context): ContextImpactRecord (context.cultural_norm = this norm)
 * Tab 6 (Research): ConclusionRecord (resolves/preferred = this norm), ResearchQuestionRecord (target.cultural_norm = this norm)
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 * Tab 10 (Audit): audit
 */
public class CulturalNormRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 950729006569948384L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_RULE_TYPE = "RULE_TYPE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_AUDIT = "AUDIT";

	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_RESEARCH_QUESTION = "RESEARCH_QUESTION";


	private final RecordDialogComponents components;

	private final BoundTextField titleField;
	private final BoundComboBox<String> ruleTypeCombo;
	private final PlaceCitationField placeCitationField;
	private final EvidenceQualifiersPanel placeEvidencePanel;
	private final DateField validFromField;
	private final DateField validToField;


	public static CulturalNormRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, CulturalNormRecordDialog::new);
	}

	public static CulturalNormRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, CulturalNormRecordDialog::new);
	}


	private CulturalNormRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, CulturalNormHandler.class);

		titleField = new BoundTextField(TAG_TITLE);
		ruleTypeCombo = new BoundComboBox<>(TAG_RULE_TYPE, new String[]{
			StringUtils.EMPTY,
			// Lifecycle and age-related customs:
			"age_of_majority", "marriage_minimum_age", "baptism_age", "confirmation_age", "military_service_age",
			"retirement_age",
			// Naming practices:
			"naming_convention", "surname_transmission", "patronymic_system", "matronymic_system", "title_usage",
			// Family and household customs:
			"inheritance_rule", "succession_rule", "dowry_practice", "guardianship_rule", "adoption_practice",
			// Marriage customs:
			"marriage_practice", "marriage_prohibited_degree", "widowhood_rule",
			// Residence and social organization:
			"residence_pattern", "household_structure", "social_classification",
			// Religious and ecclesiastical customs:
			"religious_practice", "burial_practice",
			// Legal and citizenship rules:
			"citizenship_rule", "legitimacy_rule",
			// Genealogical inference rules:
			"age_difference_convention", "generational_interval"
		});
		ruleTypeCombo.setEditable(true);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		placeEvidencePanel = new EvidenceQualifiersPanel(TAG_PLACE, this, "Evidence", model, null);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, CulturalNormHandler.class)
			.withComponent(PanelKey.CONCLUSION_ON_RESOLVES, TAG_CONCLUSION, "Conclusions", ConclusionHandler.class, CulturalNormHandler.class)
			.withComponent(PanelKey.RESEARCH_QUESTION_ON_TARGET, TAG_RESEARCH_QUESTION, "Research Questions", ResearchQuestionHandler.class, CulturalNormHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, CulturalNormHandler.class)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(titleField);
		components.bind(ruleTypeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]5[]10[]10[]10[]");

		// title
		GUIHelper.addLabeledComponent(propertiesPanel, "Title:", titleField);

		// rule type
		GUIHelper.addLabeledComponent(propertiesPanel, "Rule Type:", ruleTypeCombo);

		// place panel:
		final JPanel placePanel = GUIHelper.createLabelFieldPanel(10, "[]5[]");
		placePanel.setBorder(new TitledBorder("Place with Citation"));
		GUIHelper.addComponent(placePanel, placeCitationField);
		GUIHelper.addComponent(placePanel, placeEvidencePanel);
		GUIHelper.addComponent(propertiesPanel, placePanel);

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

		placeCitationField.load(record);
		placeEvidencePanel.load(record);
		validFromField.load(record);
		validToField.load(record);
	}

	@Override
	protected void saveData(){
		components.save(record);

		placeCitationField.saveReferences(record);
		placeEvidencePanel.save(record);
		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args) throws IOException{
		try(final InputStream is = CulturalNormRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(CulturalNormRecordDialog::createEdit, content, "CN1");
		}
	}

}
