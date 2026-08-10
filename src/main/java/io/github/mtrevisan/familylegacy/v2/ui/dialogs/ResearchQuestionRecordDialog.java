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
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.AssociationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code RESEARCH_STATUS_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchStatusRecord {
 *   id: LocalID
 *   question: Text
 *   status?: enum { active, completed, blocked }
 *   priority?: enum { high, medium, low }
 *   association*: struct {
 *     target: ResearchTarget
 *     name?: Text
 *   }
 *   blocked_by*: Xref&lt;ResearchStatusRecord&gt;
 *   plan?: Text
 *   resolution?: Text
 *   modification: ModificationStructure
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
 *   void: struct { }
 * }
 * </pre>
 */
public class ResearchQuestionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4693851314612375503L;


	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_PRIORITY = "PRIORITY";
	private static final String TAG_ASSOCIATION = "ASSOCIATION";
	private static final String TAG_BLOCKED_BY = "BLOCKED_BY";
	private static final String TAG_PLAN = "PLAN";
	private static final String TAG_RESOLUTION = "RESOLUTION";


	static{
		HandlerRegistry.register(new ResearchQuestionHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]10[]10[]"));
	private final JPanel referencesPanel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextArea questionArea;
	private final BoundComboBox<String> statusCombo;
	private final BoundComboBox<String> priorityCombo;
	private final AssociationListPanel associationPanel;
	private final BoundTextArea planArea;
	private final BoundTextArea resolutionArea;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static ResearchQuestionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new ResearchQuestionRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static ResearchQuestionRecordDialog createEdit(final Dialog parent, final FLEFModel model,
		final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new ResearchQuestionRecordDialog(parent, model, record);
	}


	private ResearchQuestionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ResearchQuestionHandler.TYPE));

		questionArea = new BoundTextArea(TAG_QUESTION, 3, 30);
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{StringUtils.EMPTY,
			"active", "completed", "blocked"});
		priorityCombo = new BoundComboBox<>(TAG_PRIORITY, new String[]{StringUtils.EMPTY,
			"high", "medium", "low"});
		associationPanel = new AssociationListPanel(TAG_ASSOCIATION, this, model);
		planArea = new BoundTextArea(TAG_PLAN, 3, 30);
		resolutionArea = new BoundTextArea(TAG_RESOLUTION, 3, 30);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(questionArea);
		bindingManager.bind(statusCombo);
		bindingManager.bind(priorityCombo);
		bindingManager.bind(planArea);
		bindingManager.bind(resolutionArea);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// question
		mainPanel.add(new JLabel("Question*:"), "align label");
		mainPanel.add(questionArea, "growx,wrap");

		// status
		mainPanel.add(new JLabel("Status:"), "align label");
		mainPanel.add(statusCombo, "growx,wrap");

		// priority
		mainPanel.add(new JLabel("Priority:"), "align label");
		mainPanel.add(priorityCombo, "growx,wrap");

		// plan
		mainPanel.add(new JLabel("Plan:"), "align label");
		mainPanel.add(planArea, "growx,wrap");

		// resolution
		mainPanel.add(new JLabel("Resolution:"), "align label");
		mainPanel.add(resolutionArea, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		referencesPanel.add(associationPanel, "growx");
		return referencesPanel;
	}

	@Override
	protected void loadData(){
		bindingManager.load(record);

		associationPanel.load(record);

		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(questionArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Question is required.",
				tabbedPane, mainPanel, questionArea);
			return false;
		}

		// Check for circular blocked_by references (basic check)
		// A more thorough check would require full graph analysis
//		final String currentId = (record != null? record.getId(): null);
//		if(StringUtils.isNotEmpty(currentId))
//			for(String blockedId : blockedByPanel.getBlockedByIds())
//				if(currentId.equals(blockedId)){
//					GUIHelper.showValidationErrorAndFocus(this,
//						"A research status cannot be blocked by itself.",
//						tabbedPane, referencesPanel, blockedByPanel);
//
//					return false;
//				}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		associationPanel.save(record);

		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final ResearchQuestionRecordDialog dialog = ResearchQuestionRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
