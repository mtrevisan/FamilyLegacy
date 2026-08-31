package io.github.mtrevisan.familylegacy.v2.ui.components.biologicalparents;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.individual.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.AsyncResourceLoader;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.ImageIcon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;


/* ONGOING */
/**
 * Extracts display information for a biological group from a FLEFModel.
 */
public final class BiologicalParentsData{

	private static final AsyncResourceLoader<ImageIcon> IMAGE_LOADER = new AsyncResourceLoader<>();

	private static final String DOT = ".";
	private static final String TAG_PIPE = "|";

	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
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
	private static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_URI = "uri";
	private static final String TAG_CROP = "crop";
	private static final String TAG_X = "x";
	private static final String TAG_Y = "y";
	private static final String TAG_WIDTH = "width";
	private static final String TAG_HEIGHT = "height";
	private static final String TAG_DATE_VALUE_POINT_FULL_DATE = TAG_DATE + DOT + TAG_VALUE + DOT + TAG_POINT + DOT + TAG_FULL_DATE;
	private static final String TAG_NAME_VALUE = TAG_NAME + DOT + TAG_VALUE;
	private static final String TAG_PLACE_PLACE = TAG_PLACE + DOT + TAG_PLACE;
	private static final String TAG_PREFERRED_IMAGE_URI = TAG_PREFERRED_IMAGE + DOT + TAG_URI;
	private static final String TAG_PREFERRED_IMAGE_CROP = TAG_PREFERRED_IMAGE + DOT + TAG_CROP;

	private static final String EVENT_TYPE_BIRTH = "birth";
	private static final String EVENT_TYPE_DEATH = "death";

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";
	private static final String TAG_FIGURE_DASH = "\u2012";

	private static final String CIRCA_SYMBOL = "~";
	private static final String CIRCA = "circa";
	private static final String ABOUT = "about";
	private static final String LESS_THAN_ABOUT = "< ~";
	private static final String OPEN_PARENTHESIS = "(";
	private static final String CLOSE_PARENTHESIS = ")";
	private static final String YEARS_OLD = "y/o";

	private static final String NO_DATA = "?";
	private static final String[] NO_NAME = {NO_DATA, NO_DATA};

	private static final ImageIcon ADD_PHOTO = ResourceHelper.getImage("/images/add_photo.jpg");

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_GROUP = "group";

	private static final String ENUM_TYPE_BIOLOGICAL_CHILD = "biological_child";
	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";
	private static final String ENUM_TYPE_FAMILY = "family";


	/**
	 * Represents a single event (birth or death) with date and place.
	 *
	 * @param type        `birth` or `death`
	 * @param rawDate     original date string (for display)
	 * @param place       place name or original_text
	 * @param approximate whether the date is approximate
	 */
	public record EventInfo(String type, String rawDate, ParsedGenealogicalDate date, String place, boolean approximate){}


	private final FLEFModel model;
	private final BoxPanelType boxType;


	private FLEFRecord individual;
	private FLEFRecord father;
	private FLEFRecord mother;
	private FLEFRecord group;
	private List<FLEFRecord> siblings;

	private String preferredImageKey;
	private String preferredImage;
	private Rectangle preferredImageCropRect;
	private int preferredImageWidth;
	private int preferredImageHeight;
	private ImageIcon groupImage;


