/**
 * Copyright (c) 2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class DatePanel extends JPanel{

	//https://thenounproject.com/term/weekly-calendar/541199/
	private static final ImageIcon ICON_DATE = ResourceHelper.getImage("/images/date.png", 20, 20);

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});


	private final JTextField dateField = new JTextField();
	private final JTextField dateOriginalTextField = new JTextField();
	private final JComboBox<String> dateCredibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private String calendarXRef;


	public DatePanel(){
		super(new MigLayout(StringUtils.EMPTY, "[grow]"));

		final JLabel dateLabel = new JLabel("Date:");
		dateLabel.setLabelFor(dateField);
		final JLabel dateCredibilityLabel = new JLabel("Credibility:");
		dateCredibilityLabel.setLabelFor(dateCredibilityComboBox);

		setBorder(BorderFactory.createTitledBorder("Date"));
		add(dateLabel, "align label,split 3,sizegroup label");
		add(dateField, "grow");
		final JButton dateButton = new JButton(ICON_DATE);
		add(dateButton, "wrap");
		final JLabel dateOriginalTextLabel = new JLabel("Original text:");
		add(dateOriginalTextLabel, "align label,split 2,sizegroup label");
		add(dateOriginalTextField, "grow,wrap");
		add(dateCredibilityLabel, "align label,split 2,sizegroup label");
		add(dateCredibilityComboBox, "grow");
	}

	public final void loadData(final String date, final String calendarXRef, final String dateOriginalText, final int dateCredibilityIndex){
		dateField.setText(date);
		this.calendarXRef = calendarXRef;
		dateOriginalTextField.setText(dateOriginalText);
		dateCredibilityComboBox.setSelectedIndex(dateCredibilityIndex);
	}

	public String getDate(){
		return dateField.getText();
	}

	public String getCalendarXRef(){
		return calendarXRef;
	}

	public String getDateOriginalText(){
		return dateOriginalTextField.getText();
	}

	public int getDateCredibility(){
		return dateCredibilityComboBox.getSelectedIndex();
	}

}