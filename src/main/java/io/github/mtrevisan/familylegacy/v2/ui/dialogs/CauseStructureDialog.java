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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CauseHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Structure:
 * <pre>
 * cause?: struct {
 *   value: Text
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 */
public class CauseStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 7424080475408411889L;


	private static final String TAG_CAUSE = "CAUSE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_EVIDENCE = "EVIDENCE";


	private final RecordDialogComponents components;

	private final BoundTextField valueField;


	public static CauseStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, CauseStructureDialog::new);
	}

	public static CauseStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, CauseStructureDialog::new);
	}


	private CauseStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, CauseHandler.class);

		valueField = new BoundTextField(TAG_VALUE);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.EVIDENCE, TAG_EVIDENCE, "Evidence", null, RelationshipHandler.class)
			.build();

		components.bind(valueField);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(0, "[]10[]");

		// value:
		GUIHelper.addLabeledComponent(panel, "Value:", valueField);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(panel, evidencePanel);

		return panel;
	}


	@Override
	protected void loadData(){
		components.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Value cannot be empty.",
				null, null, valueField);

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		record.setTag(TAG_CAUSE);

		components.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(CauseStructureDialog::createNew);
	}

}
