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
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
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
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record CulturalNormRecord {
 *   id: LocalID
 *   title?: Text
 *   rule_type?: enum { age_of_majority, naming_convention, inheritance_rule, marriage_minimum_age } | Text
 *   place?: PlaceCitation
 *   valid_from?: DateStructure
 *   valid_to?: DateStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class CulturalNormRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 950729006569948384L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_RULE_TYPE = "RULE_TYPE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_VALID_FROM = "VALID_FROM";
	private static final String TAG_VALID_TO = "VALID_TO";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField;
	private final BoundComboBox<String> ruleTypeCombo;
	private final PlaceCitationField placeCitationField;
	private final EvidenceQualifiersPanel placeQualifiers;
	private final DateField validFromField;
	private final DateField validToField;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final EvidenceQualifiersPanel qualifiers;
	private final ModificationPanel modificationPanel;


	public static CulturalNormRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new CulturalNormRecordDialog(parent, model, null);
	}

	public static CulturalNormRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new CulturalNormRecordDialog(parent, model, record);
	}


	private CulturalNormRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(CulturalNormHandler.TYPE));

		titleField = new BoundTextField(TAG_TITLE, 30);
		ruleTypeCombo = new BoundComboBox<>(TAG_RULE_TYPE, new String[]{"age_of_majority", "naming_convention", "inheritance_rule", "marriage_minimum_age"});
		ruleTypeCombo.setEditable(true);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, parent, model);
		placeQualifiers = new EvidenceQualifiersPanel(TAG_PLACE, "Evidence");
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "Valid From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "Valid To Date", model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		qualifiers = new EvidenceQualifiersPanel(null, "Evidence");
		modificationPanel = new ModificationPanel(this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	protected void initComponents(){
		bindingManager.bind(titleField);
		bindingManager.bind(ruleTypeCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
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
		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]5[]"));

		// title
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// rule type
		mainPanel.add(new JLabel("Rule Type:"), "align label");
		mainPanel.add(ruleTypeCombo, "growx, wrap");

		// place
		final JPanel placePanel = new JPanel(new MigLayout("ins 10,fillx,top", "[grow]", "[]5[]"));
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.add(placeCitationField, "growx,wrap");
		placePanel.add(placeQualifiers, "growx,wrap");
		mainPanel.add(placePanel, "span 2,growx,wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx,wrap");
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx,wrap");
		mainPanel.add(validityPanel, "span 2,growx,wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		placeCitationField.load(record);
		placeQualifiers.load(record);
		validFromField.load(record);
		validToField.load(record);
		notePanel.load(record);
		sourceCitationPanel.load(record);
		qualifiers.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		placeCitationField.save(record);
		placeQualifiers.save(record);
		validFromField.save(record);
		validToField.save(record);
		notePanel.save(record);
		sourceCitationPanel.save(record);
		qualifiers.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final CulturalNormRecordDialog dialog = CulturalNormRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
