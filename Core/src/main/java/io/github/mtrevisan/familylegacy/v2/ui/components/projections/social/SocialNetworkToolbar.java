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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.util.Calendar;
import java.util.Date;


/**
 * Toolbar of the Social Network view.
 * <p>
 * Exposes the controls that drive the graph construction and the layout:
 * <ul>
 *   <li>focus picker — opens a record selection dialog and notifies the
 *       panel of the chosen entity;</li>
 *   <li>max degree spinner — controls the BFS expansion radius;</li>
 *   <li>layout mode combo — {@link SocialLayoutMode};</li>
 *   <li>include inactive checkbox — whether ended relationships are
 *       considered;</li>
 *   <li>include empty role checkbox — whether relationships without an
 *       explicit role are considered;</li>
 *   <li>categories button — opens a filter dialog for the categories;</li>
 *   <li>path finder button — opens the path finder dialog;</li>
 *   <li>fit button — recenters and rescales the graph.</li>
 * </ul>
 */
public final class SocialNetworkToolbar extends JPanel{

	/**
	 * Listener for toolbar actions.
	 */
	public interface Listener{
		void onPickFocus();

		void onMaxDegreeChanged(int maxDegree);

		void onLayoutModeChanged(SocialLayoutMode mode);

		void onIncludeInactiveChanged(boolean value);

		void onIncludeEmptyRoleChanged(boolean value);

		void onCategoriesRequested();

		void onPathFinderRequested();

		void onFitRequested();

		/**
		 * Notifies that the temporal window has changed.
		 *
		 * @param enabled whether the temporal filter is active
		 * @param from    the lower bound; may be {@code null} when the filter
		 *                is disabled
		 * @param to      the upper bound; may be {@code null} when the filter
		 *                is disabled
		 */
		void onTemporalWindowChanged(boolean enabled, Date from, Date to);
	}


