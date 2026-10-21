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
package io.github.mtrevisan.familylegacy.ui.components.fields;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


/**
 * A text field that displays a singular embedded structure (e.g. a {@code DateStructure}) and edits it through a dialog.
 * <p>
 * Unlike {@link EntityField}, the structure is not a reference to a record of the model: it has no ID, it is never
 * selected among existing records, and it is owned by the record that contains it.
 * <p>
 * Subclasses provide the dialogs and the textual rendering of the structure.
 */
public abstract class StructureField extends BoundTextField{

	protected final Window parent;
	protected final FLEFModel model;

	private FLEFRecord record;


	/**
	 * @param path	the path of the structure inside the record passed to {@link #load(FLEFRecord)} and
	 * 	{@link #save(FLEFRecord)}; it may be {@code null} if the caller passes the structure's wrapper node directly
	 * @param parent	the parent window of the dialogs
	 * @param model	the FLEF model
	 */
	protected StructureField(final String path, final Window parent, final FLEFModel model){
		super(path);

		this.parent = parent;
		this.model = model;

		BindingsHelper.installBehavior(this,
			this::edit, null,
			null, null,
			builder -> {
				builder.item(I18N.t("popupmenu.set"), this::createNew);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::edit);
				builder.selectionSensitiveItem(I18N.t("popupmenu.clear"), this::clear);
			}
		);
	}


	/**
	 * Opens the dialog for creating a new structure.
	 *
	 * @return	the new structure, or {@code null} if the dialog was cancelled.
	 */
	protected abstract FLEFRecord openNewDialog();

	/**
	 * Opens the dialog for editing an existing structure in place.
	 *
	 * @param record	the structure to edit.
	 * @return	whether the dialog was saved.
	 */
	protected abstract boolean openEditDialog(FLEFRecord record);

	/**
	 * Renders the structure as text for the field.
	 *
	 * @param record	the structure (never {@code null}).
	 * @return	the text to display.
	 */
	protected abstract String toDisplayText(FLEFRecord record);


	/**
	 * Returns the current structure.
	 *
	 * @return	the structure, or {@code null} if none.
	 */
	public FLEFRecord getRecord(){
		return record;
	}

	/**
	 * Updates the underlying structure and refreshes the display.
	 *
	 * @param record	the structure (it may be {@code null}).
	 */
	public void setRecord(final FLEFRecord record){
		this.record = record;

		updateDisplay();
	}

	@Override
	public void clear(){
		setRecord(null);
	}

	public boolean hasData(){
		return (record != null && record.hasData());
	}


	/**
	 * Loads the structure found at the field's path inside the given record.
	 *
	 * @param source	the record containing the structure.
	 */
	public void load(final FLEFRecord source){
		clear();

		if(source == null || source.isEmpty())
			return;

		final FLEFRecord child = FLEFRecordHelper.findChild(source, path);
		setRecord(child);
	}

	/**
	 * Saves the structure into the given record, replacing the structure already present at the field's path.
	 * <p>
	 * The structure is singular: any previous content at the path is removed first, so that saving twice, or saving
	 * into a record that already contains the structure, never produces duplicated leaves. When the field has no data,
	 * the structure is removed from the target.
	 * <p>
	 * The target receives a deep copy, so that later in-place edits of this field do not alter the saved record.
	 *
	 * @param target	the record to save into.
	 */
	public void save(final FLEFRecord target){
		if(target == null)
			return;

		clearRecord(record);

		if(!hasData())
			return;

		final FLEFRecord copy = FLEFRecord.createEmpty();
		record.deepCopyTo(copy);
		final FLEFRecord targetNode = FLEFRecordHelper.getOrCreateTargetNode(target, path);
		if(StringUtils.isEmpty(path))
			// No wrapper node: replace only the children that the structure is about to write
			for(final FLEFRecord child : copy.getChildren())
				FLEFRecordHelper.removeChildren(targetNode, child.getTag());
		targetNode.addChildren(copy.getChildren());
	}

	/**
	 * Clears all tags from the record before saving.
	 *
	 * @param record the FLEF record to clean
	 */
	private void clearRecord(final FLEFRecord record){
		if(StringUtils.isNotEmpty(path))
			FLEFRecordHelper.removeChildren(record, path);
	}


	private void createNew(){
		final FLEFRecord created = openNewDialog();
		if(created != null)
			setRecord(created);
	}

	private void edit(){
		if(!hasData()){
			createNew();

			return;
		}

		if(openEditDialog(record))
			// Needed because the structure is edited in place: the reference does not change, its content does
			updateDisplay();
	}

	protected void updateDisplay(){
		BindingsHelper.updateDisplay(this,
			this::hasData,
			() -> toDisplayText(record));
	}

}
