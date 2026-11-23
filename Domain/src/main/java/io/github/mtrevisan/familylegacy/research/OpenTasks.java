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
import io.github.mtrevisan.familylegacy.io.model.RecordChangeSummary;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The research tasks still to be done in a file (what the tasks panel of the main window lists): those whose status is
 * {@code open} or {@code in_progress} (or that have none), ordered by due date and then by priority.
 * <p>
 * A task has no target of its own: the records it is about are the targets of its research questions.
 * </p>
 */
public final class OpenTasks{

	/** Length of an ISO date ({@code YYYY-MM-DD}), the form the due date is written in. */
	private static final int ISO_DATE_LENGTH = 10;

	/** The order of the list: the earliest due date first (the tasks with none last), then the highest priority, then the description. */
	public static final Comparator<Task> ORDER = Comparator.comparing(Task::due, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparingInt(task -> priorityRank(task.priority()))
		.thenComparing(task -> StringUtils.defaultString(task.description()), String.CASE_INSENSITIVE_ORDER)
		.thenComparing(task -> StringUtils.defaultString(task.record().getId()));


	/**
	 * A task to be done.
	 *
	 * @param record	The research task.
	 * @param description	What is to be done, or {@code null}.
	 * @param status	The status ({@code open}, {@code in_progress}), or {@code null} if the task has none.
	 * @param priority	The priority ({@code low}, {@code normal}, {@code high}), or {@code null} if the task has none.
	 * @param due	The day it is due, or {@code null} if it has none (or it is not an ISO date).
	 * @param questions	The research questions the task serves (the visible ones that exist).
	 * @param targetIds	The records the questions are about.
	 */
	public record Task(FLEFRecord record, String description, String status, String priority, LocalDate due, List<FLEFRecord> questions,
			Set<String> targetIds){

		/** @return	Whether the due date is before {@code today}. */
		public boolean isOverdue(final LocalDate today){
			return (due != null && due.isBefore(today));
		}

		/** @return	Whether one of the questions of the task is about the record. */
		public boolean concerns(final String recordId){
			return (recordId != null && targetIds.contains(recordId));
		}

	}


	private OpenTasks(){}


	/**
	 * @param model	The model.
	 * @return	The visible tasks still to be done, in {@link #ORDER}.
	 */
	public static List<Task> list(final FLEFModel model){
		final Map<String, FLEFRecord> questionsById = new HashMap<>();
		for(final FLEFRecord question : ResearchHelper.listQuestions(model))
			questionsById.put(question.getId(), question);

		final List<Task> tasks = new ArrayList<>();
		for(final FLEFRecord record : ResearchHelper.listTasks(model)){
			final String status = ResearchHelper.status(record);
			if(!isOpen(status))
				continue;

			final List<FLEFRecord> questions = new ArrayList<>();
			final Set<String> targetIds = new LinkedHashSet<>();
			for(final String questionId : references(record, ResearchTaskReader.TAG_QUESTION)){
				final FLEFRecord question = questionsById.get(questionId);
				if(question != null && !questions.contains(question)){
					questions.add(question);
					targetIds.addAll(targetIds(question));
				}
			}

			tasks.add(new Task(record, ResearchHelper.taskDescription(record), StringUtils.trimToNull(status),
				StringUtils.trimToNull(ResearchHelper.taskPriority(record)),
				parseDue(ResearchHelper.firstTextValue(record, ResearchTaskReader.TAG_DUE_DATE)),
				Collections.unmodifiableList(questions), Collections.unmodifiableSet(targetIds)));
		}
		tasks.sort(ORDER);
		return tasks;
	}

	/**
	 * The research questions a new task about a record can serve: the visible ones about the record (it is one of their targets) that
	 * are still under way ({@code open}, {@code on_hold}, or no status).
	 *
	 * @param model	The model.
	 * @param recordId	The record (a person, a group...), or {@code null}.
	 * @return	The questions, by title; empty if there is no record.
	 */
	public static List<FLEFRecord> questionsAbout(final FLEFModel model, final String recordId){
		if(model == null || StringUtils.isBlank(recordId))
			return List.of();

		final List<FLEFRecord> questions = new ArrayList<>();
		for(final FLEFRecord question : ResearchHelper.listQuestions(model))
			if(isUnderWay(ResearchHelper.status(question)) && targetIds(question).contains(recordId))
				questions.add(question);
		questions.sort(Comparator.comparing(ResearchHelper::questionTitle, String.CASE_INSENSITIVE_ORDER));
		return questions;
	}

	/** @return	Whether a question with this status is still under way: {@code open}, {@code on_hold}, or no status at all. */
	private static boolean isUnderWay(final String status){
		return (StringUtils.isBlank(status)
			|| ResearchQuestionReader.ENUM_STATUS_OPEN.equalsIgnoreCase(status.strip())
			|| ResearchQuestionReader.ENUM_STATUS_ON_HOLD.equalsIgnoreCase(status.strip()));
	}

	/**
	 * @param status	The status of a task.
	 * @return	Whether a task with this status is still to be done: {@code open}, {@code in_progress}, or no status at all.
	 */
	public static boolean isOpen(final String status){
		return (StringUtils.isBlank(status)
			|| ResearchTaskReader.ENUM_STATUS_OPEN.equalsIgnoreCase(status.strip())
			|| ResearchTaskReader.ENUM_STATUS_IN_PROGRESS.equalsIgnoreCase(status.strip()));
	}

	/**
	 * @param text	A due date, as written in the task.
	 * @return	The day, or {@code null} if there is none or it is not an ISO date ({@code YYYY-MM-DD}, possibly followed by a time).
	 */
	public static LocalDate parseDue(final String text){
		final String stripped = StringUtils.strip(text);
		if(StringUtils.isEmpty(stripped))
			return null;

		try{
			return LocalDate.parse(StringUtils.left(stripped, ISO_DATE_LENGTH));
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}

	/**
	 * Changes the status of a task in place, and records the change in its audit (as the record editor does on save).
	 *
	 * @param task	The research task.
	 * @param status	The new status (one of {@link ResearchTaskReader#STATUSES}).
	 * @return	Whether the status changed.
	 */
	public static boolean changeStatus(final FLEFRecord task, final String status){
		if(!ResearchTaskReader.isOfType(task) || StringUtils.isBlank(status)
				|| status.equalsIgnoreCase(StringUtils.strip(ResearchHelper.status(task))))
			return false;

		final FLEFRecord before = FLEFRecord.createEmpty();
		task.deepCopyTo(before);

		//the status is written once: a repeated one (not allowed by the protocol) is replaced by the new one
		FLEFRecordHelper.removeChildren(task, ResearchTaskReader.TAG_STATUS);
		FLEFRecordHelper.updateChildValue(task, ResearchTaskReader.TAG_STATUS, status);

		RecordChangeSummary.recordUpdate(before, task);
		return true;
	}


	/** @return	The rank of a priority, the most urgent first (no priority counts as normal). */
	public static int priorityRank(final String priority){
		if(ResearchTaskReader.ENUM_PRIORITY_HIGH.equalsIgnoreCase(priority))
			return 0;
		if(ResearchTaskReader.ENUM_PRIORITY_LOW.equalsIgnoreCase(priority))
			return 2;
		return 1;
	}

	/** @return	The ids a record references through a (possibly repeated) tag. */
	private static List<String> references(final FLEFRecord record, final String tag){
		final List<String> ids = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag)){
			final String id = referenceOf(child);
			if(id != null)
				ids.add(id);
		}
		return ids;
	}

	/** @return	The records a question is about: each target is a "oneof" that holds one reference. */
	private static Set<String> targetIds(final FLEFRecord question){
		final Set<String> ids = new LinkedHashSet<>();
		for(final FLEFRecord target : FLEFRecordHelper.findChildren(question, ResearchQuestionReader.TAG_TARGET)){
			final String id = referenceOf(target);
			if(id != null)
				ids.add(id);
		}
		return ids;
	}

	/** @return	The value of a field, or the one of its only arm (a reference written as {@code target.individual I1}). */
	private static String referenceOf(final FLEFRecord field){
		final String direct = StringUtils.trimToNull(field.getValue());
		if(direct != null)
			return direct;

		for(final FLEFRecord arm : field.getChildren()){
			final String value = StringUtils.trimToNull(arm.getValue());
			if(value != null)
				return value;
		}
		return null;
	}

}
