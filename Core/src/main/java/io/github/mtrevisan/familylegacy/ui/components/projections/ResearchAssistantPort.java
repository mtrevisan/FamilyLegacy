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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import javax.swing.JComponent;
import java.awt.Window;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


/**
 * What the main window needs from the Research Assistant, without depending on it: the assistant depends on the editors and the views of
 * the application, so the window finds it, as a {@link ServiceLoader} provider, instead of naming it.
 */
public interface ResearchAssistantPort{

	/** The count of the findings, to show in the toolbar. */
	interface Indicator{

		/** @return the component to put in the toolbar (a click opens the assistant) */
		JComponent component();

		/** Computes the findings again, soon: it is called when the model changes. */
		void refreshSoon();

	}


	/**
	 * Makes the assistant available from every window with its shortcut.
	 *
	 * @param modelSupplier gives the model being edited
	 * @param scopeSupplier gives what identifies the file being edited (its path), or {@code null} if it was never saved
	 */
	void install(Supplier<FLEFModel> modelSupplier, Supplier<String> scopeSupplier);

	/** Opens the assistant on what the user is doing now. */
	void open();

	/**
	 * @param window             the window the indicator is in
	 * @param modelSupplier      gives the model being edited
	 * @param scopeSupplier      gives what identifies the file being edited, or {@code null} if it was never saved
	 * @param modalDialogShowing whether the user is busy in a modal dialog, so that the findings are not computed now
	 * @return the indicator of the findings
	 */
	Indicator createIndicator(Window window, Supplier<FLEFModel> modelSupplier, Supplier<String> scopeSupplier,
		BooleanSupplier modalDialogShowing);

	/** @return the assistant of the application, if there is one */
	static Optional<ResearchAssistantPort> find(){
		return ServiceLoader.load(ResearchAssistantPort.class).findFirst();
	}

}
