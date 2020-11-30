package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;


public class LocaleFilteredComboBox extends JComboBox<Locale>{

	private static final Locale[] LOCALE_ITEMS;
	static{
		final Locale[] availableLocales = DateFormat.getAvailableLocales();
		final Collection<Locale> list = new ArrayList<>(availableLocales.length);
		for(final Locale availableLocale : availableLocales)
			if(availableLocale.toLanguageTag() != null)
				list.add(availableLocale);
		LOCALE_ITEMS = list.toArray(Locale[]::new);
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(Locale::getDisplayName));
	}


	private final JTextField field;


	public LocaleFilteredComboBox(){
		super(LOCALE_ITEMS);

		setEditable(true);
		setRenderer(new LocaleListCellRenderer());
		setEditor(new LocaleComboBoxEditor());

		setFixedWidth();

		field = (JTextField)getEditor().getEditorComponent();
		field.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent ke){
				SwingUtilities.invokeLater(() -> comboFilter(field.getText()));
			}
		});
	}

	/** Fix size of combobox to maximum size required to show the longest name. */
	private void setFixedWidth(){
		double maxWidth = 0.;
		Locale maxLocale = Locale.US;
		for(final Locale locale : LOCALE_ITEMS){
			final double w = FontHelper.getStringBounds(getFont(), locale.getDisplayName()).getWidth();
			if(w > maxWidth){
				maxWidth = w;
				maxLocale = locale;
			}
		}
		setPrototypeDisplayValue(maxLocale);
	}

	private void comboFilter(final String enteredText){
		if(!isPopupVisible())
			showPopup();

		//filter
		final Collection<Locale> filterArray = new ArrayList<>();
		for(final Locale locale : LOCALE_ITEMS)
			if(locale.getDisplayName().toLowerCase(Locale.ROOT).contains(enteredText.toLowerCase(Locale.ROOT)))
				filterArray.add(locale);

		if(!filterArray.isEmpty()){
			final DefaultComboBoxModel<Locale> model = (DefaultComboBoxModel<Locale>)getModel();
			model.removeAllElements();
			model.addAll(filterArray);

			field.setText(enteredText);
		}
	}

	public void setSelectedByLanguageTag(String languageTag){
		languageTag = languageTag.toLowerCase(Locale.ROOT);
		final DefaultComboBoxModel<Locale> model = (DefaultComboBoxModel<Locale>)getModel();
		for(int index = 0; index < model.getSize(); index ++){
			final Locale locale = model.getElementAt(index);
			if(locale.toLanguageTag().toLowerCase(Locale.ROOT).equals(languageTag)){
				setSelectedItem(locale);
				break;
			}
		}
	}


	private static class LocaleListCellRenderer extends DefaultListCellRenderer{
		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus){
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			setText(((Locale)value).getDisplayName());
			return this;
		}
	}

	private static class LocaleComboBoxEditor extends BasicComboBoxEditor{
		private final JLabel label = new JLabel();
		private final JTextField editorComponent = new JTextField();
		private Object selectedItem;


		LocaleComboBoxEditor(){
			label.setOpaque(false);
			label.setBorder(null);

			editorComponent.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
			editorComponent.setBorder(null);
			editorComponent.add(label);
		}

		@Override
		public Component getEditorComponent(){
			return editorComponent;
		}

		@Override
		public Object getItem(){
			return selectedItem;
		}

		@Override
		public void setItem(final Object item){
			selectedItem = item;
			label.setText(((Locale)item).getDisplayName());
		}

	}

}
