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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionResolution;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The timeline of the research (the work, not the life of the people): when a research task was created or completed, a repository was
 * visited, a source was added, an extract or a citation was created, a conclusion was written or updated, a conflict was resolved.
 * <p>
 * The file has no journal of its own: the timeline is read from what the records say about themselves, that is the {@code audit}
 * (creation and updates) of each record. So an entry is as precise as the audit: a citation, that has no audit of its own, is dated by the
 * update of the record that received it (a citation written together with its record is not an entry, because nothing tells it apart
 * from the record), and a record with no audit goes at the end, undated.
 * </p>
 * Each entry knows the records it is about ({@code involved}: the individuals, the families and the like, reached through the targets
 * of the research questions, the citations and the conclusions), the sources and the repositories, so that the timeline can be filtered.
 */
public final class ResearchTimeline{

	/** What happened. The order is the one used for the entries of the same instant. */
	public enum Kind{
		SOURCE_ADDED, EXTRACT_CREATED, CITATION_ADDED, TASK_CREATED, ACTIVITY, REPOSITORY_VISITED, TASK_COMPLETED, CONCLUSION_CREATED,
		CONCLUSION_UPDATED, CONFLICT_RESOLVED
	}

	/**
	 * One thing that happened.
	 *
	 * @param when	The instant, as written in the audit ({@code 2026-03-01} or {@code 2026-03-01T10:20:30Z}); {@code null} if not known.
	 * @param day	The day of {@code when}, or {@code null}.
	 * @param kind	What happened.
	 * @param recordId	The record it happened to (the task, the source, the extract, the record that got the citation...).
	 * @param title	What the record is (the description of the task, the title of the source...).
	 * @param detail	More about it: what the update changed, the repositories visited, the result of the activity; may be empty.
	 * @param involved	The records it is about: individuals, families and the other targets of the research.
	 * @param sourceIds	The sources it is about.
	 * @param repositoryIds	The repositories it is about.
	 */
	public record Entry(String when, LocalDate day, Kind kind, String recordId, String title, String detail, Set<String> involved,
			Set<String> sourceIds, Set<String> repositoryIds){}

	/**
	 * What to keep of the timeline; a {@code null} field keeps everything.
	 *
	 * @param individualId	An individual: the entries about them.
	 * @param familyId	A family (a partnership, a group): the entries about it.
	 * @param sourceId	A source.
	 * @param repositoryId	A repository.
	 * @param from	The first day (included); the entries with no date are left out if there is one.
	 * @param to	The last day (included); the entries with no date are left out if there is one.
	 */
	public record Filter(String individualId, String familyId, String sourceId, String repositoryId, LocalDate from, LocalDate to){

		/** Keeps everything. */
		public static final Filter NONE = new Filter(null, null, null, null, null, null);


