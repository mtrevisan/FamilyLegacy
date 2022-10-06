package io.github.mtrevisan.familylegacy.ui.utilities;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import java.awt.Component;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;


public class LocaleFilteredComboBox extends JComboBox<Locale>{

	//TODO preload on startup (progress bar)
	private static final Locale[] LOCALE_ITEMS;
	static{
		final Locale[] availableLocales = DateFormat.getAvailableLocales();
		final Collection<Locale> list = new ArrayList<>(availableLocales.length);
		for(final Locale availableLocale : availableLocales)
			if(availableLocale.toLanguageTag() != null)
				list.add(availableLocale);
		LOCALE_ITEMS = list.toArray(Locale[]::new);
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(locale -> getLocaleDisplayText(locale).toLowerCase(Locale.ROOT)));
	}


	public LocaleFilteredComboBox(){
		super(LOCALE_ITEMS);

		setRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				final Locale locale = (Locale)value;
				if(locale != null){
					final String text = getLocaleDisplayText(locale);
					setText(text);
				}
				return this;
			}
		});
		AutoCompleteDecorator.decorate(this, new ObjectToStringConverter(){
			@Override
			public String getPreferredStringForItem(final Object item){
				return getLocaleDisplayText((Locale)item);
			}
		});
	}

	private static String getLocaleDisplayText(final Locale locale){
		return locale.getDisplayName();
	}

	public final String getSelectedLanguageTag(){
		final Object selectedItem = getSelectedItem();
		return (selectedItem != null? ((Locale)selectedItem).toLanguageTag(): StringUtils.EMPTY);
	}

	public final void setSelectedByLanguageTag(String languageTag){
		if(languageTag != null){
			languageTag = languageTag.toLowerCase(Locale.ROOT);
			final ComboBoxModel<Locale> model = getModel();
			for(int index = 0; index < model.getSize(); index ++){
				final Locale locale = model.getElementAt(index);
				if(locale.toLanguageTag().toLowerCase(Locale.ROOT).equals(languageTag)){
					setSelectedItem(locale);
					break;
				}
			}
		}
	}

}
