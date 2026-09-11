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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.NormalizedDate;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal.TemporalSpan;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * Immutable filter configuration for the Social Network views.
 * <p>
 * A filter combines five independent constraints:
 * <ul>
 *   <li><b>categories</b> — which {@link SocialRelationCategory} values are
 *       accepted. An empty set means "no category restriction".</li>
 *   <li><b>roles</b> — exact role strings to accept (case‑insensitive).
 *       An empty set means "no role restriction".</li>
 *   <li><b>maxDegree</b> — the maximum distance, in edges, from the focus
 *       entity. Zero means "focus only", one means "focus and its direct
 *       contacts", and so on.</li>
 *   <li><b>minDate</b> / <b>maxDate</b> — optional temporal window. When
 *       both are {@code null}, no temporal restriction is applied. When
 *       set, an edge is accepted only if its span intersects the window
 *       (open‑ended spans are considered to intersect every window that
 *       reaches their open end).</li>
 *   <li><b>includeInactive</b> — when {@code false}, edges whose span
 *       status is {@code ended} are rejected. {@code active} and
 *       {@code unknown} edges are always accepted.</li>
 * </ul>
 * A sixth flag, {@code includeEmptyRole}, controls whether edges without
 * an explicit role are accepted. This matters because the protocol allows
 * {@code associate} to carry no role at all, and users may want to exclude
 * such generic relationships when looking for specific roles.
 * <p>
 * The record is immutable: the sets are copied on construction and exposed
 * as unmodifiable views. Use the {@code with*} methods to obtain modified
 * copies.
 */
