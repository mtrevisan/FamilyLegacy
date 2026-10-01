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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;


/**
 * Listener for individual panel actions.
 */
public interface IndividualListener extends EntityListener{

	/**
	 * Called when the user selects a panel by clicking on it (outside the
	 * name label). The selection is a lighter action than navigation: the
	 * panel is highlighted, and any detail panel observing the selection
	 * is populated, but the tree is not re-rooted.
	 *
	 * @param selectedPanel the panel that was selected
	 * @param individual the individual displayed by the panel
	 */
	void onIndividualSelected(IndividualPanel selectedPanel, FLEFRecord individual);

	void onIndividualAddOrConnect(IndividualPanel selectedPanel, TreeOperation operation);

	void onChildAddOrConnect(IndividualPanel selectedPanel, TreeOperation operation);

	void onIndividualUnlink(IndividualPanel selectedPanel, FLEFRecord individual);

	/**
	 * Pastes the individual from the clipboard into the current context.
	 * The source individual is unlinked from all previous relationships.
	 */
	void onIndividualPaste(IndividualPanel selectedPanel);

}
