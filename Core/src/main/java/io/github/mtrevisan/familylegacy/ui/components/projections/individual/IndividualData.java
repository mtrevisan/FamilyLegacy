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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverter;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateService;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.components.projections.PlaceholderImages;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.AsyncResourceLoader;
import io.github.mtrevisan.familylegacy.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";
	private static final String TAG_FIGURE_DASH = "\u2012";

	private static final String CIRCA_SYMBOL = "~";
	private static final String LESS_THAN_ABOUT = "< ~";
	private static final String OPEN_PARENTHESIS = "(";
	private static final String CLOSE_PARENTHESIS = ")";

	private static final String NO_DATA = "?";


	private final FLEFRecord individual;
	private final String id;
	private final SexType sex;
	private final String nameText;
	private String nameTooltip;
	private boolean isBiological;
	private boolean hasParents;
	private boolean hasFather;
	private boolean hasMother;
	private boolean hasPartner;
	private boolean hasChildren;

	private final String infoText;
	private final String infoTooltip;

	private String preferredImageKey;
	private String preferredImageUri;
	private Rectangle preferredImageCropRect;
	private ImageIcon imagePrimary;
	private ImageIcon imageSecondary;


	public static IndividualData create(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		return (individual != null
			? new IndividualData(individual, eventsMap, model)
			: null);
	}


	private IndividualData(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		this.individual = individual;
		id = individual.getId();

		sex = IndividualReader.extractSex(individual);

		final List<String> names = IndividualReader.extractFullNames(individual);
		if(!names.isEmpty()){
			nameText = IndividualReader.extractPrimaryFullname(individual);
			nameTooltip = TAG_HTML_OPEN + "[" + id + "]" + TAG_BR + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;
		}
		else
			nameText = NO_DATA;

		// Check for parent relationships
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipReader.TAG_OBJECT, IndividualHandler.TYPE);
			if(subjectId == null || objectId == null || !subjectId.equals(id) && !objectId.equals(id))
				continue;

			String type = RelationshipReader.extractType(relationship);
			if(type != null){
				type = type.toLowerCase(Locale.ROOT);
				if(RelationshipReader.isTypeBiologicalChild(type)){
					if(subjectId.equals(id)){
						isBiological = true;

						final FLEFRecord object = model.getRecordById(objectId);
						final String targetSex = IndividualReader.extractRawSex(object);
						if(IndividualReader.isSexMale(targetSex))
							hasFather = true;
						else if(IndividualReader.isSexFemale(targetSex))
							hasMother = true;
						else
							hasParents = true;
					}

					if(objectId.equals(id))
						hasChildren = true;
				}
				else if(subjectId.equals(id) && RelationshipReader.isTypeChild(type)){
					final FLEFRecord target = model.getRecordById(objectId);
					final SexType targetSex = IndividualReader.extractSex(target);
					if(targetSex == SexType.MALE)
						hasFather = true;
					else if(targetSex == SexType.FEMALE)
						hasMother = true;
					else
						hasParents = true;
				}
				else if(RelationshipReader.isTypePartner(type))
					// Partner/Spouse relationship (non-child type)
					hasPartner = true;
			}

			if(isBiological && hasPartner && hasChildren)
				break;
		}

		// extract events
		final List<EventInfo> events = extractEvents(individual, eventsMap, model);
		final EventInfo birthInfo = events.stream()
			.filter(e -> EventReader.isTypeBirth(e.type()) && e.representativeDate() != null)
			.min(Comparator.comparing(e -> e.representativeDate().jdn()))
			.orElse(null);
		final EventInfo deathInfo = events.stream()
			.filter(e -> EventReader.isTypeDeath(e.type()) && e.representativeDate() != null)
			.max(Comparator.comparing(e -> e.representativeDate().jdn()))
			.orElse(null);

		// ---- Birth/Death summary ----
		final NormalizedDate normalizedBirthDate = (birthInfo != null && birthInfo.date() != null
			? birthInfo.representativeDate()
			: null);
		final NormalizedDate normalizedDeathDate = (deathInfo != null && deathInfo.date() != null
			? deathInfo.representativeDate()
			: null);
		final LocalDate birthDate = (normalizedBirthDate != null
			? CalendarConverter.jdnToGregorian(normalizedBirthDate.jdn())
			: null);
		final LocalDate deathDate = (normalizedDeathDate != null
			? CalendarConverter.jdnToGregorian(normalizedDeathDate.jdn())
			: null);
		String age = null;
		if(birthDate != null && deathDate != null){
			final long years = ChronoUnit.YEARS.between(birthDate, deathDate);
			String prefix = StringUtils.EMPTY;
			if(normalizedBirthDate.approximate() || normalizedDeathDate.approximate())
				prefix = CIRCA_SYMBOL;
			if(normalizedBirthDate.approximate() && birthDate.isBefore(deathDate))
				prefix = LESS_THAN_ABOUT;
			age = prefix + years;
		}

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(birthInfo != null? birthInfo.year(): NO_DATA);
		sj.add(TAG_FIGURE_DASH);
		sj.add(deathInfo != null? deathInfo.year(): NO_DATA);
		if(age != null)
			sj.add(OPEN_PARENTHESIS + age + StringUtils.SPACE + I18N.t("dialog.individual.years.old.abbreviation") + CLOSE_PARENTHESIS);

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


	public FLEFRecord getIndividual(){
		return individual;
	}

	public String getId(){
		return id;
	}

	public SexType getSex(){
		return sex;
	}

	public String getNameText(){
		return nameText;
	}

	public String getNameTooltip(){
		return nameTooltip;
	}

	public boolean isBiological(){
		return isBiological;
	}

	public boolean hasFather(){
		return (hasFather || hasParents);
	}

	public boolean hasMother(){
		return (hasMother || hasParents);
	}

	public boolean hasParents(){
		return (hasFather || hasMother || hasParents);
	}

	public boolean hasPartner(){
		return hasPartner;
	}

	public boolean hasChildren(){
		return hasChildren;
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

	public ImageIcon getImagePrimary(){
		return imagePrimary;
	}

	public ImageIcon getImageSecondary(){
		return imageSecondary;
	}

	public boolean isEmpty(){
		return (id == null);
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

		final List<FLEFRecord> events = (eventsMap != null? eventsMap.get(individualId): null);
		if(events == null || events.isEmpty())
			return Collections.emptyList();

		final List<EventInfo> eventInfos = new ArrayList<>();
		for(final FLEFRecord event : events){
			final String type = EventReader.extractType(event);
			if(EventReader.isTypeBirth(type) || EventReader.isTypeDeath(type)){
				final EventInfo info = extractEventInfo(event, type, model);
				eventInfos.add(info);
			}
		}
		return eventInfos;
	}

	private EventInfo extractEventInfo(final FLEFRecord event, final String type, final FLEFModel model){
		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		final String dateOriginalText = DateReader.extractOriginalText(dateRecord);
		final String date = DateReader.extractPrettyPrintDate(dateRecord);
		final FLEFRecord valueRecord = FLEFRecordHelper.findChild(dateRecord, DateReader.TAG_VALUE);
		final String year = DateService.getYearDisplayText(valueRecord);
		final TemporalSpan temporalSpan = DateReader.extractTemporalSpan(dateRecord);
		final NormalizedDate representativeDate = (temporalSpan != null? temporalSpan.representativeDate(): null);
		final String place = FLEFRecordHelper.extractPlace(event, model);
		final String cause = (EventReader.isTypeDeath(type)? EventReader.extractCauseReason(event): null);
		return new EventInfo(type, dateOriginalText, date, year, representativeDate, place, cause);
	}


	private void extractPreferredImage(final FLEFRecord record){
		if(record == null){
			preferredImageUri = null;
			preferredImageCropRect = null;
			imagePrimary = PlaceholderImages.placeholder(BoxPanelType.PRIMARY);
			imageSecondary = PlaceholderImages.placeholder(BoxPanelType.SECONDARY);
			preferredImageKey = StringUtils.EMPTY;

			return;
		}

		preferredImageUri = IndividualReader.extractPreferredImageUri(record);
// TODO to be removed
if(preferredImageUri != null)
	preferredImageUri = "C:\\mauro\\heritage\\My Genealogy Projects\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)" + preferredImageUri;
		preferredImageCropRect = IndividualReader.extractPreferredImageCrop(record);

		final String rawSex = IndividualReader.extractRawSex(record);
		// Set the default image immediately
		imagePrimary = PlaceholderImages.placeholderFor(rawSex, BoxPanelType.PRIMARY);
		imageSecondary = PlaceholderImages.placeholderFor(rawSex, BoxPanelType.SECONDARY);
		preferredImageKey = composePreferredImageKey(preferredImageUri, preferredImageCropRect);
	}

	public void loadPreferredImageAsync(final BiConsumer<String, ImageIcon[]> imageConsumer){
		if(StringUtils.isEmpty(preferredImageUri))
			return;

		IMAGE_LOADER.load(
			preferredImageKey,
			() -> {
				final ImageIcon croppedImage = ResourceHelper.getCroppedImage(preferredImageUri, preferredImageCropRect);
				if(croppedImage != null){
					final ImageIcon imagePrimary = PlaceholderImages.resize(croppedImage, BoxPanelType.PRIMARY);
					final ImageIcon imageSecondary = PlaceholderImages.resize(croppedImage, BoxPanelType.SECONDARY);
					return new ImageIcon[]{imagePrimary, imageSecondary};
				}
				else{
					LOGGER.error("Non-existent image for {}", preferredImageUri);

					return null;
				}
			},
			images -> {
				if(images != null){
					imagePrimary = images[0];
					imageSecondary = images[1];
				}

				imageConsumer.accept(preferredImageKey, images);
			}
		);
	}

	private static String composePreferredImageKey(final String preferredImage, final Rectangle preferredImageCropRect){
		return (StringUtils.isNotEmpty(preferredImage)? preferredImage: StringUtils.EMPTY)
			+ (preferredImageCropRect != null? TAG_PIPE + (int)preferredImageCropRect.getX() + DOT
			+ (int)preferredImageCropRect.getY() + DOT + (int)preferredImageCropRect.getWidth() + DOT
			+ (int)preferredImageCropRect.getHeight(): StringUtils.EMPTY);
	}


	@Override
	public String toString(){
		return nameText + " [" + id + "]";
	}

}
