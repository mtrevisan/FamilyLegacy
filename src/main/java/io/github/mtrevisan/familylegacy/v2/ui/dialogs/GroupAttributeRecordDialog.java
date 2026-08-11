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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code GROUP_ATTRIBUTE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupAttributeRecord {
 *   id: LocalID
 *   group: Xref&lt;GroupRecord&gt;
 *   type: enum {
 *     residence,
 *     member_count,
 *     children_count,
 *     social_class,
 *     ethnicity,
 *     religion,
 *     language,
 *     wealth,
 *     land_holding,
 *     primary_income_source
 *   } | Text
 *   value?: Text
 *   valid_on?: DateStructure
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   place?: PlaceCitation
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class GroupAttributeRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -5939902730413020982L;


	private static final String TAG_GROUP = "GROUP";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_VALID_ON = "VALID_ON";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new GroupAttributeHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final DateField validOnField;
	private final DateField validFromField;
	private final DateField validToField;
	private final PlaceCitationField placeCitationField;
	private final SourceCitationListPanel sourceCitationPanel;
	private final EvidenceQualifiersPanel qualifiers;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static GroupAttributeRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, GroupAttributeRecordDialog::new);
	}

	public static GroupAttributeRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, GroupAttributeRecordDialog::new);
	}


	private GroupAttributeRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupAttributeHandler.TYPE));

		typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{StringUtils.EMPTY, "residence", "member_count", "children_count", "social_class", "ethnicity", "religion", "language", "wealth", "land_holding", "primary_income_source"});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE, 20);
		validOnField = DateField.createWithWrapperTag(TAG_VALID_ON, this, "Valid On", model);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "Valid From", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "Valid To", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, "Sources", model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(parentEntity);
		bindingManager.bind(typeCombo);
		bindingManager.bind(valueField);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// type
		mainPanel.add(new JLabel("Type*:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// value
		mainPanel.add(new JLabel("Value:"), "align label");
		mainPanel.add(valueField, "growx, wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid on
		validityPanel.add(new JLabel("Valid On:"), "align label");
		validityPanel.add(validOnField, "growx,wrap");
		// valid from
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx,wrap");
		// valid to
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx");
		mainPanel.add(validityPanel, "span 2,growx,wrap");

		// place
		mainPanel.add(new JLabel("Place:"), "align label");
		mainPanel.add(placeCitationField, "growx,wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]"));
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	public void setGroup(final String groupId){
		if(StringUtils.isNotEmpty(groupId)){
			if(!confirmRecordExistsForType(groupId, GroupHandler.TYPE))
				return;

			parentEntity.setText(groupId);

			refreshLayout();
		}
	}

	private void refreshLayout(){
		mainPanel.revalidate();
		mainPanel.repaint();

		pack();
	}


	@Override
	protected void loadData(){
		if(record == null)
			return;

		bindingManager.load(record);

		validOnField.load(record);
		validFromField.load(record);
		validToField.load(record);
		placeCitationField.load(record);
		sourceCitationPanel.load(record);
		qualifiers.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(parentEntity.isEmpty()){
			JOptionPane.showMessageDialog(null,
				"Parent Group is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(StringUtils.isEmpty((String)typeCombo.getSelectedItem())){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type cannot be empty.",
				tabbedPane, mainPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		validOnField.save(record);
		validFromField.save(record);
		validToField.save(record);
		placeCitationField.saveReferences(record);
		sourceCitationPanel.save(record);
		qualifiers.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord group = FLEFRecord.createMainRecord("G1", TAG_GROUP);
			model.addRecord(group);

//			final FLEFRecord groupAttribute = FLEFRecord.createEmpty();
//			groupAttribute.addChild(FLEFRecord.createChildWithValue(TAG_GROUP, "G1"));
//			final GroupAttributeRecordDialog dialog = GroupAttributeRecordDialog.createEdit(null, model, groupAttribute);
			final GroupAttributeRecordDialog dialog = GroupAttributeRecordDialog.createNew(null, model);
			dialog.setGroup("G1");
			dialog.setVisible(true);
		});
	}

}
