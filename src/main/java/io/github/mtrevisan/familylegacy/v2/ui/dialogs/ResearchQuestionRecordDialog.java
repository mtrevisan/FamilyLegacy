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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ResearchQuestionStatusPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dialog;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;


/* DONE */
/**
 * Dialog for editing a {@code RESEARCH_QUESTION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchQuestionRecord {
 *   id: LocalID
 *   title: Text
 *   question: Text
 *   target*: ResearchTarget
 *   status: enum { open, on_hold, resolved, disproven }
 *   conclusion?: Text
 *   conclusion_confidence?: enum { low, medium, high }
 *   rationale?: Text
 *   created: Date
 *   closed?: Date
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 *
 * ResearchTarget = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   event: Xref&lt;EventRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   source: Xref&lt;SourceRecord&gt;
 *   document: Xref&lt;DocumentRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 *   void: struct {}
 * }
 * </pre>
 */
public class ResearchQuestionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4693851314612375503L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_CONCLUSION = "CONCLUSION";
	private static final String TAG_CONCLUSION_CONFIDENCE = "CONCLUSION_CONFIDENCE";
	private static final String TAG_RATIONALE = "RATIONALE";
	private static final String TAG_CREATED = "CREATED";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new ResearchQuestionHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new EventParticipationHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new PlaceRelationshipHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new DocumentHandler());
		HandlerRegistry.register(new IdentityHypothesisHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new HistoricEventHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]10[]10[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField;
	private final BoundTextArea questionArea;
	private final ParticipantField targetField;
	private final ResearchQuestionStatusPanel statusPanel;
	private final BoundTextArea conclusionArea;
	private final BoundComboBox<String> conclusionConfidenceCombo;
	private final BoundTextArea rationaleArea;
	private final BoundTextField createdField;
	private final RestrictionPanel privacyPanel;
	private final ModificationPanel auditPanel;


	public static ResearchQuestionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ResearchQuestionRecordDialog::new);
	}

	public static ResearchQuestionRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ResearchQuestionRecordDialog::new);
	}


	private ResearchQuestionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchQuestionHandler.TYPE));

		titleField = new BoundTextField(TAG_TITLE);
		questionArea = new BoundTextArea(TAG_QUESTION, 3, 30);
		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE, EventHandler.TYPE,
			EventParticipationHandler.TYPE, RelationshipHandler.TYPE, IndividualAttributeHandler.TYPE,
			GroupAttributeHandler.TYPE, PlaceRelationshipHandler.TYPE, SourceHandler.TYPE, DocumentHandler.TYPE,
			IdentityHypothesisHandler.TYPE, CulturalNormHandler.TYPE, HistoricEventHandler.TYPE));
		statusPanel = new ResearchQuestionStatusPanel();
		conclusionArea = new BoundTextArea(TAG_CONCLUSION, 3, 30);
		conclusionConfidenceCombo = new BoundComboBox<>(TAG_CONCLUSION_CONFIDENCE, new String[]{
			StringUtils.EMPTY,
			"low", "medium", "high"});
		rationaleArea = new BoundTextArea(TAG_RATIONALE, 3, 30);
		createdField = new BoundTextField(TAG_CREATED);
		createdField.setEditable(false);
		privacyPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		auditPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(titleField);
		bindingManager.bind(questionArea);
		bindingManager.bind(conclusionArea);
		bindingManager.bind(conclusionConfidenceCombo);
		bindingManager.bind(rationaleArea);
		bindingManager.bind(createdField);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Privacy", privacyPanel);
		tabbedPane.addTab("Audit", auditPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// title
		mainPanel.add(new JLabel("Title*:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// question
		mainPanel.add(new JLabel("Question*:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(questionArea), "growx,wrap");

		// target
		mainPanel.add(new JLabel("Target:"), "align label");
		mainPanel.add(targetField, "growx,wrap");

		// status
		mainPanel.add(new JLabel("Status*:"), "align label");
		mainPanel.add(statusPanel, "growx,wrap");

		// conclusion panel
		final JPanel conclusionPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
		conclusionPanel.setBorder(BorderFactory.createTitledBorder("Conclusion"));
		// conclusion
		conclusionPanel.add(GUIHelper.createScrollPane(conclusionArea), "span 2,growx,wrap");
		// confidence
		conclusionPanel.add(new JLabel("Confidence:"), "align label");
		conclusionPanel.add(conclusionConfidenceCombo, "growx");
		mainPanel.add(conclusionPanel, "span 2,growx,wrap");

		// rationale
		mainPanel.add(new JLabel("Rationale:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(rationaleArea), "growx,wrap");

		return mainPanel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		targetField.load(record);
		statusPanel.load(record);
		privacyPanel.load(record);
		auditPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(titleField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Title is required.",
				tabbedPane, mainPanel, titleField);
			return false;
		}

		if(questionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Question is required.",
				tabbedPane, mainPanel, questionArea);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		if(createdField.isEmpty()){
			final String creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
			createdField.setText(creationDate);
		}

		bindingManager.save(record);

		targetField.saveReferences(record);
		statusPanel.save(record);
		privacyPanel.save(record);
		auditPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ResearchQuestionRecordDialog::createNew);
	}

}
