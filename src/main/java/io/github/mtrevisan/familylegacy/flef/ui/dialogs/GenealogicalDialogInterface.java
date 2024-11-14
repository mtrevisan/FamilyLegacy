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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;


import java.util.Map;


public interface GenealogicalDialogInterface{

	int COMPONENT_ID_ASSERTION_BUTTON = "assertion".hashCode();
	int COMPONENT_ID_CITATION_BUTTON = "citation".hashCode();
	int COMPONENT_ID_SOURCE_BUTTON = "source".hashCode();

	int COMPONENT_ID_DATE_BUTTON = "date".hashCode();
	int COMPONENT_ID_DATE_START_BUTTON = "dateStart".hashCode();
	int COMPONENT_ID_DATE_END_BUTTON = "dateEnd".hashCode();
	int COMPONENT_ID_CALENDAR_ORIGINAL_BUTTON = "calendarOriginal".hashCode();

	int COMPONENT_ID_PLACE_BUTTON = "place".hashCode();

	int COMPONENT_ID_TRANSCRIBED_EXTRACT_BUTTON = "transcribedExtract".hashCode();
	int COMPONENT_ID_TRANSCRIBED_NAME_BUTTON = "transcribedName".hashCode();
	int COMPONENT_ID_NOTE_BUTTON = "note".hashCode();

	int COMPONENT_ID_MEDIA_BUTTON = "media".hashCode();
	int COMPONENT_ID_PHOTO_BUTTON = "photo".hashCode();
	int COMPONENT_ID_PHOTO_CROP_BUTTON = "photoCrop".hashCode();
	int COMPONENT_ID_OPEN_FOLDER_BUTTON = "openFolder".hashCode();
	int COMPONENT_ID_OPEN_LINK_BUTTON = "openLink".hashCode();
	int COMPONENT_ID_FILE_BUTTON = "file".hashCode();

	int COMPONENT_ID_REFERENCE_PERSON_BUTTON = "referencePerson".hashCode();
	int COMPONENT_ID_PERSON_NAME_BUTTON = "personName".hashCode();

	int COMPONENT_ID_GROUP_BUTTON = "group".hashCode();
	int COMPONENT_ID_PERSON_GROUP_BUTTON = "personGroup".hashCode();
	int COMPONENT_ID_GROUP_GROUP_BUTTON = "groupGroup".hashCode();
	int COMPONENT_ID_PLACE_GROUP_BUTTON = "placeGroup".hashCode();

	int COMPONENT_ID_EVENT_BUTTON = "event".hashCode();
	int COMPONENT_ID_ADD_TYPE_BUTTON = "addType".hashCode();
	int COMPONENT_ID_REMOVE_TYPE_BUTTON = "removeType".hashCode();

	int COMPONENT_ID_CULTURAL_NORM_BUTTON = "culturalNorm".hashCode();


	String getTableName();

	boolean isViewOnlyComponent(int componentID);

	void refreshButtonStates(int recordID);

	Map<String, Object> extractRelationshipData(int collectionID, int dataColumnIndex);

}
