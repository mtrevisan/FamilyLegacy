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

import io.github.mtrevisan.familylegacy.privacy.LivingPrivacy;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportDateFormat;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;
import io.github.mtrevisan.reports.ImageAlignment;

import java.lang.reflect.RecordComponent;
import java.util.Objects;


/**
 * Report generation options.
 */
public record ReportConfig(
	// Language
	ReportLanguage language,
	// Privacy
	PrivacyPolicy privacyPolicy,
	/** What is done with the individuals that may be living (it has no effect if the privacy policy shows everything). */
	LivingPrivacy livingPrivacy,
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
	/** How many generations the ancestor and descendant reports follow, the root not counted; 0 for all of them. */
	int generations,
	/** The generation the ancestor and descendant reports start from, the root being 0 (the earlier ones are left out); with the same value of generations it is a single generation. */
	int firstGeneration,
	/** Whether the generations of the ancestor and descendant reports go from the nearest to the root or from the farthest. */
	GenerationOrder generationOrder,
	/** How the persons of the Family Book are put in order. */
	PersonOrder personOrder,
	/** How the dates are written. */
	ReportDateFormat dateFormat,
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
	/** Whether the sources a cited source is derived from (its originals) are listed too, even if nothing cites them. */
	boolean includeOriginalSources,
	/** Whether the sources are grouped by their kind (census, church register...) instead of one alphabetical list. */
	boolean groupSourcesByKind,
	boolean eventFullParticipants,
	boolean coverageReport,
	boolean timeline,
	// Indexes
	boolean indexIndividuals,
	boolean indexPlaces,
	// Style
	boolean includeDescriptions,
	boolean includePictures,
	// Narrative style
	/** How the persons of an ancestor or descendant report are numbered. */
	NumberingSystem numbering,
	/** Whether the direct ancestors of the subject are in bold wherever they are mentioned. */
	boolean boldDirectAncestors,
	/** Whether a person that has a number in the report shows it in brackets wherever the person is mentioned in another chapter. */
	boolean crossReferences,
	/** How the given names of the persons are written. */
	GivenNameStyle givenNames,
	/** What is written for a person with no name; empty to leave what the file has (the identifier). */
	String unknownName,
	/** Whether the index of the individuals has the years of birth and death. */
	boolean indexYears,
	// Placement of notes, sources and media
	TextPlacement notesPlacement,
	/** Where the text a source cites (the extract, its note, its crop) is written. */
	TextPlacement sourcesPlacement,
	/** A text longer than this number of lines is long, for the placements that depend on it. */
	int longTextLines,
	/** Whether only the best sources of what is told are cited (the others are left out). */
	boolean principalSourcesOnly,
	/** Whether the citations tell the locator (the page, the folio...) of the source. */
	boolean sourceLocator,
	MediaPlacement mediaPlacement,
	/** How many images are on a page of the media, 0 if the layout is left to the renderer. */
	int imagesPerPage,
	/** The largest image embedded, in megapixels (a larger one is reduced); 0 for the images as they are. */
	int maxImageMegapixels,
	/** The longest side of the preferred image of a person, in pixels; 0 for the image as it is. */
	int thumbnailSize,
	PortraitMode portraitMode,
	/** Whether the caption of the preferred image of a person is the name of the person. */
	boolean portraitNameTitle,
	/** Where the preferred image of a person is put in the narrative. */
	ImageAlignment portraitAlignment,
	/** Which conflicts the Conflicts report lists. */
	ConflictFilter conflictFilter,
	/** Which conflicts the Conflicts report lists, by the confidence given to the competing assertions. */
	ConflictConfidence conflictConfidence,
	/** Which conflicts the Conflicts report lists, by the kind of record in conflict. */
	ConflictRecordType conflictRecordType,
	/** Which hypotheses the Identity Hypotheses report lists, by where they stand. */
	HypothesisFilter hypothesisFilter,
	/** Which hypotheses the Identity Hypotheses report lists, by the kind of record that may be a duplicate. */
	HypothesisRecordType hypothesisRecordType
){

	public ReportConfig{
		Objects.requireNonNull(language, "language");
		Objects.requireNonNull(privacyPolicy, "privacyPolicy");
		Objects.requireNonNull(livingPrivacy, "livingPrivacy");
		Objects.requireNonNull(personOrder, "personOrder");
		Objects.requireNonNull(dateFormat, "dateFormat");
		Objects.requireNonNull(numbering, "numbering");
		Objects.requireNonNull(givenNames, "givenNames");
		Objects.requireNonNull(notesPlacement, "notesPlacement");
		Objects.requireNonNull(sourcesPlacement, "sourcesPlacement");
		Objects.requireNonNull(mediaPlacement, "mediaPlacement");
		Objects.requireNonNull(portraitMode, "portraitMode");
		Objects.requireNonNull(generationOrder, "generationOrder");
		Objects.requireNonNull(portraitAlignment, "portraitAlignment");
		Objects.requireNonNull(conflictFilter, "conflictFilter");
		Objects.requireNonNull(conflictConfidence, "conflictConfidence");
		Objects.requireNonNull(conflictRecordType, "conflictRecordType");
		Objects.requireNonNull(hypothesisFilter, "hypothesisFilter");
		Objects.requireNonNull(hypothesisRecordType, "hypothesisRecordType");
		generations = Math.max(0, generations);
		firstGeneration = Math.max(0, firstGeneration);
		unknownName = (unknownName != null? unknownName.strip(): "");
		longTextLines = Math.max(0, longTextLines);
		imagesPerPage = Math.max(0, imagesPerPage);
		maxImageMegapixels = Math.max(0, maxImageMegapixels);
		thumbnailSize = Math.max(0, thumbnailSize);
	}


	/** Convenience accessor for callers that only need the boolean test. */
	public boolean respectPrivacy(){
		return privacyPolicy != PrivacyPolicy.SHOW_ALL;
	}


	/** @return	A copy of this configuration with the index of the individuals on or off. */
	public ReportConfig withIndexIndividuals(final boolean value){
		return with(ReportOption.INDEX_INDIVIDUALS, value);
	}

	/**
	 * @param option	The option to change.
	 * @param value	Its new value: a {@code Boolean}, an {@code Integer} (the generations) or a constant of the enumeration the option is.
	 * @return	A copy of this configuration with the option changed.
	 */
	public ReportConfig with(final ReportOption option, final Object value){
		return withComponent(option.componentName(), value);
	}

	/** @return	A copy of this configuration with what is done with the individuals that may be living changed. */
	public ReportConfig withLivingPrivacy(final LivingPrivacy value){
		return withComponent("livingPrivacy", value);
	}

	private ReportConfig withComponent(final String name, final Object value){
		final RecordComponent[] components = ReportConfig.class.getRecordComponents();
		final Object[] arguments = new Object[components.length];
		final Class<?>[] types = new Class<?>[components.length];
		try{
			for(int i = 0; i < components.length; i ++){
				types[i] = components[i].getType();
				arguments[i] = (components[i].getName().equals(name)? value: components[i].getAccessor().invoke(this));
			}
			//the canonical constructor of a record always exists
			return ReportConfig.class.getDeclaredConstructor(types)
				.newInstance(arguments);
		}
		catch(final ReflectiveOperationException e){
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param value	Whether every option is on or off ({@code collateralScope} is the widest or none, {@code generations} is all and {@code firstGeneration} the subject, {@code personOrder}
	 * 	is by birth date or alphabetical; the other options that are not a flag are the one that changes the report, or the one that leaves it as it is).
	 * @param language	The language.
	 * @param privacyPolicy	The privacy policy.
	 * @return	A configuration with every option the same.
	 */
	public static ReportConfig uniform(final boolean value, final ReportLanguage language, final PrivacyPolicy privacyPolicy){
		final CollateralScope[] scopes = CollateralScope.values();
		return new ReportConfig(language, privacyPolicy, LivingPrivacy.NAME_AND_ROLE, value, value, value, value, value, value, value, value, value,
			(value? scopes[scopes.length - 1]: CollateralScope.NONE), 0, 0, (value? GenerationOrder.FARTHEST_FIRST: GenerationOrder.NEAREST_FIRST),
			(value? PersonOrder.BIRTH_DATE: PersonOrder.ALPHABETICAL), ReportDateFormat.AS_RECORDED, value, value,
			value, value, value, value, value, value, value,
			value, value, value, value, value, value, value, value, value, value, value, value,
			(value? NumberingSystem.AUTO: NumberingSystem.NONE), value, value, (value? GivenNameStyle.FIRST_AND_INITIALS: GivenNameStyle.ALL),
			(value? "?": ""), value, (value? TextPlacement.SECTION: TextPlacement.INLINE), (value? TextPlacement.SECTION: TextPlacement.INLINE),
			(value? 3: 0), value, value, MediaPlacement.SECTION, (value? 2: 0), (value? 2: 0), (value? 200: 0),
			(value? PortraitMode.CROP_WITHOUT_ORIGINAL: PortraitMode.WHOLE), value,
			(value? ImageAlignment.WRAP_RIGHT: ImageAlignment.DEFAULT), (value? ConflictFilter.UNRESOLVED: ConflictFilter.ALL),
			(value? ConflictConfidence.LOW: ConflictConfidence.ALL), (value? ConflictRecordType.EVENT: ConflictRecordType.ALL),
			(value? HypothesisFilter.OPEN: HypothesisFilter.ALL), (value? HypothesisRecordType.INDIVIDUAL: HypothesisRecordType.ALL));
	}

	public static ReportConfig defaults(){
		return new ReportConfig(
			ReportLanguage.ENGLISH,
			PrivacyPolicy.HIDE_CONFIDENTIAL,
			LivingPrivacy.NAME_AND_ROLE,
			false,
			false,
			true,
			true,
			true,
			true,
			true,
			false,
			false,
			CollateralScope.NONE,
			0,
			0,
			GenerationOrder.NEAREST_FIRST,
			PersonOrder.ALPHABETICAL,
			ReportDateFormat.AS_RECORDED,
			true,
			true,
			true,
			false,
			false,
			false,
			true,
			true,
			true,
			true,
			true,
			true,
			true,
			false,
			false,
			false,
			false,
			true,
			true,
			true,
			true,
			NumberingSystem.AUTO,
			false,
			false,
			GivenNameStyle.ALL,
			"",
			true,
			TextPlacement.INLINE,
			TextPlacement.INLINE,
			6,
			false,
			true,
			MediaPlacement.SECTION,
			0,
			0,
			0,
			PortraitMode.WHOLE,
			false,
			ImageAlignment.DEFAULT,
			ConflictFilter.ALL,
			ConflictConfidence.ALL,
			ConflictRecordType.ALL,
			HypothesisFilter.ALL,
			HypothesisRecordType.ALL
		);
	}

}
