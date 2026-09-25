package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import com.ibm.icu.text.MessageFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


/**
 * Provides localized labels and text fragments used by {@link ReportGenerator},
 * {@link LifeNarrator}, {@link GenealogicalDateHelper} and every
 * {@link SectionBuilder}.
 *
 * <p>Translations are loaded from UTF-8 {@code .properties} files located in
 * {@code src/main/resources/i18n/}. Adding a new language requires:</p>
 * <ol>
 *   <li>creating {@code i18n/report_messages_xx.properties} (UTF-8);</li>
 *   <li>adding a corresponding {@link ReportLanguage} constant.</li>
 * </ol>
 *
 * <p>Java's default {@code ResourceBundle} reads {@code .properties} files as
 * ISO-8859-1. The {@link Utf8Control} inner class overrides the loading to use
 * UTF-8, so accented characters and non-Latin scripts work without
 * {@code \\uXXXX} escapes.</p>
 *
 * <p><b>Plural-sensitive messages</b> use ICU4J {@link MessageFormat} with the
 * CLDR plural syntax, so languages with more than two forms (Slavic languages
 * have three, Arabic has six, Japanese has one) are handled correctly. Any
 * message whose wording depends on a count must use the plural pattern, not
 * {@link String#format}.</p>
 */
public final class ReportLabels{

	private static final String BUNDLE_BASE = "i18n.report_messages";

	private final ReportLanguage language;
	private final ResourceBundle bundle;


	public ReportLabels(final ReportLanguage language){
		this.language = Objects.requireNonNull(language, "language");
		this.bundle = ResourceBundle.getBundle(BUNDLE_BASE, language.locale(), new Utf8Control());
	}


	public ReportLanguage language(){
		return language;
	}


	/* ======================================================================
	 *                          Section headings
	 * ====================================================================== */

	public String subtitle(){
		return t(Key.SUBTITLE);
	}

	public String introduction(){
		return t(Key.INTRODUCTION);
	}

	public String statistics(){
		return t(Key.STATISTICS);
	}

	public String paternalAncestry(){
		return t(Key.PATERNAL_ANCESTRY);
	}

	public String maternalAncestry(){
		return t(Key.MATERNAL_ANCESTRY);
	}

	public String descendants(){
		return t(Key.DESCENDANTS);
	}

	public String directRelations(){
		return t(Key.DIRECT_RELATIONS);
	}

	public String indirectRelations(){
		return t(Key.INDIRECT_RELATIONS);
	}

	public String notes(){
		return t(Key.NOTES);
	}

	public String sources(){
		return t(Key.SOURCES);
	}

	public String media(){
		return t(Key.MEDIA);
	}

	public String places(){
		return t(Key.PLACES);
	}

	public String documents(){
		return t(Key.DOCUMENTS);
	}

	public String indexOfIndividuals(){
		return t(Key.INDEX_OF_INDIVIDUALS);
	}

	public String indexOfPlaces(){
		return t(Key.INDEX_OF_PLACES);
	}

	/* ======================================================================
	 *                          Life story sections
	 * ====================================================================== */

	public String lifeOf(){
		return t(Key.LIFE_OF);
	}

	public String personalData(){
		return t(Key.PERSONAL_DATA);
	}

	public String lifeEvents(){
		return t(Key.LIFE_EVENTS);
	}

	public String attributes(){
		return t(Key.ATTRIBUTES);
	}

	public String relationships(){
		return t(Key.RELATIONSHIPS);
	}

	/* ======================================================================
	 *                          Context and research sections
	 * ====================================================================== */

	public String contextSection(){
		return t(Key.CONTEXT_SECTION);
	}

	public String researchSection(){
		return t(Key.RESEARCH_SECTION);
	}

	public String identityHypotheses(){
		return t(Key.IDENTITY_HYPOTHESES);
	}

	public String conclusions(){
		return t(Key.CONCLUSIONS);
	}

	/* ======================================================================
	 *                          Narrative fragments
	 * ====================================================================== */

	public String introductionBody(){
		return t(Key.INTRODUCTION_BODY);
	}

	public String narrativeNote(){
		return t(Key.NARRATIVE_NOTE);
	}

	/* ======================================================================
	 *                          Inline labels
	 * ====================================================================== */

	public String childOf(){
		return t(Key.CHILD_OF);
	}

	public String sex(){
		return t(Key.SEX);
	}

	public String sexUnknown(){
		return t(Key.SEX_UNKNOWN);
	}

	public String citations(){
		return t(Key.CITATIONS);
	}

	public String question(){
		return t(Key.QUESTION);
	}

	public String status(){
		return t(Key.STATUS);
	}

	public String conclusion(){
		return t(Key.CONCLUSION);
	}

	public String type(){
		return t(Key.TYPE);
	}

	public String date(){
		return t(Key.DATE);
	}

	public String place(){
		return t(Key.PLACE);
	}

	public String cause(){
		return t(Key.CAUSE);
	}

	public String role(){
		return t(Key.ROLE);
	}

	public String person(){
		return t(Key.PERSON);
	}

	public String value(){
		return t(Key.VALUE);
	}

	public String from(){
		return t(Key.FROM);
	}

	public String to(){
		return t(Key.TO);
	}

	public String id(){
		return t(Key.ID);
	}

	public String name(){
		return t(Key.NAME);
	}

	public String subject(){
		return t(Key.SUBJECT);
	}

	public String roleParent(){
		return t(Key.ROLE_PARENT);
	}

