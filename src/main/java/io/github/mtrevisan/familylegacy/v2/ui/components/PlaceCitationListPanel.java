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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceCitationDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;

import javax.swing.JDialog;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Panel for managing a list of PLACE references (XREF IDs).
 * <p>
 * UNUSED!!!
 */
public class PlaceCitationListPanel extends AbstractCitationListPanel{

	@Serial
	private static final long serialVersionUID = -5998352597761066840L;


	private static final String TAG_PLACE = "PLACE";


	static{
		HandlerRegistry.register(new PlaceHandler());
	}


	public PlaceCitationListPanel(final String path, final Dialog parent, final FLEFModel model){
		this(path, parent, "Places", model);
	}

	public PlaceCitationListPanel(final String path, final Dialog parent, final String borderTitle,
			final FLEFModel model){
		super(path, parent, borderTitle, model, TAG_PLACE, PlaceHandler.TYPE);
	}


	@Override
	protected boolean isDialogSaved(final JDialog dialog){
		if(dialog instanceof PlaceRecordDialog placeDialog)
			return placeDialog.isSaved();
		if(dialog instanceof PlaceCitationDialog citationDialog)
			return citationDialog.isSaved();
		return false;
	}

	@Override
	protected FLEFRecord getRecordFromDialog(final JDialog dialog){
		if(dialog instanceof PlaceRecordDialog placeDialog)
			return placeDialog.getRecord();
		if(dialog instanceof PlaceCitationDialog citationDialog)
			return citationDialog.getRecord();
		return null;
	}

	@Override
	protected JDialog createCitationEditDialog(final FLEFRecord citation){
		return PlaceCitationDialog.createEdit(parent, model, citation);
	}

	@Override
	protected JDialog createTargetEditDialog(final FLEFRecord entity){
		return PlaceRecordDialog.createEdit(parent, model, entity);
	}

}
