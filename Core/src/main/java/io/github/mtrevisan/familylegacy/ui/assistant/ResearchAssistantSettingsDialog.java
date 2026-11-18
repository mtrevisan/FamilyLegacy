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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.i18n.I18N;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;


/**
 * The settings of the Research Assistant: the indicator of the toolbar (shown or not, blinking or not), whether the palette opens again
 * after an action, and the assistants that look for findings, for the palette and for the indicator.
 * <p>
 * They are presentation settings: nothing changes in the research. Every choice is kept as soon as it is made (there is no Cancel; it is
 * a JOptionPane, which Esc closes), and used the next time the findings are computed.
 * </p>
 */
public final class ResearchAssistantSettingsDialog{

	private ResearchAssistantSettingsDialog(){}


	/** Shows the dialog. */
	public static void show(final Window owner, final ResearchAssistantService service){
		JOptionPane.showMessageDialog(owner, tabs(service), I18N.t("dialog.assistant_settings.title"), JOptionPane.PLAIN_MESSAGE);
	}

	/** @return the settings in tabs: the general ones, then the numbers of each assistant that has some */
	static JTabbedPane tabs(final ResearchAssistantService service){
		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(I18N.t("dialog.assistant_settings.general"), panel(service));
		for(final AssistantTuning.Group group : AssistantTuning.Group.values())
			tabs.addTab(group.title(), tuningPanel(service.settings(), group));
		return tabs;
	}

	/**
	 * @return the numbers of an assistant (the ages, the weights of the usefulness), each in its range, kept in the settings as soon as it
	 * is changed; a number that cannot be what it was typed (a medium level above the high one) is brought back, and shown so
	 */
	static JPanel tuningPanel(final AssistantSettings settings, final AssistantTuning.Group group){
		final JPanel panel = new JPanel(new GridBagLayout());
		final Map<AssistantTuning.Parameter, JSpinner> spinners = new EnumMap<>(AssistantTuning.Parameter.class);
		final boolean[] updating = {false};
		final AssistantTuning current = settings.tuning();
		int row = 0;
		for(final AssistantTuning.Parameter parameter : AssistantTuning.of(group)){
			final JSpinner spinner = new JSpinner(new SpinnerNumberModel(current.get(parameter), parameter.min(), parameter.max(), 1));
			((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);
			spinner.setToolTipText(I18N.tf("dialog.assistant_settings.range", parameter.min(), parameter.max(), parameter.defaultValue()));
			spinner.addChangeListener(e -> {
				if(updating[0])
					return;

				settings.setTuning(settings.tuning().with(parameter, (Integer)spinner.getValue()));
				showTuning(settings.tuning(), spinners, updating);
			});
			spinners.put(parameter, spinner);

			final GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridy = row ++;
			constraints.insets = new Insets(1, 4, 1, 4);
			constraints.anchor = GridBagConstraints.WEST;
			constraints.gridx = 0;
			panel.add(new JLabel(parameter.label()), constraints);
			constraints.gridx = 1;
			panel.add(spinner, constraints);
		}

		final JButton defaults = new JButton(I18N.t("dialog.identity_settings.defaults"));
		defaults.addActionListener(e -> {
			settings.setTuning(settings.tuning().withDefaults(group));
			showTuning(settings.tuning(), spinners, updating);
		});
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row ++;
		constraints.gridwidth = 2;
		constraints.insets = new Insets(10, 4, 4, 4);
		constraints.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel("<html><body style='width: 420px'>" + I18N.t("dialog.assistant_settings.tuning_note") + "</body></html>"), constraints);
		constraints.gridy = row;
		panel.add(defaults, constraints);
		return panel;
	}

	private static void showTuning(final AssistantTuning tuning, final Map<AssistantTuning.Parameter, JSpinner> spinners,
			final boolean[] updating){
		updating[0] = true;
		try{
			spinners.forEach((parameter, spinner) -> spinner.setValue(tuning.get(parameter)));
		}
		finally{
			updating[0] = false;
		}
	}

