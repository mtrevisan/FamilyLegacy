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
package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;

import java.awt.Dialog;
import java.util.List;


/**
 * Handler for a specific record type (INDIVIDUAL, FAMILY, GROUP, EVENT, etc.).
 * Provides methods for display name, edit dialog, and new dialog creation.
 *
 * @param <T>	the specific dialog type that extends JDialog
 */
public interface RecordTypeHandler<T extends BaseRecordDialog>{

	default boolean isTopLevelEntity(){
		return true;
	}

	@SuppressWarnings("unchecked")
	default Class<? extends RecordTypeHandler<?>> getHandlerClass(){
		return (Class<? extends RecordTypeHandler<?>>)getClass();
	}

	/**
	 * Returns the label for printing.
	 */
	String getLabel();

	/**
	 * Returns the record type name (e.g., "INDIVIDUAL", "FAMILY").
	 */
	String getType();

	/**
	 * For citations, returns the record's cited type name (e.g., "REPOSITORY" if it's a Repository Citation).
	 */
	default String getCitedType(){
		return getType();
	}

	/**
	 * Returns the record ID prefix (e.g., "I", "F").
	 */
	String getIdPrefix();

	default RecordTypeHandler<?> getRecordHandler(){
		return null;
	}

	default RecordTypeHandler<?> getParentHandler(){
		return null;
	}

	default List<FLEFRecord> extractEntities(final FLEFRecord record, final String path){
		return FLEFRecordHelper.findChildren(record, path);
	}

	default List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return List.of();
	}

	/**
	 * Returns a human-readable display name for the given record.
	 * Used for list rendering.
	 *
	 * @param record	the record
	 * @param model	the FLEF model
	 * @return the display name (e.g., "John Doe (I1)", "Smith Family (F1)")
	 */
	String getDisplayText(FLEFRecord record, FLEFModel model);

	/**
	 * Creates a dialog to create a new record.
	 *
	 * @param parent	The parent frame.
	 * @param model	The FLEF model.
	 * @return	The dialog (already configured but not shown).
	 */
	T createNewDialog(Dialog parent, FLEFModel model);

	/**
	 * Creates a dialog to edit an existing record.
	 *
	 * @param parent	The parent frame.
	 * @param model	The FLEF model.
	 * @param record	The record to edit.
	 * @return	The dialog (already configured but not shown).
	 */
	T createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record);

}