	public String roleSpouse(){
		return t(Key.ROLE_SPOUSE);
	}

	public String roleChild(){
		return t(Key.ROLE_CHILD);
	}

	public String year(){
		return t(Key.YEAR);
	}

	public String title(){
		return t(Key.TITLE);
	}

	public String timeline(){
		return t(Key.TIMELINE);
	}

	public String extractNote(){
		return t(Key.EXTRACT_NOTE);
	}

	public String extractCrop(){
		return t(Key.EXTRACT_CROP);
	}

	public String sourceRepositoryNote(){
		return t(Key.SOURCE_REPOSITORY_NOTE);
	}

	public String nameVariant(){
		return t(Key.NAME_VARIANT);
	}

	public String culturalNormOf(){
		return t(Key.CULTURAL_NORM_OF);
	}

	public String culturalNormInvocations(){
		return t(Key.CULTURAL_NORM_INVOCATIONS);
	}

	public String historicEventOf(){
		return t(Key.HISTORIC_EVENT_OF);
	}

	public String historicEventImpacts(){
		return t(Key.HISTORIC_EVENT_IMPACTS);
	}

	public String identityHypothesis(){
		return t(Key.IDENTITY_HYPOTHESIS);
	}

	public String researchActivityOf(){
		return t(Key.RESEARCH_ACTIVITY_OF);
	}

	public String researchTaskOf(){
		return t(Key.RESEARCH_TASK_OF);
	}

	/* ======================================================================
	 *                          Indirect-relations table
	 * ====================================================================== */

	public String relation(){
		return t(Key.RELATION);
	}

	public String of(){
		return t(Key.OF);
	}

	public String relationSpouseOf(){
		return t(Key.RELATION_SPOUSE_OF);
	}

	public String relationAssociateOf(){
		return t(Key.RELATION_ASSOCIATE_OF);
	}

	/* ======================================================================
	 *                          Event / attribute / relationship labels
	 * ====================================================================== */

	public String agency(){
		return t(Key.AGENCY);
	}

	public String note(){
		return t(Key.NOTE);
	}

	public String description(){
		return t(Key.DESCRIPTION);
	}

	public String validFrom(){
		return t(Key.VALID_FROM);
	}

	public String validTo(){
		return t(Key.VALID_TO);
	}

	public String relationshipStatus(){
		return t(Key.RELATIONSHIP_STATUS);
	}

	/* ======================================================================
	 *                          Source / repository / document labels
	 * ====================================================================== */

	public String source(){
		return t(Key.SOURCE);
	}

	public String sourceAuthor(){
		return t(Key.SOURCE_AUTHOR);
	}

	public String sourcePublisher(){
		return t(Key.SOURCE_PUBLISHER);
	}

	public String sourceDate(){
		return t(Key.SOURCE_DATE);
	}

	public String sourcePlace(){
		return t(Key.SOURCE_PLACE);
	}

	public String sourceMediaType(){
		return t(Key.SOURCE_MEDIA_TYPE);
	}

	public String sourceRepository(){
		return t(Key.SOURCE_REPOSITORY);
	}

	public String sourceLocator(){
		return t(Key.SOURCE_LOCATOR);
	}

	public String sourceDocuments(){
		return t(Key.SOURCE_DOCUMENTS);
	}

	public String extractText(){
		return t(Key.EXTRACT_TEXT);
	}

	public String documentUri(){
		return t(Key.DOCUMENT_URI);
	}

	public String documentDescription(){
		return t(Key.DOCUMENT_DESCRIPTION);
	}

	public String documentMapping(){
		return t(Key.DOCUMENT_MAPPING);
	}

	public String preferredImage(){
		return t(Key.PREFERRED_IMAGE);
	}

	/* ======================================================================
	 *                          Place labels
	 * ====================================================================== */

	public String placeType(){
		return t(Key.PLACE_TYPE);
	}

	public String placeCoordinates(){
		return t(Key.PLACE_COORDINATES);
	}

	public String placeOriginalText(){
		return t(Key.PLACE_ORIGINAL_TEXT);
	}

	public String placeJurisdictions(){
		return t(Key.PLACE_JURISDICTIONS);
	}

	public String placeNameVariant(){
		return t(Key.PLACE_NAME_VARIANT);
	}

	public String placeCitations(){
		return t(Key.PLACE_CITATIONS);
	}

	public String placeCitedBy(){
		return t(Key.PLACE_CITED_BY);
	}

	/* ======================================================================
	 *                          Evidence labels
	 * ====================================================================== */

	public String evidence(){
		return t(Key.EVIDENCE);
	}

	public String evidenceSourceType(){
		return t(Key.EVIDENCE_SOURCE_TYPE);
	}

	public String evidenceInformationType(){
		return t(Key.EVIDENCE_INFORMATION_TYPE);
	}

	public String evidenceType(){
		return t(Key.EVIDENCE_TYPE);
	}

	/* ======================================================================
	 *                          Research / conclusion labels
	 * ====================================================================== */

	public String researchIssue(){
		return t(Key.RESEARCH_ISSUE);
	}

	public String researchProofStatus(){
		return t(Key.RESEARCH_PROOF_STATUS);
	}

	public String researchNarrative(){
		return t(Key.RESEARCH_NARRATIVE);
	}

	public String researchConclusionConfidence(){
		return t(Key.RESEARCH_CONCLUSION_CONFIDENCE);
	}

	public String researchClosedDate(){
		return t(Key.RESEARCH_CLOSED_DATE);
	}

