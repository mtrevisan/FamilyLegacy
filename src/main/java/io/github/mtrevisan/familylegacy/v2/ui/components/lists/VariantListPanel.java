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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.VariantHandler;

import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing variants (phonetic and transcription) of a record.
 */
public class VariantListPanel extends EntityReferenceListPanel{

	@Serial
	private static final long serialVersionUID = -298718064629353117L;


	private static final String TAG_VARIANT = "VARIANT";


	public VariantListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model){
		super(path, null, parent, panelTitle, model, VariantHandler.class, RelationType.STRUCTURE,
			null);
	}


	@Override
	public void load(final FLEFRecord record){
		clear();

		final List<FLEFRecord> variants = FLEFRecordHelper.findChildren(record, TAG_VARIANT);
		setItems(variants);
	}

	/**
	 * Saves the current variants to the given record.
	 *
	 * @param record	the record to save to
	 */
	public void save(final FLEFRecord record){
		for(final FLEFRecord item : getItems())
			record.addChild(item);
	}

}
