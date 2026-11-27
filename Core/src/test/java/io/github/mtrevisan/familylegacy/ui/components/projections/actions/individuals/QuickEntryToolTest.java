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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


/** The quick entry is in the Individual menu, next to Add Individual, with its own shortcut. */
class QuickEntryToolTest{

	@Test
	void itIsRightAfterAddIndividualInTheMenu(){
		final List<ToolOperation> tools = IndividualToolRegistry.primaryTools();

		final int add = tools.indexOf(tools.stream().filter(AddIndividualTool.class::isInstance).findFirst().orElseThrow());
		Assertions.assertInstanceOf(QuickEntryTool.class, tools.get(add + 1));
	}

	@Test
	void itHasCtrlShiftNAsItsShortcutAndNoOtherCommandHasIt(){
		final KeyStroke expected = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
		Assertions.assertEquals(expected, new QuickEntryTool().getAccelerator());

		final List<KeyStroke> strokes = new ArrayList<>();
		for(final ShortcutRegistry.ShortcutDefinition definition : ShortcutRegistry.getAllShortcuts())
			strokes.add(definition.keyStroke());
		Assertions.assertEquals(1, strokes.stream().filter(expected::equals).count());
	}

}
