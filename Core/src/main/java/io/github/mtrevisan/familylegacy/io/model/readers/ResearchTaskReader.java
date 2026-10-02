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
