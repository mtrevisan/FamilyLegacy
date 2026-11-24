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
package io.github.mtrevisan.familylegacy.reports.i18n;

import java.util.Locale;



/**
 * Provides localized labels for section headings, structural headers, metadata,
 * tables, inline fields, research entities, and report coverage details.
 */
public final class SectionLabelProvider{

	private final ReportLabels labels;

	public SectionLabelProvider(final ReportLabels labels){
		this.labels = labels;
	}

	/* ======================================================================
	 *                          Section Headings
	 * ====================================================================== */

	public String contents(){
		return labels.getString("CONTENTS");
	}

	public String subtitle(){
		return labels.getString("SUBTITLE");
	}

	public String introduction(){
		return labels.getString("INTRODUCTION");
	}

	public String statistics(){
		return labels.getString("STATISTICS");
	}

	public String paternalAncestry(){
		return labels.getString("PATERNAL_ANCESTRY");
	}

	public String maternalAncestry(){
		return labels.getString("MATERNAL_ANCESTRY");
	}

	public String descendants(){
		return labels.getString("DESCENDANTS");
	}

	public String directRelations(){
		return labels.getString("DIRECT_RELATIONS");
	}

	public String indirectRelations(){
		return labels.getString("INDIRECT_RELATIONS");
	}

	public String notes(){
		return labels.getString("NOTES");
	}

	public String stories(){
		return labels.getString("STORIES");
	}

	public String sources(){
		return labels.getString("SOURCES");
	}

	public String media(){
		return labels.getString("MEDIA");
	}

	public String places(){
		return labels.getString("PLACES");
	}

	public String documents(){
		return labels.getString("DOCUMENTS");
	}

	public String indexOfIndividuals(){
		return labels.getString("INDEX_OF_INDIVIDUALS");
	}

	public String indexOfGroups(){
		return labels.getString("INDEX_OF_GROUPS");
	}

	public String indexOfPlaces(){
		return labels.getString("INDEX_OF_PLACES");
	}

	/* ======================================================================
	 *                          Life story sections
	 * ====================================================================== */

	public String lifeOf(){
		return labels.getString("LIFE_OF");
	}

	public String personalData(){
		return labels.getString("PERSONAL_DATA");
	}

	public String lifeEvents(){
		return labels.getString("LIFE_EVENTS");
	}

	public String attributes(){
		return labels.getString("ATTRIBUTES");
	}

	public String relationships(){
		return labels.getString("RELATIONSHIPS");
	}

	/* ======================================================================
	 *                          Context and research sections
	 * ====================================================================== */

	public String contextSection(){
		return labels.getString("CONTEXT_SECTION");
	}

	public String researchSection(){
		return labels.getString("RESEARCH_SECTION");
	}

	public String identityHypotheses(){
		return labels.getString("IDENTITY_HYPOTHESES");
	}

	public String conclusions(){
		return labels.getString("CONCLUSIONS");
	}

	/* ======================================================================
	 *                          Inline Labels & Tables
	 * ====================================================================== */

	public String childOf(){
		return labels.getString("CHILD_OF");
	}

	public String sex(){
		return labels.getString("SEX");
	}

	public String sexUnknown(){
		return labels.getString("SEX_UNKNOWN");
	}

	public String citations(){
		return labels.getString("CITATIONS");
	}

	public String question(){
		return labels.getString("QUESTION");
	}

	public String status(){
		return labels.getString("STATUS");
	}

	public String conclusion(){
		return labels.getString("CONCLUSION");
	}

	public String type(){
		return labels.getString("TYPE");
	}

	public String date(){
		return labels.getString("DATE");
	}

	public String place(){
		return labels.getString("PLACE");
	}

	public String cause(){
		return labels.getString("CAUSE");
	}

	public String role(){
		return labels.getString("ROLE");
	}

	public String person(){
		return labels.getString("PERSON");
	}

	public String value(){
		return labels.getString("VALUE");
	}

	public String from(){
		return labels.getString("FROM");
	}

	public String to(){
		return labels.getString("TO");
	}

	public String id(){
		return labels.getString("ID");
	}

	public String name(){
		return labels.getString("NAME");
	}

	public String subject(){
		return labels.getString("SUBJECT");
	}

	public String roleChild(){
		return labels.getString("ROLE_CHILD");
	}

	public String year(){
		return labels.getString("YEAR");
	}

	public String title(){
		return labels.getString("TITLE");
	}

	public String timeline(){
		return labels.getString("TIMELINE");
	}

	public String extractNote(){
		return labels.getString("EXTRACT_NOTE");
	}

	public String extractCrop(){
		return labels.getString("EXTRACT_CROP");
	}

	public String stanceContradicts(){
		return labels.getString("STANCE_CONTRADICTS");
	}

	public String sourceRepositoryNote(){
		return labels.getString("SOURCE_REPOSITORY_NOTE");
	}

	public String nameVariant(){
		return labels.getString("NAME_VARIANT");
	}

	/* ======================================================================
	 *                          Indirect-relations table
	 * ====================================================================== */

	public String relation(){
		return labels.getString("RELATION");
	}

