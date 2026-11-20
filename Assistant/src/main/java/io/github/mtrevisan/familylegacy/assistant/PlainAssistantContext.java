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

import java.util.function.Supplier;


/**
 * A context with no user interface: what the checks of the assistant need (the model, the file, the selected record), e.g. to run them
 * from a test or from a command line.
 *
 * @param model            the model being edited (can be {@code null})
 * @param scope            what identifies the file being edited, or {@code null} if it was never saved
 * @param snapshots        gives a copy of the model that nobody changes (see {@link AssistantContext#forReading()}), or {@code null} if the
 *                         model is read as it is
 * @param selectedRecordId the identifier of the record selected, or {@code null} if nothing is selected
 */
public record PlainAssistantContext(FLEFModel model, String scope, Supplier<FLEFModel> snapshots, String selectedRecordId)
		implements AssistantContext{

	/** A context of a file that was never saved. */
	public PlainAssistantContext(final FLEFModel model){
		this(model, null);
	}

	/** A context whose model is read as it is (it is not copied: use it where nobody edits the model meanwhile). */
	public PlainAssistantContext(final FLEFModel model, final String scope){
		this(model, scope, null, null);
	}

	/** A context with no record selected. */
	public PlainAssistantContext(final FLEFModel model, final String scope, final Supplier<FLEFModel> snapshots){
		this(model, scope, snapshots, null);
	}


	@Override
	public AssistantContext with(final FLEFModel model, final Supplier<FLEFModel> snapshots){
		return new PlainAssistantContext(model, scope, snapshots, selectedRecordId);
	}

}
