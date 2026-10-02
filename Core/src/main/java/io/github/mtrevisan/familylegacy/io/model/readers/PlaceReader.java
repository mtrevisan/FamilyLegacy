package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.io.model.readers.names.NameAnatomyService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * Handler for PLACE records.
 * <p>
 * Structure:
 * <pre>
 * // A geographical, administrative, ecclesiastical, cadastral, historical, or physical location. Places may be linked together using PLACE_RELATIONSHIP
 * // records in order to model changing jurisdictions and historical geography.
 * record PlaceRecord {
 *   name+: NameStructure   // the name of the place formatted for display and address generation
 *   type?: enum {
 *     address, building, street, hamlet, village, town, municipality, city,
 *     metropolitan_area, county, province, department, district, region,
 *     macro_region, country, empire, parish, diocese, cemetery, archive, unknown
 *   } | Text
 *   map?: struct {         // Contains the location of a place in a Global Positioning System.
 *     coordinates: Coord   // geographic coordinates of the place
 *     evidence?: EvidenceQualifiers
 *   }
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class PlaceReader{

	public static final String TAG_NAME = "name";
	public static final String TAG_TYPE = "type";
	public static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_EVIDENCE = "evidence";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	public static final String TAG_MAP_COORDINATES = FLEFRecordHelper.composePath(TAG_MAP, TAG_COORDINATES);
	public static final String TAG_MAP_EVIDENCE = FLEFRecordHelper.composePath(TAG_MAP, TAG_EVIDENCE);

	public static final String[] TYPES = new String[]{
		"address", "building", "street", "hamlet", "village", "town", "municipality", "city", "metropolitan_area",
		"county", "province", "department", "district", "region", "macro_region", "country", "empire", "parish",
		"diocese", "cemetery", "archive", "unknown"
	};


	private PlaceReader(){}


	/**
	 * Extracts names from an PlaceRecord.
	 *
	 * @param place the PlaceRecord
	 * @return a list of name strings with excluded parts omitted
	 */
	public static List<String> extractNames(final FLEFRecord place){
		final List<Name> names = NameAnatomyService.extractForGeneric(place);
		return names.stream()
			.map(Name::value)
			.filter(StringUtils::isNotEmpty)
			.map(name -> name.replace(StringUtils.LF, ", "))
			.toList();
	}

	public static String extractPrimaryName(final FLEFRecord place){
		final List<String> names = extractNames(place);
		return (!names.isEmpty()? names.getFirst(): null);
	}

	public static String extractType(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_TYPE);
	}

	/**
	 * Extracts the coordinates of a place from the nested
	 * {@code map.coordinates} structure, or {@code null} when missing.
	 */
	public static String extractCoordinates(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_MAP_COORDINATES);
	}

}