	public String of(){
		return labels.getString("OF");
	}

	public String relationSpouseOf(){
		return labels.getString("RELATION_SPOUSE_OF");
	}

	public String relationAssociateOf(){
		return labels.getString("RELATION_ASSOCIATE_OF");
	}

	/* ======================================================================
	 *                          Event / attribute / relationship labels
	 * ====================================================================== */

	public String note(){
		return labels.getString("NOTE");
	}

	public String description(){
		return labels.getString("DESCRIPTION");
	}

	public String validFrom(){
		return labels.getString("VALID_FROM");
	}

	public String validTo(){
		return labels.getString("VALID_TO");
	}

	public String relationshipStatus(){
		return labels.getString("RELATIONSHIP_STATUS");
	}

	/* ======================================================================
	 *                          Sources, Repositories, Documents
	 * ====================================================================== */

	public String source(){
		return labels.getString("SOURCE");
	}

	public String sourceAuthor(){
		return labels.getString("SOURCE_AUTHOR");
	}

	public String sourcePublisher(){
		return labels.getString("SOURCE_PUBLISHER");
	}

	public String sourceDate(){
		return labels.getString("SOURCE_DATE");
	}

	public String sourcePlace(){
		return labels.getString("SOURCE_PLACE");
	}

	public String sourceKind(){
		return labels.getString("SOURCE_KIND");
	}

	public String sourceKindUnspecified(){
		return labels.getString("SOURCE_KIND_UNSPECIFIED");
	}

	public String sourceMediaType(){
		return labels.getString("SOURCE_MEDIA_TYPE");
	}

	public String sourceRepository(){
		return labels.getString("SOURCE_REPOSITORY");
	}

	public String sourceLocator(){
		return labels.getString("SOURCE_LOCATOR");
	}

	public String sourceDocuments(){
		return labels.getString("SOURCE_DOCUMENTS");
	}

	public String extractText(){
		return labels.getString("EXTRACT_TEXT");
	}

	public String documentUri(){
		return labels.getString("DOCUMENT_URI");
	}

	public String documentDescription(){
		return labels.getString("DOCUMENT_DESCRIPTION");
	}

	public String documentMapping(){
		return labels.getString("DOCUMENT_MAPPING");
	}

	public String preferredImage(){
		return labels.getString("PREFERRED_IMAGE");
	}

	/* ======================================================================
	 *                          Place labels
	 * ====================================================================== */

	public String placeType(){
		return labels.getString("PLACE_TYPE");
	}

	public String placeAttested(){
		return labels.getString("PLACE_ATTESTED");
	}

	public String placeCoordinates(){
		return labels.getString("PLACE_COORDINATES");
	}

	public String placeOriginalText(){
		return labels.getString("PLACE_ORIGINAL_TEXT");
	}

	public String placeJurisdictions(){
		return labels.getString("PLACE_JURISDICTIONS");
	}

	public String placeNameVariant(){
		return labels.getString("PLACE_NAME_VARIANT");
	}

	public String placeCitations(){
		return labels.getString("PLACE_CITATIONS");
	}

	public String placeCitedBy(){
		return labels.getString("PLACE_CITED_BY");
	}

	/* ======================================================================
	 *                          Evidence labels
	 * ====================================================================== */

	public String evidence(){
		return labels.getString("EVIDENCE");
	}

	public String evidenceSourceType(){
		return labels.getString("EVIDENCE_SOURCE_TYPE");
	}

	public String evidenceInformationType(){
		return labels.getString("EVIDENCE_INFORMATION_TYPE");
	}

	public String evidenceType(){
		return labels.getString("EVIDENCE_TYPE");
	}

	/* ======================================================================
	 *                          Research, Questions & Tasks
	 * ====================================================================== */

	public String researchIssue(){
		return labels.getString("RESEARCH_ISSUE");
	}

	public String researchProofStatus(){
		return labels.getString("RESEARCH_PROOF_STATUS");
	}

	public String researchNarrative(){
		return labels.getString("RESEARCH_NARRATIVE");
	}

	public String researchConclusionConfidence(){
		return labels.getString("RESEARCH_CONCLUSION_CONFIDENCE");
	}

	public String researchClosedDate(){
		return labels.getString("RESEARCH_CLOSED_DATE");
	}

	public String researchActivities(){
		return labels.getString("RESEARCH_ACTIVITIES");
	}

	public String researchTasks(){
		return labels.getString("RESEARCH_TASKS");
	}

	public String researchResult(){
		return labels.getString("RESEARCH_RESULT");
	}

	public String researchSearchScope(){
		return labels.getString("RESEARCH_SEARCH_SCOPE");
	}

	public String researchTarget(){
		return labels.getString("RESEARCH_TARGET");
	}

	public String researchObservation(){
		return labels.getString("RESEARCH_OBSERVATION");
	}

	public String researchParentActivity(){
		return labels.getString("RESEARCH_PARENT_ACTIVITY");
	}

	public String researchTaskPriority(){
		return labels.getString("RESEARCH_TASK_PRIORITY");
	}

	public String researchTaskDueDate(){
		return labels.getString("RESEARCH_TASK_DUE_DATE");
	}

