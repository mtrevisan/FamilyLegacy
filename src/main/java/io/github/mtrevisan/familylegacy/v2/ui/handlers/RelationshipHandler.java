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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.awt.Window;
import java.util.List;


public class RelationshipHandler extends AbstractRecordTypeHandler<RelationshipRecordDialog>{

	public static final String TYPE = "RELATIONSHIP";
	public static final String ID_PREFIX = "RL";

	public static final String TAG_SUBJECT = "SUBJECT";
	public static final String TAG_OBJECT = "OBJECT";
	public static final String TAG_TYPE = "TYPE";
	public static final String TAG_ROLE = "ROLE";
	public static final String TAG_STATUS = "STATUS";
	public static final String TAG_VALID_FROM = "VALID_FROM";
	public static final String TAG_VALID_TO = "VALID_TO";
	public static final String TAG_SOURCE = "SOURCE";
	public static final String TAG_NOTE = "NOTE";
	public static final String TAG_EVIDENCE = "EVIDENCE";
	public static final String TAG_PRIVACY = "PRIVACY";
	public static final String TAG_AUDIT = "AUDIT";

	public static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	public static final String ENUM_TYPE_ADOPTIVE_CHILD = "adoptive_child";
	public static final String ENUM_TYPE_FOSTER_CHILD = "foster_child";
	public static final String ENUM_TYPE_GUARDED_CHILD = "guarded_child";
	public static final String ENUM_TYPE_STEP_CHILD = "step_child";
	public static final String ENUM_TYPE_CIVIL_SPOUSE = "civil_spouse";
	public static final String ENUM_TYPE_RELIGIOUS_SPOUSE = "religious_spouse";
	public static final String ENUM_TYPE_CUSTOMARY_SPOUSE = "customary_spouse";
	public static final String ENUM_TYPE_COHABITING_PARTNER = "cohabiting_partner";
	public static final String ENUM_TYPE_ENGAGED_PARTNER = "engaged_partner";
	public static final String ENUM_TYPE_GROUP_MEMBER = "group_member";
	public static final String ENUM_TYPE_ASSOCIATE = "associate";
	public static final String ENUM_TYPE_PART_OF = "part_of";
	public static final String ENUM_TYPE_ENDS_WITH_CHILD = "_child";
	public static final String ENUM_TYPE_ENDS_WITH_SPOUSE = "_spouse";
	public static final String ENUM_TYPE_ENDS_WITH_PARTNER = "_partner";
	public static final String[] TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD,
		ENUM_TYPE_STEP_CHILD, ENUM_TYPE_CIVIL_SPOUSE, ENUM_TYPE_RELIGIOUS_SPOUSE, ENUM_TYPE_CUSTOMARY_SPOUSE,
		ENUM_TYPE_COHABITING_PARTNER, ENUM_TYPE_ENGAGED_PARTNER, ENUM_TYPE_GROUP_MEMBER, ENUM_TYPE_ASSOCIATE,
		ENUM_TYPE_PART_OF
	};
	public static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD
	};
	public static final String[] INDIVIDUAL_TO_INDIVIDUAL_TYPES = new String[]{
		ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD, ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD,
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	public static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner",
		"associate"
	};
	public static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{"group_member", "associate"};
	public static final String[] GROUP_TO_GROUP_TYPES = new String[]{"part_of", "associate"};
	public static final String[] GROUP_TO_INDIVIDUAL_TYPES = new String[0];
	public static final String[] EMPTY_TYPES = new String[0];
	public static final String[] BIOLOGICAL = new String[]{ENUM_TYPE_BIOLOGICAL_CHILD};
	public static final String[] FAMILY = new String[]{ENUM_TYPE_BIOLOGICAL_CHILD, ENUM_TYPE_ADOPTIVE_CHILD,
		ENUM_TYPE_FOSTER_CHILD, ENUM_TYPE_GUARDED_CHILD, ENUM_TYPE_STEP_CHILD};

	public static final String[] STATUSES = new String[]{
		"active", "ended", "unknown"
	};


	private static final class SingletonHelper{
		private static final RelationshipHandler INSTANCE = new RelationshipHandler();
	}


	public static RelationshipHandler getInstance(){
		return SingletonHelper.INSTANCE;
	}


	@Override
	public String getLabel(){
		return I18N.t("confirmation.exist.record.relationship");
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
	public List<FLEFRecord> findReferences(final FLEFModel model, final String recordId,
			final String parentEntityType){
		return model.getRecordsByType(TYPE);
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final FLEFRecord subject = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_SUBJECT, model)
			.getFirst();
		String subjectDisplayText = "--";
		if(subject != null){
			final RecordTypeHandler<?> subjectHandler = HandlerRegistry.getHandler(subject.getTag());
			subjectDisplayText = subjectHandler.getDisplayText(subject, model);
		}

		final FLEFRecord object = FLEFRecordHelper.extractRecordsFromOneOfReference(record, TAG_OBJECT, model)
			.getFirst();
		String objectDisplayText = "--";
		if(object != null){
			final RecordTypeHandler<?> objectHandler = HandlerRegistry.getHandler(object.getTag());
			objectDisplayText = objectHandler.getDisplayText(object, model);
		}

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String role = FLEFRecordHelper.getChildValue(record, TAG_ROLE);

		final String id = record.getId();

		final StringBuilder sb = new StringBuilder();
		sb.append(subjectDisplayText);
		if(StringUtils.isNotEmpty(role))
			sb.append(" is ")
				.append(role)
				.append(" w.r.t. ");
		else
			sb.append(" is related to ");
		sb.append(objectDisplayText)
			.append(" as ")
			.append(type)
			.append(" [")
			.append(id)
			.append(']');
		return sb.toString();
	}

	@Override
	public RelationshipRecordDialog createNewDialog(final Window parent, final FLEFModel model){
		return RelationshipRecordDialog.createNew(parent, model);
	}

	@Override
	public RelationshipRecordDialog createEditDialog(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return RelationshipRecordDialog.createEdit(parent, model, record);
	}

}
