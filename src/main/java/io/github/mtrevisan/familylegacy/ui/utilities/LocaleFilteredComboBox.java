/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(locale -> LocaleRenderer.getLocaleDisplayText(locale).toLowerCase(Locale.ROOT)));
	}


	private final JTextField field;


	public LocaleFilteredComboBox(){
		super(LOCALE_ITEMS);

//		setModel(new LocaleFilteredComboBoxModel(this));
		setRenderer(new LocaleRenderer());

		setFixedWidth();

		field = (JTextField)getEditor().getEditorComponent();
		field.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent ke){
				comboFilter(field.getText());
			}
		});

		setEditable(true);
	}

	/** Fix size of combo-box to maximum size required to show the longest name. */
	private void setFixedWidth(){
		double maxWidth = 0.;
		Locale maxLocale = Locale.US;
		for(final Locale locale : LOCALE_ITEMS){
			final double w = FontHelper.getStringBounds(getFont(), LocaleRenderer.getLocaleDisplayText(locale)).getWidth();
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
		final Collection<Locale> filterArray = new ArrayList<>(LOCALE_ITEMS.length);
		for(final Locale locale : LOCALE_ITEMS)
			if(LocaleRenderer.getLocaleDisplayText(locale).toLowerCase(Locale.ROOT).contains(enteredText.toLowerCase(Locale.ROOT)))
				filterArray.add(locale);

		if(!filterArray.isEmpty()){
			final DefaultComboBoxModel<Locale> model = (DefaultComboBoxModel<Locale>)getModel();
			model.removeAllElements();
			model.addAll(filterArray);

			field.setText(enteredText);
		}
	}

	public String getSelectedLanguageTag(){
		final Object originalSelectedItem = getModel().getSelectedItem();
		if(originalSelectedItem instanceof Locale selectedItem)
			return selectedItem.toLanguageTag();
		return StringUtils.EMPTY;
	}

	public void setSelectedByLanguageTag(String languageTag){
		if(languageTag != null){
			languageTag = languageTag.toLowerCase(Locale.ROOT);
			Locale locale = Locale.ROOT;
			final DefaultComboBoxModel<Locale> model = (DefaultComboBoxModel<Locale>)getModel();
			for(int index = 0; index < model.getSize(); index ++){
				final Locale loc = model.getElementAt(index);
				if(loc.toLanguageTag().toLowerCase(Locale.ROOT).equals(languageTag)){
					locale = loc;
					break;
				}
			}
			setSelectedItem(locale);
		}
	}


	private static class LocaleRenderer extends DefaultListCellRenderer{

		@Override
		public final Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus){
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			final Locale locale = (Locale)value;
			if(locale != null){
				final String text = getLocaleDisplayText(locale);
				setText(text);
			}
			return this;
		}

		static <T> String getLocaleDisplayText(final T locale){
			return ((Locale)locale).getDisplayName();
		}

	}

}