	public String researchTaskCreatedBy(){
		return labels.getString("RESEARCH_TASK_CREATED_BY");
	}

	public String researchTaskOutcome(){
		return labels.getString("RESEARCH_TASK_OUTCOME");
	}

	public String researchResolves(){
		return labels.getString("RESEARCH_RESOLVES");
	}

	public String researchPreferred(){
		return labels.getString("RESEARCH_PREFERRED");
	}

	public String researchLinkedQuestions(){
		return labels.getString("RESEARCH_LINKED_QUESTIONS");
	}

	public String contextImpact(){
		return labels.getString("CONTEXT_IMPACT");
	}

	public String contextRationale(){
		return labels.getString("CONTEXT_RATIONALE");
	}

	public String identityCandidates(){
		return labels.getString("IDENTITY_CANDIDATES");
	}

	public String identityComment(){
		return labels.getString("IDENTITY_COMMENT");
	}

	/* ======================================================================
	 *                          Cultural norms
	 * ====================================================================== */

	public String culturalNorms(){
		return labels.getString("CULTURAL_NORMS");
	}

	public String culturalNormRuleType(){
		return labels.getString("CULTURAL_NORM_RULE_TYPE");
	}

	public String culturalNormValidFrom(){
		return labels.getString("CULTURAL_NORM_VALID_FROM");
	}

	public String culturalNormValidTo(){
		return labels.getString("CULTURAL_NORM_VALID_TO");
	}

	public String culturalNormPlace(){
		return labels.getString("CULTURAL_NORM_PLACE");
	}

	public String culturalNormOf(){
		return labels.getString("CULTURAL_NORM_OF");
	}

	public String culturalNormInvocations(){
		return labels.getString("CULTURAL_NORM_INVOCATIONS");
	}

	public String nameCulturalNorm(){
		return labels.getString("NAME_CULTURAL_NORM");
	}

	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	public String groups(){
		return labels.getString("GROUPS");
	}

	public String groupType(){
		return labels.getString("GROUP_TYPE");
	}

	public String groupMemberships(){
		return labels.getString("GROUP_MEMBERSHIPS");
	}

	public String groupAttributes(){
		return labels.getString("GROUP_ATTRIBUTES");
	}

	public String groupParentGroups(){
		return labels.getString("GROUP_PARENT_GROUPS");
	}

	public String groupChildGroups(){
		return labels.getString("GROUP_CHILD_GROUPS");
	}

	public String groupEvents(){
		return labels.getString("GROUP_EVENTS");
	}

	/* ======================================================================
	 *                          Header, Notes, Audit & Empty States
	 * ====================================================================== */

	public String header(){
		return labels.getString("HEADER");
	}

	public String headerProtocol(){
		return labels.getString("HEADER_PROTOCOL");
	}

	public String headerProtocolVersion(){
		return labels.getString("HEADER_PROTOCOL_VERSION");
	}

	public String headerSource(){
		return labels.getString("HEADER_SOURCE");
	}

	public String headerSourceVersion(){
		return labels.getString("HEADER_SOURCE_VERSION");
	}

	public String headerOrganization(){
		return labels.getString("HEADER_ORGANIZATION");
	}

	public String headerDate(){
		return labels.getString("HEADER_DATE");
	}

	public String headerCopyright(){
		return labels.getString("HEADER_COPYRIGHT");
	}

	public String headerSubmitter(){
		return labels.getString("HEADER_SUBMITTER");
	}

	public String headerSubmitterNote(){
		return labels.getString("HEADER_SUBMITTER_NOTE");
	}

	public String headerScope(){
		return labels.getString("HEADER_SCOPE");
	}

	public String noteTitle(){
		return labels.getString("NOTE_TITLE");
	}

	public String noteTranslation(){
		return labels.getString("NOTE_TRANSLATION");
	}

	public String imageCroppedRegion(){
		return labels.getString("IMAGE_CROPPED_REGION");
	}

	public String audit(){
		return labels.getString("AUDIT");
	}

	public String auditCreated(){
		return labels.getString("AUDIT_CREATED");
	}

	public String auditUpdated(){
		return labels.getString("AUDIT_UPDATED");
	}

	public String historicEvents(){
		return labels.getString("HISTORIC_EVENTS");
	}

	public String historicEventType(){
		return labels.getString("HISTORIC_EVENT_TYPE");
	}

	public String historicEventOf(){
		return labels.getString("HISTORIC_EVENT_OF");
	}

	public String historicEventImpacts(){
		return labels.getString("HISTORIC_EVENT_IMPACTS");
	}

	public String repositories(){
		return labels.getString("REPOSITORIES");
	}

	public String repositoryCustodian(){
		return labels.getString("REPOSITORY_CUSTODIAN");
	}

	public String placeContainedPlaces(){
		return labels.getString("PLACE_CONTAINED_PLACES");
	}

	public String eventOf(){
		return labels.getString("EVENT_OF");
	}

	/** @return	The heading of a generation of the direct relations ("Generation 2"). */
	public String generationN(final String number){
		return String.format(labels.getString("GENERATION"), number);
	}

