/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.persistence.db;

import java.util.Map;


public class EntityManager{

	public static final String TABLE_NAME_NOTE = "note";
	public static final String TABLE_NAME_MEDIA_JUNCTION = "media_junction";
	public static final String TABLE_NAME_RESTRICTION = "restriction";
	public static final String TABLE_NAME_LOCALIZED_TEXT_JUNCTION = "localized_text_junction";
	public static final String TABLE_NAME_MODIFICATION = "modification";
	public static final String TABLE_NAME_ASSERTION = "assertion";
	public static final String TABLE_NAME_CITATION = "citation";
	public static final String TABLE_NAME_SOURCE = "source";
	public static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	public static final String TABLE_NAME_CULTURAL_NORM = "cultural_norm";
	public static final String TABLE_NAME_EVENT = "event";
	public static final String TABLE_NAME_EVENT_TYPE = "event_type";
	public static final String TABLE_NAME_EVENT_SUPER_TYPE = "event_super_type";
	public static final String TABLE_NAME_GROUP = "group";
	public static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	public static final String TABLE_NAME_LOCALIZED_PERSON_NAME = "localized_person_name";
	public static final String TABLE_NAME_HISTORIC_DATE = "historic_date";
	public static final String TABLE_NAME_CALENDAR = "calendar";
	public static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	public static final String TABLE_NAME_MEDIA = "media";
	public static final String TABLE_NAME_PERSON = "person";
	public static final String TABLE_NAME_PERSON_NAME = "person_name";
	public static final String TABLE_NAME_PLACE = "place";
	public static final String TABLE_NAME_REPOSITORY = "repository";
	public static final String TABLE_NAME_RESEARCH_STATUS = "research_status";
	public static final String TABLE_NAME_PROJECT = "project";

	public static final String PROTOCOL_NAME_DEFAULT = "Family LEgacy Format";
	public static final String PROTOCOL_VERSION_DEFAULT = "0.0.10";

	public static final String MEDIA_TYPE_LINK = "link";
	public static final String MEDIA_TYPE_PHOTO = "photo";

	public static final String GROUP_ROLE_PARTNER = "partner";
	public static final String GROUP_ROLE_CHILD = "child";
	public static final String GROUP_ROLE_ADOPTEE = "adoptee";

	public static final String LOCALIZED_TEXT_TYPE_NAME = "name";
	public static final String LOCALIZED_TEXT_TYPE_EXTRACT = "extract";

	public static final String RESTRICTION_CONFIDENTIAL = "confidential";
	public static final String RESTRICTION_PUBLIC = "public";

	public static final String EVENT_TYPE_CATEGORY_ADOPTION = "adoption";


	private EntityManager(){}


	public static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	public static String extractRecordCertainty(final Map<String, Object> record){
		return (String)record.get("certainty");
	}

	public static String extractRecordCredibility(final Map<String, Object> record){
		return (String)record.get("credibility");
	}

	public static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	public static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	public static String extractRecordReferenceType(final Map<String, Object> record){
		return (String)record.get("reference_type");
	}

	public static String extractRecordRestriction(final Map<String, Object> record){
		return (String)record.get("restriction");
	}

	public static Integer extractRecordCitationID(final Map<String, Object> record){
		return (Integer)record.get("citation_id");
	}

	public static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	public static Integer extractRecordSourceID(final Map<String, Object> record){
		return (Integer)record.get("source_id");
	}

