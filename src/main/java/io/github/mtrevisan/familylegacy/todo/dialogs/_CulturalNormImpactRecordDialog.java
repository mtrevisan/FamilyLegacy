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
package io.github.mtrevisan.familylegacy.todo.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code CulturalNormImpactRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record CulturalNormImpactRecord {
 *   id: LocalID
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   target: CulturalNormImpactTarget
 *   significance?: Text
 *   restriction?: RestrictionStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public class _CulturalNormImpactRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -6765752765100985186L;

	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_SIGNIFICANCE = "SIGNIFICANCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";

	static{
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new ConclusionHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	// Cultural Norm selection
	private final JTextField culturalNormField;
	private final JButton culturalNormButton;

	// Target selection
//	private final MultiTypeSelectionDialog targetField;

	// Significance
	private final JTextArea significanceArea;

	// Restriction and Modification
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Handler for cultural norm
	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();


	public static _CulturalNormImpactRecordDialog createNew(Dialog parent, FLEFModel model){
		return createNew(parent, model, _CulturalNormImpactRecordDialog::new);
	}

	public static _CulturalNormImpactRecordDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		return createEdit(parent, model, record, _CulturalNormImpactRecordDialog::new);
	}


	private _CulturalNormImpactRecordDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(_CulturalNormImpactHandler.TYPE));

		// Cultural norm field
		culturalNormField = new JTextField(null);
		culturalNormField.setEditable(false);
		culturalNormButton = new JButton("…");
		culturalNormButton.setToolTipText("Select a Cultural Norm record");

		// Target field
//		targetField = new MultiTypeSelectionDialog(parent, model, List.of(
//			new TargetTypeDescriptor(IndividualHandler.TYPE, "Individual", new IndividualHandler()),
//			new TargetTypeDescriptor(GroupHandler.TYPE, "Group", new GroupHandler()),
//			new TargetTypeDescriptor(EventHandler.TYPE, "Event", new EventHandler()),
//			new TargetTypeDescriptor(RelationshipHandler.TYPE, "Relationship", new RelationshipHandler()),
//			new TargetTypeDescriptor(IndividualAttributeHandler.TYPE, "Individual Attribute", new IndividualAttributeHandler()),
//			new TargetTypeDescriptor(GroupAttributeHandler.TYPE, "Group Attribute", new GroupAttributeHandler()),
//			new TargetTypeDescriptor(ConclusionHandler.TYPE, "Conclusion", new ConclusionHandler())
//		));

		// Significance
		significanceArea = new JTextArea(3, 25);
		significanceArea.setLineWrap(true);
		significanceArea.setWrapStyleWord(true);

		// Restriction and Modification
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		// Listeners
		culturalNormButton.addActionListener(e -> selectCulturalNorm());


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
//		bindingManager.bind(significanceArea);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Audit", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// Cultural Norm
		final JPanel normPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow][shrink 0]"));
		normPanel.add(culturalNormField, "growx");
		normPanel.add(culturalNormButton, "width 30!");
		mainPanel.add(new JLabel("Cultural Norm*:"), "align label");
		mainPanel.add(normPanel, "growx,wrap");

		// Target
//		mainPanel.add(new JLabel("Target*:"), "align label");
//		mainPanel.add(targetField, "growx,wrap");

		// Significance
		mainPanel.add(new JLabel("Significance:"), "align label");
		mainPanel.add(GUIHelper.createScrollPane(significanceArea), "growx,wrap");

		return mainPanel;
	}

	private void selectCulturalNorm(){
		_GenericSelectionDialog<?> dialog = new _GenericSelectionDialog<>(
			this, model, culturalNormHandler, selectedRecord -> {
			if(selectedRecord != null){
				culturalNormField.setText(culturalNormHandler.getDisplayText(selectedRecord, model));
				culturalNormField.putClientProperty("selectedId", selectedRecord.getId());
			}
		});
		dialog.setVisible(true);
	}

	// ------------------------------------------------------------------------
	// Load / Save
	// ------------------------------------------------------------------------

	@Override
	protected void loadData(){
		// Load cultural norm
		String normRef = FLEFRecordHelper.getChildValue(record, TAG_CULTURAL_NORM);
		if(StringUtils.isNotEmpty(normRef)){
			FLEFRecord norm = model.getRecordById(normRef);
			if(norm != null){
				culturalNormField.setText(culturalNormHandler.getDisplayText(norm, model));
				culturalNormField.putClientProperty("selectedId", normRef);
			}
			else{
				culturalNormField.setText(normRef);
				culturalNormField.putClientProperty("selectedId", normRef);
			}
		}
		else{
			culturalNormField.setText(StringUtils.EMPTY);
			culturalNormField.putClientProperty("selectedId", null);
		}

		// Load target
//		targetField.load(record, TAG_TARGET);

		// Load significance
		String sig = FLEFRecordHelper.getChildValue(record, TAG_SIGNIFICANCE);
		significanceArea.setText(StringUtils.defaultString(sig));

		// Load restriction and modification
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Cultural norm required
		String normId = (String)culturalNormField.getClientProperty("selectedId");
		if(StringUtils.isEmpty(normId)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Cultural Norm is required.",
				tabbedPane, mainPanel, culturalNormField);
			return false;
		}

		// Target required
//		if(!targetField.hasData()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Target is required.",
//				tabbedPane, mainPanel, targetField);
//			return false;
//		}

		return true;
	}

	@Override
	protected void saveData(){
		// Clear existing children to avoid duplication
		FLEFRecordHelper.removeChildren(record, TAG_CULTURAL_NORM);
		FLEFRecordHelper.removeChildren(record, TAG_TARGET);
		FLEFRecordHelper.removeChildren(record, TAG_SIGNIFICANCE);

		// Save cultural norm
		String normId = (String)culturalNormField.getClientProperty("selectedId");
		if(StringUtils.isNotEmpty(normId)){
			FLEFRecordHelper.updateChildValue(record, TAG_CULTURAL_NORM, XRefHelper.formatXRef(normId));
		}

		// Save target
//		targetField.save(record, TAG_TARGET);

		// Save significance
		String sig = significanceArea.getText().trim();
		if(StringUtils.isNotEmpty(sig)){
			FLEFRecordHelper.updateChildValue(record, TAG_SIGNIFICANCE, sig);
		}

		// Save restriction and modification
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}

	// ------------------------------------------------------------------------
	// Main for testing
	// ------------------------------------------------------------------------

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}
		SwingUtilities.invokeLater(() -> {
			FLEFModel model = new FLEFModel();
			// Add dummy records if needed
			_CulturalNormImpactRecordDialog dialog = _CulturalNormImpactRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