		/** @return	Whether the entry passes the filter. */
		public boolean matches(final Entry entry){
			if(individualId != null && !entry.involved().contains(individualId)
					|| familyId != null && !entry.involved().contains(familyId)
					|| sourceId != null && !entry.sourceIds().contains(sourceId)
					|| repositoryId != null && !entry.repositoryIds().contains(repositoryId))
				return false;
			if(from == null && to == null)
				return true;

			final LocalDate day = entry.day();
			return (day != null && (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to)));
		}

	}


	/** The order of the timeline: the oldest first, the undated last, then the kind and the record. */
	public static final Comparator<Entry> ORDER = Comparator.comparing((Entry entry) -> entry.when() == null)
		.thenComparing(entry -> StringUtils.defaultString(entry.when()))
		.thenComparing(Entry::kind)
		.thenComparing(entry -> StringUtils.defaultString(entry.recordId()));

	private static final String ARROW_COMPLETED = " → " + ResearchTaskReader.ENUM_STATUS_COMPLETED;
	private static final String ADDED = "+ ";
	private static final String CHANGED_STATUS = "~ " + ResearchTaskReader.TAG_STATUS;
	/** The activities that are about looking at what a repository holds. */
	private static final Set<String> VISITS = Set.of(ResearchActivityReader.ENUM_ACTIVITY_TYPE_REVIEW, "search");


	private final FLEFModel model;
	private final Map<String, FLEFRecord> questionsById = new HashMap<>();
	private final Map<String, Set<String>> participantsByEvent = new HashMap<>();
	/** The records that cite each source, with the records they are about. */
	private final Map<String, Set<String>> involvedBySource = new HashMap<>();
	private final List<Entry> entries = new ArrayList<>();


	private ResearchTimeline(final FLEFModel model){
		this.model = model;
	}


	/**
	 * @param model	The model.
	 * @return	Every entry of the research of the file, the visible ones, in {@link #ORDER}.
	 */
	public static List<Entry> build(final FLEFModel model){
		final ResearchTimeline timeline = new ResearchTimeline(model);
		timeline.collect();
		timeline.entries.sort(ORDER);
		return List.copyOf(timeline.entries);
	}

	/**
	 * @param entries	The entries.
	 * @param filter	The filter.
	 * @return	The entries that pass the filter, in the same order.
	 */
	public static List<Entry> filter(final List<Entry> entries, final Filter filter){
		return entries.stream()
			.filter(filter::matches)
			.toList();
	}


	private void collect(){
		for(final FLEFRecord question : model.getRecordsByType(ResearchQuestionReader.TYPE))
			questionsById.put(question.getId(), question);
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final String individualId = EventParticipationReader.extractParticipantRef(participation,
				EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL);
			if(eventId != null && individualId != null)
				participantsByEvent.computeIfAbsent(eventId, key -> new LinkedHashSet<>())
					.add(individualId);
		}
		indexCitations();

		collectTasks();
		collectActivities();
		collectSources();
		collectExtracts();
		collectConclusions();
		collectCitations();
	}

	/** Reads, once, which records cite each source. */
	private void indexCitations(){
		for(final String type : SourceHelper.allSourceCitingTypes())
			for(final FLEFRecord owner : model.getRecordsByType(type))
				for(final FLEFRecord citation : FLEFRecordHelper.findChildren(owner, SourceReader.TYPE)){
					final String sourceId = SourceHelper.extractReferencedId(citation);
					if(sourceId != null)
						involvedBySource.computeIfAbsent(sourceId, key -> new LinkedHashSet<>())
							.addAll(involvedOf(owner.getId()));
				}
	}


	private void collectTasks(){
		for(final FLEFRecord task : PrivacyService.visible(model, model.getRecordsByType(ResearchTaskReader.TYPE))){
			final Set<String> involved = involvedOfQuestions(OpenTasks.references(task, ResearchTaskReader.TAG_QUESTION));
			final String title = StringUtils.defaultIfBlank(ResearchHelper.taskDescription(task), task.getId());

			final String created = creationDate(task);
			add(created, Kind.TASK_CREATED, task.getId(), title, StringUtils.EMPTY, involved, Set.of(), Set.of());

			if(!ResearchTaskReader.ENUM_STATUS_COMPLETED.equalsIgnoreCase(StringUtils.strip(ResearchHelper.status(task))))
				continue;

			//the update that completed it; else the last update, else the creation (it was written as completed)
			String when = created;
			String detail = StringUtils.EMPTY;
			boolean found = false;
			for(final Update update : updates(task)){
				final boolean completes = update.comment().lines().anyMatch(ResearchTimeline::completesTask);
				if(update.date() != null && (completes || !found)){
					when = update.date();
					detail = (completes? flat(update.comment()): StringUtils.EMPTY);
					found = completes;
				}
			}
			add(when, Kind.TASK_COMPLETED, task.getId(), title, detail, involved, Set.of(), Set.of());
		}
	}

	private static boolean completesTask(final String line){
		return (line.startsWith(CHANGED_STATUS) && line.endsWith(ARROW_COMPLETED));
	}

	private void collectActivities(){
		for(final FLEFRecord activity : PrivacyService.visible(model, model.getRecordsByType(ResearchActivityReader.TYPE))){
			final Set<String> involved = involvedOfQuestions(OpenTasks.references(activity, ResearchActivityReader.TAG_QUESTION));
			final Set<String> sourceIds = new LinkedHashSet<>();
			final Set<String> repositoryIds = new LinkedHashSet<>();
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(activity, ResearchActivityReader.TAG_SOURCE)){
				final String sourceId = SourceHelper.extractReferencedId(citation);
				if(sourceId != null){
					sourceIds.add(sourceId);
					repositoryIds.addAll(repositoriesOf(sourceId));
				}
			}
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(activity, ResearchActivityReader.TAG_TARGET)){
				final String targetId = OpenTasks.referenceOf(target);
				final FLEFRecord record = (targetId != null? model.getRecordById(targetId): null);
				if(record == null)
					continue;

				involved.addAll(involvedOf(targetId));
				if(RepositoryReader.isOfType(record))
					repositoryIds.add(targetId);
				else if(SourceReader.isOfType(record)){
					sourceIds.add(targetId);
					repositoryIds.addAll(repositoriesOf(targetId));
				}
			}

			final String type = ResearchHelper.activityType(activity);
			final boolean visit = (!repositoryIds.isEmpty() && type != null && VISITS.contains(type.strip().toLowerCase(Locale.ROOT)));
			final List<String> parts = new ArrayList<>();
			if(visit)
				for(final String repositoryId : repositoryIds)
					parts.add(SourceHelper.repositoryName(model.getRecordById(repositoryId)));
			if(StringUtils.isNotBlank(ResearchHelper.activityResult(activity)))
				parts.add(ResearchHelper.activityResult(activity));
			add(creationDate(activity), (visit? Kind.REPOSITORY_VISITED: Kind.ACTIVITY), activity.getId(),
				StringUtils.defaultIfBlank(ResearchHelper.action(activity), activity.getId()), String.join(", ", parts), involved,
				sourceIds, repositoryIds);
		}
	}

	private void collectSources(){
		for(final FLEFRecord source : SourceHelper.listAllSources(model)){
			final String id = source.getId();
			add(creationDate(source), Kind.SOURCE_ADDED, id, SourceHelper.sourceTitle(source), StringUtils.EMPTY,
				involvedBySource.getOrDefault(id, Set.of()), Set.of(id), repositoriesOf(id));
		}
	}

	private void collectExtracts(){
		for(final FLEFRecord extract : PrivacyService.visible(model, model.getRecordsByType(ExtractReader.TYPE))){
			final String sourceId = ExtractReader.extractSource(extract);
			final String text = StringUtils.abbreviate(StringUtils.normalizeSpace(ExtractReader.extractText(extract)), 80);
			final boolean hasSource = (sourceId != null && model.getRecordById(sourceId) != null);
			add(creationDate(extract), Kind.EXTRACT_CREATED, extract.getId(), StringUtils.defaultIfBlank(text, extract.getId()),
				(hasSource? SourceHelper.sourceTitle(model.getRecordById(sourceId)): StringUtils.EMPTY),
				(hasSource? involvedBySource.getOrDefault(sourceId, Set.of()): Set.of()), (hasSource? Set.of(sourceId): Set.of()),
				(hasSource? repositoriesOf(sourceId): Set.of()));
		}
	}

	private void collectConclusions(){
		for(final FLEFRecord conclusion : PrivacyService.visible(model, model.getRecordsByType(ConclusionReader.TYPE))){
			final List<String> resolves = OpenTasks.references(conclusion, ConclusionReader.TAG_RESOLVES);
			final Set<String> involved = involvedOfQuestions(OpenTasks.references(conclusion, ConclusionReader.TAG_RESEARCH));
			for(final String id : resolves)
				involved.addAll(involvedOf(id));
			final String title = StringUtils.defaultIfBlank(ResearchHelper.conclusionIssue(conclusion), conclusion.getId());

			final String created = creationDate(conclusion);
			add(created, Kind.CONCLUSION_CREATED, conclusion.getId(), title, StringUtils.EMPTY, involved, Set.of(), Set.of());

			String settledOn = created;
			for(final Update update : updates(conclusion)){
				add(update.date(), Kind.CONCLUSION_UPDATED, conclusion.getId(), title, flat(update.comment()), involved, Set.of(), Set.of());
				//the position that settles the conflict is taken by the last update that touches the preferred assertion or the status
				if(update.date() != null && StringUtils.containsAny(update.comment(), ConclusionReader.TAG_PREFERRED,
						ConclusionReader.TAG_PROOF_STATUS))
					settledOn = update.date();
			}

			final FLEFRecord preferred = FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED);
			final Set<String> competing = new HashSet<>(resolves);
			final ConclusionResolution.Evaluation evaluation = new ConclusionResolution.Evaluation(competing,
				(preferred != null? OpenTasks.referenceOf(preferred): null), StringUtils.trimToNull(ResearchHelper.proofStatus(conclusion)));
			if(ConclusionResolution.settles(competing, evaluation))
				add(settledOn, Kind.CONFLICT_RESOLVED, conclusion.getId(), title, StringUtils.defaultString(evaluation.proofStatus()),
					involved, Set.of(), Set.of());
		}
	}

	/** A citation has no audit: it is dated by the update of the record that received it ({@code + source[0]: S1}). */
	private void collectCitations(){
		for(final String type : SourceHelper.allSourceCitingTypes())
			for(final FLEFRecord owner : PrivacyService.visible(model, model.getRecordsByType(type))){
				final Set<String> involved = involvedOf(owner.getId());
				for(final Update update : updates(owner))
					for(final String line : update.comment().lines().toList()){
						if(!line.startsWith(ADDED))
							continue;

						final String path = StringUtils.strip(StringUtils.substringBefore(line.substring(ADDED.length()), ":"));
						final String leaf = StringUtils.substringBefore(StringUtils.substringAfterLast(path, "."), "[");
						final String name = (leaf.isEmpty()? StringUtils.substringBefore(path, "["): leaf);
						if(!SourceReader.TYPE.equalsIgnoreCase(name))
							continue;

						final String value = StringUtils.strip(StringUtils.substringAfter(line, ":"));
						final FLEFRecord source = (!value.isEmpty()? model.getRecordById(value): null);
						final boolean known = (source != null && SourceReader.isOfType(source));
						add(update.date(), Kind.CITATION_ADDED, owner.getId(), (known? SourceHelper.sourceTitle(source): owner.getId()),
							StringUtils.EMPTY, involved, (known? Set.of(value): Set.of()), (known? repositoriesOf(value): Set.of()));
					}
			}
	}


	private void add(final String when, final Kind kind, final String recordId, final String title, final String detail,
			final Set<String> involved, final Set<String> sourceIds, final Set<String> repositoryIds){
		entries.add(new Entry(when, OpenTasks.parseDue(when), kind, recordId, title, StringUtils.defaultString(detail),
			Set.copyOf(involved), Set.copyOf(sourceIds), Set.copyOf(repositoryIds)));
	}

	/** @return	The records that the questions are about, and the individuals they lead to. */
	private Set<String> involvedOfQuestions(final List<String> questionIds){
		final Set<String> involved = new LinkedHashSet<>();
		for(final String questionId : questionIds){
			final FLEFRecord question = questionsById.get(questionId);
			if(question != null)
				for(final String targetId : OpenTasks.targetIds(question))
					involved.addAll(involvedOf(targetId));
		}
		return involved;
	}

	/** @return	The record and the individuals it is about (the owner of an attribute, the participants of an event, the ends of a relationship). */
	private Set<String> involvedOf(final String id){
		final Set<String> involved = new LinkedHashSet<>();
		final FLEFRecord record = (id != null? model.getRecordById(id): null);
		if(record == null)
			return involved;

		involved.add(id);
		if(IndividualAttributeReader.isOfType(record))
			addIfPresent(involved, FLEFRecordHelper.getChildValue(record, IndividualAttributeReader.TAG_INDIVIDUAL));
		else if(EventReader.isOfType(record))
			involved.addAll(participantsByEvent.getOrDefault(id, Set.of()));
		else if(EventParticipationReader.isOfType(record))
			addIfPresent(involved, EventParticipationReader.extractParticipantRef(record, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL));
		else if(RelationshipReader.isOfType(record)){
			addIfPresent(involved, FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_SUBJECT_INDIVIDUAL));
			addIfPresent(involved, FLEFRecordHelper.getChildValue(record, RelationshipReader.TAG_TARGET_INDIVIDUAL));
		}
		return involved;
	}

	private static void addIfPresent(final Set<String> set, final String id){
		if(StringUtils.isNotBlank(id))
			set.add(id.strip());
	}

	/** @return	The repositories holding a source (the visible ones that exist). */
	private Set<String> repositoriesOf(final String sourceId){
		final Set<String> ids = new LinkedHashSet<>();
		final FLEFRecord source = model.getRecordById(sourceId);
		if(source == null)
			return ids;

		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_REPOSITORY)){
			final String id = StringUtils.strip(FLEFRecordHelper.getChildValue(citation, RepositoryCitationReader.TAG_REPOSITORY));
			final FLEFRecord repository = (id != null? model.getRecordById(id): null);
			if(repository != null && PrivacyService.isVisible(model, repository))
				ids.add(id);
		}
		return ids;
	}


	/** One update of the audit of a record. */
	private record Update(String date, String comment){}

	/** @return	The date a record was created ({@code audit.creation.date}), or {@code null}. */
	private static String creationDate(final FLEFRecord record){
		final FLEFRecord audit = FLEFRecordHelper.findChild(record, AuditReader.TAG_AUDIT);
		final FLEFRecord creation = (audit != null? FLEFRecordHelper.findChild(audit, AuditReader.TAG_CREATION): null);
		return (creation != null? StringUtils.trimToNull(ResearchHelper.firstTextValue(creation, AuditReader.TAG_DATE)): null);
	}

	private static List<Update> updates(final FLEFRecord record){
		final FLEFRecord audit = FLEFRecordHelper.findChild(record, AuditReader.TAG_AUDIT);
		if(audit == null)
			return List.of();

		final List<Update> updates = new ArrayList<>();
		for(final FLEFRecord update : FLEFRecordHelper.findChildren(audit, AuditReader.TAG_UPDATE))
			updates.add(new Update(StringUtils.trimToNull(ResearchHelper.firstTextValue(update, AuditReader.TAG_DATE)),
				StringUtils.defaultString(ResearchHelper.firstTextValue(update, AuditReader.TAG_COMMENT))));
		return updates;
	}

	private static String flat(final String comment){
		return String.join("; ", comment.lines().toList());
	}

}
