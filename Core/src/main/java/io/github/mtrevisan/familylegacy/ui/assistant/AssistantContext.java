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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.Optional;


/**
 * A snapshot of where the user is when the Research Assistant is opened: it is captured before the palette takes the focus,
 * so that the actions act on the editor the user was working in.
 *
 * @param window     the window that had the focus (can be {@code null})
 * @param focusOwner the component that had the focus (can be {@code null})
 * @param model      the model being edited (can be {@code null})
 * @param scope      what identifies the file being edited, to keep the choices of the user about it (the path of the file), or
 *                   {@code null} if it was never saved
 */
public record AssistantContext(Window window, Component focusOwner, FLEFModel model, String scope){

	/** A context of a file that was never saved. */
	public AssistantContext(final Window window, final Component focusOwner, final FLEFModel model){
		this(window, focusOwner, model, null);
	}

	/**
	 * Finds the editor the focus is in.
	 *
	 * @param type the type of the editor (usually an interface implemented by the editor)
	 * @return the closest component, starting from the one that has the focus, that is of the given type
	 */
	public <T> Optional<T> focusedAncestor(final Class<T> type){
		Component component = focusOwner;
		while(component != null){
			if(type.isInstance(component))
				return Optional.of(type.cast(component));

			component = component.getParent();
		}
		return Optional.empty();
	}

	/** @return the window to use as the owner of the dialogs opened by an action */
	public Window owner(){
		return (window != null || focusOwner == null? window: SwingUtilities.getWindowAncestor(focusOwner));
	}

}
