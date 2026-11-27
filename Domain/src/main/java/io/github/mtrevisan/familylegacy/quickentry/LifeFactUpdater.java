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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordChangeSummary;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Changes, in place, the common facts of an individual that exists already: its sex, and the date and the place of its birth and death.
 * <p>
 * What is changed is the record that already holds the fact (the attribute, the event), the way the user would change it in the full
 * editor, and an update is added to its audit. What is cited is not touched: a date that has a source or evidence, or a place with a
 * citation of its own, is not editable here ({@link #isDateEditable}, {@link #isPlaceEditable}), because overwriting it would leave the
 * citation pointing at a value it does not support; the full editor of the event is the place to settle it (two sources that disagree
 * are two assertions, not one overwritten).
 * <p>
 * Every change gives back how to undo it, for the editor that opened it to run if it is cancelled.
 */
public final class LifeFactUpdater{

	/**
	 * What to change in an event.
	 *
	 * @param dateChanged	Whether the date is to be replaced.
	 * @param date	The new {@code DateStructure}, {@code null} to remove the date (used only if {@code dateChanged}).
	 * @param placeChanged	Whether the place is to be replaced.
	 * @param place	The new place, {@link QuickEntryService.PlaceRef#NONE} to remove it (used only if {@code placeChanged}).
	 */
	public record Edit(boolean dateChanged, FLEFRecord date, boolean placeChanged, QuickEntryService.PlaceRef place){

		public Edit{
			place = (place != null? place: QuickEntryService.PlaceRef.NONE);
		}

	}

	/**
	 * What a change did.
	 *
	 * @param addedIds	The ids of the records that were added to the model.
	 * @param undo	Puts back what there was.
	 */
	public record Applied(List<String> addedIds, Runnable undo){

		/** Nothing was changed. */
		public static final Applied NOTHING = new Applied(List.of(), () -> {});


		public Applied{
			addedIds = List.copyOf(addedIds);
		}

	}


	/** The tags that come before the date, and before the place, in an event. */
	private static final Set<String> BEFORE_DATE = Set.of(EventReader.TAG_TYPE, EventReader.TAG_DESCRIPTION);
	private static final Set<String> BEFORE_PLACE = Set.of(EventReader.TAG_TYPE, EventReader.TAG_DESCRIPTION, EventReader.TAG_DATE);


	private LifeFactUpdater(){}


	/**
	 * @param model	The model.
	 * @param individualId	The id of the individual.
	 * @param type	The type of the events ({@code birth}, {@code death}).
	 * @return	The events of the type that the individual is the principal of (a participation without role is the principal one), in the
	 * 	order of the file; empty if there is none.
	 */
	public static List<FLEFRecord> principalEvents(final FLEFModel model, final String individualId, final String type){
		final List<FLEFRecord> events = new ArrayList<>();
		if(individualId == null)
			return events;

		final Set<String> seen = new HashSet<>();
		for(final FLEFRecord record : model.getRecordsByType(EventParticipationReader.TYPE)){
			final EventHelper.Participation participation = EventHelper.toParticipation(record);
			if(!individualId.equals(participation.participantId()) || !Strings.CI.equals(IndividualReader.TYPE, participation.participantType())
					|| StringUtils.isNotBlank(participation.role())
					&& !EventParticipationReader.ENUM_ROLE_PRINCIPAL.equalsIgnoreCase(participation.role()))
				continue;

			final FLEFRecord event = model.getRecordById(participation.eventId());
			if(event != null && Strings.CI.equals(type, EventHelper.eventType(event)) && seen.add(event.getId()))
				events.add(event);
		}
		return events;
	}

	/** @return	Whether the date of the event can be replaced: there is none, or it has neither a source nor evidence. */
	public static boolean isDateEditable(final FLEFRecord event){
		final FLEFRecord date = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
		return (date == null || FLEFRecordHelper.findChild(date, DateReader.TAG_SOURCE) == null
			&& FLEFRecordHelper.findChild(date, EvidenceQualifiersReader.TAG_EVIDENCE) == null);
	}

	/** @return	Whether the place of the event can be replaced: there is none, or it is only the reference to a place. */
	public static boolean isPlaceEditable(final FLEFRecord event){
		final FLEFRecord citation = FLEFRecordHelper.findChild(event, EventReader.TAG_PLACE);
		if(citation == null)
			return true;

		for(final FLEFRecord child : citation.getChildren())
			if(!Strings.CI.equals(PlaceCitationReader.TAG_PLACE, child.getTag()))
				return false;
		return true;
	}


	/**
	 * Replaces the date and/or the place of an event.
	 *
	 * @param model	The model.
	 * @param event	The event (of the model).
	 * @param edit	What to change.
	 * @return	What was done ({@link Applied#NOTHING} if nothing was to be changed).
	 * @throws IllegalArgumentException	If a place is said to be in the file and is not (nothing is changed).
	 * @throws IllegalStateException	If the date or the place is to be changed and is not editable.
	 */
	public static Applied update(final FLEFModel model, final FLEFRecord event, final Edit edit){
		if(!edit.dateChanged() && !edit.placeChanged())
			return Applied.NOTHING;

		if(edit.dateChanged() && !isDateEditable(event))
			throw new IllegalStateException("The date has a citation: " + event.getId());
		if(edit.placeChanged() && !isPlaceEditable(event))
			throw new IllegalStateException("The place has a citation: " + event.getId());
		QuickEntryService.requireExistingPlace(model, edit.place());

		final FLEFRecord before = copyOf(event);
		final List<FLEFRecord> added = new ArrayList<>();
		try{
			if(edit.dateChanged()){
				FLEFRecordHelper.removeChildren(event, EventReader.TAG_DATE);
				if(edit.date() != null && edit.date().hasData())
					insertAfter(event, edit.date(), BEFORE_DATE);
			}
			if(edit.placeChanged()){
				FLEFRecordHelper.removeChildren(event, EventReader.TAG_PLACE);
				final String placeId = QuickEntryService.resolvePlaceId(model, added, edit.place(), new HashMap<>());
				if(placeId != null)
					insertAfter(event, FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
						.addChild(FLEFRecord.createChildWithTagAndValue(PlaceCitationReader.TAG_PLACE, placeId)), BEFORE_PLACE);
			}
		}
		catch(final RuntimeException re){
			event.clear();
			before.deepCopyTo(event);
			for(final FLEFRecord record : added)
				FLEFRecord.releaseReservedId(QuickEntryService.prefixOf(record));
			throw re;
		}

		final List<String> ids = new ArrayList<>(added.size());
		for(final FLEFRecord record : added){
			model.addRecord(record);
			ids.add(record.getId());
		}
		RecordChangeSummary.recordUpdate(before, event);
		return new Applied(ids, () -> {
			event.clear();
			before.deepCopyTo(event);
			ids.forEach(model::removeRecord);
		});
	}

	/**
	 * Sets the sex of an individual by changing its {@code sex} attribute (or adding one): an unknown sex is the attribute with the value
	 * {@code unknown}, so that what is cited by it stays.
	 *
	 * @param model	The model.
	 * @param individualId	The id of the individual.
	 * @param sex	The sex.
	 * @return	What was done ({@link Applied#NOTHING} if the individual has no sex attribute and the sex is unknown).
	 * @throws IllegalStateException	If the individual has more than one sex attribute: which one is meant is for the user to say.
	 */
	public static Applied setSex(final FLEFModel model, final String individualId, final SexType sex){
		final SexType wanted = (sex != null? sex: SexType.UNKNOWN);
		final List<FLEFRecord> assertions = PreferredSex.assertions(individualId, model);
		if(assertions.size() > 1)
			throw new IllegalStateException("More than one sex attribute: " + individualId);

		if(assertions.isEmpty()){
			if(wanted == SexType.UNKNOWN)
				return Applied.NOTHING;

			final FLEFRecord attribute = FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individualId))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, wanted.getRawSex()))
				.addChild(AuditStamp.now());
			model.addRecord(attribute);
			final String id = attribute.getId();
			return new Applied(List.of(id), () -> model.removeRecord(id));
		}

		final FLEFRecord attribute = assertions.getFirst();
		if(SexType.fromRaw(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE)) == wanted)
			return Applied.NOTHING;

		final FLEFRecord before = copyOf(attribute);
		FLEFRecordHelper.updateChildValue(attribute, IndividualAttributeReader.TAG_VALUE, wanted.getRawSex());
		RecordChangeSummary.recordUpdate(before, attribute);
		return new Applied(List.of(), () -> {
			attribute.clear();
			before.deepCopyTo(attribute);
		});
	}


	/** The tags that make an occupation more than its text: a citation, a degree of confidence, the dates, the place. */
	private static final Set<String> OCCUPATION_DETAILS = Set.of(IndividualAttributeReader.TAG_SOURCE, IndividualAttributeReader.TAG_CONFIDENCE,
		IndividualAttributeReader.TAG_VALID_FROM, IndividualAttributeReader.TAG_VALID_TO, IndividualAttributeReader.TAG_PLACE);


	/**
	 * @param model	The model.
	 * @param individualId	The id of the individual.
	 * @return	The attributes of the type {@code occupation} of the individual, in the order of the file.
	 */
	public static List<FLEFRecord> occupations(final FLEFModel model, final String individualId){
		final List<FLEFRecord> occupations = new ArrayList<>();
		if(individualId == null)
			return occupations;

		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeReader.TYPE))
			if(individualId.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL))
					&& Strings.CI.equals(IndividualAttributeReader.ENUM_TYPE_OCCUPATION,
					FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
				occupations.add(attribute);
		return occupations;
	}

	/**
	 * @param attribute	An occupation.
	 * @return	Whether its text can be replaced: it has no source, no confidence, no dates and no place of its own (they are about the
	 * 	occupation that the text says, and would be left pointing at another one).
	 */
	public static boolean isOccupationEditable(final FLEFRecord attribute){
		for(final FLEFRecord child : attribute.getChildren())
			if(child.getTag() != null && OCCUPATION_DETAILS.contains(child.getTag().toLowerCase(java.util.Locale.ROOT)))
				return false;
		return true;
	}

	/**
	 * Sets the occupation of an individual by changing the text of its {@code occupation} attribute, adding one if there is none, and
	 * removing it if the text is blank.
	 *
	 * @param model	The model.
	 * @param individualId	The id of the individual.
	 * @param occupation	The occupation (blank: none).
	 * @return	What was done ({@link Applied#NOTHING} if the occupation is the one there was).
	 * @throws IllegalStateException	If the individual has more than one occupation, or the one it has is not editable
	 * 	({@link #isOccupationEditable}).
	 */
	public static Applied setOccupation(final FLEFModel model, final String individualId, final String occupation){
		final String text = StringUtils.defaultString(StringUtils.normalizeSpace(occupation));
		final List<FLEFRecord> occupations = occupations(model, individualId);
		if(occupations.size() > 1)
			throw new IllegalStateException("More than one occupation: " + individualId);

		if(occupations.isEmpty()){
			if(text.isEmpty())
				return Applied.NOTHING;

			final FLEFRecord attribute = QuickEntryService.buildOccupation(model, individualId, text);
			model.addRecord(attribute);
			final String id = attribute.getId();
			return new Applied(List.of(id), () -> model.removeRecord(id));
		}

		final FLEFRecord attribute = occupations.getFirst();
		if(!isOccupationEditable(attribute))
			throw new IllegalStateException("The occupation has details of its own: " + attribute.getId());

		if(text.equals(StringUtils.normalizeSpace(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE))))
			return Applied.NOTHING;

		if(text.isEmpty()){
			model.removeRecord(attribute.getId());
			return new Applied(List.of(), () -> model.addRecord(attribute));
		}

		final FLEFRecord before = copyOf(attribute);
		FLEFRecordHelper.updateChildValue(attribute, IndividualAttributeReader.TAG_VALUE, text);
		RecordChangeSummary.recordUpdate(before, attribute);
		return new Applied(List.of(), () -> {
			attribute.clear();
			before.deepCopyTo(attribute);
		});
	}

	private static FLEFRecord copyOf(final FLEFRecord record){
		final FLEFRecord copy = FLEFRecord.createEmpty();
		record.deepCopyTo(copy);
		return copy;
	}

	/** Puts the child after the last child whose tag is one of the given (at the start if there is none), so that the event keeps its order. */
	private static void insertAfter(final FLEFRecord event, final FLEFRecord child, final Set<String> tags){
		final List<FLEFRecord> children = event.getChildren();
		int index = 0;
		for(int i = 0; i < children.size(); i ++){
			final String tag = children.get(i).getTag();
			if(tags.stream().anyMatch(before -> Strings.CI.equals(before, tag)))
				index = i + 1;
		}
		children.add(index, child);
	}

}
