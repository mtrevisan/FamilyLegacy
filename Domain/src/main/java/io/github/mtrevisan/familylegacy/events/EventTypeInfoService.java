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
package io.github.mtrevisan.familylegacy.events;

import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * Lookup service over the catalog of {@link EventTypeInfo}: the single source of truth for the behavior of the built-in event types.
 * <p>
 * Validators, editors, reports and the Research Assistant ask here instead of hard-coding event semantics. Every answer about a
 * custom (free text) type is neutral: no expectation, nothing is flagged.
 * <p>
 * All the checks are advisory, they yield warnings (never errors) because the data may be incomplete.
 */
public final class EventTypeInfoService{

	/** An advisory about the number of principals of an event. */
	public enum ParticipantAdvisory{
		/** No principal at all (e.g. a burial without the buried). */
		NO_PRINCIPAL,
		/** Fewer principals than expected (e.g. a marriage with only one). */
		TOO_FEW_PRINCIPALS,
		/** More principals than expected (e.g. a birth with two). */
		TOO_MANY_PRINCIPALS
	}


	private static final String PRINCIPAL = EventParticipationReader.ENUM_ROLE_PRINCIPAL;
	private static final String OFFICIANT = EventParticipationReader.ENUM_ROLE_OFFICIANT;
	private static final String WITNESS = EventParticipationReader.ENUM_ROLE_WITNESS;
	private static final String INFORMANT = EventParticipationReader.ENUM_ROLE_INFORMANT;
	private static final String JUDGE = EventParticipationReader.ENUM_ROLE_JUDGE;
	private static final String COMMANDER = EventParticipationReader.ENUM_ROLE_COMMANDER;
	private static final String EXECUTOR = EventParticipationReader.ENUM_ROLE_EXECUTOR;
	private static final String GRANTOR = EventParticipationReader.ENUM_ROLE_GRANTOR;
	private static final String GRANTEE = EventParticipationReader.ENUM_ROLE_GRANTEE;

	private static final Map<String, EventTypeInfo> CATALOG = new LinkedHashMap<>();
	static{
		// life
		add(EventReader.ENUM_TYPE_BIRTH, 1, false, false, PRINCIPAL, EventParticipationReader.ENUM_ROLE_MIDWIFE, INFORMANT);
		add(EventReader.ENUM_TYPE_ADOPTION, 1, false, false, PRINCIPAL, WITNESS);
		// the death itself is dated by several sources: a second date of it is not 'after the death'
		add(EventReader.ENUM_TYPE_DEATH, 1, true, false, PRINCIPAL, INFORMANT);
		add(EventReader.ENUM_TYPE_CREMATION, 1, true, false, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_BURIAL, 1, true, false, PRINCIPAL, OFFICIANT, INFORMANT);
		add(EventReader.ENUM_TYPE_FUNERAL, 1, true, false, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_EXHUMATION, 1, true, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_REBURIAL, 1, true, false, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_CORONER_REPORT, 1, true, false, PRINCIPAL, INFORMANT);
		add(EventReader.ENUM_TYPE_ILLNESS, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_HOSPITALIZATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_MEDICAL_PROCEDURE, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_NAME_CHANGE, 1, false, false, PRINCIPAL);
		// family
		add(EventReader.ENUM_TYPE_ENGAGEMENT, 2, false, true, PRINCIPAL, WITNESS);
		add(EventReader.ENUM_TYPE_MARRIAGE_BANN, 2, false, true, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_MARRIAGE_CONTRACT, 2, false, true, PRINCIPAL, WITNESS);
		add(EventReader.ENUM_TYPE_MARRIAGE_LICENSE, 2, false, true, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT, 2, false, true, PRINCIPAL, WITNESS);
		add(EventReader.ENUM_TYPE_MARRIAGE, 2, false, true, PRINCIPAL, OFFICIANT, WITNESS);
		add(EventReader.ENUM_TYPE_SEPARATION, 2, false, true, PRINCIPAL);
		add(EventReader.ENUM_TYPE_DIVORCE_FILED, 2, false, true, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_DIVORCE_DECREE, 2, false, true, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_DIVORCE, 2, false, true, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_ANNULMENT, 2, false, true, PRINCIPAL, JUDGE);
		// achievements
		add(EventReader.ENUM_TYPE_EDUCATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_GRADUATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_RETIREMENT, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_MILITARY_INDUCTION, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_MUSTER_ROLL, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_SERVICE, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_AWARD, 1, true, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_RELEASE, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_DISCHARGE, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_RESIGNATION, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_MILITARY_RETIREMENT, 1, false, false, PRINCIPAL, COMMANDER);
		add(EventReader.ENUM_TYPE_PRISON, 1, false, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_PARDON, 1, true, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_JURY_DUTY, 1, false, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_HONOR, 1, true, false, PRINCIPAL, OFFICIANT);
		add(EventReader.ENUM_TYPE_BANKRUPTCY, 1, false, false, PRINCIPAL, JUDGE);
		// national / government
		add(EventReader.ENUM_TYPE_EMIGRATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_IMMIGRATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_NATURALIZATION, 1, false, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_DEPORTATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_INTERNMENT, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_LIBERATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_EMANCIPATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_RELOCATION, 1, false, false, PRINCIPAL);
		add(EventReader.ENUM_TYPE_CENSUS, 1, false, false, PRINCIPAL, INFORMANT);
		// possessions and titles
		add(EventReader.ENUM_TYPE_DEED, 1, true, false, PRINCIPAL, GRANTOR, GRANTEE);
		add(EventReader.ENUM_TYPE_ESCROW, 1, true, false, PRINCIPAL, GRANTOR, GRANTEE);
		add(EventReader.ENUM_TYPE_CHANCERY, 1, true, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_WILL, 1, true, false, PRINCIPAL, WITNESS, EXECUTOR);
		add(EventReader.ENUM_TYPE_PROBATE, 1, true, false, PRINCIPAL, EXECUTOR, JUDGE);
		add(EventReader.ENUM_TYPE_GUARDIANSHIP, 1, false, false, PRINCIPAL, JUDGE);
		add(EventReader.ENUM_TYPE_LAND_PURCHASE, 1, false, false, PRINCIPAL, GRANTOR, GRANTEE);
		add(EventReader.ENUM_TYPE_LAND_SALE, 1, false, false, PRINCIPAL, GRANTOR, GRANTEE);
	}

