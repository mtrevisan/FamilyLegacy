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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.quality.FileValidation;
import io.github.mtrevisan.familylegacy.reports.index.RelationIndex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Collects what a researcher has still to verify in a file: what is wrong in it (the structural checks of the validation, the
 * contradicting citations, the media that are not found), what is missing from the vital events (a date, a place, a source), the
 * individuals with no birth date, the sources nobody cites and the research tasks that are not done.
 * <p>
 * It does not repeat the checks: the problems of the file are the ones of {@link FileValidation#structure(FLEFModel, String)} (the
 * grammar of the protocol is not run here: it is a validation of the format, not a thing to research). It is read only, and it
 * judges only what is written in the records: a birth that is not recorded is a thing to find, not an error. The findings of the
 * research assistant (research gaps, the chronology, the citations, the identities) are computed by the application, not here: they
 * are in the assistant.
 */
public final class ToVerifyFinder{

	/** What a finding is about, in the order the report writes them. */
	public enum Kind{
		/** What the structural checks of the validation find (circles of parentage, impossible dates, ...). */
		FILE_PROBLEM,
		/** A citation whose stance is that the source contradicts the record. */
		CONTRADICTING_CITATION,
		/** A media whose file is not found. */
		MEDIA_MISSING,
		/** An individual that has no dated birth. */
		NO_BIRTH_DATE,
		/** A vital event (birth, death, burial, cremation, marriage) with no date. */
		EVENT_NO_DATE,
		/** A vital event with no place. */
		EVENT_NO_PLACE,
		/** A vital event that no source supports. */
		EVENT_NO_SOURCE,
		/** A source that no record cites. */
		SOURCE_NOT_CITED,
		/** A research task that is open or in progress. */
		OPEN_TASK;

		/** @return	the part of the key of the label of the kind, in the report labels ({@code TO_VERIFY_<key>}) */
		public String key(){
			return name();
		}
	}

	/**
	 * A thing to verify.
	 *
	 * @param kind	what it is about
	 * @param recordId	the record it is about ({@code null} if the issue is about no record)
	 * @param detail	what is to say about it, or an empty text
	 */
	public record Finding(Kind kind, String recordId, String detail){

		public Finding{
			detail = StringUtils.defaultString(detail);
		}
	}


	/** The events that make the life of a person and that a researcher expects to be dated, placed and sourced. */
	private static final Set<String> VITAL_EVENT_TYPES = Set.of(EventReader.ENUM_TYPE_BIRTH, EventReader.ENUM_TYPE_DEATH,
		EventReader.ENUM_TYPE_BURIAL, EventReader.ENUM_TYPE_CREMATION, EventReader.ENUM_TYPE_MARRIAGE);
	private static final String MEDIA_MISSING_KIND = "media_missing";


	private ToVerifyFinder(){}


	/**
	 * @param model	the model
	 * @param visible	whether a record can be shown (the privacy policy of the report); what is about a hidden record is not told
	 * @param mediaBaseDirectory	the directory the relative locations of the media are relative to ({@code null}: they are not judged)
	 * @return	the findings, by the order of the kinds, and inside each kind in the order of the records
	 */
	public static List<Finding> find(final FLEFModel model, final Predicate<FLEFRecord> visible, final String mediaBaseDirectory){
		final List<Finding> out = new ArrayList<>();
		fileProblems(model, visible, mediaBaseDirectory, out);
		contradictingCitations(model, visible, out);
		missingFromTheLife(model, visible, out);
		sourcesNotCited(model, visible, out);
		openTasks(model, visible, out);

		//the kinds in their order, stable inside each
		out.sort(Comparator.comparing(Finding::kind));
		return out;
	}


	private static void fileProblems(final FLEFModel model, final Predicate<FLEFRecord> visible, final String mediaBaseDirectory,
			final List<Finding> out){
		for(final FLEFValidator.ValidationError error : FileValidation.structure(model, mediaBaseDirectory)){
			if(error.recordId() != null && !isVisible(model, visible, error.recordId()))
				continue;

			out.add(new Finding((MEDIA_MISSING_KIND.equals(error.kind())? Kind.MEDIA_MISSING: Kind.FILE_PROBLEM), error.recordId(), error.message()));
		}
	}