public record SocialFilters(
	Set<SocialRelationCategory> categories,
	Set<String> roles,
	int maxDegree,
	NormalizedDate minDate,
	NormalizedDate maxDate,
	boolean includeInactive,
	boolean includeEmptyRole
){

	/**
	 * Compact constructor with normalization and validation.
	 */
	public SocialFilters{
		categories = (categories != null && !categories.isEmpty()? Collections.unmodifiableSet(EnumSet.copyOf(categories)): Collections.emptySet());
		roles = (roles != null? Set.copyOf(roles): Set.of());
		if(maxDegree < 0)
			throw new IllegalArgumentException("Max degree must not be negative");
		if(minDate != null && maxDate != null && minDate.compareTo(maxDate) > 0)
			throw new IllegalArgumentException("minDate must not be greater than maxDate");
	}


	/**
	 * Returns a filter that accepts every edge, with the given maximum
	 * degree and the given temporal behaviour defaults (inactive edges
	 * excluded, empty‑role edges included).
	 *
	 * @param maxDegree the maximum distance from the focus
	 * @return a fully permissive filter
	 */
	public static SocialFilters all(final int maxDegree){
		return new SocialFilters(null, null, maxDegree, null, null, false, true);
	}

	/**
	 * Returns a filter that accepts every edge and includes inactive ones.
	 * Useful for historical analyses in which ended relationships are still
	 * relevant.
	 *
	 * @param maxDegree the maximum distance from the focus
	 * @return a fully permissive filter that includes inactive edges
	 */
	public static SocialFilters allIncludingInactive(final int maxDegree){
		return new SocialFilters(null, null, maxDegree, null, null, true, true);
	}


	/* ======================================================================
	 *                          Acceptance
	 * ====================================================================== */

	/**
	 * Returns whether the given edge passes all filter constraints.
	 *
	 * @param edge the edge to test (must not be {@code null})
	 * @return {@code true} if the edge is accepted
	 */
	public boolean accepts(final SocialEdgeRef edge){
		if(edge == null)
			return false;
		if(!acceptsCategory(edge.category()))
			return false;
		if(!acceptsRole(edge.role()))
			return false;
		if(!acceptsSpan(edge.span()))
			return false;
		return true;
	}

	/**
	 * Returns whether the given category is accepted.
	 *
	 * @param category the category to test
	 * @return {@code true} if the category is accepted
	 */
	public boolean acceptsCategory(final SocialRelationCategory category){
		if(category == null)
			return false;
		return (categories.isEmpty() || categories.contains(category));
	}

	/**
	 * Returns whether the given role is accepted. An empty role is accepted
	 * only when {@code includeEmptyRole} is true.
	 *
	 * @param role the role string to test; may be {@code null} or empty
	 * @return {@code true} if the role is accepted
	 */
	public boolean acceptsRole(final String role){
		final boolean empty = (role == null || role.isBlank());
		if(empty)
			return includeEmptyRole;
		if(roles.isEmpty())
			return true;
		for(final String candidate : roles)
			if(candidate.equalsIgnoreCase(role))
				return true;
		return false;
	}

	/**
	 * Returns whether the given span is accepted by the temporal and
	 * activity filters.
	 *
	 * @param span the span to test (must not be {@code null})
	 * @return {@code true} if the span is accepted
	 */
	public boolean acceptsSpan(final TemporalSpan span){
		if(span == null)
			return false;
		if(!includeInactive && TemporalSpan.STATUS_ENDED.equals(span.status()))
			return false;
		if(minDate != null && span.maxJdn() < minDate.jdn())
			return false;
		if(maxDate != null && span.minJdn() > maxDate.jdn())
			return false;
		return true;
	}

	/**
	 * Returns whether a temporal restriction is active.
	 *
	 * @return {@code true} if either {@code minDate} or {@code maxDate} is set
	 */
	public boolean hasTemporalWindow(){
		return (minDate != null || maxDate != null);
	}

	/**
	 * Returns whether a category restriction is active.
	 *
	 * @return {@code true} if the category set is non‑empty
	 */
	public boolean hasCategoryRestriction(){
		return !categories.isEmpty();
	}

	/**
	 * Returns whether a role restriction is active.
	 *
	 * @return {@code true} if the role set is non‑empty
	 */
	public boolean hasRoleRestriction(){
		return !roles.isEmpty();
	}


	/* ======================================================================
	 *                          Copy builders
	 * ====================================================================== */

	/**
	 * Returns a copy of this filter with the given maximum degree.
	 *
	 * @param maxDegree the new maximum degree (must be non‑negative)
	 * @return a new filter
	 */
	public SocialFilters withMaxDegree(final int maxDegree){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	/**
	 * Returns a copy of this filter with the given category set.
	 *
	 * @param categories the new categories; may be {@code null} or empty to
	 *                   remove the restriction
	 * @return a new filter
	 */
	public SocialFilters withCategories(final Set<SocialRelationCategory> categories){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	/**
	 * Returns a copy of this filter with the given role set.
	 *
	 * @param roles the new roles; may be {@code null} or empty to remove
	 *              the restriction
	 * @return a new filter
	 */
	public SocialFilters withRoles(final Set<String> roles){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	/**
	 * Returns a copy of this filter with the given temporal window.
	 *
	 * @param minDate the lower bound; may be {@code null}
	 * @param maxDate the upper bound; may be {@code null}
	 * @return a new filter
	 */
	public SocialFilters withTemporalWindow(final NormalizedDate minDate, final NormalizedDate maxDate){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	/**
	 * Returns a copy of this filter with the given inactive‑edges flag.
	 *
	 * @param includeInactive the new flag value
	 * @return a new filter
	 */
	public SocialFilters withIncludeInactive(final boolean includeInactive){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	/**
	 * Returns a copy of this filter with the given empty‑role flag.
	 *
	 * @param includeEmptyRole the new flag value
	 * @return a new filter
	 */
	public SocialFilters withIncludeEmptyRole(final boolean includeEmptyRole){
		return new SocialFilters(categories, roles, maxDegree, minDate, maxDate, includeInactive, includeEmptyRole);
	}

	@Override
	public String toString(){
		return "SocialFilters[categories=" + categories.size()
			+ ", roles=" + roles.size()
			+ ", maxDegree=" + maxDegree
			+ ", window=" + (hasTemporalWindow()? minDate + " .. " + maxDate: "any")
			+ ", inactive=" + includeInactive
			+ ", emptyRole=" + includeEmptyRole
			+ "]";
	}

}
