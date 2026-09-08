package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for Place records.
 * Supports filtering by place type and jurisdiction relationship.
 */
public class PlaceSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_PLACE = "place";
	private static final String TAG_MAP = "map";
	private static final String TAG_COORDINATES = "coordinates";

	private static final String TAG_SUBJECT_PLACE = TAG_SUBJECT + DOT + TAG_PLACE;
	private static final String TAG_TARGET_PLACE = TAG_TARGET + DOT + TAG_PLACE;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final PlaceHandler HANDLER = PlaceHandler.getInstance();

	private String placeType;
	private String parentJurisdiction;
	private boolean fuzzy;
	private boolean wholeWord;

	private FLEFModel model;

	// Cache subject place ID -> list of parent place IDs
	private final Map<String, List<String>> parentPlacesMap = new HashMap<>();


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		placeType = criteria.getFilterFor("placeType");
		parentJurisdiction = criteria.getFilterFor("parentJurisdiction");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		this.model = model;

		if(StringUtils.isNotEmpty(parentJurisdiction)){
			precomputeJurisdictionRelationships();
		}

		return place -> {
			// Type filter
			if(StringUtils.isNotEmpty(placeType)){
				final String recordType = FLEFRecordHelper.getChildValue(place, TAG_TYPE);
				if(!placeType.equalsIgnoreCase(recordType)){
					return false;
				}
			}

			// Parent jurisdiction filter
			if(StringUtils.isNotEmpty(parentJurisdiction)){
				if(!matchesParentJurisdiction(place.getId())){
					return false;
				}
			}

			return true;
		};
	}

	private void precomputeJurisdictionRelationships(){
		parentPlacesMap.clear();

		final List<FLEFRecord> relationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord rel : relationships){
			final String subjectPlaceId = rel.extractReferencedId(TAG_SUBJECT_PLACE, PlaceHandler.TYPE);
			final String targetPlaceId = rel.extractReferencedId(TAG_TARGET_PLACE, PlaceHandler.TYPE);

			if(subjectPlaceId != null && targetPlaceId != null){
				parentPlacesMap.computeIfAbsent(subjectPlaceId, k -> new ArrayList<>())
					.add(targetPlaceId);
			}
		}
	}

	private boolean matchesParentJurisdiction(final String placeId){
		final List<String> parentIds = parentPlacesMap.get(placeId);
		if(parentIds == null || parentIds.isEmpty()){
			return false;
		}

		for(final String parentId : parentIds){
			final FLEFRecord parentRecord = model.getRecordById(parentId);
			if(parentRecord != null){
				final String parentName = HANDLER.getDisplayText(parentRecord, model);
				if(TextSearchHelper.matchesText(parentName, parentJurisdiction, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);
		final String coords = FLEFRecordHelper.getChildValue(record, TAG_MAP + DOT + TAG_COORDINATES);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type)){
			details.add(type);
		}
		if(StringUtils.isNotEmpty(coords)){
			details.add("📍 " + coords);
		}

		return baseDisplayText + details;
	}

}
