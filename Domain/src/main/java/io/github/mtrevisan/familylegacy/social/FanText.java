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
package io.github.mtrevisan.familylegacy.social;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * The words of a FAN table: roles, periods and the text of the suggestions, shared by the dossier and the Research Assistant so that
 * both say the same.
 */
public final class FanText{

	private FanText(){}


	/** @return the label of a role of an event participation (a role the program does not know is shown as it is) */
	public static String roleLabel(final String role){
		if(StringUtils.isEmpty(role))
			return I18N.t("text.fan.role.unspecified");

		return I18N.t("enum.event_participation.role." + role, role);
	}

	/** @return the roles with how many times each one occurs, such as {@code witness ×2, midwife} */
	public static String roles(final Map<String, Integer> roles){
		final List<String> parts = new ArrayList<>(roles.size());
		roles.entrySet()
			.stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
			.forEach(entry -> parts.add(roleLabel(entry.getKey()) + (entry.getValue() > 1? " ×" + entry.getValue(): StringUtils.EMPTY)));
		return String.join(", ", parts);
	}

	/** @return the roles of the subject and of the other person, such as {@code principal / witness ×2} */
	public static String rolePair(final FanEntry entry){
		return I18N.tf("text.fan.roles", roles(entry.ownRoles()), roles(entry.otherRoles()));
	}

	/** @return the years of the shared events, such as {@code 1890-1905} or {@code 1890}; empty if none is dated */
	public static String period(final FanEntry entry){
		if(entry.firstYear() == null)
			return StringUtils.EMPTY;

		return (entry.firstYear().equals(entry.lastYear())
			? String.valueOf(entry.firstYear())
			: I18N.tf("text.fan.period.range", String.valueOf(entry.firstYear()), String.valueOf(entry.lastYear())));
	}

	/** @return the places of the shared events, such as {@code Treviso; Preganziol} */
	public static String places(final FanEntry entry){
		return String.join("; ", entry.places());
	}


	/** @return the key that names the kind of a suggestion, the same for the dossier and the Research Assistant */
	public static String kindOf(final FanSuggestion suggestion){
		return switch(suggestion.kind()){
			case POSSIBLE_COUPLE -> "fan.couple";
			case RECURRING_WITHOUT_KINSHIP -> "fan.recurring";
		};
	}

	/**
	 * @param suggestion the suggestion
	 * @param subject    the name of the subject of the table
	 * @param names      gives the name of an individual from its identifier
	 * @return what to look for, in a short sentence
	 */
	public static String title(final FanSuggestion suggestion, final String subject, final Function<String, String> names){
		return switch(suggestion.kind()){
			case POSSIBLE_COUPLE -> I18N.tf("fan.couple.title", names.apply(suggestion.personIds().get(0)),
				names.apply(suggestion.personIds().get(1)));
			case RECURRING_WITHOUT_KINSHIP -> I18N.tf("fan.recurring.title", names.apply(suggestion.personIds().get(0)), subject);
		};
	}

	/** @return why the suggestion is made, and what to do about it */
	public static String explanation(final FanSuggestion suggestion, final String subject, final Function<String, String> names){
		return switch(suggestion.kind()){
			case POSSIBLE_COUPLE -> I18N.tf("fan.couple.explanation", names.apply(suggestion.personIds().get(0)),
				names.apply(suggestion.personIds().get(1)), suggestion.eventIds().size(), subject);
			case RECURRING_WITHOUT_KINSHIP -> I18N.tf("fan.recurring.explanation", names.apply(suggestion.personIds().get(0)), subject,
				suggestion.eventIds().size());
		};
	}

	/**
	 * @param entries the people of a FAN table
	 * @param limit   the most people to name
	 * @return the people that are not relatives of the subject, the strongest first, to be searched together with him: those that
	 * had a role other than principal in his events (witnesses, godparents, midwives...)
	 */
	public static List<FanEntry> toSearchTogether(final List<FanEntry> entries, final int limit){
		final List<FanEntry> result = new ArrayList<>();
		for(final FanEntry entry : entries)
			if(!entry.kin() && entry.otherRoles().keySet().stream().anyMatch(role -> !role.isEmpty() && !EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(role))){
				result.add(entry);
				if(result.size() == limit)
					break;
			}
		return result;
	}

}
