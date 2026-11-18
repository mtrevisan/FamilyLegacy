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

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.PendingFile;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/** Writing the files embedded in an imported file, and linking their media (no UI). */
class EmbeddedFilesTest{

	private static FLEFModel model(){
		final FLEFModel model = new FLEFModel();
		model.addRecord(FLEFRecord.createMainRecord("M1", MediaReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(MediaReader.TAG_URI, "unknown")));
		return model;
	}


	@Test
	void theFilesAreWrittenWithoutOverwritingAndTheMediaAreLinked(@TempDir final Path folder) throws IOException{
		Files.write(folder.resolve("M1.jpg"), new byte[]{9});
		final FLEFModel model = model();
		final ImportReport report = new ImportReport();
		report.addPendingFile(new PendingFile("M1", "M1.jpg", new byte[]{1, 2, 3}));

		Assertions.assertEquals(1, EmbeddedFiles.writeAndLink(model, report, folder.toFile()));

		//the existing file is untouched, the new one has a numeric suffix
		Assertions.assertArrayEquals(new byte[]{9}, Files.readAllBytes(folder.resolve("M1.jpg")));
		final File written = folder.resolve("M1-2.jpg").toFile();
		Assertions.assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(written.toPath()));
		Assertions.assertEquals(written.getAbsolutePath(), FLEFRecordHelper.getChildValue(model.getRecordById("M1"), MediaReader.TAG_URI));
		Assertions.assertTrue(report.pendingFiles().isEmpty());
		Assertions.assertTrue(report.isEmpty(), report.issues().toString());
	}

	@Test
	void aFileThatCannotBeWrittenIsReported(@TempDir final Path folder){
		final FLEFModel model = model();
		final ImportReport report = new ImportReport();
		report.addPendingFile(new PendingFile("M1", "M1.jpg", new byte[]{1}));

		//the "folder" does not exist
		Assertions.assertEquals(0, EmbeddedFiles.writeAndLink(model, report, folder.resolve("missing").toFile()));

		Assertions.assertEquals("unknown", FLEFRecordHelper.getChildValue(model.getRecordById("M1"), MediaReader.TAG_URI));
		Assertions.assertEquals(1, report.countsByReason().get(ImportReasons.MEDIA_DETAIL));
		Assertions.assertTrue(report.pendingFiles().isEmpty());
	}

	@Test
	void decliningReportsEveryFileAndKeepsThePlaceholder(){
		final FLEFModel model = model();
		final ImportReport report = new ImportReport();
		report.addPendingFile(new PendingFile("M1", "M1.jpg", new byte[]{1}));

		EmbeddedFiles.decline(report);

		Assertions.assertEquals("unknown", FLEFRecordHelper.getChildValue(model.getRecordById("M1"), MediaReader.TAG_URI));
		Assertions.assertEquals(1, report.issues().size());
		Assertions.assertEquals(ImportReasons.MEDIA_DETAIL, report.issues().getFirst().reasonKey());
		Assertions.assertEquals("M1", report.issues().getFirst().where());
		Assertions.assertTrue(report.pendingFiles().isEmpty());
	}

}
