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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.util.List;


/**
 * Handler for GROUP records.
 * <p>
 * Structure:
 * <pre>
 * // A group of genealogical entities. A group can be a family, a household, a neighborhood, a club, a research group, etc. Membership in a GROUP is
 * // determined exclusively through RELATIONSHIP_RECORD objects whose OBJECT references the GROUP record and whose TYPE identifies a group membership
 * // relationship (e.g., 'group_member'). GROUP_RECORD does not directly contain a member list. This design ensures that membership is treated as a
 * // relationship with its own properties (dates, evidence, conclusions).
 * record GroupRecord {
 *   name*: NameStructure   // names associated with the group. Different names may represent historical, official, customary, translated, abbreviated, or otherwise variant forms used at different times or in different contexts.
 *   type?: enum {
 *     family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe
 *   } | Text
 *   source*: SourceCitation
 *   note*: NoteStructure
 *   preferred_image?: struct {
 *     uri: Uri          // Resource URI pointing to a preferred image of this individual. FIXME? document: Xref&lt;DocumentRecord&gt;
 *     crop?: CropRect   // specifies the portion of the image that should be displayed as the preferred representation
 *   }
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class GroupReader{

	public static final String TAG_NAME = "name";
	public static final String TAG_TYPE = "type";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_NOTE = "note";
	public static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_PREFERRED_IMAGE_URI = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, "uri");
	private static final String TAG_PREFERRED_IMAGE_CROP = FLEFRecordHelper.composePath(TAG_PREFERRED_IMAGE, "crop");
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String[] TYPES = new String[]{
		"family", "household", "neighbourhood", "fraternity", "club", "literary_society",
		"association", "organisation", "tribe"
	};


	private GroupReader(){}


	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}


	public static String extractPreferredImageUri(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PREFERRED_IMAGE_URI);
	}

	public static Rectangle extractPreferredImageCrop(final FLEFRecord record){
		final FLEFRecord crop = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE_CROP);
		return CropReader.extractPreferredImageCrop(crop);
	}


	/**
	 * Extracts names from an GroupRecord.
	 *
	 * @param group the GroupRecord
	 * @return a list of name strings with excluded parts omitted
	 */
	public static List<String> extractNames(final FLEFRecord group){
		final List<Name> names = NameAnatomyService.extractForGeneric(group);
		return names.stream()
			.map(Name::value)
			.filter(StringUtils::isNotEmpty)
			.toList();
	}

	public static String extractPrimaryName(final FLEFRecord group){
		final List<Name> names = NameAnatomyService.extractForGeneric(group);
		final Name officialName = names.stream()
			.filter(n -> NameReader.ENUM_TYPE_OFFICIAL.equals(n.type()))
			.findFirst()
			.orElse(null);
		if(officialName != null)
			return officialName.value();

		final List<String> rawNames = extractNames(group);
		return (!rawNames.isEmpty()? rawNames.getFirst(): null);
	}

	public static String extractSource(final FLEFRecord group){
		return FLEFRecordHelper.getChildValue(group, TAG_SOURCE);
	}

}