	public String researchActivities(){
		return t(Key.RESEARCH_ACTIVITIES);
	}

	public String researchTasks(){
		return t(Key.RESEARCH_TASKS);
	}

	public String researchResult(){
		return t(Key.RESEARCH_RESULT);
	}

	public String researchSearchScope(){
		return t(Key.RESEARCH_SEARCH_SCOPE);
	}

	public String researchTarget(){
		return t(Key.RESEARCH_TARGET);
	}

	public String researchObservation(){
		return t(Key.RESEARCH_OBSERVATION);
	}

	public String researchParentActivity(){
		return t(Key.RESEARCH_PARENT_ACTIVITY);
	}

	public String researchTaskPriority(){
		return t(Key.RESEARCH_TASK_PRIORITY);
	}

	public String researchTaskDueDate(){
		return t(Key.RESEARCH_TASK_DUE_DATE);
	}

	public String researchTaskCreatedBy(){
		return t(Key.RESEARCH_TASK_CREATED_BY);
	}

	public String researchTaskOutcome(){
		return t(Key.RESEARCH_TASK_OUTCOME);
	}

	public String researchResolves(){
		return t(Key.RESEARCH_RESOLVES);
	}

	public String researchPreferred(){
		return t(Key.RESEARCH_PREFERRED);
	}

	public String researchLinkedQuestions(){
		return t(Key.RESEARCH_LINKED_QUESTIONS);
	}

	public String contextImpact(){
		return t(Key.CONTEXT_IMPACT);
	}

	public String contextRationale(){
		return t(Key.CONTEXT_RATIONALE);
	}

	public String identityCandidates(){
		return t(Key.IDENTITY_CANDIDATES);
	}

	public String identityComment(){
		return t(Key.IDENTITY_COMMENT);
	}

	/* ======================================================================
	 *                          Empty states
	 * ====================================================================== */

	public String noAncestors(){
		return t(Key.NO_ANCESTORS);
	}

	public String noIndirectRelations(){
		return t(Key.NO_INDIRECT_RELATIONS);
	}

	public String empty(){
		return t(Key.EMPTY);
	}

	/* ======================================================================
	 *                          Statistic labels (non-plural)
	 * ====================================================================== */

	public String lifespan(){
		return t(Key.LIFESPAN);
	}

	public String eventsCount(){
		return t(Key.EVENTS_COUNT);
	}

	public String sourcesCount(){
		return t(Key.SOURCES_COUNT);
	}

	/* ======================================================================
	 *                          Cultural norms
	 * ====================================================================== */

	public String culturalNorms(){
		return t(Key.CULTURAL_NORMS);
	}

	public String culturalNormRuleType(){
		return t(Key.CULTURAL_NORM_RULE_TYPE);
	}

	public String culturalNormValidFrom(){
		return t(Key.CULTURAL_NORM_VALID_FROM);
	}

	public String culturalNormValidTo(){
		return t(Key.CULTURAL_NORM_VALID_TO);
	}

	public String culturalNormPlace(){
		return t(Key.CULTURAL_NORM_PLACE);
	}

	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	public String groups(){
		return t(Key.GROUPS);
	}

	public String groupType(){
		return t(Key.GROUP_TYPE);
	}

	public String groupMemberships(){
		return t(Key.GROUP_MEMBERSHIPS);
	}

	public String groupAttributes(){
		return t(Key.GROUP_ATTRIBUTES);
	}

	public String groupParentGroups(){
		return t(Key.GROUP_PARENT_GROUPS);
	}

	public String groupChildGroups(){
		return t(Key.GROUP_CHILD_GROUPS);
	}

	public String groupEvents(){
		return t(Key.GROUP_EVENTS);
	}

	/* ======================================================================
	 *                          Header
	 * ====================================================================== */

	public String header(){
		return t(Key.HEADER);
	}

	public String headerProtocol(){
		return t(Key.HEADER_PROTOCOL);
	}

	public String headerProtocolVersion(){
		return t(Key.HEADER_PROTOCOL_VERSION);
	}

	public String headerSource(){
		return t(Key.HEADER_SOURCE);
	}

	public String headerSourceVersion(){
		return t(Key.HEADER_SOURCE_VERSION);
	}

	public String headerOrganization(){
		return t(Key.HEADER_ORGANIZATION);
	}

	public String headerDate(){
		return t(Key.HEADER_DATE);
	}

	public String headerCopyright(){
		return t(Key.HEADER_COPYRIGHT);
	}

	public String headerSubmitter(){
		return t(Key.HEADER_SUBMITTER);
	}

	public String headerSubmitterNote(){
		return t(Key.HEADER_SUBMITTER_NOTE);
	}

	public String headerScope(){
		return t(Key.HEADER_SCOPE);
	}

	/* ======================================================================
	 *                          Notes
	 * ====================================================================== */

	public String noteTitle(){
		return t(Key.NOTE_TITLE);
	}

	public String noteTranslation(){
		return t(Key.NOTE_TRANSLATION);
	}

	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	public String imageCroppedRegion(){
		return t(Key.IMAGE_CROPPED_REGION);
	}

	/* ======================================================================
	 *                          Audit
	 * ====================================================================== */

	public String audit(){
		return t(Key.AUDIT);
	}

	public String auditCreated(){
		return t(Key.AUDIT_CREATED);
	}

	public String auditUpdated(){
		return t(Key.AUDIT_UPDATED);
	}

	/* ======================================================================
	 *                          Date display
	 * ====================================================================== */

