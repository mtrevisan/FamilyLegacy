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
package io.github.mtrevisan.familylegacy.preferences;

import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;


/** The tags are kept for each file apart, and a file does not touch the tags of another one. */
class TagStoreTest{

	@TempDir
	Path directory;


	private static RecordTags tags(final String id, final String... values){
		final RecordTags tags = new RecordTags();
		tags.set(id, List.of(values));
		return tags;
	}


	@Test
	void theTagsOfAFileAreReadBackAsTheyWereSaved(){
		final TagStore store = new TagStore(directory.resolve("sub").resolve("tags.properties"));
		final RecordTags tags = tags("I1", "Brick Wall", "Needs Translation", "Città di confine");
		tags.set("P7", List.of("Open Conflict"));

		store.save("C:\research\family.flef", tags);

		final Map<String, List<String>> loaded = new TagStore(directory.resolve("sub").resolve("tags.properties"))
			.load("C:\research\family.flef");
		Assertions.assertEquals(Map.of("I1", List.of("Brick Wall", "Città di confine", "Needs Translation"), "P7", List.of("Open Conflict")),
			loaded);
	}

	@Test
	void theTagsOfAnotherFileAreLeftAlone(){
		final TagStore store = new TagStore(directory.resolve("tags.properties"));
		store.save("/a/one.flef", tags("I1", "Brick Wall"));
		store.save("/a/two.flef", tags("I1", "Immigration"));

		store.save("/a/one.flef", tags("I2", "To Verify"));

		Assertions.assertEquals(Map.of("I2", List.of("To Verify")), store.load("/a/one.flef"));
		Assertions.assertEquals(Map.of("I1", List.of("Immigration")), store.load("/a/two.flef"));
	}

	@Test
	void aFileWithNoTagsOrNoPathHasNone(){
		final TagStore store = new TagStore(directory.resolve("missing.properties"));

		Assertions.assertTrue(store.load("/a/one.flef").isEmpty());
		Assertions.assertTrue(store.load(null).isEmpty());

		store.save(null, tags("I1", "Brick Wall"));

		Assertions.assertFalse(directory.resolve("missing.properties").toFile().exists());
	}

	@Test
	void takingAwayEveryTagLeavesNothingForTheFile(){
		final TagStore store = new TagStore(directory.resolve("tags.properties"));
		store.save("/a/one.flef", tags("I1", "Brick Wall"));

		store.save("/a/one.flef", new RecordTags());

		Assertions.assertTrue(store.load("/a/one.flef").isEmpty());
	}

}
