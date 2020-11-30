package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;


public class LocaleFilteredComboBox extends JComboBox<Locale>{

	private final Locale[] array;


	public LocaleFilteredComboBox(final Locale[] array){
		super(array);

		this.array = array;
		setEditable(true);
		setRenderer(new LocaleListCellRenderer());

		final JTextField textfield = (JTextField)getEditor().getEditorComponent();
		textfield.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent ke){
				SwingUtilities.invokeLater(() -> comboFilter(textfield.getText()));
			}
		});
	}

	public void comboFilter(final String enteredText){
		if(!isPopupVisible())
			showPopup();

		final Collection<Locale> filterArray = new ArrayList<>();
		for(final Locale locale : array)
			if(locale.getDisplayName().toLowerCase(Locale.ROOT).contains(enteredText.toLowerCase(Locale.ROOT)))
				filterArray.add(locale);

		if(!filterArray.isEmpty()){
			final DefaultComboBoxModel<Locale> model = (DefaultComboBoxModel<Locale>)getModel();
			model.removeAllElements();
			for(final Locale s : filterArray)
				model.addElement(s);

			final JTextField textfield = (JTextField)getEditor().getEditorComponent();
			textfield.setText(enteredText);
		}
	}


	private static class LocaleListCellRenderer implements ListCellRenderer<Locale>{
		private final JLabel itemLabel = new JLabel();

		@Override
		public Component getListCellRendererComponent(final JList<? extends Locale> list, final Locale value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			itemLabel.setText(value.getDisplayName());
			return itemLabel;
		}
	}
}