	/** @return	What the direct relations are: who is in them and how they are ordered. */
	public String directRelationsNote(){
		return labels.getString("DIRECT_RELATIONS_NOTE");
	}

	/** @return	What a tree says of a person that is drawn already, with the children: they are not drawn again. */
	public String treeSeeAbove(){
		return labels.getString("TREE_SEE_ABOVE");
	}

	/** @return	A kinship term that holds through an adoptive, foster or other legal link, not by blood. */
	public String kinshipLegal(final String kinship){
		return String.format(labels.getString("KINSHIP_LEGAL"), kinship);
	}

	/** @return	The kinship term of a person from the report subject, as in "uncle of John": the pattern takes the term and the name. */
	public String collateralOf(final String kinship, final String person){
		return String.format(labels.getString("COLLATERAL_OF"), kinship, person);
	}

	/** @return	The heading of an event without a type (capitalised). */
	public String eventGeneric(){
		return labels.getString("EVENT_GENERIC");
	}

	/** @return	The name of an event without a type, in the middle of a sentence. */
	public String eventGenericLower(){
		return labels.getString("EVENT_GENERIC_LOWER");
	}

	public String eventParticipants(){
		return labels.getString("EVENT_PARTICIPANTS");
	}

	public String eventOtherParticipants(){
		return labels.getString("EVENT_OTHER_PARTICIPANTS");
	}

	public String kind(){
		return labels.getString("KIND");
	}

	/* ======================================================================
	 *                          Source root
	 * ====================================================================== */

	public String sourceOf(){
		return labels.getString("SOURCE_OF");
	}

	public String sourceCitedBy(){
		return labels.getString("SOURCE_CITED_BY");
	}

	public String sourceDerivedFrom(){
		return labels.getString("SOURCE_DERIVED_FROM");
	}

	public String sourceOriginal(){
		return labels.getString("SOURCE_ORIGINAL");
	}

	public String sourceDerivatives(){
		return labels.getString("SOURCE_DERIVATIVES");
	}

	public String sourceOriginalOfCited(){
		return labels.getString("SOURCE_ORIGINAL_OF_CITED");
	}

	/* ======================================================================
	 *                          Privacy details
	 * ====================================================================== */

	public String privacy(){
		return labels.getString("PRIVACY");
	}

	public String privacyLevel(){
		return labels.getString("PRIVACY_LEVEL");
	}

	public String privacyReason(){
		return labels.getString("PRIVACY_REASON");
	}

	public String privacyExpires(){
		return labels.getString("PRIVACY_EXPIRES");
	}

	/* ======================================================================
	 *                          Coverage report
	 * ====================================================================== */

	public String coverageReport(){
		return labels.getString("COVERAGE_REPORT");
	}

	public String coverageRecord(){
		return labels.getString("COVERAGE_RECORD");
	}

	public String coverageFields(){
		return labels.getString("COVERAGE_FIELDS");
	}

	public String coverageSources(){
		return labels.getString("COVERAGE_SOURCES");
	}

	public String coverageEvidence(){
		return labels.getString("COVERAGE_EVIDENCE");
	}

	/** The display name of a kind of record in the coverage report; {@code kind} is the suffix of the key {@code COVERAGE_KIND_*}. */
	public String coverageKind(final String kind){
		return labels.getString("COVERAGE_KIND_" + kind);
	}

	public String coverageMissingSources(){
		return labels.getString("COVERAGE_MISSING_SOURCES");
	}

	public String placeOf(){
		return labels.getString("PLACE_OF");
	}

	public String placeEvents(){
		return labels.getString("PLACE_EVENTS");
	}

	public String placeEventParticipations(){
		return labels.getString("PLACE_EVENT_PARTICIPATIONS");
	}

	public String placeAttributes(){
		return labels.getString("PLACE_ATTRIBUTES");
	}

	public String repositoryOf(){
		return labels.getString("REPOSITORY_OF");
	}

	public String repositorySources(){
		return labels.getString("REPOSITORY_SOURCES");
	}

	/* ======================================================================
	 *                          Document root
	 * ====================================================================== */

	public String documentOf(){
		return labels.getString("DOCUMENT_OF");
	}

	public String documentReferencedBySources(){
		return labels.getString("DOCUMENT_REFERENCED_BY_SOURCES");
	}

	public String documentReferencedByCitations(){
		return labels.getString("DOCUMENT_REFERENCED_BY_CITATIONS");
	}

	/* ======================================================================
	 *                          Research question / Conclusion roots
	 * ====================================================================== */

	public String researchQuestionOf(){
		return labels.getString("RESEARCH_QUESTION_OF");
	}

	public String researchTargets(){
		return labels.getString("RESEARCH_TARGETS");
	}

	public String gpsProofOf(){
		return labels.getString("GPS_PROOF_OF");
	}

	public String gpsResearchQuestion(){
		return labels.getString("GPS_RESEARCH_QUESTION");
	}

	public String gpsEvidenceSummary(){
		return labels.getString("GPS_EVIDENCE_SUMMARY");
	}

	public String gpsConflictingEvidence(){
		return labels.getString("GPS_CONFLICTING_EVIDENCE");
	}