	private static void contradictingCitations(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<Finding> out){
		for(final FLEFRecord record : model.getRecords()){
			if(SourceReader.isOfType(record) || !visible.test(record))
				continue;

			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_SOURCE)){
				if(!SourceCitationReader.isContradicting(citation))
					continue;

				final FLEFRecord source = model.getRecordById(SourceCitationReader.extractSource(citation));
				out.add(new Finding(Kind.CONTRADICTING_CITATION, record.getId(), (source != null && visible.test(source)
					? SourceReader.displayText(source): StringUtils.defaultString(SourceCitationReader.extractSource(citation)))));
			}
		}
	}

	private static void missingFromTheLife(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<Finding> out){
		final Map<String, List<EventHelper.Participation>> participations = EventHelper.participationsByEvent(model);
		final Set<String> withBirthDate = new HashSet<>();

		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			if(!visible.test(event))
				continue;

			final String type = EventHelper.eventType(event);
			if(type == null || !VITAL_EVENT_TYPES.contains(type))
				continue;

			final boolean dated = (EventHelper.eventDateRaw(event) != null);
			if(EventReader.isTypeBirth(type) && dated)
				for(final EventHelper.Participation participation : participations.getOrDefault(event.getId(), List.of()))
					if(isTheOneWhoIsBorn(participation))
						withBirthDate.add(participation.participantId());

			if(!dated)
				out.add(new Finding(Kind.EVENT_NO_DATE, event.getId(), StringUtils.EMPTY));
			if(EventHelper.eventPlaceId(event) == null && FLEFRecordHelper.findChild(event, EventReader.TAG_PLACE) == null)
				out.add(new Finding(Kind.EVENT_NO_PLACE, event.getId(), StringUtils.EMPTY));
			if(!SourceCitationReader.hasSupportingCitation(event))
				out.add(new Finding(Kind.EVENT_NO_SOURCE, event.getId(), StringUtils.EMPTY));
		}

		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE))
			if(visible.test(individual) && !withBirthDate.contains(individual.getId()))
				out.add(new Finding(Kind.NO_BIRTH_DATE, individual.getId(), StringUtils.EMPTY));
	}

	/** An individual that takes part in a birth with no role, or as the principal one, is the one born (not a witness, not a parent). */
	private static boolean isTheOneWhoIsBorn(final EventHelper.Participation participation){
		return (EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL.equals(participation.participantType())
			&& participation.participantId() != null
			&& (StringUtils.isBlank(participation.role()) || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(participation.role())));
	}

	private static void sourcesNotCited(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<Finding> out){
		final List<FLEFRecord> sources = model.getRecordsByType(SourceReader.TYPE);
		if(sources.isEmpty())
			return;

		final RelationIndex index = RelationIndex.build(model, visible);
		for(final FLEFRecord source : sources)
			if(visible.test(source) && index.citationsOfSource(source).isEmpty())
				out.add(new Finding(Kind.SOURCE_NOT_CITED, source.getId(), StringUtils.EMPTY));
	}

	private static void openTasks(final FLEFModel model, final Predicate<FLEFRecord> visible, final List<Finding> out){
		for(final FLEFRecord task : model.getRecordsByType(ResearchTaskReader.TYPE)){
			if(!visible.test(task))
				continue;

			final String status = ResearchTaskReader.extractStatus(task);
			if(ResearchTaskReader.ENUM_STATUS_OPEN.equals(status) || ResearchTaskReader.ENUM_STATUS_IN_PROGRESS.equals(status))
				out.add(new Finding(Kind.OPEN_TASK, task.getId(), StringUtils.defaultString(ResearchTaskReader.extractDescription(task))));
		}
	}


	private static boolean isVisible(final FLEFModel model, final Predicate<FLEFRecord> visible, final String recordId){
		final FLEFRecord record = model.getRecordById(recordId);
		return (record == null || visible.test(record));
	}

}
