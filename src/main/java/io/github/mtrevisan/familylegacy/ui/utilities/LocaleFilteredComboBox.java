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

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;


public class LocaleFilteredComboBox extends JComboBox<LocaleFilteredComboBox.MyLocale>{

	private static final MyLocale[] LOCALE_ITEMS;
	static{
		final Locale[] availableLocales = DateFormat.getAvailableLocales();
		final Collection<MyLocale> list = new ArrayList<>(availableLocales.length);
		for(final Locale availableLocale : availableLocales)
			if(availableLocale.toLanguageTag() != null)
				list.add(new MyLocale(availableLocale));
		LOCALE_ITEMS = list.toArray(MyLocale[]::new);
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(ml -> ml.toString().toLowerCase(Locale.ROOT)));
	}

	protected static final class MyLocale{
		private final Locale locale;

		MyLocale(final Locale locale){
			this.locale = locale;
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
				SwingUtilities.invokeLater(() -> comboFilter(field.getText()));
			}
		});
	}

	/** Fix size of combobox to maximum size required to show the longest name. */
	private void setFixedWidth(){
		double maxWidth = 0.;
		MyLocale maxLocale = new MyLocale(Locale.US);
		for(final MyLocale locale : LOCALE_ITEMS){
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
		final Collection<MyLocale> filterArray = new ArrayList<>();
		for(final MyLocale locale : LOCALE_ITEMS)
			if(locale.toString().toLowerCase(Locale.ROOT).contains(enteredText.toLowerCase(Locale.ROOT)))
				filterArray.add(locale);

		if(!filterArray.isEmpty()){
			final DefaultComboBoxModel<MyLocale> model = (DefaultComboBoxModel<MyLocale>)getModel();
			model.removeAllElements();
			model.addAll(filterArray);

			field.setText(enteredText);
		}
	}

	public void setSelectedByLanguageTag(String languageTag){
		languageTag = languageTag.toLowerCase(Locale.ROOT);
		MyLocale locale = new MyLocale(Locale.ROOT);
		final DefaultComboBoxModel<MyLocale> model = (DefaultComboBoxModel<MyLocale>)getModel();
		for(int index = 0; index < model.getSize(); index ++){
			final MyLocale loc = model.getElementAt(index);
			if(loc.toString().toLowerCase(Locale.ROOT).equals(languageTag)){
				locale = loc;
				break;
			}
		}
		setSelectedItem(locale);
	}

}
