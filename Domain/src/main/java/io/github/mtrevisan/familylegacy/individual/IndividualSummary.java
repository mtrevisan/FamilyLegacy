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
package io.github.mtrevisan.familylegacy.individual;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateService;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyResolver;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;


/**
 * Extracts display information for an individual from a FLEFModel: name, sex, birth and death summary, tooltips, kinship hints,
 * privacy. The image, being a UI concern, is added by {@code IndividualImage}.
 */
public final class IndividualSummary{

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
	private final PrivacyResolver.Effective privacy;
	private final SexType sex;
	private final String nameText;
	private String nameTooltip;
	private boolean hasParents;
	private boolean hasFather;
	private boolean hasMother;
	private boolean hasPartner;
	private boolean hasChildren;

	private final String infoText;
	private final String infoTooltip;
	/** The representative date of the preferred birth, if known (and not hidden): used to order the views, never stored. */
	private final NormalizedDate birthDate;


	public static IndividualSummary create(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		return (individual != null
			? new IndividualSummary(individual, eventsMap, model)
			: null);
	}


	private IndividualSummary(final FLEFRecord individual, final Map<String, List<FLEFRecord>> eventsMap,
			final FLEFModel model){
		this.individual = individual;
		id = individual.getId();

		privacy = PrivacyService.resolver(model)
			.describe(individual);
		if(PrivacyService.policy().hides(privacy.level())){
			//the data of the individual are not shown: only the id remains
			sex = SexType.UNKNOWN;
			nameText = NO_DATA;
			nameTooltip = TAG_HTML_OPEN + "[" + id + "]" + TAG_HTML_CLOSE;
			infoText = StringUtils.EMPTY;
			infoTooltip = StringUtils.EMPTY;
			birthDate = null;
			return;
		}

		sex = PreferredSex.of(individual, model);

		final List<String> names = IndividualReader.extractFullNames(individual);
		if(!names.isEmpty()){
			nameText = IndividualReader.extractPrimaryFullname(individual);
			nameTooltip = TAG_HTML_OPEN + "[" + id + "]" + TAG_BR + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;
		}
		else
			nameText = NO_DATA;

		// Check for parent relationships
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipReader.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String objectId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subjectId == null || objectId == null || !subjectId.equals(id) && !objectId.equals(id))
				continue;
			//a hidden relative does not count
			if(PrivacyService.isHidden(model, model.getRecordById(subjectId.equals(id)? objectId: subjectId)))
				continue;

