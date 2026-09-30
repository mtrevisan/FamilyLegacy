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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures.DateStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Window;


// TODO EntityField?
/**
 * Component for selecting and displaying dates.
 */
public class DateField extends JPanel{

	private final Window parent;
	private final String dialogTitle;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(null);


	public static DateField create(final Window parent, final String dialogTitle, final FLEFModel model){
		return new DateField(null, parent, dialogTitle, model);
	}

	public static DateField createWithWrapperTag(final String path, final Window parent, final String dialogTitle,
			final FLEFModel model){
		return new DateField(path, parent, dialogTitle, model);
	}


	private DateField(final String path, final Window parent, final String dialogTitle, final FLEFModel model){
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
		BindingsHelper.installBehavior(field,
			editAction, null,
			null, null,
			builder -> {
				builder.item(I18N.t("popupmenu.set"), newAction);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), editAction);
				builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), clearAction);
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

	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(record, path);
		setRecord(child);
	}

	public void save(final FLEFRecord targetRecord){
		if(record != null){
			final FLEFRecord targetNode = FLEFRecordHelper.getOrCreateTargetNode(targetRecord, path);
			targetNode.addChildren(record.getChildren());
		}
	}

	private void createNew(){
		final DateStructureDialog dialog = DateStructureDialog.createNew(parent, model, dialogTitle);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void edit(){
		if(!hasData()){
			createNew();

			return;
		}

		final DateStructureDialog dialog = DateStructureDialog.createEdit(parent, model, dialogTitle, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// Only necessary here if changes are in-place
			updateDisplay();
	}

	private void updateDisplay(){
		BindingsHelper.updateDisplay(displayField,
			this::hasData,
			() -> DateReader.extractPrettyPrintDate(record));
	}


	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("value: ");
		final String text = BindingsHelper.getText(displayField.getText());
		sb.append(text != null? (text.isEmpty()? "''": text): "<null>")
			.append(", path: ")
			.append(path);
		return sb.toString();
	}

}
