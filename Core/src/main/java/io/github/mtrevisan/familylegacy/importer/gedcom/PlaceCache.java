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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


public class PlaceCache{

	private static final Pattern DEGREES_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");


	private final FLEFModel model;
	private final Map<String, FLEFRecord> cache = new HashMap<>();
	private final Set<String> relationshipCache = new HashSet<>();


	public PlaceCache(FLEFModel model){
		this.model = model;
	}

	/**
	 * Gets or creates a PlaceRecord from a GEDCOM PLAC node.
	 * Parses hierarchical place parts separated by commas, creates individual PlaceRecords
	 * for each level, links them hierarchically, and avoids duplicate or redundant information.
	 * <p>
	 * A place is identified by its name together with the names of the places containing it, so that places with the same name
	 * in different jurisdictions (e.g. "San Martino, Treviso" and "San Martino, Venezia", or the town and the province in
	 * "Treviso, Treviso, Italia") stay distinct.
	 */
	public FLEFRecord getOrCreatePlace(GEDCOMNode placNode){
		String placeName = placNode.getValue();
		if(StringUtils.isBlank(placeName))
			return null;

		// Split place name into its jurisdictions, skipping the empty ones (e.g. ", , Treviso, Italia")
		final List<String> parts = new ArrayList<>();
		for(final String rawPart : StringUtils.split(placeName, ',')){
			final String trimmed = StringUtils.normalizeSpace(rawPart);
			if(!trimmed.isEmpty())
				parts.add(trimmed);
		}
		if(parts.isEmpty())
			return null;

		final String placeKey = String.join(", ", parts).toLowerCase(Locale.ROOT);
		FLEFRecord leafPlace = cache.get(placeKey);
		if(leafPlace == null){
			// Build the place hierarchy by linking each part to its parent container
			FLEFRecord containerPlace = null;
			for(int i = parts.size() - 1; i >= 0; i --){
				final String key = String.join(", ", parts.subList(i, parts.size())).toLowerCase(Locale.ROOT);
				final FLEFRecord place = getOrCreateSinglePlace(key, parts.get(i), placNode);
				if(containerPlace != null)
					linkPlaces(place, containerPlace, placNode);
				containerPlace = place;
			}
			leafPlace = containerPlace;
		}

		// The metadata of a PLAC (FONE, ROMN, LANG, TRAN, MAP, TYPE) is about the most specific place
		attachPlaceMetadata(leafPlace, placNode);
		return leafPlace;
	}

