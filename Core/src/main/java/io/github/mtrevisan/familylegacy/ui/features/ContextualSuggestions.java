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
package io.github.mtrevisan.familylegacy.ui.features;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.awt.Window;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;


/**
 * Where the editors ask which advanced concepts could help with the record they show, without depending on the Research Assistant that
 * knows it (the assistant depends on the editors): it is found as a {@link ServiceLoader} provider. The suggestions are the findings of
 * the assistant about the record, so they are the same the Research Assistant lists, shown where the user is working. The detection is
 * the one of the assistants (every assistant is a provider of findings): there is no second detection to keep aligned.
 */
public interface ContextualSuggestions{

	/**
	 * Looks for the suggestions about a record away from the event dispatch thread, and hands them over on it. Nothing is called back
	 * if there is nothing to suggest, or the assistant cannot tell now.
	 *
	 * @param owner    the editor, owner of what the suggestions open
	 * @param model    the model being edited
	 * @param recordId the record the editor shows
	 * @param callback receives the suggestions (never empty), on the event dispatch thread
	 */
	void suggest(Window owner, FLEFModel model, String recordId, Consumer<List<Suggestion>> callback);

	/** @return the provider of the suggestions, if there is one */
	static Optional<ContextualSuggestions> find(){
		return ServiceLoader.load(ContextualSuggestions.class).findFirst();
	}

}
