package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.AsyncResourceLoader;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ParsedGenealogicalDate;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Rectangle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;


/**
 * Extracts display information for an individual from a FLEFModel.
 */
public final class IndividualData{

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualData.class);


	private static final AsyncResourceLoader<ImageIcon> IMAGE_LOADER = new AsyncResourceLoader<>();

	private static final String DOT = ".";
	private static final String TAG_PIPE = "|";

	private static final String TAG_INDIVIDUAL = "individual";
	private static final String TAG_SEX = "sex";
	private static final String TAG_NAME = "name";
	private static final String TAG_PART = "part";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_CAUSE = "cause";
	private static final String TAG_REASON = "reason";
	private static final String TAG_DATE = "date";
	private static final String TAG_PLACE = "place";
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
	private static final String TAG_CAUSE_REASON = TAG_CAUSE + DOT + TAG_REASON;
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

	private static final ImageIcon ADD_PHOTO = ResourceHelper.getImage("/images/add_photo.jpg");

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;


	private final String individualId;
	private final String individualSex;
	private final String individualNameText;
	private String individualNameTooltip;

	private final String infoText;
	private final String infoTooltip;

	private String preferredImageKey;
	private String preferredImageUri;
	private Rectangle preferredImageCropRect;
	private ImageIcon individualImagePrimary;
	private ImageIcon individualImageSecondary;


	public static IndividualData create(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		return new IndividualData(individual, eventsMap, model);
	}


	private IndividualData(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		individualId = individual.getId();

		individualSex = FLEFRecordHelper.getChildValue(individual, TAG_SEX);

		final List<String> names = extractFullNames(individual);
		if(!names.isEmpty()){
			individualNameText = names.getFirst();
			individualNameTooltip = TAG_HTML_OPEN + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;
		}
		else
			individualNameText = NO_DATA;


		// extract events
		final List<EventInfo> events = extractEvents(individual, eventsMap, model);
		final EventInfo birthInfo = events.stream()
			.filter(e -> EVENT_TYPE_BIRTH.equals(e.type()) && e.date() != null)
			.min(Comparator.comparing(e -> e.date().isoDate()))
			.orElse(null);
		final EventInfo deathInfo = events.stream()
			.filter(e -> EVENT_TYPE_DEATH.equals(e.type()) && e.date() != null)
			.max(Comparator.comparing(e -> e.date().isoDate()))
			.orElse(null);

		// ---- Birth/Death summary ----
		final String birthYear = (birthInfo != null && birthInfo.date() != null
			? String.valueOf(birthInfo.date().isoDate().getYear())
			: NO_DATA);
		final String deathYear = (deathInfo != null && deathInfo.date() != null
			? String.valueOf(deathInfo.date().isoDate().getYear())
			: NO_DATA);
		String age = null;
		if(birthInfo != null && deathInfo != null && birthInfo.date() != null && deathInfo.date() != null){
			final long years = ChronoUnit.YEARS.between(birthInfo.date().isoDate(), deathInfo.date().isoDate());
			String prefix = StringUtils.EMPTY;
			if(birthInfo.approximate() || deathInfo.approximate())
				prefix = CIRCA_SYMBOL;
			if(birthInfo.approximate() && birthInfo.date().isoDate().isBefore(deathInfo.date().isoDate()))
				prefix = LESS_THAN_ABOUT;
			age = prefix + years;
		}

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(birthYear);
		sj.add(TAG_FIGURE_DASH);
		sj.add(deathYear);
		if(age != null)
			sj.add(OPEN_PARENTHESIS + age + StringUtils.SPACE + YEARS_OLD + CLOSE_PARENTHESIS);

		final StringJoiner tooltip = new StringJoiner(StringUtils.EMPTY);
		final String birthPlace = (birthInfo != null? birthInfo.place(): null);
		final String deathPlace = (deathInfo != null? deathInfo.place(): null);
		final String deathCause = (deathInfo != null? deathInfo.deathCause(): null);
		if(birthPlace != null || deathPlace != null || deathCause != null){
			tooltip.add(TAG_HTML_OPEN);
			tooltip.add(birthInfo != null? birthInfo.rawDate(): NO_DATA);
			if(birthPlace != null)
				tooltip.add(TAG_BR + birthPlace);
			tooltip.add(TAG_BR + TAG_FIGURE_DASH + TAG_BR);
			tooltip.add(deathInfo != null? deathInfo.rawDate(): NO_DATA);
			if(deathPlace != null)
				tooltip.add(TAG_BR + deathPlace);
			if(deathCause != null)
				tooltip.add(TAG_BR + OPEN_PARENTHESIS + deathCause + CLOSE_PARENTHESIS);
			tooltip.add(TAG_HTML_CLOSE);
		}
		else{
			tooltip.add(birthInfo != null? birthInfo.rawDate(): NO_DATA);
			tooltip.add(StringUtils.SPACE + TAG_FIGURE_DASH + StringUtils.SPACE);
			tooltip.add(deathInfo != null? deathInfo.rawDate(): NO_DATA);
		}
		infoText = sj.toString();
		infoTooltip = tooltip.toString();

		extractPreferredImage(individual);
	}


	public String getIndividualId() {
		return individualId;
	}

	public String getIndividualSex(){
		return individualSex;
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

	public String getPreferredImageKey(){
		return preferredImageKey;
	}

	public ImageIcon getIndividualImagePrimary(){
		return individualImagePrimary;
	}

	public ImageIcon getIndividualImageSecondary(){
		return individualImageSecondary;
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
		if(individual == null || !TAG_INDIVIDUAL.equals(individual.getTag()))
			return names;

		for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(individual, TAG_NAME)){
			final StringBuilder fullName = new StringBuilder();
			for(FLEFRecord part : FLEFRecordHelper.findChildren(nameStruct, TAG_PART)){
				final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
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
	private List<EventInfo> extractEvents(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
		final FLEFModel model){
		final String individualId = individual.getId();
		if(individualId == null)
			return Collections.emptyList();

		final List<FLEFRecord> events = eventsMap.get(individualId);
		if(events == null || events.isEmpty())
			return Collections.emptyList();

		final List<EventInfo> eventInfos = new ArrayList<>();
		for(final FLEFRecord event : events){
			final String type = FLEFRecordHelper.getChildValue(event, TAG_TYPE);
			if(EVENT_TYPE_BIRTH.equals(type) || EVENT_TYPE_DEATH.equals(type)){
				final EventInfo info = extractEventInfo(event, type, model);
				if(info != null)
					eventInfos.add(info);
			}
		}
		return eventInfos;
	}

	private EventInfo extractEventInfo(final FLEFRecord event, final String type, final FLEFModel model){
		final String date = extractFullDate(event);
		if(date == null)
			return null;

		final String calendar = extractDateCalendar(event);
		final ParsedGenealogicalDate parsedDate = UniversalDateConverter.parse(calendar, date);
		final boolean approximate = isApproximate(date);
		final String place = extractPlace(event, model);
		final String deathCause = FLEFRecordHelper.getChildValue(event, TAG_CAUSE_REASON);
		return new EventInfo(type, date, parsedDate, approximate, place, deathCause);
	}

	private String extractFullDate(final FLEFRecord event){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE_VALUE_POINT_FULL_DATE);
		if(fullDate == null)
			return null;

		return FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
	}

	private String extractDateCalendar(final FLEFRecord event){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(event, TAG_DATE_VALUE_POINT_FULL_DATE);
		if(fullDate == null)
			return null;

		return FLEFRecordHelper.getChildValue(fullDate, TAG_CALENDAR);
	}

	private boolean isApproximate(final String dateStr){
		return (dateStr != null
			&& (dateStr.toLowerCase().contains(ABOUT) || dateStr.toLowerCase().contains(CIRCA)
			|| dateStr.contains(CIRCA_SYMBOL)));
	}

	private String extractPlace(final FLEFRecord event, final FLEFModel model){
		final String placeId = FLEFRecordHelper.getChildValue(event, TAG_PLACE_PLACE);
		if(placeId == null)
			return null;

		// Try to get the place record via xref
		final FLEFRecord place = model.getRecordById(placeId);
		// get first name value
		for(final FLEFRecord name : FLEFRecordHelper.findChildren(place, TAG_NAME_VALUE))
			if(name != null)
				return name.getValue();
		return null;
	}


	private void extractPreferredImage(final FLEFRecord record){
		if(record == null){
			preferredImageUri = null;
			preferredImageCropRect = null;
			individualImagePrimary = resize(ADD_PHOTO, BoxPanelType.PRIMARY);
			individualImageSecondary = resize(ADD_PHOTO, BoxPanelType.SECONDARY);
			preferredImageKey = StringUtils.EMPTY;

			return;
		}

		preferredImageUri = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED_IMAGE_URI);
		final FLEFRecord preferredImageCrop = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE_CROP);
		preferredImageCropRect = null;
		try{
			final int cropX = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_X));
			final int cropY = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_Y));
			final int cropWidth = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_WIDTH));
			final int cropHeight = Integer.parseInt(FLEFRecordHelper.getChildValue(preferredImageCrop, TAG_HEIGHT));
			preferredImageCropRect = new Rectangle(cropX, cropY, cropWidth, cropHeight);
		}
		catch(final Exception ignored){}

		// Set the default image immediately
		individualImagePrimary = resize(ADD_PHOTO, BoxPanelType.PRIMARY);
		individualImageSecondary = resize(ADD_PHOTO, BoxPanelType.SECONDARY);
		preferredImageKey = composePreferredImageKey(preferredImageUri, preferredImageCropRect);
	}

	public void loadPreferredImageAsync(final BiConsumer<String, ImageIcon[]> imageConsumer){
		if(StringUtils.isEmpty(preferredImageUri))
			return;

		IMAGE_LOADER.load(
			preferredImageKey,
			() -> {
				final ImageIcon croppedImage = ResourceHelper.getCroppedImage(preferredImageUri, preferredImageCropRect);
				if(croppedImage == null){
					LOGGER.error("Non-existent image for {}", preferredImageUri);

					return null;
				}

				final ImageIcon imagePrimary = resize(croppedImage, BoxPanelType.PRIMARY);
				final ImageIcon imageSecondary = resize(croppedImage, BoxPanelType.SECONDARY);
				return new ImageIcon[]{imagePrimary, imageSecondary};
			},
			images -> {
				if(images != null){
					individualImagePrimary = images[0];
					individualImageSecondary = images[1];
				}

				imageConsumer.accept(preferredImageKey, images);
			}
		);
	}

	private ImageIcon resize(final ImageIcon image, final BoxPanelType boxType){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int preferredImageWidth = (int)Math.ceil(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		final int preferredImageHeight = (int)Math.ceil(PREFERRED_IMAGE_WIDTH * IMAGE_ASPECT_RATIO / shrinkFactor);
		return ResourceHelper.resize(image, preferredImageWidth, preferredImageHeight);
	}

	private static String composePreferredImageKey(final String preferredImage, final Rectangle preferredImageCropRect){
		return preferredImage
			+ (preferredImageCropRect != null? TAG_PIPE + (int)preferredImageCropRect.getX() + DOT
			+ (int)preferredImageCropRect.getY() + DOT + (int)preferredImageCropRect.getWidth() + DOT
			+ (int)preferredImageCropRect.getHeight(): StringUtils.EMPTY);
	}

}
