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
package io.github.mtrevisan.familylegacy.reports;

import java.util.Locale;
import java.util.function.Function;


/** An option of a report that the user can choose: one for each setting of {@link ReportConfig} but the language and the privacy policy. */
public enum ReportOption{

	SHOW_PRIVACY_DETAILS(ReportConfig::showPrivacyDetails),
	AUDIT(ReportConfig::audit),
	HEADER(ReportConfig::header),
	INTRODUCTION(ReportConfig::introduction),
	PATERNAL_ANCESTRY(ReportConfig::paternalAncestry),
	MATERNAL_ANCESTRY(ReportConfig::maternalAncestry),
	DESCENDANTS(ReportConfig::descendants),
	DIRECT_RELATIONS(ReportConfig::directRelations),
	INDIRECT_RELATIONS(ReportConfig::indirectRelations),
	COLLATERAL_SCOPE(ReportConfig::collateralScope),
	GENERATIONS(ReportConfig::generations),
	FIRST_GENERATION(ReportConfig::firstGeneration),
	GENERATION_ORDER(ReportConfig::generationOrder),
	PERSON_ORDER(ReportConfig::personOrder),
	DATE_FORMAT(ReportConfig::dateFormat),
	NOTES(ReportConfig::notes),
	SOURCES(ReportConfig::sources),
	MEDIA(ReportConfig::media),
	EVIDENCE(ReportConfig::evidence),
	CONTEXT_AND_RESEARCH(ReportConfig::contextAndResearch),
	CULTURAL_NORMS(ReportConfig::culturalNorms),
	HISTORIC_EVENTS(ReportConfig::historicEvents),
	REPOSITORIES(ReportConfig::repositories),
	INCLUDE_ORIGINAL_SOURCES(ReportConfig::includeOriginalSources),
	GROUP_SOURCES_BY_KIND(ReportConfig::groupSourcesByKind),
	GROUPS(ReportConfig::groups),
	GROUP_MEMBERS(ReportConfig::groupMembers),
	GROUP_SUBGROUPS(ReportConfig::groupSubgroups),
	GROUP_ATTRIBUTES(ReportConfig::groupAttributes),
	EVENT_FULL_PARTICIPANTS(ReportConfig::eventFullParticipants),
	COVERAGE_REPORT(ReportConfig::coverageReport),
	TIMELINE(ReportConfig::timeline),
	INDEX_INDIVIDUALS(ReportConfig::indexIndividuals),
	INDEX_PLACES(ReportConfig::indexPlaces),
	INCLUDE_DESCRIPTIONS(ReportConfig::includeDescriptions),
	INCLUDE_PICTURES(ReportConfig::includePictures),
	NUMBERING(ReportConfig::numbering),
	BOLD_DIRECT_ANCESTORS(ReportConfig::boldDirectAncestors),
	CROSS_REFERENCES(ReportConfig::crossReferences),
	GIVEN_NAMES(ReportConfig::givenNames),
	UNKNOWN_NAME(ReportConfig::unknownName),
	INDEX_YEARS(ReportConfig::indexYears),
	NOTES_PLACEMENT(ReportConfig::notesPlacement),
	SOURCES_PLACEMENT(ReportConfig::sourcesPlacement),
	LONG_TEXT_LINES(ReportConfig::longTextLines),
	PRINCIPAL_SOURCES_ONLY(ReportConfig::principalSourcesOnly),
	SOURCE_LOCATOR(ReportConfig::sourceLocator),
	MEDIA_PLACEMENT(ReportConfig::mediaPlacement),
	IMAGES_PER_PAGE(ReportConfig::imagesPerPage),
	MAX_IMAGE_MEGAPIXELS(ReportConfig::maxImageMegapixels),
	THUMBNAIL_SIZE(ReportConfig::thumbnailSize),
	PORTRAIT_MODE(ReportConfig::portraitMode),
	PORTRAIT_NAME_TITLE(ReportConfig::portraitNameTitle),
	PORTRAIT_ALIGNMENT(ReportConfig::portraitAlignment),
	CONFLICT_FILTER(ReportConfig::conflictFilter),
	CONFLICT_CONFIDENCE(ReportConfig::conflictConfidence),
	CONFLICT_RECORD_TYPE(ReportConfig::conflictRecordType),
	HYPOTHESIS_FILTER(ReportConfig::hypothesisFilter),
	HYPOTHESIS_RECORD_TYPE(ReportConfig::hypothesisRecordType);


	private final Function<ReportConfig, Object> reader;


	ReportOption(final Function<ReportConfig, Object> reader){
		this.reader = reader;
	}

	/**
	 * @param config	A configuration.
	 * @return	The value this option has in the configuration.
	 */
	public Object valueIn(final ReportConfig config){
		return reader.apply(config);
	}

	/** @return	The name of the component of {@link ReportConfig} this option is ({@code PATERNAL_ANCESTRY} is {@code paternalAncestry}). */
	public String componentName(){
		final StringBuilder sb = new StringBuilder();
		boolean upper = false;
		for(final char c : name().toLowerCase(Locale.ROOT).toCharArray()){
			if(c == '_')
				upper = true;
			else{
				sb.append(upper? Character.toUpperCase(c): c);
				upper = false;
			}
		}
		return sb.toString();
	}

}
