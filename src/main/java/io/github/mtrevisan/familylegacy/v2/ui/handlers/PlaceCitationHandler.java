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
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceCitationDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


/**
 * Handler for {@code PLACE_CITATION} entities according to FLEF 0.1.1.
 */
public class PlaceCitationHandler implements RecordTypeHandler<PlaceCitationDialog>{

	/** The record type identifier for groups. */
	public static final String TYPE = "PLACE_CITATION";

	private static final String TAG_PLACE = "PLACE";


	private final PlaceHandler placeHandle = new PlaceHandler();


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return "Place Citation";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIDPrefix(){
		return null;
	}

	@Override
	public PlaceCitationDialog createNewDialog(final Dialog parent, final FLEFModel model){
		return PlaceCitationDialog.createEdit(parent, model, null);
	}

	@Override
	public PlaceCitationDialog createEditDialog(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return PlaceCitationDialog.createEdit(parent, model, record);
	}

	/**
	 * Returns a display name for the given place structure.
	 *
	 * @param record the place structure record
	 * @return a human-readable display name
	 */
	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return StringUtils.EMPTY;

		final String xref = XRefHelper.extractXRef(FLEFRecordHelper.getChildValuesAsString(record, TAG_PLACE));
		final FLEFRecord place = model.getRecordById(xref);
		return placeHandle.getDisplayText(place, model);
	}

}
