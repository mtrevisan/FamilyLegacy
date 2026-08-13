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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Dialog;
import java.io.Serial;
import java.util.function.Consumer;


/* ONGOING test */
/**
 * Dialog for editing a {@code CONCLUSION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ConclusionRecord {
 *   id: LocalID
 *   context: Text
 *   resolves*: ConclusionTarget
 *   preferred?: ConclusionTarget
 *   proof_status: enum { unresearched, conflicting_evidence, supported, proven, disproven }
 *   narrative?: Text
 *   research*: Xref&lt;ResearchQuestionRecord&gt;
 *   date?: Date
 *   source*: SourceCitation
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 *
 *   require preferred in resolves
 * }
 * </pre>
 */
public class ConclusionRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2667811782933374258L;


	public static final String PROPERTY_CONCLUSION = "conclusion";


	private static final String TAG_CONTEXT = "CONTEXT";
	private static final String TAG_RESOLVES = "RESOLVES";
	private static final String TAG_PREFERRED = "PREFERRED";
	private static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	private static final String TAG_NARRATIVE = "NARRATIVE";
	private static final String TAG_RESEARCH = "RESEARCH";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";


	static{
		HandlerRegistry.register(new ConclusionHandler());
		HandlerRegistry.register(new ConclusionTargetHandler());
		HandlerRegistry.register(new ResearchQuestionHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel propertiesPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField contextField;
	private final EntityReferenceListPanel resolvesPanel;
	private final BoundComboBox<FLEFRecord> preferredCombo;
	private final BoundComboBox<String> proofStatusCombo;
	private final BoundTextArea narrativeArea;
	private final EntityReferenceListPanel researchPanel;
	private final DateField dateField;
	private final EntityCitationListPanel sourcePanel;
	private final RestrictionPanel privacyPanel;
	private final ModificationPanel auditPanel;

	// Other
	private final EntityReferenceListPanel culturalNormPanel;

	private final RecordTypeHandler<?> conclusionTargetHandler = HandlerRegistry.getHandler(ConclusionTargetHandler.TYPE);


	public static ConclusionRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ConclusionRecordDialog::new);
	}

	public static ConclusionRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ConclusionRecordDialog::new);
	}


	private ConclusionRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ConclusionHandler.TYPE));

		contextField = new BoundTextField(TAG_CONTEXT);
		resolvesPanel = EntityReferenceListPanel.createForStructure(TAG_RESOLVES, this, "Resolves", model, ConclusionTargetHandler.TYPE);
		resolvesPanel.addPropertyChangeListener(PROPERTY_CONCLUSION, evt -> updatePreferredCombo());
		preferredCombo = new BoundComboBox<>(TAG_PREFERRED);
		preferredCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				final String displayText = (value != null
					? conclusionTargetHandler.getDisplayText((FLEFRecord)value, model)
					: null);
				setText(displayText);

				return this;
			}
		});
		proofStatusCombo = new BoundComboBox<>(TAG_PROOF_STATUS, new String[]{
			StringUtils.EMPTY,
			"unresearched", "conflicting_evidence", "supported", "proven", "disproven"
		});
		narrativeArea = new BoundTextArea(TAG_NARRATIVE, 5, 30);
		researchPanel = EntityReferenceListPanel.createForRecord(TAG_RESEARCH, this, "Research Questions", model, ResearchQuestionHandler.TYPE)
			.withParentEntity(this.record.getId(), ConclusionHandler.TYPE);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Conclusion Date", model);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);
		privacyPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		auditPanel = new ModificationPanel(this);

		culturalNormPanel = EntityReferenceListPanel.createForRecord(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE)
			.withParentEntity(this.record.getId(), ConclusionHandler.TYPE);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(contextField);
		bindingManager.bind(proofStatusCombo);
		bindingManager.bind(narrativeArea);


		tabbedPane.addTab("Properties", createPropertiesPanel());
		tabbedPane.addTab("Context", createContextPanel());
		tabbedPane.addTab("Research", createResearchPanel());
		tabbedPane.addTab("Sources", createSourcesPanel());
		tabbedPane.addTab("Privacy", privacyPanel);
		tabbedPane.addTab("Audit", auditPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createPropertiesPanel(){
		// context
		propertiesPanel.add(new JLabel("Context*:"), "align label");
		propertiesPanel.add(contextField, "growx,wrap");

		// resolves
		propertiesPanel.add(resolvesPanel, "span 2,growx,wrap");

		// preferred
		propertiesPanel.add(new JLabel("Preferred:"), "align label");
		propertiesPanel.add(preferredCombo, "growx,wrap");

		// proof status
		propertiesPanel.add(new JLabel("Proof Status*:"), "align label");
		propertiesPanel.add(proofStatusCombo, "growx,wrap");

		// narrative
		propertiesPanel.add(new JLabel("Narrative:"), "align label");
		propertiesPanel.add(GUIHelper.createScrollPane(narrativeArea), "growx,wrap");

		// date
		propertiesPanel.add(new JLabel("Date:"), "align label");
		propertiesPanel.add(dateField, "growx");

		return propertiesPanel;
	}

	private JPanel createResearchPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]"));

		// research
		panel.add(researchPanel, "span 2,growx,wrap");

		return panel;
	}

	private JPanel createSourcesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(sourcePanel, "growx");
		return panel;
	}

	//TODO ContextImpactRecord (target.conclusion = this conclusion)
	private JPanel createContextPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(culturalNormPanel, "growx");
		return panel;
	}

	private void updatePreferredCombo(){
		final FLEFRecord currentSelection = (FLEFRecord)preferredCombo.getSelectedItem();
		final DefaultComboBoxModel<FLEFRecord> model = new DefaultComboBoxModel<>();
		model.addElement(null);
		for(final FLEFRecord resolve : resolvesPanel.getItems())
			model.addElement(resolve);
		preferredCombo.removeAllItems();
		preferredCombo.setModel(model);

		if(currentSelection != null && !currentSelection.isEmpty())
			preferredCombo.setSelectedItem(currentSelection);
	}

	@Override
	protected void loadData(){
		contextField.setText(FLEFRecordHelper.getChildValue(record, TAG_CONTEXT));
		proofStatusCombo.setSelectedItem(FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS));
		narrativeArea.setText(FLEFRecordHelper.getChildValue(record, TAG_NARRATIVE));

		dateField.load(record);
		resolvesPanel.load(record);
		updatePreferredCombo();

		// preferred
		final String prefRef = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED);
		if(StringUtils.isNotEmpty(prefRef)){
			// Find and select in combo
			final FLEFRecord pref = model.getRecordById(prefRef);
			preferredCombo.setSelectedItem(pref);
			for(int i = 0; i < preferredCombo.getItemCount(); i ++){
				final FLEFRecord item = preferredCombo.getItemAt(i);
				if(item != null && item.getValue().equals(prefRef))
					preferredCombo.setSelectedIndex(i);
			}
		}

		researchPanel.load(record);
		sourcePanel.load(record);
		privacyPanel.load(record);
		auditPanel.load(record);
	}

	@Override
	protected boolean validData(){
		String context = contextField.getText();
		if(context.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Context is required.",
				tabbedPane, propertiesPanel, contextField);
			return false;
		}

		String proofStatus = (String)proofStatusCombo.getSelectedItem();
		if(StringUtils.isEmpty(proofStatus)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Proof status is required.",
				tabbedPane, propertiesPanel, proofStatusCombo);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		dateField.save(record);
		resolvesPanel.save(record);

		// preferred
		FLEFRecordHelper.removeChildren(record, TAG_PREFERRED);
		final FLEFRecord selectedPreferred = (FLEFRecord)preferredCombo.getSelectedItem();
		if(selectedPreferred != null)
			FLEFRecordHelper.updateChildValue(record, TAG_PREFERRED, XRefHelper.formatXRef(selectedPreferred.getValue()));

		researchPanel.save(record);
		sourcePanel.save(record);
		privacyPanel.save(record);
		auditPanel.save(record);
	}


	public static void main(final String[] args){
//		GUIHelper.launch(ConclusionRecordDialog::createNew);

		final FLEFRecord conclusion = FLEFRecord.createMainRecord("CC1", "CONCLUSION");
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("CONTEXT", "fdgh"));
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("PLACE", "@P1@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTag("RESOLVES")
			.addChild(FLEFRecord.createChildWithTagAndValue("PLACE", "@P2@"))
		);
		conclusion.addChild(FLEFRecord.createChildWithTagAndValue("PROOF_STATUS", "PROOF_STATUS"));
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
		GUIHelper.launch(ConclusionRecordDialog::createEdit, modelFiller, conclusion);
	}

}
