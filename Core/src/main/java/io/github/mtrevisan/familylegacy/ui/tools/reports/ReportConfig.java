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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

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
	CollateralScope collateralScope,
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
			CollateralScope.NONE,
			true,   // notes
			true,   // sources
			true,   // media
			false,  // evidence
			false,  // contextAndResearch
			false,  // culturalNormIds
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
