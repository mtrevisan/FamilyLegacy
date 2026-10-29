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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchMode;

import javax.swing.JComponent;
import java.util.Set;


/**
 * The view of the hierarchy of the places that the selection dialog shows to choose a place relationship. The dialog does not name the
 * view (it lives with the projections, that depend on the dialog): it gets it from a {@link SelectionViewProvider}.
 */
public interface PlaceHierarchyView{

	/** The property that tells how far the filtering is (an {@link Integer} between 0 and 100). */
	String PROPERTY_FILTERING_PROGRESS = "filteringProgress";
	/** The property that tells that the filtering is over. */
	String PROPERTY_FILTERING_DONE = "filteringDone";


	/** @return the component to show (it fires the two properties above) */
	JComponent component();

	/** Reads the places again. */
	void reload();

	void setEditable(boolean editable);

	/**
	 * @param modes        the modes of the search
	 * @param search       the text to look for
	 * @param relationType the type of relationship to show ({@code null} for any)
	 * @return whether the view is being rebuilt (the progress is then notified)
	 */
	boolean setFilters(Set<SearchMode> modes, String search, String relationType);

	/** @return the place relationship that is selected, or {@code null} */
	FLEFRecord getSelectedPlaceRecord();

	/** Opens the editor of the selected place relationship. */
	void openEditorForSelection();

}
