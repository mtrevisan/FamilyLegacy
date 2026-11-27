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
package io.github.mtrevisan.familylegacy.ui.components.projections.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.quickentry.LifeFactRow;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Dialog that displays a list of items with a combo box for each.
 * The user selects a relationship type for each item.
 */
public class RelationshipTypeSelectionDialog extends JDialog{

	/**
	 * Represents one row in the dialog.
	 *
	 * @param id          the entity ID (used for opening edit dialog)
	 * @param label       the display name/label
	 * @param defaultType default relationship type
	 */
	public record Item(String id, String label, String defaultType){}

	/**
	 * An extra row where the user also chooses <em>who</em> the other end of the relationship is.
	 *
	 * @param partners	the suggested individuals (the first one is preselected; may be empty).
	 * @param picker	called when the user asks for another individual, with whether both parents are biological: returns it, or {@code null}
	 *	if none was picked.
	 * @param compatible	whether an individual can be the other parent of a child whose parents are both biological (the biological parents
	 * 	of a child are of different sexes): the others are not offered, and not accepted, while both types are biological child.
	 */
	public record OtherParentRow(List<Item> partners, Function<Boolean, Item> picker, Predicate<Item> compatible){

		/** A row where every individual is accepted as the other parent. */
		public OtherParentRow(final List<Item> partners, final Supplier<Item> picker){
			this(partners, biological -> (picker != null? picker.get(): null), item -> true);
		}

	}

	/**
	 * The outcome of {@link #selectParents}.
	 *
	 * @param parentType	the type chosen for the first parent.
	 * @param otherParent	the other parent ({@code null} if the user chose nobody).
	 * @param otherType	the type chosen for the other parent ({@code null} if there is none).
	 */
	public record ParentsChoice(String parentType, Item otherParent, String otherType){}

	/**
	 * The outcome of {@link #selectUnion}.
	 *
	 * @param type	the type of the relationship chosen.
	 * @param fact	when and where the union began (nothing if the type has no event to tell it, or nothing was typed).
	 */
	public record UnionChoice(String type, QuickEntryService.LifeFact fact){}

	/** An entry of the "other parent" combo: an individual, or one of the two commands. */
	private record Choice(Item item, String text){

		@Override
		public String toString(){
			return text;
		}

	}


	private final FLEFModel model;
	private final List<Item> items;
	private final String[] allowedTypes;
	private final IndividualListener listener;
	private final OtherParentRow otherParentRow;
	/** Whether the dialog also asks when and where the union began (for the partners). */
	private final boolean withUnion;
	private LifeFactRow unionRow;
	private boolean unionApplies;
	private QuickEntryService.LifeFact selectedUnion = QuickEntryService.LifeFact.NONE;

	private final List<JComboBox<String>> comboBoxes = new ArrayList<>();
	private JComboBox<Choice> otherParentCombo;
	private JComboBox<String> otherTypeCombo;
	private final List<Item> otherCandidates = new ArrayList<>();
	private Choice anotherChoice;
	private Choice nobodyChoice;
	private int previousOtherParent;
	private boolean refillingOtherParent;
	private List<String> selectedTypes;
	private Item selectedOther;
	private String selectedOtherType;
	private boolean canceled = true;


	/**
	 * Constructor.
	 *
	 * @param owner        parent window
	 * @param items        list of items to show (non-empty)
	 * @param allowedTypes the allowed relationship type strings
	 * @param listener callback to edit an entity by ID
	 * @param model        the FLEF model
	 */
	public RelationshipTypeSelectionDialog(final Window owner, final List<Item> items,
			final String[] allowedTypes, final IndividualListener listener, final FLEFModel model){
		this(owner, items, allowedTypes, listener, model, null, false);
	}

