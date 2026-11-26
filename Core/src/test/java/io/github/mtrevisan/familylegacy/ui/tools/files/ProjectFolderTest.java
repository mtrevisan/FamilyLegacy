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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/** A project goes in a folder of its own, and the user is told when the folder chosen is shared with other trees. */
class ProjectFolderTest{

	@TempDir
	Path directory;


	@Test
	void theFileGoesInAFolderNamedAfterIt(){
		final File target = directory.resolve("rossi.flef").toFile();

		Assertions.assertEquals(directory.resolve("rossi").resolve("rossi.flef").toFile(), ProjectFolder.inOwnFolder(target));
	}

	@Test
	void aFileAlreadyInAFolderWithItsNameStaysThere(){
		final File target = directory.resolve("Rossi").resolve("rossi.flef").toFile();

		Assertions.assertEquals(target, ProjectFolder.inOwnFolder(target));
	}

	@Test
	void aFolderWithAnotherTreeIsShared() throws IOException{
		Files.writeString(directory.resolve("bianchi.flef"), "x");
		Files.writeString(directory.resolve("notes.txt"), "x");

		Assertions.assertTrue(ProjectFolder.sharesFolderWithOtherProjects(directory.resolve("rossi.flef").toFile()));
	}

	@Test
	void theFileItselfAndOtherFilesDoNotCount() throws IOException{
		Files.writeString(directory.resolve("rossi.flef"), "x");
		Files.writeString(directory.resolve("notes.txt"), "x");
		Files.createDirectory(directory.resolve("other.flef.meta"));

		Assertions.assertFalse(ProjectFolder.sharesFolderWithOtherProjects(directory.resolve("rossi.flef").toFile()));
		Assertions.assertFalse(ProjectFolder.sharesFolderWithOtherProjects(directory.resolve("missing").resolve("a.flef").toFile()));
	}

}
