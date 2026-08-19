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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for managing a list of translations with value and locale.
 */
public class TranslationListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -2934528588234172844L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_LOCALE = "LOCALE";


	private final String path;


	public TranslationListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem, null,
			this::createNewItem, this::removeItem,
			builder -> {
				builder.item("Create New…", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit…", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	@Override
	protected String getDisplayText(final FLEFRecord item){
		if(item != null){
			final String value = FLEFRecordHelper.getChildValue(item, TAG_VALUE);
			final String locale = FLEFRecordHelper.getChildValue(item, TAG_LOCALE);

			final StringBuilder sb = new StringBuilder();
			if(StringUtils.isNotEmpty(locale))
				sb.append('[')
					.append(locale)
					.append("] ");
			if(StringUtils.isNotEmpty(value))
				sb.append(GUIHelper.limitTextLength(value));
			return sb.toString();
		}

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return showTranslationDialog(null);
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showTranslationDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		return showTranslationDialog(record);
	}

	private FLEFRecord showTranslationDialog(final FLEFRecord initial){
		final JDialog dialog = new JDialog(parent, initial == null? "Add Translation": "Edit Translation", true);
		GUIHelper.setLayoutLabelFieldPanel(dialog, 10, "[]10[]");

		final String value = FLEFRecordHelper.getChildValue(initial, TAG_VALUE);
		final String locale = FLEFRecordHelper.getChildValue(initial, TAG_LOCALE);

		final BoundTextArea valueArea = new BoundTextArea(TAG_VALUE, 3, 25);
		if(initial != null){
			valueArea.setText(value);
		}
		GUIHelper.addLabeledComponent(dialog, "Value*:", valueArea);

		final BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY,
			"en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && StringUtils.isNotEmpty(locale))
			localeCombo.setSelectedItem(locale);
		GUIHelper.addLabeledComponent(dialog, "Locale:", localeCombo);

		final FLEFRecord[] result = {null};
		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			() -> {
				if(valueArea.isEmpty()){
					JOptionPane.showMessageDialog(dialog,
						"Translation value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);

					return;
				}
				final FLEFRecord res = FLEFRecord.createEmpty();
				res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_VALUE, valueArea.getText()));
				res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
				result[0] = res;

				dialog.dispose();
			},
			dialog::dispose);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	public void load(final FLEFRecord record){
		clear();

		final List<FLEFRecord> translations = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, path)){
			final String translationValue = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			final String translationLocale = FLEFRecordHelper.getChildValue(child, TAG_LOCALE);
			if(StringUtils.isNotEmpty(translationValue)){
				final FLEFRecord res = FLEFRecord.createEmpty();
				res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_VALUE, translationValue));
				res.addChild(FLEFRecord.createChildWithTagAndValue(TAG_LOCALE, translationLocale));
				translations.add(res);
			}
		}
		setItems(translations);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
	}

}
