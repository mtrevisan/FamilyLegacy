package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel that manages a list of UPDATE entries (date + comment) for a modification structure.
 * Extends {@link AbstractListPanel} with {@link FLEFRecord} items.
 * Follows the same pattern as {@link CulturalNormListPanel} and {@link NoteListPanel}.
 */
public class UpdateListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -6236872036080417379L;


	private static final String TAG_UPDATE = "UPDATE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_COMMENT = "COMMENT";


	public UpdateListPanel(Dialog parent, FLEFModel model){
		super(parent, "Updates", model);
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(FLEFRecord update){
		if(update == null){
			return "--";
		}

		String date = FLEFRecordHelper.getChildValue(update, TAG_DATE);
		String comment = FLEFRecordHelper.getChildValue(update, TAG_COMMENT);

		StringBuilder sb = new StringBuilder(StringUtils.defaultString(date));
		if(comment != null && !comment.isEmpty()){
			sb.append(": ").append(comment);
		}

		// Truncate for display if too long
		if(sb.length() > 60){
			return sb.substring(0, 57) + "...";
		}
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showUpdateDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Update entry not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		return showUpdateDialog(existing);
	}

	/**
	 * Shows a dialog to create or edit an update entry.
	 *
	 * @param existing the existing update record, or {@code null} for a new one
	 * @return the (possibly updated) record, or {@code null} if cancelled
	 */
	private FLEFRecord showUpdateDialog(FLEFRecord existing){
		JDialog dialog = new JDialog(parent, (existing == null? "Add Update": "Edit Update"), true);
		dialog.setLayout(new MigLayout("ins 10, fillx", "[grow]", "[]10[]"));

		// Comment field
		String initialComment = (existing != null)? FLEFRecordHelper.getChildValue(existing, TAG_COMMENT): "";
		JTextArea commentArea = new JTextArea(StringUtils.defaultString(initialComment), 3, 25);
		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);
		JScrollPane commentScroll = GUIHelper.createScrollPane(commentArea);

		dialog.add(commentScroll, "growx,wrap");

		// Buttons
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "growx");

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			String date = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
			String comment = commentArea.getText()
				.trim();

			// If editing, keep the original date; for new entries, use current date.
			String finalDate = (existing != null? FLEFRecordHelper.getChildValue(existing, TAG_DATE): date);

			FLEFRecord updateRecord = (existing != null? existing: FLEFRecord.createEmpty());
			updateRecord.setTag(TAG_UPDATE);
			FLEFRecordHelper.updateChildValue(updateRecord, TAG_DATE, finalDate);
			FLEFRecordHelper.updateChildValue(updateRecord, TAG_COMMENT, comment);

			result[0] = updateRecord;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	/**
	 * Loads all UPDATE entries from the given record's children.
	 *
	 * @param record the record containing UPDATE children
	 */
	public void load(FLEFRecord record){
		List<FLEFRecord> entries = new ArrayList<>();
		for(FLEFRecord child : record.getChildren()){
			if(TAG_UPDATE.equals(child.getTag())){
				entries.add(child);
			}
		}
		setItems(entries);
	}

	/**
	 * Saves the current list of update entries as children of the given record.
	 * All existing UPDATE children are removed and replaced.
	 *
	 * @param record the record to save into
	 */
	public void save(FLEFRecord record){
		// Remove all existing UPDATE children
		FLEFRecordHelper.removeChildren(record, TAG_UPDATE);

		// Add each update as a child
		for(FLEFRecord update : getItems()){
			update.setTag(TAG_UPDATE);
			record.addChild(update);
		}
	}

}
