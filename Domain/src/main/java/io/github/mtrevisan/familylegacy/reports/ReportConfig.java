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

import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;

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
	/** How the persons of the Family Book are put in order. */
	PersonOrder personOrder,
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
	boolean eventFullParticipants,
	boolean coverageReport,
	boolean timeline,
	// Indexes
	boolean indexIndividuals,
	boolean indexPlaces,
	// Style
	boolean includeDescriptions,
	boolean includePictures
){

	public ReportConfig{
		Objects.requireNonNull(language, "language");
		Objects.requireNonNull(privacyPolicy, "privacyPolicy");
		Objects.requireNonNull(personOrder, "personOrder");
		generations = Math.max(0, generations);
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
	 * @param value	Its new value: a {@code Boolean}, an {@code Integer} (the generations) or a {@link CollateralScope}.
	 * @return	A copy of this configuration with the option changed.
	 */
	public ReportConfig with(final ReportOption option, final Object value){
		final RecordComponent[] components = ReportConfig.class.getRecordComponents();
		final Object[] arguments = new Object[components.length];
		final Class<?>[] types = new Class<?>[components.length];
		try{
			for(int i = 0; i < components.length; i ++){
				types[i] = components[i].getType();
				arguments[i] = (components[i].getName().equals(option.componentName())? value: components[i].getAccessor().invoke(this));
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
	 * @param value	Whether every option is on or off ({@code collateralScope} is the widest or none, {@code generations} is all, {@code personOrder} is by birth date or alphabetical).
	 * @param language	The language.
	 * @param privacyPolicy	The privacy policy.
	 * @return	A configuration with every option the same.
	 */
	public static ReportConfig uniform(final boolean value, final ReportLanguage language, final PrivacyPolicy privacyPolicy){
		final CollateralScope[] scopes = CollateralScope.values();
		return new ReportConfig(language, privacyPolicy, value, value, value, value, value, value, value, value, value,
			(value? scopes[scopes.length - 1]: CollateralScope.NONE), 0, (value? PersonOrder.BIRTH_DATE: PersonOrder.ALPHABETICAL), value, value,
			value, value, value, value, value, value, value,
			value, value, value, value, value, value, value, value, value, value, value);
	}

	public static ReportConfig defaults(){
		return new ReportConfig(
			ReportLanguage.ENGLISH,
			PrivacyPolicy.HIDE_CONFIDENTIAL,
			false,
			false,
			true,
			true,
			true,
			true,
			true,
			true,
			false,
			CollateralScope.NONE,
			0,
			PersonOrder.ALPHABETICAL,
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
			true,
			true,
			true,
			true
		);
	}

}
