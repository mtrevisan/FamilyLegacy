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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDispatcher;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.EditIndividualTool;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
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


	private final FLEFModel model;
	private final List<Item> items;
	private final String[] allowedTypes;
	private final IndividualListener listener;

	private final List<JComboBox<String>> comboBoxes = new ArrayList<>();
	private List<String> selectedTypes;
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
		super(owner, "Select Relationship Types", ModalityType.APPLICATION_MODAL);

		if(items == null || items.isEmpty())
			throw new IllegalArgumentException("Items list must not be empty");
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");

		this.model = model;
		this.items = items;
		this.allowedTypes = allowedTypes;
		this.listener = listener;

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


	private void initComponents(){
		setLayout(GUIHelper.createLabelFieldLayout(10, "[grow]"));

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
			itemLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			// Double click on target name triggers edit via ToolContext
			itemLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && item.id() != null){
						final ToolContext context = new ToolContext(
							model,
							item::id,
							() -> itemLabel,
							new ToolDispatcher(){
								@Override
								public void editEntity(final String id){
									if(listener != null)
										listener.onEntityEdit(model.getRecordById(id));
								}
							}
						);
						new EditIndividualTool().run(context);
					}
				}
			});

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

		add(scrollPane, "grow,wrap,gapbottom 10");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			"Confirm", this::onOk,
			"Cancel", this::onCancel);
		add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton((JButton)buttonPanel.getComponent(0));
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
