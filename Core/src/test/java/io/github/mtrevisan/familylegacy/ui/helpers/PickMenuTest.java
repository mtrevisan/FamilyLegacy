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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;


/** A field that is picked with the right button says it while it is empty, and offers Set... in its menu. */
class PickMenuTest{

	@Test
	void aFieldThatCannotBeTypedInSaysThatTheRightButtonSets(){
		final HintTextField field = new HintTextField(20);
		field.setEditable(false);

		PickMenu.install(field, () -> {}, null);

		Assertions.assertEquals(I18N.t("placeholder.text"), field.getHint());
		//the hint is drawn, not part of the text: the text of the field stays empty
		Assertions.assertEquals("", field.getText());
	}

	@Test
	void aFieldThatCanBeTypedInSaysItToo(){
		final HintTextField field = new HintTextField(20);

		PickMenu.install(field, () -> {}, null);

		Assertions.assertEquals(I18N.t("placeholder.type_or_set"), field.getHint());
		Assertions.assertTrue(field.getHint().contains("type"));
		Assertions.assertTrue(field.getHint().contains("right-click"));
	}

	@Test
	void theDoubleClickPicksOnlyWhenTheFieldCannotBeTypedIn(){
		final AtomicInteger picks = new AtomicInteger();
		final HintTextField readOnly = new HintTextField(20);
		readOnly.setEditable(false);
		PickMenu.install(readOnly, picks::incrementAndGet, null);
		final HintTextField typed = new HintTextField(20);
		PickMenu.install(typed, picks::incrementAndGet, null);

		click(readOnly, 2);
		click(typed, 2);

		Assertions.assertEquals(1, picks.get());
	}

	@Test
	void theRightButtonMenuHasSetAndClearWhenThereIsSomethingToClear(){
		final HintTextField field = new HintTextField(20);
		final AtomicInteger cleared = new AtomicInteger();
		PickMenu.install(field, () -> {}, cleared::incrementAndGet);

		//the menu is shown by a popup trigger: it is built once, with the two items
		final JPopupMenu[] shown = new JPopupMenu[1];
		for(final java.awt.event.MouseListener listener : field.getMouseListeners())
			if(listener.getClass().getName().startsWith(PickMenu.class.getName()))
				shown[0] = menuOf(listener);
		Assertions.assertNotNull(shown[0]);
		Assertions.assertEquals(2, shown[0].getComponentCount());
		((JMenuItem)shown[0].getComponent(1)).doClick();
		Assertions.assertEquals(1, cleared.get());
	}


	private static void click(final Component component, final int count){
		final MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, count, false,
			MouseEvent.BUTTON1);
		for(final java.awt.event.MouseListener listener : component.getMouseListeners())
			listener.mouseClicked(event);
	}

	private static JPopupMenu menuOf(final java.awt.event.MouseListener listener){
		try{
			for(final java.lang.reflect.Field field : listener.getClass().getDeclaredFields())
				if(JPopupMenu.class.isAssignableFrom(field.getType())){
					field.setAccessible(true);
					return (JPopupMenu)field.get(listener);
				}
		}
		catch(final ReflectiveOperationException ignored){
			//the menu is not reachable
		}
		return null;
	}

}
