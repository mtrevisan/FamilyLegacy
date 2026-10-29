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
package io.github.mtrevisan.familylegacy.ui.dialogs.api;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.awt.Window;


/**
 * What the code that opens the editor of a record needs of it: to show it, to know whether the user saved the record, and which record
 * it is about. It is what the handlers hand out, so that the handlers do not depend on the editors (the editors depend on the handlers).
 * <p>
 * Whoever needs more (a draft, a tab to show) knows the editor it opens: it can ask the editor class for it, or cast.
 * </p>
 */
public interface RecordDialog{

	/** Shows or hides the editor (a modal editor returns when the user closes it). */
	void setVisible(boolean visible);

	/** Closes the editor and releases its resources. */
	void dispose();

	/** @return whether the user saved the record */
	boolean isSaved();

	/** @return the record the editor is about (the new one, for a new record) */
	FLEFRecord getRecord();

	/** @return the window of the editor */
	Window window();

	/**
	 * Presets a record in the part it plays (the editors that have no such part ignore it).
	 *
	 * @param role   the part
	 * @param record the record
	 */
	default void link(final LinkRole role, final FLEFRecord record){}

	/**
	 * Makes a change of a record nested in this one (an item of one of its lists) undoable from this editor.
	 *
	 * @param target  the nested record that changed
	 * @param before  how it was
	 * @param after   how it is
	 * @param refresh refreshes the list that shows the nested record
	 */
	default void addNestedEdit(final FLEFRecord target, final FLEFRecord before, final FLEFRecord after, final Runnable refresh){}

}
