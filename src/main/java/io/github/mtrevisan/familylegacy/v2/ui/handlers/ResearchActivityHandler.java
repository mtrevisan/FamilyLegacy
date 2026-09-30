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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.ResearchActivityRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;


/**
 * Handler for ResearchActivityRecord.
 */
public class ResearchActivityHandler extends AbstractRecordTypeHandler<ResearchActivityRecordDialog>{

	public static final String TYPE = "RESEARCH_ACTIVITY";
	public static final String ID_PREFIX = "RA";

	public static final String TAG_QUESTION = "QUESTION";
	public static final String TAG_ACTIVITY_TYPE = "ACTIVITY_TYPE";
	public static final String TAG_STATUS = "STATUS";
	public static final String TAG_ACTION = "ACTION";
	public static final String TAG_TARGET = "TARGET";
	public static final String TAG_SEARCH_SCOPE = "SEARCH_SCOPE";
	public static final String TAG_TYPE = "TYPE";
	public static final String TAG_DETAIL = "DETAIL";
	public static final String TAG_RESULT = "RESULT";
	public static final String TAG_OBSERVATION = "OBSERVATION";
	public static final String TAG_CONCLUSION = "CONCLUSION";
	public static final String TAG_CONCLUSION_CONFIDENCE = "CONCLUSION_CONFIDENCE";
	public static final String TAG_SOURCE = "SOURCE";
	public static final String TAG_PARENT_ACTIVITY = "PARENT_ACTIVITY";
	public static final String TAG_TASK = "TASK";
	public static final String TAG_PRIVACY = "PRIVACY";
	public static final String TAG_AUDIT = "AUDIT";

	public static final String[] TYPES = new String[]{
		"search", "review", "analysis", "correspondence", "interview", "hypothesis"
	};
	public static final String[] STATUSES = new String[]{
		"planned", "in_progress", "completed", "abandoned"
	};
	public static final String[] SEARCH_SCOPES = new String[]{
		"entire_source",
		"index_only",
		"partial_source",
		"selected_entries"
	};
	public static final String[] RESULTS = new String[]{
		"positive", "negative", "inconclusive", "conflicting", "unavailable"
	};
	public static final String[] CONFIDENCES = new String[]{
		"low", "medium", "high"
	};


	private static final class SingletonHelper{
		private static final ResearchActivityHandler INSTANCE = new ResearchActivityHandler();
	}


	public static ResearchActivityHandler getInstance(){
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
		return I18N.t("confirmation.exist.record.research.activity");
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		if(record == null)
			return "--";

		final String action = FLEFRecordHelper.getChildValue(record, TAG_ACTION);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_ACTIVITY_TYPE);
		if(StringUtils.isNotEmpty(action)){
			String display = GUIHelper.limitTextLength(StringUtils.replaceChars(action, '\n', '|'));
			if(StringUtils.isNotEmpty(type))
				display += " [" + type + "]";
			return display;
		}
		return record.getId() != null? record.getId(): "(unnamed)";
	}

	@Override
	public ResearchActivityRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return ResearchActivityRecordDialog.createNew(parent, model);
	}

	@Override
	public ResearchActivityRecordDialog createEditDialog(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return ResearchActivityRecordDialog.createEdit(parent, model, record);
	}

}
