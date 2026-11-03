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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;

import java.awt.Window;
import java.util.List;


/**
 * Opens the "Find record" dialog (Ctrl+F) and returns the chosen record with its handler, or {@code null} when the user cancels.
 * <p>
 * Unlike {@link JumpToIndividualDialog}, that looks for individuals (and groups), it looks for any record that can be searched
 * ({@link SearchService#searchableHandlers()}): the type is chosen in the dialog, with its own filters, and the text query is scored
 * by the same matcher.
 */
public final class FindRecordDialog{

	/**
	 * The record found.
	 *
	 * @param record   the record chosen
	 * @param handler  the handler of its type
	 */
	public record Found(FLEFRecord record, RecordTypeHandler<?> handler){}


	private FindRecordDialog(){}


	/**
	 * @param owner  the parent window; may be {@code null}
	 * @param model  the model to search in
	 * @return	The record chosen, {@code null} if cancelled.
	 */
	@SuppressWarnings("unchecked")
	public static Found showAndGet(final Window owner, final FLEFModel model){
		final List<Class<? extends RecordTypeHandler<?>>> handlerList = SearchService.searchableHandlers();
		final Class<? extends RecordTypeHandler<?>>[] handlers = handlerList.toArray(new Class[0]);

		final Found[] result = {null};
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(owner, model,
			(record, handler) -> {
				if(record != null && record.getId() != null)
					result[0] = new Found(record, handler);
			},
			handlers);
		dialog.setVisible(true);

		return result[0];
	}

}
