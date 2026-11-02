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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;


class RecordDialogRegistryTest{

	/**
	 * The handlers do not name the editors: the catalog registers them. A type with a handler and no editor would fail only when the user
	 * opens it, so every type of the handlers has to have its editor.
	 */
	@Test
	void everyTypeOfTheHandlersHasItsEditor(){
		final Set<String> missing = new TreeSet<>(HandlerRegistry.types());
		missing.removeAll(RecordDialogs.registeredTypes());

		Assertions.assertTrue(missing.isEmpty(), "No editor is registered for " + missing);
		Assertions.assertFalse(HandlerRegistry.types().isEmpty());
	}

	@Test
	void aTypeWithoutAnEditorIsReportedClearly(){
		final IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
			() -> RecordDialogs.createNew("NO_SUCH_TYPE", null, null));

		Assertions.assertTrue(e.getMessage().contains("NO_SUCH_TYPE"), e.getMessage());
	}

}
