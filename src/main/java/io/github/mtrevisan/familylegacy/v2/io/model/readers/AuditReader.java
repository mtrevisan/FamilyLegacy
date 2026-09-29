package io.github.mtrevisan.familylegacy.v2.io.model.readers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


public final class AuditReader{

	private static final String DOT = ".";

	public static final String AUDIT_CREATION_DATE = FlefTags.AUDIT + DOT + FlefTags.CREATION + DOT + FlefTags.DATE;
	public static final String AUDIT_CREATION_COMMENT = FlefTags.AUDIT + DOT + FlefTags.CREATION + DOT + FlefTags.COMMENT;
	public static final String AUDIT_UPDATE_DATE = FlefTags.AUDIT + DOT + FlefTags.UPDATE + DOT + FlefTags.DATE;
	public static final String AUDIT_UPDATE_COMMENT = FlefTags.AUDIT + DOT + FlefTags.UPDATE + DOT + FlefTags.COMMENT;


	private AuditReader(){}


	public static String creationDate(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, AUDIT_CREATION_DATE);
	}

	public static String creationComment(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, AUDIT_CREATION_COMMENT);
	}

	public static String updateDate(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, AUDIT_UPDATE_DATE);
	}

	public static String updateComment(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, AUDIT_UPDATE_COMMENT);
	}

}
