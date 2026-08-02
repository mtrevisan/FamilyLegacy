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


public class TranslationListPanel extends AbstractListPanel<TranslationListPanel.TranslationEntry>{

	@Serial
	private static final long serialVersionUID = -2934528588234172844L;


	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_LOCALE = "LOCALE";


	public static class TranslationEntry{
		private final String locale;
		private final String value;

		public TranslationEntry(String value, String locale){
			this.locale = StringUtils.defaultString(locale);
			this.value = StringUtils.defaultString(value);
		}

		public String getLocale(){
			return locale;
		}

		public String getValue(){
			return value;
		}

	}

	private final String path;


	public TranslationListPanel(final String path, FLEFModel model, Dialog parentDialog){
		super(parentDialog, "Translations", model);

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
	protected String getDisplay(TranslationEntry item){
		StringBuilder sb = new StringBuilder();
		if(!item.locale.isEmpty()){
			sb.append("[").append(item.locale).append("] ");
		}
		String display = item.value;
		if(display.length() > 50){
			display = display.substring(0, 47) + "...";
		}
		sb.append(display);
		return sb.toString();
	}

	@Override
	protected TranslationEntry showAddDialog(){
		return showTranslationDialog(null);
	}

	/**
	 * Creates a new translation and adds it to the list.
	 */
	@Override
	protected TranslationEntry showCreateNewDialog(){
		//TODO
		return null;
	}

	@Override
	protected TranslationEntry showEditDialog(TranslationEntry existing){
		return showTranslationDialog(existing);
	}

	private TranslationEntry showTranslationDialog(TranslationEntry initial){
		JDialog dialog = new JDialog(parentDialog, initial == null ? "Add Translation" : "Edit Translation", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		BoundTextArea valueArea = new BoundTextArea("VALUE", 5, 25);
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		if(initial != null){
			valueArea.setText(initial.value);
		}
		dialog.add(new JLabel("Value*:"), "align label,top");
		dialog.add(GUIHelper.createScrollPane(valueArea), "growx,wrap");

		BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !initial.locale.isEmpty()){
			localeCombo.setSelectedItem(initial.locale);
		}
		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final TranslationEntry[] result = {null};
		okBtn.addActionListener(e -> {
			String locale = (String)localeCombo.getSelectedItem();
			String value = valueArea.getText().trim();
			if(value.isEmpty()){
				JOptionPane.showMessageDialog(dialog, "Translation value cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			result[0] = new TranslationEntry(value, locale != null && !locale.isEmpty() ? locale : null);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);

		return result[0];
	}

	public void load(final FLEFRecord record){
		final List<TranslationEntry> translations = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, path)){
			final String translationValue = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			final String translationLocale = FLEFRecordHelper.getChildValue(child, TAG_LOCALE);
			if(StringUtils.isNotEmpty(translationValue))
				translations.add(new TranslationEntry(translationValue, translationLocale));
		}
		setItems(translations);
	}

	public void save(final FLEFRecord record){
		final List<TranslationEntry> translations = getItems();
		for(int i = 0; i < translations.size(); i ++){
			final TranslationEntry entry = translations.get(i);
			FLEFRecordHelper.addChild(record, "TRANSLATION[" + i + "].VALUE", entry.getValue());
			if(StringUtils.isNotEmpty(entry.getLocale()))
				FLEFRecordHelper.addChild(record, "TRANSLATION[" + i + "].LOCALE", entry.getLocale());
		}
	}

}
