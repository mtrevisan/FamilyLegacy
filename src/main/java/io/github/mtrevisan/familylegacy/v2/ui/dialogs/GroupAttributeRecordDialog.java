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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing a {@code GROUP_ATTRIBUTE_RECORD} according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * record GroupAttributeRecord {
 *   id: LocalID
 *   group: Xref&lt;GroupRecord&gt;
 *   type: enum {
 *     residence,
 *     children_count,
 *     social_class
 *   } | Text
 *   value?: Text
 *   date?: DateStructure
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
	private static final long serialVersionUID = -4670126000119212973L;


	private static final String TAG_GROUP = "GROUP";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]5[]5[]10[]"));

	private final String groupId;
	private final BoundComboBox<String> typeCombo;
	private final BoundTextField valueField;
	private final DateField dateField;
	private final DateField validFromField;
	private final DateField validToField;
	private final PlaceCitationField placeCitationField;
	private final SourceCitationListPanel sourceCitationPanel;
	private final EvidenceQualifiersPanel qualifiers;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler(GroupHandler.TYPE);


	public static GroupAttributeRecordDialog createNew(final Dialog parent, final FLEFModel model, final String groupId){
		return new GroupAttributeRecordDialog(parent, model, groupId, null);
	}

	public static GroupAttributeRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new GroupAttributeRecordDialog(parent, model, null, record);
	}


	private GroupAttributeRecordDialog(final Dialog parent, final FLEFModel model, final String groupId,
			final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupAttributeHandler.TYPE));

		this.groupId = (groupId != null
			? groupId
			: XRefHelper.extractXRef(FLEFRecordHelper.getChildValue(record, TAG_PLACE)));

		typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{StringUtils.EMPTY, "residence", "children_count", "social_class"});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE, 20);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Date", model);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, parent, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(valueField);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// type
		mainPanel.add(new JLabel("Type:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// value
		mainPanel.add(new JLabel("Name Value*:"), "align label");
		mainPanel.add(valueField, "growx, wrap");

		// date
		mainPanel.add(new JLabel("Date:"), "align label");
		mainPanel.add(dateField, "growx,wrap");

		// valid from date
		mainPanel.add(new JLabel("Valid From Date:"), "align label");
		mainPanel.add(validFromField, "growx,wrap");

		// valid to date
		mainPanel.add(new JLabel("Valid To Date:"), "align label");
		mainPanel.add(validToField, "growx,wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]"));
		panel.add(placeCitationField, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		if(StringUtils.isBlank(groupId)){
			JOptionPane.showMessageDialog(this, "Invalid Group ID: `" + groupId + "`.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		if(record == null)
			return;

		bindingManager.load(record);

		dateField.load(record);
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
		if(groupId == null || groupId.isEmpty()){
			JOptionPane.showMessageDialog(null,
				"GROUP is required for an attribute.\n" +
					"Please select a group record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(typeCombo.getSelectedItem() == null){
			GUIHelper.showValidationErrorAndFocus(this,
				"Type cannot be empty.",
				tabbedPane, mainPanel, typeCombo);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_GROUP, XRefHelper.formatXRef(groupId));

		bindingManager.save(record);

		dateField.save(record);
		validFromField.save(record);
		validToField.save(record);
		placeCitationField.save(record);
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
//			FLEFRecord groupAttribute = FLEFRecord.createEmpty();
//			groupAttribute.addChild(FLEFRecord.createChildWithValue(TAG_GROUP, "@G1@"));
//			final GroupAttributeRecordDialog dialog = GroupAttributeRecordDialog.createEdit(null, model, groupAttribute);
			final GroupAttributeRecordDialog dialog = GroupAttributeRecordDialog.createNew(null, model, "@G1@");
			dialog.setVisible(true);
		});
	}

}
