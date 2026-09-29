package io.github.mtrevisan.familylegacy.v2.io.model.readers;


/**
 * Canonical tag names of the FLEF protocol.
 *
 * <p>Every field name that appears in a FLEF file is defined exactly once
 * here. Code that navigates the record tree must reference these constants
 * rather than string literals, so that a protocol change propagates from a
 * single point.</p>
 *
 * <p>The names are grouped by protocol area (audit, individual, event, ...).
 * A group with more than ~30 constants should be split into its own class
 * — see the note at the bottom of the file.</p>
 */
public final class FlefTags{

	private FlefTags(){}


	/* ----- Audit -------------------------------------------------------- */

	public static final String AUDIT = "audit";
	public static final String CREATION = "creation";
	public static final String COMMENT = "comment";
	public static final String UPDATE = "update";

	/* ----- Common scalar fields ----------------------------------------- */

	public static final String DATE = "date";
	public static final String VALUE = "value";
	public static final String TYPE = "type";
	public static final String ORIGINAL_TEXT = "original_text";
	public static final String PLACE = "place";
	public static final String SOURCE = "source";
	public static final String NOTE = "note";
	public static final String PRIVACY = "privacy";
	public static final String EVIDENCE = "evidence";
	public static final String LOCATOR = "locator";

	/* ----- Individual --------------------------------------------------- */

	public static final String INDIVIDUAL = "individual";
	public static final String NAME = "name";
	public static final String PART = "part";
	public static final String SEX = "sex";

	/* ----- Event -------------------------------------------------------- */

	public static final String EVENT = "event";
	public static final String EVENT_PARTICIPATION = "event_participation";
	public static final String PARTICIPANT = "participant";
	public static final String ROLE = "role";
	public static final String AGENCY = "agency";
	public static final String CAUSE = "cause";
	public static final String REASON = "reason";

	/* ----- Relationship ------------------------------------------------- */

	public static final String RELATIONSHIP = "relationship";
	public static final String SUBJECT = "subject";
	public static final String TARGET = "target";
	public static final String STATUS = "status";

	/* ----- Source / document / repository ------------------------------- */

	public static final String SOURCE_RECORD = "source";  // alias semantico
	public static final String DOCUMENT = "document";
	public static final String DOCUMENT_PART = "document_part";
	public static final String REPOSITORY = "repository";
	public static final String EXTRACT = "extract";
	public static final String URI = "uri";
	public static final String TITLE = "title";

	/* ----- Research ----------------------------------------------------- */

	public static final String RESEARCH_QUESTION = "research_question";
	public static final String RESEARCH_ACTIVITY = "research_activity";
	public static final String RESEARCH_TASK = "research_task";
	public static final String CONCLUSION = "conclusion";
	public static final String IDENTITY_HYPOTHESIS = "identity_hypothesis";
	public static final String CULTURAL_NORM = "cultural_norm";
	public static final String HISTORIC_EVENT = "historic_event";

	// ... e così via per tutte le aree del protocollo.

}