	public String dateDecade(){
		return t(Key.DATE_DECADE);
	}

	public String dateCentury(){
		return t(Key.DATE_CENTURY);
	}

	public String dateCenturyWithPart(){
		return t(Key.DATE_CENTURY_WITH_PART);
	}

	public String dateBetween(){
		return t(Key.DATE_BETWEEN);
	}

	public String dateAfter(){
		return t(Key.DATE_AFTER);
	}

	public String dateBefore(){
		return t(Key.DATE_BEFORE);
	}

	public String dateFromTo(){
		return t(Key.DATE_FROM_TO);
	}

	public String dateFrom(){
		return t(Key.DATE_FROM);
	}

	public String dateTo(){
		return t(Key.DATE_TO);
	}

	public String dateMargin(){
		return t(Key.DATE_MARGIN);
	}

	public String approxBasisStated(){
		return t(Key.APPROX_BASIS_STATED);
	}

	public String approxBasisCalculated(){
		return t(Key.APPROX_BASIS_CALCULATED);
	}

	public String approxBasisConventional(){
		return t(Key.APPROX_BASIS_CONVENTIONAL);
	}

	public String approxBasisUnspecified(){
		return t(Key.APPROX_BASIS_UNSPECIFIED);
	}

	public String approxBasisConventionalPer(final String titles){
		return String.format(t(Key.APPROX_BASIS_CONVENTIONAL_PER), titles);
	}

	/**
	 * Localized display for the {@code CenturyPart} enum. Unknown values are
	 * prettified by replacing underscores with spaces.
	 */
	public String centuryPart(final String part){
		if(part == null || part.isBlank())
			return "";
		final String key = "CENTURY_PART_" + part.toUpperCase(Locale.ROOT);
		try{
			return t(Key.valueOf(key));
		}
		catch(final IllegalArgumentException ignored){
			return part.replace('_', ' ');
		}
	}

	/**
	 * Localized display for a calendar code. Unknown calendars fall back to
	 * the raw code.
	 */
	public String calendarDisplay(final String code){
		if(code == null || code.isBlank())
			return "";
		final String key = "CALENDAR_" + code.toUpperCase(Locale.ROOT);
		try{
			return t(Key.valueOf(key));
		}
		catch(final IllegalArgumentException ignored){
			return code;
		}
	}

	/* ======================================================================
	 *                          Name cultural norm
	 * ====================================================================== */

	public String nameCulturalNorm(){
		return t(Key.NAME_CULTURAL_NORM);
	}

	/* ======================================================================
	 *                          Plural-aware numeric labels
	 * ====================================================================== */

	/**
	 * Localized label for a group of children by relationship type and count.
	 * The plural form is chosen by ICU4J using the CLDR plural rules of the
	 * current language, so Slavic (3 forms) and Arabic (6 forms) are handled
	 * correctly.
	 *
	 * @param relationshipType one of {@code biological_child},
	 *                         {@code adoptive_child}, {@code foster_child},
	 *                         {@code guarded_child}, {@code step_child};
	 *                         unknown types fall back to {@code REL_CHILD}
	 * @param count            the number of children in the group
	 */
	public String childGroupLabel(final String relationshipType, final int count){
		final String base = childTypeKeyBase(relationshipType);
		return pluralKey(Key.valueOf(base), count);
	}

	/** Plural-aware age label, e.g. "1 year", "2 years", "5 anni". */
	public String ageYears(final int years){
		return pluralKey(Key.AGE_YEARS, years);
	}

	public String marginYears(final int n){
		return pluralKey(Key.MARGIN_YEARS, n);
	}

	public String marginMonths(final int n){
		return pluralKey(Key.MARGIN_MONTHS, n);
	}

	public String marginWeeks(final int n){
		return pluralKey(Key.MARGIN_WEEKS, n);
	}

	public String marginDays(final int n){
		return pluralKey(Key.MARGIN_DAYS, n);
	}

	/**
	 * Plural-aware relation-count line, with three nested plurals (parents,
	 * spouses, children).
	 */
	public String relationsCount(final int parents, final int spouses, final int children){
		return new MessageFormat(t(Key.RELATIONS_COUNT), language.locale())
			.format(new Object[]{parents, spouses, children});
	}

	/* ======================================================================
	 *                          Narrative methods (plural-aware)
	 * ====================================================================== */

	public String narrativeBirth(final String name, final String date, final String place){
		return String.format(t(Key.NARRATIVE_BIRTH_BASE), name, dateFrag(date), placeFrag(place));
	}

	public String narrativeBirthUnknown(final String name){
		return String.format(t(Key.NARRATIVE_BIRTH_UNKNOWN), name);
	}

	public String narrativeMarriage(final String name, final String spouse,
		final String date, final String place){
		if(spouse != null)
			return String.format(t(Key.NARRATIVE_MARRIAGE_BASE),
				name, spouse, dateFrag(date), placeFrag(place));
		return String.format(t(Key.NARRATIVE_MARRIAGE_NOSPOUSE_BASE),
			name, dateFrag(date), placeFrag(place));
	}

	public String narrativeDivorce(final String name, final String spouse, final String date){
		return String.format(t(Key.NARRATIVE_DIVORCE), name, spouse, dateFrag(date));
	}

	/**
	 * Plural-aware narrative for a group of children: "X had one biological
	 * child: Y." / "X had 2 biological children: Y, Z."
	 */
	public String narrativeChildrenGroup(final String name, final int count,
		final String groupLabel, final String names){
		return pluralKey(Key.NARRATIVE_CHILDREN_GROUP_NN,
			count, name, groupLabel, names);
	}

