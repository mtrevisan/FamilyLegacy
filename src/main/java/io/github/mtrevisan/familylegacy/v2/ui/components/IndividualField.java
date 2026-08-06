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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
public class IndividualField extends JPanel{

	@Serial
	private static final long serialVersionUID = -406821028241563963L;


	static{
		HandlerRegistry.register(new IndividualHandler());
	}


	private final Dialog parent;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(20);

	private final RecordTypeHandler<?> individualHandler;


	public static IndividualField create(final String path, final Dialog parent, final FLEFModel model){
		return new IndividualField(path, parent, model);
	}

	public static IndividualField createWithWrapperTag(final String path, final Dialog parent, final FLEFModel model){
		return new IndividualField(path, parent, model);
	}


	private IndividualField(final String path, final Dialog parent, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;

		this.path = path;
		this.model = model;

		individualHandler = HandlerRegistry.getHandler(IndividualHandler.TYPE);

		initComponents();
	}


	private void initComponents(){
		setupField(displayField,
			this::createNew,
			this::add,
			this::edit,
			this::clear
		);

		add(displayField, "growx");
	}

	private void setupField(final JTextField field,
			final Runnable newAction, final Runnable addAction, final Runnable editAction,
			final Runnable clearAction){
		GUIHelper.installBehavior(field,
			null,
			null,
			null,
			builder -> {
				builder.item("Create New...", newAction);
				builder.item("Add Existing...", addAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Clear", clearAction);
			}
		);

		updateDisplay();
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
		clear();

		if(parentRecord == null)
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(parentRecord, path);
		setRecord(child);
	}

	public void save(final FLEFRecord parentRecord){
		FLEFRecordHelper.removeChildren(parentRecord, path);

		if(record != null)
			FLEFRecordHelper.updateChildValue(parentRecord, path, record.getFormattedId());
	}

	private void createNew(){
		final IndividualRecordDialog dialog = IndividualRecordDialog.createNew(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void add(){
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, individualHandler, selectedItem -> {
			final String selectedId = selectedItem.getValue();
			if(selectedId != null){
					final FLEFRecord record = model.getRecordById(selectedId);
					setRecord(record);
				}
			}
		);
		dialog.setVisible(true);
	}

	private void edit(){
		if(record == null){
			add();
			return;
		}

		final IndividualRecordDialog dialog = IndividualRecordDialog.createEdit(parent, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			updateDisplay();
	}

	private void updateDisplay(){
		GUIHelper.updateDisplay(displayField,
			() -> (record != null && record.hasData()),
			() -> individualHandler.getDisplayText(record, model));
	}

}
