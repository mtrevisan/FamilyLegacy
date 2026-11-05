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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/** The list of the recent files has no file twice, however the path of the file is written. */
class RecentFilesManagerTest{

	private Preferences preferences;


	@BeforeEach
	void createStore(){
		//a store of its own: the list of the user is left alone
		preferences = Preferences.userRoot()
			.node("familylegacy-test-" + UUID.randomUUID());
	}

	@AfterEach
	void removeStore() throws BackingStoreException{
		preferences.removeNode();
	}


	private static File file(final Path directory, final String name) throws IOException{
		return Files.writeString(directory.resolve(name), "x").toFile();
	}


	@Test
	void theSameFileReachedByDifferentPathsIsOneEntryAtTheFront(@TempDir final Path directory) throws IOException{
		final RecentFilesManager recent = new RecentFilesManager(preferences);
		final File tree = file(directory, "tree.flef");
		final File other = file(directory, "other.flef");
		Files.createDirectories(directory.resolve("sub"));

		recent.add(tree);
		recent.add(other);
		//the same file as "dir/sub/../tree.flef", and as a File built from the string of the path
		recent.add(directory.resolve("sub").resolve("..").resolve("tree.flef").toFile());
		recent.add(new File(tree.getAbsolutePath()));

		Assertions.assertEquals(2, recent.list().size(), recent.list().toString());
		Assertions.assertEquals(RecentFilesManager.normalized(tree), recent.list().getFirst());
		Assertions.assertEquals(RecentFilesManager.normalized(other), recent.list().get(1));
	}

	@Test
	void capitalsOfThePathAreNotAnotherFileWhereTheSystemIgnoresThem(@TempDir final Path directory) throws IOException{
		Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"), "Windows ignores the capitals of a path");
		final RecentFilesManager recent = new RecentFilesManager(preferences);
		final File tree = file(directory, "Tree.flef");

		recent.add(tree);
		recent.add(new File(tree.getParentFile(), "TREE.FLEF"));

		Assertions.assertEquals(1, recent.list().size(), recent.list().toString());
	}

	@Test
	void aFileIsRemovedAndFoundWhicheverPathIsUsed(@TempDir final Path directory) throws IOException{
		final RecentFilesManager recent = new RecentFilesManager(preferences);
		final File tree = file(directory, "tree.flef");
		recent.add(tree);
		final File alternative = directory.resolve(".").resolve("tree.flef").toFile();

		Assertions.assertTrue(recent.contains(alternative));

		recent.remove(alternative);
		Assertions.assertTrue(recent.list().isEmpty());
		Assertions.assertFalse(recent.contains(tree));
	}

	@Test
	void aListWrittenWithADuplicateIsReadWithoutItAndCorrected(@TempDir final Path directory) throws IOException{
		final File tree = file(directory, "tree.flef");
		final File other = file(directory, "other.flef");
		//what an older version could have kept: the same file with two spellings
		preferences.putInt("count", 3);
		preferences.put("file.0", tree.getAbsolutePath());
		preferences.put("file.1", other.getAbsolutePath());
		preferences.put("file.2", directory.resolve("x").resolve("..").resolve("tree.flef").toString());

		final RecentFilesManager recent = new RecentFilesManager(preferences);

		Assertions.assertEquals(2, recent.list().size(), recent.list().toString());
		//the list kept is the corrected one
		Assertions.assertEquals(2, preferences.getInt("count", 0));
	}

	@Test
	void theListHasAtMostTenFilesTheOldestDropped(@TempDir final Path directory) throws IOException{
		final RecentFilesManager recent = new RecentFilesManager(preferences);
		for(int i = 0; i < 12; i ++)
			recent.add(file(directory, "f" + i + ".flef"));

		Assertions.assertEquals(10, recent.list().size());
		Assertions.assertEquals("f11.flef", recent.list().getFirst().getName());
		Assertions.assertFalse(recent.contains(directory.resolve("f0.flef").toFile()));
	}

	@Test
	void twoDifferentFilesWithTheSameNameAreToldApartInTheMenu(@TempDir final Path directory) throws IOException{
		final RecentFilesManager recent = new RecentFilesManager(preferences);
		Files.createDirectories(directory.resolve("a"));
		Files.createDirectories(directory.resolve("b"));
		final File first = file(directory.resolve("a"), "tree.flef");
		final File second = file(directory.resolve("b"), "tree.flef");
		final File alone = file(directory, "alone.flef");
		recent.add(first);
		recent.add(second);
		recent.add(alone);
		final List<File> all = recent.list();

		Assertions.assertEquals(2, all.stream().filter(file -> file.getName().equals("tree.flef")).count());
		for(final File file : all)
			if(file.getName().equals("tree.flef"))
				Assertions.assertTrue(RecentFilesManager.label(file, all).contains(file.getParent()));
			else
				Assertions.assertEquals("alone.flef", RecentFilesManager.label(file, all));
	}

}
