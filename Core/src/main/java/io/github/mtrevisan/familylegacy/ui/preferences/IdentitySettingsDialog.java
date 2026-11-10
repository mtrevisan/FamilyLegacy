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
package io.github.mtrevisan.familylegacy.ui.preferences;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.quality.IdentitySettings;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;


/**
 * The settings of the Identity Assistant: from which similarity a pair is proposed, how many, the scores of the confidence and the
 * probability for the other kinds of record. They are saved when the dialog is closed (there is no Cancel; it is a JOptionPane, which
 * Esc closes), and used the next time the findings are computed.
 */
final class IdentitySettingsDialog{

	private IdentitySettingsDialog(){}


	/** Shows the dialog, and saves the settings if the user confirms. */
	static void show(final Window owner){
		final IdentitySettings current = IdentitySettings.load();
		final JSpinner threshold = spinner(current.threshold(), IdentitySettings.MIN_THRESHOLD, IdentitySettings.MAX_THRESHOLD);
		final JSpinner maxPairs = spinner(current.maxPairs(), IdentitySettings.MIN_MAX_PAIRS, IdentitySettings.MAX_MAX_PAIRS);
		final JSpinner high = spinner(current.highScore(), IdentitySettings.MIN_THRESHOLD, IdentitySettings.MAX_THRESHOLD);
		final JSpinner medium = spinner(current.mediumScore(), IdentitySettings.MIN_THRESHOLD, IdentitySettings.MAX_THRESHOLD);
		final JSpinner other = spinner(current.otherPercent(), IdentitySettings.MIN_OTHER_PERCENT, IdentitySettings.MAX_OTHER_PERCENT);

		final JButton defaults = new JButton(I18N.t("dialog.identity_settings.defaults"));
		defaults.addActionListener(e -> {
			threshold.setValue(IdentitySettings.DEFAULT_THRESHOLD);
			maxPairs.setValue(IdentitySettings.DEFAULT_MAX_PAIRS);
			high.setValue(IdentitySettings.DEFAULT_HIGH_SCORE);
			medium.setValue(IdentitySettings.DEFAULT_MEDIUM_SCORE);
			other.setValue(IdentitySettings.DEFAULT_OTHER_PERCENT);
		});

		final JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		row = addRow(panel, row, "dialog.identity_settings.threshold", threshold);
		row = addRow(panel, row, "dialog.identity_settings.max_pairs", maxPairs);
		row = addRow(panel, row, "dialog.identity_settings.high_score", high);
		row = addRow(panel, row, "dialog.identity_settings.medium_score", medium);
		row = addRow(panel, row, "dialog.identity_settings.other_percent", other);
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(8, 4, 4, 4);
		panel.add(new JLabel("<html><body style='width: 380px'>" + I18N.t("dialog.identity_settings.note") + "</body></html>"), constraints);
		constraints.gridy = row + 1;
		panel.add(defaults, constraints);

		//there is no Cancel: whatever way the dialog is closed (OK or Esc), the values shown are the settings
		JOptionPane.showMessageDialog(owner, panel, I18N.t("dialog.identity_settings.title"), JOptionPane.PLAIN_MESSAGE);
		read(threshold, maxPairs, high, medium, other)
			.save();
	}

	/** @return	The settings the spinners show (in range, and the medium score not above the high one). */
	static IdentitySettings read(final JSpinner threshold, final JSpinner maxPairs, final JSpinner high, final JSpinner medium,
			final JSpinner other){
		return new IdentitySettings((Integer)threshold.getValue(), (Integer)maxPairs.getValue(), (Integer)high.getValue(),
			(Integer)medium.getValue(), (Integer)other.getValue());
	}


	private static JSpinner spinner(final int value, final int min, final int max){
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
		//all the spinners have the same width, whatever the number of digits of their values
		((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);
		return spinner;
	}

	private static int addRow(final JPanel panel, final int row, final String labelKey, final JSpinner spinner){
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row;
		constraints.insets = new Insets(2, 4, 2, 4);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0;
		panel.add(new JLabel(I18N.t(labelKey)), constraints);
		constraints.gridx = 1;
		panel.add(spinner, constraints);
		return row + 1;
	}

}
