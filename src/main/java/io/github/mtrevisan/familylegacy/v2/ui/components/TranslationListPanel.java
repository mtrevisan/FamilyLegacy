package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


public class TranslationListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -2934528588234172844L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_LOCALE = "LOCALE";


	private final String path;


	public TranslationListPanel(final String path, Dialog parent, FLEFModel model){
		super(parent, "Translations", model);

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
	protected String getDisplay(FLEFRecord item){
		final String value = FLEFRecordHelper.getChildValue(item, TAG_VALUE);
		final String locale = FLEFRecordHelper.getChildValue(item, TAG_LOCALE);

		StringBuilder sb = new StringBuilder();
		if(!StringUtils.isEmpty(locale)){
			sb.append("[").append(locale).append("] ");
		}
		if(!StringUtils.isEmpty(value)){
			String display = value;
			if(display.length() > 50){
				display = display.substring(0, 47) + "...";
			}
			sb.append(display);
		}
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return showTranslationDialog(null);
	}

	/**
	 * Creates a new translation and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		//TODO
		return null;
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		return showTranslationDialog(existing);
	}

	private FLEFRecord showTranslationDialog(FLEFRecord initial){
		JDialog dialog = new JDialog(parent, initial == null? "Add Translation": "Edit Translation", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		final String value = FLEFRecordHelper.getChildValue(initial, TAG_VALUE);
		final String locale = FLEFRecordHelper.getChildValue(initial, TAG_LOCALE);

		BoundTextArea valueArea = new BoundTextArea(TAG_VALUE, 3, 25);
		if(initial != null){
			valueArea.setText(value);
		}
		dialog.add(new JLabel("Value*:"), "align label,top");
		dialog.add(GUIHelper.createScrollPane(valueArea), "growx,wrap");

		BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !StringUtils.isEmpty(locale)){
			localeCombo.setSelectedItem(locale);
		}
		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(StringUtils.isEmpty(valueArea.getValue())){
				JOptionPane.showMessageDialog(dialog, "Translation value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			final FLEFRecord res = FLEFRecord.createEmpty();
			res.addChild(FLEFRecord.createChildWithValue(TAG_VALUE, valueArea.getValue()));
			res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
			result[0] = res;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> translations = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, path)){
			final String translationValue = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			final String translationLocale = FLEFRecordHelper.getChildValue(child, TAG_LOCALE);
			if(StringUtils.isNotEmpty(translationValue)){
				final FLEFRecord res = FLEFRecord.createEmpty();
				res.addChild(FLEFRecord.createChildWithValue(TAG_VALUE, translationValue));
				res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, translationLocale));
				translations.add(res);
			}
		}
		setItems(translations);
	}

	public void save(final FLEFRecord record){
		super.save(record, path);
//		final List<FLEFRecord> translations = getItems();
//		for(int i = 0; i < translations.size(); i ++){
//			final FLEFRecord entry = translations.get(i);
//			final String value = FLEFRecordHelper.getChildValue(entry, TAG_VALUE);
//			final String locale = FLEFRecordHelper.getChildValue(entry, TAG_LOCALE);
//
//			FLEFRecordHelper.addChild(record, "TRANSLATION[" + i + "].VALUE", value);
//			FLEFRecordHelper.addChild(record, "TRANSLATION[" + i + "].LOCALE", locale);
//		}
	}

}
