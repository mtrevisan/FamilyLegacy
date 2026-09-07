package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Predicate;


/**
 * Search strategy for Individual records.
 * Supports event‑based filters: eventType, dateFrom, dateTo, locationContains.
 */
public class IndividualSearchStrategy implements SearchStrategy{

	private static final String TAG_TYPE = "type";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String INDIVIDUAL_TYPE = "individual";
	private static final String EVENT_PARTICIPATION_TYPE = "EVENT_PARTICIPATION";
	private static final String PARTICIPANT = "participant";
	private static final String EVENT = "event";

	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria){
		final String eventType = criteria.getFilter("eventType");
		final String dateFrom = criteria.getFilter("dateFrom");
		final String dateTo = criteria.getFilter("dateTo");
		final String locationContains = criteria.getFilter("locationContains");

		// If no event filters, accept all individuals
		if(StringUtils.isEmpty(eventType) && StringUtils.isEmpty(dateFrom) &&
			StringUtils.isEmpty(dateTo) && StringUtils.isEmpty(locationContains)){
			return record -> true;
		}

		return record -> {
			// Find a matching event for this individual
			return hasMatchingEvent(record, eventType, dateFrom, dateTo, locationContains);
		};
	}

	private boolean hasMatchingEvent(final FLEFRecord individual,
		final String eventType,
		final String dateFrom,
		final String dateTo,
		final String locationContains){
		// Iterate over event participations where this individual is the participant
		final List<FLEFRecord> participations = model.getRecordsByType(EVENT_PARTICIPATION_TYPE);
		for(final FLEFRecord participation : participations){
			final String participantId = participation.extractReferencedId(PARTICIPANT, INDIVIDUAL_TYPE);
			if(!individual.getId().equals(participantId)){
				continue;
			}
			final String eventId = participation.extractReferencedId(EVENT, "EVENT");
			if(eventId == null) continue;
			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null) continue;

			if(matchesEvent(event, eventType, dateFrom, dateTo, locationContains)){
				return true;
			}
		}
		return false;
	}

	private boolean matchesEvent(final FLEFRecord event,
		final String eventType,
		final String dateFrom,
		final String dateTo,
		final String locationContains){
		// Event type
		if(StringUtils.isNotEmpty(eventType)){
			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(!eventType.equalsIgnoreCase(type)){
				return false;
			}
		}
		// Date range
		if(StringUtils.isNotEmpty(dateFrom) || StringUtils.isNotEmpty(dateTo)){
			final String eventDate = FLEFRecordHelper.getChildValue(event, TAG_DATE);
			if(!isDateInRange(eventDate, dateFrom, dateTo)){
				return false;
			}
		}
		// Location
		if(StringUtils.isNotEmpty(locationContains)){
			final String place = FLEFRecordHelper.getChildValue(event, TAG_PLACE);
			if(place == null || !place.toLowerCase().contains(locationContains.toLowerCase())){
				return false;
			}
		}
		return true;
	}

	private boolean isDateInRange(final String dateStr, final String fromStr, final String toStr){
		// Simple year extraction (as before)
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
		// As before
	}

}
