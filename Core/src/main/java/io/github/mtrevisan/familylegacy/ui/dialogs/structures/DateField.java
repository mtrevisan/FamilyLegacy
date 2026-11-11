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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.fields.StructureField;

import java.awt.Window;
import java.util.function.Supplier;


/**
 * Component for selecting and displaying dates ({@code DateStructure}).
 */
public final class DateField extends StructureField{

	private final String dialogTitle;
	private Supplier<String> placeSupplier;


	public static DateField create(final Window parent, final String dialogTitle, final FLEFModel model){
		return new DateField(null, parent, dialogTitle, model);
	}

	public static DateField createWithWrapperTag(final String path, final Window parent, final String dialogTitle,
			final FLEFModel model){
		return new DateField(path, parent, dialogTitle, model);
	}


	private DateField(final String path, final Window parent, final String dialogTitle, final FLEFModel model){
		super(path, parent, model);

		this.dialogTitle = dialogTitle;
	}


	/**
	 * Tells where to find the place of the event the date belongs to, so that the date assistant can start from the reform of
	 * the jurisdiction of that place.
	 *
	 * @param placeSupplier	gives the id of the place, or {@code null} if the event has none; asked each time a dialog opens
	 * @return	This field.
	 */
	public DateField withPlace(final Supplier<String> placeSupplier){
		this.placeSupplier = placeSupplier;
		return this;
	}


	@Override
	protected FLEFRecord openNewDialog(){
		final DateStructureDialog dialog = DateStructureDialog.createNew(parent, model, dialogTitle, placeSupplier);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected boolean openEditDialog(final FLEFRecord record){
		final DateStructureDialog dialog = DateStructureDialog.createEdit(parent, model, dialogTitle, record, placeSupplier);
		dialog.setVisible(true);

		return dialog.isSaved();
	}

	@Override
	protected String toDisplayText(final FLEFRecord record){
		return DateReader.extractPrettyPrintDate(record);
	}

}
