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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.TextValueVariantDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.VariantHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
/**
 * Panel for managing a list of {@code TEXT_VALUE_VARIANT} entries according to FLEF 0.1.1.
 * <p>
 * Provides:
 * <ul>
 *   <li>Add a new variant</li>
 *   <li>Edit an existing variant</li>
 *   <li>Remove a variant</li>
 * </ul>
 */
public class VariantListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -298718064629353117L;


	private static final String DOT = ".";

	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";


	private final String path;


	private final RecordTypeHandler<?> variantHandler = HandlerRegistry.getHandler(VariantHandler.TYPE);


	public VariantListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Variants", model);

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
	protected String getDisplay(final FLEFRecord variant){
		if(variant != null)
			return variantHandler.getDisplayText(variant, model);

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new variant and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final TextValueVariantDialog dialog = TextValueVariantDialog.createNew(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Text Variant entry not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		return showTextVariantDialog(existing);
	}

	/**
	 * Shows a dialog to create or edit a text variant entry.
	 *
	 * @param existing the existing text variant record, or {@code null} for a new one
	 * @return the (possibly updated) record, or {@code null} if cancelled
	 */
	private FLEFRecord showTextVariantDialog(FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Text Variant not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = TextValueVariantDialog.createEdit(parent, model, existing);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return existing;
	}

	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		final FLEFRecord variantPhonetic = FLEFRecordHelper.findChild(record, path + DOT + TAG_PHONETIC);
		final FLEFRecord variantTranscription = FLEFRecordHelper.findChild(record, path + DOT + TAG_TRANSCRIPTION);
		List<FLEFRecord> items = new ArrayList<>();
		items.add(variantPhonetic);
		items.add(variantTranscription);
		setItems(items);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
//		List<FLEFRecord> items = getItems();
//		if(!items.isEmpty()){
//			final FLEFRecord variant = FLEFRecordHelper.getOrCreateTargetNode(record, path);
//			for(final FLEFRecord item : items)
//				variant.addChild(item);
//		}
	}

}

