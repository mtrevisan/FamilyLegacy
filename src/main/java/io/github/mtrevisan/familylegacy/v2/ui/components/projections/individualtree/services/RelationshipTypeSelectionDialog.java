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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services;

import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Dialog that displays a list of items with a combo box for each.
 * The user selects a relationship type for each item.
 */
public class RelationshipTypeSelectionDialog extends JDialog{

	/**
	 * Represents one row in the dialog.
	 */
	public record Item(String label, String defaultType){}


	private final List<Item> items;
	private final String[] allowedTypes;

	private final List<JComboBox<String>> comboBoxes = new ArrayList<>();
	private List<String> selectedTypes;
	private boolean canceled = true;


	/**
	 * Constructor.
	 *
	 * @param owner        parent window
	 * @param items        list of items to show (non-empty)
	 * @param allowedTypes the allowed relationship type strings
	 */
	public RelationshipTypeSelectionDialog(final Window owner, final List<Item> items, final String[] allowedTypes){
		super(owner, "Select Relationship Types", ModalityType.APPLICATION_MODAL);

		if(items == null || items.isEmpty())
			throw new IllegalArgumentException("Items list must not be empty");
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");

		this.items = new ArrayList<>(items);
		this.allowedTypes = allowedTypes;

		initUI();

		pack();

		setMinimumSize(new Dimension(480, getPreferredSize().height));
		setLocationRelativeTo(owner);
	}


	/**
	 * Shows the dialog only when more than one relationship type is
	 * available. When a single type is allowed, returns a list that
	 * assigns that type to every item without opening any dialog.
	 * <p>
	 * This helper lets callers avoid duplicating the "single type" check
	 * and keeps the "no choice" fast path in a single place.
	 *
	 * @param owner        parent window; may be {@code null}
	 * @param items        the items to display; if empty, an empty list is
	 *                     returned
	 * @param allowedTypes the allowed relationship type strings
	 * @return the selected types in the same order as the items, or
	 *         {@code null} if the user cancelled the dialog
	 */
	public static List<String> showIfNeeded(final Window owner, final List<Item> items,
		final String[] allowedTypes){
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");
		if(items == null || items.isEmpty())
			return Collections.emptyList();

		if(allowedTypes.length == 1)
			return Collections.nCopies(items.size(), allowedTypes[0]);

		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(owner, items, allowedTypes);
		dialog.setVisible(true);
		return dialog.getSelectedTypes();
	}


	private void initUI(){
		final JPanel mainPanel = new JPanel(new MigLayout("ins 15,fill", "[grow]", "[][][grow][][30!]"));

		// Header Section
		final JLabel header = new JLabel("Select relationship type for each entity:");
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		mainPanel.add(header, "wrap, gapbottom 5");

		mainPanel.add(new JSeparator(), "growx, wrap, gapbottom 10");

		// Content Table
		final JPanel tablePanel = new JPanel(new MigLayout("ins 0,fillx,gap 10 8", "[grow, fill][grow, fill]", "[]"));

		// Column headers
		final JLabel targetHeader = new JLabel("Target");
		targetHeader.setFont(targetHeader.getFont().deriveFont(Font.BOLD));
		final JLabel typeHeader = new JLabel("Relationship Type");
		typeHeader.setFont(typeHeader.getFont().deriveFont(Font.BOLD));

		tablePanel.add(targetHeader);
		tablePanel.add(typeHeader, "wrap");

		for(final Item item : items){
			final JLabel itemLabel = new JLabel(item.label);
			tablePanel.add(itemLabel, "aligny center");

			final JComboBox<String> combo = new BoundComboBox<>(null, allowedTypes);
			combo.setSelectedItem(item.defaultType);
			comboBoxes.add(combo);
			tablePanel.add(combo, "wrap");
		}

		final JScrollPane scrollPane = new JScrollPane(tablePanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		final int preferredHeight = Math.min(250, items.size() * 38 + 30);
		scrollPane.setPreferredSize(new Dimension(450, preferredHeight));

		mainPanel.add(scrollPane, "grow,wrap,gapbottom 10");

		mainPanel.add(new JSeparator(), "growx,wrap,gapbottom 10");

		// Buttons
		final JPanel buttonPanel = new JPanel(new MigLayout("ins 0", "[grow][100!][100!]", "[]"));
		final JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> onOk());
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> onCancel());

		buttonPanel.add(okButton, "cell 1 0,growx");
		buttonPanel.add(cancelButton, "cell 2 0,growx");

		mainPanel.add(buttonPanel, "growx,align right");

		setContentPane(mainPanel);
		getRootPane().setDefaultButton(okButton);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void onOk(){
		selectedTypes = new ArrayList<>();
		for(final JComboBox<String> combo : comboBoxes){
			final String selected = (String)combo.getSelectedItem();
			selectedTypes.add(selected != null? selected: allowedTypes[0]);
		}
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
