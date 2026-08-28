package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.v2.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


public class PlaceCache{
	private final FLEFModel model;
	private final Map<String, FLEFRecord> cache = new HashMap<>();

	public PlaceCache(FLEFModel model){
		this.model = model;
	}

	/**
	 * Gets or creates a PlaceRecord from a GEDCOM PLAC node.
	 * Parses subfields: FONE, ROMN, MAP (LATI/LONG) and adds them.
	 */
	public FLEFRecord getOrCreatePlace(GEDCOMNode placNode){
		String placeName = placNode.getValue();
		if(StringUtils.isBlank(placeName)) return null;

		return cache.computeIfAbsent(placeName, name -> {
			FLEFRecord place = FLEFRecord.createMainRecord(IDGenerator.nextId(PlaceHandler.ID_PREFIX), PlaceHandler.TYPE)
				// ---- Primary name (text.value) ----
				.addChild(FLEFRecord.createChildWithTag("name")
					.addChild(FLEFRecord.createChildWithTagAndValue("value", name))
				);

			// ---- Phonetic variations (FONE) -> variant > phonetic ----
			for(GEDCOMNode fone : GEDCOMHelper.findChildren(placNode, "FONE")){
				FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
				FLEFRecord phonetic = FLEFRecord.createChildWithTag("phonetic");
				GEDCOMNode foneType = GEDCOMHelper.findFirstChild(fone, "TYPE");
				String system = (foneType != null && foneType.getValue() != null)? foneType.getValue(): "IPA";
				phonetic.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
				phonetic.addChild(FLEFRecord.createChildWithTagAndValue("value", fone.getValue()));
				variant.addChild(phonetic);
			}

			// ---- Romanized variations (ROMN) -> variant > transcription ----
			for(GEDCOMNode romn : GEDCOMHelper.findChildren(placNode, "ROMN")){
				FLEFRecord variant = FLEFRecord.createChildWithTag("variant");
				FLEFRecord transcription = FLEFRecord.createChildWithTag("transcription");
				GEDCOMNode romnType = GEDCOMHelper.findFirstChild(romn, "TYPE");
				String system = (romnType != null && romnType.getValue() != null)? romnType.getValue(): "scientific";
				transcription.addChild(FLEFRecord.createChildWithTagAndValue("system", system));
				transcription.addChild(FLEFRecord.createChildWithTagAndValue("value", romn.getValue()));
				variant.addChild(transcription);
			}

			// ---- Map coordinates (MAP -> LATI, LONG) ----
			GEDCOMNode mapNode = GEDCOMHelper.findFirstChild(placNode, "MAP");
			if(mapNode != null){
				GEDCOMNode latiNode = GEDCOMHelper.findFirstChild(mapNode, "LATI");
				GEDCOMNode longNode = GEDCOMHelper.findFirstChild(mapNode, "LONG");
				if(latiNode != null && latiNode.getValue() != null &&
					longNode != null && longNode.getValue() != null){
					FLEFRecord map = FLEFRecord.createChildWithTag("map");
					// Coordinates string: "lat long" (ISO 6709 format)
					String coords = latiNode.getValue().trim() + " " + longNode.getValue().trim();
					map.addChild(FLEFRecord.createChildWithTagAndValue("coordinates", coords));
					place.addChild(map);
				}
			}

			// ---- Type (optional) ----
			GEDCOMNode typeNode = GEDCOMHelper.findFirstChild(placNode, "TYPE");
			if(typeNode != null && typeNode.getValue() != null){
				place.addChild(FLEFRecord.createChildWithTagAndValue("type", typeNode.getValue()));
			}

			// ---- Audit ----
			place.addChild(AuditBuilder.build(placNode));

			model.addRecord(place);
			return place;
		});
	}

}
