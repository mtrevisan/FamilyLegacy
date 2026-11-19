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

import io.github.mtrevisan.familylegacy.importer.FileImporter;
import io.github.mtrevisan.familylegacy.importer.FileImporters;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.files.FileToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;


/** The File > Import menu is one entry for all the formats: the importers that are installed give the formats of its file chooser. */
class ImportToolsTest{

	@Test
	void theInstalledImportersGiveOneEnabledEntry(){
		final List<String> installed = FileImporters.available().stream()
			.map(importer -> importer.id())
			.toList();
		Assertions.assertEquals(FileImporters.KNOWN_IDS, installed, "the plugins of the tests are the GEDCOM and the Gramps ones");

		final ToolOperation tool = FileToolRegistry.importTool();

		Assertions.assertInstanceOf(ImportFileTool.class, tool);
		Assertions.assertTrue(tool.isEnabled(null), tool.getName());
	}

	@Test
	void withoutImportersTheEntryIsShownButDisabled(){
		final ToolOperation tool = new ImportFileTool(List.of());

		Assertions.assertFalse(tool.isEnabled(null));
		Assertions.assertTrue(tool.getName().contains("no importer installed"), tool.getName());
	}

	@Test
	void aFileIsRecognisedFromItsExtensionIgnoringTheCase(){
		final List<FileImporter> importers = FileImporters.available();

		Assertions.assertEquals(List.of("gedcom"), idsAccepting(importers, "family.ged"));
		Assertions.assertEquals(List.of("gedcom"), idsAccepting(importers, "FAMILY.GED"));
		Assertions.assertEquals(List.of("gramps"), idsAccepting(importers, "tree.gramps"));
		Assertions.assertEquals(List.of("gramps"), idsAccepting(importers, "tree.gpkg"));
		Assertions.assertEquals(List.of(), idsAccepting(importers, "tree.flef"));
		Assertions.assertEquals(List.of(), idsAccepting(importers, "ged"));
	}

	@Test
	void theNameOfTheJarOfAFormatIsTheOneTheBuildProduces(){
		Assertions.assertEquals("importer-gedcom.jar", FileImporters.pluginFileName("gedcom"));
		Assertions.assertEquals("importer-gramps.jar", FileImporters.pluginFileName("gramps"));
	}


	private static List<String> idsAccepting(final List<FileImporter> importers, final String fileName){
		return importers.stream()
			.filter(importer -> importer.accepts(Path.of(fileName)))
			.map(FileImporter::id)
			.toList();
	}

}
