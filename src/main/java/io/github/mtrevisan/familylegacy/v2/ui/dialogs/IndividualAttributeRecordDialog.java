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
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
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
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code INDIVIDUAL_ATTRIBUTE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record IndividualAttributeRecord {
 *   id: LocalID
 *   individual: Xref&lt;IndividualRecord&gt;
 *   type: enum {
 *     characteristic, residence, occupation, possession, military_rank, caste, social_class, ethnicity, citizenship,
 *     nationality, ssn, title, children_count, marriages_count, religion, language, literacy, education
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
public class IndividualAttributeRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4220284900986598102L;


	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
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
		HandlerRegistry.register(new IndividualAttributeHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]10[]"));

	private final String individualId;
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


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static IndividualAttributeRecordDialog createNew(final Dialog parent, final FLEFModel model, final String individualId){
		return new IndividualAttributeRecordDialog(parent, model, individualId, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static IndividualAttributeRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new IndividualAttributeRecordDialog(parent, model, null, record);
	}


	private IndividualAttributeRecordDialog(final Dialog parent, final FLEFModel model, final String individualId,
			final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualAttributeHandler.TYPE));

		this.individualId = extractReferenceId(individualId, record, TAG_INDIVIDUAL);

		typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{StringUtils.EMPTY,
				"characteristic", "residence", "occupation", "possession", "military_rank", "caste", "social_class",
				"ethnicity", "citizenship", "nationality", "ssn", "title", "children_count", "marriages_count", "religion",
				"language", "literacy", "education"
			});
		typeCombo.setEditable(true);
		valueField = new BoundTextField(TAG_VALUE, 20);
		validOnField = DateField.createWithWrapperTag(TAG_VALID_ON, this, "Date", model);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, parent, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


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
		mainPanel.add(new JLabel("Type*:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// value
		mainPanel.add(new JLabel("Value:"), "align label");
		mainPanel.add(valueField, "growx, wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid on
		validityPanel.add(new JLabel("Valid On:"), "align label");
		validityPanel.add(validOnField, "growx,wrap");
		// valid from
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx,wrap");
		// valid to
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx,wrap");
		add(validityPanel, "span 2,growx,wrap");

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


	@Override
	protected void loadData(){
		if(StringUtils.isBlank(individualId)){
			JOptionPane.showMessageDialog(this, "Invalid Individual ID: `" + individualId + "`.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

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
		if(StringUtils.isEmpty(individualId)){
			JOptionPane.showMessageDialog(null,
				"INDIVIDUAL is required for an attribute.\n" +
					"Please select a individual record.",
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
		FLEFRecordHelper.updateChildValue(record, TAG_INDIVIDUAL, XRefHelper.formatXRef(individualId));

		bindingManager.save(record);

		validOnField.save(record);
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
//			FLEFRecord individualAttribute = FLEFRecord.createEmpty();
//			individualAttribute.addChild(FLEFRecord.createChildWithValue(TAG_INDIVIDUAL, "@I1@"));
//			final IndividualAttributeRecordDialog dialog = IndividualAttributeRecordDialog.createEdit(null, model, individualAttribute);
			final IndividualAttributeRecordDialog dialog = IndividualAttributeRecordDialog.createNew(null, model, "@I1@");
			dialog.setVisible(true);
		});
	}

}
