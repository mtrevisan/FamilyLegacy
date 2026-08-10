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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PartStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
public class PartStructureListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 2221818245328724967L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";


	static{
		HandlerRegistry.register(new PartHandler());
	}


	private final String path;


	public PartStructureListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Parts*", model);

		this.path = path;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord part){
		if(part == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(part, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
		final StringBuilder sb = new StringBuilder();

		if(StringUtils.isNotEmpty(type))
			sb.append("[")
				.append(type)
				.append("] ");
		sb.append(StringUtils.defaultString(value));

		final List<FLEFRecord> phonetics = FLEFRecordHelper.findChildren(part, TAG_PHONETIC);
		final List<FLEFRecord> transcriptions = FLEFRecordHelper.findChildren(part, TAG_TRANSCRIPTION);
		final int variantCount = phonetics.size() + transcriptions.size();
		if(variantCount > 0)
			sb.append(" (")
				.append(variantCount)
				.append(" variants)");
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final RecordTypeHandler<?> partHandler = HandlerRegistry.getHandler(PartHandler.TYPE);
		final PartStructureDialog dialog = (PartStructureDialog)partHandler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Part not found", "Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> partHandler = HandlerRegistry.getHandler(PartHandler.TYPE);
		final PartStructureDialog dialog = (PartStructureDialog)partHandler.createEditDialog(parent, model, existing);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final List<FLEFRecord> parts = FLEFRecordHelper.findChildren(record, path);
		setItems(parts);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
//		record.addChildren(getItems());
	}

	public boolean hasData(){
		return !isEmpty();
	}

	public boolean validateData(){
		for(final FLEFRecord part : getItems()){
			final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(StringUtils.isEmpty(value)){
				JOptionPane.showMessageDialog(parent,
					"Part has no value.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);

				return false;
			}
		}
		return true;
	}

}
