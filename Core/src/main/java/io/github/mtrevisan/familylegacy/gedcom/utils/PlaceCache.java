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
package io.github.mtrevisan.familylegacy.gedcom.utils;

import io.github.mtrevisan.familylegacy.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class PlaceCache{

	private final FLEFModel model;
	private final Map<String, FLEFRecord> cache = new HashMap<>();
	private final Map<String, FLEFRecord> placeNameCache = new HashMap<>();
	private final Set<String> relationshipCache = new HashSet<>();


	public PlaceCache(FLEFModel model){
		this.model = model;
	}

	/**
	 * Gets or creates a PlaceRecord from a GEDCOM PLAC node.
	 * Parses hierarchical place parts separated by commas, creates individual PlaceRecords
	 * for each level, links them hierarchically, and avoids duplicate or redundant information.
	 */
	public FLEFRecord getOrCreatePlace(GEDCOMNode placNode){
		String placeName = placNode.getValue();
		if(StringUtils.isEmpty(placeName))
			return null;

		return cache.computeIfAbsent(placeName, name -> {
			// Split place name into clean, non-redundant parts (removing adjacent duplicates)
			final List<String> parts = new ArrayList<>();
			for(final String rawPart : placeName.split("\\s*,\\s*")){
				final String trimmed = rawPart.trim();
				if(!trimmed.isEmpty() && (parts.isEmpty() || !parts.get(parts.size() - 1).equalsIgnoreCase(trimmed)))
					parts.add(trimmed);
			}

			if(parts.isEmpty())
				return null;

			// Get or create the most specific (leaf) place with full metadata (FONE, ROMN, MAP, TYPE)
			final FLEFRecord leafPlace = getOrCreateSinglePlace(parts.get(0), placNode, true);

			// Build the place hierarchy by linking each part to its parent container
			FLEFRecord currentPlace = leafPlace;
			for(int i = 0; i < parts.size() - 1; i++){
				final FLEFRecord parentPlace = getOrCreateSinglePlace(parts.get(i + 1), placNode, false);
				linkPlaces(currentPlace, parentPlace, placNode);
				currentPlace = parentPlace;
			}

			return leafPlace;
		});
	}

	/**
	 * Gets or creates a single PlaceRecord for a specific part name.
	 * Specific metadata (coordinates, phonetic/romanized variants, type) is attached only to the leaf place.
	 */
	private FLEFRecord getOrCreateSinglePlace(final String partName, final GEDCOMNode placNode, final boolean isLeaf){
		final String cacheKey = partName.toLowerCase(Locale.ROOT);
		return placeNameCache.computeIfAbsent(cacheKey, key -> {
			final FLEFRecord place = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceHandler.ID_PREFIX), PlaceHandler.TYPE)
				// ---- Primary name (text.value) ----
				.addChild(FLEFRecord.createChildWithTag("name")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", partName))
				);

			if(isLeaf){
				// ---- Phonetic variations (FONE) -> variant > phonetic ----
				for(final GEDCOMNode fone : GEDCOMHelper.findChildren(placNode, "FONE")){
					final FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
					final FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
					final GEDCOMNode foneType = GEDCOMHelper.findFirstChild(fone, "TYPE");
					final String system = (foneType != null && foneType.getValue() != null)? foneType.getValue(): "IPA";
					phonetic.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
					phonetic.addChild(FLEFRecord.createChildWithTagAndValue("value", fone.getValue()));
					variant.addChild(phonetic);
					place.addChild(variant);
				}

				// ---- Romanized variations (ROMN) -> variant > transcription ----
				for(final GEDCOMNode romn : GEDCOMHelper.findChildren(placNode, "ROMN")){
					final FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
					final FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
					final GEDCOMNode romnType = GEDCOMHelper.findFirstChild(romn, "TYPE");
					final String system = (romnType != null && romnType.getValue() != null)? romnType.getValue(): "scientific";
					transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
					transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
					variant.addChild(transcription);
					place.addChild(variant);
				}

				// ---- Map coordinates (MAP -> LATI, LONG) ----
				final GEDCOMNode mapNode = GEDCOMHelper.findFirstChild(placNode, "MAP");
				if(mapNode != null){
					final GEDCOMNode latiNode = GEDCOMHelper.findFirstChild(mapNode, "LATI");
					final GEDCOMNode longNode = GEDCOMHelper.findFirstChild(mapNode, "LONG");
					if(latiNode != null && latiNode.getValue() != null &&
						longNode != null && longNode.getValue() != null){
						final FLEFRecord map = FLEFRecord.createChildWithTag(PlaceReader.TAG_MAP);
						// Coordinates string: "lat long" (ISO 6709 format)
						final String coords = latiNode.getValue().trim() + StringUtils.SPACE + longNode.getValue().trim();
						map.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_COORDINATES, coords));
						place.addChild(map);
					}
				}

				// ---- Type (optional) ----
				final GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(placNode, "TYPE");
				if(typeNode != null && typeNode.getValue() != null)
					place.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, typeNode.getValue()));
			}

			// ---- Audit ----
			place.addChild(AuditBuilder.build(placNode));

			model.addRecord(place);
			return place;
		});
	}

	/**
	 * Links a child place to its parent place with an administrative relationship if not already linked.
	 */
	private void linkPlaces(final FLEFRecord child, final FLEFRecord parent, final GEDCOMNode placNode){
		final String relKey = child.getId() + "->" + parent.getId();
		if(relationshipCache.add(relKey)){
			final FLEFRecord relationship = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceRelationshipHandler.ID_PREFIX), PlaceRelationshipHandler.TYPE)
				.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceHandler.TYPE, child.getId()))
				)
				.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_OBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(PlaceHandler.TYPE, parent.getId()))
				)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE, PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF))
				.addChild(AuditBuilder.build(placNode));

			model.addRecord(relationship);
		}
	}

}
