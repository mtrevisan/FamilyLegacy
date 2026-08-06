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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ExtractListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
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


/* DONE */
/**
 * Dialog for editing a {@code SOURCE_CITATION} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct SourceCitation {
 *   source: Xref&lt;SourceRecord&gt;
 *   location?: Text
 *   extract*: ExtractStructure
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *
 *   require extract.document in source.document
 * }
 * struct ExtractStructure {
 *   document_part+: struct {
 *     document: Xref&lt;DocumentRecord&gt;
 *     crop?: CropRect
 *   }
 *   text?: Text
 *   type?: enum { verbatim, summarized, translated, normalized }
 *   locale?: LocaleCode
 *   note*: Text
 *
 *   require one_of(crop, text, type, locale, note)
 * }
* </pre>
 */
public class SourceCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -7024588390352183760L;


	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_LOCATION = "LOCATION";
	private static final String TAG_EXTRACT = "EXTRACT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new SourceCitationHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]"));

	private String sourceId;
	private final BoundTextField locationField;
	private final ExtractListPanel extractPanel;
	private final NoteListPanel notePanel;
	private final EvidenceQualifiersPanel qualifiers;
	private final RestrictionPanel restrictionPanel;


	public static SourceCitationDialog createNew(final Dialog parent, final FLEFModel model, final String sourceId){
		return new SourceCitationDialog(parent, model, sourceId, null);
	}

	public static SourceCitationDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new SourceCitationDialog(parent, model, null, record);
	}


	private SourceCitationDialog(final Dialog parent, final FLEFModel model, final String sourceId,
			final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(SourceCitationHandler.TYPE));

		this.sourceId = (sourceId != null
			? sourceId:
			XRefHelper.extractXRef(FLEFRecordHelper.getChildValue(record, TAG_SOURCE)));

		locationField = new BoundTextField(TAG_LOCATION, 20);
		extractPanel = new ExtractListPanel(TAG_EXTRACT, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, null, model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(locationField);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Notes", notePanel);
		tabbedPane.addTab("Restriction", restrictionPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// location
		mainPanel.add(new JLabel("Location:"), "align label");
		mainPanel.add(locationField, "growx,wrap");

		// extract
		mainPanel.add(extractPanel, "span 2,growx,wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx");

		return mainPanel;
	}

	@Override
	protected void loadData(){
		if(StringUtils.isBlank(sourceId)){
			JOptionPane.showMessageDialog(this, "Invalid Source ID: `" + sourceId + "`.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		if(record == null)
			return;

		bindingManager.load(record);

		notePanel.load(record);
		extractPanel.load(record);
		qualifiers.load(record);
	}

	@Override
	protected boolean validData(){
		if(sourceId == null || sourceId.isEmpty()){
			JOptionPane.showMessageDialog(null,
				"SOURCE is required for a citation.\n" +
					"Please select a source record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_SOURCE, XRefHelper.formatXRef(sourceId));

		bindingManager.save(record);

		notePanel.saveReferences(record);
		extractPanel.save(record);
		qualifiers.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
//			FLEFRecord sourceCitation = FLEFRecord.createEmpty();
//			sourceCitation.addChild(FLEFRecord.createChildWithValue(TAG_SOURCE, "@S1@"));
//			final SourceCitationDialog dialog = SourceCitationDialog.createEdit(null, model, sourceCitation);
			final SourceCitationDialog dialog = SourceCitationDialog.createNew(null, model, "@S1@");
			dialog.setVisible(true);
		});
	}

}