	public BiologicalParentsData(final FLEFRecord individual, final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		this.individual = individual;
		final String individualId = individual.getId();

		// 1. Find biological parents
		extractBiologicalParents(individualId);

		// 2. Find the primary family group (where the individual is a member)
		extractPrimaryFamilyGroup(individualId);

		// 3. Find siblings (other members of the same family group)
		extractSiblings(individualId);



//		final List<String> names = extractFullNames(individual);
//		if(!names.isEmpty()){
//			individualNameText = names.getFirst();
//			individualNameTooltip = TAG_HTML_OPEN + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;
//		}
//		else
//			individualNameText = NO_DATA;
//
//
//		// extract events
//		final List<EventInfo> events = extractEvents(individual);
//		final EventInfo birthInfo = events.stream()
//			.filter(e -> EVENT_TYPE_BIRTH.equals(e.type) && e.date != null)
//			.min(Comparator.comparing(e -> e.date.isoDate()))
//			.orElse(null);
//		final EventInfo deathInfo = events.stream()
//			.filter(e -> EVENT_TYPE_DEATH.equals(e.type) && e.date != null)
//			.max(Comparator.comparing(e -> e.date.isoDate()))
//			.orElse(null);
//
//		// ---- Birth/Death summary ----
//		final String birthYear = (birthInfo != null && birthInfo.date != null
//			? String.valueOf(birthInfo.date.isoDate().getYear())
//			: NO_DATA);
//		final String deathYear = (deathInfo != null && deathInfo.date != null
//			? String.valueOf(deathInfo.date.isoDate().getYear())
//			: NO_DATA);
//		String age = null;
//		if(birthInfo != null && deathInfo != null && birthInfo.date != null && deathInfo.date != null){
//			final long years = ChronoUnit.YEARS.between(birthInfo.date.isoDate(), deathInfo.date.isoDate());
//			String prefix = StringUtils.EMPTY;
//			if(birthInfo.approximate || deathInfo.approximate)
//				prefix = CIRCA_SYMBOL;
//			if(birthInfo.approximate && birthInfo.date.isoDate().isBefore(deathInfo.date.isoDate()))
//				prefix = LESS_THAN_ABOUT;
//			age = prefix + years;
//		}
//
//		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
//		sj.add(birthYear);
//		sj.add(TAG_FIGURE_DASH);
//		sj.add(deathYear);
//		if(age != null)
//			sj.add(OPEN_PARENTHESIS + age + StringUtils.SPACE + YEARS_OLD + CLOSE_PARENTHESIS);
//
//		final StringJoiner toolTipSJ = new StringJoiner(StringUtils.EMPTY);
//		final String birthPlace = (birthInfo != null? birthInfo.place: null);
//		final String deathPlace = (deathInfo != null? deathInfo.place: null);
//		if(birthPlace != null || deathPlace != null){
//			toolTipSJ.add(TAG_HTML_OPEN);
//			toolTipSJ.add(birthInfo != null? birthInfo.rawDate: NO_DATA);
//			if(birthPlace != null)
//				toolTipSJ.add(TAG_BR + birthPlace);
//			toolTipSJ.add(TAG_BR + TAG_FIGURE_DASH + TAG_BR);
//			toolTipSJ.add(deathInfo != null? deathInfo.rawDate: NO_DATA);
//			if(deathPlace != null)
//				toolTipSJ.add(TAG_BR + deathPlace);
//			toolTipSJ.add(TAG_HTML_CLOSE);
//		}
//		else{
//			toolTipSJ.add(birthInfo != null? birthInfo.rawDate: NO_DATA);
//			toolTipSJ.add(StringUtils.SPACE + TAG_FIGURE_DASH + StringUtils.SPACE);
//			toolTipSJ.add(deathInfo != null? deathInfo.rawDate: NO_DATA);
//		}
//		infoText = sj.toString();
//		infoTooltip = toolTipSJ.toString();
//
//		extractPreferredImage(individual);
	}

