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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;


class MediaPathDisplayTest{

	private static final String BASE = Path.of("media", "archive").toAbsolutePath().toString();
	private static final String INSIDE = Path.of(BASE, "parish", "1633", "birth.jpg").toString();
	private static final String OUTSIDE = Path.of("other", "photo.png").toAbsolutePath().toString();


	@Test
	void fullLeavesEverythingAsStored(){
		Assertions.assertEquals(INSIDE, MediaPathDisplay.FULL.format(INSIDE, BASE));
		Assertions.assertEquals("https://example.org/a/b.jpg", MediaPathDisplay.FULL.format("https://example.org/a/b.jpg", BASE));
	}

	@Test
	void fileNameKeepsOnlyTheName(){
		Assertions.assertEquals("birth.jpg", MediaPathDisplay.FILE_NAME.format(INSIDE, null));
		Assertions.assertEquals("b.jpg", MediaPathDisplay.FILE_NAME.format("https://example.org/a/b.jpg", null));
		Assertions.assertEquals("birth.jpg", MediaPathDisplay.FILE_NAME.format(Path.of(INSIDE).toUri().toString(), null));
	}

	@Test
	void relativeHidesOnlyTheBaseDirectory(){
		Assertions.assertEquals(Path.of("parish", "1633", "birth.jpg").toString(),
			MediaPathDisplay.RELATIVE_TO_BASE.format(INSIDE, BASE));
		Assertions.assertEquals(Path.of("parish", "1633", "birth.jpg").toString(),
			MediaPathDisplay.RELATIVE_TO_BASE.format(Path.of(INSIDE).toUri().toString(), BASE));
	}

	@Test
	void relativeFallsBackToTheWholeLocation(){
		//not under the base directory
		Assertions.assertEquals(OUTSIDE, MediaPathDisplay.RELATIVE_TO_BASE.format(OUTSIDE, BASE));
		//no base directory
		Assertions.assertEquals(INSIDE, MediaPathDisplay.RELATIVE_TO_BASE.format(INSIDE, null));
		//remote
		Assertions.assertEquals("https://example.org/a/b.jpg",
			MediaPathDisplay.RELATIVE_TO_BASE.format("https://example.org/a/b.jpg", BASE));
	}

	@AfterEach
	void forgetTheOpenFile(){
		MediaPathDisplay.setDocumentFile(null);
	}

	@Test
	void theDirectoryOfTheOpenFileIsTheDefaultBase(){
		final File flef = new File(BASE, "family.flef");
		MediaPathDisplay.setDocumentFile(flef);

		Assertions.assertEquals(BASE, MediaPathDisplay.baseDirectory(null));
		Assertions.assertEquals(Path.of("parish", "1633", "birth.jpg").toString(),
			MediaPathDisplay.RELATIVE_TO_BASE.format(INSIDE, MediaPathDisplay.baseDirectory(null)));
	}

	@Test
	void theBaseChosenByTheUserWinsOverTheOpenFile(){
		MediaPathDisplay.setDocumentFile(new File(BASE, "family.flef"));
		final String chosen = Path.of(BASE, "parish").toString();

		Assertions.assertEquals(chosen, MediaPathDisplay.baseDirectory(chosen));
		Assertions.assertEquals(Path.of("1633", "birth.jpg").toString(),
			MediaPathDisplay.RELATIVE_TO_BASE.format(INSIDE, MediaPathDisplay.baseDirectory(chosen)));
	}

	@Test
	void withoutAnOpenFileNorAChoiceThereIsNoBase(){
		MediaPathDisplay.setDocumentFile(new File(BASE, "family.flef"));
		MediaPathDisplay.setDocumentFile(null);

		Assertions.assertNull(MediaPathDisplay.baseDirectory(null));
		Assertions.assertEquals(INSIDE, MediaPathDisplay.RELATIVE_TO_BASE.format(INSIDE, MediaPathDisplay.baseDirectory(null)));
	}

	@Test
	void emptyValuesAreReturnedAsTheyAre(){
		Assertions.assertNull(MediaPathDisplay.FILE_NAME.format(null, BASE));
		Assertions.assertEquals("", MediaPathDisplay.RELATIVE_TO_BASE.format("", BASE));
	}

}