			String type = RelationshipReader.extractType(relationship);
			if(type != null){
				type = type.toLowerCase(Locale.ROOT);
				if(RelationshipReader.isTypeBiologicalChild(type)){
					if(subjectId.equals(id)){
						final FLEFRecord object = model.getRecordById(objectId);
						final SexType targetSex = PreferredSex.of(object, model);
						if(targetSex == SexType.MALE)
							hasFather = true;
						else if(targetSex == SexType.FEMALE)
							hasMother = true;
						else
							hasParents = true;
					}

					if(objectId.equals(id))
						hasChildren = true;
				}
				else if(subjectId.equals(id) && RelationshipReader.isTypeChild(type)){
					final FLEFRecord target = model.getRecordById(objectId);
					final SexType targetSex = PreferredSex.of(target, model);
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

			//everything is known (stopping earlier would miss the parents that come later in the file)
			if(hasPartner && hasChildren && hasFather && hasMother)
				break;
		}

		// Extract events and resolve birth/death considering ConclusionRecords
		final List<EventInfo> events = extractEvents(individual, eventsMap, model);
		final EventInfo birthInfo = resolvePreferredEvent(events, model, true);
		final EventInfo deathInfo = resolvePreferredEvent(events, model, false);

		// ---- Birth/Death summary ----
		final NormalizedDate normalizedBirthDate = (birthInfo != null && birthInfo.date() != null
			? birthInfo.representativeDate()
			: null);
		final NormalizedDate normalizedDeathDate = (deathInfo != null && deathInfo.date() != null
			? deathInfo.representativeDate()
			: null);
		final LocalDate birthDate = (normalizedBirthDate != null
			? CalendarType.jdnToProlepticGregorianLocalDate(normalizedBirthDate.jdn())
			: null);
		final LocalDate deathDate = (normalizedDeathDate != null
			? CalendarType.jdnToProlepticGregorianLocalDate(normalizedDeathDate.jdn())
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
			sj.add(OPEN_PARENTHESIS + age + StringUtils.SPACE + I18N.t("dialog.individual.years_old.abbreviation") + CLOSE_PARENTHESIS);

		final StringJoiner tooltip = createTooltip(birthInfo, deathInfo);
		infoText = sj.toString();
		infoTooltip = tooltip.toString();
		this.birthDate = normalizedBirthDate;

	}

	private static StringJoiner createTooltip(final EventInfo birthInfo, final EventInfo deathInfo){
		final StringJoiner tooltip = new StringJoiner(StringUtils.EMPTY);
		final String birthPlace = (birthInfo != null? birthInfo.place(): null);
		final String deathPlace = (deathInfo != null? deathInfo.place(): null);
		final String deathCause = (deathInfo != null? deathInfo.deathCause(): null);
		if(birthPlace != null || deathPlace != null || deathCause != null){
			tooltip.add(TAG_HTML_OPEN);
			tooltip.add(birthInfo != null? birthInfo.date(): NO_DATA);
			if(birthPlace != null)
				tooltip.add(TAG_BR + birthPlace);
			tooltip.add(TAG_BR + TAG_FIGURE_DASH + TAG_BR);
			tooltip.add(deathInfo != null? deathInfo.date(): NO_DATA);
			if(deathPlace != null)
				tooltip.add(TAG_BR + deathPlace);
			if(deathCause != null)
				tooltip.add(TAG_BR + OPEN_PARENTHESIS + deathCause + CLOSE_PARENTHESIS);
			tooltip.add(TAG_HTML_CLOSE);
		}
		else{
			tooltip.add(birthInfo != null? birthInfo.date(): NO_DATA);
			tooltip.add(StringUtils.SPACE + TAG_FIGURE_DASH + StringUtils.SPACE);
			tooltip.add(deathInfo != null? deathInfo.date(): NO_DATA);
		}
		return tooltip;
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

	/** @return	Whether the individual has a name (it has not when it is unknown or hidden by the privacy policy). */
	public boolean hasName(){
		return !NO_DATA.equals(nameText);
	}

	public String getNameTooltip(){
		return nameTooltip;
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

	/**
	 * The date of the preferred birth (the preferred assertion, if a conclusion selects one, otherwise the earliest birth), reduced
	 * to its representative date when it is approximate or bounded.
	 *
	 * @return	The date, or {@code null} if the birth is unknown or hidden by the privacy policy.
	 */
	public NormalizedDate getBirthDate(){
		return birthDate;
	}

	/** @return	Whether the privacy policy hides the data of the individual (only the id is shown). */
	public boolean isHidden(){
		return PrivacyService.policy().hides(privacy.level());
	}

	public boolean isEmpty(){
		return (id == null);
	}

	/** The effective privacy of the individual (it is public when there is no restriction). */
	public PrivacyResolver.Effective getPrivacy(){
		return privacy;
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
		final String eventId = event.getId();
		final FLEFRecord dateRecord = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		final String dateOriginalText = DateReader.extractOriginalText(dateRecord);
		final String date = DateReader.extractPrettyPrintDate(dateRecord);
		final FLEFRecord valueRecord = FLEFRecordHelper.findChild(dateRecord, DateReader.TAG_VALUE);
		final String year = DateService.getYearDisplayText(valueRecord);
		final TemporalSpan temporalSpan = DateReader.extractTemporalSpan(dateRecord);
		final NormalizedDate representativeDate = (temporalSpan != null? temporalSpan.representativeDate(): null);
		final String place = PlaceCitationReader.extractPlace(event, model);
		final String cause = (EventReader.isTypeDeath(type)? EventReader.extractCauseReason(event): null);
		return new EventInfo(type, dateOriginalText, date, year, representativeDate, place, cause, eventId, event);
	}

	/**
	 * Resolves the preferred birth or death event using applicable ConclusionRecords,
	 * falling back to heuristic date selection if no formal conclusion exists.
	 */
	private static EventInfo resolvePreferredEvent(final List<EventInfo> events, final FLEFModel model,
			final boolean isBirth){
		if(events == null || events.isEmpty())
			return null;

		final List<EventInfo> candidates = new ArrayList<>();
		for(final EventInfo e : events)
			if((isBirth? EventReader.isTypeBirth(e.type()): EventReader.isTypeDeath(e.type()))
				&& e.representativeDate() != null)
				candidates.add(e);
		if(candidates.isEmpty())
			return null;

		// Evaluate ConclusionRecords for preferred assertion selection
		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionReader.TYPE);
		EventInfo preferredEvent = null;
		String bestStatus = null;

		for(final FLEFRecord conclusion : conclusions){
			final String issue = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_ISSUE);
			if(issue == null || !issue.toLowerCase(Locale.ROOT).contains(isBirth? EventReader.ENUM_TYPE_BIRTH: EventReader.ENUM_TYPE_DEATH))
				continue;

			final FLEFRecord preferred = FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED);
			if(preferred == null)
				continue;
			final FLEFRecord ref = preferred.getTheOnlyChild();
			if(ref == null || ref.getValue() == null)
				continue;

			final String targetId = ref.getValue();
			for(final EventInfo candidate : candidates)
				if(candidate.matchesId(targetId)){
					final String status = FLEFRecordHelper.getChildValue(conclusion, ConclusionReader.TAG_PROOF_STATUS);
					if(preferredEvent == null || isProofStatusStronger(status, bestStatus)){
						preferredEvent = candidate;
						bestStatus = status;
					}
				}
		}

		if(preferredEvent != null)
			return preferredEvent;

		// Fallback to default heuristic (min for birth, max for death)
		return (isBirth
			? candidates.stream().min(Comparator.comparing(e -> e.representativeDate().jdn())).orElse(null)
			: candidates.stream().max(Comparator.comparing(e -> e.representativeDate().jdn())).orElse(null));
	}

	private static boolean isProofStatusStronger(final String newStatus, final String currentStatus){
		return rankOfStatus(newStatus) > rankOfStatus(currentStatus);
	}

	private static int rankOfStatus(final String status){
		if(ConclusionReader.ENUM_PROOF_STATUS_PROVEN.equals(status))
			return 3;
		if(ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED.equals(status))
			return 2;
		if(ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE.equals(status))
			return 1;
		return 0;
	}


	private record EventInfo(
		String type,
		String rawDate,
		String date,
		String year,
		NormalizedDate representativeDate,
		String place,
		String deathCause,
		String eventId,
		FLEFRecord record
	){
		public boolean matchesId(final String id){
			return eventId != null && eventId.equals(id);
		}
	}


	@Override
	public String toString(){
		return nameText + " [" + id + "]";
	}

}
