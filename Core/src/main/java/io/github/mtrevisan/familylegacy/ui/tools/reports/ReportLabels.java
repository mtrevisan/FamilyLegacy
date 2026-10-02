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

import com.ibm.icu.text.MessageFormat;
import io.github.mtrevisan.familylegacy.ui.tools.reports.i18n.DateLabelProvider;
import io.github.mtrevisan.familylegacy.ui.tools.reports.i18n.NarrativeFormatter;
import io.github.mtrevisan.familylegacy.ui.tools.reports.i18n.SectionLabelProvider;

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
 * Lightweight facade and bundle manager for report internationalization.
 * Delegates localized formatting to specialized providers:
 * {@link SectionLabelProvider}, {@link NarrativeFormatter}, and {@link DateLabelProvider}.
 */
public final class ReportLabels{

	private static final String BUNDLE_BASE = "i18n.report.report_messages";

	private final ReportLanguage language;
	private final ResourceBundle bundle;

	private final SectionLabelProvider sectionLabels;
	private final NarrativeFormatter narrativeFormatter;
	private final DateLabelProvider dateLabels;


	public ReportLabels(final ReportLanguage language){
		this.language = Objects.requireNonNull(language, "language");
		this.bundle = ResourceBundle.getBundle(BUNDLE_BASE, language.locale());

		this.sectionLabels = new SectionLabelProvider(this);
		this.narrativeFormatter = new NarrativeFormatter(this);
		this.dateLabels = new DateLabelProvider(this);
	}


	public ReportLanguage language(){
		return language;
	}

	public SectionLabelProvider sections(){
		return sectionLabels;
	}

	public NarrativeFormatter narrative(){
		return narrativeFormatter;
	}

	public DateLabelProvider dates(){
		return dateLabels;
	}

	public String getString(final String key){
		return bundle.getString(key);
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
	public String pluralKey(final String key, final int count, final Object... extraArgs){
		return plural(getString(key), count, extraArgs);
	}

	/* ======================================================================
	 *                          Backwards Compatibility Delegates
	 * ====================================================================== */

	public String subtitle(){
		return sectionLabels.subtitle();
	}

	public String introduction(){
		return sectionLabels.introduction();
	}

	public String statistics(){
		return sectionLabels.statistics();
	}

	public String paternalAncestry(){
		return sectionLabels.paternalAncestry();
	}

	public String maternalAncestry(){
		return sectionLabels.maternalAncestry();
	}

	public String descendants(){
		return sectionLabels.descendants();
	}

	public String directRelations(){
		return sectionLabels.directRelations();
	}

	public String indirectRelations(){
		return sectionLabels.indirectRelations();
	}

	public String notes(){
		return sectionLabels.notes();
	}

	public String sources(){
		return sectionLabels.sources();
	}

	public String media(){
		return sectionLabels.media();
	}

	public String places(){
		return sectionLabels.places();
	}

	public String documents(){
		return sectionLabels.documents();
	}

	public String indexOfIndividuals(){
		return sectionLabels.indexOfIndividuals();
	}

	public String indexOfPlaces(){
		return sectionLabels.indexOfPlaces();
	}

	public String lifeOf(){
		return sectionLabels.lifeOf();
	}

	public String personalData(){
		return sectionLabels.personalData();
	}

	public String lifeEvents(){
		return sectionLabels.lifeEvents();
	}

	public String attributes(){
		return sectionLabels.attributes();
	}

	public String relationships(){
		return sectionLabels.relationships();
	}

	public String narrativeBirth(final String name, final String date, final String place){
		return narrativeFormatter.birth(name, date, place);
	}

	public String narrativeBirthUnknown(final String name){
		return narrativeFormatter.birthUnknown(name);
	}

	public String narrativeMarriage(final String name, final String spouse, final String date, final String place){
		return narrativeFormatter.marriage(name, spouse, date, place);
	}

	public String narrativeDivorce(final String name, final String spouse, final String date){
		return narrativeFormatter.divorce(name, spouse, date);
	}

	public String narrativeChildrenGroup(final String name, final int count, final String groupLabel, final String names){
		return narrativeFormatter.childrenGroup(name, count, groupLabel, names);
	}

	public String narrativeChildrenGroupWith(final String name, final String otherParent, final int count, final String groupLabel, final String names){
		return narrativeFormatter.childrenGroupWith(name, otherParent, count, groupLabel, names);
	}

	public String narrativeOccupation(final String name, final String occupation){
		return narrativeFormatter.occupation(name, occupation);
	}

	public String narrativeCharacteristic(final String name, final String value){
		return narrativeFormatter.characteristic(name, value);
	}

	public String narrativeAttribute(final String name, final String label, final String value){
		return narrativeFormatter.attribute(name, label, value);
	}

	public String narrativeResidence(final String name, final String place, final String from, final String to){
		return narrativeFormatter.residence(name, place, from, to);
	}

	public String narrativeMove(final String name, final String place, final String from, final String to){
		return narrativeFormatter.move(name, place, from, to);
	}

	public String narrativeDeath(final String name, final String date, final String place, final String cause){
		return narrativeFormatter.death(name, date, place, cause);
	}

	public String narrativeTitle(final String name, final String title){
		return narrativeFormatter.title(name, title);
	}

	public String narrativeBaptism(final String name, final String date, final String place){
		return narrativeFormatter.baptism(name, date, place);
	}

	public String narrativeEmigration(final String name, final String date, final String place){
		return narrativeFormatter.emigration(name, date, place);
	}

	public String narrativeImmigration(final String name, final String date, final String place){
		return narrativeFormatter.immigration(name, date, place);
	}

	public String narrativeBurial(final String name, final String date, final String place){
		return narrativeFormatter.burial(name, date, place);
	}

	public String narrativeCremation(final String name, final String date, final String place){
		return narrativeFormatter.cremation(name, date, place);
	}

	public String childGroupLabel(final String relationshipType, final int count){
		return narrativeFormatter.childGroupLabel(relationshipType, count);
	}

	public String ageYears(final int years){
		return dateLabels.ageYears(years);
	}

	public String marginYears(final int n){
		return dateLabels.marginYears(n);
	}

	public String marginMonths(final int n){
		return dateLabels.marginMonths(n);
	}

	public String marginWeeks(final int n){
		return dateLabels.marginWeeks(n);
	}

	public String marginDays(final int n){
		return dateLabels.marginDays(n);
	}

	public String relationsCount(final int parents, final int spouses, final int children){
		return dateLabels.relationsCount(parents, spouses, children);
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
		public ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
				final ClassLoader loader, final boolean reload) throws IOException, IllegalAccessException,
				InstantiationException{
			if(!"java.properties".equals(format))
				return super.newBundle(baseName, locale, format, loader, reload);

			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");
			try(final InputStream is = loader.getResourceAsStream(resourceName)){
				if(is == null)
					return null;

				try(final Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)){
					return new PropertyResourceBundle(reader);
				}
			}
		}
	}

}