	private static final Set<String> MARRIAGE_FORMATION_TYPES;
	static{
		final Set<String> types = new LinkedHashSet<>();
		for(final EventTypeInfo info : CATALOG.values())
			if(info.marital() && !ArrayUtils.contains(EventReader.DIVORCE_TYPES, info.type()))
				types.add(info.type());
		MARRIAGE_FORMATION_TYPES = Collections.unmodifiableSet(types);
	}


	private EventTypeInfoService(){}

	private static void add(final String type, final int principals, final boolean allowedAfterDeath, final boolean marital,
			final String... roles){
		CATALOG.put(type, new EventTypeInfo(type, principals, allowedAfterDeath, marital, new LinkedHashSet<>(List.of(roles))));
	}


	/**
	 * @param type	The event type (case and surrounding spaces are ignored).
	 * @return	The info of a built-in type, empty for a custom type or a blank value.
	 */
	public static Optional<EventTypeInfo> find(final String type){
		return (StringUtils.isBlank(type)? Optional.empty(): Optional.ofNullable(CATALOG.get(type.strip().toLowerCase(Locale.ROOT))));
	}

	/** @return	Whether the type is one of the built-in ones. */
	public static boolean isBuiltIn(final String type){
		return find(type).isPresent();
	}

	/** @return	The whole catalog, in the order of {@link EventReader#TYPES}. */
	public static Collection<EventTypeInfo> all(){
		return Collections.unmodifiableCollection(CATALOG.values());
	}

	/** @return	Whether the type is part of the marriage lifecycle (false for a custom type). */
	public static boolean isMarital(final String type){
		return find(type).map(EventTypeInfo::marital).orElse(false);
	}

	/**
	 * @return	Whether the event forms a marriage: the marriage itself and what leads to it (engagement, banns, contract, license,
	 *		settlement), not what dissolves it (false for a custom type).
	 */
	public static boolean isMarriageFormation(final String type){
		return (type != null && MARRIAGE_FORMATION_TYPES.contains(type.strip().toLowerCase(Locale.ROOT)));
	}

	/** @return	The types that form a marriage (see {@link #isMarriageFormation(String)}). */
	public static Set<String> marriageFormationTypes(){
		return MARRIAGE_FORMATION_TYPES;
	}

	/** @return	Whether the event may occur after the death of its principals (true for a custom type: nothing is known, so nothing is flagged). */
	public static boolean isAllowedAfterDeath(final String type){
		return find(type).map(EventTypeInfo::allowedAfterDeath).orElse(true);
	}

	/** @return	The expected number of principals, empty for a custom type. */
	public static Optional<Integer> expectedPrincipals(final String type){
		return find(type).map(EventTypeInfo::principals);
	}

	/** @return	The roles to prepopulate in an editor, in presentation order (empty for a custom type). */
	public static Set<String> defaultRoles(final String type){
		return find(type).map(EventTypeInfo::defaultRoles).orElse(Set.of());
	}

	/**
	 * Advisory check of the number of principals of an event.
	 *
	 * @param type	The event type.
	 * @param principalCount	The number of participants with the principal role.
	 * @return	The advisory, empty if the count looks fine or the type is custom.
	 */
	public static Optional<ParticipantAdvisory> checkPrincipals(final String type, final int principalCount){
		return find(type).flatMap(info -> {
			if(principalCount <= 0)
				return Optional.of(ParticipantAdvisory.NO_PRINCIPAL);
			if(principalCount < info.principals())
				return Optional.of(ParticipantAdvisory.TOO_FEW_PRINCIPALS);
			if(principalCount > info.principals())
				return Optional.of(ParticipantAdvisory.TOO_MANY_PRINCIPALS);
			return Optional.empty();
		});
	}

}
