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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.HeaderDialog;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;

import java.awt.Window;


/**
 * Handler for HEADER records.
 */
public class HeaderHandler extends AbstractRecordTypeHandler<HeaderDialog>{

	public static final String TYPE = "HEADER";

	public static final String TAG_PROTOCOL = "PROTOCOL";
	public static final String TAG_NAME = "NAME";
	public static final String TAG_VERSION = "VERSION";
	public static final String TAG_SOURCE = "SOURCE";
	public static final String TAG_ORGANIZATION = "ORGANIZATION";
	public static final String TAG_DATE = "DATE";
	public static final String TAG_COPYRIGHT = "COPYRIGHT";
	public static final String TAG_SUBMITTER = "SUBMITTER";
	public static final String TAG_CONTACT = "CONTACT";
	public static final String TAG_NOTE = "NOTE";
	public static final String TAG_SCOPE = "SCOPE";


	private static final class SingletonHelper{
		private static final HeaderHandler INSTANCE = new HeaderHandler();
	}


	public static HeaderHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public boolean isTopLevelEntity(){
		return false;
	}

	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.header");
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		return getLabel();
	}

	@Override
	public HeaderDialog createNewDialog(final Window parent, final FLEFModel model){
		return createEditDialog(parent, model, null);
	}

	@Override
	public HeaderDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return new HeaderDialog(parent, model, null);
	}

}