	private RelationshipTypeSelectionDialog(final Window owner, final List<Item> items,
			final String[] allowedTypes, final IndividualListener listener, final FLEFModel model, final OtherParentRow otherParentRow,
			final boolean withUnion){
		super(owner, I18N.t("dialog.relationship_types.title"), ModalityType.APPLICATION_MODAL);

		if(items == null || items.isEmpty())
			throw new IllegalArgumentException("Items list must not be empty");
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");

		this.model = model;
		this.items = items;
		this.allowedTypes = allowedTypes;
		this.listener = listener;
		this.otherParentRow = otherParentRow;
		this.withUnion = withUnion;

		initComponents();

		pack();

		setMinimumSize(new Dimension(480, getPreferredSize().height));
		setLocationRelativeTo(owner);
	}


	/**
	 * Shows the dialog only when more than one relationship type is
	 * available. When a single type is allowed, returns a list that
	 * assigns that type to every item without opening any dialog.
	 *
	 * @param owner        parent window; may be {@code null}
	 * @param items        the items to display; if empty, an empty list is returned
	 * @param allowedTypes the allowed relationship type strings
	 * @param listener     callback to edit an entity by ID
	 * @param model        the FLEF model
	 * @return the selected types in the same order as the items, or {@code null} if cancelled
	 */
	public static List<String> selectRelationshipType(final Window owner, final List<Item> items,
			final String[] allowedTypes, final IndividualListener listener, final FLEFModel model){
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");

		if(items == null || items.isEmpty())
			return Collections.emptyList();

		if(allowedTypes.length == 1)
			return Collections.nCopies(items.size(), allowedTypes[0]);

		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(owner, items,
			allowedTypes, listener, model);
		dialog.setVisible(true);

		return dialog.getSelectedTypes();
	}

	/**
	 * Shows the dialog for a new child: the first parent with its type, and a second row where the user chooses the other parent
	 * (one of the suggested partners, another individual, or nobody) and its type.
	 *
	 * @return	The choice, or {@code null} if cancelled.
	 */
	public static ParentsChoice selectParents(final Window owner, final Item parent, final OtherParentRow otherParent,
			final String[] allowedTypes, final FLEFModel model){
		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(owner, List.of(parent), allowedTypes, null, model,
			otherParent, false);
		dialog.setVisible(true);

		final List<String> types = dialog.getSelectedTypes();
		if(types == null)
			return null;

		return new ParentsChoice(types.getFirst(), dialog.selectedOther, dialog.selectedOtherType);
	}


	/**
	 * Shows the dialog for a new union: the type of the relationship (the kinds of spouse, the engaged or cohabiting partner) and, for the
	 * types that begin with an event (a marriage, an engagement), when and where it happened.
	 *
	 * @param owner	parent window; may be {@code null}.
	 * @param partner	the individual that is being linked.
	 * @param allowedTypes	the allowed relationship types.
	 * @param model	the FLEF model.
	 * @return	The choice, or {@code null} if cancelled.
	 */
	public static UnionChoice selectUnion(final Window owner, final Item partner, final String[] allowedTypes, final FLEFModel model){
		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(owner, List.of(partner), allowedTypes, null, model,
			null, true);
		dialog.setVisible(true);

		final List<String> types = dialog.getSelectedTypes();
		return (types != null? new UnionChoice(types.getFirst(), dialog.selectedUnion): null);
	}


