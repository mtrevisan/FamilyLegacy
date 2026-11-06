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
package io.github.mtrevisan.familylegacy.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;

import java.util.function.Predicate;


/**
 * Finds the record that a report on a place, a source or a research question is about. These records cannot be selected in the views,
 * as a person or a group can: the one that is selected is used if it is of the right kind, otherwise the user picks one from the list.
 */
final class ReportSubjectPicker{

	private ReportSubjectPicker(){}


	/**
	 * @param context	The context.
	 * @param isOfKind	Tells whether a record is of the kind the report is about.
	 * @param handler	The handler of that kind, that lists the records to pick from.
	 * @return	The record, or {@code null} if the user did not pick one.
	 */
	@SuppressWarnings("unchecked")
	static FLEFRecord pick(final ToolContext context, final Predicate<FLEFRecord> isOfKind,
			final Class<? extends RecordTypeHandler<?>> handler){
		final FLEFRecord selected = (context.hasSelectedEntity()? context.getSelectedEntity(): null);
		if(selected != null && isOfKind.test(selected))
			return selected;

		final RecordSelectionDialog dialog = RecordSelectionDialog.create(context.owner(), context.model(), (record, type) -> {}, handler);
		dialog.setVisible(true);
		return (dialog.isConfirmed()? dialog.getSelectedRecord(): null);
	}

}
