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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * <pre>
 * record HistoricEventRecord {
 *   id: LocalID
 *   title?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class HistoricEventRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3544157573805016620L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new HistoricEventHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final PlaceField subjectField;
	private final PlaceField objectField;
	private final BoundComboBox<String> typeCombo;
	private final DateField validFromField;
	private final DateField validToField;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final ModificationPanel modificationPanel;


	public static HistoricEventRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new HistoricEventRecordDialog(parent, model, null);
	}

	public static HistoricEventRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new HistoricEventRecordDialog(parent, model, record);
	}


	private HistoricEventRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PlaceRelationshipHandler.TYPE));

		setTitle(record == null? "Add Place Relationship": "Edit Place Relationship");

		subjectField = PlaceField.create(TAG_SUBJECT, parent, model);
		objectField = PlaceField.create(TAG_OBJECT, parent, model);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			"administrative_part_of", "geographic_part_of", "ecclesiastical_part_of", "judicial_part_of",
			"cadastral_part_of"
		});
		typeCombo.setEditable(true);
		validFromField = DateField.createWithWrapperTag(TAG_VALID_FROM, this, "From Date", model);
		validToField = DateField.createWithWrapperTag(TAG_VALID_TO, this, "To Date", model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);

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
		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]10[]"));

		// subject
		mainPanel.add(new JLabel("Subject:"), "align label");
		mainPanel.add(subjectField, "growx,wrap");

		// object
		mainPanel.add(new JLabel("Object:"), "align label");
		mainPanel.add(objectField, "growx,wrap");

		// type
		mainPanel.add(new JLabel("Part Type*:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		// valid from
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx,wrap");
		// valid to
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx,wrap");
		mainPanel.add(validityPanel, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
//		subjectField.load(record);
//		objectField.load(record);
//
//		bindingManager.load(record);
//
//		validFromField.load(record);
//		validToField.load(record);
//		notePanel.load(record);
//		sourceCitationPanel.load(record);
//		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
//		if(!subjectField.hasData()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Subject cannot be empty.",
//				tabbedPane, mainPanel, subjectField);
//
//			return false;
//		}
//
//		if(!objectField.hasData()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Object cannot be empty.",
//				tabbedPane, mainPanel, objectField);
//
//			return false;
//		}
//
//		if(!typeCombo.isSelected()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Type cannot be empty.",
//				tabbedPane, mainPanel, typeCombo);
//
//			return false;
//		}

		return true;
	}

	@Override
	protected void saveData(){
//		subjectField.save(record);
//		objectField.save(record);
//
//		bindingManager.save(record);
//
//		validFromField.save(record);
//		validToField.save(record);
//		notePanel.saveReferences(record);
//		sourceCitationPanel.save(record);
//		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final HistoricEventRecordDialog dialog = HistoricEventRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
