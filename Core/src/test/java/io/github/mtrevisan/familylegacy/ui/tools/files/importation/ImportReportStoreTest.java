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

import io.github.mtrevisan.familylegacy.importer.ImportReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;


/** The import report is kept next to the file and read back as it was. */
class ImportReportStoreTest{

	@Test
	void theReportComesBack(@TempDir final Path directory){
		final String path = directory.resolve("a.flef").toString();
		final ImportReport report = new ImportReport();
		report.discard("import.gramps.discard.coordinates", "45.1, 12.2", "place P1");
		report.discard("import.gramps.discard.coordinates", "tab\tand\nnewline", "place P2");

		ImportReportStore.save(path, report, "tree.ged");

		Assertions.assertTrue(ImportReportStore.exists(path));
		final ImportReportStore.Stored stored = ImportReportStore.load(path);
		Assertions.assertEquals("tree.ged", stored.source());
		Assertions.assertEquals(report.countsByReason(), stored.report().countsByReason());
		Assertions.assertEquals(report.issues(), stored.report().issues());
		Assertions.assertEquals(report.overflow(), stored.report().overflow());
	}

	@Test
	void anEmptyReportLeavesNothingBehind(@TempDir final Path directory){
		final String path = directory.resolve("a.flef").toString();

		ImportReportStore.save(path, new ImportReport(), "tree.ged");

		Assertions.assertFalse(ImportReportStore.exists(path));
		Assertions.assertNull(ImportReportStore.load(path));
	}

	@Test
	void theCountsOfTheIssuesNotListedAreKept(@TempDir final Path directory){
		final String path = directory.resolve("a.flef").toString();
		final ImportReport report = ImportReport.restored(List.of(), java.util.Map.of("import.gramps.discard.coordinates", 7), 7);

		ImportReportStore.save(path, report, "x.gramps");

		final ImportReportStore.Stored stored = ImportReportStore.load(path);
		Assertions.assertEquals(7, stored.report().overflow());
		Assertions.assertEquals(7, stored.report().countsByReason().get("import.gramps.discard.coordinates"));
	}

}
