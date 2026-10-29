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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.awt.Window;
import java.util.ServiceLoader;


/**
 * The views that the selection dialog uses and that live with the projections, which depend on the dialog: the dialog finds them as a
 * service instead of naming them.
 */
public interface SelectionViewProvider{

	/** @return the view of the hierarchy of the places */
	PlaceHierarchyView createPlaceHierarchyView(FLEFModel model);

	/** @return the dialog that lists the links of a record, to remove them */
	UnlinkPrompt createUnlinkPrompt(Window parent, FLEFModel model, String entityId);

	/** @return the provider of the application */
	static SelectionViewProvider find(){
		return ServiceLoader.load(SelectionViewProvider.class).findFirst()
			.orElseThrow(() -> new IllegalStateException("No provider of the selection views is declared"));
	}

}