	/**
	 * Finds biological parents of the given individual.
	 */
	private void extractBiologicalParents(final String individualId){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			// The subject must be this individual
			String subjectId = extractReferencedId(relationship, TAG_SUBJECT);
			if(!individualId.equals(subjectId))
				continue;

			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(!ENUM_TYPE_BIOLOGICAL_CHILD.equals(type))
				continue;

			// The target is the parent
			final String parentId = extractReferencedId(relationship, TAG_TARGET);
			if(parentId == null)
				continue;

			final FLEFRecord parent = model.getRecordById(parentId);
			if(parent == null)
				continue;

			// Determine the parent's sex
			final String sex = FLEFRecordHelper.getChildValue(parent, "sex");
			if(ENUM_SEX_MALE.equals(sex))
				father = parent;
			else if(ENUM_SEX_FEMALE.equals(sex))
				mother = parent;
			else{
				// If sex is unknown, we could store as a generic parent,
				// but we ignore for now.
			}
		}
	}

	/**
	 * Finds the primary family group (GroupRecord with type "family") where the individual is a member.
	 * <p>
	 * If multiple groups exist, returns the first one found.
	 */
	private void extractPrimaryFamilyGroup(final String individualId){
		final List<FLEFRecord> groups = model.getRecordsByType(TAG_GROUP);
		for(final FLEFRecord group : groups){
			// Check if the group has a "family" type
			final String type = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
			if(!ENUM_TYPE_FAMILY.equals(type))
				continue;

			// Check if the individual is a member of this group
			if(isMemberOfGroup(individualId, group))
				this.group = group;
		}
	}

	/**
	 * Checks if an individual is a member of a given group via a "group_member"
	 * relationship.
	 */
	private boolean isMemberOfGroup(final String individualId, final FLEFRecord group){
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !type.endsWith("child"))
				continue;

			// Subject must be the individual
			final String subjectId = extractReferencedId(relationship, TAG_SUBJECT);
			if(!individualId.equals(subjectId))
				continue;

			// Target must be this group
			final String targetId = extractReferencedId(relationship, TAG_TARGET);
			if(group.getId().equals(targetId))
				return true;
		}
		return false;
	}

	/**
	 * Finds siblings of the given individual within a family group.
	 * Siblings are other individuals who are also members of the same group.
	 * The current individual is excluded.
	 */
	private void extractSiblings(final String individualId){
		siblings = new ArrayList<>();

		if(group == null)
			return;

		final String groupId = group.getId();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type == null || !type.endsWith("child"))
				continue;

			final String subjectId = extractReferencedId(relationship, TAG_SUBJECT);
			if(subjectId == null || individualId.equals(subjectId))
				continue;

			final String targetId = extractReferencedId(relationship, TAG_TARGET);
			if(groupId.equals(targetId)){
				final FLEFRecord sibling = model.getRecordById(subjectId);
				if(sibling != null)
					siblings.add(sibling);
			}
		}
	}

	/**
	 * Extracts the referenced ID from a child field of a relationship record.
	 * The field is expected to have a single child that is an Xref.
	 */
	private String extractReferencedId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;

		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null)
			return null;

		return ref.getValue();
	}

