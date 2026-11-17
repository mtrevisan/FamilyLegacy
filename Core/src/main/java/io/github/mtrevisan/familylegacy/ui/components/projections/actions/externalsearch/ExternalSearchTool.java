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

package io.github.mtrevisan.familylegacy.ui.components.projections.actions.externalsearch;

import io.github.mtrevisan.familylegacy.externalsearch.ExternalSearchSettings;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;


/** Searches the selected individual on external sites (see {@link ExternalSearchDialog}). */
public final class ExternalSearchTool implements ToolOperation{

	/** Like the other research tools, from the standard level (the Research Assistant reaches it at every level). */
	@Override
	public UxLevel minimumLevel(){
		return UxLevel.STANDARD;
	}

	@Override
	public String getName(){
		return I18N.t("menu.research.external_search");
	}

	@Override
	public void run(final ToolContext context){
		if(!context.hasSelectedIndividual())
			return;

		new ExternalSearchDialog(context.owner(), context.model(), context.getSelectedEntity(), ExternalSearchSettings.shared())
			.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.model() != null && context.hasSelectedIndividual());
	}

}
