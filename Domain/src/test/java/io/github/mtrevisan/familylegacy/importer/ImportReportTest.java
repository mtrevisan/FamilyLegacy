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
package io.github.mtrevisan.familylegacy.importer;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class ImportReportTest{

	/** The reasons the converters can give. */
	private static final List<String> REASONS = List.of("import.discard.identifier", "import.discard.contact_data",
		"import.discard.place_detail", "import.discard.note_detail", "import.discard.media_detail", "import.discard.name_detail",
		"import.discard.date_detail", "import.discard.source_detail", "import.discard.inconsistent_role", "import.discard.unmapped_tag",
		"import.discard.other");

	private static final List<String> DIALOG_KEYS = List.of("dialog.import.report.title", "dialog.import.report.intro",
		"dialog.import.report.summary", "dialog.import.report.details", "dialog.import.report.column.reason",
		"dialog.import.report.column.count", "dialog.import.report.column.item", "dialog.import.report.column.where",
		"dialog.import.report.overflow", "dialog.import.report.copy", "dialog.import.report.save",
		"dialog.import.report.save.error", "dialog.import.report.text.header", "dialog.import.report.text.overflow",
		"dialog.import.files.title", "dialog.import.files.message", "dialog.import.files.choose");


	@Test
	void aNewReportIsEmpty(){
		final ImportReport report = new ImportReport();

		Assertions.assertTrue(report.isEmpty());
		Assertions.assertTrue(report.issues().isEmpty());
		Assertions.assertEquals(0, report.overflow());
	}

	@Test
	void issuesAreKeptInOrderAndCounted(){
		final ImportReport report = new ImportReport();
		report.discard("import.discard.identifier", "UID 1", "INDI @I1@");
		report.discard("import.discard.date_detail", "?", null);
		report.discard("import.discard.identifier", "UID 2", "INDI @I2@");

		Assertions.assertFalse(report.isEmpty());
		Assertions.assertEquals(new ImportIssue("import.discard.identifier", "UID 1", "INDI @I1@"), report.issues().getFirst());
		Assertions.assertEquals("", report.issues().get(1).where());
		Assertions.assertEquals(Map.of("import.discard.identifier", 2, "import.discard.date_detail", 1), report.countsByReason());
		Assertions.assertEquals(List.of("import.discard.identifier", "import.discard.date_detail"),
			List.copyOf(report.countsByReason().keySet()));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> report.issues().add(null));
	}

	@Test
	void beyondTheCapOnlyTheCountsGrow(){
		final ImportReport report = new ImportReport();
		for(int i = 0; i < ImportReport.MAX_ISSUES + 7; i ++)
			report.discard("import.discard.other", "x" + i, "");

		Assertions.assertEquals(ImportReport.MAX_ISSUES, report.issues().size());
		Assertions.assertEquals(7, report.overflow());
		Assertions.assertEquals(ImportReport.MAX_ISSUES + 7, report.countsByReason().get("import.discard.other"));
	}

	@Test
	void pendingFilesAreKeptUntilCleared(){
		final ImportReport report = new ImportReport();
		Assertions.assertTrue(report.pendingFiles().isEmpty());

		final byte[] data = {1, 2, 3};
		report.addPendingFile(new PendingFile("M1", "M1.jpg", data));
		report.addPendingFile(new PendingFile("M1", "other.jpg", new byte[0]));
		report.addPendingFile(new PendingFile("M2", "M2.bin", data));
		report.addPendingFile(null);

		Assertions.assertEquals(List.of("M1.jpg", "M2.bin"), report.pendingFiles().stream()
			.map(PendingFile::suggestedName)
			.toList());
		Assertions.assertSame(data, report.pendingFiles().getFirst().data());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> report.pendingFiles().add(null));
		//a file to write is not a discard
		Assertions.assertTrue(report.isEmpty());

		report.clearPendingFiles();
		Assertions.assertTrue(report.pendingFiles().isEmpty());
	}

	@Test
	void everyReasonAndDialogKeyIsInTheBundle(){
		for(final String key : REASONS)
			Assertions.assertFalse(I18N.t(key).isBlank(), key);
		for(final String key : DIALOG_KEYS)
			Assertions.assertFalse(I18N.t(key).isBlank(), key);
	}

}
