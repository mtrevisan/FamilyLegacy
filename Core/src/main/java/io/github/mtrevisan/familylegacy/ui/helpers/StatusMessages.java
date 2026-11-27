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

import java.util.function.Consumer;


/**
 * A way for the tools to tell the user, without a window to close, what they did: the main window shows the message for a few seconds
 * (what a quick entry created, and that Undo takes it back). Nothing is shown if the main window has not asked for the messages.
 */
public final class StatusMessages{

	private static final StatusMessages SHARED = new StatusMessages();


	private volatile Consumer<String> listener;


	private StatusMessages(){}


	/** @return	The messages of the application. */
	public static StatusMessages shared(){
		return SHARED;
	}


	/**
	 * Sets who shows the messages (the main window).
	 *
	 * @param listener	Shows a message ({@code null}: nobody does).
	 */
	public void setListener(final Consumer<String> listener){
		this.listener = listener;
	}

	/**
	 * Tells the user something.
	 *
	 * @param message	The message.
	 */
	public void show(final String message){
		final Consumer<String> current = listener;
		if(current != null && message != null)
			current.accept(message);
	}

}
