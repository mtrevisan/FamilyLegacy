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
package io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


/** The bookmarks are of the file they were made in, and their names are told apart. */
class BookmarkStoreTest{

	private static final String FLEF = """
		header {
			protocol {
				name Family LEgacy Format
				version 0.2.0
			}
			date 2026-01-01T00:00:00Z
		}
		individual I1 {
			name {
				part {
					type given
					value Anna
				}
			}
		}
		""";


	private static Bookmark bookmark(final String name, final String rootId, final String file){
		return new Bookmark(BookmarkStore.newId(), name, BookmarkType.BIOLOGICAL_TREE, rootId,
			(file != null? Map.of(BookmarkStore.FILE_PROPERTY, file): Map.of()), System.nanoTime());
	}

	private static List<String> names(final List<Bookmark> bookmarks){
		return bookmarks.stream()
			.map(Bookmark::name)
			.toList();
	}


	@Test
	void aBookmarkIsShownOnlyInTheFileItWasMadeIn(@TempDir final Path directory){
		final BookmarkStore store = new BookmarkStore(directory.resolve("bookmarks.properties"));
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final String here = directory.resolve("a.flef").toAbsolutePath().toString();
		final String elsewhere = directory.resolve("b.flef").toAbsolutePath().toString();
		store.add(bookmark("here", "I1", here));
		store.add(bookmark("elsewhere", "I1", elsewhere));

		Assertions.assertEquals(List.of("here"), names(store.visible(model, here)));
		Assertions.assertEquals(List.of("elsewhere"), names(store.visible(model, elsewhere)));
		//a document with no file has none of them
		Assertions.assertTrue(store.visible(model, null).isEmpty());
	}

	@Test
	void aBookmarkTiedToNoFileBelongsToNone(@TempDir final Path directory){
		final BookmarkStore store = new BookmarkStore(directory.resolve("bookmarks.properties"));
		final FLEFModel model = new FLEFParser().parse(FLEF);
		//made before the bookmarks were tied to a file: whose Anna is "I1"?
		store.add(bookmark("old", "I1", null));

		Assertions.assertTrue(store.visible(model, "any").isEmpty());
		Assertions.assertTrue(store.visible(model, null).isEmpty());
		//it is not lost: it is still in the store
		Assertions.assertEquals(1, store.all().size());
	}

	@Test
	void theRootIsShownByItsNameAndNotByItsIdentifier(){
		final FLEFModel model = new FLEFParser().parse(FLEF);

		final String label = bookmark("x", "I1", null).rootLabel(model);

		//with no handlers of the application the text of a record is its identifier: with them (the application) it is the name
		Assertions.assertNotNull(label);
		Assertions.assertEquals("I99", bookmark("x", "I99", null).rootLabel(model), "a record that is not there is told by its identifier");
	}

	@Test
	void theBookmarksAreWrittenWithTheirFileAndNamesOfAnyAlphabet(@TempDir final Path directory) throws IOException{
		final Path file = directory.resolve("sub").resolve("bookmarks.properties");
		final BookmarkStore store = new BookmarkStore(file);
		final String name = "\u00c8 la \u5bb6\u65cf";
		store.add(bookmark(name, "I1", "C:\\data\\tree.flef"));

		final BookmarkStore reloaded = new BookmarkStore(file);

		Assertions.assertEquals(1, reloaded.all().size());
		Assertions.assertEquals(name, reloaded.all().getFirst().name());
		Assertions.assertEquals("C:\\data\\tree.flef", reloaded.all().getFirst().property(BookmarkStore.FILE_PROPERTY));
		//no half-written file is left beside it
		try(final var files = Files.list(file.getParent())){
			Assertions.assertEquals(1, files.count());
		}
		Assertions.assertFalse(Files.readString(file, java.nio.charset.StandardCharsets.ISO_8859_1).contains("\n#=FamilyLegacy"));
	}

}
