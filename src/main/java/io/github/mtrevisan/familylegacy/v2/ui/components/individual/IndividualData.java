package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;


/**
 * Extracts display information for an individual from a FLEFModel.
 */
public final class IndividualData{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_EVENT = "event";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
	private static final String TAG_EVENT_PARTICIPATION = "event_participation";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_ORIGINAL_TEXT = "original_text";
	private static final String TAG_POINT = "point";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_CALENDAR = "calendar";

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";

	private static final String NO_DATA = "?";
	private static final String[] NO_NAME = {NO_DATA, NO_DATA};

	private static final ImageIcon ADD_PHOTO = ResourceHelper.getOriginalImage("/images/add_photo.jpg");

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;


	/**
	 * Represents a single event (birth or death) with date and place.
	 */
	public static class EventInfo{
		public final String type;          // "birth" or "death"
		public final String rawDate;    // original date string (for display)
		public final ParsedGenealogicalDate date;
		public final String place;         // place name or original_text
		public final boolean approximate;  // whether the date is approximate

		public EventInfo(final String type, final String rawDate, final ParsedGenealogicalDate date,
				final String place, final boolean approximate){
			this.type = type;
			this.rawDate = rawDate;
			this.date = date;
			this.place = place;
			this.approximate = approximate;
		}
	}


	private final FLEFModel model;


	private final String individualNameText;
	private String individualNameTooltip;
	private final String infoText;
	private final String infoTooltip;
	private final ImageIcon individualImage;


	public IndividualData(final FLEFRecord individual, final BoxPanelType boxType, final FLEFModel model){
		this.model = model;

		final List<String> names = extractFullNames(individual);
		individualNameText = names.getFirst();

		if(!names.isEmpty())
			individualNameTooltip = TAG_HTML_OPEN + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;


		// extract events
		final List<EventInfo> events = extractEvents(individual);
		final EventInfo birthInfo = events.stream()
			.filter(e -> "birth".equals(e.type) && e.date != null)
			.min(Comparator.comparing(e -> e.date.isoDate()))
			.orElse(null);
		final EventInfo deathInfo = events.stream()
			.filter(e -> "death".equals(e.type) && e.date != null)
			.max(Comparator.comparing(e -> e.date.isoDate()))
			.orElse(null);

		// ---- Birth/Death summary ----
		final String birthYear = (birthInfo != null && birthInfo.date != null? String.valueOf(birthInfo.date.isoDate().getYear()): NO_DATA);
		final String deathYear = (deathInfo != null && deathInfo.date != null? String.valueOf(deathInfo.date.isoDate().getYear()): NO_DATA);
		String age = null;
		if(birthInfo != null && deathInfo != null && birthInfo.date != null && deathInfo.date != null){
			final long years = ChronoUnit.YEARS.between(birthInfo.date.isoDate(), deathInfo.date.isoDate());
			String prefix = StringUtils.EMPTY;
			if(birthInfo.approximate || deathInfo.approximate)
				prefix = "~";
			if(birthInfo.approximate && birthInfo.date.isoDate().isBefore(deathInfo.date.isoDate()))
				prefix = "< ~";
			age = prefix + years;
		}

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(birthYear);
		sj.add("–");
		sj.add(deathYear);
		if(age != null)
			sj.add("(" + age + " y/o)");

		final StringJoiner toolTipSJ = new StringJoiner(StringUtils.EMPTY);
		final String birthPlace = (birthInfo != null? birthInfo.place: null);
		final String deathPlace = (deathInfo != null? deathInfo.place: null);
		if(birthPlace != null || deathPlace != null){
			toolTipSJ.add("<html>");
			toolTipSJ.add(birthInfo != null? birthInfo.rawDate: NO_DATA);
			if(birthPlace != null)
				toolTipSJ.add("<br>" + birthPlace);
			toolTipSJ.add("<br>-<br>");
			toolTipSJ.add(deathInfo != null? deathInfo.rawDate: NO_DATA);
			if(deathPlace != null)
				toolTipSJ.add("<br>" + deathPlace);
			toolTipSJ.add("</html>");
		}
		else{
			toolTipSJ.add(birthInfo != null? birthInfo.rawDate: NO_DATA);
			toolTipSJ.add(" – ");
			toolTipSJ.add(deathInfo != null? deathInfo.rawDate: NO_DATA);
		}
		infoText = sj.toString();
		infoTooltip = toolTipSJ.toString();

		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		final int height = (int)Math.ceil(PREFERRED_IMAGE_WIDTH * IMAGE_ASPECT_RATIO / shrinkFactor);
		final ImageIcon img = extractPreferredImage(individual);
//		individualImage = (img != null? img: ResourceHelper.getImage(ADD_PHOTO, imageLabel.getPreferredSize()));
		individualImage = img;
	}

