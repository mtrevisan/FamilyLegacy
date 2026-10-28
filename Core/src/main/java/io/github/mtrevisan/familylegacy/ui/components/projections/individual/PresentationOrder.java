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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.ui.preferences.ChildrenOrder;
import io.github.mtrevisan.familylegacy.ui.preferences.PartnerOrder;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * The single place where the order of children and partners shown by the views is decided.
 * <p>
 * <b>Genealogical data and presentation are separate.</b> Genealogical assertions represent evidence and conclusions; the order
 * in which children and partners are listed is not evidence, it is a presentation concern, and different users may legitimately
 * prefer different orders for the same file. So the order is computed at render time, every time a list is built, from the
 * evidence and from the user's choice ({@link OrderPreferences}); it is never written back to the model, so no record is created,
 * modified or deleted by it, it is not exported, and the file format is not affected. For the same reason there is no "sort
 * children" or "sort partners" command: reordering stored records would change nothing the user can see.
 * <p>
 * The strategies are {@link ChildrenOrder} and {@link PartnerOrder}. In the default ones (birth date, union chronology) the items
 * with a usable date come first, oldest first, and the others follow in the order they were given. In the alphabetical ones the
 * items are sorted by preferred display name (with the rules of the current locale) and the unnamed ones follow. In the "existing"
 * ones nothing is re-sorted. Every sort is stable, so the result is deterministic whenever the input is. Dates are compared as
 * {@link NormalizedDate}, that is on the Julian Day Number axis, so calendars mix and approximate or bounded dates are already
 * reduced to their representative date.
 * <p>
 * The kind of relationship (biological, adoptive, foster, ...) plays no role in any order: it may be displayed, but a child is
 * placed by its birth (or name) only.
 */
public final class PresentationOrder{

	private PresentationOrder(){}


	/**
	 * Orders the children as chosen by the user ({@link OrderPreferences#children()}).
	 *
	 * @param children	The children (a {@code null} element is considered without birth date and without name).
	 * @return	A new list; the input is not modified.
	 */
	public static List<IndividualData> children(final Collection<IndividualData> children){
		return children(children, OrderPreferences.children(), Function.identity());
	}

	/**
	 * Orders the children with the given strategy. The preferred birth of each child is the one resolved by
	 * {@link IndividualData} (the preferred assertion, as defined by the conclusions, or the earliest one).
	 *
	 * @param items	The children, in any representation.
	 * @param order	The strategy.
	 * @param dataOf	Gives the data of a child ({@code null} if not available).
	 * @return	A new list; the input is not modified.
	 */
	public static <T> List<T> children(final Collection<T> items, final ChildrenOrder order,
			final Function<? super T, IndividualData> dataOf){
		return switch(order){
			case BIRTH_DATE -> byDate(items, item -> {
				final IndividualData data = dataOf.apply(item);
				return (data != null? data.getBirthDate(): null);
			});
			case ALPHABETICAL -> alphabetical(items, item -> {
				final IndividualData data = dataOf.apply(item);
				return (data != null && data.hasName()? data.getNameText(): null);
			});
			case EXISTING -> new ArrayList<>(items);
		};
	}

	/**
	 * Orders the partners of an individual as chosen by the user ({@link OrderPreferences#partners()}).
	 *
	 * @param partnerIds	The ids of the partners.
	 * @param unionDateOf	Gives the date of the union with a partner, or {@code null} if it cannot be determined.
	 * @param nameOf	Gives the preferred display name of a partner, or {@code null} if it has none.
	 * @return	A new list; the input is not modified.
	 */
	public static List<String> partners(final Collection<String> partnerIds, final Function<String, NormalizedDate> unionDateOf,
			final Function<String, String> nameOf){
		return partners(partnerIds, OrderPreferences.partners(), unionDateOf, nameOf);
	}

	/** Orders the partners of an individual with the given strategy. */
	public static List<String> partners(final Collection<String> partnerIds, final PartnerOrder order,
			final Function<String, NormalizedDate> unionDateOf, final Function<String, String> nameOf){
		return switch(order){
			case UNION_CHRONOLOGY -> byDate(partnerIds, unionDateOf);
			case ALPHABETICAL -> alphabetical(partnerIds, nameOf);
			case EXISTING -> new ArrayList<>(partnerIds);
		};
	}

	/**
	 * Orders the items by date: dated items first, oldest first, then the undated ones in their original order.
	 *
	 * @param items	The items.
	 * @param dateOf	Gives the date of an item, or {@code null} if it has none.
	 * @return	A new list; the input is not modified.
	 */
	public static <T> List<T> byDate(final Collection<T> items, final Function<? super T, NormalizedDate> dateOf){
		return sortKeyed(items, dateOf, (a, b) -> a.compareTo(b));
	}

	/**
	 * Orders the items by name, with the rules of the current locale (case and accents are secondary): named items first, then the
	 * unnamed ones in their original order.
	 *
	 * @param items	The items.
	 * @param nameOf	Gives the name of an item, or {@code null} if it has none.
	 * @return	A new list; the input is not modified.
	 */
	public static <T> List<T> alphabetical(final Collection<T> items, final Function<? super T, String> nameOf){
		final Collator collator = Collator.getInstance();
		return sortKeyed(items, item -> {
			final String name = nameOf.apply(item);
			return (name != null && !name.isBlank()? name: null);
		}, collator::compare);
	}


	/**
	 * Sorts by a key, computed once per item, with the items without a key last; the sort is stable, so the original order of the
	 * items with equal or missing key is kept.
	 */
	private static <T, K> List<T> sortKeyed(final Collection<T> items, final Function<? super T, K> keyOf,
			final java.util.Comparator<K> comparator){
		final List<Keyed<T, K>> keyed = new ArrayList<>(items.size());
		for(final T item : items)
			keyed.add(new Keyed<>(item, keyOf.apply(item)));

		keyed.sort((a, b) -> {
			if(a.key() == null || b.key() == null)
				return (a.key() == null? 1: 0) - (b.key() == null? 1: 0);
			return comparator.compare(a.key(), b.key());
		});

		final List<T> result = new ArrayList<>(keyed.size());
		for(final Keyed<T, K> entry : keyed)
			result.add(entry.item());
		return result;
	}


	private record Keyed<T, K>(T item, K key){}

}
