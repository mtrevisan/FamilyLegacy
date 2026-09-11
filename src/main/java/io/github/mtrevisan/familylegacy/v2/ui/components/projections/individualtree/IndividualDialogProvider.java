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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import java.awt.Window;
import java.util.Locale;


/**
 * Centralizes the three individual-related dialogs used by the ancestor
 * tree: creation, editing, and search.
 * <p>
 * The provider was extracted from {@code IndividualTreePanel} to reduce the
 * responsibilities of that class and to make the dialog flow reusable from
 * other views that operate on the same records (e.g. the Ego Network and
 * the Social Network views). Instances are immutable and thread-safe.
 */
final class IndividualDialogProvider{

	private static final String TAG_SEX = "sex";


	private final FLEFModel model;


	/**
	 * Constructor.
	 *
	 * @param model the FLEF model (must not be {@code null})
	 */
	IndividualDialogProvider(final FLEFModel model){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");

		this.model = model;
	}


	/**
	 * Opens the edit dialog for the given individual.
	 *
	 * @param parent     the parent window; may be {@code null}
	 * @param individual the individual to edit (must not be {@code null})
	 * @return the edited record, or {@code null} if the user canceled
	 */
	FLEFRecord showEditDialog(final Window parent, final FLEFRecord individual){
		if(individual == null)
			return null;

		final IndividualHandler handler = IndividualHandler.getInstance();
		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, individual);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	/**
	 * Opens the creation dialog for a new individual.
	 *
	 * @param parent the parent window; may be {@code null}
	 * @param sex    the sex to pre-select, or {@code null} to leave it empty
	 * @return the created record, or {@code null} if the user canceled
	 */
	FLEFRecord showCreateDialog(final Window parent, final SexType sex){
		final IndividualHandler handler = IndividualHandler.getInstance();
		final IndividualRecordDialog dialog = handler.createNewDialog(parent, model)
			.witSex(sex);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	/**
	 * Opens the search dialog for an existing individual.
	 *
	 * @param parent the parent window; may be {@code null}
	 * @param sex    the sex to filter by, or {@code null} to show all
	 * @return the selected record, or {@code null} if the user canceled
	 */
	FLEFRecord showSearchDialog(final Window parent, final SexType sex){
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		if(sex != null)
			dialog.withFilter(TAG_SEX, sex.name()
				.toLowerCase(Locale.ROOT));
		dialog.setVisible(true);

		return result[0];
	}

}