	/** Same as above, with a co-parent. */
	public String narrativeChildrenGroupWith(final String name, final String otherParent,
		final int count, final String groupLabel, final String names){
		return pluralKey(Key.NARRATIVE_CHILDREN_GROUP_WITH_NN,
			count, name, otherParent, groupLabel, names);
	}

	public String narrativeOccupation(final String name, final String occupation){
		return String.format(t(Key.NARRATIVE_OCCUPATION), name, occupation);
	}

	public String narrativeCharacteristic(final String name, final String value){
		return String.format(t(Key.NARRATIVE_CHARACTERISTIC), name, value);
	}

	public String narrativeAttribute(final String name, final String label, final String value){
		return String.format(t(Key.NARRATIVE_ATTRIBUTE), name, label, value);
	}

	public String narrativeResidence(final String name, final String place,
		final String from, final String to){
		return String.format(t(Key.NARRATIVE_RESIDENCE_BASE), name, place, periodFrag(from, to));
	}

	public String narrativeMove(final String name, final String place,
		final String from, final String to){
		return String.format(t(Key.NARRATIVE_MOVE_BASE), name, place, periodFrag(from, to));
	}

	public String narrativeDeath(final String name, final String date,
		final String place, final String cause){
		return String.format(t(Key.NARRATIVE_DEATH_BASE), name,
			causeFrag(cause), dateFrag(date), placeFrag(place));
	}


	/* ======================================================================
	 *                          Plural-aware formatting
	 * ====================================================================== */

	/**
	 * Formats a plural-sensitive message. The pattern must use ICU plural
	 * syntax, e.g.
	 * {@code "{0,plural,one{1 year}other{{0} years}}"}. The first argument
	 * is always the count; further arguments follow.
	 *
	 * <p>This replaces {@link String#format} for any message whose wording
	 * depends on the number of items, because English "1 item / 2 items"
	 * only scratches the surface: Slavic languages have three forms and
	 * Arabic six. ICU4J selects the correct form for the current locale
	 * using the CLDR plural rules.</p>
	 */
	public String plural(final String pattern, final int count, final Object... extraArgs){
		final Object[] args = new Object[extraArgs.length + 1];
		args[0] = count;
		System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
		return new MessageFormat(pattern, language.locale()).format(args);
	}

	/** Plural form looked up directly from the bundle by key. */
	public String pluralKey(final Key key, final int count, final Object... extraArgs){
		return plural(bundle.getString(key.name()), count, extraArgs);
	}

	/**
	 * Maps a relationship type to the base key of its plural pattern.
	 * Unknown types fall back to the generic {@code REL_CHILD}.
	 */
	private static String childTypeKeyBase(final String type){
		if(type == null)
			return "REL_CHILD";
		return switch(type.toLowerCase(Locale.ROOT)){
			case "biological_child" -> "REL_BIOLOGICAL_CHILD";
			case "adoptive_child" -> "REL_ADOPTIVE_CHILD";
			case "foster_child" -> "REL_FOSTER_CHILD";
			case "guarded_child" -> "REL_GUARDED_CHILD";
			case "step_child" -> "REL_STEP_CHILD";
			default -> "REL_CHILD";
		};
	}


	/* ======================================================================
	 *                          Group report
	 * ====================================================================== */

	public String groupIntroductionBody(){
		return t(Key.GROUP_INTRODUCTION_BODY);
	}

	public String groupLifespan(){
		return t(Key.GROUP_LIFESPAN);
	}

	public String groupLifeOf(){
		return t(Key.GROUP_LIFE_OF);
	}

	public String groupData(){
		return t(Key.GROUP_DATA);
	}

	public String groupMembers(){
		return t(Key.GROUP_MEMBERS);
	}

	public String groupNarrativeFounding(final String name, final String date, final String place){
		return String.format(t(Key.GROUP_NARRATIVE_FOUNDING_BASE),
			name, dateFrag(date), placeFrag(place));
	}

	public String groupNarrativeFoundingUnknown(final String name){
		return String.format(t(Key.GROUP_NARRATIVE_FOUNDING_UNKNOWN), name);
	}

	public String groupNarrativeResidence(final String name, final String place){
		return String.format(t(Key.GROUP_NARRATIVE_RESIDENCE), name, place);
	}

	public String groupNarrativeDissolution(final String name, final String date, final String place){
		return String.format(t(Key.GROUP_NARRATIVE_DISSOLUTION_BASE),
			name, dateFrag(date), placeFrag(place));
	}

	/** Plural-aware group counts line (members, subgroups, attributes). */
	public String groupCounts(final int members, final int subgroups, final int attributes){
		return new MessageFormat(t(Key.GROUP_COUNTS), language.locale())
			.format(new Object[]{members, subgroups, attributes});
	}


	/* ======================================================================
	 *                          Historic events / repositories / events
	 * ====================================================================== */

	public String historicEvents(){
		return t(Key.HISTORIC_EVENTS);
	}

	public String historicEventType(){
		return t(Key.HISTORIC_EVENT_TYPE);
	}

	public String repositories(){
		return t(Key.REPOSITORIES);
	}

	public String repositoryCustodian(){
		return t(Key.REPOSITORY_CUSTODIAN);
	}

	public String placeContainedPlaces(){
		return t(Key.PLACE_CONTAINED_PLACES);
	}

