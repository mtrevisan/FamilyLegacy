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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.DateDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dialog;
import java.util.function.Supplier;


/* DONE */
public class DateField extends JPanel{

	private static final Color COLOR_BACKGROUND = UIManager.getColor("TextField.background");
	private static final Color COLOR_FOREGROUND_ENABLED = UIManager.getColor("TextField.foreground");
	private static final Color COLOR_FOREGROUND_DISABLED = UIManager.getColor("Label.disabledForeground");

	private static final String PLACEHOLDER_TEXT = "(right-click to set date)";
	private static final String TOOLTIP_TEXT = "right-click or double-click to set, edit, or clear date";


	private final Dialog parent;
	private final String dialogTitle;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(20);


	public static DateField create(final Dialog parentDialog, final String dialogTitle, final FLEFModel model){
		return new DateField(null, parentDialog, dialogTitle, model);
	}

	public static DateField createWithWrapperTag(final String path, final Dialog parentDialog, final String dialogTitle,
			final FLEFModel model){
		return new DateField(path, parentDialog, dialogTitle, model);
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
		displayField.setEditable(false);
		displayField.setBackground(COLOR_BACKGROUND);
		displayField.setToolTipText(TOOLTIP_TEXT);

		setupField(displayField,
			() -> (record != null),
			this::createNew,
			this::edit,
			this::clear
		);

		add(displayField, "growx");
	}

	private void setupField(final JTextField field,
			final Supplier<Boolean> hasSelection,
			final Runnable newAction, final Runnable editAction, final Runnable clearAction){
		GUIHelper.installBehavior(field,
			hasSelection,
			editAction,
			newAction,
			clearAction,
			builder -> {
				builder.item("Set Date...", newAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			}
		);
	}

	public void setRecord(final FLEFRecord record){
		this.record = record;

		updateDisplay();
	}

	public void clear(){
		setRecord(null);
	}

	public boolean hasData(){
		return (record != null);
	}

	public void load(final FLEFRecord parentRecord){
		if(parentRecord != null){
			final FLEFRecord child = FLEFRecordHelper.findChild(parentRecord, path);
			setRecord(child);
		}
		else
			clear();
	}

	public void save(final FLEFRecord parentRecord){
		FLEFRecordHelper.removeChildren(parentRecord, path);

		if(record != null){
			record.setTag(path);
			parentRecord.addChild(record);
		}
	}

	private void createNew(){
		final DateDialog dialog = DateDialog.createNew(parent, model, dialogTitle);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void edit(){
		if(record == null){
			createNew();
			return;
		}

		final DateDialog dialog = DateDialog.createEdit(parent, model, dialogTitle, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void updateDisplay(){
		if(record != null && record.hasData()){
			displayField.setText(DateFieldPanel.extractDateSummary(record));
			displayField.setForeground(COLOR_FOREGROUND_ENABLED);
		}
		else{
			displayField.setText(PLACEHOLDER_TEXT);
			displayField.setForeground(COLOR_FOREGROUND_DISABLED);
		}
	}

}