	public static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
	}

	public static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	public static String extractRecordLocation(final Map<String, Object> record){
		return (String)record.get("location");
	}

	public static String extractRecordExtract(final Map<String, Object> record){
		return (String)record.get("extract");
	}

	public static String extractRecordExtractLocale(final Map<String, Object> record){
		return (String)record.get("extract_locale");
	}

	public static String extractRecordExtractType(final Map<String, Object> record){
		return (String)record.get("extract_type");
	}

	public static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	public static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	public static Integer extractRecordDateStartID(final Map<String, Object> record){
		return (Integer)record.get("date_start_id");
	}

	public static Integer extractRecordDateEndID(final Map<String, Object> record){
		return (Integer)record.get("date_end_id");
	}

	public static Integer extractRecordMediaID(final Map<String, Object> record){
		return (Integer)record.get("media_id");
	}

	public static Integer extractRecordCulturalNormID(final Map<String, Object> record){
		return (Integer)record.get("cultural_norm_id");
	}

	public static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	public static Integer extractRecordTypeID(final Map<String, Object> record){
		return (Integer)record.get("type_id");
	}

	public static Integer extractRecordSuperTypeID(final Map<String, Object> record){
		return (Integer)record.get("super_type_id");
	}

	public static String extractRecordSuperType(final Map<String, Object> record){
		return (String)record.get("super_type");
	}

	public static String extractRecordCategory(final Map<String, Object> record){
		return (String)record.get("category");
	}

	public static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	public static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (record != null? (String)record.get("photo_crop"): null);
	}

	public static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	public static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	public static Integer extractRecordPersonNameID(final Map<String, Object> record){
		return (Integer)record.get("person_name_id");
	}

	public static String extractRecordPersonalName(final Map<String, Object> record){
		return (String)record.get("personal_name");
	}

	public static String extractRecordFamilyName(final Map<String, Object> record){
		return (String)record.get("family_name");
	}

	public static String extractRecordDate(final Map<String, Object> record){
		return (record != null? (String)record.get("date"): null);
	}

	public static String extractRecordDateOriginal(final Map<String, Object> record){
		return (String)record.get("date_original");
	}

	public static Integer extractRecordCalendarOriginalID(final Map<String, Object> record){
		return (Integer)record.get("calendar_original_id");
	}

	public static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	public static String extractRecordTranscription(final Map<String, Object> record){
		return (String)record.get("transcription");
	}

	public static String extractRecordTranscriptionType(final Map<String, Object> record){
		return (String)record.get("transcription_type");
	}

	public static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	public static Integer extractRecordLocalizedTextID(final Map<String, Object> record){
		return (Integer)record.get("localized_text_id");
	}

	public static String extractRecordTitle(final Map<String, Object> record){
		return (String)record.get("title");
	}

	public static String extractRecordPhotoProjection(final Map<String, Object> record){
		return (String)record.get("photo_projection");
	}

	public static String extractRecordNote(final Map<String, Object> record){
		return (String)record.get("note");
	}

	public static String extractRecordName(final Map<String, Object> record){
		return (String)record.get("name");
	}

	public static String extractRecordCoordinate(final Map<String, Object> record){
		return (String)record.get("coordinate");
	}

	public static String extractRecordCoordinateSystem(final Map<String, Object> record){
		return (String)record.get("coordinate_system");
	}

	public static String extractRecordCoordinateCredibility(final Map<String, Object> record){
		return (String)record.get("coordinate_credibility");
	}

	public static String extractRecordCopyright(final Map<String, Object> record){
		return (String)record.get("copyright");
	}

	public static String extractRecordCreationDate(final Map<String, Object> record){
		return (String)record.get("creation_date");
	}

	public static String extractRecordUpdateDate(final Map<String, Object> record){
		return (String)record.get("update_date");
	}

	public static Integer extractRecordRepositoryID(final Map<String, Object> record){
		return (Integer)record.get("repository_id");
	}

	public static String extractRecordStatus(final Map<String, Object> record){
		return (String)record.get("status");
	}

	public static Integer extractRecordPriority(final Map<String, Object> record){
		return (Integer)record.get("priority");
	}

	public static String extractRecordAuthor(final Map<String, Object> record){
		return (String)record.get("author");
	}


	public static void insertRecordID(final Map<String, Object> record, final int id){
		record.put("id", id);
	}

	public static void insertRecordRestriction(final Map<String, Object> record, final String restriction){
		record.put("restriction", restriction);
	}

	public static void insertRecordReferenceTable(final Map<String, Object> record, final String referenceTable){
		record.put("reference_table", referenceTable);
	}

	public static void insertRecordReferenceID(final Map<String, Object> record, final int referenceID){
		record.put("reference_id", referenceID);
	}

	public static void insertRecordCreationDate(final Map<String, Object> record, final String creationDate){
		record.put("creation_date", creationDate);
	}

	public static void insertRecordUpdateDate(final Map<String, Object> record, final String updateDate){
		record.put("update_date", updateDate);
	}

	public static void insertRecordCertainty(final Map<String, Object> record, final String certainty){
		record.put("certainty", certainty);
	}

	public static void insertRecordCredibility(final Map<String, Object> record, final String credibility){
		record.put("credibility", credibility);
	}

	public static void insertRecordRole(final Map<String, Object> record, final String role){
		record.put("role", role);
	}

	public static void insertRecordType(final Map<String, Object> record, final String type){
		record.put("type", type);
	}

	public static void insertRecordLocation(final Map<String, Object> record, final String location){
		record.put("location", location);
	}

	public static void insertRecordExtract(final Map<String, Object> record, final String extract){
		record.put("extract", extract);
	}

	public static void insertRecordExtractLocale(final Map<String, Object> record, final String extractLocale){
		record.put("extract_locale", extractLocale);
	}

	public static void insertRecordExtractType(final Map<String, Object> record, final String extractType){
		record.put("extract_type", extractType);
	}

	public static void insertRecordIdentifier(final Map<String, Object> record, final String identifier){
		record.put("identifier", identifier);
	}

	public static void insertRecordDescription(final Map<String, Object> record, final String description){
		record.put("description", description);
	}

	public static void insertRecordTypeID(final Map<String, Object> record, final Integer typeID){
		record.put("type_id", typeID);
	}

	public static void insertRecordSuperTypeID(final Map<String, Object> record, final Integer superTypeID){
		record.put("super_type_id", superTypeID);
	}

	public static void insertRecordCategory(final Map<String, Object> record, final String category){
		record.put("category", category);
	}

	public static void insertRecordPlaceID(final Map<String, Object> record, final Integer placeID){
		record.put("place_id", placeID);
	}

	public static void insertRecordPhotoID(final Map<String, Object> record, final Integer photoID){
		record.put("photo_id", photoID);
	}

	public static void insertRecordPhotoCrop(final Map<String, Object> record, final String photoCrop){
		record.put("photo_crop", photoCrop);
	}

	public static void insertRecordPersonID(final Map<String, Object> record, final Integer personID){
		record.put("person_id", personID);
	}

	public static void insertRecordPersonNameID(final Map<String, Object> record, final Integer personNameID){
		record.put("person_name_id", personNameID);
	}

	public static void insertRecordDate(final Map<String, Object> record, final String date){
		record.put("date", date);
	}

	public static void insertRecordDateOriginal(final Map<String, Object> record, final String dateOriginal){
		record.put("date_original", dateOriginal);
	}

	public static void insertRecordCalendarOriginalID(final Map<String, Object> record, final Integer calendarOriginalID){
		record.put("calendar_original_id", calendarOriginalID);
	}

	public static void insertRecordPersonalName(final Map<String, Object> record, final String personalName){
		record.put("personal_name", personalName);
	}

	public static void insertRecordFamilyName(final Map<String, Object> record, final String familyName){
		record.put("family_name", familyName);
	}

	public static void insertRecordLocale(final Map<String, Object> record, final String locale){
		record.put("locale", locale);
	}

	public static void insertRecordTranscription(final Map<String, Object> record, final String transcription){
		record.put("transcription", transcription);
	}

	public static void insertRecordTranscriptionType(final Map<String, Object> record, final String transcriptionType){
		record.put("transcription_type", transcriptionType);
	}

	public static void insertRecordText(final Map<String, Object> record, final String text){
		record.put("text", text);
	}

	public static void insertRecordReferenceType(final Map<String, Object> record, final String referenceType){
		record.put("reference_type", referenceType);
	}

	public static void insertRecordLocalizedTextID(final Map<String, Object> record, final int localizedTextID){
		record.put("localized_text_id", localizedTextID);
	}

	public static void insertRecordTitle(final Map<String, Object> record, final String title){
		record.put("title", title);
	}

	public static void insertRecordPhotoProjection(final Map<String, Object> record, final String photoProjection){
		record.put("photo_projection", photoProjection);
	}

	public static void insertRecordPhotoMediaID(final Map<String, Object> record, final int mediaID){
		record.put("media_id", mediaID);
	}

	public static void insertRecordNote(final Map<String, Object> record, final String note){
		record.put("note", note);
	}

	public static void insertRecordName(final Map<String, Object> record, final String name){
		record.put("name", name);
	}

	public static void insertRecordCoordinate(final Map<String, Object> record, final String coordinate){
		record.put("coordinate", coordinate);
	}

	public static void insertRecordCoordinateSystem(final Map<String, Object> record, final String coordinateSystem){
		record.put("coordinate_system", coordinateSystem);
	}

	public static void insertRecordCoordinateCredibility(final Map<String, Object> record, final String coordinateCredibility){
		record.put("coordinate_credibility", coordinateCredibility);
	}

	public static void insertRecordProtocolName(final Map<String, Object> record, final String protocolName){
		record.put("protocol_name", protocolName);
	}

	public static void insertRecordProtocolVersion(final Map<String, Object> record, final String protocolVersion){
		record.put("protocol_version", protocolVersion);
	}

	public static void insertRecordCopyright(final Map<String, Object> record, final String copyright){
		record.put("copyright", copyright);
	}

	public static void insertRecordStatus(final Map<String, Object> record, final String status){
		record.put("status", status);
	}

	public static void insertRecordPriority(final Map<String, Object> record, final Integer priority){
		record.put("priority", priority);
	}

	public static void insertRecordAuthor(final Map<String, Object> record, final String author){
		record.put("author", author);
	}

	public static void insertRecordRepositoryID(final Map<String, Object> record, final Integer repositoryID){
		record.put("repository_id", repositoryID);
	}

	public static void insertRecordDateStartID(final Map<String, Object> record, final Integer dateStartID){
		record.put("date_start_id", dateStartID);
	}

	public static void insertRecordDateEndID(final Map<String, Object> record, final Integer dateEndID){
		record.put("date_end_id", dateEndID);
	}

	public static void insertRecordSourceID(final Map<String, Object> record, final Integer sourceID){
		record.put("source_id", sourceID);
	}

	public static void insertRecordCitationID(final Map<String, Object> record, final Integer citationID){
		record.put("citation_id", citationID);
	}

	public static void insertRecordDateID(final Map<String, Object> record, final Integer dateID){
		record.put("date_id", dateID);
	}

	public static void insertRecordCulturalNormID(final Map<String, Object> record, final Integer culturalNormID){
		record.put("cultural_norm_id", culturalNormID);
	}

	public static void insertRecordGroupID(final Map<String, Object> record, final Integer groupID){
		record.put("group_id", groupID);
	}

	public static void insertRecordMediaID(final Map<String, Object> record, final Integer mediaID){
		record.put("media_id", mediaID);
	}

}
