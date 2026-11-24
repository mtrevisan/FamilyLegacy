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
package io.github.mtrevisan.familylegacy.assistant.task;

import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.draft.DraftEnrichment;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.TaskBoard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Turns the findings of the Research Assistant into research tasks: the ones that ask the researcher to do something (find a source,
 * settle a conflict, decide whether two records are the same one, find the parents, search an archive) are proposed as a task with
 * what to do, why, the records it is about, a priority and a due date.
 * <p>
 * The proposals are only that: the assistant creates a task only when the researcher accepts it ({@link #create}). A finding is
 * proposed once: the key of the finding is kept in the task ({@code origin}), and a finding that already became a task, whatever the
 * state of the task, is not proposed again.
 * </p>
 */
public final class TaskProposals{

	/** The records a research question, and then a task, can be about. */
	private static final Set<String> TARGET_TYPES = Set.of(IndividualReader.TYPE, GroupReader.TYPE, EventReader.TYPE,
		EventParticipationReader.TYPE, RelationshipReader.TYPE, IndividualAttributeReader.TYPE, GroupAttributeReader.TYPE,
		PlaceReader.TYPE, PlaceRelationshipReader.TYPE, RepositoryReader.TYPE, SourceReader.TYPE, MediaReader.TYPE,
		ConclusionReader.TYPE, IdentityHypothesisReader.TYPE, CulturalNormReader.TYPE, HistoricEventReader.TYPE);

	/** Among the findings that only inform, the ones at least this useful are of normal priority (the other ones are of low priority). */
	private static final double NORMAL_RANK = 66.;


	private TaskProposals(){}


	/**
	 * @param findings the findings of the assistant, in the order they are shown
	 * @param model    the model
	 * @param today    today
	 * @return the tasks the findings ask for, in the same order: only the findings that are to be done (see {@link TaskTopic#of}), that
	 * have a key and that did not become a task yet
	 */
	public static List<TaskProposal> propose(final List<Diagnostic> findings, final FLEFModel model, final LocalDate today){
		final Set<String> known = new HashSet<>(TaskBoard.origins(model));
		final List<TaskProposal> proposals = new ArrayList<>();
		for(final Diagnostic finding : findings){
			final String key = finding.key();
			final TaskTopic topic = TaskTopic.of(finding.kind());
			if(key == null || topic == null || !known.add(key))
				continue;

			final List<String> related = new ArrayList<>();
			final List<String> questions = new ArrayList<>();
			final List<String> activities = new ArrayList<>();
			for(final String id : finding.recordIds()){
				final FLEFRecord record = model.getRecordById(id);
				if(record == null)
					continue;

				if(ResearchQuestionReader.isOfType(record))
					questions.add(id);
				else if(ResearchActivityReader.isOfType(record))
					activities.add(id);
				else if(record.getTag() != null && TARGET_TYPES.contains(record.getTag().toUpperCase(Locale.ROOT)) && !related.contains(id))
					related.add(id);
			}

			final String priority = priority(finding);
			proposals.add(new TaskProposal(key, topic, topic.description(finding.title()), finding.explanation(), priority,
				DraftEnrichment.dueDate(priority, today), related, questions, (activities.size() == 1? activities.getFirst(): null)));
		}
		return proposals;
	}

	private static String priority(final Diagnostic finding){
		return switch(finding.severity()){
			case ERROR -> ResearchTaskReader.ENUM_PRIORITY_HIGH;
			case WARNING -> ResearchTaskReader.ENUM_PRIORITY_NORMAL;
			case INFORMATION -> (finding.rank() >= NORMAL_RANK? ResearchTaskReader.ENUM_PRIORITY_NORMAL: ResearchTaskReader.ENUM_PRIORITY_LOW);
		};
	}


	/**
	 * Creates the task of a proposal and adds it to the model: open, with its description, the questions it serves, the activity it
	 * follows, the priority, the due date, the records it is about, the rationale and the key of the finding it comes from.
	 *
	 * @param model    the model
	 * @param proposal what to create
	 * @param clock    the clock that dates the creation of the task
	 * @return the new task
	 */
	public static FLEFRecord create(final FLEFModel model, final TaskProposal proposal, final Clock clock){
		FLEFRecord task;
		do{
			task = FLEFRecord.createMainRecord(ResearchTaskReader.TYPE, ResearchTaskReader.ID_PREFIX, model);
		}while(model.hasRecord(task.getId()));

		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DESCRIPTION, proposal.description()));
		for(final String question : proposal.questionIds())
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_QUESTION, question));
		if(proposal.activityId() != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_CREATED_BY, proposal.activityId()));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_STATUS, ResearchTaskReader.ENUM_STATUS_OPEN));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_PRIORITY, proposal.priority()));
		if(proposal.dueDate() != null)
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_DUE_DATE, proposal.dueDate()));
		for(final String id : proposal.relatedIds()){
			final FLEFRecord target = model.getRecordById(id);
			if(target != null)
				task.addChild(FLEFRecord.createChildWithTag(ResearchTaskReader.TAG_RELATED)
					.addChild(FLEFRecord.createChildWithTagAndValue(target.getTag().toLowerCase(Locale.ROOT), id)));
		}
		if(proposal.rationale() != null && !proposal.rationale().isBlank())
			task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_RATIONALE, proposal.rationale()));
		task.addChild(FLEFRecord.createChildWithTagAndValue(ResearchTaskReader.TAG_ASSISTANT_FINDING_ID, proposal.origin()));
		task.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
			.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
				.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, OffsetDateTime.now(clock)
					.truncatedTo(ChronoUnit.SECONDS)
					.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))));

		model.addRecord(task);
		return task;
	}

}
