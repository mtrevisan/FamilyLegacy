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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.i18n;

import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportLabels;


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

	public String agency(){
		return labels.getString("AGENCY");
	}

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
	 *                          Date provenance
	 * ====================================================================== */

	public String dateProvenance(){
		return labels.getString("DATE_PROVENANCE");
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

	public String multiPersonCover(){
		return labels.getString("MULTI_PERSON_COVER");
	}

	public String columnIndividual(){
		return labels.getString("COLUMN_INDIVIDUAL");
	}

}
