package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
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


public class TranslationListPanel extends AbstractListPanel<TranslationListPanel.TranslationEntry>{

	@Serial
	private static final long serialVersionUID = -2934528588234172844L;


	public static class TranslationEntry{
		private final String locale;
		private final String value;

		public TranslationEntry(String locale, String value){
			this.locale = (locale != null ? locale : StringUtils.EMPTY);
			this.value = (value != null ? value : StringUtils.EMPTY);
		}

		public String getLocale(){
			return locale;
		}

		public String getValue(){
			return value;
		}

	}

	public TranslationListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Translations");
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
			result[0] = new TranslationEntry(locale != null && !locale.isEmpty() ? locale : null, value);
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parentDialog);
		dialog.setVisible(true);
		return result[0];
	}

}
