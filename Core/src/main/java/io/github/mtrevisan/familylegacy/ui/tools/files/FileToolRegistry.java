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
package io.github.mtrevisan.familylegacy.ui.tools.files;

import io.github.mtrevisan.familylegacy.importer.FileImporter;
import io.github.mtrevisan.familylegacy.importer.FileImporters;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.files.exportation.ExportFlefTool;
import io.github.mtrevisan.familylegacy.ui.tools.files.importation.ImportFileTool;
import io.github.mtrevisan.familylegacy.ui.tools.files.importation.ImportUnavailableTool;
import io.github.mtrevisan.familylegacy.ui.tools.maintenance.BackupTool;
import io.github.mtrevisan.familylegacy.ui.tools.maintenance.RestoreFromBackupTool;

import java.util.ArrayList;
import java.util.List;


/**
 * Static catalogue of the import and export operations exposed by the
 * {@code File} menu.
 */
public final class FileToolRegistry{

	private FileToolRegistry(){}


	public static List<ToolOperation> maintenanceTools(){
		return List.of(
			new BackupTool(),
			new RestoreFromBackupTool());
	}

	/**
	 * @return	One entry for each format that can be imported: the importers are plugins, so the ones that are installed give the
	 * 	entries, and for the formats the application knows and whose plugin is not there an entry says so.
	 */
	public static List<ToolOperation> importTools(){
		final List<ToolOperation> tools = new ArrayList<>();
		final List<String> found = new ArrayList<>();
		for(final FileImporter importer : FileImporters.available()){
			tools.add(new ImportFileTool(importer));
			found.add(importer.id());
		}
		for(final String id : FileImporters.KNOWN_IDS)
			if(!found.contains(id))
				tools.add(new ImportUnavailableTool(id));
		return tools;
	}

	public static List<ToolOperation> exportTools(){
		return List.of(
			new ExportFlefTool()
			// TODO ?
//			new ExportGedcom551Tool(),
//			new ExportGedcom7Tool(),
//			new ExportImageTool(),
//			new ExportExcelTool()
		);
	}

}
