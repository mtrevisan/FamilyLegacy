package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PartDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/* DONE */
public class PartListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = 2221818245328724967L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_PHONETIC = "PHONETIC";
	private static final String TAG_TRANSCRIPTION = "TRANSCRIPTION";


	private final String path;


	public PartListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Parts", model);

		this.path = path;
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
	protected String getDisplay(final FLEFRecord part){
		if(part == null)
			return "--";

		final String type = FLEFRecordHelper.getChildValue(part, TAG_TYPE);
		final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
		final StringBuilder sb = new StringBuilder();

		if(StringUtils.isNotEmpty(type))
			sb.append("[")
				.append(type)
				.append("] ");
		sb.append(StringUtils.defaultString(value));

		final List<FLEFRecord> phonetics = FLEFRecordHelper.findChildren(part, TAG_PHONETIC);
		final List<FLEFRecord> transcriptions = FLEFRecordHelper.findChildren(part, TAG_TRANSCRIPTION);
		final int variantCount = phonetics.size() + transcriptions.size();
		if(variantCount > 0)
			sb.append(" (")
				.append(variantCount)
				.append(" variants)");
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		final PartDialog dialog = PartDialog.createNew(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		if(existing == null){
			JOptionPane.showMessageDialog(parent, "Part not found", "Error", JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final PartDialog dialog = PartDialog.createEdit(parent, model, existing);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> parts = new ArrayList<>();
		if(record != null)
			parts.addAll(FLEFRecordHelper.findChildren(record, path));
		setItems(parts);
	}

	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		for(final FLEFRecord part : getItems())
			record.addChild(part);
	}

	public boolean hasData(){
		return !isEmpty();
	}

	public boolean validateData(){
		for(final FLEFRecord part : getItems()){
			final String value = FLEFRecordHelper.getChildValue(part, TAG_VALUE);
			if(StringUtils.isEmpty(value)){
				JOptionPane.showMessageDialog(parent,
					"Part has no value.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);

				return false;
			}
		}
		return true;
	}

}