	private void initComponents(){
		setLayout(GUIHelper.createLabelFieldLayout(10, "[grow]"));

		// Content Table
		final JPanel tablePanel = new JPanel(new MigLayout("ins 0,fillx,gap 10 8", "[grow, fill][grow, fill]", "[]"));

		// Column headers
		final JLabel objectHeader = new JLabel(I18N.t("label.target"));
		objectHeader.setFont(objectHeader.getFont().deriveFont(Font.BOLD));
		final JLabel typeHeader = new JLabel(I18N.t("label.relationship_type"));
		typeHeader.setFont(typeHeader.getFont().deriveFont(Font.BOLD));

		tablePanel.add(objectHeader);
		tablePanel.add(typeHeader, "wrap");

		for(final Item item : items){
			final JLabel itemLabel = new JLabel(item.label);
			itemLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			// Double-click to edit individual
			itemLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && listener != null){
						if(item.id() != null)
							listener.onEntityEdit(model.getRecordById(item.id()));
					}
				}
			});

			tablePanel.add(itemLabel, "aligny center");

			final BoundComboBox<String> allowedTypeCombo = new BoundComboBox<>(null, allowedTypes);
			allowedTypeCombo.setI18NPrefix("enum.relationship.type");
			allowedTypeCombo.setSelectedItem(item.defaultType);
			comboBoxes.add(allowedTypeCombo);
			tablePanel.add(allowedTypeCombo, "wrap");
		}

		if(otherParentRow != null)
			addOtherParentRow(tablePanel);

		final JScrollPane scrollPane = new JScrollPane(tablePanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		final int preferredHeight = Math.min(250, (items.size() + (otherParentRow != null? 1: 0)) * 38 + 30);
		scrollPane.setPreferredSize(new Dimension(450, preferredHeight));

		add(scrollPane, "grow,wrap,gapbottom 10");

		if(withUnion)
			add(createUnionPanel(), "growx,wrap,gapbottom 10");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			I18N.t("button.ok"), this::onOk,
			this::onCancel);
		add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton((JButton)buttonPanel.getComponent(0));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	/**
	 * Adds the row where the other parent is chosen: a combo with the suggested individuals, "Another individual…" and "Nobody", and
	 * the combo of its type (disabled while nobody is chosen). While both parents are biological, only the individuals that can be the
	 * other biological parent are offered and accepted.
	 */
	private void addOtherParentRow(final JPanel tablePanel){
		anotherChoice = new Choice(null, I18N.t("dialog.other_parent.other"));
		nobodyChoice = new Choice(null, I18N.t("dialog.other_parent.none"));
		otherCandidates.addAll(otherParentRow.partners());

		otherParentCombo = new JComboBox<>();

		final String defaultType = (items.getFirst().defaultType() != null? items.getFirst().defaultType(): allowedTypes[0]);
		final BoundComboBox<String> typeCombo = new BoundComboBox<>(null, allowedTypes);
		typeCombo.setI18NPrefix("enum.relationship.type");
		typeCombo.setSelectedItem(defaultType);
		otherTypeCombo = typeCombo;

		refillOtherParent();

		otherParentCombo.addActionListener(e -> {
			if(refillingOtherParent)
				return;

			final Choice selected = (Choice)otherParentCombo.getSelectedItem();
			if(selected == anotherChoice){
				final Item picked = (otherParentRow.picker() != null? otherParentRow.picker().apply(areBothParentsBiological()): null);
				if(picked == null || !isAcceptedAsOtherParent(picked)){
					if(picked != null)
						MessageDialogs.show(this, I18N.tf("validation.other_parent_sex", picked.label()), I18N.t("validation.title"),
							JOptionPane.ERROR_MESSAGE);

					//nothing (valid) was picked: back to the previous choice
					otherParentCombo.setSelectedIndex(previousOtherParent);

					return;
				}

				//the picked individual becomes an entry of the combo, right before the commands
				otherCandidates.add(picked);
				final int index = otherParentCombo.getItemCount() - 2;
				otherParentCombo.insertItemAt(new Choice(picked, picked.label()), index);
				otherParentCombo.setSelectedIndex(index);

				return;
			}

			previousOtherParent = otherParentCombo.getSelectedIndex();
			otherTypeCombo.setEnabled(selected != nobodyChoice);
		});
		//the choice of the types changes who can be the other parent
		comboBoxes.getFirst().addActionListener(e -> refillOtherParent());
		otherTypeCombo.addActionListener(e -> refillOtherParent());

		tablePanel.add(otherParentCombo, "aligny center");
		tablePanel.add(otherTypeCombo, "wrap");
	}

	/** The panel where the user says when and where the union began: it applies only to the types that have an event to tell it. */
	private JPanel createUnionPanel(){
		unionRow = new LifeFactRow(this, model, "dialog.quick_entry.union_date");

		final JPanel panel = new JPanel(new MigLayout("ins 5,hidemode 3,fillx,gapy 5", "[right]rel[]rel[right]rel[grow,fill]rel[]"));
		panel.setBorder(new TitledBorder(I18N.t("dialog.quick_entry.union")));
		unionRow.addTo(panel);

		comboBoxes.getFirst().addActionListener(e -> updateUnionApplies());
		updateUnionApplies();
		return panel;
	}

	/** A type that has no event to tell when it began (the cohabiting partners) has no date and no place to ask. */
	private void updateUnionApplies(){
		unionApplies = (QuickEntryService.unionEventType((String)comboBoxes.getFirst().getSelectedItem()) != null);
		unionRow.setActive(unionApplies);
	}

	/** @return	Whether both parents are biological, that is the biological parents of the child must be of different sexes. */
	private boolean areBothParentsBiological(){
		return (RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(comboBoxes.getFirst().getSelectedItem())
			&& RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(otherTypeCombo.getSelectedItem()));
	}

	private boolean isAcceptedAsOtherParent(final Item candidate){
		return (!areBothParentsBiological() || otherParentRow.compatible().test(candidate));
	}

	/** Fills the combo with the candidates that are accepted now, keeping the current choice if it is still there. */
	private void refillOtherParent(){
		final Choice current = (Choice)otherParentCombo.getSelectedItem();

		refillingOtherParent = true;
		try{
			otherParentCombo.removeAllItems();
			int selectedIndex = -1;
			for(final Item candidate : otherCandidates)
				if(isAcceptedAsOtherParent(candidate)){
					if(current != null && current.item != null && current.item.id().equals(candidate.id()))
						selectedIndex = otherParentCombo.getItemCount();
					otherParentCombo.addItem(new Choice(candidate, candidate.label()));
				}
			final boolean hasCandidates = (otherParentCombo.getItemCount() > 0);
			otherParentCombo.addItem(anotherChoice);
			otherParentCombo.addItem(nobodyChoice);

			if(selectedIndex < 0)
				//the first candidate, or nobody if there is none (or if nobody was chosen)
				selectedIndex = (hasCandidates && (current == null || current != nobodyChoice)? 0: otherParentCombo.getItemCount() - 1);
			otherParentCombo.setSelectedIndex(selectedIndex);
			previousOtherParent = selectedIndex;
			otherTypeCombo.setEnabled(otherParentCombo.getSelectedItem() != nobodyChoice);
		}
		finally{
			refillingOtherParent = false;
		}
	}


	private void onOk(){
		//the date is read and the place decided before anything else, so that the user can still correct them
		if(withUnion && unionApplies && !unionRow.prepare())
			return;

		selectedTypes = new ArrayList<>();
		for(final JComboBox<String> combo : comboBoxes){
			final String selected = (String)combo.getSelectedItem();
			selectedTypes.add(selected != null? selected: allowedTypes[0]);
		}
		if(otherParentCombo != null){
			final Choice chosen = (Choice)otherParentCombo.getSelectedItem();
			selectedOther = (chosen != null? chosen.item: null);
			if(selectedOther != null){
				final String type = (String)otherTypeCombo.getSelectedItem();
				selectedOtherType = (type != null? type: allowedTypes[0]);
			}
		}
		if(withUnion && unionApplies)
			selectedUnion = unionRow.fact();
		canceled = false;

		dispose();
	}

	private void onCancel(){
		canceled = true;

		dispose();
	}

	/**
	 * Returns the selected types in the same order as the input items,
	 * or {@code null} if the user canceled.
	 */
	public List<String> getSelectedTypes(){
		return (canceled? null: new ArrayList<>(selectedTypes));
	}

}
