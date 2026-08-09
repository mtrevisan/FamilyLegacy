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
package io.github.mtrevisan.familylegacy.v2.ui.components.fields;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateFieldPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DateDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Component for selecting and displaying dates.
 */
public class DateField extends JPanel{

	@Serial
	private static final long serialVersionUID = 4495716172290856838L;


	private final Dialog parent;
	private final String dialogTitle;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(20);


	public static DateField create(final Dialog parent, final String dialogTitle, final FLEFModel model){
		return new DateField(null, parent, dialogTitle, model);
	}

	public static DateField createWithWrapperTag(final String path, final Dialog parent, final String dialogTitle,
			final FLEFModel model){
		return new DateField(path, parent, dialogTitle, model);
	}


	private DateField(final String path, final Dialog parent, final String dialogTitle, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;
		this.dialogTitle = dialogTitle;

		this.path = path;
		this.model = model;


		initComponents();
	}


	private void initComponents(){
		setupField(displayField,
			this::createNew,
			this::edit,
			this::clear
		);

		add(displayField, "growx");
	}

	private void setupField(final JTextField field,
			final Runnable newAction, final Runnable editAction, final Runnable clearAction){
		GUIHelper.installBehavior(field,
			editAction,
			null,
			null,
			builder -> {
				builder.item("Set Date...", newAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			}
		);

		updateDisplay();
	}

	/**
	 * Updates the underlying record and automatically refreshes the display.
	 */
	public void setRecord(final FLEFRecord record){
		this.record = record;

		updateDisplay();
	}

	public void clear(){
		setRecord(null);
	}

	public boolean hasData(){
		return (record != null && record.hasData());
	}

	public void load(final FLEFRecord targetRecord){
		clear();

		if(targetRecord == null)
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(targetRecord, path);
		setRecord(child);
	}

	public void save(final FLEFRecord targetRecord){
		FLEFRecordHelper.removeChildren(targetRecord, path);

		if(record != null){
			final FLEFRecord targetNode = FLEFRecordHelper.getOrCreateTargetNode(targetRecord, path);
			targetNode.addChildren(record.getChildren());
		}
	}

	private void createNew(){
		final DateDialog dialog = DateDialog.createNew(parent, model, dialogTitle);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void edit(){
		if(!hasData()){
			createNew();

			return;
		}

		final DateDialog dialog = DateDialog.createEdit(parent, model, dialogTitle, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// Only necessary here if changes are in-place
			updateDisplay();
	}

	private void updateDisplay(){
		GUIHelper.updateDisplay(displayField,
			this::hasData,
			() -> DateFieldPanel.extractDateSummary(record));
	}

}
