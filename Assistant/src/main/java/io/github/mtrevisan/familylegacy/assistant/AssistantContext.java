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
package io.github.mtrevisan.familylegacy.assistant;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * Where the user is when the Research Assistant is opened, as far as the checks of the assistant are concerned: it knows nothing of
 * Swing, so the checks can run (and be tested) without a user interface. The user interface adds the window and the focus owner
 * ({@code SwingAssistantContext}), which only the actions that open dialogs need.
 */
public interface AssistantContext{

	/** Marks the context of a copy of the model that nobody changes (see {@link #forReading()}): it is not asked for another copy. */
	Supplier<FLEFModel> COPY_TAKEN = () -> {
		throw new IllegalStateException("The model of this context is already a copy");
	};


	/** @return the model being edited (can be {@code null}) */
	FLEFModel model();

	/**
	 * @return what identifies the file being edited, to keep the choices of the user about it (the path of the file), or {@code null}
	 * 	if it was never saved
	 */
	String scope();

	/**
	 * @return gives a copy of the model that nobody changes (see {@link #forReading()}), or {@code null} if the model is read as it is
	 */
	Supplier<FLEFModel> snapshots();

	/**
	 * @return the identifier of the record selected in the view of the main window (the individual or the group the user is looking
	 * 	at), or {@code null} if nothing is selected or the assistant is opened from an editor
	 */
	String selectedRecordId();

	/**
	 * @param model	The model of the new context.
	 * @param snapshots	The snapshots of the new context.
	 * @return	A context like this one (same scope, same selected record, same user interface) with the given model and snapshots.
	 */
	AssistantContext with(FLEFModel model, Supplier<FLEFModel> snapshots);


	/** @return the record selected in the view of the main window, if any */
	default Optional<FLEFRecord> selectedRecord(){
		final String selectedRecordId = selectedRecordId();
		final FLEFModel model = model();
		return (selectedRecordId != null && model != null? Optional.ofNullable(model.getRecordById(selectedRecordId)): Optional.empty());
	}

	/**
	 * A context to read, not to edit, from a thread other than the event dispatch thread: its model is a copy that nobody changes (a
	 * consistent view), taken now, so it must be called on the event dispatch thread. The copy is shared by whoever asks for it while
	 * the model does not change, so it is cheap to ask for it more than once.
	 *
	 * @return	This context if the model is read as it is.
	 */
	default AssistantContext forReading(){
		final Supplier<FLEFModel> snapshots = snapshots();
		final FLEFModel model = model();
		return (snapshots == null || model == null || snapshots == COPY_TAKEN
			? this
			: with(snapshots.get(), COPY_TAKEN));
	}

	/**
	 * @return	Whether the model is a copy that nobody changes, taken by {@link #forReading()}: what is computed on it stays true for as long as
	 * 	the copy is the one given (the history gives the same copy until the model changes), so it can be kept and shared.
	 */
	default boolean isReadingCopy(){
		return (snapshots() == COPY_TAKEN);
	}

}
