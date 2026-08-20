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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.ExtractListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


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
 *   privacy?: PrivacyStructure
 *
 *   require extract.document_part.document in source.document
 * }
 *
 * struct ExtractStructure {
 *   document_part*: struct {
 *     document: Xref&lt;DocumentRecord&gt;
 *     crop?: CropRect
 *   }
 *   text?: Text
 *   type?: enum { verbatim, summarized, translated, normalized }
 *   locale?: LocaleCode
 *   note*: Text
 *
 *   require one_of(document_part, text)
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): location, extract, evidence
 * Tab 8 (Notes): note
 * Tab 9 (Privacy): privacy
 */
public class SourceCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -7024588390352183760L;


	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_LOCATION = "LOCATION";
	private static final String TAG_EXTRACT = "EXTRACT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_PRIVACY = "PRIVACY";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField sourceField;
	private final BoundTextField locationField;
	private final ExtractListPanel extractPanel;


	public static SourceCitationDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, SourceCitationDialog::new);
	}

	public static SourceCitationDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, SourceCitationDialog::new);
	}


	private SourceCitationDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, SourceCitationHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		sourceField = new BoundTextField(TAG_SOURCE);
		locationField = new BoundTextField(TAG_LOCATION);
		extractPanel = new ExtractListPanel(TAG_EXTRACT, this, "Extracts", model);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, GroupAttributeHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.build();

		components.bind(sourceField);
		components.bind(locationField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// location
		GUIHelper.addLabeledComponent(propertiesPanel, "Location:", locationField);

		// extract
		GUIHelper.addComponent(propertiesPanel, extractPanel);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}


	public void setSource(final String sourceId){
		if(StringUtils.isNotEmpty(sourceId)){
			if(!confirmRecordExistsForType(sourceId, SourceHandler.class))
				return;

			sourceField.setText(sourceId);
		}
	}


	@Override
	protected void loadData(){
		components.load(record);

		extractPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(sourceField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Source cannot be empty.",
				tabbedPane, propertiesPanel, sourceField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		extractPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord source = FLEFRecord.createMainRecord("S1", TAG_SOURCE);
			model.addRecord(source);

//			final FLEFRecord sourceCitation = FLEFRecord.createEmpty();
//			sourceCitation.addChild(FLEFRecord.createChildWithValue(TAG_SOURCE, "S1"));
//			final SourceCitationDialog dialog = SourceCitationDialog.createEdit(null, model, sourceCitation);
			final SourceCitationDialog dialog = SourceCitationDialog.createNew(null, model);
			dialog.setSource("S1");
			dialog.setVisible(true);
		});
	}

}
