package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.exportation.ExportFlefTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.files.importation.ImportFlefTool;

import java.util.List;


/**
 * Static catalogue of the import and export operations exposed by the
 * {@code File} menu.
 */
public final class FileToolRegistry{

	private FileToolRegistry(){
	}


	public static List<ToolOperation> importTools(){
		return List.of(
			new ImportFlefTool()//,
//			new ImportGedcomTool(),
//			new ImportCsvTool(),
//			new ImportLegacyTool()
		);
	}

	public static List<ToolOperation> exportTools(){
		return List.of(
//			new ExportGedcom551Tool(),
//			new ExportGedcom7Tool(),
			new ExportFlefTool()//,
//			new ExportPdfTool(),
//			new ExportHtmlTool(),
//			new ExportImageTool(),
//			new ExportExcelTool()
		);
	}

}
