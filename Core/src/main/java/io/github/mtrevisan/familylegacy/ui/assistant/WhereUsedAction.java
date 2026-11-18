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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;


/**
 * "Where used": for each record of a finding, the records that point to it, to see what depends on a duplicate or on a record with a
 * problem before deciding what to do with it. It only shows and opens the usual editors: nothing changes unless the user edits a
 * record and saves it there.
 * <p>
 * It is not one of the actions of a finding (those have the four keys of Enter): the palette has its own key for it.
 * </p>
 */
final class WhereUsedAction implements DiagnosticAction{

	private final List<String> recordIds;


	WhereUsedAction(final List<String> recordIds){
		this.recordIds = List.copyOf(recordIds);
	}


	@Override
	public String title(){
		return I18N.t("assistant.where_used");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records of the finding may have been deleted since it was computed: the ones that are gone are left out
		final List<FLEFRecord> records = new ArrayList<>();
		for(final String id : recordIds){
			final FLEFRecord record = model.getRecordById(id);
			if(record != null && !records.contains(record))
				records.add(record);
		}
		if(records.isEmpty())
			JOptionPane.showMessageDialog(context.owner(), I18N.t("assistant.open_records.gone"), title(), JOptionPane.INFORMATION_MESSAGE);
		else
			new WhereUsedDialog(context.owner(), model, records).setVisible(true);
	}

}
