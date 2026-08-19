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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code PLACE_CITATION} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct PlaceCitation {
 *   place: Xref&lt;PlaceRecord&gt;
 *   original_text?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): original text, evidence
 * Tab 7 (Sources): source
 */
public class PlaceCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6489523892351201199L;


	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField place;
	private final BoundTextField originalTextField;


	public static PlaceCitationDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PlaceCitationDialog::new);
	}

	public static PlaceCitationDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PlaceCitationDialog::new);
	}


	private PlaceCitationDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PlaceCitationHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]");

		place = new BoundTextField(TAG_PLACE);
		originalTextField = new BoundTextField(TAG_ORIGINAL_TEXT);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withCitationComponent(PanelKey.SOURCE, TAG_SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, PlaceCitationHandler.class)
			.build();

		components.bind(place);
		components.bind(originalTextField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// original text
		GUIHelper.addLabeledComponent(propertiesPanel, "Original Text*:", originalTextField);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}


	public void setPlace(final String placeId){
		if(StringUtils.isNotEmpty(placeId)){
			if(!confirmRecordExistsForType(placeId, PlaceHandler.class))
				return;

			place.setText(placeId);
		}
	}


	@Override
	protected void loadData(){
		components.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(place.getText())){
			JOptionPane.showMessageDialog(null,
				"Place is required for a citation.\n" +
					"Please select a place record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_PLACE, place.getText());

		components.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord place = FLEFRecord.createMainRecord("P1", TAG_PLACE);
			model.addRecord(place);

//			final FLEFRecord placeCitation = FLEFRecord.createEmpty();
//			placeCitation.addChild(FLEFRecord.createChildWithValue(TAG_PLACE, "P1"));
//			final PlaceCitationDialog dialog = PlaceCitationDialog.createEdit(null, model, placeCitation);
			final PlaceCitationDialog dialog = PlaceCitationDialog.createNew(null, model);
			dialog.setPlace("P1");
			dialog.setVisible(true);
		});
	}

}
