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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CauseHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
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


	static{
		HandlerRegistry.register(new CauseHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField valueField;
	private final EvidenceQualifiersPanel evidencePanel;


	public static CauseStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, CauseStructureDialog::new);
	}

	public static CauseStructureDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, CauseStructureDialog::new);
	}


	private CauseStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(CauseHandler.TYPE));

		setTitle(record == null? "Add Cause": "Edit Cause");

		valueField = new BoundTextField(TAG_VALUE);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(valueField);


		setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		final JPanel valuePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		valuePanel.add(new JLabel("Value:"), "align label");
		valuePanel.add(valueField, "growx");
		add(valuePanel, "growx,wrap");

		add(evidencePanel, "growx,wrap");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		evidencePanel.load(record);
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

		bindingManager.save(record);

		evidencePanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(CauseStructureDialog::createNew);
	}

}