	public String gpsAnalysis(){
		return labels.getString("GPS_ANALYSIS");
	}

	public String gpsReasoning(){
		return labels.getString("GPS_REASONING");
	}

	public String gpsNothingRecorded(){
		return labels.getString("GPS_NOTHING_RECORDED");
	}

	public String gpsCitationsTally(){
		return labels.getString("GPS_CITATIONS_TALLY");
	}

	public String gpsContradictingCitations(){
		return labels.getString("GPS_CONTRADICTING_CITATIONS");
	}

	public String gpsNoProofStatus(){
		return labels.getString("GPS_NO_PROOF_STATUS");
	}

	public String conclusionOf(){
		return labels.getString("CONCLUSION_OF");
	}

	public String identityHypothesis(){
		return labels.getString("IDENTITY_HYPOTHESIS");
	}

	public String researchActivityOf(){
		return labels.getString("RESEARCH_ACTIVITY_OF");
	}

	public String researchTaskOf(){
		return labels.getString("RESEARCH_TASK_OF");
	}

	public String columnPlace(){
		return labels.getString("COLUMN_PLACE");
	}

	public String empty(){
		return labels.getString("EMPTY");
	}

	public String noAncestors(){
		return labels.getString("NO_ANCESTORS");
	}

	public String noIndirectRelations(){
		return labels.getString("NO_INDIRECT_RELATIONS");
	}

	/* ======================================================================
	 *                          Statistic labels (non-plural)
	 * ====================================================================== */

	public String lifespan(){
		return labels.getString("LIFESPAN");
	}

	public String eventsCount(){
		return labels.getString("EVENTS_COUNT");
	}

	public String sourcesCount(){
		return labels.getString("SOURCES_COUNT");
	}

	/* ======================================================================
	 *                          Pedigree tree
	 * ====================================================================== */

	public String ancestorTree(){
		return labels.getString("ANCESTOR_TREE");
	}

	public String descendantTree(){
		return labels.getString("DESCENDANT_TREE");
	}

	public String pedigreeTreeNote(){
		return labels.getString("PEDIGREE_TREE_NOTE");
	}

	public String collateralRelatives(){
		return labels.getString("COLLATERAL_RELATIVES");
	}

	public String ancestorReportTitle(){
		return labels.getString("ANCESTOR_REPORT_TITLE");
	}

	public String descendantReportTitle(){
		return labels.getString("DESCENDANT_REPORT_TITLE");
	}

	public String familyBookTitle(){
		return labels.getString("FAMILY_BOOK_TITLE");
	}

	public String fullBibliographyTitle(){
		return labels.getString("FULL_BIBLIOGRAPHY_TITLE");
	}

