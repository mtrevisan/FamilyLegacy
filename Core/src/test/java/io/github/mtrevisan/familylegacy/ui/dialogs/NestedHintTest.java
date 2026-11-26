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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** A nested editor says, beside OK, whether its change survives closing the parent editor with ESC. */
class NestedHintTest{

	@Test
	void notNestedHasNoHint(){
		Assertions.assertNull(BaseRecordDialog.nestedHintKey(false, true));
		Assertions.assertNull(BaseRecordDialog.nestedHintKey(false, false));
	}

	@Test
	void nestedRecordOfTheFileIsSavedAtOnce(){
		Assertions.assertEquals("dialog.nested.hint.saved", BaseRecordDialog.nestedHintKey(true, true));
	}

	@Test
	void nestedPartOfTheParentIsKeptOnlyIfTheParentIsConfirmed(){
		Assertions.assertEquals("dialog.nested.hint.pending", BaseRecordDialog.nestedHintKey(true, false));
	}

	@Test
	void textsAreTranslated(){
		for(final String key : new String[]{"dialog.nested.hint.saved", "dialog.nested.hint.pending"})
			Assertions.assertNotEquals(key, I18N.t(key));
	}

}
