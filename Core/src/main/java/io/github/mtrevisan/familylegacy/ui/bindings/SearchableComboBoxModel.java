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
package io.github.mtrevisan.familylegacy.ui.bindings;

import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * A combo box model that can temporarily show only the items matching a search text.
 * <p>
 * The full list of items is always kept (selection, indexes of the mutating methods and {@code getIndexOf} refer to it):
 * only the reading methods used by the view ({@link #getSize()}, {@link #getElementAt(int)}) follow the filter.
 * Every structural change clears the filter.
 * </p>
 *
 * @param <E>	The type of the elements.
 */
public class SearchableComboBoxModel<E> extends DefaultComboBoxModel<E>{

	/** The typed text is matched word by word, as in every search field. */
	private static final Set<SearchMode> NO_MODES = EnumSet.noneOf(SearchMode.class);


	private Function<Object, String> labelProvider = String::valueOf;
	private String searchText = StringUtils.EMPTY;
	private List<E> visible;


	public SearchableComboBoxModel(){
		super();
	}

	public SearchableComboBoxModel(final E[] items){
		super(items);
	}


	/** @param labelProvider	The text of an item as the user sees it. */
	public void setLabelProvider(final Function<Object, String> labelProvider){
		this.labelProvider = labelProvider;
	}

	public String getSearchText(){
		return searchText;
	}

	public boolean isFiltered(){
		return (visible != null);
	}

	/** @return	The number of items, whatever the filter. */
	public int getTotalSize(){
		return super.getSize();
	}

	/**
	 * Shows only the items matching the given text; a blank text shows everything.
	 *
	 * @param text	The search text.
	 */
	public void setSearchText(final String text){
		final String trimmed = (text == null? StringUtils.EMPTY: text);
		if(trimmed.equals(searchText))
			return;

		final int oldSize = getSize();
		searchText = trimmed;
		if(trimmed.isBlank())
			visible = null;
		else{
			final String query = trimmed.trim().toLowerCase();
			final int total = super.getSize();
			final List<E> matches = new ArrayList<>(total);
			for(int i = 0; i < total; i ++){
				final E item = super.getElementAt(i);
				final String label = labelProvider.apply(item);
				if(StringUtils.isNotEmpty(label) && SearchMatcher.score(query, label.toLowerCase(), NO_MODES).passes())
					matches.add(item);
			}
			visible = matches;
		}
		fireContentsChanged(this, 0, Math.max(oldSize, getSize()));
	}

	public void clearSearch(){
		setSearchText(StringUtils.EMPTY);
	}


	/** The whole list is back before the selection changes, so that whoever listens sees the indexes of the full list. */
	@Override
	public void setSelectedItem(final Object item){
		clearSearch();

		super.setSelectedItem(item);
	}


	@Override
	public int getSize(){
		return (visible != null? visible.size(): super.getSize());
	}

	@Override
	public E getElementAt(final int index){
		if(visible == null)
			return super.getElementAt(index);

		return (index >= 0 && index < visible.size()? visible.get(index): null);
	}


	@Override
	public void addElement(final E item){
		clearSearch();
		super.addElement(item);
	}

	@Override
	public void addAll(final Collection<? extends E> items){
		clearSearch();
		super.addAll(items);
	}

	@Override
	public void addAll(final int index, final Collection<? extends E> items){
		clearSearch();
		super.addAll(index, items);
	}

	@Override
	public void insertElementAt(final E item, final int index){
		clearSearch();
		super.insertElementAt(item, index);
	}

	@Override
	public void removeElement(final Object item){
		clearSearch();
		super.removeElement(item);
	}

	@Override
	public void removeElementAt(final int index){
		clearSearch();
		super.removeElementAt(index);
	}

	@Override
	public void removeAllElements(){
		clearSearch();
		super.removeAllElements();
	}

}
