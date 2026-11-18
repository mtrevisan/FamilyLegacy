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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;


/**
 * Panel for a repeated plain-text field (e.g. {@code remark*: Text}): each item is a child with the tag of the field and the text as
 * its value.
 */
public class TextListPanel extends AbstractListPanel<FLEFRecord>{

	private final String tag;
	private final String itemLabelKey;


	/**
	 * @param tag	The tag of the field.
	 * @param parent	The parent window.
	 * @param panelTitle	The border title, or {@code null} for no border.
	 * @param itemLabelKey	The i18n key of the name of one item (used in the editor title and in the validation message).
	 */
	public TextListPanel(final String tag, final Window parent, final String panelTitle, final String itemLabelKey){
		super(parent, panelTitle, null);

		this.tag = tag;
		this.itemLabelKey = itemLabelKey;


		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		BindingsHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item(I18N.t("popupmenu.create_new"), this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::editItem);
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord item){
		final String text = item.getValue();
		return (StringUtils.isNotEmpty(text)
			? GUIHelper.limitTextLength(StringUtils.replaceChars(text, '\n', '|'))
			: "--");
	}

	@Override
	protected FLEFRecord showAddDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showTextDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord item){
		return showTextDialog(item);
	}

	private FLEFRecord showTextDialog(final FLEFRecord item){
		final JDialog dialog = new JDialog(parent, I18N.t(itemLabelKey), Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(GUIHelper.createLabelFieldLayout(10, "[]"));

		final BoundTextArea textArea = new BoundTextArea(tag, 6, 50);
		if(item != null)
			textArea.setText(item.getValue());
		GUIHelper.addComponent(dialog, textArea);

		final FLEFRecord[] result = {item};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(dialog,
			() -> {
				final String text = StringUtils.trimToNull(textArea.getText());
				if(text == null){
					GUIHelper.showValidationErrorAndFocus(dialog,
						I18N.tf("validation.required", I18N.t(itemLabelKey)),
						null, null, textArea);

					return;
				}

				if(item == null)
					result[0] = FLEFRecord.createChildWithTagAndValue(tag, text);
				else
					item.setValue(text);

				dialog.dispose();
			},
			dialog::dispose);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	/** Loads the items from the children of the record with the tag of the field. */
	public void load(final FLEFRecord record){
		clear();

		if(record == null)
			return;

		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, tag)){
			final FLEFRecord copy = FLEFRecord.createEmpty();
			child.deepCopyTo(copy);
			addItemDirectly(copy);
		}
	}

	/** Replaces the children of the record with the tag of the field with the items. */
	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, tag);
		for(final FLEFRecord item : getItems())
			record.addChild(item);
	}

}