//	/**
//	 * Extracts birth and death events for a given individual.
//	 *
//	 * @param individual the IndividualRecord
//	 * @return a list of events (birth and death, if found)
//	 */
//	private List<EventInfo> extractEvents(final FLEFRecord individual){
//		final List<EventInfo> events = new ArrayList<>();
//
//		// Find all EventParticipationRecords where participant = this individual
//		final String individualId = individual.getId();
//		if(individualId == null)
//			return events;
//
//		for(final FLEFRecord ep : model.getRecordsByType(TAG_EVENT_PARTICIPATION)){
//			final FLEFRecord participant = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
//			if(participant == null)
//				continue;
//			final FLEFRecord individualRef = participant.getTheOnlyChild();
//			if(individualRef == null)
//				continue;
//			if(!individualId.equals(individualRef.getValue()))
//				continue;
//
//			final String eventId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
//			if(eventId == null)
//				continue;
//			final FLEFRecord event = model.getRecordById(eventId);
//			if(event == null || !TAG_EVENT.equals(event.getTag()))
//				continue;
//
//			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
//			if(!EVENT_TYPE_BIRTH.equals(type) && !EVENT_TYPE_DEATH.equals(type))
//				continue;
//
//			// Extract date
//			final EventInfo info = extractEventInfo(event, type);
//			if(info != null)
//				events.add(info);
//		}
//		return events;
//	}
//
//	private EventInfo extractEventInfo(final FLEFRecord event, final String type){
//		final String date = extractFullDate(event);
//		if(date == null)
//			return null;
//
//		final String calendar = extractDateCalendar(event);
//		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendar, date);
//		final String place = extractPlace(event);
//		final boolean approximate = isApproximate(date);
//		return new EventInfo(type, date, parsedDate, place, approximate);
//	}
//
//	private String extractFullDate(final FLEFRecord event){
//		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE_VALUE_POINT_FULL_DATE);
//		if(fullDate == null)
//			return null;
//
//		return FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
//	}
//
//	private String extractDateCalendar(final FLEFRecord event){
//		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE_VALUE_POINT_FULL_DATE);
//		if(fullDate == null)
//			return null;
//
//		return FLEFRecordHelper.getChildValue(fullDate, TAG_CALENDAR);
//	}
//
//	private String extractPlace(final FLEFRecord event){
//		final String placeId = FLEFRecordHelper.getChildValue(event, TAG_PLACE_PLACE);
//		if(placeId == null)
//			return null;
//
//		// Try to get the place record via xref
//		final FLEFRecord place = model.getRecordById(placeId);
//		// get first name value
//		for(final FLEFRecord name : FLEFRecordHelper.findChildren(place, TAG_NAME_VALUE))
//			if(name != null)
//				return name.getValue();
//		return null;
//	}
//
//	private boolean isApproximate(final String dateStr){
//		return (dateStr != null
//			&& (dateStr.toLowerCase().contains(ABOUT) || dateStr.toLowerCase().contains(CIRCA)
//			|| dateStr.contains(CIRCA_SYMBOL)));
//	}
//
//
//	private void extractPreferredImage(final FLEFRecord individual){
//		preferredImage = FLEFRecordHelper.getChildValue(individual, TAG_PREFERRED_IMAGE_URI);
//		final FLEFRecord preferredImageCrop = FLEFRecordHelper.findChild(individual, TAG_PREFERRED_IMAGE_CROP);
//		preferredImageCropRect = null;
//		try{
//			final int cropX = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_X));
//			final int cropY = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_Y));
//			final int cropWidth = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_WIDTH));
//			final int cropHeight = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_HEIGHT));
//			preferredImageCropRect = new Rectangle(cropX, cropY, cropWidth, cropHeight);
//		}
//		catch(final Exception ignored){}
//
//		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
//		preferredImageWidth = (int)Math.ceil(PREFERRED_IMAGE_WIDTH / shrinkFactor);
//		preferredImageHeight = (int)Math.ceil(PREFERRED_IMAGE_WIDTH * IMAGE_ASPECT_RATIO / shrinkFactor);
//		individualImage = ResourceHelper.resize(ADD_PHOTO, preferredImageWidth, preferredImageHeight);
//
//		// 1. Set the default image immediately
//		preferredImageKey = composePreferredImageKey(preferredImage, preferredImageWidth, preferredImageHeight,
//			preferredImageCropRect);
//	}
//
//	public void loadPreferredImageAsync(final BiConsumer<String, ImageIcon> imageConsumer){
//		if(StringUtils.isEmpty(preferredImage))
//			return;
//
//		IMAGE_LOADER.load(
//			preferredImageKey,
//			() -> ResourceHelper.getCroppedResizedImage(
//				preferredImage,
//				preferredImageCropRect,
//				preferredImageWidth,
//				preferredImageHeight
//			),
//			image -> {
//				if(image != null)
//					this.individualImage = image;
//
//				imageConsumer.accept(preferredImageKey, image);
//			}
//		);
//	}

	private static String composePreferredImageKey(final String preferredImage, final int width, final int height,
			final Rectangle preferredImageCropRect){
		return preferredImage
			+ "@" + width + "x" + height
			+ (preferredImageCropRect != null? TAG_PIPE + preferredImageCropRect : StringUtils.EMPTY);
	}

}
