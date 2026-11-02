/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;


/**
 * The choice of the year the group views are restricted to: a box "Any year" and a year, that is used when the box is not checked. It is
 * shared by the timeline of a group, the career of an individual and the network of the groups.
 */
public final class GroupYearControl extends JPanel{

	private static final int INITIAL_YEAR = 1900;

	private final JCheckBox anyYear = new JCheckBox(I18N.t("label.group_year.any"), true);
	private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(INITIAL_YEAR, -9999, 9999, 1));

	private Runnable listener;
	private boolean silent;


	public GroupYearControl(){
		super(new FlowLayout(FlowLayout.LEFT, 6, 0));

		//a year is not a quantity: no thousands separator
		yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
		yearSpinner.setEnabled(false);

		add(anyYear);
		add(new JLabel(I18N.t("label.group_year.year")));
		add(yearSpinner);

		anyYear.addActionListener(e -> {
			yearSpinner.setEnabled(!anyYear.isSelected());
			notifyListener();
		});
		yearSpinner.addChangeListener(e -> notifyListener());
	}


	/**
	 * Tells what to do when the year changes (the view has to be shown again).
	 *
	 * @param action	What to do.
	 * @return	This control.
	 */
	public GroupYearControl withListener(final Runnable action){
		listener = action;

		return this;
	}

	/** @return	The year, {@code null} for any. */
	public Integer getYear(){
		return (anyYear.isSelected()? null: (Integer)yearSpinner.getValue());
	}

	/**
	 * Sets the year, and tells the listener once.
	 *
	 * @param year	The year, {@code null} for any.
	 */
	public void setYear(final Integer year){
		silent = true;
		try{
			anyYear.setSelected(year == null);
			yearSpinner.setEnabled(year != null);
			if(year != null)
				yearSpinner.setValue(year);
		}
		finally{
			silent = false;
		}
		notifyListener();
	}


	private void notifyListener(){
		if(!silent && listener != null)
			listener.run();
	}

}
