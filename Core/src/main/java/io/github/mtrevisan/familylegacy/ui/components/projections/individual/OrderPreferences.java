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

import io.github.mtrevisan.familylegacy.ui.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.ui.preferences.ChildrenOrder;
import io.github.mtrevisan.familylegacy.ui.preferences.PartnerOrder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * The display order of children and partners chosen by the user.
 * <p>
 * <b>Separation of data and presentation.</b> Genealogical assertions represent evidence and conclusions. The order in which
 * children and partners are listed is not evidence: it is a presentation concern, decided by the views when they are built and
 * possibly different for each user looking at the same file. Therefore the choice lives here, in the user preferences
 * ({@link AppPreferences}, a file of the user's home), and <em>never</em> in the genealogy model: choosing an order does not
 * create, modify or delete any record, and it is not exported as an assertion. Do not "improve" this by saving the order in the
 * records (for example by reordering the children of a family when the file is saved): the file format must not depend on how a
 * user prefers to look at it.
 */
public final class OrderPreferences{

	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

	private static volatile ChildrenOrder children = AppPreferences.getInstance()
		.childrenOrder();
	private static volatile PartnerOrder partners = AppPreferences.getInstance()
		.partnerOrder();
	/** Incremented at every change, so that the caches of already ordered data can tell they are stale. */
	private static volatile int version;


	private OrderPreferences(){}


	/** @return	The order of the children (default {@link ChildrenOrder#BIRTH_DATE}). */
	public static ChildrenOrder children(){
		return children;
	}

	/** @return	The order of the partners (default {@link PartnerOrder#UNION_CHRONOLOGY}). */
	public static PartnerOrder partners(){
		return partners;
	}

	/** @return	A number that changes every time an order preference changes. */
	public static int version(){
		return version;
	}

	/** Changes the order of the children, persists the choice in the user preferences, and notifies the listeners. */
	public static void setChildren(final ChildrenOrder order){
		Objects.requireNonNull(order, "order");

		if(order == children)
			return;

		children = order;
		version ++;
		AppPreferences.getInstance()
			.setChildrenOrder(order);
		fireChanged();
	}

	/** Changes the order of the partners, persists the choice in the user preferences, and notifies the listeners. */
	public static void setPartners(final PartnerOrder order){
		Objects.requireNonNull(order, "order");

		if(order == partners)
			return;

		partners = order;
		version ++;
		AppPreferences.getInstance()
			.setPartnerOrder(order);
		fireChanged();
	}

	/** Registers a listener called when an order preference changes, so that the views can be rebuilt. */
	public static void addListener(final Runnable listener){
		LISTENERS.add(Objects.requireNonNull(listener, "listener"));
	}

	public static void removeListener(final Runnable listener){
		LISTENERS.remove(listener);
	}

	private static void fireChanged(){
		for(final Runnable listener : LISTENERS)
			listener.run();
	}

}
