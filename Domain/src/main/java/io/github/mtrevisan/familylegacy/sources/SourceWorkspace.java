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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentifierReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Everything that hangs from one source: the citations that name it, the individuals, families and events those citations back, the
 * extracts that belong to it and the media it points to. It is the source-centric view of the file, the reverse of the provenance of an
 * assertion: {@code source -> citation -> record -> individual}.
 * <p>
 * The same class lists the sources by where they are kept ({@link #group(FLEFModel, Grouping)}): by repository, by kind of repository
 * (an archive, a library, a private collection...) and by the shelf mark of the repository citation, that names the collection or the
 * record group inside the repository.
 * <p>
 * It is read from the model and never changes it; the records hidden by the privacy policy are left out.
 *
 * @param source	The source.
 * @param citations	The citations of the source, in the order of the file.
 * @param individuals	The individuals that the citations talk about (the cited individual, the subject and target of a cited relationship,
 * 	the participants of a cited event or participation, the owner of a cited attribute).
 * @param families	The partnerships that cite the source.
 * @param events	The events that cite the source, or whose participation does.
 * @param extracts	The extracts of the source, cited or not.
 * @param media	The media that the source or its extracts refer to.
 */
public record SourceWorkspace(FLEFRecord source, List<Citation> citations, List<FLEFRecord> individuals, List<FLEFRecord> families,
		List<FLEFRecord> events, List<FLEFRecord> extracts, List<FLEFRecord> media){

	/**
	 * One citation of the source.
	 *
	 * @param owner	The record that carries the citation (the assertion).
	 * @param locator	The place in the source (empty if none).
	 * @param stance	{@code supports} or {@code contradicts}.
	 * @param extracts	The extracts the citation references.
	 * @param individuals	The individuals the assertion is about: where the way back from the source ends.
	 */
	public record Citation(FLEFRecord owner, String locator, String stance, List<FLEFRecord> extracts, List<FLEFRecord> individuals){

		/** @return	Whether the citation contradicts the assertion. */
		public boolean contradicts(){
			return SourceCitationReader.STANCE_CONTRADICTS.equals(stance);
		}

	}

	/** What the sources are grouped by. */
	public enum Grouping{
		/** One group for each repository. */
		REPOSITORY,
		/** One group for each kind of repository: archive, library, private collection... */
		REPOSITORY_TYPE,
		/** One group for each shelf mark or call number inside a repository: its collection or record group. */
		RECORD_GROUP
	}

	/**
	 * @param key	What the sources have in common: the name of the repository, its type as written in the file, the shelf mark; empty for
	 * 	the sources that say nothing about it.
	 * @param sources	The sources of the group, by title.
	 */
	public record SourceGroup(String key, List<FLEFRecord> sources){}


	/** The group of the sources that have no repository, or no repository of a known type, or no shelf mark. */
	public static final String NO_GROUP = StringUtils.EMPTY;


	/**
	 * Reads the workspace of a source.
	 *
	 * @param model	The model.
	 * @param sourceId	The identifier of the source.
	 * @return	The workspace; {@code null} if there is no such source or it is hidden by the privacy policy.
	 */
	public static SourceWorkspace of(final FLEFModel model, final String sourceId){
		final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);
		if(source == null || !SourceReader.isOfType(source) || PrivacyService.isHidden(model, source))
			return null;

		final Map<String, List<String>> participantsOfEvent = participantsOfEvents(model);

		final List<Citation> citations = new ArrayList<>();
		final Map<String, FLEFRecord> individuals = new LinkedHashMap<>();
		final Map<String, FLEFRecord> families = new LinkedHashMap<>();
		final Map<String, FLEFRecord> events = new LinkedHashMap<>();
		for(final String type : SourceHelper.allSourceCitingTypes())
			for(final FLEFRecord owner : PrivacyService.visible(model, model.getRecordsByType(type)))
				for(final FLEFRecord citation : owner.getChildren()){
					//the sources that a source is derived from are not citations of it
					if(!SourceReader.isOfType(citation) || !sourceId.equals(SourceHelper.extractReferencedId(citation)))
						continue;

					final List<FLEFRecord> people = individualsOf(model, owner, participantsOfEvent);
					final String stance = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE);
					citations.add(new Citation(owner, StringUtils.defaultString(FLEFRecordHelper.getChildValue(citation,
						SourceCitationReader.TAG_LOCATOR)), (stance != null? stance: SourceCitationReader.STANCE_SUPPORTS),
						ExtractReader.extractsOf(citation, model), people));
					for(final FLEFRecord person : people)
						individuals.putIfAbsent(person.getId(), person);
					collectFamilyAndEvent(model, owner, families, events);
				}

		final List<FLEFRecord> extracts = PrivacyService.visible(model, ExtractReader.extractsOfSource(sourceId, model));
		return new SourceWorkspace(source, List.copyOf(citations), List.copyOf(individuals.values()), List.copyOf(families.values()),
			List.copyOf(events.values()), List.copyOf(extracts), mediaOf(model, source, extracts));
	}

	private static void collectFamilyAndEvent(final FLEFModel model, final FLEFRecord owner, final Map<String, FLEFRecord> families,
			final Map<String, FLEFRecord> events){
		if(RelationshipReader.isOfType(owner)){
			final String type = RelationshipReader.extractType(owner);
			if(type != null && RelationshipReader.isTypePartner(type))
				families.putIfAbsent(owner.getId(), owner);
		}
		else if(EventReader.isOfType(owner))
			events.putIfAbsent(owner.getId(), owner);
		else if(EventParticipationReader.isOfType(owner)){
			//a cited participation backs the event as well
			final FLEFRecord event = model.getRecordById(FLEFRecordHelper.getChildValue(owner, EventParticipationReader.TAG_EVENT));
			if(event != null && EventReader.isOfType(event) && PrivacyService.isVisible(model, event))
				events.putIfAbsent(event.getId(), event);
		}
	}

	/** The identifiers of the individuals that take part in each event. */
	private static Map<String, List<String>> participantsOfEvents(final FLEFModel model){
		final Map<String, List<String>> participants = new LinkedHashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final String individualId = EventParticipationReader.extractParticipantRef(participation,
				EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL);
			if(eventId != null && individualId != null)
				participants.computeIfAbsent(eventId, key -> new ArrayList<>())
					.add(individualId);
		}
		return participants;
	}

	private static List<FLEFRecord> individualsOf(final FLEFModel model, final FLEFRecord owner,
			final Map<String, List<String>> participantsOfEvent){
		final List<String> ids = new ArrayList<>(2);
		if(IndividualReader.isOfType(owner))
			ids.add(owner.getId());
		else if(IndividualAttributeReader.isOfType(owner))
			ids.add(FLEFRecordHelper.getChildValue(owner, IndividualAttributeReader.TAG_INDIVIDUAL));
		else if(EventReader.isOfType(owner))
			ids.addAll(participantsOfEvent.getOrDefault(owner.getId(), List.of()));
		else if(EventParticipationReader.isOfType(owner))
			ids.add(EventParticipationReader.extractParticipantRef(owner, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL));
		else if(RelationshipReader.isOfType(owner)){
			ids.add(EventParticipationReader.extractParticipantRef(owner, RelationshipReader.TAG_SUBJECT));
			ids.add(EventParticipationReader.extractParticipantRef(owner, RelationshipReader.TAG_TARGET));
		}

		final Map<String, FLEFRecord> people = new LinkedHashMap<>(ids.size());
		for(final String id : ids){
			final FLEFRecord record = (id != null? model.getRecordById(id): null);
			if(record != null && IndividualReader.isOfType(record) && PrivacyService.isVisible(model, record))
				people.putIfAbsent(id, record);
		}
		return List.copyOf(people.values());
	}

	private static List<FLEFRecord> mediaOf(final FLEFModel model, final FLEFRecord source, final List<FLEFRecord> extracts){
		final Map<String, FLEFRecord> media = new LinkedHashMap<>();
		for(final FLEFRecord reference : FLEFRecordHelper.findChildren(source, SourceReader.TAG_MEDIA))
			addMedium(model, reference.getValue(), media);
		for(final FLEFRecord extract : extracts)
			for(final FLEFRecord part : FLEFRecordHelper.findChildren(extract, ExtractReader.TAG_MEDIA_PART_MEDIA))
				addMedium(model, part.getValue(), media);
		return List.copyOf(media.values());
	}

	private static void addMedium(final FLEFModel model, final String id, final Map<String, FLEFRecord> media){
		final FLEFRecord medium = (StringUtils.isNotBlank(id)? model.getRecordById(id.strip()): null);
		if(medium != null && PrivacyService.isVisible(model, medium))
			media.putIfAbsent(medium.getId(), medium);
	}


	/**
	 * Lists the sources by where they are kept. A source with several repositories is in several groups; one with none is in the
	 * {@link #NO_GROUP} group, that is the last.
	 *
	 * @param model	The model.
	 * @param grouping	What to group by.
	 * @return	The groups, by key; every visible source is in at least one.
	 */
	public static List<SourceGroup> group(final FLEFModel model, final Grouping grouping){
		final Map<String, List<FLEFRecord>> groups = new TreeMap<>(Comparator.<String, Boolean>comparing(String::isEmpty)
			.thenComparing(String.CASE_INSENSITIVE_ORDER)
			.thenComparing(Comparator.naturalOrder()));
		for(final FLEFRecord source : SourceHelper.listAllSources(model)){
			final List<String> keys = keysOf(model, source, grouping);
			if(keys.isEmpty())
				keys.add(NO_GROUP);
			for(final String key : keys)
				groups.computeIfAbsent(key, k -> new ArrayList<>())
					.add(source);
		}

		final Comparator<FLEFRecord> byTitle = Comparator.comparing((FLEFRecord source) -> SourceHelper.sourceTitle(source),
				String.CASE_INSENSITIVE_ORDER)
			.thenComparing(FLEFRecord::getId);
		final List<SourceGroup> result = new ArrayList<>(groups.size());
		for(final Map.Entry<String, List<FLEFRecord>> entry : groups.entrySet()){
			entry.getValue().sort(byTitle);
			result.add(new SourceGroup(entry.getKey(), List.copyOf(entry.getValue())));
		}
		return result;
	}

	/** The keys of the groups a source belongs to, without repetitions. */
	private static List<String> keysOf(final FLEFModel model, final FLEFRecord source, final Grouping grouping){
		final List<String> keys = new ArrayList<>(1);
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_REPOSITORY)){
			final FLEFRecord repository = model.getRecordById(FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_REPOSITORY));
			if(repository == null || PrivacyService.isHidden(model, repository))
				continue;

			final String name = SourceHelper.repositoryName(repository);
			switch(grouping){
				case REPOSITORY -> addKey(keys, name);
				case REPOSITORY_TYPE -> addKey(keys, SourceHelper.firstTextValue(repository, RepositoryReader.TAG_TYPE));
				case RECORD_GROUP -> {
					for(final FLEFRecord identifier : RepositoryCitationReader.extractIdentifiers(citation)){
						final String value = StringUtils.trimToNull(IdentifierReader.extractValue(identifier));
						if(value != null)
							addKey(keys, name + " / " + value);
					}
				}
			}
		}
		return keys;
	}

	private static void addKey(final List<String> keys, final String key){
		final String stripped = StringUtils.strip(key);
		if(StringUtils.isNotEmpty(stripped) && !keys.contains(stripped))
			keys.add(stripped);
	}

}