	public String eventOf(){
		return t(Key.EVENT_OF);
	}

	public String eventParticipants(){
		return t(Key.EVENT_PARTICIPANTS);
	}

	public String eventOtherParticipants(){
		return t(Key.EVENT_OTHER_PARTICIPANTS);
	}

	public String kind(){
		return t(Key.KIND);
	}


	/* ======================================================================
	 *                          Source root
	 * ====================================================================== */

	public String sourceOf(){
		return t(Key.SOURCE_OF);
	}

	public String sourceCitedBy(){
		return t(Key.SOURCE_CITED_BY);
	}

	/* ======================================================================
	 *                          Privacy details
	 * ====================================================================== */

	public String privacy(){
		return t(Key.PRIVACY);
	}

	public String privacyLevel(){
		return t(Key.PRIVACY_LEVEL);
	}

	public String privacyReason(){
		return t(Key.PRIVACY_REASON);
	}

	public String privacyExpires(){
		return t(Key.PRIVACY_EXPIRES);
	}

	/* ======================================================================
	 *                          Date provenance
	 * ====================================================================== */

	public String dateProvenance(){
		return t(Key.DATE_PROVENANCE);
	}

	/* ======================================================================
	 *                          Coverage report
	 * ====================================================================== */

	public String coverageReport(){
		return t(Key.COVERAGE_REPORT);
	}

	public String coverageRecord(){
		return t(Key.COVERAGE_RECORD);
	}

	public String coverageFields(){
		return t(Key.COVERAGE_FIELDS);
	}

	public String coverageSources(){
		return t(Key.COVERAGE_SOURCES);
	}

	public String coverageEvidence(){
		return t(Key.COVERAGE_EVIDENCE);
	}

	public String coverageMissingSources(){
		return t(Key.COVERAGE_MISSING_SOURCES);
	}


	/* ======================================================================
	 *                          Place / Repository roots
	 * ====================================================================== */

	public String placeOf(){
		return t(Key.PLACE_OF);
	}

	public String placeEvents(){
		return t(Key.PLACE_EVENTS);
	}

	public String placeEventParticipations(){
		return t(Key.PLACE_EVENT_PARTICIPATIONS);
	}

	public String placeAttributes(){
		return t(Key.PLACE_ATTRIBUTES);
	}

	public String repositoryOf(){
		return t(Key.REPOSITORY_OF);
	}

	public String repositorySources(){
		return t(Key.REPOSITORY_SOURCES);
	}


	/* ======================================================================
	 *                          Document root
	 * ====================================================================== */

	public String documentOf(){
		return t(Key.DOCUMENT_OF);
	}

	public String documentReferencedBySources(){
		return t(Key.DOCUMENT_REFERENCED_BY_SOURCES);
	}

	public String documentReferencedByCitations(){
		return t(Key.DOCUMENT_REFERENCED_BY_CITATIONS);
	}

	/* ======================================================================
	 *                          Research question / Conclusion roots
	 * ====================================================================== */

	public String researchQuestionOf(){
		return t(Key.RESEARCH_QUESTION_OF);
	}

	public String researchTargets(){
		return t(Key.RESEARCH_TARGETS);
	}

	public String conclusionOf(){
		return t(Key.CONCLUSION_OF);
	}


	public String columnIndividual(){
		return t(Key.COLUMN_INDIVIDUAL);
	}

	public String columnPlace(){
		return t(Key.COLUMN_PLACE);
	}


	/* ======================================================================
	 *                          Fragment helpers
	 * ====================================================================== */

	private String dateFrag(final String date){
		return (date != null? String.format(t(Key.DATE_FRAGMENT), date): "");
	}

	private String placeFrag(final String place){
		return (place != null? String.format(t(Key.PLACE_FRAGMENT), place): "");
	}

	private String causeFrag(final String cause){
		return (cause != null? String.format(t(Key.CAUSE_FRAGMENT), cause): "");
	}

	private String periodFrag(final String from, final String to){
		if(from != null && to != null)
			return String.format(t(Key.PERIOD_FROM_TO_FRAGMENT), from, to);
		if(from != null)
			return String.format(t(Key.PERIOD_FROM_FRAGMENT), from);
		if(to != null)
			return String.format(t(Key.PERIOD_TO_FRAGMENT), to);
		return "";
	}


	private String t(final Key key){
		return bundle.getString(key.name());
	}


	/* ======================================================================
	 *                          Keys
	 * ====================================================================== */

