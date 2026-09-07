package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Service for performing advanced searches on any record type,
 * with optional event‑based filters for individuals.
 */
public class AdvancedSearchService{

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";

	private static final double TRIGRAM_THRESHOLD = 0.05;

	/**
	 * Performs the search according to the given criteria.
	 *
	 * @param model    the FLEF model
	 * @param criteria the search criteria
	 * @return a list of search results (each containing a record and an optional matching event)
	 */
	public List<SearchResult> search(final FLEFModel model, final AdvancedSearchCriteria criteria){
		final List<SearchResult> results = new ArrayList<>();

		if(model == null || criteria == null || criteria.handlerType() == null){
			return results;
		}

		final RecordTypeHandler<?> handler = criteria.handlerType();
		final List<FLEFRecord> records = model.getRecordsByType(handler.getType());

		// Check if we need event‑based filtering (only for individuals)
		final boolean isIndividual = handler.getType().equals(IndividualHandler.TYPE);
		final boolean hasEventFilters = hasEventFilters(criteria);

		for(final FLEFRecord record : records){
			final String displayText = handler.getDisplayText(record, model);

			// First, apply text matching
			final boolean textMatches = TextSearchUtils.matchesText(
				displayText,
				criteria.searchText(),
				criteria.fuzzy(),
				criteria.wholeWord(),
				TRIGRAM_THRESHOLD
			);
			if(!textMatches) continue;

			// If event filters are specified and we are searching individuals,
			// we must find at least one matching event.
			FLEFRecord matchingEvent = null;
			if(isIndividual && hasEventFilters){
				matchingEvent = findMatchingEvent(model, record, criteria);
				if(matchingEvent == null){
					continue; // no matching event, skip this record
				}
			}

			results.add(new SearchResult(record, matchingEvent));
		}

		return results;
	}

	private boolean hasEventFilters(final AdvancedSearchCriteria criteria){
		return StringUtils.isNotEmpty(criteria.eventType())
			|| StringUtils.isNotEmpty(criteria.dateFrom())
			|| StringUtils.isNotEmpty(criteria.dateTo())
			|| StringUtils.isNotEmpty(criteria.locationContains());
	}

	private FLEFRecord findMatchingEvent(final FLEFModel model,
		final FLEFRecord individual,
		final AdvancedSearchCriteria criteria){
		final List<FLEFRecord> participations = model.getRecordsByType("EVENT_PARTICIPATION");
		for(final FLEFRecord participation : participations){
			final String participantId = participation.extractReferencedId("participant", IndividualHandler.TYPE);
			if(!individual.getId().equals(participantId)){
				continue;
			}

			final String eventId = participation.extractReferencedId("event", "EVENT");
			if(eventId == null) continue;
			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null) continue;

			if(eventMatches(event, criteria)){
				return event;
			}
		}
		return null;
	}

	private boolean eventMatches(final FLEFRecord event, final AdvancedSearchCriteria criteria){
		// Event type filter
		if(StringUtils.isNotEmpty(criteria.eventType())){
			final String eventType = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(!criteria.eventType().equalsIgnoreCase(eventType)){
				return false;
			}
		}

		// Date range
		final String eventDate = FLEFRecordHelper.getChildValue(event, TAG_DATE);
		if(StringUtils.isNotEmpty(criteria.dateFrom()) || StringUtils.isNotEmpty(criteria.dateTo())){
			if(!isDateInRange(eventDate, criteria.dateFrom(), criteria.dateTo())){
				return false;
			}
		}

		// Location contains
		if(StringUtils.isNotEmpty(criteria.locationContains())){
			final String location = FLEFRecordHelper.getChildValue(event, TAG_PLACE);
			if(location == null || !location.toLowerCase().contains(criteria.locationContains().toLowerCase())){
				return false;
			}
		}

		return true;
	}

	private boolean isDateInRange(final String dateStr, final String fromStr, final String toStr){
		if(StringUtils.isEmpty(dateStr)) return false;
		final int dateYear = extractYear(dateStr);
		if(dateYear == -1) return false;

		if(StringUtils.isNotEmpty(fromStr)){
			final int fromYear = extractYear(fromStr);
			if(fromYear == -1 || dateYear < fromYear) return false;
		}
		if(StringUtils.isNotEmpty(toStr)){
			final int toYear = extractYear(toStr);
			if(toYear == -1 || dateYear > toYear) return false;
		}
		return true;
	}

	private int extractYear(final String dateStr){
		if(StringUtils.isEmpty(dateStr)) return -1;
		final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(\\d{4})\\b");
		final java.util.regex.Matcher matcher = pattern.matcher(dateStr);
		if(matcher.find()){
			return Integer.parseInt(matcher.group(1));
		}
		return -1;
	}

	/**
	 * Search result: a record and optionally the matching event (if any).
	 */
	public record SearchResult(FLEFRecord record, FLEFRecord matchingEvent){
		public String getDisplayText(final FLEFModel model, final RecordTypeHandler<?> handler){
			final String base = handler.getDisplayText(record, model);
			if(matchingEvent != null){
				final String eventType = FLEFRecordHelper.getChildValue(matchingEvent, "type");
				final String date = FLEFRecordHelper.getChildValue(matchingEvent, "date");
				final String place = FLEFRecordHelper.getChildValue(matchingEvent, "place");
				return base + " (" + (eventType != null? eventType: "") +
					(date != null? " " + date: "") +
					(place != null? " - " + place: "") + ")";
			}
			return base;
		}
	}

}
