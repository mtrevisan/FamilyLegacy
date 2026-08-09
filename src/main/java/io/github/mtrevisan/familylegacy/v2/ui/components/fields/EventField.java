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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.EventRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Component for selecting and displaying events.
 */
public class EventField extends JPanel{

	@Serial
	private static final long serialVersionUID = 5888430347868386145L;


	static{
		HandlerRegistry.register(new EventHandler());
	}


	private final Dialog parent;

	private final String path;
	private final FLEFModel model;

	private FLEFRecord record;

	private final JTextField displayField = new JTextField(20);

	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler(EventHandler.TYPE);


	public static EventField create(final String path, final Dialog parent, final FLEFModel model){
		return new EventField(path, parent, model);
	}

	public static EventField createWithWrapperTag(final String path, final Dialog parent, final FLEFModel model){
		return new EventField(path, parent, model);
	}


	private EventField(final String path, final Dialog parent, final FLEFModel model){
		super(new MigLayout("ins 0,fillx", "[grow]"));

		this.parent = parent;

		this.path = path;
		this.model = model;


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
			editAction,
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

		if(record != null)
			FLEFRecordHelper.updateChildValue(targetRecord, path, record.getFormattedId());
	}

	/**
	 * Creates a new place and adds a citation for it.
	 */
	private FLEFRecord createNew(){
		final EventRecordDialog dialog = EventRecordDialog.createNew(parent, model);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord newRecord = dialog.getRecord();
			setRecord(newRecord);

			return newRecord;
		}

		return null;
	}

	private void add(){
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model,
			EventHandler.TYPE,
			(handlerType, selectedRecord) -> setRecord(selectedRecord)
		);
		dialog.setVisible(true);
	}

	private void edit(){
		if(!hasData()){
			createNew();

			return;
		}

		final EventRecordDialog dialog = EventRecordDialog.createEdit(parent, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			// Only necessary here if changes are in-place
			updateDisplay();
	}

	private void updateDisplay(){
		GUIHelper.updateDisplay(displayField,
			this::hasData,
			() -> eventHandler.getDisplayText(record, model));
	}

}
