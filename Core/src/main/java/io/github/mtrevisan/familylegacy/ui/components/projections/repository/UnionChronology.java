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
package io.github.mtrevisan.familylegacy.ui.components.projections.repository;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;


/**
 * Derives, from the evidence, the date of the union of two partners, which is what orders the partners of an individual (see
 * {@link PresentationOrder}). The result is computed at render time and is not genealogical data.
 * <p>
 * The first of these that can be determined is used:
 * <ol>
 *   <li>the date of the marriage event shared by the two partners (the earliest, if there are several);</li>
 *   <li>the earliest date of any other marriage-related event shared by the two partners (banns, contract, license, settlement,
 *   divorce, annulment): the union is not later than any of them;</li>
 *   <li>the date from which their relationship is known to be in effect ({@code valid_from}).</li>
 * </ol>
 */
public final class UnionChronology{

	/** An event the two partners both took part in. */
	public record SharedEvent(String type, NormalizedDate date){}


	private static final Set<String> MARRIAGE_RELATED_TYPES = Set.of(
		EventReader.ENUM_TYPE_MARRIAGE, EventReader.ENUM_TYPE_MARRIAGE_BANN, EventReader.ENUM_TYPE_MARRIAGE_CONTRACT,
		EventReader.ENUM_TYPE_MARRIAGE_LICENSE, EventReader.ENUM_TYPE_MARRIAGE_SETTLEMENT,
		EventReader.ENUM_TYPE_DIVORCE_FILED, EventReader.ENUM_TYPE_DIVORCE_DECREE, EventReader.ENUM_TYPE_DIVORCE,
		EventReader.ENUM_TYPE_ANNULMENT);


	private UnionChronology(){}


	/**
	 * @param sharedEvents	The events both partners took part in.
	 * @param relationshipValidFrom	The date from which the relationship between the partners is in effect, if known.
	 * @return	The date of the union, or {@code null} if it cannot be determined.
	 */
	public static NormalizedDate unionDate(final Collection<SharedEvent> sharedEvents, final NormalizedDate relationshipValidFrom){
		NormalizedDate marriage = null;
		NormalizedDate marriageRelated = null;
		for(final SharedEvent event : sharedEvents){
			if(event.date() == null || event.type() == null)
				continue;

			final String type = event.type().toLowerCase(Locale.ROOT);
			if(EventReader.ENUM_TYPE_MARRIAGE.equals(type))
				marriage = earliest(marriage, event.date());
			if(MARRIAGE_RELATED_TYPES.contains(type))
				marriageRelated = earliest(marriageRelated, event.date());
		}

		if(marriage != null)
			return marriage;
		return (marriageRelated != null? marriageRelated: relationshipValidFrom);
	}

	private static NormalizedDate earliest(final NormalizedDate a, final NormalizedDate b){
		return (a == null || b.compareTo(a) < 0? b: a);
	}

}
