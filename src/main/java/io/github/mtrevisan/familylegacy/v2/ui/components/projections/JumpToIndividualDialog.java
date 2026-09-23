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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Opens the "Jump to Individual" dialog and returns the chosen id, or
 * {@code null} when the user cancels. In the ego network projection the
 * dialog also offers groups.
 */
public final class JumpToIndividualDialog{

	private JumpToIndividualDialog(){}


	/**
	 * @param owner      the parent window; may be {@code null}
	 * @param model      the model to search in
	 * @param projection the active projection; only
	 *                   {@link ProjectionType#EGO_NETWORK} adds groups
	 *                   to the record types offered
	 * @return the chosen record id, or {@code null} if cancelled
	 */
	public static String showAndGet(final Window owner, final FLEFModel model,
		final ProjectionType projection){
		final List<Class<? extends RecordTypeHandler<?>>> handlerList = new ArrayList<>();
		handlerList.add(IndividualHandler.class);
		if(projection == ProjectionType.EGO_NETWORK)
			handlerList.add(GroupHandler.class);

		@SuppressWarnings("unchecked")
		final Class<? extends RecordTypeHandler<?>>[] handlers = handlerList.toArray(new Class[0]);

		final String[] result = {null};
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			owner, model,
			(record, handler) -> {
				if(record != null && record.getId() != null)
					result[0] = record.getId();
			},
			handlers);
		dialog.setVisible(true);

		return result[0];
	}

}
