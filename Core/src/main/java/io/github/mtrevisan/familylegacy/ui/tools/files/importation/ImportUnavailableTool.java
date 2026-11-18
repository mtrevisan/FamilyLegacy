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
import io.github.mtrevisan.familylegacy.importer.FileImporters;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;


/**
 * The entry of the menu of a format whose importer is not installed (its jar, {@code importer-<id>.jar}, is not in the {@code plugins}
 * folder): it is shown, so that the user knows the format exists, but it is disabled: the import is possible only if the jar is there.
 */
public final class ImportUnavailableTool implements ToolOperation{

	private final String label;


	/**
	 * @param id  the identifier of the format (see {@link FileImporters#KNOWN_IDS})
	 */
	public ImportUnavailableTool(final String id){
		label = I18N.t("import.format." + id);
	}


	@Override
	public String getName(){
		return I18N.tf("import.format.unavailable", label);
	}

	@Override
	public void run(final ToolContext context){
		//disabled: nothing to do
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return false;
	}

}