	public String toVerifyTitle(){
		return labels.getString("TO_VERIFY_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: the number of findings, the number of groups). */
	public String toVerifyIntro(){
		return labels.getString("TO_VERIFY_INTRO");
	}

	public String toVerifyNothing(){
		return labels.getString("TO_VERIFY_NOTHING");
	}

	public String toVerifyNotIncluded(){
		return labels.getString("TO_VERIFY_NOT_INCLUDED");
	}

	/** @param columnKey	{@code GROUP}, {@code COUNT}, {@code RECORD} or {@code DETAIL}. */
	public String toVerifyColumn(final String columnKey){
		return labels.getString("TO_VERIFY_COLUMN_" + columnKey);
	}

	public String taggedRecordsTitle(){
		return labels.getString("TAGGED_RECORDS_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: the number of records, the number of tags). */
	public String taggedRecordsIntro(){
		return labels.getString("TAGGED_RECORDS_INTRO");
	}

	public String taggedRecordsNothing(){
		return labels.getString("TAGGED_RECORDS_NOTHING");
	}

	public String taggedRecordsNotInFile(){
		return labels.getString("TAGGED_RECORDS_NOT_IN_FILE");
	}

	/** @param columnKey	{@code TAG}, {@code COUNT}, {@code KIND} or {@code RECORD}. */
	public String taggedRecordsColumn(final String columnKey){
		return labels.getString("TAGGED_RECORDS_COLUMN_" + columnKey);
	}

	/** @param recordType	The type of a record that can be tagged (as the record names it, in any case). */
	public String taggedRecordsKind(final String recordType){
		return labels.getString("TAGGED_RECORDS_KIND_" + recordType.toUpperCase(Locale.ROOT));
	}

	public String conflictsTitle(){
		return labels.getString("CONFLICTS_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: all the conflicts, the ones not settled, the ones settled). */
	public String conflictsIntro(){
		return labels.getString("CONFLICTS_INTRO");
	}

	public String conflictsNothing(){
		return labels.getString("CONFLICTS_NOTHING");
	}

	public String conflictsNoneShown(){
		return labels.getString("CONFLICTS_NONE_SHOWN");
	}

	/** @param filterName	{@code UNRESOLVED} or {@code RESOLVED}. */
	public String conflictsFilter(final String filterName){
		return labels.getString("CONFLICTS_FILTER_" + filterName);
	}

	/** @return	The sentence of the filter by record type (a {@link String#format} pattern: the type, see {@link #conflictsType}). */
	public String conflictsFilterType(){
		return labels.getString("CONFLICTS_FILTER_TYPE");
	}

	/** @return	The sentence of the filter by confidence (a {@link String#format} pattern: the confidence, see {@link #conflictsConfidence}). */
	public String conflictsFilterConfidence(){
		return labels.getString("CONFLICTS_FILTER_CONFIDENCE");
	}

	/** @param typeName	The name of a {@code ConflictRecordType} (not {@code ALL}). */
	public String conflictsType(final String typeName){
		return labels.getString("CONFLICTS_TYPE_" + typeName);
	}

	public String conflictsNotes(){
		return labels.getString("CONFLICTS_NOTES");
	}

	/** @param columnKey	{@code KIND}, {@code TOTAL}, {@code UNRESOLVED}, {@code RESOLVED}, {@code RECORD}, {@code ASSERTION}, {@code PLACE}, {@code CONFIDENCE}, {@code SUPPORTING}, {@code CONTRADICTING} or {@code PREFERRED}. */
	public String conflictsColumn(final String columnKey){
		return labels.getString("CONFLICTS_COLUMN_" + columnKey);
	}

	public String conflictsPreferred(){
		return labels.getString("CONFLICTS_PREFERRED");
	}

	public String conflictsStatusLabel(){
		return labels.getString("CONFLICTS_STATUS_LABEL");
	}

	public String conflictsConclusion(){
		return labels.getString("CONFLICTS_CONCLUSION_LABEL");
	}

	public String conflictsQuestions(){
		return labels.getString("CONFLICTS_QUESTIONS_LABEL");
	}

	/** @param kindKey	The key of a kind of conflict (see {@code ReportConflict.Kind#key()}). */
	public String conflictsKind(final String kindKey){
		return labels.getString("CONFLICTS_KIND_" + kindKey.toUpperCase(Locale.ROOT));
	}

	/** @param problemKey	The key of a problem (see {@code ReportConflict.Problem#key()}). */
	public String conflictsProblem(final String problemKey){
		return labels.getString("CONFLICTS_PROBLEM_" + problemKey.toUpperCase(Locale.ROOT));
	}

	/** @param statusKey	{@code UNRESOLVED}, {@code PREFERRED} or a proof status in capitals. */
	public String conflictsStatus(final String statusKey){
		return labels.getString("CONFLICTS_STATUS_" + statusKey);
	}

	/** @param confidenceKey	{@code LOW}, {@code MEDIUM}, {@code HIGH}, {@code NOT_STATED} or {@code ALL}. */
	public String conflictsConfidence(final String confidenceKey){
		return labels.getString("CONFLICTS_CONFIDENCE_" + confidenceKey);
	}

	public String hypothesesTitle(){
		return labels.getString("HYPOTHESES_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: all, open, accepted, rejected). */
	public String hypothesesIntro(){
		return labels.getString("HYPOTHESES_INTRO");
	}

	public String hypothesesNothing(){
		return labels.getString("HYPOTHESES_NOTHING");
	}

	public String hypothesesNoneShown(){
		return labels.getString("HYPOTHESES_NONE_SHOWN");
	}

	/** @param filterName	The name of a {@code HypothesisFilter} (not {@code ALL}). */
	public String hypothesesFilter(final String filterName){
		return labels.getString("HYPOTHESES_FILTER_" + filterName);
	}

	/** @return	The sentence of the filter by kind of record (a {@link String#format} pattern: see {@link #hypothesesType}). */
	public String hypothesesFilterType(){
		return labels.getString("HYPOTHESES_FILTER_TYPE");
	}

	/** @param typeName	The name of a {@code HypothesisRecordType} (not {@code ALL}). */
	public String hypothesesType(final String typeName){
		return labels.getString("HYPOTHESES_TYPE_" + typeName);
	}

	public String hypothesesNotes(){
		return labels.getString("HYPOTHESES_NOTES");
	}

	/** @param statusName	The name of a {@code HypothesisStatus}: the heading of the group. */
	public String hypothesesStatusHeading(final String statusName){
		return labels.getString("HYPOTHESES_HEADING_" + statusName);
	}

	/** @param statusName	The name of a {@code HypothesisStatus}: where the hypothesis stands. */
	public String hypothesesStatus(final String statusName){
		return labels.getString("HYPOTHESES_STATUS_" + statusName);
	}

	/** @param key	{@code STATUS}, {@code CONFIDENCE}, {@code NOT_STATED}, {@code RATIONALE}, {@code SUPPORTING}, {@code CONTRADICTING}, {@code NONE}, {@code QUESTIONS} or {@code CONCLUSIONS}. */
	public String hypothesesLabel(final String key){
		return labels.getString("HYPOTHESES_LABEL_" + key);
	}

	public String familyGroupTitle(){
		return labels.getString("FAMILY_GROUP_TITLE");
	}

	public String familyGroupParents(){
		return labels.getString("FAMILY_GROUP_PARENTS");
	}

	public String familyGroupMarriage(){
		return labels.getString("FAMILY_GROUP_MARRIAGE");
	}

	public String familyGroupChildren(){
		return labels.getString("FAMILY_GROUP_CHILDREN");
	}

	public String familyGroupNoChildren(){
		return labels.getString("FAMILY_GROUP_NO_CHILDREN");
	}

	/** @return	What stands where the other parent of the children is not known. */
	public String familyGroupNoPartner(){
		return labels.getString("FAMILY_GROUP_NO_PARTNER");
	}

	/** @param columnKey	{@code NAME}, {@code BORN}, {@code BIRTH_PLACE}, {@code DIED} or {@code DEATH_PLACE}. */
	public String familyGroupColumn(final String columnKey){
		return labels.getString("FAMILY_GROUP_COLUMN_" + columnKey);
	}

	public String surnamesTitle(){
		return labels.getString("SURNAMES_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: individuals, surnames, places). */
	public String surnamesIntro(){
		return labels.getString("SURNAMES_INTRO");
	}

	public String surnamesNote(){
		return labels.getString("SURNAMES_NOTE");
	}

	public String surnamesNoPlace(){
		return labels.getString("SURNAMES_NO_PLACE");
	}

	public String surnamesEmpty(){
		return labels.getString("SURNAMES_EMPTY");
	}

	/** @param columnKey	{@code SURNAME}, {@code COUNT}, {@code PERIOD}, {@code UNDATED}, {@code SPELLINGS} or {@code PLACES}. */
	public String surnamesColumn(final String columnKey){
		return labels.getString("SURNAMES_COLUMN_" + columnKey);
	}

	/** @return	The name of a century (a {@link String#format} pattern: the number of the century, in Roman numerals). */
	public String surnamesCentury(){
		return labels.getString("SURNAMES_CENTURY");
	}

	/** @return	The sentence of the level the places are grouped by (a {@link String#format} pattern: {@link #surnamesLevel}). */
	public String surnamesNoteLevel(){
		return labels.getString("SURNAMES_NOTE_LEVEL");
	}

	public String surnamesNoteAllNames(){
		return labels.getString("SURNAMES_NOTE_ALL_NAMES");
	}

	/** @param levelName	The name of a {@code SurnamePlaceLevel} (not {@code PLACE}). */
	public String surnamesLevel(final String levelName){
		return labels.getString("SURNAMES_LEVEL_" + levelName);
	}

	public String surnamesSimilarTitle(){
		return labels.getString("SURNAMES_SIMILAR_TITLE");
	}

	/** @return	The sentence that opens the section (a {@link String#format} pattern: the number of groups). */
	public String surnamesSimilarIntro(){
		return labels.getString("SURNAMES_SIMILAR_INTRO");
	}

	public String surnamesSimilarNone(){
		return labels.getString("SURNAMES_SIMILAR_NONE");
	}

	public String endOfLineTitle(){
		return labels.getString("END_OF_LINE_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: ancestors at the end of the line, ancestors, generations). */
	public String endOfLineIntro(){
		return labels.getString("END_OF_LINE_INTRO");
	}

	/** @return	The sentence for an individual whose ancestors all have parents (a {@link String#format} pattern: ancestors). */
	public String endOfLineNone(){
		return labels.getString("END_OF_LINE_NONE");
	}

	public String endOfLineNoAncestors(){
		return labels.getString("END_OF_LINE_NO_ANCESTORS");
	}

	public String endOfLineNote(){
		return labels.getString("END_OF_LINE_NOTE");
	}

	/** @param columnKey	{@code GENERATION}, {@code NAME}, {@code BORN}, {@code BIRTH_PLACE}, {@code DIED} or {@code DEATH_PLACE}. */
	public String endOfLineColumn(final String columnKey){
		return labels.getString("END_OF_LINE_COLUMN_" + columnKey);
	}

	public String kinshipTitle(){
		return labels.getString("KINSHIP_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: relatives, degrees, generations). */
	public String kinshipIntro(){
		return labels.getString("KINSHIP_INTRO");
	}

	public String kinshipNone(){
		return labels.getString("KINSHIP_NONE");
	}

	/** @return	The heading of a degree (a {@link String#format} pattern: the degree). */
	public String kinshipDegree(){
		return labels.getString("KINSHIP_DEGREE");
	}

	public String kinshipNote(){
		return labels.getString("KINSHIP_NOTE");
	}

	/** @param columnKey	{@code KINSHIP}, {@code NAME}, {@code BORN} or {@code DIED}. */
	public String kinshipColumn(final String columnKey){
		return labels.getString("KINSHIP_COLUMN_" + columnKey);
	}

	public String pedigreeCollapseTitle(){
		return labels.getString("PEDIGREE_COLLAPSE_TITLE");
	}

	/**
	 * @return	The sentence that opens the report (a {@link String#format} pattern: known places, possible places, generations, different
	 * 	individuals, pedigree collapse).
	 */
	public String pedigreeCollapseIntro(){
		return labels.getString("PEDIGREE_COLLAPSE_INTRO");
	}

	public String pedigreeCollapseNoAncestors(){
		return labels.getString("PEDIGREE_COLLAPSE_NO_ANCESTORS");
	}

	public String pedigreeCollapseNone(){
		return labels.getString("PEDIGREE_COLLAPSE_NONE");
	}

	public String pedigreeCollapseRepeatedTitle(){
		return labels.getString("PEDIGREE_COLLAPSE_REPEATED_TITLE");
	}

	/** @return	The sentence that opens the list of the repeated ancestors (a {@link String#format} pattern: how many). */
	public String pedigreeCollapseRepeatedIntro(){
		return labels.getString("PEDIGREE_COLLAPSE_REPEATED_INTRO");
	}

	/** @return	The sentence that tells the list is cut (a {@link String#format} pattern: how many are listed). */
	public String pedigreeCollapseRepeatedMore(){
		return labels.getString("PEDIGREE_COLLAPSE_REPEATED_MORE");
	}

	public String pedigreeCollapseNote(){
		return labels.getString("PEDIGREE_COLLAPSE_NOTE");
	}

	/** @param columnKey	{@code GENERATION}, {@code POSSIBLE}, {@code KNOWN}, {@code KNOWN_PERCENT}, {@code DIFFERENT}, {@code COLLAPSE}, {@code NAME}, {@code TIMES} or {@code GENERATIONS}. */
	public String pedigreeCollapseColumn(final String columnKey){
		return labels.getString("PEDIGREE_COLLAPSE_COLUMN_" + columnKey);
	}

	public String databaseSummaryTitle(){
		return labels.getString("DATABASE_SUMMARY_TITLE");
	}

	/** @return	The sentence that opens the report (a {@link String#format} pattern: records, kinds, individuals). */
	public String databaseSummaryIntro(){
		return labels.getString("DATABASE_SUMMARY_INTRO");
	}

	public String databaseSummaryEmpty(){
		return labels.getString("DATABASE_SUMMARY_EMPTY");
	}

	public String databaseSummaryKinds(){
		return labels.getString("DATABASE_SUMMARY_KINDS");
	}

	public String databaseSummaryCompleteness(){
		return labels.getString("DATABASE_SUMMARY_COMPLETENESS");
	}

	public String databaseSummaryPeriod(){
		return labels.getString("DATABASE_SUMMARY_PERIOD");
	}

	public String databaseSummaryNote(){
		return labels.getString("DATABASE_SUMMARY_NOTE");
	}

	/** @param columnKey	{@code KIND}, {@code RECORDS}, {@code MEASURE}, {@code INDIVIDUALS}, {@code SHARE} or {@code VALUE}. */
	public String databaseSummaryColumn(final String columnKey){
		return labels.getString("DATABASE_SUMMARY_COLUMN_" + columnKey);
	}

	/** @param measureKey	{@code SURNAME}, {@code SEX}, {@code BIRTH_DATE}, {@code BIRTH_PLACE}, {@code DEATH_DATE}, {@code PARENTS}, {@code SOURCE}, {@code EVENTS_DATED}, {@code FIRST_EVENT}, {@code LAST_EVENT}, {@code FIRST_BIRTH} or {@code LAST_BIRTH}. */
	public String databaseSummaryMeasure(final String measureKey){
		return labels.getString("DATABASE_SUMMARY_MEASURE_" + measureKey);
	}

	/** @param kindKey	The key of a kind of finding (see {@code ToVerifyFinder.Kind#key()}). */
	public String toVerifyKind(final String kindKey){
		return labels.getString("TO_VERIFY_" + kindKey);
	}

	/** @return	The title of what cites a name ("Sources of the name: Antonia Pastrelo"): the records where the name is written. */
	public String nameSources(){
		return labels.getString("NAME_SOURCES");
	}

	/** @return	The title of the chapter that opens a report, after the front matter: it is not the title of the report, that is already above. */
	public String overview(){
		return labels.getString("OVERVIEW");
	}

	/** @param count	How many individuals the report covers (the message is plural-aware: it is not a {@link String#format} pattern). */
	public String multiPersonCover(final int count){
		return labels.pluralKey("MULTI_PERSON_COVER", count);
	}

	/** @return	The header of the column with the years of birth and death of an individual. */
	public String columnYears(){
		return labels.getString("COLUMN_YEARS");
	}

	/** @return	The header of the column with the number of a person in the numbering of the report (Ahnentafel, d'Aboville). */
	public String columnNumber(){
		return labels.getString("COLUMN_NUMBER");
	}

	/** @return	The word that tells a section is an appendix of the report ("Appendix"). */
	public String appendix(){
		return labels.getString("APPENDIX");
	}

	public String columnIndividual(){
		return labels.getString("COLUMN_INDIVIDUAL");
	}

	public String columnGroup(){
		return labels.getString("COLUMN_GROUP");
	}

	public String indexOfSources(){
		return labels.getString("INDEX_OF_SOURCES");
	}

	public String indexOfRepositories(){
		return labels.getString("INDEX_OF_REPOSITORIES");
	}

	/** @return	The heading of the preface of a book. */
	public String preface(){
		return labels.getString("PREFACE");
	}

	/** @return	The title of a book that has none. */
	public String bookTitle(){
		return labels.getString("BOOK_TITLE");
	}

}
