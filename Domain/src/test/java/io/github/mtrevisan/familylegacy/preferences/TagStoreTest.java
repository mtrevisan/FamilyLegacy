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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/** The tags are kept next to each file, in its own folder, and travel with it. */
class TagStoreTest{

	@TempDir
	Path directory;


	private static RecordTags tags(final String id, final String... values){
		final RecordTags tags = new RecordTags();
		tags.set(id, List.of(values));
		return tags;
	}

	private TagStore store(){
		return new TagStore(directory.resolve("legacy").resolve("tags.properties"));
	}

	private String document(final String name){
		return directory.resolve(name).toString();
	}


	@Test
	void theTagsOfAFileAreReadBackAsTheyWereSaved(){
		final RecordTags tags = tags("I1", "Brick Wall", "Needs Translation", "Città di confine");
		tags.set("P7", List.of("Open Conflict"));

		store().save(document("family.flef"), tags);

		Assertions.assertTrue(Files.isRegularFile(directory.resolve("family.flef.meta").resolve("tags.properties")));
		Assertions.assertEquals(Map.of("I1", List.of("Brick Wall", "Città di confine", "Needs Translation"), "P7", List.of("Open Conflict")),
			store().load(document("family.flef")));
	}

	@Test
	void theTagsOfAnotherFileAreLeftAlone(){
		final TagStore store = store();
		store.save(document("one.flef"), tags("I1", "Brick Wall"));
		store.save(document("two.flef"), tags("I1", "Immigration"));

		store.save(document("one.flef"), tags("I2", "To Verify"));

		Assertions.assertEquals(Map.of("I2", List.of("To Verify")), store.load(document("one.flef")));
		Assertions.assertEquals(Map.of("I1", List.of("Immigration")), store.load(document("two.flef")));
	}

	@Test
	void aFileWithNoTagsOrNoPathHasNoneAndNothingIsCreated(){
		final TagStore store = store();

		Assertions.assertTrue(store.load(document("one.flef")).isEmpty());
		Assertions.assertTrue(store.load(null).isEmpty());

		store.save(null, tags("I1", "Brick Wall"));
		store.save(document("one.flef"), new RecordTags());

		Assertions.assertFalse(Files.exists(directory.resolve("one.flef.meta")));
	}

	@Test
	void takingAwayEveryTagLeavesNothingForTheFile(){
		final TagStore store = store();
		store.save(document("one.flef"), tags("I1", "Brick Wall"));

		store.save(document("one.flef"), new RecordTags());

		Assertions.assertTrue(store.load(document("one.flef")).isEmpty());
	}

	@Test
	void theTagsOfTheOlderVersionsAreBroughtNextToTheFileOnce() throws IOException{
		final Path legacy = directory.resolve("legacy").resolve("tags.properties");
		Files.createDirectories(legacy.getParent());
		final Properties props = new Properties();
		props.setProperty(document("one.flef") + "|I1", "Brick Wall\nTo Verify");
		props.setProperty(document("two.flef") + "|I1", "Immigration");
		try(final var out = Files.newOutputStream(legacy)){
			props.store(out, null);
		}
		final TagStore store = new TagStore(legacy);

		Assertions.assertEquals(Map.of("I1", List.of("Brick Wall", "To Verify")), store.load(document("one.flef")));
		Assertions.assertTrue(Files.isRegularFile(directory.resolve("one.flef.meta").resolve("tags.properties")));

		//once the tags are taken away they do not come back from the old store
		store.save(document("one.flef"), new RecordTags());
		Assertions.assertTrue(store.load(document("one.flef")).isEmpty());
		//the other file is untouched
		Assertions.assertFalse(Files.exists(directory.resolve("two.flef.meta")));
	}

	@Test
	void theFolderOfAFileIsCopiedWithTheCopyOfTheFile(){
		final TagStore store = store();
		store.save(document("one.flef"), tags("I1", "Brick Wall"));

		TagStore.copyMetaDirectory(document("one.flef"), document("copy.flef"));

		Assertions.assertEquals(Map.of("I1", List.of("Brick Wall")), store.load(document("copy.flef")));
		//a file with no folder copies nothing
		TagStore.copyMetaDirectory(document("none.flef"), document("other.flef"));
		Assertions.assertFalse(Files.exists(directory.resolve("other.flef.meta")));
	}

}
