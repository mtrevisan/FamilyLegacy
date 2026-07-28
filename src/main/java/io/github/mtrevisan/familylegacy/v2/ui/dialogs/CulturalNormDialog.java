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

import io.github.mtrevisan.familylegacy.v2.io.FLEFFile;
import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PlaceField;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
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
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Dialog for editing a {@code CULTURAL_NORM_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * CULTURAL_NORM_RECORD :=
 *   n @<XREF:CULTURAL_NORM>@ CULTURAL_NORM    {1:1}
 *     +1 TITLE <CULTURAL_NORM_DESCRIPTIVE_TITLE>    {0:1}
 *     +1 <<PLACE_STRUCTURE>>    {0:1}
 *     +1 VALID_FROM    {0:1}
 *       +2 <<DATE_STRUCTURE>>    {1:1}
 *     +1 VALID_TO    {0:1}
 *       +2 <<DATE_STRUCTURE>>    {1:1}
 *     +1 NOTE @<XREF:NOTE>@    {0:M}
 *     +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class CulturalNormDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 950729006569948384L;


	// Handlers
	static{
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField;
	private final PlaceField placeField;
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel("PLACE", "Evidence");
	private final DateField validFromField;
	private final DateField validToField;
	private final EvidenceQualifiersPanel qualifiers = new EvidenceQualifiersPanel(null, "Evidence");
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final ModificationPanel modificationPanel;


	public static CulturalNormDialog createNew(final Dialog parent, final FLEFModel model){
		return new CulturalNormDialog(parent, model, null);
	}

	public static CulturalNormDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new CulturalNormDialog(parent, model, record);
	}


	private CulturalNormDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(CulturalNormHandler.TYPE));

		titleField = new BoundTextField("TITLE", 30);
		placeField = PlaceField.create(parent, "Place", model);
		validFromField = DateField.createWithWrapperTag(this, "Valid From Date", model, "VALID_FROM");
		validToField = DateField.createWithWrapperTag(this, "Valid To Date", model, "VALID_TO");
		notePanel = new NoteListPanel(model, this);
		sourceCitationPanel = new SourceCitationListPanel(this, model);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents(){
		bindingManager.bind(titleField);

		setLayout(new MigLayout("fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]5[]"));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// title
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// place
		final JPanel placePanel = new JPanel(new MigLayout("ins 5,fillx,top", "[grow]", "[]5[]"));
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.add(placeField, "growx,wrap");
		placePanel.add(placeQualifiers, "growx,wrap");
		mainPanel.add(placePanel, "span 2,growx,wrap");

		// validity range
		final JPanel validityPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		validityPanel.setBorder(BorderFactory.createTitledBorder("Validity Range"));
		validityPanel.add(new JLabel("Valid From:"), "align label");
		validityPanel.add(validFromField, "growx, wrap");
		validityPanel.add(new JLabel("Valid To:"), "align label");
		validityPanel.add(validToField, "growx, wrap");
		mainPanel.add(validityPanel, "span 2, growx, wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		// title
		bindingManager.loadFromRecord(record);

		// place
		placeField.load(record);
		placeQualifiers.load(record);

		// validity range
		validFromField.load(record);
		validToField.load(record);

		// notes
		final List<FLEFRecord> notes = new ArrayList<>();
		for(final FLEFRecord child : record.findChildren("NOTE"))
			if(child.getValue() != null)
				notes.add(child);
		notePanel.loadFromNotes(notes);

		// sources
		sourceCitationPanel.setItems(record.findChildren("SOURCE"));

		// qualifiers
		qualifiers.load(record);

		// modification
		modificationPanel.load(record);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		// title
		bindingManager.saveToRecord(record);

		// place
		placeField.save(record);
		placeQualifiers.save(record);

		// validity range
		validFromField.save(record);
		validToField.save(record);

		// notes
		for(final FLEFRecord note : notePanel.getNotes())
			FLEFRecordUtils.addChild(record, "NOTE", note.getFormattedId());

		// sources
		for(final FLEFRecord source : sourceCitationPanel.getItems())
			record.addChild(source);

		// qualifiers
		qualifiers.save(record);

		// modification
		modificationPanel.save(record);

		if(isNew)
			model.addRecord(record);
		isSaved = true;

// TODO to be removed
FLEFFile.print(model);
//		dispose();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final CulturalNormDialog dialog = CulturalNormDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
