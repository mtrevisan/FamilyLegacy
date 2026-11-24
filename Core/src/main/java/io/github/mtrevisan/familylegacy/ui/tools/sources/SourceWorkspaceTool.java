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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance.SourceWorkspaceDialog;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;


/**
 * The source-centric workspace: for a source, the individuals, families, events, citations, extracts and media that depend on it, and
 * the way back from a citation to the person; the sources by repository, kind of repository and shelf mark. It opens on the selected
 * source, if there is one. The numbers come from {@link io.github.mtrevisan.familylegacy.sources.SourceWorkspace}; the tool does not
 * modify the model.
 */
public final class SourceWorkspaceTool implements ToolOperation{

	@Override
	public UxLevel minimumLevel(){
		return UxLevel.STANDARD;
	}

	@Override
	public String getName(){
		return I18N.t("menu.source.workspace");
	}

	@Override
	public void run(final ToolContext context){
		final FLEFRecord selected = (context.hasSelectedEntity()? context.getSelectedEntity(): null);
		final String sourceId = (selected != null && SourceHandler.isOfType(selected)? selected.getId(): null);

		new SourceWorkspaceDialog(context.owner(), context.model(), sourceId)
			.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAnySources());
	}

}
