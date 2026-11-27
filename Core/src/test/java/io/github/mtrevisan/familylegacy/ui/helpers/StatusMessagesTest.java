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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


class StatusMessagesTest{

	@AfterEach
	void tearDown(){
		StatusMessages.shared()
			.setListener(null);
	}


	@Test
	void theMessageGoesToWhoShowsThem(){
		final List<String> shown = new ArrayList<>();
		StatusMessages.shared()
			.setListener(shown::add);

		StatusMessages.shared()
			.show("Created 4 people");

		Assertions.assertEquals(List.of("Created 4 people"), shown);
	}

	@Test
	void withNobodyToShowThemTheMessagesAreLeftOutWithoutAnError(){
		Assertions.assertDoesNotThrow(() -> StatusMessages.shared().show("nobody listens"));

		final List<String> shown = new ArrayList<>();
		StatusMessages.shared()
			.setListener(shown::add);
		StatusMessages.shared()
			.show(null);

		Assertions.assertTrue(shown.isEmpty());
	}

}
