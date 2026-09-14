package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceHelper;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * Modal dialog that lets the user select several individuals from a
 * filterable list. The dialog is used by the group member tools, which
 * need to build a set of individuals in one step rather than picking
 * them one at a time through {@code RecordSelectionDialog}.
 * <p>
 * The list is a {@link JList} in {@link ListSelectionModel#MULTIPLE_INTERVAL_SELECTION}
 * mode: the user can click to select one, Ctrl+click to toggle individual
 * entries, and Shift+click to select a range. The search field filters
 * the visible entries by name; entries already selected are preserved
 * across filter changes, so the user can build a selection incrementally.
 */
public final class MultiIndividualPickerDialog extends JDialog{

	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> list = new JList<>(listModel);
	private final JTextField searchField = new JTextField(20);

	private final List<Entry> allEntries = new ArrayList<>();
	private final Set<String> selectedIds = new LinkedHashSet<>();

	private boolean accepted;


	private record Entry(String id, String label){
		@Override public String toString(){ return label; }
	}


	private MultiIndividualPickerDialog(final Window owner, final FLEFModel model){
		super(owner, "Select individuals", ModalityType.APPLICATION_MODAL);

		// Build the full list once. Each entry carries the id and the
		// display name, resolved from the individual record.
		for(final FLEFRecord individual : model.getRecordsByType(GroupHelper.TYPE_INDIVIDUAL)){
			final String id = individual.getId();
			if(id == null)
				continue;
			final String name = PlaceHelper.displayName(individual);
			allEntries.add(new Entry(id, (name != null && !name.isBlank()? name: id) + "  [" + id + "]"));
		}
		allEntries.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));

		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(14);
		// Restore the selection when the user changes filter, so entries
		// selected earlier are not lost.
		list.addListSelectionListener(e -> {
			if(e.getValueIsAdjusting())
				return;
			selectedIds.clear();
			for(final Entry entry : list.getSelectedValuesList())
				selectedIds.add(entry.id());
		});

		applyFilter("");

		setLayout(new BorderLayout(6, 6));

		final JPanel top = new JPanel(new BorderLayout(4, 4));
		top.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
		top.add(new JLabel("Filter:"), BorderLayout.WEST);
		top.add(searchField, BorderLayout.CENTER);
		add(top, BorderLayout.NORTH);

		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(searchField.getText()); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(searchField.getText()); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(searchField.getText()); }
		});

		final JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(420, 320));
		add(scroll, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
		final JButton ok = new JButton("OK");
		ok.addActionListener(e -> {
			accepted = true;
			dispose();
		});
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);
		add(buttons, BorderLayout.SOUTH);

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(owner);
	}


	/**
	 * Opens the picker and returns the list of selected individual ids,
	 * or an empty list when the user cancels.
	 */
	public static List<String> pick(final Window owner, final FLEFModel model){
		final MultiIndividualPickerDialog dialog = new MultiIndividualPickerDialog(owner, model);
		dialog.setVisible(true);
		return (dialog.accepted? new ArrayList<>(dialog.selectedIds): List.of());
	}


	private void applyFilter(final String text){
		final String needle = (text == null? "": text.trim().toLowerCase(Locale.ROOT));

		// Remember the current selection before rebuilding the model.
		final List<String> previouslySelected = new ArrayList<>(selectedIds);

		listModel.clear();
		for(final Entry entry : allEntries)
			if(needle.isEmpty() || entry.label().toLowerCase(Locale.ROOT).contains(needle))
				listModel.addElement(entry);

		// Restore the selection on the entries that survived the filter.
		final List<Integer> indicesToSelect = new ArrayList<>();
		for(int i = 0; i < listModel.size(); i++)
			if(previouslySelected.contains(listModel.get(i).id()))
				indicesToSelect.add(i);
		list.setSelectedIndices(indicesToSelect.stream().mapToInt(Integer::intValue).toArray());
	}

}
