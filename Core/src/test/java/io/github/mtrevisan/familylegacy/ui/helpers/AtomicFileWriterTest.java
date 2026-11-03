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
package io.github.mtrevisan.familylegacy.ui.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;


class AtomicFileWriterTest{

	private static List<String> names(final Path directory) throws IOException{
		try(final Stream<Path> files = Files.list(directory)){
			return files.map(file -> file.getFileName().toString()).sorted().toList();
		}
	}


	@Test
	void writesANewFile(@TempDir final Path directory) throws IOException{
		final Path target = directory.resolve("family.flef");

		AtomicFileWriter.write(target, "records { }", StandardCharsets.UTF_8);

		Assertions.assertEquals("records { }", Files.readString(target, StandardCharsets.UTF_8));
		Assertions.assertEquals(List.of("family.flef"), names(directory), "a temporary file was left behind");
	}

	@Test
	void replacesAnExistingFileWholly(@TempDir final Path directory) throws IOException{
		final Path target = directory.resolve("family.flef");
		Files.writeString(target, "a much longer previous content that must not survive", StandardCharsets.UTF_8);

		AtomicFileWriter.write(target, "new", StandardCharsets.UTF_8);

		Assertions.assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
		Assertions.assertEquals(List.of("family.flef"), names(directory));
	}

	@Test
	void keepsTheEncoding(@TempDir final Path directory) throws IOException{
		final Path target = directory.resolve("names.flef");

		AtomicFileWriter.write(target, "Ježek, Žaneta, Müller, 山田", StandardCharsets.UTF_8);

		Assertions.assertEquals("Ježek, Žaneta, Müller, 山田", Files.readString(target, StandardCharsets.UTF_8));
	}

	@Test
	void aFailureLeavesTheTargetAndTheDirectoryAsTheyWere(@TempDir final Path directory) throws IOException{
		//a directory that is not empty cannot be replaced by a file
		final Path target = Files.createDirectory(directory.resolve("family.flef"));
		Files.writeString(target.resolve("inside.txt"), "kept", StandardCharsets.UTF_8);

		Assertions.assertThrows(IOException.class, () -> AtomicFileWriter.write(target, "new", StandardCharsets.UTF_8));

		Assertions.assertTrue(Files.isDirectory(target));
		Assertions.assertEquals("kept", Files.readString(target.resolve("inside.txt"), StandardCharsets.UTF_8));
		Assertions.assertEquals(List.of("family.flef"), names(directory), "the temporary file was not removed");
	}

	@Test
	void aMissingDirectoryIsAnError(@TempDir final Path directory){
		final Path target = directory.resolve("missing").resolve("family.flef");

		Assertions.assertThrows(IOException.class, () -> AtomicFileWriter.write(target, "new", StandardCharsets.UTF_8));
	}

}
