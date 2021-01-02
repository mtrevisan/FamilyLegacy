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

import io.github.mtrevisan.familylegacy.services.JavaHelper;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;


public class LocaleFilteredComboBox extends JComboBox<LocaleFilteredComboBox.FlefLocale>{

	//TODO preload on startup (progress bar)
	private static final FlefLocale[] LOCALE_ITEMS;
	static{
		final Locale[] availableLocales = DateFormat.getAvailableLocales();
		final Collection<FlefLocale> list = new ArrayList<>(availableLocales.length);
		for(final Locale availableLocale : availableLocales)
			if(availableLocale.toLanguageTag() != null)
				list.add(new FlefLocale(availableLocale));
		LOCALE_ITEMS = list.toArray(FlefLocale[]::new);
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(ml -> ml.toString().toLowerCase(Locale.ROOT)));
	}

	public static final class FlefLocale{
		private final Locale locale;

		FlefLocale(final Locale locale){
			this.locale = locale;
		}

		public String toLanguageTag(){
			return locale.toLanguageTag();
		}

		@Override
		public String toString(){
			return locale.getDisplayName();
		}
	}


	private final JTextField field;


	public LocaleFilteredComboBox(){
		super(LOCALE_ITEMS);

		setEditable(true);

		setFixedWidth();

		field = (JTextField)getEditor().getEditorComponent();
		field.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent ke){
				JavaHelper.executeOnEventDispatchThread(() -> comboFilter(field.getText()));
			}
		});
	}

	/** Fix size of combobox to maximum size required to show the longest name. */
	private void setFixedWidth(){
		double maxWidth = 0.;
		FlefLocale maxLocale = new FlefLocale(Locale.US);
		for(final FlefLocale locale : LOCALE_ITEMS){
			final double w = FontHelper.getStringBounds(getFont(), locale.toString()).getWidth();
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
		final Collection<FlefLocale> filterArray = new ArrayList<>();
		for(final FlefLocale locale : LOCALE_ITEMS)
			if(locale.toString().toLowerCase(Locale.ROOT).contains(enteredText.toLowerCase(Locale.ROOT)))
				filterArray.add(locale);

		if(!filterArray.isEmpty()){
			final DefaultComboBoxModel<FlefLocale> model = (DefaultComboBoxModel<FlefLocale>)getModel();
			model.removeAllElements();
			model.addAll(filterArray);

			field.setText(enteredText);
		}
	}

	public String getSelectedLanguageTag(){
		return ((LocaleFilteredComboBox.FlefLocale)getModel().getSelectedItem())
			.toLanguageTag();
	}

	public void setSelectedByLanguageTag(String languageTag){
		languageTag = languageTag.toLowerCase(Locale.ROOT);
		FlefLocale locale = new FlefLocale(Locale.ROOT);
		final DefaultComboBoxModel<FlefLocale> model = (DefaultComboBoxModel<FlefLocale>)getModel();
		for(int index = 0; index < model.getSize(); index ++){
			final FlefLocale loc = model.getElementAt(index);
			if(loc.toLanguageTag().toLowerCase(Locale.ROOT).equals(languageTag)){
				locale = loc;
				break;
			}
		}
		setSelectedItem(locale);
	}

}
