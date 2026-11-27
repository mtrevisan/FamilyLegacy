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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.FamilyQuickEntry;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The quick entry of a small family in one window: a person (the individual selected in the view, or a new one), the partner, how and when
 * the union began, and the children, each with the name, the sex and the birth. Only what is filled is created (see
 * {@link FamilyQuickEntry}); the dialog only asks, and gives back what the user typed once it is confirmed.
 */
public final class FamilyQuickEntryDialog extends JDialog{

	private static final String COLUMNS = "[right]rel[]rel[right]rel[grow,fill]rel[]";
	private static final String PANEL_LAYOUT = "ins 5,hidemode 3,fillx,gapy 5";
	private static final int CHILDREN_HEIGHT = 200;
	private static final int SCROLL_UNIT = 20;


	private final FLEFModel model;
	private final FLEFRecord selected;

	private final JCheckBox useSelected = new JCheckBox();
	private final PersonRow personRow;
	private final PersonRow partnerRow;
	private final PersonRow fatherRow;
	private final PersonRow motherRow;
	private final JComboBox<String> fatherTypeCombo = new JComboBox<>(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	private final JComboBox<String> motherTypeCombo = new JComboBox<>(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	private final JComboBox<String> unionTypeCombo = new JComboBox<>(RelationshipReader.PARTNER_TYPES.toArray(String[]::new));
	private final LifeFactRow unionRow;
	private final JComboBox<String> childTypeCombo = new JComboBox<>(RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES);
	private final List<PersonRow> childRows = new ArrayList<>();
	private final Map<PersonRow, JButton> removeButtons = new HashMap<>();
	/** The children, one block each, one under the other. */
	private final JPanel childrenGrid = new JPanel(new MigLayout("ins 5,hidemode 3,fillx,gapy 12", "[grow,fill]"));
	private final JPanel personGrid = new JPanel(new MigLayout(PANEL_LAYOUT, COLUMNS));

	private FamilyQuickEntry.Input result;
	private LifeFactRow.Reporter reporter = (parent, message) -> MessageDialogs.show(parent, message, I18N.t("validation.title"),
		JOptionPane.ERROR_MESSAGE);


	/**
	 * Asks the user for a family.
	 *
	 * @param owner	The window that owns the dialog.
	 * @param model	The model.
	 * @param selected	The individual selected in the view ({@code null} if none): the family may be the one of that individual.
	 * @return	What the user typed, {@code null} if the dialog was cancelled.
	 */
	public static FamilyQuickEntry.Input show(final Window owner, final FLEFModel model, final FLEFRecord selected){
		final FamilyQuickEntryDialog dialog = new FamilyQuickEntryDialog(owner, model, selected);
		dialog.setVisible(true);

		return dialog.result;
	}


	FamilyQuickEntryDialog(final Window owner, final FLEFModel model, final FLEFRecord selected){
		super(owner, I18N.t("dialog.family_entry.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.selected = selected;
		personRow = new PersonRow(this, model);
		partnerRow = new PersonRow(this, model);
		fatherRow = new PersonRow(this, model);
		motherRow = new PersonRow(this, model);
		unionRow = new LifeFactRow(this, model, "dialog.quick_entry.union_date");

		setLayout(new MigLayout("ins 10,hidemode 3,fillx,gapy 8", "[grow,fill]"));
		add(new JLabel(I18N.t("dialog.family_entry.hint")), "wrap");
		add(createPersonPanel(), "growx,wrap");
		add(createParentsPanel(), "growx,wrap");
		add(createPartnerPanel(), "growx,wrap");
		add(createChildrenPanel(), "growx,wrap");
		add(GUIHelper.createButtonPanel(this, this::onOk, this::dispose), "growx");

		pack();
		setLocationRelativeTo(owner);
	}


	private JPanel createPersonPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,hidemode 3,fillx,gapy 5", "[grow,fill]"));
		panel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.person")));

		personRow.addTo(personGrid);
		if(selected != null){
			useSelected.setText(I18N.tf("dialog.family_entry.use_selected", IndividualReader.displayText(selected)));
			useSelected.setSelected(true);
			useSelected.addActionListener(e -> showPerson());
			panel.add(useSelected, "wrap");
		}
		panel.add(personGrid, "growx");
		showPerson();
		return panel;
	}

	/** The fields of a new person are shown unless the selected individual is the one. */
	private void showPerson(){
		personGrid.setVisible(selected == null || !useSelected.isSelected());
		revalidate();
		repaint();
	}

	/** The parents of the person: the father and the mother, each with its own type of relationship (biological by default). */
	private JPanel createParentsPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,hidemode 3,fillx,gapy 5", "[grow,fill]"));
		panel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.parents")));

		final JPanel fatherPanel = new JPanel(new MigLayout(PANEL_LAYOUT, COLUMNS));
		fatherPanel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.father")));
		fatherRow.addTo(fatherPanel);
		addParentType(fatherPanel, fatherTypeCombo);
		panel.add(fatherPanel, "growx,wrap");

		final JPanel motherPanel = new JPanel(new MigLayout(PANEL_LAYOUT, COLUMNS));
		motherPanel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.mother")));
		motherRow.addTo(motherPanel);
		addParentType(motherPanel, motherTypeCombo);
		panel.add(motherPanel, "growx");
		return panel;
	}

	private static void addParentType(final JPanel panel, final JComboBox<String> combo){
		panel.add(new JLabel(I18N.t("dialog.family_entry.parent_type")));
		combo.setSelectedItem(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		combo.setRenderer(relationshipTypeRenderer());
		panel.add(combo, "span 4,growx 0,wrap");
	}

	private JPanel createPartnerPanel(){
		final JPanel panel = new JPanel(new MigLayout(PANEL_LAYOUT, COLUMNS));
		panel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.partner")));

		partnerRow.addTo(panel);
		panel.add(new JLabel(I18N.t("dialog.family_entry.union_type")));
		unionTypeCombo.setSelectedItem(RelationshipReader.ENUM_TYPE_SPOUSE);
		unionTypeCombo.setRenderer(relationshipTypeRenderer());
		panel.add(unionTypeCombo, "span 4,growx 0,wrap");
		unionRow.addTo(panel);

		//the date and the place of the union are for the types that began with an event, and only if there is a partner
		unionTypeCombo.addActionListener(e -> updateUnion());
		partnerRow.onChange(this::updateUnion);
		updateUnion();
		return panel;
	}

	private void updateUnion(){
		final boolean applies = (!partnerRow.isEmpty() && QuickEntryService.unionEventType((String)unionTypeCombo.getSelectedItem()) != null);
		unionRow.setActive(applies);
	}

	private JPanel createChildrenPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,hidemode 3,fillx,gapy 5", "[grow,fill]"));
		panel.setBorder(new TitledBorder(I18N.t("dialog.family_entry.children")));

		final JPanel typeRow = new JPanel(new MigLayout("ins 0", "[]rel[]"));
		typeRow.add(new JLabel(I18N.t("dialog.family_entry.child_type")));
		childTypeCombo.setSelectedItem(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
		childTypeCombo.setRenderer(relationshipTypeRenderer());
		typeRow.add(childTypeCombo);
		panel.add(typeRow, "wrap");

		final JScrollPane scroll = GUIHelper.createScrollPane(childrenGrid);
		scroll.setBorder(null);
		//a turn of the wheel moves by a few lines, not by a pixel: the default is too slow for a long list
		scroll.getVerticalScrollBar()
			.setUnitIncrement(SCROLL_UNIT);
		scroll.getVerticalScrollBar()
			.setBlockIncrement(CHILDREN_HEIGHT);
		panel.add(scroll, "growx,wrap");

		final JButton addChild = new JButton(I18N.t("dialog.family_entry.add_child"));
		addChild.addActionListener(e -> addChildRow());
		panel.add(addChild, "growx 0,alignx left");

		addChildRow();
		//the area is as wide as the blocks of the children (plus the bar), so that nothing is cut at the right and there is no bar below
		final int scrollBarWidth = scroll.getVerticalScrollBar()
			.getPreferredSize().width;
		scroll.setPreferredSize(new Dimension(childrenGrid.getPreferredSize().width + scrollBarWidth + 4, CHILDREN_HEIGHT));
		return panel;
	}

	private static DefaultListCellRenderer relationshipTypeRenderer(){
		return new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
					final boolean cellHasFocus){
				return super.getListCellRendererComponent(list, I18N.t("enum.relationship.type." + value), index, isSelected, cellHasFocus);
			}
		};
	}

	private PersonRow newChildRow(){
		final PersonRow row = new PersonRow(this, model);
		//a row can be removed only if there is something to remove: what was typed in it, or the row itself when there are others
		row.onChange(this::updateRemoveButtons);
		return row;
	}

	private void updateRemoveButtons(){
		for(final Map.Entry<PersonRow, JButton> entry : removeButtons.entrySet())
			entry.getValue()
				.setEnabled(!entry.getKey().isEmpty() || childRows.size() > 1);
	}

	private void addChildRow(){
		final PersonRow row = newChildRow();
		childRows.add(row);
		rebuildChildren();
		row.focus();
	}

	private void removeChildRow(final PersonRow row){
		childRows.remove(row);
		//there is always a row to type in
		if(childRows.isEmpty())
			childRows.add(newChildRow());
		rebuildChildren();
	}

	private void rebuildChildren(){
		childrenGrid.removeAll();
		removeButtons.clear();
		int number = 0;
		for(final PersonRow row : childRows){
			number ++;
			//each child is a block of its own, with a frame and a title, so that where a child ends and the next begins is clear
			final JPanel block = new JPanel(new MigLayout(PANEL_LAYOUT, COLUMNS));
			block.setBorder(new TitledBorder(I18N.tf("dialog.family_entry.child_number", String.valueOf(number))));
			row.addTo(block);
			final JButton remove = new JButton(I18N.t("popupmenu.remove"));
			remove.addActionListener(e -> removeChildRow(row));
			removeButtons.put(row, remove);
			block.add(remove, "skip 4,alignx right");
			childrenGrid.add(block, "growx,wrap");
		}
		updateRemoveButtons();
		childrenGrid.revalidate();
		childrenGrid.repaint();
	}


	/** @return	What the user typed ({@code null} if the dialog was not confirmed). */
	FamilyQuickEntry.Input result(){
		return result;
	}

	PersonRow personRow(){
		return personRow;
	}

	PersonRow fatherRow(){
		return fatherRow;
	}

	PersonRow motherRow(){
		return motherRow;
	}

	JComboBox<String> fatherTypeCombo(){
		return fatherTypeCombo;
	}

	JComboBox<String> motherTypeCombo(){
		return motherTypeCombo;
	}

	PersonRow partnerRow(){
		return partnerRow;
	}

	LifeFactRow unionRow(){
		return unionRow;
	}

	JComboBox<String> unionTypeCombo(){
		return unionTypeCombo;
	}

	JComboBox<String> childTypeCombo(){
		return childTypeCombo;
	}

	/** @return	The button that removes the row of a child. */
	JButton removeButton(final int index){
		return removeButtons.get(childRows.get(index));
	}

	void addChild(){
		addChildRow();
	}

	void removeChild(final int index){
		removeChildRow(childRows.get(index));
	}

	List<PersonRow> childRows(){
		return childRows;
	}

	void setReporter(final LifeFactRow.Reporter reporter){
		this.reporter = reporter;
	}

	/** Confirms the dialog: the dates are read, the places decided, the sexes of the parents checked; the user stays here if not right. */
	void onOk(){
		final boolean newPerson = (selected == null || !useSelected.isSelected());
		if(newPerson && !personRow.prepare() || !partnerRow.prepare() || !fatherRow.prepare() || !motherRow.prepare())
			return;
		for(final PersonRow child : childRows)
			if(!child.prepare())
				return;
		if(!partnerRow.isEmpty() && unionApplies() && !unionRow.prepare())
			return;

		final FamilyQuickEntry.Person first = (newPerson? personRow.person(): FamilyQuickEntry.Person.existing(selected.getId()));
		final FamilyQuickEntry.Person partner = (partnerRow.isEmpty()? null: partnerRow.person());
		final String childType = (String)childTypeCombo.getSelectedItem();
		final FamilyQuickEntry.Person father = (fatherRow.isEmpty()? null: parent(fatherRow, SexType.MALE));
		final FamilyQuickEntry.Person mother = (motherRow.isEmpty()? null: parent(motherRow, SexType.FEMALE));
		final String fatherType = (String)fatherTypeCombo.getSelectedItem();
		final String motherType = (String)motherTypeCombo.getSelectedItem();
		if(father != null && mother != null && RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(fatherType)
				&& RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(motherType)
				&& father.sex() == mother.sex()){
			reporter.report(this, I18N.t("dialog.family_entry.parents_same_sex"));

			return;
		}		if(partner != null && RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(childType)){
			final SexType firstSex = (newPerson? first.sex(): PreferredSex.of(selected, model));
			if(firstSex != SexType.UNKNOWN && firstSex == partner.sex() && hasChildren()){
				reporter.report(this, I18N.t("dialog.family_entry.same_sex_parents"));

				return;
			}
		}

		final List<FamilyQuickEntry.Person> children = new ArrayList<>();
		for(final PersonRow child : childRows)
			if(!child.isEmpty())
				children.add(child.person());
		final String unionType = (String)unionTypeCombo.getSelectedItem();
		result = new FamilyQuickEntry.Input(first, partner, unionType, (partner != null && unionApplies()? unionRow.fact(): QuickEntryService.LifeFact.NONE), childType,
			children, father, mother, fatherType, motherType);
		dispose();
	}

	/** @return	The parent as it was typed, of the given sex if the user did not say otherwise (the father is a man, the mother a woman). */
	private static FamilyQuickEntry.Person parent(final PersonRow row, final SexType defaultSex){
		final FamilyQuickEntry.Person person = row.person();
		return (person.sex() != SexType.UNKNOWN? person: new FamilyQuickEntry.Person(null, person.name(), defaultSex, person.birth(), person.death()));
	}

	private boolean unionApplies(){
		return (QuickEntryService.unionEventType((String)unionTypeCombo.getSelectedItem()) != null);
	}

	private boolean hasChildren(){
		return childRows.stream()
			.anyMatch(row -> !row.isEmpty());
	}

}
