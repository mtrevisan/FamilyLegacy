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
package io.github.mtrevisan.familylegacy.ui.preferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;


/** When the application starts again to change the language it opens the file that was open, not the one it was launched with. */
class ApplicationRestarterTest{

	private static final List<String> LAUNCH = List.of("java", "-jar", "app.jar");


	@Test
	void theOpenFileIsAddedToTheCommand(){
		final File file = new File("tree.flef");

		final List<String> command = ApplicationRestarter.withFile(LAUNCH, file);

		Assertions.assertEquals(List.of("java", "-jar", "app.jar", file.getAbsolutePath()), command);
	}

	@Test
	void theFileTheApplicationWasLaunchedWithIsReplaced(){
		final File file = new File("other.flef");

		final List<String> command = ApplicationRestarter.withFile(List.of("java", "-jar", "app.jar", "C:\\old\\Tree.FLEF"), file);

		Assertions.assertEquals(List.of("java", "-jar", "app.jar", file.getAbsolutePath()), command);
	}

	@Test
	void aDocumentWithNoFileStartsEmptyEvenIfTheApplicationWasLaunchedWithAFile(){
		final List<String> command = ApplicationRestarter.withFile(List.of("java", "-jar", "app.jar", "old.flef"), null);

		Assertions.assertEquals(LAUNCH, command);
		Assertions.assertEquals(LAUNCH, ApplicationRestarter.withFile(LAUNCH, null));
	}

}
