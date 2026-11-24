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

import io.github.mtrevisan.familylegacy.i18n.I18N;


/**
 * What a finding of the Research Assistant asks the researcher to do, when it can become a research task: the kind of work, that
 * gives the task its words.
 */
public enum TaskTopic{

	/** A fact with no source behind it. */
	MISSING_SOURCE,
	/** Evidence that contradicts itself. */
	CONFLICT,
	/** Two records that may be the same one. */
	DUPLICATE,
	/** Parents, or the birth that would name them, not known. */
	PARENTAGE,
	/** Something that an archive or a repository may settle. */
	ARCHIVE_SEARCH,
	/** Dates that cannot all be true. */
	CHRONOLOGY;


	/**
	 * @param kind the kind of a finding ({@code Diagnostic#kind()}, e.g. {@code gap.no_birth}, {@code conclusion.conflicting_events})
	 * @return what the finding asks, or {@code null} if it is not something to put in the list of the work (a hint on the style of a
	 * record, a curiosity of the context, a task that is already overdue...)
	 */
	public static TaskTopic of(final String kind){
		if(kind == null)
			return null;

		if(kind.equals("gap.unsourced_vital_event") || kind.equals("place.no_source"))
			return MISSING_SOURCE;
		if(kind.startsWith("gap.no_birth") || kind.equals("planner.brick_wall"))
			return PARENTAGE;
		//a task that is overdue is already a task
		if(kind.equals("gap.overdue_task"))
			return null;
		//a relationship that the records suggest but nothing documents: the archives may have the evidence
		if(kind.startsWith("gap.") || kind.equals("planner.next_step") || kind.equals("relationship.marriage_without_relationship")
				|| kind.equals("relationship.partner_of_parent_not_linked") || kind.equals("relationship.birth_informant_not_linked"))
			return ARCHIVE_SEARCH;
		if(kind.startsWith("conclusion."))
			return CONFLICT;
		if(kind.startsWith("identity.possible_duplicate") || kind.equals("place.possible_duplicate")
				|| kind.equals("relationship.duplicate_relationship"))
			return DUPLICATE;
		if(kind.startsWith("chronology.") && !kind.equals("chronology.date_not_checked"))
			return CHRONOLOGY;
		return null;
	}

	/** @return the name of the topic */
	public String label(){
		return switch(this){
			case MISSING_SOURCE -> I18N.t("task.topic.missing_source");
			case CONFLICT -> I18N.t("task.topic.conflict");
			case DUPLICATE -> I18N.t("task.topic.duplicate");
			case PARENTAGE -> I18N.t("task.topic.parentage");
			case ARCHIVE_SEARCH -> I18N.t("task.topic.archive_search");
			case CHRONOLOGY -> I18N.t("task.topic.chronology");
		};
	}

	/**
	 * @param title the title of the finding
	 * @return what to do about it, as the description of a task
	 */
	public String description(final String title){
		return switch(this){
			case MISSING_SOURCE -> I18N.tf("task.generate.missing_source", title);
			case CONFLICT -> I18N.tf("task.generate.conflict", title);
			case DUPLICATE -> I18N.tf("task.generate.duplicate", title);
			case PARENTAGE -> I18N.tf("task.generate.parentage", title);
			case ARCHIVE_SEARCH -> I18N.tf("task.generate.archive_search", title);
			case CHRONOLOGY -> I18N.tf("task.generate.chronology", title);
		};
	}

}
