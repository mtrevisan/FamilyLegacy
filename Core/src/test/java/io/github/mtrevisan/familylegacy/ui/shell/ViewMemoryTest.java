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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;


/** The file opens on the person and the view it was left on. */
class ViewMemoryTest{

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
		individual I7 {
			name {
				part {
					type given
					value Berta
				}
			}
		}
		""";


	@Test
	void whatWasShownComesBack(@TempDir final Path directory) throws Exception{
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final String path = directory.resolve("a.flef").toString();

		Assertions.assertNull(ViewMemory.read(path, model));

		ViewMemory.save(path, "I7", ProjectionType.SUGIYAMA_GRAPH, model);

		final ViewMemory.Start start = ViewMemory.read(path, model);
		Assertions.assertEquals("I7", start.rootId());
		Assertions.assertEquals(ProjectionType.SUGIYAMA_GRAPH, start.projection());
	}

	@Test
	void theDefaultPersonLeavesNothingBehind(@TempDir final Path directory) throws Exception{
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final String path = directory.resolve("a.flef").toString();

		//I1 is the person a file opens on anyway: no folder is made for it
		ViewMemory.save(path, "I1", ProjectionType.BIOLOGICAL_TREE, model);

		Assertions.assertFalse(Files.exists(directory.resolve("a.flef.meta")));
	}

	@Test
	void aPersonThatIsGoneIsNotRemembered(@TempDir final Path directory) throws Exception{
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final String path = directory.resolve("a.flef").toString();
		ViewMemory.save(path, "I7", ProjectionType.BIOLOGICAL_TREE, model);

		model.removeRecord("I7");

		Assertions.assertNull(ViewMemory.read(path, model));
	}

	@Test
	void theChronomapIsNotOpenedByItself(@TempDir final Path directory) throws Exception{
		final FLEFModel model = new FLEFParser().parse(FLEF);
		final String path = directory.resolve("a.flef").toString();
		ViewMemory.save(path, "I7", ProjectionType.CHRONOMAP, model);

		final ViewMemory.Start start = ViewMemory.read(path, model);
		Assertions.assertEquals("I7", start.rootId());
		Assertions.assertNull(start.projection());
	}

}
