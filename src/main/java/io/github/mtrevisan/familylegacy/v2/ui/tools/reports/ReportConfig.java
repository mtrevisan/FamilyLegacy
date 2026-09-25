package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.util.Objects;


/**
 * Report generation options.
 */
public record ReportConfig(
	// Language
	ReportLanguage language,
	// Privacy
	PrivacyPolicy privacyPolicy,
	boolean showPrivacyDetails,
	boolean audit,
	// Presentation
	boolean header,
	// Narrative content (individual)
	boolean introduction,
	boolean paternalAncestry,
	boolean maternalAncestry,
	boolean descendants,
	boolean directRelations,
	// Additional sections
	boolean indirectRelations,
	boolean notes,
	boolean sources,
	boolean media,
	boolean evidence,
	boolean contextAndResearch,
	boolean culturalNorms,
	boolean groups,
	boolean groupMembers,
	boolean groupSubgroups,
	boolean groupAttributes,
	boolean historicEvents,
	boolean repositories,
	boolean eventFullParticipants,
	boolean dateProvenance,
	boolean coverageReport,
	// Indexes
	boolean indexIndividuals,
	boolean indexPlaces,
	// Style
	boolean includeDescriptions,
	boolean includePictures,
	boolean timeline
){

	public ReportConfig{
		Objects.requireNonNull(language, "language");
		Objects.requireNonNull(privacyPolicy, "privacyPolicy");
	}


	/** Convenience accessor for callers that only need the boolean test. */
	public boolean respectPrivacy(){
		return privacyPolicy != PrivacyPolicy.SHOW_ALL;
	}


	public static ReportConfig defaults(){
		return new ReportConfig(
			ReportLanguage.ENGLISH,
			PrivacyPolicy.HIDE_CONFIDENTIAL,
			false,  // showPrivacyDetails
			false,  // audit
			true,   // header
			true,   // introduction
			true,   // paternalAncestry
			true,   // maternalAncestry
			true,   // descendants
			true,   // directRelations
			false,  // indirectRelations
			true,   // notes
			true,   // sources
			true,   // media
			false,  // evidence
			false,  // contextAndResearch
			false,  // culturalNorms
			true,   // groups
			true,   // groupMembers
			true,   // groupSubgroups
			true,   // groupAttributes
			true,   // historicEvents
			true,   // repositories
			false,  // eventFullParticipants
			false,  // dateProvenance
			false,  // coverageReport
			true,   // indexIndividuals
			true,   // indexPlaces
			true,   // includeDescriptions
			false,  // includePictures
			false   // includeTimeline
		);
	}

}
