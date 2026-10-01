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
import io.github.mtrevisan.familylegacy.v2.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import java.awt.Window;
import java.util.List;


/**
 * Handler for PLACE records.
 */
public class PlaceHandler extends AbstractRecordTypeHandler<PlaceRecordDialog>{

	public static final String TYPE = "PLACE";
	public static final String ID_PREFIX = "P";


	private static final class SingletonHelper{
		private static final PlaceHandler INSTANCE = new PlaceHandler();
	}


	public static PlaceHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.place");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String id = record.getId();
		if(id == null)
			return PlaceCitationHandler.getInstance()
				.getDisplayText(record, model);

		final List<String> names = PlaceReader.extractNames(record);
		return (!names.isEmpty()? names.getFirst() + " [" + id + "]": "[" + id + "]");
	}

	@Override
	public PlaceRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return PlaceRecordDialog.createNew(parent, model);
	}

	@Override
	public PlaceRecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return PlaceRecordDialog.createEdit(parent, model, record);
	}

}
