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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;


/**
 * Handler for RESEARCH TASK records.
 * <p>
 * Structure:
 * <pre>
 * // Represents a planned or actionable follow-up item resulting from one or more research activities or research questions.
 * record ResearchTaskRecord {
 *   description: Text                           // work that should be performed
 *   question*: Xref&lt;ResearchQuestionRecord&gt;     // research questions supported by this task
 *   created_by?: Xref&lt;ResearchActivityRecord&gt;   // activity that generated the task
 *   status: enum {
 *    open,          // not yet started
 *    in_progress,   // currently being worked on
 *    completed,     // successfully completed
 *    abandoned      // intentionally discontinued
 *   }
 *   priority?: enum { low, normal, high }
 *   due_date?: Date                             // optional target completion date
 *   outcome?: Text                              // notes describing the result once completed
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 */
public final class ResearchTaskReader{

	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_QUESTION = "question";
	public static final String TAG_CREATED_BY = "created_by";
	public static final String TAG_STATUS = "status";
	public static final String TAG_PRIORITY = "priority";
	public static final String TAG_DUE_DATE = "due_date";
	public static final String TAG_OUTCOME = "outcome";
	public static final String TAG_PRIVACY = "privacy";
	public static final String TAG_AUDIT = "audit";

	private static final String ENUM_STATUS_OPEN = "open";
	private static final String ENUM_STATUS_IN_PROGRESS = "in_progress";
	private static final String ENUM_STATUS_COMPLETED = "completed";
	private static final String ENUM_STATUS_ABANDONED = "abandoned";
	public static final String[] STATUSES = {
		ENUM_STATUS_OPEN,
		ENUM_STATUS_IN_PROGRESS,
		ENUM_STATUS_COMPLETED,
		ENUM_STATUS_ABANDONED
	};

	private static final String ENUM_PRIORITY_LOW = "low";
	private static final String ENUM_PRIORITY_NORMAL = "normal";
	private static final String ENUM_PRIORITY_HIGH = "high";
	public static final String[] PRIORITIES = {
		StringUtils.EMPTY,
		ENUM_PRIORITY_LOW,
		ENUM_PRIORITY_NORMAL,
		ENUM_PRIORITY_HIGH
	};


	private ResearchTaskReader(){}


	public static String extractDescription(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_DESCRIPTION);
	}

	public static String extractStatus(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_STATUS);
	}

	public static String extractPriority(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_PRIORITY);
	}

	public static String extractOutcome(final FLEFRecord record){
		return FLEFRecordHelper.getChildValue(record, TAG_OUTCOME);
	}

}