	private final JButton focusBtn = new JButton("Pick focus…");
	private final JLabel focusLabel = new JLabel("(none)");
	private final JSpinner maxDegreeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 6, 1));
	private final JComboBox<SocialLayoutMode> layoutCombo = new JComboBox<>(SocialLayoutMode.values());
	private final JCheckBox includeInactiveCheck = new JCheckBox("Ended");
	private final JCheckBox includeEmptyRoleCheck = new JCheckBox("Empty role", true);
	private final JButton categoriesBtn = new JButton("Categories…");
	private final JButton pathFinderBtn = new JButton("Path finder…");
	private final JButton fitBtn = new JButton("Fit");

	private final JCheckBox temporalFilterCheck = new JCheckBox("Dates");
	private final JSpinner fromDateSpinner = createDateSpinner(defaultFromDate());
	private final JSpinner toDateSpinner = createDateSpinner(defaultToDate());


	public SocialNetworkToolbar(final Listener listener){
		initComponents();
		installListeners(listener);
	}


	/* ======================================================================
	 *                          State accessors
	 * ====================================================================== */

	public void setFocusLabel(final String text){
		focusLabel.setText(text != null && !text.isBlank()? text: "(none)");
	}

	public int getMaxDegree(){
		return (Integer)maxDegreeSpinner.getValue();
	}

	public void setMaxDegree(final int maxDegree){
		maxDegreeSpinner.setValue(Math.max(0, maxDegree));
	}

	public SocialLayoutMode getLayoutMode(){
		return (SocialLayoutMode)layoutCombo.getSelectedItem();
	}

	public void setLayoutMode(final SocialLayoutMode mode){
		if(mode != null)
			layoutCombo.setSelectedItem(mode);
	}

	public boolean isIncludeInactive(){
		return includeInactiveCheck.isSelected();
	}

	public void setIncludeInactive(final boolean value){
		includeInactiveCheck.setSelected(value);
	}

	public boolean isIncludeEmptyRole(){
		return includeEmptyRoleCheck.isSelected();
	}

	public void setIncludeEmptyRole(final boolean value){
		includeEmptyRoleCheck.setSelected(value);
	}


	/* ======================================================================
	 *                          Setup
	 * ====================================================================== */

	private void initComponents(){
		setLayout(new BorderLayout());

		final JPanel content = new JPanel(new MigLayout("ins 6,gapx 6", "[]", "[]"));
		content.add(focusBtn);
		content.add(focusLabel);
		content.add(new JLabel("  Degree:"));
		content.add(maxDegreeSpinner);
		content.add(new JLabel("  Layout:"));
		content.add(layoutCombo);
		content.add(new JLabel("  Show:"));
		content.add(includeInactiveCheck);
		content.add(includeEmptyRoleCheck);
		content.add(new JLabel("  Dates:"));
		content.add(temporalFilterCheck);
		content.add(fromDateSpinner);
		content.add(new JLabel("–"));
		content.add(toDateSpinner);
		content.add(new JLabel("  "));
		content.add(categoriesBtn);
		content.add(pathFinderBtn);
		content.add(fitBtn);

		add(content, BorderLayout.WEST);
	}

	private void installListeners(final Listener listener){
		if(listener == null)
			return;

		focusBtn.addActionListener(e -> listener.onPickFocus());
		maxDegreeSpinner.addChangeListener(e -> listener.onMaxDegreeChanged(getMaxDegree()));
		layoutCombo.addActionListener(e -> listener.onLayoutModeChanged(getLayoutMode()));
		includeInactiveCheck.addActionListener(e -> listener.onIncludeInactiveChanged(isIncludeInactive()));
		includeEmptyRoleCheck.addActionListener(e -> listener.onIncludeEmptyRoleChanged(isIncludeEmptyRole()));
		categoriesBtn.addActionListener(e -> listener.onCategoriesRequested());
		pathFinderBtn.addActionListener(e -> listener.onPathFinderRequested());
		fitBtn.addActionListener(e -> listener.onFitRequested());
	}

	private static JSpinner createDateSpinner(final Date initial){
		final SpinnerDateModel model = new SpinnerDateModel(initial,
			null, null, Calendar.YEAR);
		final JSpinner spinner = new JSpinner(model);
		final JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
		spinner.setEditor(editor);
		final JFormattedTextField field = editor.getTextField();
		field.setColumns(10);
		field.setHorizontalAlignment(JTextField.CENTER);
		spinner.setEnabled(false);
		return spinner;
	}

	private static Date defaultFromDate(){
		final Calendar c = Calendar.getInstance();
		c.clear();
		c.set(1800, Calendar.JANUARY, 1);
		return c.getTime();
	}

	private static Date defaultToDate(){
		final Calendar c = Calendar.getInstance();
		c.clear();
		c.set(2000, Calendar.DECEMBER, 31);
		return c.getTime();
	}

	/**
	 * Returns whether the temporal filter is currently active.
	 */
	public boolean isTemporalFilterEnabled(){
		return temporalFilterCheck.isSelected();
	}

	/**
	 * Returns the lower bound of the temporal window, or {@code null} if the
	 * filter is disabled.
	 */
	public Date getFromDate(){
		return (isTemporalFilterEnabled()? (Date)fromDateSpinner.getValue(): null);
	}

	/**
	 * Returns the upper bound of the temporal window, or {@code null} if the
	 * filter is disabled.
	 */
	public Date getToDate(){
		return (isTemporalFilterEnabled()? (Date)toDateSpinner.getValue(): null);
	}

	/**
	 * Sets the temporal window and enables the filter.
	 *
	 * @param from the lower bound; may be {@code null} to leave the default
	 * @param to   the upper bound; may be {@code null} to leave the default
	 */
	public void setTemporalWindow(final Date from, final Date to){
		if(from != null)
			fromDateSpinner.setValue(from);
		if(to != null)
			toDateSpinner.setValue(to);
		temporalFilterCheck.setSelected(true);
		fromDateSpinner.setEnabled(true);
		toDateSpinner.setEnabled(true);
	}

	/**
	 * Disables the temporal filter without changing the stored dates.
	 */
	public void clearTemporalWindow(){
		temporalFilterCheck.setSelected(false);
		fromDateSpinner.setEnabled(false);
		toDateSpinner.setEnabled(false);
	}

}
