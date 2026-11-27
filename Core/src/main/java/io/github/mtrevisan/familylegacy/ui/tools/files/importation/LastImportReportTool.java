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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.tools.maintenance.DetectDuplicatesTool;

import javax.swing.JOptionPane;
import java.io.File;


/** File &gt; Last Import Report...: shows again what the import that created the open file could not keep. */
public final class LastImportReportTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.file.import.last_report");
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && ImportReportStore.exists(context.documentPath()));
	}

	@Override
	public void run(final ToolContext context){
		final String path = context.documentPath();
		final ImportReportStore.Stored stored = ImportReportStore.load(path);
		if(stored == null){
			MessageDialogs.show(context.owner(), I18N.t("dialog.import.report.none"), I18N.t("dialog.import.report.title"),
				JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		ImportReportDialog.show(context.owner(), stored.report(), new File(path), stored.source(), stored.date(),
			() -> context.runTool(new DetectDuplicatesTool()));
	}

}
