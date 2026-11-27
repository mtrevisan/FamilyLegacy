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
package io.github.mtrevisan.familylegacy.ui.components.quickentry;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.quickentry.FamilyQuickEntry;
import io.github.mtrevisan.familylegacy.quickentry.FullNameParser;
import io.github.mtrevisan.familylegacy.quickentry.ParsedName;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;


/**
 * A person of a family quick entry, as the rows of a grid: the full name, the sex, and the birth and the death (date and place). What is typed as the
 * full name is split into the parts of a name when the dialog is confirmed (see {@link FullNameParser}); under the field the user sees
 * how it will be split, and can correct it afterwards with the editor of the individual.
 */
public final class PersonRow{

	private static final String PARTS_SEPARATOR = " \u00B7 ";


	private final JTextField fullNameField = new JTextField(28);
	private final JLabel namePreview = new JLabel(StringUtils.SPACE);
	private final JComboBox<SexType> sexCombo = new JComboBox<>(new SexType[]{SexType.UNKNOWN, SexType.MALE, SexType.FEMALE});
	private final LifeFactRow birth;
	private final LifeFactRow death;
	private final List<Runnable> listeners = new ArrayList<>();


	/**
	 * @param owner	The window that owns the dialogs that the row opens.
	 * @param model	The model.
	 */
	public PersonRow(final Window owner, final FLEFModel model){
		birth = new LifeFactRow(owner, model, "dialog.quick_entry.birth_date");
		birth.onChange(this::fire);
		death = new LifeFactRow(owner, model, "dialog.quick_entry.death_date");
		death.onChange(this::fire);

		fullNameField.setToolTipText(I18N.t("dialog.quick_entry.full_name.tooltip"));
		namePreview.setVisible(false);
		LifeFactRow.listen(fullNameField, () -> {
			updateNamePreview();
			fire();
		});
		sexCombo.setToolTipText(I18N.t("dialog.individual.sex"));
		sexCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
					final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, I18N.t("enum.individual.sex." + ((SexType)value).name().toLowerCase(Locale.ROOT)),
					index, isSelected, cellHasFocus);
			}
		});
		sexCombo.addActionListener(e -> fire());
	}


	/**
	 * Adds the cells of the person to a grid of five columns (label, field, label, field that grows, button).
	 *
	 * @param grid	The panel, with a {@code MigLayout} of five columns, whose current row is empty.
	 */
	public void addTo(final JPanel grid){
		grid.add(new JLabel(I18N.t("dialog.quick_entry.full_name")));
		grid.add(fullNameField, "span 3,growx");
		grid.add(sexCombo, "wrap");
		grid.add(namePreview, "skip 1,span 4,wrap");
		birth.addTo(grid);
		death.addTo(grid);
	}

	/** Runs the action whenever the name or the sex change (what the others depend on: the union asks for a date only if there is a partner). */
	public void onChange(final Runnable listener){
		listeners.add(listener);
	}

	/** @return	Whether nothing has been typed or chosen. */
	public boolean isEmpty(){
		return (StringUtils.isBlank(fullNameField.getText()) && sex() == SexType.UNKNOWN && birth.isEmpty() && death.isEmpty());
	}

	/** @return	The sex chosen. */
	public SexType sex(){
		return (SexType)sexCombo.getSelectedItem();
	}

	/**
	 * Checks the dates and settles the places of the birth and the death (see {@link LifeFactRow#prepare()}).
	 *
	 * @return	Whether the person is ready.
	 */
	public boolean prepare(){
		return (birth.prepare() && death.prepare());
	}

	/** @return	The person as it was typed, as {@link #prepare()} settled it. */
	public FamilyQuickEntry.Person person(){
		return new FamilyQuickEntry.Person(null, FullNameParser.parse(fullNameField.getText()), sex(), birth.fact(), death.fact());
	}

	/** Types the full name (what the user would type). */
	void setFullName(final String name){
		LifeFactRow.onEventThread(() -> fullNameField.setText(name));
	}

	/** Chooses the sex (what the user would choose). */
	void setSex(final SexType sex){
		LifeFactRow.onEventThread(() -> sexCombo.setSelectedItem(sex));
	}

	/** @return	The row of the birth. */
	LifeFactRow birthRow(){
		return birth;
	}

	/** @return	The row of the death. */
	LifeFactRow deathRow(){
		return death;
	}

	/** Puts the focus on the field of the full name. */
	public void focus(){
		fullNameField.requestFocusInWindow();
	}


	private void fire(){
		for(final Runnable listener : listeners)
			listener.run();
	}

	/** Tells, under the field, how the name will be split (nothing while there is nothing typed). */
	private void updateNamePreview(){
		final ParsedName name = FullNameParser.parse(fullNameField.getText());
		final boolean visible = namePreview.isVisible();
		namePreview.setVisible(!name.isEmpty());
		if(visible == name.isEmpty()){
			final java.awt.Container grid = namePreview.getParent();
			if(grid != null){
				grid.revalidate();
				grid.repaint();
			}
		}
		if(name.isEmpty()){
			namePreview.setText(StringUtils.SPACE);
			return;
		}

		final StringJoiner parts = new StringJoiner(PARTS_SEPARATOR);
		for(final ParsedName.Part part : name.parts())
			parts.add(I18N.t("enum.part.type." + part.type()) + StringUtils.SPACE + part.value());
		namePreview.setText(I18N.tf("dialog.quick_entry.full_name.preview", parts.toString()));
		namePreview.setForeground(UIManager.getColor("Label.disabledForeground"));
	}

}
