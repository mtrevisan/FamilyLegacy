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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.reports.ReportPreferences;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * The saved sets of options of a type of report: a list to choose one (it is applied at once), a button that saves the options as they
 * are under a name and a button that deletes the chosen one.
 */
public final class ReportPresetsPanel extends JPanel{

	private final ReportPreferences preferences;
	private final ReportType type;
	/** Applies the preset with the name. */
	private final Consumer<String> apply;
	/** Saves the options as they are under the name. */
	private final Consumer<String> save;
	private final Supplier<Component> owner;
	private final DefaultComboBoxModel<String> names = new DefaultComboBoxModel<>();
	private final JComboBox<String> combo = new JComboBox<>(names);
	private final JButton saveButton = new JButton(I18N.t("button.report.preset.save"));
	private final JButton deleteButton = new JButton(I18N.t("button.report.preset.delete"));
	private boolean refreshing;


	/**
	 * @param preferences	Where the presets are kept.
	 * @param type	The type of the report.
	 * @param apply	Applies the preset with the name to the options.
	 * @param save	Saves the options as they are under the name.
	 * @param owner	The component the questions are asked over.
	 */
	public ReportPresetsPanel(final ReportPreferences preferences, final ReportType type, final Consumer<String> apply,
			final Consumer<String> save, final Supplier<Component> owner){
		super(new MigLayout("ins 4 8 8 8,fillx", "[grow,fill][][]"));
		this.preferences = preferences;
		this.type = type;
		this.apply = apply;
		this.save = save;
		this.owner = owner;

		setBorder(BorderFactory.createTitledBorder(I18N.t("border.report.presets")));
		setToolTipText(I18N.t("tooltip.report.presets"));
		combo.setToolTipText(I18N.t("tooltip.report.presets"));

		combo.addActionListener(e -> {
			if(!refreshing && combo.getSelectedIndex() > 0)
				apply.accept((String)combo.getSelectedItem());
			deleteButton.setEnabled(combo.getSelectedIndex() > 0);
		});
		saveButton.addActionListener(e -> onSave());
		deleteButton.addActionListener(e -> onDelete());

		add(combo);
		add(saveButton);
		add(deleteButton);

		refresh(null);
	}


	private void onSave(){
		final Object current = (combo.getSelectedIndex() > 0? combo.getSelectedItem(): null);
		final Object answer = JOptionPane.showInputDialog(owner.get(), I18N.t("dialog.report.preset.save.message"),
			I18N.t("dialog.report.preset.save.title"), JOptionPane.QUESTION_MESSAGE, null, null, current);
		if(answer == null)
			return;

		final String name = answer.toString().strip();
		if(name.isEmpty())
			return;
		for(int i = 0; i < names.getSize(); i ++)
			if(i > 0 && names.getElementAt(i).equalsIgnoreCase(name)
					&& JOptionPane.showConfirmDialog(owner.get(), I18N.t("dialog.report.preset.overwrite.message"),
					I18N.t("dialog.report.preset.save.title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
				return;

		save.accept(name);
		refresh(name);
	}

	private void onDelete(){
		if(combo.getSelectedIndex() <= 0)
			return;

		final String name = (String)combo.getSelectedItem();
		if(JOptionPane.showConfirmDialog(owner.get(), I18N.tf("dialog.report.preset.delete.message", name),
				I18N.t("dialog.report.preset.delete.title"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;

		preferences.deletePreset(type, name);
		refresh(null);
	}

	/** Lists the presets of the type, with the one selected (the first item, that is none, if the name is {@code null}). */
	private void refresh(final String selected){
		refreshing = true;
		try{
			names.removeAllElements();
			names.addElement(I18N.t("text.report.preset.none"));
			String chosen = null;
			for(final String name : preferences.presetNames(type)){
				names.addElement(name);
				if(selected != null && name.equalsIgnoreCase(selected))
					chosen = name;
			}
			combo.setSelectedItem(chosen != null? chosen: names.getElementAt(0));
			deleteButton.setEnabled(chosen != null);
		}
		finally{
			refreshing = false;
		}
	}

}
