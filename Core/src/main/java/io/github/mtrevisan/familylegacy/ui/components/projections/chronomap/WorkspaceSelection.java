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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Shared selection state between the chronomap and the agora panels.
 *
 * <p>When the user selects an individual in one view, the other view is
 * notified through the listeners registered here. The selection is a
 * simple identifier, so the state is a single string; {@code null} means
 * "no selection".</p>
 *
 * <p>The class is not thread-safe: all invocations must happen on the
 * Event Dispatch Thread. This matches the behaviour of every Swing model
 * that the surrounding panels use.</p>
 */
public final class WorkspaceSelection{

	private final List<Consumer<String>> listeners = new ArrayList<>();

	private String selectedId;


	/** Returns the currently selected identifier, or {@code null}. */
	public String get(){
		return selectedId;
	}


	/**
	 * Sets the selection. Listeners are notified only when the value
	 * actually changes, so repeated selections of the same identifier
	 * do not trigger redundant repaints.
	 *
	 * @param id the identifier to select, or {@code null} to clear the
	 *           selection
	 */
	public void select(final String id){
		if(Objects.equals(id, selectedId))
			return;

		selectedId = id;
		for(final Consumer<String> l : listeners)
			l.accept(id);
	}


	/** Clears the selection. */
	public void clear(){
		select(null);
	}


	/**
	 * Registers a listener that is notified whenever the selection
	 * changes. The listener receives the new identifier, or {@code null}
	 * when the selection is cleared.
	 */
	public void addListener(final Consumer<String> listener){
		if(listener != null && !listeners.contains(listener))
			listeners.add(listener);
	}


	/** Removes a previously registered listener. */
	public void removeListener(final Consumer<String> listener){
		listeners.remove(listener);
	}

}