	public String getIndividualNameText(){
		return individualNameText;
	}

	public String getIndividualNameTooltip(){
		return individualNameTooltip;
	}

	public String getInfoText(){
		return infoText;
	}

	public String getInfoTooltip(){
		return infoTooltip;
	}

	public ImageIcon getIndividualImage(){
		return individualImage;
	}


	/**
	 * Extracts all full names from an IndividualRecord.
	 * For each PersonalNameStructure, concatenates the values of all part structures
	 * in the order they appear, separated by a space.
	 *
	 * @param individual the IndividualRecord (tag must be "individual")
	 * @return a list of full name strings (empty if none found)
	 */
	private List<String> extractFullNames(final FLEFRecord individual){
		final List<String> names = new ArrayList<>();
		if(individual == null || !"individual".equals(individual.getTag()))
			return names;

		for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(individual, "name")){
			final StringBuilder fullName = new StringBuilder();
			for(FLEFRecord part : FLEFRecordHelper.findChildren(nameStruct, "part")){
				final String value = FLEFRecordHelper.getChildValue(part, "value");
				if(value != null){
					if(!fullName.isEmpty())
						fullName.append(' ');
					fullName.append(value);
				}
			}

			if(!fullName.isEmpty())
				names.add(fullName.toString());
		}
		return names;
	}


	/**
	 * Extracts birth and death events for a given individual.
	 *
	 * @param individual the IndividualRecord
	 * @return a list of events (birth and death, if found)
	 */
	private List<EventInfo> extractEvents(final FLEFRecord individual){
		final List<EventInfo> events = new ArrayList<>();

		// Find all EventParticipationRecords where participant = this individual
		final String individualId = individual.getId();
		if(individualId == null)
			return events;

		for(final FLEFRecord ep : model.getRecordsByType(TAG_EVENT_PARTICIPATION)){
			final FLEFRecord participant = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
			if(participant == null)
				continue;
			final FLEFRecord individualRef = participant.getTheOnlyChild();
			if(individualRef == null)
				continue;
			if(!individualId.equals(individualRef.getValue()))
				continue;

			final String eventId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
			if(eventId == null)
				continue;
			final FLEFRecord event = model.getRecordById(eventId);
			if(event == null || !TAG_EVENT.equals(event.getTag()))
				continue;

			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(!"birth".equals(type) && !"death".equals(type))
				continue;

			// Extract date
			final EventInfo info = extractEventInfo(event, type);
			if(info != null)
				events.add(info);
		}
		return events;
	}

	private EventInfo extractEventInfo(final FLEFRecord event, final String type){
		final String date = extractFullDate(event);
		if(date == null)
			return null;

		final String calendar = extractDateCalendar(event);
		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendar, date);
		final String place = extractPlace(event);
		final boolean approximate = isApproximate(date);
		return new EventInfo(type, date, parsedDate, place, approximate);
	}

	private String extractFullDate(final FLEFRecord event){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE);
		if(fullDate == null)
			return null;

		return FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
	}

	private String extractDateCalendar(final FLEFRecord event){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE);
		if(fullDate == null)
			return null;

		return FLEFRecordHelper.getChildValue(fullDate, TAG_CALENDAR);
	}

	private String extractPlace(final FLEFRecord event){
		final String placeId = FLEFRecordHelper.getChildValue(event, TAG_PLACE + DOT + TAG_PLACE);
		if(placeId == null)
			return null;

		// Try to get the place record via xref
		final FLEFRecord place = model.getRecordById(placeId);
		if(place != null)
			// get first name value
			for(final FLEFRecord name : FLEFRecordHelper.findChildren(place, "name")){
				final String val = FLEFRecordHelper.getChildValue(name, "value");
				if(val != null)
					return val;
			}
		return null;
	}

	private boolean isApproximate(final String dateStr){
		return (dateStr != null
			&& (dateStr.toLowerCase().contains("about") || dateStr.toLowerCase().contains("circa")
			|| dateStr.contains("~")));
	}


	private ImageIcon extractPreferredImage(final FLEFRecord individual){
		final String preferredImage = FLEFRecordHelper.getChildValue(individual, "preferred_image");
		ImageIcon icon = null;
//		final Integer photoID = extractRecordPhotoID(individual);
//		if(photoID != null){
//			//recover image URI
//			final TreeMap<Integer, Map<String, Object>> media = getRecords(EntityManager.TABLE_NAME_MEDIA);
//			final Map<String, Object> md = media.get(photoID);
//			if(md != null){
//				final String identifier = FileHelper.getTargetPath(FileHelper.documentsDirectory(), extractRecordIdentifier(md));
//				icon = ResourceHelper.getImage(identifier, imageLabel.getPreferredSize());
//			}
//		}
		return icon;
	}

}
