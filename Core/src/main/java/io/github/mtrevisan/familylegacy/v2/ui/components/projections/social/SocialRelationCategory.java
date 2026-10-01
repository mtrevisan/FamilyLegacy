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

/**
 * Broad semantic category of a non‑parental social relationship.
 * <p>
 * The FLEF protocol leaves {@code RelationshipRecord.role} as free text and
 * does not impose a taxonomy on {@code associate} or on the specific roles
 * that can accompany {@code group_member}. This enum provides a
 * presentation‑layer classification that the Social Network views use to
 * group, filter and color relationships without altering the underlying
 * protocol data.
 * <p>
 * A category is <i>advisory</i>: it is derived from the raw role string by
 * {@code SocialRoleClassifier}, and it never replaces the original role.
 * The original role remains available for tooltips, filtering by exact
 * value, and round‑tripping back to the FLEF model.
 */
public enum SocialRelationCategory{

	/**
	 * Kinship relations, including relations by marriage or adoption that
	 * are not covered by the biological tree views (e.g. in‑law, godparent
	 * in a traditional kinship sense, extended family roles). Family
	 * relations represented by dedicated FLEF types
	 * ({@code biological_child}, {@code civil_spouse}, …) are excluded from
	 * the social network and do not fall into this category.
	 */
	FAMILY("Family"),

	/**
	 * Religious roles and religious institutional relationships: godparent,
	 * witness at a baptism, officiant, member of a religious order.
	 */
	RELIGIOUS("Religious"),

	/**
	 * Professional and economic roles: employer, employee, business partner,
	 * mentor, apprentice, colleague, trade associate.
	 */
	PROFESSIONAL("Professional"),

	/**
	 * Legal and judicial roles: executor, notary, judge, accused, grantor,
	 * grantee, power of attorney, guardian (in the legal sense).
	 */
	LEGAL("Legal"),

	/**
	 * Community and civic roles: neighbor, friend, member of a fraternity,
	 * clan or tribe, elder, head of household, custodian.
	 */
	COMMUNITY("Community"),

	/**
	 * Political and administrative roles: elected official, elector, noble
	 * title bearer, subject or citizen of a polity.
	 */
	POLITICAL("Political"),

	/**
	 * Any relationship whose role is empty, unrecognized, or does not fit
	 * any of the categories above. This is the fallback category and is
	 * always accepted by any filter that includes it.
	 */
	OTHER("Other");


	private final String displayLabel;


	SocialRelationCategory(final String displayLabel){
		this.displayLabel = displayLabel;
	}


	/**
	 * Returns a human-readable label suitable for display in combo boxes,
	 * legends and tooltips.
	 *
	 * @return the display label, never {@code null}
	 */
	public String getDisplayLabel(){
		return displayLabel;
	}

	/**
	 * Returns whether this category is the fallback category.
	 *
	 * @return {@code true} for {@link #OTHER}
	 */
	public boolean isFallback(){
		return (this == OTHER);
	}

	/**
	 * Returns whether this category is the {@code FAMILY} one, which
	 * groups non-biological family relations and is normally excluded from
	 * the social network views because it overlaps with the biological tree
	 * and the Ego Network.
	 *
	 * @return {@code true} for {@link #FAMILY}
	 */
	public boolean isKinship(){
		return (this == FAMILY);
	}

}
