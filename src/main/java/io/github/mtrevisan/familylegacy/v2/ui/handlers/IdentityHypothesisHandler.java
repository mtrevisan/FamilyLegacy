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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IdentityHypothesisRecordDialog;

import java.awt.Window;
import java.util.List;


/**
 * Handler for IdentityHypothesisRecord.
 */
public class IdentityHypothesisHandler extends AbstractRecordTypeHandler<IdentityHypothesisRecordDialog>{

	public static final String TYPE = "IDENTITY_HYPOTHESIS";
	public static final String ID_PREFIX = "IH";

	private static final String TAG_IDENTITY = "IDENTITY";


	private static final class SingletonHelper{
		private static final IdentityHypothesisHandler INSTANCE = new IdentityHypothesisHandler();
	}


	public static IdentityHypothesisHandler getInstance(){
		return SingletonHelper.INSTANCE;
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
	public String getLabel(){
		return "Identity Hypothesis";
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		String identity1Text = null;
		String identity2Text = null;

		final List<FLEFRecord> identities = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_IDENTITY, model);
		final FLEFRecord identity1 = identities.get(0);
		if(identity1 != null)
			identity1Text = HandlerRegistry.getHandler(identity1.getTag())
				.getDisplayText(identity1, model);

		final FLEFRecord identity2 = identities.get(1);
		if(identity2 != null)
			identity2Text = HandlerRegistry.getHandler(identity2.getTag())
				.getDisplayText(identity2, model);

		if(identity1Text != null && identity2Text != null)
			return identity1Text + " ↔ " + identity2Text;
		return (record.getId() != null? record.getId(): "(unnamed)");
	}

	@Override
	public IdentityHypothesisRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return IdentityHypothesisRecordDialog.createNew(parent, model);
	}

	@Override
	public IdentityHypothesisRecordDialog createEditDialog(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return IdentityHypothesisRecordDialog.createEdit(parent, model, record);
	}

}
