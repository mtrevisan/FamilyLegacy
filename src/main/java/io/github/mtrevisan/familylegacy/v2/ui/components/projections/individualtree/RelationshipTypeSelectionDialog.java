package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that displays a list of items (e.g., parents or children) with a combo box for each.
 * The user selects a relationship type for each item.
 * Returns a list of selected types in the same order as the input items, or null if canceled.
 */
public class RelationshipTypeSelectionDialog extends JDialog{

	/**
	 * Represents one row in the dialog.
	 */
	public static class Item{
		public final String label;
		public final String defaultType;

		public Item(final String label, final String defaultType){
			this.label = label;
			this.defaultType = defaultType;
		}
	}


	private final List<Item> items;
	private final String[] allowedTypes;

	private final List<JComboBox<String>> comboBoxes = new ArrayList<>();
	private List<String> selectedTypes;
	private boolean canceled = true;

	/**
	 * Constructor.
	 *
	 * @param owner        parent window
	 * @param items        list of items to show (non‑empty)
	 * @param allowedTypes the allowed relationship type strings
	 */
	public RelationshipTypeSelectionDialog(final Window owner, final List<Item> items, final String[] allowedTypes){
		super(owner, "Select Relationship Types", ModalityType.APPLICATION_MODAL);

		if(items == null || items.isEmpty())
			throw new IllegalArgumentException("Items list must not be empty");

		this.items = new ArrayList<>(items);
		this.allowedTypes = allowedTypes;

		initUI();

		pack();

		setLocationRelativeTo(owner);
	}

	private void initUI(){
		final JPanel mainPanel = new JPanel(new MigLayout("fill,ins 10", "[grow]", "[][grow][]"));

		// Header
		final JLabel header = new JLabel("Select relationship type for each entry:");
		mainPanel.add(header, "wrap,span,gapbottom 10");

		// Table of items
		final JPanel tablePanel = new JPanel(new MigLayout("ins 0", "[grow][grow]", "[]"));
		// Column headers
		tablePanel.add(new JLabel("Target"), "split 2,align left");
		tablePanel.add(new JLabel("Relationship Type"), "align left,wrap");

		for(final Item item : items){
			// Label
			tablePanel.add(new JLabel(item.label), "split 2,align left");

			// Combo box for this item
			final JComboBox<String> combo = new JComboBox<>(allowedTypes);
			combo.setSelectedItem(item.defaultType);
			comboBoxes.add(combo);
			tablePanel.add(combo, "align left,wrap");
		}

		final JScrollPane scrollPane = new JScrollPane(tablePanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(400, Math.min(300, items.size() * 35 + 40)));
		mainPanel.add(scrollPane, "grow,span,wrap,gapbottom 10");

		// Buttons
		final JPanel buttonPanel = new JPanel(new MigLayout("ins 0", "[][][]", "[]"));
		final JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> onOk());
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> onCancel());

		buttonPanel.add(okButton, "split 2,align right");
		buttonPanel.add(cancelButton, "align right");

		mainPanel.add(buttonPanel, "span,align right");

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

	/**
	 * Convenience method to show the dialog and get the result.
	 *
	 * @param owner        parent window
	 * @param items        list of items
	 * @param allowedTypes allowed types
	 * @return selected types list, or null if canceled
	 */
	public static List<String> showDialog(final Window owner, final List<Item> items, final String[] allowedTypes){
		final RelationshipTypeSelectionDialog dialog = new RelationshipTypeSelectionDialog(owner, items, allowedTypes);
		dialog.setVisible(true);

		return dialog.getSelectedTypes();
	}

}
