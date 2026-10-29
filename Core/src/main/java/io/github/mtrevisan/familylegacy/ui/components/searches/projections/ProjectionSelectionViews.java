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
package io.github.mtrevisan.familylegacy.ui.components.searches.projections;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship.UnlinkDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.places.PlaceHierarchyPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.PlaceHierarchyView;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.SelectionViewProvider;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.UnlinkPrompt;

import java.awt.Window;


/** The views of the projections that the selection dialog uses. */
public final class ProjectionSelectionViews implements SelectionViewProvider{

	@Override
	public PlaceHierarchyView createPlaceHierarchyView(final FLEFModel model){
		return new PlaceHierarchyPanel(model);
	}

	@Override
	public UnlinkPrompt createUnlinkPrompt(final Window parent, final FLEFModel model, final String entityId){
		return new UnlinkDialog(parent, model, entityId);
	}

}
