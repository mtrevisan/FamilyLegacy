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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.preferences.TagStore;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.TreeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BooleanSupplier;


/** The free tags are kept for each file apart, outside the file: they come back when the file is opened again. */
class TagsEndToEndTest{

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


	@BeforeEach
	void useATagsFileOfItsOwn(@TempDir final Path directory){
		//the tags of the user are left alone
		System.setProperty(TagStore.FILE_PROPERTY_NAME, directory.resolve("tags.properties").toString());
	}

	@AfterEach
	void restoreTheTagsFile(){
		System.clearProperty(TagStore.FILE_PROPERTY_NAME);
	}


	private static File write(final Path path) throws IOException{
		Files.writeString(path, FLEF, StandardCharsets.UTF_8);
		return path.toFile();
	}

	private static void waitFor(final BooleanSupplier condition) throws InterruptedException{
		final long deadline = System.currentTimeMillis() + 20_000;
		while(!condition.getAsBoolean() && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
	}

	private static FamilyLegacyFrame frame() throws Exception{
		final FamilyLegacyFrame[] frame = new FamilyLegacyFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			frame[0] = new FamilyLegacyFrame(TreeType.FAMILY, new FLEFModel());
			frame[0].setTerminator(() -> {});
		});
		return frame[0];
	}

	private static void open(final FamilyLegacyFrame frame, final File file) throws Exception{
		SwingUtilities.invokeLater(() -> frame.openFile(file));
		waitFor(() -> file.equals(frame.fileController().currentFile()));
		//the model is replaced after the file is set
		SwingUtilities.invokeAndWait(() -> {});
		SwingUtilities.invokeAndWait(() -> {});
	}

	private static List<String> tagsOf(final FamilyLegacyFrame frame, final String id) throws Exception{
		final List<String>[] tags = new List[1];
		SwingUtilities.invokeAndWait(() -> tags[0] = frame.model().tags().get(id));
		return tags[0];
	}


	@Test
	void theTagsOfAFileComeBackWhenItIsOpenedAgainAndAreNotThoseOfAnotherFile(@TempDir final Path directory) throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The window needs a display");
		final File one = write(directory.resolve("one.flef"));
		final File two = write(directory.resolve("two.flef"));
		final FamilyLegacyFrame frame = frame();
		try{
			open(frame, one);
			SwingUtilities.invokeAndWait(() -> frame.model().tags().set("I1", List.of("Brick Wall", "To Verify")));

			//another file has its own tags
			open(frame, two);
			Assertions.assertEquals(List.of(), tagsOf(frame, "I1"));

			//the first one has them back, though its model is a new one
			open(frame, one);
			Assertions.assertEquals(List.of("Brick Wall", "To Verify"), tagsOf(frame, "I1"));
			//and they are not in the file
			Assertions.assertEquals(FLEF, Files.readString(one.toPath(), StandardCharsets.UTF_8));
		}
		finally{
			SwingUtilities.invokeAndWait(frame::dispose);
		}
	}

}
