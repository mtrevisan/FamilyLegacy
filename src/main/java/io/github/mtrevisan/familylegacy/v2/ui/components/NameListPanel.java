package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NameStructureDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


public class NameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -922034547054981789L;


	private final String path;


	public NameListPanel(final String path, final Dialog parentDialog, final FLEFModel model){
		super(parentDialog, "Names*", model);

		this.path = path;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			() -> (list.getSelectedIndex() >= 0),
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
	protected String getDisplay(final FLEFRecord name){
		String value = FLEFRecordHelper.getChildValue(name, "VALUE");
		if(value != null && !value.isEmpty()){
			// Truncate long names
			if(value.length() > 50)
				value = value.substring(0, 50) + "...";

			final String type = FLEFRecordHelper.getChildValue(name, "TYPE");
			if(type != null && !type.isEmpty())
				value += " (" +  type + ")";

			return value;
		}
		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new name and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		final NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, null);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parentDialog, "Name not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, existing);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(record, path);
		setItems(names);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord name : getItems()){
			name.setTag("NAME");
			record.addChild(name);
		}
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
