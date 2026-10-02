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
package io.github.mtrevisan.familylegacy.ui.tools.reports.i18n;

import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportLabels;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;


/**
 * Handles localized narrative sentence formatting for individual biographies
 * and group histories.
 */
public final class NarrativeFormatter{

	private final ReportLabels labels;

	public NarrativeFormatter(final ReportLabels labels){
		this.labels = labels;
	}

	/* ======================================================================
	 *                          Individual Biography
	 * ====================================================================== */

	public String introductionBody(){
		return labels.getString("INTRODUCTION_BODY");
	}

	public String birth(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_BIRTH_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String birthUnknown(final String name){
		return String.format(labels.getString("NARRATIVE_BIRTH_UNKNOWN"), name);
	}

	public String marriage(final String name, final String spouse, final String date, final String place){
		if(spouse != null)
			return String.format(labels.getString("NARRATIVE_MARRIAGE_BASE"), name, spouse, dateFrag(date), placeFrag(place));
		return String.format(labels.getString("NARRATIVE_MARRIAGE_NOSPOUSE_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String divorce(final String name, final String spouse, final String date){
		return String.format(labels.getString("NARRATIVE_DIVORCE"), name, spouse, dateFrag(date));
	}

	public String childrenGroup(final String name, final int count, final String groupLabel, final String names){
		return labels.pluralKey("NARRATIVE_CHILDREN_GROUP_NN", count, name, groupLabel, names);
	}

	public String childrenGroupWith(final String name, final String otherParent, final int count, final String groupLabel, final String names){
		return labels.pluralKey("NARRATIVE_CHILDREN_GROUP_WITH_NN", count, name, otherParent, groupLabel, names);
	}

	public String occupation(final String name, final String occupation){
		return String.format(labels.getString("NARRATIVE_OCCUPATION"), name, occupation);
	}

	public String characteristic(final String name, final String value){
		return String.format(labels.getString("NARRATIVE_CHARACTERISTIC"), name, value);
	}

	public String attribute(final String name, final String label, final String value){
		return String.format(labels.getString("NARRATIVE_ATTRIBUTE"), name, label, value);
	}

	public String residence(final String name, final String place, final String from, final String to){
		return String.format(labels.getString("NARRATIVE_RESIDENCE_BASE"), name, place, periodFrag(from, to));
	}

	public String move(final String name, final String place, final String from, final String to){
		return String.format(labels.getString("NARRATIVE_MOVE_BASE"), name, place, periodFrag(from, to));
	}

	public String death(final String name, final String date, final String place, final String cause){
		return String.format(labels.getString("NARRATIVE_DEATH_BASE"), name, causeFrag(cause), dateFrag(date), placeFrag(place));
	}

	public String title(final String name, final String title){
		return String.format(labels.getString("NARRATIVE_TITLE_BASE"), name, title);
	}

	public String baptism(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_BAPTISM_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String emigration(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_EMIGRATION_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String immigration(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_IMMIGRATION_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String burial(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_BURIAL_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String cremation(final String name, final String date, final String place){
		return String.format(labels.getString("NARRATIVE_CREMATION_BASE"), name, dateFrag(date), placeFrag(place));
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
		return labels.pluralKey(base, count);
	}

	/* ======================================================================
	 *                          Group Narrative
	 * ====================================================================== */

	public String groupIntroductionBody(){
		return labels.getString("GROUP_INTRODUCTION_BODY");
	}

	public String groupLifespan(){
		return labels.getString("GROUP_LIFESPAN");
	}

	public String groupLifeOf(){
		return labels.getString("GROUP_LIFE_OF");
	}

	public String groupData(){
		return labels.getString("GROUP_DATA");
	}

	public String groupMembers(){
		return labels.getString("GROUP_MEMBERS");
	}

	public String groupNarrativeFounding(final String name, final String date, final String place){
		return String.format(labels.getString("GROUP_NARRATIVE_FOUNDING_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String groupNarrativeFoundingUnknown(final String name){
		return String.format(labels.getString("GROUP_NARRATIVE_FOUNDING_UNKNOWN"), name);
	}

	public String groupNarrativeResidence(final String name, final String place){
		return String.format(labels.getString("GROUP_NARRATIVE_RESIDENCE"), name, place);
	}

	public String groupNarrativeDissolution(final String name, final String date, final String place){
		return String.format(labels.getString("GROUP_NARRATIVE_DISSOLUTION_BASE"), name, dateFrag(date), placeFrag(place));
	}

	public String groupCounts(final int members, final int subgroups, final int attributes){
		return labels.pluralKey("GROUP_COUNTS", members, members, subgroups, attributes);
	}

	/* ======================================================================
	 *                          Fragment Helpers
	 * ====================================================================== */

	/**
	 * Maps a relationship type to the base key of its plural pattern.
	 * Unknown types fall back to the generic {@code REL_CHILD}.
	 */
	private static String childTypeKeyBase(final String type){
		if(type == null)
			return "REL_CHILD";
		return switch(type.toLowerCase(Locale.ROOT)){
			case RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD -> "REL_BIOLOGICAL_CHILD";
			case RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD -> "REL_ADOPTIVE_CHILD";
			case "foster_child" -> "REL_FOSTER_CHILD";
			case "guarded_child" -> "REL_GUARDED_CHILD";
			case "step_child" -> "REL_STEP_CHILD";
			default -> "REL_CHILD";
		};
	}

	private String dateFrag(final String date){
		return (date != null? String.format(labels.getString("DATE_FRAGMENT"), date): StringUtils.EMPTY);
	}

	private String placeFrag(final String place){
		return (place != null? String.format(labels.getString("PLACE_FRAGMENT"), place): StringUtils.EMPTY);
	}

	private String causeFrag(final String cause){
		return (cause != null? String.format(labels.getString("CAUSE_FRAGMENT"), cause): StringUtils.EMPTY);
	}

	private String periodFrag(final String from, final String to){
		if(from != null && to != null)
			return String.format(labels.getString("PERIOD_FROM_TO_FRAGMENT"), from, to);
		if(from != null)
			return String.format(labels.getString("PERIOD_FROM_FRAGMENT"), from);
		if(to != null)
			return String.format(labels.getString("PERIOD_TO_FRAGMENT"), to);
		return StringUtils.EMPTY;
	}

}
