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
package io.github.mtrevisan.familylegacy.backup;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/** A project is archived in one ZIP with its data and, if asked, its media; extracting it gives back a working project. */
class ProjectArchiveTest{

	private static final String AUDIT = "\taudit.creation.date 2020-01-01T00:00:00Z\n";

	@TempDir
	Path directory;


	private FLEFModel model(final Path photo){
		final FLEFModel model = new FLEFParser().parse("media M1 {\n\turi " + photo.toUri() + "\n\tkind image\n" + AUDIT + "}\n"
			+ "media M2 {\n\turi https://example.org/remote.jpg\n\tkind image\n" + AUDIT + "}\n"
			+ "media M3 {\n\turi " + directory.resolve("gone.jpg").toUri() + "\n\tkind image\n" + AUDIT + "}\n");
		model.tags().set("M1", List.of("Brick Wall"));
		return model;
	}

	private Path project() throws IOException{
		final Path project = Files.createDirectories(directory.resolve("project"));
		//what the application keeps next to the file: the bookmarks are copied, the tags are written from the model
		final Path meta = Files.createDirectories(project.resolve("tree.flef.meta"));
		Files.writeString(meta.resolve("bookmarks.properties"), "bookmark=1\n");
		Files.writeString(meta.resolve("tags.properties"), "M9=Stale\n");
		return project.resolve("tree.flef");
	}


	@Test
	void theArchiveHasTheFileTheDataAndTheMediaAndExtractsBackLinked() throws IOException{
		final Path photo = directory.resolve("photo.jpg");
		Files.write(photo, new byte[]{1, 2, 3, 4});
		final Path zip = directory.resolve("backup.zip");

		final ProjectArchive.WriteResult result = ProjectArchive.write(zip, model(photo), project().toString(), true, null);

		Assertions.assertEquals(new ProjectArchive.WriteResult(1, 1, 1), result);
		Assertions.assertFalse(Files.exists(directory.resolve("backup.zip.tmp")));

		final Path folder = directory.resolve("restored");
		final ProjectArchive.Extracted extracted = ProjectArchive.extract(zip, folder);

		Assertions.assertEquals(folder.resolve("tree.flef").toAbsolutePath(), extracted.flef());
		Assertions.assertEquals(1, extracted.mediaRestored());
		//the media record is linked to the extracted file, the others are left as they were
		final FLEFModel restored = new FLEFParser().parse(extracted.flef());
		final Path linked = Path.of(MediaReader.extractUri(restored.getRecordById("M1")));
		Assertions.assertTrue(linked.startsWith(folder.toAbsolutePath().resolve("media")));
		Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(linked));
		Assertions.assertEquals("https://example.org/remote.jpg", MediaReader.extractUri(restored.getRecordById("M2")));
		//the data of the application come with it: the tags are the ones of the model, not the stale ones of the folder
		Assertions.assertEquals("bookmark=1\n", Files.readString(folder.resolve("tree.flef.meta").resolve("bookmarks.properties")));
		Assertions.assertTrue(Files.readString(folder.resolve("tree.flef.meta").resolve("tags.properties")).contains("M1=Brick Wall"));
		Assertions.assertFalse(Files.readString(folder.resolve("tree.flef.meta").resolve("tags.properties")).contains("Stale"));
		//and nothing of the archive itself is left in the project
		Assertions.assertFalse(Files.exists(folder.resolve("manifest.properties")));
		Assertions.assertFalse(Files.exists(folder.resolve("media-map.properties")));
	}

	@Test
	void withoutTheMediaTheFileIsExtractedAsItWasArchived() throws IOException{
		final Path photo = directory.resolve("photo.jpg");
		Files.write(photo, new byte[]{1});
		final Path zip = directory.resolve("backup.zip");

		final ProjectArchive.WriteResult result = ProjectArchive.write(zip, model(photo), project().toString(), false, null);
		final ProjectArchive.Extracted extracted = ProjectArchive.extract(zip, directory.resolve("restored"));

		Assertions.assertEquals(new ProjectArchive.WriteResult(0, 0, 0), result);
		Assertions.assertEquals(0, extracted.mediaRestored());
		Assertions.assertFalse(Files.exists(directory.resolve("restored").resolve("media")));
		Assertions.assertEquals(photo.toUri().toString(),
			MediaReader.extractUri(new FLEFParser().parse(extracted.flef()).getRecordById("M1")));
	}

	@Test
	void aDocumentWithNoFileIsArchivedWithItsTags() throws IOException{
		final Path zip = directory.resolve("backup.zip");

		ProjectArchive.write(zip, model(directory.resolve("photo.jpg")), null, false, null);
		final ProjectArchive.Extracted extracted = ProjectArchive.extract(zip, directory.resolve("restored"));

		Assertions.assertEquals("project.flef", extracted.flef().getFileName().toString());
		Assertions.assertTrue(Files.isRegularFile(directory.resolve("restored").resolve("project.flef.meta").resolve("tags.properties")));
	}

	@Test
	void theFolderMustBeFreeAndTheArchiveMustBeOneOfTheApplication() throws IOException{
		final Path zip = directory.resolve("backup.zip");
		ProjectArchive.write(zip, model(directory.resolve("photo.jpg")), null, false, null);
		final Path taken = Files.createDirectories(directory.resolve("taken"));
		Files.writeString(taken.resolve("a.txt"), "x");
		final Path other = directory.resolve("other.zip");
		try(final ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(other))){
			out.putNextEntry(new ZipEntry("readme.txt"));
			out.write("hello".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}

		Assertions.assertThrows(IOException.class, () -> ProjectArchive.extract(zip, taken));
		Assertions.assertThrows(IOException.class, () -> ProjectArchive.extract(other, directory.resolve("restored")));
		Assertions.assertTrue(Files.exists(taken.resolve("a.txt")));
	}

	@Test
	void anEntryThatWouldLeaveTheFolderIsRefused() throws IOException{
		final Path zip = directory.resolve("evil.zip");
		try(final ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))){
			out.putNextEntry(new ZipEntry("manifest.properties"));
			out.write("flef=a.flef\n".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("a.flef"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("../evil.txt"));
			out.write("x".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}

		Assertions.assertThrows(IOException.class, () -> ProjectArchive.extract(zip, directory.resolve("sub").resolve("restored")));
		Assertions.assertFalse(Files.exists(directory.resolve("sub").resolve("evil.txt")));
	}

	@Test
	void anArchiveIsRecognisedByItsExtension(){
		Assertions.assertTrue(ProjectArchive.isArchive(Path.of("a", "backup.ZIP")));
		Assertions.assertFalse(ProjectArchive.isArchive(Path.of("a", "tree.flef")));
		Assertions.assertFalse(ProjectArchive.isArchive(null));
	}

}
