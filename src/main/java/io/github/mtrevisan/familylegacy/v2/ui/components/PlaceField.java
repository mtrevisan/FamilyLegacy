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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs._PlaceStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING FIXME popupmenu is always `selected` */
public class PlaceField extends JPanel{

	@Serial
	private static final long serialVersionUID = -4552674609094385732L;


	static{
		HandlerRegistry.register(new PlaceHandler());
	}


	private final Dialog parent;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(20);

	private final RecordTypeHandler<?> placeHandler;


	public static PlaceField create(final String path, final Dialog parent, final FLEFModel model){
		return new PlaceField(path, parent, model);
	}

	public static PlaceField createWithWrapperTag(final String path, final Dialog parent, final FLEFModel model){
		return new PlaceField(path, parent, model);
	}


	private PlaceField(final String path, final Dialog parent, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;

		this.path = path;
		this.model = model;

		placeHandler = HandlerRegistry.getHandler(PlaceHandler.TYPE);

		initComponents();
	}


	private void initComponents(){
		setupField(displayField,
			this::createNew,
			this::add,
			this::edit,
			this::editCitation,
			this::clear
		);

		add(displayField, "growx");
	}

	private void setupField(final JTextField field,
			final Runnable newAction, final Runnable addAction, final Runnable editAction,
			final Runnable editCitationAction, final Runnable clearAction){
		GUIHelper.installBehavior(field,
			editAction,
			newAction,
			clearAction,
			builder -> {
				builder.item("Create New...", newAction);
				builder.item("Add Existing...", addAction);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", editAction);
				builder.selectionSensitiveItem("Edit Citation...", editCitationAction);
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
		if(parentRecord != null){
			final FLEFRecord child = FLEFRecordHelper.findChild(parentRecord, path);
			setRecord(child);
		}
		else
			clear();
	}

	public void save(final FLEFRecord parentRecord){
		FLEFRecordHelper.removeChildren(parentRecord, path);

		if(record != null)
			FLEFRecordHelper.updateChildValue(parentRecord, path, record.getFormattedId());
	}

	private void createNew(){
		final PlaceRecordDialog dialog = PlaceRecordDialog.createNew(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved())
			setRecord(dialog.getRecord());
	}

	private void add(){
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, placeHandler,
			selectedId -> {
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

		final PlaceRecordDialog dialog = PlaceRecordDialog.createEdit(parent, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			updateDisplay();
	}

	private void editCitation(){
		if(record == null)
			return;

		final _PlaceStructureDialog dialog = new _PlaceStructureDialog(parent, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			updateDisplay();
	}

	private void updateDisplay(){
		GUIHelper.updateDisplay(displayField,
			() -> (record != null && record.hasData()),
			() -> placeHandler.getDisplayText(record, model));
	}

}