	/**
	 * Enum whose {@link #name()} values are the property keys. Keeping keys
	 * in an enum makes typos compile-time errors.
	 */
	private enum Key{
		// section headings
		SUBTITLE,
		INTRODUCTION,
		STATISTICS,
		PATERNAL_ANCESTRY,
		MATERNAL_ANCESTRY,
		DESCENDANTS,
		DIRECT_RELATIONS,
		INDIRECT_RELATIONS,
		NOTES,
		SOURCES,
		MEDIA,
		PLACES,
		DOCUMENTS,
		INDEX_OF_INDIVIDUALS,
		INDEX_OF_PLACES,
		// context / research
		CONTEXT_SECTION,
		RESEARCH_SECTION,
		IDENTITY_HYPOTHESES,
		CONCLUSIONS,
		// life story
		LIFE_OF,
		PERSONAL_DATA,
		LIFE_EVENTS,
		ATTRIBUTES,
		RELATIONSHIPS,
		// narrative fragments
		INTRODUCTION_BODY,
		NARRATIVE_NOTE,
		// inline labels
		CHILD_OF,
		SEX,
		SEX_UNKNOWN,
		CITATIONS,
		QUESTION,
		STATUS,
		CONCLUSION,
		TYPE,
		DATE,
		PLACE,
		CAUSE,
		ROLE,
		PERSON,
		VALUE,
		FROM,
		TO,
		ID,
		NAME,
		SUBJECT,
		ROLE_PARENT,
		ROLE_SPOUSE,
		ROLE_CHILD,
		// indirect-relations table
		RELATION,
		OF,
		RELATION_SPOUSE_OF,
		RELATION_ASSOCIATE_OF,
		// event / attribute / relationship labels
		AGENCY,
		NOTE,
		DESCRIPTION,
		VALID_FROM,
		VALID_TO,
		RELATIONSHIP_STATUS,
		// source / repository / document labels
		SOURCE,
		SOURCE_AUTHOR,
		SOURCE_PUBLISHER,
		SOURCE_DATE,
		SOURCE_PLACE,
		SOURCE_MEDIA_TYPE,
		SOURCE_REPOSITORY,
		SOURCE_LOCATOR,
		SOURCE_DOCUMENTS,
		EXTRACT_TEXT,
		DOCUMENT_URI,
		DOCUMENT_DESCRIPTION,
		DOCUMENT_MAPPING,
		PREFERRED_IMAGE,
		// place labels
		PLACE_TYPE,
		PLACE_COORDINATES,
		PLACE_ORIGINAL_TEXT,
		PLACE_JURISDICTIONS,
		PLACE_NAME_VARIANT,
		PLACE_CITATIONS,
		PLACE_CITED_BY,
		// evidence
		EVIDENCE,
		EVIDENCE_SOURCE_TYPE,
		EVIDENCE_INFORMATION_TYPE,
		EVIDENCE_TYPE,
		// research / conclusion
		RESEARCH_ISSUE,
		RESEARCH_PROOF_STATUS,
		RESEARCH_NARRATIVE,
		RESEARCH_CONCLUSION_CONFIDENCE,
		RESEARCH_CLOSED_DATE,
		RESEARCH_ACTIVITIES,
		RESEARCH_TASKS,
		RESEARCH_RESULT,
		RESEARCH_SEARCH_SCOPE,
		RESEARCH_TARGET,
		RESEARCH_OBSERVATION,
		RESEARCH_PARENT_ACTIVITY,
		RESEARCH_TASK_PRIORITY,
		RESEARCH_TASK_DUE_DATE,
		RESEARCH_TASK_CREATED_BY,
		RESEARCH_TASK_OUTCOME,
		RESEARCH_RESOLVES,
		RESEARCH_PREFERRED,
		RESEARCH_LINKED_QUESTIONS,
		CONTEXT_IMPACT,
		CONTEXT_RATIONALE,
		IDENTITY_CANDIDATES,
		IDENTITY_COMMENT,
		// empty states
		NO_ANCESTORS,
		NO_INDIRECT_RELATIONS,
		EMPTY,
		// statistic labels (non-plural)
		LIFESPAN,
		EVENTS_COUNT,
		SOURCES_COUNT,
		// cultural norms
		CULTURAL_NORMS,
		CULTURAL_NORM_RULE_TYPE,
		CULTURAL_NORM_VALID_FROM,
		CULTURAL_NORM_VALID_TO,
		CULTURAL_NORM_PLACE,
		// groups
		GROUPS,
		GROUP_TYPE,
		GROUP_MEMBERSHIPS,
		GROUP_ATTRIBUTES,
		GROUP_PARENT_GROUPS,
		GROUP_CHILD_GROUPS,
		GROUP_EVENTS,
		// header
		HEADER,
		HEADER_PROTOCOL,
		HEADER_PROTOCOL_VERSION,
		HEADER_SOURCE,
		HEADER_SOURCE_VERSION,
		HEADER_ORGANIZATION,
		HEADER_DATE,
		HEADER_COPYRIGHT,
		HEADER_SUBMITTER,
		HEADER_SUBMITTER_NOTE,
		HEADER_SCOPE,
		// notes
		NOTE_TITLE,
		NOTE_TRANSLATION,
		// images
		IMAGE_CROPPED_REGION,
		// audit
		AUDIT,
		AUDIT_CREATED,
		AUDIT_UPDATED,
		// date display
		DATE_DECADE,
		DATE_CENTURY,
		DATE_CENTURY_WITH_PART,
		DATE_BETWEEN,
		DATE_AFTER,
		DATE_BEFORE,
		DATE_FROM_TO,
		DATE_FROM,
		DATE_TO,
		DATE_MARGIN,
		APPROX_BASIS_STATED,
		APPROX_BASIS_CALCULATED,
		APPROX_BASIS_CONVENTIONAL,
		APPROX_BASIS_UNSPECIFIED,
		APPROX_BASIS_CONVENTIONAL_PER,
		// century parts
		CENTURY_PART_FIRST_QUARTER,
		CENTURY_PART_SECOND_QUARTER,
		CENTURY_PART_THIRD_QUARTER,
		CENTURY_PART_FOURTH_QUARTER,
		CENTURY_PART_FIRST_HALF,
		CENTURY_PART_SECOND_HALF,
		CENTURY_PART_EARLY,
		CENTURY_PART_MID,
		CENTURY_PART_LATE,
		// calendars
		CALENDAR_JULIAN,
		CALENDAR_ISLAMIC,
		CALENDAR_HEBREW,
		CALENDAR_CHINESE,
		CALENDAR_INDIAN,
		CALENDAR_BUDDHIST,
		CALENDAR_FRENCH_REPUBLICAN,
		CALENDAR_COPTIC,
		CALENDAR_SOVIET_ETERNAL,
		CALENDAR_ETHIOPIAN,
		CALENDAR_MAYAN,
		// names
		NAME_CULTURAL_NORM,
		// plural-sensitive children group labels
		REL_CHILD,
		REL_BIOLOGICAL_CHILD,
		REL_ADOPTIVE_CHILD,
		REL_FOSTER_CHILD,
		REL_GUARDED_CHILD,
		REL_STEP_CHILD,
		// plural-sensitive narrative
		NARRATIVE_CHILDREN_GROUP_NN,
		NARRATIVE_CHILDREN_GROUP_WITH_NN,
		// plural-sensitive units
		AGE_YEARS,
		MARGIN_YEARS,
		MARGIN_MONTHS,
		MARGIN_WEEKS,
		MARGIN_DAYS,
		RELATIONS_COUNT,
		// narrative templates (non-plural)
		NARRATIVE_BIRTH_BASE,
		NARRATIVE_BIRTH_UNKNOWN,
		NARRATIVE_MARRIAGE_BASE,
		NARRATIVE_MARRIAGE_NOSPOUSE_BASE,
		NARRATIVE_DIVORCE,
		NARRATIVE_OCCUPATION,
		NARRATIVE_CHARACTERISTIC,
		NARRATIVE_ATTRIBUTE,
		NARRATIVE_RESIDENCE_BASE,
		NARRATIVE_MOVE_BASE,
		NARRATIVE_DEATH_BASE,
		DATE_FRAGMENT,
		PLACE_FRAGMENT,
		CAUSE_FRAGMENT,
		PERIOD_FROM_TO_FRAGMENT,
		PERIOD_FROM_FRAGMENT,
		PERIOD_TO_FRAGMENT,
		// group report
		GROUP_INTRODUCTION_BODY,
		GROUP_LIFESPAN,
		GROUP_LIFE_OF,
		GROUP_DATA,
		GROUP_MEMBERS,
		GROUP_COUNTS,
		GROUP_NARRATIVE_FOUNDING_BASE,
		GROUP_NARRATIVE_FOUNDING_UNKNOWN,
		GROUP_NARRATIVE_RESIDENCE,
		GROUP_NARRATIVE_DISSOLUTION_BASE,
		// historic events
		HISTORIC_EVENTS,
		HISTORIC_EVENT_TYPE,
		// repositories
		REPOSITORIES,
		REPOSITORY_CUSTODIAN,
		// places (reverse direction)
		PLACE_CONTAINED_PLACES,
		// event root
		EVENT_OF,
		EVENT_PARTICIPANTS,
		EVENT_OTHER_PARTICIPANTS,
		KIND,
		// source root
		SOURCE_OF,
		SOURCE_CITED_BY,
		// privacy details
		PRIVACY,
		PRIVACY_LEVEL,
		PRIVACY_REASON,
		PRIVACY_EXPIRES,
		// date provenance
		DATE_PROVENANCE,
		// coverage report
		COVERAGE_REPORT,
		COVERAGE_RECORD,
		COVERAGE_FIELDS,
		COVERAGE_SOURCES,
		COVERAGE_EVIDENCE,
		COVERAGE_MISSING_SOURCES,
		// place / repository roots
		PLACE_OF,
		PLACE_EVENTS,
		PLACE_EVENT_PARTICIPATIONS,
		PLACE_ATTRIBUTES,
		REPOSITORY_OF,
		REPOSITORY_SOURCES,
		// document root
		DOCUMENT_OF,
		DOCUMENT_REFERENCED_BY_SOURCES,
		DOCUMENT_REFERENCED_BY_CITATIONS,
		// research question / conclusion roots
		RESEARCH_QUESTION_OF,
		RESEARCH_TARGETS,
		CONCLUSION_OF,
		YEAR,
		TITLE,
		TIMELINE,
		EXTRACT_NOTE,
		EXTRACT_CROP,
		SOURCE_REPOSITORY_NOTE,
		NAME_VARIANT,
		CULTURAL_NORM_OF,
		CULTURAL_NORM_INVOCATIONS,
		HISTORIC_EVENT_OF,
		HISTORIC_EVENT_IMPACTS,
		IDENTITY_HYPOTHESIS,
		RESEARCH_ACTIVITY_OF,
		RESEARCH_TASK_OF,
		COLUMN_INDIVIDUAL,
		COLUMN_PLACE
	}


	/* ======================================================================
	 *                          UTF-8 loader
	 * ====================================================================== */

	/**
	 * {@link ResourceBundle.Control} that loads {@code .properties} files as
	 * UTF-8 instead of the ISO-8859-1 default.
	 */
	private static final class Utf8Control extends ResourceBundle.Control{

		@Override
		public ResourceBundle newBundle(final String baseName, final Locale locale,
			final String format, final ClassLoader loader, final boolean reload) throws IOException, IllegalAccessException, InstantiationException{
			if(!"java.properties".equals(format))
				return super.newBundle(baseName, locale, format, loader, reload);

			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");
			try(InputStream is = loader.getResourceAsStream(resourceName)){
				if(is == null)
					return null;
				try(Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)){
					return new PropertyResourceBundle(reader);
				}
			}
		}

	}

}
