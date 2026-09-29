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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.ConclusionRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


public class ConclusionHandler extends AbstractRecordTypeHandler<ConclusionRecordDialog>{

	public static final String TYPE = "CONCLUSION";
	public static final String ID_PREFIX = "CC";


	public static final String TAG_ISSUE = "ISSUE";
	public static final String TAG_PROOF_STATUS = "PROOF_STATUS";
	public static final String TAG_NARRATIVE = "NARRATIVE";
	public static final String TAG_RESOLVES = "RESOLVES";
	public static final String TAG_PREFERRED = "PREFERRED";
	public static final String TAG_RESEARCH = "RESEARCH";
	public static final String TAG_SOURCE = "SOURCE";
	public static final String TAG_PRIVACY = "PRIVACY";
	public static final String TAG_AUDIT = "AUDIT";

	public static final String[] PROOF_STATUSES = new String[]{
		StringUtils.EMPTY,
		"unresearched", "conflicting_evidence", "supported", "proven", "disproven"
	};


	private static final class SingletonHelper{
		private static final ConclusionHandler INSTANCE = new ConclusionHandler();
	}


	public static ConclusionHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.conclusion");
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
		if(record == null)
			return "--";

		String issue = FLEFRecordHelper.getChildValue(record, TAG_ISSUE);
		String proofStatus = FLEFRecordHelper.getChildValue(record, TAG_PROOF_STATUS);
		if(StringUtils.isNotEmpty(issue)){
			String display = GUIHelper.limitTextLength(StringUtils.replaceChars(issue, '\n', '|'));
			if(StringUtils.isNotEmpty(proofStatus))
				display += " [" + proofStatus + "]";
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ConclusionRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return ConclusionRecordDialog.createNew(parent, model);
	}

	@Override
	public ConclusionRecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		return ConclusionRecordDialog.createEdit(parent, model, record);
	}

}
