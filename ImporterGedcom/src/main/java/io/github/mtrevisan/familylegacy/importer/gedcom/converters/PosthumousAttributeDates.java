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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GedcomTexts;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.DateRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Gives a plausible date to the attributes that the file dates after the death of the individual.
 * <p>
 * The date of an attribute that is a statement about an individual who is already dead (the occupation of the father in the death
 * record of his child, {@code fu Giovanni, contadino}) is the date of the record, not the one the fact began: the fact is true at
 * some time of the life, that is, not after the death. So the validity becomes <em>before the death</em> ({@code BEF}), and the
 * date written in the file is kept in a note.
 * <p>
 * It runs when all the events are known, and it changes only an attribute whose beginning is certainly after the death of an
 * individual that has a single death with a latest day (a precise date, or "before" one: the bound is the same date; a death
 * without it, or a spanning one, is left alone: the checks of the file judge it). A
 * fact observed at a date (Gramps writes it as <em>not after</em> and <em>not before</em> the date) is changed when that date is
 * after the death: it would be a fact that goes on after the death.
 */
public final class PosthumousAttributeDates{

	private PosthumousAttributeDates(){}


	/**
	 * Replaces the validity of the attributes that begin after the death of their individual.
	 *
	 * @param model	The model of the file.
	 * @return	How many attributes have been changed.
	 */
	public static int convert(final FLEFModel model){
		final Map<String, List<FLEFRecord>> deathDates = deathDates(model);
		if(deathDates.isEmpty())
			return 0;

		int changed = 0;
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeReader.TYPE)){
			final List<FLEFRecord> dates = deathDates.get(IndividualAttributeReader.extractIndividualId(attribute));
			if(dates != null && convert(attribute, dates))
				changed ++;
		}
		return changed;
	}

	/** The dates of the death events of each individual that is a principal of them. */
	private static Map<String, List<FLEFRecord>> deathDates(final FLEFModel model){
		final Map<String, List<FLEFRecord>> deaths = new HashMap<>();
		for(final FLEFRecord record : model.getRecordsByType(EventParticipationReader.TYPE)){
			final EventHelper.Participation participation = EventHelper.toParticipation(record);
			if(participation.participantId() == null || !EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL.equals(participation.participantType())
					|| !EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(participation.role()))
				continue;

			final FLEFRecord event = model.getRecordById(participation.eventId());
			if(!EventReader.isOfType(event) || !EventReader.ENUM_TYPE_DEATH.equals(EventReader.extractType(event)))
				continue;

			final FLEFRecord date = FLEFRecordHelper.findChild(event, EventReader.TAG_DATE);
			if(date != null)
				deaths.computeIfAbsent(participation.participantId(), k -> new ArrayList<>())
					.add(date);
		}
		return deaths;
	}

	private static boolean convert(final FLEFRecord attribute, final List<FLEFRecord> deathDates){
		//a single death with a latest day (a point, or a "before"): with more than one, or without a latest day, there is no day to put the bound on
		if(deathDates.size() != 1)
			return false;

		final FLEFRecord death = deathDates.getFirst();
		FLEFRecord point = FLEFRecordHelper.findChild(death, FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_POINT));
		if(point == null)
			//the death was "before" a date (or between two): the fact is not after the latest day of the death either
			point = FLEFRecordHelper.findChild(death, FLEFRecordHelper.composePath(DateReader.TAG_VALUE, DateReader.TAG_BOUNDED,
				DateReader.TAG_NOT_AFTER));
		final FLEFRecord validFrom = FLEFRecordHelper.findChild(attribute, IndividualAttributeReader.TAG_VALID_FROM);
		if(point == null || validFrom == null)
			return false;

		final DateRange deathRange = range(death);
		final DateRange fromRange = range(validFrom);
		if(deathRange == null || fromRange == null)
			return false;

		final FLEFRecord validTo = FLEFRecordHelper.findChild(attribute, IndividualAttributeReader.TAG_VALID_TO);
		//the fact is dated after the death: it begins then (GEDCOM), or it was observed then (Gramps: not after the date, and not before it)
		final String observed = observedAt(validFrom, validTo);
		final boolean observedAfterDeath = (observed != null && deathRange.isBefore(range(validTo)));
		if(!observedAfterDeath && !DateRules.period(fromRange, null, null, deathRange).contains(DateRules.PeriodProblem.BEGINS_AFTER_DEATH))
			return false;

		final FLEFRecord notAfter = FLEFRecord.createEmpty();
		point.deepCopyTo(notAfter);
		notAfter.setTag(DateReader.TAG_NOT_AFTER);
		final FLEFRecord bound = FLEFRecord.createChildWithTag(IndividualAttributeReader.TAG_VALID_FROM)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
				.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_BOUNDED)
					.addChild(notAfter)));

		final String original = (observed != null? observed: DateReader.extractPrettyPrintDate(validFrom));
		final String deathText = DateReader.extractPrettyPrintDate(death);
		final List<FLEFRecord> children = attribute.getChildren();
		final int index = children.indexOf(validFrom);
		children.set(index, bound);
		//the end was the date of the record as well: it says nothing about when the fact ended
		if(validTo != null)
			attribute.removeChild(validTo);
		final FLEFRecord note = NoteBuilder.createNoteStruct(GedcomTexts.t("title.date_original"),
			GedcomTexts.tf("text.date_before_death", original, deathText), null);
		if(note != null)
			children.add(index + 1, note);
		return true;
	}

	/** @return	The date a fact was observed at (not after it, and not before it, as Gramps writes a single date), or {@code null}. */
	private static String observedAt(final FLEFRecord validFrom, final FLEFRecord validTo){
		if(validTo == null)
			return null;

		final String notAfter = FLEFRecordHelper.getChildValue(validFrom, FLEFRecordHelper.composePath(DateReader.TAG_VALUE,
			DateReader.TAG_BOUNDED, DateReader.TAG_NOT_AFTER, DateReader.TAG_VALUE_FULL_DATE));
		final String notBefore = FLEFRecordHelper.getChildValue(validTo, FLEFRecordHelper.composePath(DateReader.TAG_VALUE,
			DateReader.TAG_BOUNDED, DateReader.TAG_NOT_BEFORE, DateReader.TAG_VALUE_FULL_DATE));
		return (notAfter != null && notAfter.equals(notBefore)? notAfter: null);
	}

	private static DateRange range(final FLEFRecord dateStructure){
		try{
			return DateRange.of(DateNormalizer.normalize(dateStructure));
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

}
