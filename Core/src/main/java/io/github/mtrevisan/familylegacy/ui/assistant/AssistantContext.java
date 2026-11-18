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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * A snapshot of where the user is when the Research Assistant is opened: it is captured before the palette takes the focus,
 * so that the actions act on the editor the user was working in.
 *
 * @param window     the window that had the focus (can be {@code null})
 * @param focusOwner the component that had the focus (can be {@code null})
 * @param model      the model being edited (can be {@code null})
 * @param scope      what identifies the file being edited, to keep the choices of the user about it (the path of the file), or
 *                   {@code null} if it was never saved
 * @param snapshots  gives a copy of the model that nobody changes (see {@link #forReading()}), or {@code null} if the model is read as
 *                   it is
 * @param selectedRecordId the identifier of the record selected in the view of the main window (the individual or the group the user
 *                   is looking at), or {@code null} if nothing is selected or the assistant is opened from an editor
 */
public record AssistantContext(Window window, Component focusOwner, FLEFModel model, String scope, Supplier<FLEFModel> snapshots,
		String selectedRecordId){

	/** Marks the context of a copy of the model that nobody changes (see {@link #forReading()}): it is not asked for another copy. */
	private static final Supplier<FLEFModel> COPY_TAKEN = () -> {
		throw new IllegalStateException("The model of this context is already a copy");
	};

	/** A context of a file that was never saved. */
	public AssistantContext(final Window window, final Component focusOwner, final FLEFModel model){
		this(window, focusOwner, model, null);
	}

	/** A context whose model is read as it is (it is not copied: use it where nobody edits the model meanwhile). */
	public AssistantContext(final Window window, final Component focusOwner, final FLEFModel model, final String scope){
		this(window, focusOwner, model, scope, null);
	}

	/** A context with no record selected. */
	public AssistantContext(final Window window, final Component focusOwner, final FLEFModel model, final String scope,
			final Supplier<FLEFModel> snapshots){
		this(window, focusOwner, model, scope, snapshots, null);
	}

	/**
	 * @return	The selected record, if there is one and the model has it (a record deleted since is not selected any more).
	 */
	public Optional<FLEFRecord> selectedRecord(){
		return (selectedRecordId != null && model != null? Optional.ofNullable(model.getRecordById(selectedRecordId)): Optional.empty());
	}

	/**
	 * The context for something that reads the model away from the event dispatch thread (the diagnostics), while the user may be
	 * editing it: the same context, but with a copy of the model as it is now, that nobody changes. The model is changed in place, from
	 * many places, so a reader that is not on the thread of the edits would see a half-changed model.
	 * <p>
	 * Call it on the event dispatch thread, before going away from it, and hand the result to the reader. It is for <b>reading</b>
	 * only: an action must be given the original context, because it has to act on the model of the file and not on a copy. What a
	 * finding says about a record is its identifier, so it is found again in the original model when the user acts on it.
	 * </p>
	 *
	 * @return	This context if the model is read as it is.
	 */
	public AssistantContext forReading(){
		return (snapshots == null || model == null || snapshots == COPY_TAKEN
			? this
			: new AssistantContext(window, focusOwner, snapshots.get(), scope, COPY_TAKEN, selectedRecordId));
	}

	/**
	 * @return	Whether the model is a copy that nobody changes, taken by {@link #forReading()}: what is computed on it stays true for as long as
	 * 	the copy is the one given (the history gives the same copy until the model changes), so it can be kept and shared.
	 */
	public boolean isReadingCopy(){
		return (snapshots == COPY_TAKEN);
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