	/** @return the panel of the settings, that keeps every choice in the settings of the service as soon as it is made */
	static JPanel panel(final ResearchAssistantService service){
		final AssistantSettings settings = service.settings();
		final JPanel panel = new JPanel(new GridBagLayout());
		int row = 0;
		row = addFlag(panel, row, "dialog.assistant_settings.indicator", settings.isIndicatorVisible(), settings::setIndicatorVisible);
		row = addFlag(panel, row, "dialog.assistant_settings.blink", settings.isBlinkOnNewError(), settings::setBlinkOnNewError);
		row = addFlag(panel, row, "dialog.assistant_settings.reopen", settings.isReopenAfterAction(), settings::setReopenAfterAction);

		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row ++;
		constraints.gridx = 0;
		constraints.insets = new Insets(12, 4, 2, 4);
		constraints.anchor = GridBagConstraints.WEST;
		final JLabel assistants = new JLabel(I18N.t("dialog.assistant_settings.assistants"));
		assistants.setFont(assistants.getFont().deriveFont(Font.BOLD));
		panel.add(assistants, constraints);
		constraints.gridx = 1;
		panel.add(new JLabel(I18N.t("dialog.assistant_settings.palette")), constraints);
		constraints.gridx = 2;
		panel.add(new JLabel(I18N.t("dialog.assistant_settings.indicator_column")), constraints);
		//how long each took the last time: it tells which one to switch off on a big file
		constraints.gridx = 3;
		panel.add(new JLabel(I18N.t("dialog.assistant_settings.duration")), constraints);
		final Map<String, Long> durations = service.lastDurationsMillis();

		for(final AssistantContributor assistant : service.assistantsWithFindings()){
			final String id = assistant.id();
			final JCheckBox inPalette = new JCheckBox("", settings.isEnabled(id));
			final JCheckBox inIndicator = new JCheckBox("", settings.isEnabledForIndicator(id));
			//the indicator only counts what the palette would show
			inIndicator.setEnabled(inPalette.isSelected());
			inPalette.addActionListener(e -> {
				settings.setEnabled(id, inPalette.isSelected());
				inIndicator.setEnabled(inPalette.isSelected());
				inIndicator.setSelected(settings.isEnabledForIndicator(id));
			});
			inIndicator.addActionListener(e -> settings.setEnabledForIndicator(id, inIndicator.isSelected()));

			constraints.gridy = row ++;
			constraints.insets = new Insets(0, 4, 0, 4);
			constraints.gridx = 0;
			panel.add(new JLabel(assistant.name()), constraints);
			constraints.gridx = 1;
			panel.add(inPalette, constraints);
			constraints.gridx = 2;
			panel.add(inIndicator, constraints);
			constraints.gridx = 3;
			panel.add(new JLabel(duration(durations.get(id))), constraints);
		}

		//the counts of what the user does with the findings, to calibrate the numbers of the assistants: off until the user says so
		row = addFlag(panel, row, "dialog.assistant_settings.statistics", settings.isStatisticsEnabled(), settings::setStatisticsEnabled);
		final JButton statistics = new JButton(I18N.t("dialog.assistant_settings.statistics_show"));
		statistics.addActionListener(e -> AssistantStatisticsDialog.show(SwingUtilities.getWindowAncestor(statistics), service));
		constraints.gridy = row ++;
		constraints.gridx = 0;
		constraints.gridwidth = 4;
		constraints.insets = new Insets(2, 24, 2, 4);
		panel.add(statistics, constraints);

		constraints.gridy = row;
		constraints.gridx = 0;
		constraints.gridwidth = 4;
		constraints.insets = new Insets(12, 4, 4, 4);
		panel.add(new JLabel("<html><body style='width: 400px'>" + I18N.t("dialog.assistant_settings.note") + "</body></html>"), constraints);
		return panel;
	}


	/** @return the time of an assistant the last time it looked for findings, or a dash if it did not yet */
	static String duration(final Long millis){
		if(millis == null)
			return I18N.t("dialog.assistant_settings.duration.none");
		return (millis < 1_000L
			? I18N.tf("dialog.assistant_settings.duration.ms", millis)
			: I18N.tf("dialog.assistant_settings.duration.s", millis / 1_000.));
	}

	private static int addFlag(final JPanel panel, final int row, final String labelKey, final boolean selected,
			final Consumer<Boolean> setter){
		final JCheckBox box = new JCheckBox(I18N.t(labelKey), selected);
		box.addActionListener(e -> setter.accept(box.isSelected()));
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row;
		constraints.gridx = 0;
		constraints.gridwidth = 4;
		constraints.insets = new Insets(2, 4, 2, 4);
		constraints.anchor = GridBagConstraints.WEST;
		panel.add(box, constraints);
		return row + 1;
	}

}