	/**
	 * Gets or creates a single PlaceRecord.
	 *
	 * @param key	The name of the place followed by the names of the places containing it, lowercase.
	 */
	private FLEFRecord getOrCreateSinglePlace(final String key, final String partName, final GEDCOMNode placNode){
		return cache.computeIfAbsent(key, k -> {
			final FLEFRecord place = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceReader.ID_PREFIX), PlaceReader.TYPE)
				// ---- Primary name (text.value) ----
				.addChild(FLEFRecord.createChildWithTag("name")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", partName))
				)
				// ---- Audit ----
				.addChild(AuditBuilder.build(placNode));

			model.addRecord(place);
			return place;
		});
	}

	/** Attaches the variants of the name, the coordinates, and the type, when not already present. */
	private static void attachPlaceMetadata(final FLEFRecord place, final GEDCOMNode placNode){
		final FLEFRecord name = FLEFRecordHelper.findChildren(place, "name").stream()
			.findFirst()
			.orElse(null);
		if(name != null && FLEFRecordHelper.findChildren(name, "variant").isEmpty()){
			// ---- Phonetic variations (FONE) -> name.variant.phonetic ----
			for(final GEDCOMNode fone : GEDCOMHelper.findChildren(placNode, "FONE"))
				addNameVariant(name, "phonetic", fone, "ipa");
			// ---- Romanized variations (ROMN) -> name.variant.transcription ----
			for(final GEDCOMNode romn : GEDCOMHelper.findChildren(placNode, "ROMN"))
				addNameVariant(name, "transcription", romn, "scientific");
		}

		// ---- Language (LANG, GEDCOM 7) -> name.locale ----
		final String locale = GEDCOMHelper.findFirstValue(placNode, "LANG");
		if(name != null && locale != null && FLEFRecordHelper.findChild(name, "locale") == null)
			name.addChild(FLEFRecord.createChildWithTagAndValue("locale", locale));

		// ---- Translations (TRAN, GEDCOM 7) -> name of type translated ----
		for(final GEDCOMNode tran : GEDCOMHelper.findChildren(placNode, "TRAN"))
			addTranslatedName(place, tran);

		// ---- Map coordinates (MAP -> LATI, LONG) ----
		if(FLEFRecordHelper.findChild(place, PlaceReader.TAG_LOCATION) == null){
			final GEDCOMNode mapNode = GEDCOMHelper.findFirstChild(placNode, "MAP");
			final String coordinates = toISO6709(GEDCOMHelper.findFirstValue(mapNode, "LATI"),
				GEDCOMHelper.findFirstValue(mapNode, "LONG"));
			if(coordinates != null){
				final FLEFRecord location = FLEFRecord.createChildWithTag(PlaceReader.TAG_LOCATION)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_COORDINATES, coordinates));
				// the location goes after the name and the type, as in the schema
				place.addChild(location);
			}
		}

		// ---- Type (optional) ----
		if(FLEFRecordHelper.findChild(place, PlaceReader.TAG_TYPE) == null){
			final String type = GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(placNode, "TYPE"));
			if(type != null)
				place.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, type));
		}
	}

	/**
	 * Adds the translation of a place name, if not already there.
	 * <p>
	 * The translation lists the jurisdictions as the PLAC does: its first one is the name of the most specific place.
	 */
	private static void addTranslatedName(final FLEFRecord place, final GEDCOMNode tranNode){
		final String[] jurisdictions = StringUtils.split(StringUtils.defaultString(tranNode.getValue()), ',');
		final String value = (jurisdictions.length > 0? StringUtils.trimToNull(jurisdictions[0]): null);
		if(value == null)
			return;

		final String locale = GEDCOMHelper.findFirstValue(tranNode, "LANG");
		final boolean present = FLEFRecordHelper.findChildren(place, "name").stream()
			.anyMatch(name -> value.equals(FLEFRecordHelper.getChildValue(name, "value"))
				&& Objects.equals(locale, FLEFRecordHelper.getChildValue(name, "locale")));
		if(present)
			return;

		final FLEFRecord name = FLEFRecord.createChildWithTag("name")
			.addChild(FLEFRecord.createChildWithTagAndValue("type", "translated"))
			.addChild(FLEFRecord.createChildWithTagAndValue("value", value));
		if(locale != null)
			name.addChild(FLEFRecord.createChildWithTagAndValue("locale", locale));
		// the names go first, as in the schema
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(place, "name");
		place.getChildren()
			.add(place.getChildren().indexOf(names.getLast()) + 1, name);
	}

	private static void addNameVariant(final FLEFRecord name, final String kind, final GEDCOMNode variantNode,
			final String defaultSystem){
		if(StringUtils.isBlank(variantNode.getValue()))
			return;

		final String system = GEDCOMMapper.toTypeToken(GEDCOMHelper.findFirstValue(variantNode, "TYPE"));
		name.addChild(FLEFRecord.createChildWithTag("variant")
			.addChild(FLEFRecord.createChildWithTag(kind)
				.addChild(FLEFRecord.createChildWithTagAndValue("system", (system != null? system: defaultSystem)))
				.addChild(FLEFRecord.createChildWithTagAndValue("value", variantNode.getValue().trim()))));
	}

	/**
	 * Converts GEDCOM coordinates (e.g. {@code N45.65}, {@code W12.2}) into ISO 6709 (e.g. {@code +45.65-012.2/}).
	 *
	 * @return	The coordinates, or {@code null} if absent or malformed.
	 */
	public static String toISO6709(final String latitude, final String longitude){
		final String lat = toSignedDegrees(latitude, 'N', 'S', 2);
		final String lon = toSignedDegrees(longitude, 'E', 'W', 3);
		return (lat != null && lon != null? lat + lon + "/": null);
	}

	private static String toSignedDegrees(final String value, final char positive, final char negative, final int integerDigits){
		if(StringUtils.isBlank(value))
			return null;

		String text = value.trim().toUpperCase(Locale.ROOT);
		char sign = '+';
		final char hemisphere = text.charAt(0);
		if(hemisphere == positive || hemisphere == negative){
			sign = (hemisphere == negative? '-': '+');
			text = text.substring(1).trim();
		}
		else if(hemisphere == '+' || hemisphere == '-'){
			sign = hemisphere;
			text = text.substring(1).trim();
		}
		if(!DEGREES_PATTERN.matcher(text).matches())
			return null;

		final int dot = text.indexOf('.');
		final String integerPart = (dot >= 0? text.substring(0, dot): text);
		if(integerPart.length() > integerDigits)
			return null;
		return sign + StringUtils.leftPad(integerPart, integerDigits, '0') + (dot >= 0? text.substring(dot): StringUtils.EMPTY);
	}

	/**
	 * Links a child place to its parent place with an administrative relationship if not already linked.
	 */
	private void linkPlaces(final FLEFRecord child, final FLEFRecord parent, final GEDCOMNode placNode){
		final String relKey = child.getId() + "->" + parent.getId();
		if(relationshipCache.add(relKey)){
			final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceRelationshipReader.ID_PREFIX), PlaceRelationshipReader.TYPE)
				.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TYPE, child.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TYPE, parent.getId()))
				)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE, PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF))
				.addChild(AuditBuilder.build(placNode));

			model.addRecord(relationship);
		}
	}

}
