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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import java.util.ArrayList;
import java.util.List;


/**
 * Browser-style navigation history.
 * <p>
 * The history is a linear list of entries with a cursor. Pushing a new
 * entry after a "back" truncates the forward branch, exactly like a
 * browser. Consecutive duplicate pushes are ignored.
 * <p>
 * The class is mutable and not thread-safe; use it from the EDT.
 */
public final class NavigationHistory{

	private final List<String> entries = new ArrayList<>();
	private int cursor = -1;


	/**
	 * Pushes a new entry. When {@code id} equals the current entry the
	 * call is a no-op. Otherwise the forward branch (if any) is discarded
	 * and the new entry becomes the current one.
	 *
	 * @param id the entry to push; {@code null} is ignored
	 */
	public void push(final String id){
		if(id == null)
			return;
		if(cursor >= 0 && entries.get(cursor).equals(id))
			return;

		while(entries.size() > cursor + 1)
			entries.removeLast();

		entries.add(id);
		cursor = entries.size() - 1;
	}

	public boolean canGoBack(){
		return (cursor > 0);
	}

	public boolean canGoForward(){
		return (cursor < entries.size() - 1);
	}

	/**
	 * Moves the cursor one step back and returns the previous entry.
	 *
	 * @return the previous entry, or {@code null} when there is none
	 */
	public String goBack(){
		if(!canGoBack())
			return null;

		cursor --;

		return entries.get(cursor);
	}

	/**
	 * Moves the cursor one step forward and returns the next entry.
	 *
	 * @return the next entry, or {@code null} when there is none
	 */
	public String goForward(){
		if(!canGoForward())
			return null;

		cursor ++;

		return entries.get(cursor);
	}

	/** Returns the current entry, or {@code null} when the history is empty. */
	public String current(){
		return (cursor >= 0? entries.get(cursor): null);
	}

	/** Returns an immutable snapshot of the entries, for debugging. */
	public List<String> entries(){
		return List.copyOf(entries);
	}

	/** Returns the current cursor, for debugging. */
	public int cursor(){
		return cursor;
	}

	/**
	 * Clears the history: both the entries and the cursor are reset, so
	 * {@link #canGoBack()} and {@link #canGoForward()} return {@code false}
	 * afterwards and the next {@link #push(String)} starts a fresh timeline.
	 * <p>
	 * The method does not touch the currently visible view: after the call,
	 * the user is still looking at the same root, but the navigation
	 * shortcuts have nothing to go back to or forward to.
	 */
	public void clear(){
		entries.clear();
		cursor = -1;
	}

}
