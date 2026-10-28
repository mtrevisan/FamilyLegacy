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
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * "Open the records of the finding": the editor of the record, if the finding is about one, or a list of the records to open one
 * after the other, to look at them side by side while deciding what is wrong.
 * <p>
 * It only opens the usual editors: nothing changes unless the user edits a record and saves it there.
 * </p>
 */
final class OpenRecordsAction implements DiagnosticAction{

	private final List<String> recordIds;


	OpenRecordsAction(final List<String> recordIds){
		this.recordIds = List.copyOf(recordIds);
	}


	@Override
	public String title(){
		return I18N.t("assistant.open_records");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records of the finding may have been changed or deleted since it was computed: the ones that are gone are left out
		final List<FLEFRecord> records = records(model, recordIds);
		if(records.isEmpty())
			JOptionPane.showMessageDialog(context.owner(), I18N.t("assistant.open_records.gone"), title(),
				JOptionPane.INFORMATION_MESSAGE);
		else if(records.size() == 1)
			open(context.owner(), model, records.getFirst());
		else
			new RecordChooserDialog(context.owner(), model, records, OpenRecordsAction::open).setVisible(true);
	}


	/** @return the records that exist and can be edited (the ones that have an editor), in the order of the identifiers */
	static List<FLEFRecord> records(final FLEFModel model, final List<String> recordIds){
		final List<FLEFRecord> records = new ArrayList<>();
		for(final String id : recordIds){
			final FLEFRecord record = model.getRecordById(id);
			if(record != null && record.getTag() != null && HandlerRegistry.getHandler(record.getTag()) != null
					&& !records.contains(record))
				records.add(record);
		}
		return records;
	}

	/** @return the name of a record in a list: its type, what it is about, and its identifier */
	static String describe(final FLEFModel model, final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		String text = null;
		try{
			text = (handler != null? handler.getDisplayText(record, model): null);
		}
		catch(final RuntimeException ignored){
			//a record with an unexpected shape is still listed, by its identifier
		}
		final String type = (handler != null? handler.getLabel(): record.getTag());
		return type + ": " + (text != null && !text.isBlank()? text + " ": "") + "[" + record.getId() + "]";
	}

	private static void open(final Window owner, final FLEFModel model, final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler != null)
			handler.createEditDialog(owner, model, record).setVisible(true);
	}

}
