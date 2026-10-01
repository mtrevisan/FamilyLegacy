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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Maps a raw FLEF {@code role} string to a {@link SocialRelationCategory}.
 * <p>
 * The FLEF protocol leaves {@code role} as free text. Both the
 * {@code RelationshipRecord.role} field (used with {@code associate} and
 * {@code group_member}) and the {@code EventParticipationRecord.role} field
 * (used for event participants such as witnesses and officiants) accept
 * arbitrary strings, with the protocol suggesting a set of conventional
 * values but not imposing them.
 * <p>
 * This class is the <b>single point of truth</b> for the mapping from a
 * raw role to a presentation category. The mapping is heuristic and
 * case‑insensitive; unrecognized roles fall back to
 * {@link SocialRelationCategory#OTHER} and empty roles also fall back to
 * {@code OTHER} (or, when configured, to the category of the relationship
 * type itself).
 * <p>
 * The class is stateless and thread‑safe.
 */
public final class SocialRoleClassifier{

	/**
	 * Default category assigned to roles not present in the table. This is
	 * chosen so that unknown custom roles never disappear from the network
	 * due to a classification failure.
	 */
	private static final SocialRelationCategory DEFAULT_CATEGORY = SocialRelationCategory.OTHER;


	/**
	 * Immutable mapping from normalized (lowercase) role strings to
	 * categories. Roles are normalized with {@link Locale#ROOT} to avoid
	 * locale‑dependent case folding.
	 */
	private static final Map<String, SocialRelationCategory> ROLE_TO_CATEGORY = buildRoleTable();


	private SocialRoleClassifier(){
	}


	/**
	 * Classifies the given role string.
	 * <p>
	 * The lookup is case‑insensitive and ignores leading/trailing
	 * whitespace. Roles not present in the table are classified as
	 * {@link SocialRelationCategory#OTHER}. A {@code null} or blank role is
	 * also classified as {@link SocialRelationCategory#OTHER}.
	 *
	 * @param role the raw role string; may be {@code null} or blank
	 * @return the matching category, never {@code null}
	 */
	public static SocialRelationCategory classify(final String role){
		if(role == null || role.isBlank())
			return DEFAULT_CATEGORY;
		final String normalized = role.trim()
			.toLowerCase(Locale.ROOT);
		return ROLE_TO_CATEGORY.getOrDefault(normalized, DEFAULT_CATEGORY);
	}

	/**
	 * Classifies the given role string, falling back to the given default
	 * category when the role is empty. This is useful when the caller knows
	 * a natural category for empty roles: for example, an empty role on a
	 * {@code group_member} relationship naturally belongs to
	 * {@link SocialRelationCategory#COMMUNITY}, whereas an empty role on a
	 * generic {@code associate} relationship belongs to
	 * {@link SocialRelationCategory#OTHER}.
	 *
	 * @param role              the raw role string; may be {@code null} or blank
	 * @param emptyRoleCategory the category to use when the role is empty;
	 *                          must not be {@code null}
	 * @return the matching category, never {@code null}
	 */
	public static SocialRelationCategory classify(final String role, final SocialRelationCategory emptyRoleCategory){
		if(emptyRoleCategory == null)
			throw new IllegalArgumentException("Empty-role category must not be null");
		if(role == null || role.isBlank())
			return emptyRoleCategory;
		return classify(role);
	}

	/**
	 * Returns whether the given role is recognized by the classifier.
	 *
	 * @param role the raw role string
	 * @return {@code true} if the role has an explicit entry in the table
	 */
	public static boolean isKnownRole(final String role){
		if(role == null || role.isBlank())
			return false;
		final String normalized = role.trim()
			.toLowerCase(Locale.ROOT);
		return ROLE_TO_CATEGORY.containsKey(normalized);
	}

	/**
	 * Returns the number of explicit role entries in the table. Useful for
	 * diagnostics and testing.
	 *
	 * @return the table size
	 */
	public static int knownRoleCount(){
		return ROLE_TO_CATEGORY.size();
	}


	/* ======================================================================
	 *                          Role table
	 * ====================================================================== */

	/**
	 * Builds the immutable role-to-category table.
	 * <p>
	 * The table combines:
	 * <ul>
	 *   <li>the roles suggested by the FLEF protocol for
	 *       {@code group_member} ({@code member}, {@code president},
	 *       {@code secretary}, {@code treasurer}, {@code resident},
	 *       {@code head_of_household}, {@code tribal_leader}, {@code elder},
	 *       {@code custodian});</li>
	 *   <li>the roles suggested by the protocol for
	 *       {@code EventParticipationRecord} ({@code witness},
	 *       {@code officiant}, {@code executor}, {@code grantor},
	 *       {@code grantee}, {@code landlord}, {@code tenant},
	 *       {@code informant}, {@code power_of_attorney}, {@code accused},
	 *       {@code judge}, {@code soldier}, {@code commander},
	 *       {@code victim}, {@code survivor});</li>
	 *   <li>common English and Italian synonyms for religious, professional,
	 *       legal, community and political roles, so that the classifier
	 *       works on real data without configuration.</li>
	 * </ul>
	 * The table is not intended to be exhaustive: it is a starting point
	 * that can be extended without changing the API.
	 */
	private static Map<String, SocialRelationCategory> buildRoleTable(){
		final Map<String, SocialRelationCategory> table = new HashMap<>(96);

		// ---------- COMMUNITY ----------
		// Protocol-suggested group_member roles that describe a civic or
		// community position.
		table.put("member", SocialRelationCategory.COMMUNITY);
		table.put("resident", SocialRelationCategory.COMMUNITY);
		table.put("head_of_household", SocialRelationCategory.COMMUNITY);
		table.put("tribal_leader", SocialRelationCategory.COMMUNITY);
		table.put("elder", SocialRelationCategory.COMMUNITY);
		table.put("custodian", SocialRelationCategory.COMMUNITY);
		// Common civic roles.
		table.put("neighbor", SocialRelationCategory.COMMUNITY);
		table.put("neighbour", SocialRelationCategory.COMMUNITY);
		table.put("friend", SocialRelationCategory.COMMUNITY);
		table.put("vicino", SocialRelationCategory.COMMUNITY);
		table.put("amico", SocialRelationCategory.COMMUNITY);
		table.put("capofamiglia", SocialRelationCategory.COMMUNITY);
		table.put("capofuoco", SocialRelationCategory.COMMUNITY);

		// ---------- RELIGIOUS ----------
		// Protocol-suggested event roles that are typically religious in a
		// genealogical context.
		table.put("witness", SocialRelationCategory.RELIGIOUS);
		table.put("officiant", SocialRelationCategory.RELIGIOUS);
		table.put("godparent", SocialRelationCategory.RELIGIOUS);
		table.put("godfather", SocialRelationCategory.RELIGIOUS);
		table.put("godmother", SocialRelationCategory.RELIGIOUS);
		table.put("padrino", SocialRelationCategory.RELIGIOUS);
		table.put("madrina", SocialRelationCategory.RELIGIOUS);
		table.put("testimone", SocialRelationCategory.RELIGIOUS);
		table.put("celebrante", SocialRelationCategory.RELIGIOUS);
		table.put("parroco", SocialRelationCategory.RELIGIOUS);
		table.put("priest", SocialRelationCategory.RELIGIOUS);
		table.put("pastor", SocialRelationCategory.RELIGIOUS);
		table.put("rabbi", SocialRelationCategory.RELIGIOUS);
		table.put("imam", SocialRelationCategory.RELIGIOUS);
		table.put("monk", SocialRelationCategory.RELIGIOUS);
		table.put("nun", SocialRelationCategory.RELIGIOUS);
		table.put("monaco", SocialRelationCategory.RELIGIOUS);
		table.put("monaca", SocialRelationCategory.RELIGIOUS);

		// ---------- PROFESSIONAL ----------
		// Organizational roles on group_member.
		table.put("president", SocialRelationCategory.PROFESSIONAL);
		table.put("secretary", SocialRelationCategory.PROFESSIONAL);
		table.put("treasurer", SocialRelationCategory.PROFESSIONAL);
		table.put("presidente", SocialRelationCategory.PROFESSIONAL);
		table.put("segretario", SocialRelationCategory.PROFESSIONAL);
		table.put("tesoriere", SocialRelationCategory.PROFESSIONAL);
		// Economic and military roles.
		table.put("employer", SocialRelationCategory.PROFESSIONAL);
		table.put("employee", SocialRelationCategory.PROFESSIONAL);
		table.put("business_partner", SocialRelationCategory.PROFESSIONAL);
		table.put("colleague", SocialRelationCategory.PROFESSIONAL);
		table.put("apprentice", SocialRelationCategory.PROFESSIONAL);
		table.put("mentor", SocialRelationCategory.PROFESSIONAL);
		table.put("master", SocialRelationCategory.PROFESSIONAL);
		table.put("landlord", SocialRelationCategory.PROFESSIONAL);
		table.put("tenant", SocialRelationCategory.PROFESSIONAL);
		table.put("soldier", SocialRelationCategory.PROFESSIONAL);
		table.put("commander", SocialRelationCategory.PROFESSIONAL);
		table.put("datore_di_lavoro", SocialRelationCategory.PROFESSIONAL);
		table.put("dipendente", SocialRelationCategory.PROFESSIONAL);
		table.put("socio", SocialRelationCategory.PROFESSIONAL);
		table.put("collega", SocialRelationCategory.PROFESSIONAL);
		table.put("apprendista", SocialRelationCategory.PROFESSIONAL);
		table.put("maestro", SocialRelationCategory.PROFESSIONAL);
		table.put("padrone_di_casa", SocialRelationCategory.PROFESSIONAL);
		table.put("affittuario", SocialRelationCategory.PROFESSIONAL);
		table.put("soldato", SocialRelationCategory.PROFESSIONAL);
		table.put("comandante", SocialRelationCategory.PROFESSIONAL);

		// ---------- LEGAL ----------
		table.put("executor", SocialRelationCategory.LEGAL);
		table.put("notary", SocialRelationCategory.LEGAL);
		table.put("judge", SocialRelationCategory.LEGAL);
		table.put("accused", SocialRelationCategory.LEGAL);
		table.put("grantor", SocialRelationCategory.LEGAL);
		table.put("grantee", SocialRelationCategory.LEGAL);
		table.put("power_of_attorney", SocialRelationCategory.LEGAL);
		table.put("guardian", SocialRelationCategory.LEGAL);
		table.put("esecutore", SocialRelationCategory.LEGAL);
		table.put("esecutore_testamentario", SocialRelationCategory.LEGAL);
		table.put("notaio", SocialRelationCategory.LEGAL);
		table.put("giudice", SocialRelationCategory.LEGAL);
		table.put("accusato", SocialRelationCategory.LEGAL);
		table.put("mandante", SocialRelationCategory.LEGAL);
		table.put("mandatario", SocialRelationCategory.LEGAL);
		table.put("procuratore", SocialRelationCategory.LEGAL);
		table.put("tutore", SocialRelationCategory.LEGAL);

		// ---------- POLITICAL ----------
		table.put("elector", SocialRelationCategory.POLITICAL);
		table.put("elected", SocialRelationCategory.POLITICAL);
		table.put("noble", SocialRelationCategory.POLITICAL);
		table.put("subject", SocialRelationCategory.POLITICAL);
		table.put("citizen", SocialRelationCategory.POLITICAL);
		table.put("elettore", SocialRelationCategory.POLITICAL);
		table.put("eletto", SocialRelationCategory.POLITICAL);
		table.put("nobile", SocialRelationCategory.POLITICAL);
		table.put("suddito", SocialRelationCategory.POLITICAL);
		table.put("cittadino", SocialRelationCategory.POLITICAL);

		return Map.copyOf(table);
	}

}
