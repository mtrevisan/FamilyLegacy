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

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;


/**
 * One press of ESC closes one dialog.
 * <p>
 * Every dialog closes with ESC, and only itself. But a key that is held down repeats its press, and when the dialog closes the
 * repeated presses reach the dialog below it, and the one below that: holding ESC a moment too long would close the whole chain of
 * dialogs that opened one from the other. The repeated presses are consumed here, before any dialog sees them: the key has to be
 * released, and pressed again, to close the next one.
 * </p>
 */
public final class DialogEscapeSupport{

	/**
	 * A press of the key that comes before this time (in milliseconds) from the previous one, without a release in between, is a
	 * repetition. The delay before a key starts repeating is up to a second on the platforms; if a release is lost (the window lost the
	 * focus with the key down) the next press is taken as a new one after this time.
	 */
	static final long REPEAT_WINDOW_MILLIS = 1_500L;

	private static boolean installed;


	private DialogEscapeSupport(){}


	/** Makes ESC close one dialog at a time, in the whole application (once: calling it again does nothing). */
	public static synchronized void install(){
		if(installed)
			return;

		installed = true;
		final Gate gate = new Gate();
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.addKeyEventDispatcher(gate::consumes);
	}


	/** Tells the repeated presses of ESC from the first one. */
	static final class Gate{

		private boolean down;
		private long lastPress;


		/**
		 * @param event a key event
		 * @return whether the event is a repeated press of ESC, that must not reach the dialogs
		 */
		synchronized boolean consumes(final KeyEvent event){
			if(event.getKeyCode() != KeyEvent.VK_ESCAPE)
				return false;

			if(event.getID() == KeyEvent.KEY_RELEASED){
				down = false;
				return false;
			}
			if(event.getID() != KeyEvent.KEY_PRESSED)
				return false;

			//a repeated press goes on being one while the key is held, however long
			final boolean repeated = (down && event.getWhen() - lastPress < REPEAT_WINDOW_MILLIS);
			down = true;
			lastPress = event.getWhen();
			return repeated;
		}

	}

}
