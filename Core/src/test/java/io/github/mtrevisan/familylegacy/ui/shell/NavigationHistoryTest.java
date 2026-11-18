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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;


/** Back and Forward skip what cannot be shown any more: a record that was deleted. */
class NavigationHistoryTest{

	private static NavigationHistory history(final String... ids){
		final NavigationHistory history = new NavigationHistory();
		for(final String id : ids)
			history.push(id);
		return history;
	}


	@Test
	void backSkipsTheDeletedRecords(){
		final NavigationHistory history = history("I1", "I2", "I3", "I4");
		final Set<String> alive = Set.of("I1", "I4");

		Assertions.assertTrue(history.canGoBack(alive::contains));
		Assertions.assertEquals("I1", history.goBack(alive::contains));
		Assertions.assertEquals("I1", history.current());
		Assertions.assertFalse(history.canGoBack(alive::contains));
		Assertions.assertNull(history.goBack(alive::contains));
		Assertions.assertEquals("I1", history.current());
	}

	@Test
	void forwardSkipsTheDeletedRecordsToo(){
		final NavigationHistory history = history("I1", "I2", "I3", "I4");
		history.goBack();
		history.goBack();
		history.goBack();
		final Set<String> alive = Set.of("I1", "I3");

		Assertions.assertTrue(history.canGoForward(alive::contains));
		Assertions.assertEquals("I3", history.goForward(alive::contains));
		Assertions.assertFalse(history.canGoForward(alive::contains), "I4 is gone");
	}

	@Test
	void whenNothingBeforeIsLeftBackIsNotPossible(){
		final NavigationHistory history = history("I1", "I2");

		Assertions.assertFalse(history.canGoBack(id -> false));
		Assertions.assertNull(history.goBack(id -> false));
		Assertions.assertEquals("I2", history.current());
		//the plain ones are as they were
		Assertions.assertTrue(history.canGoBack());
	}

}
