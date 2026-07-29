package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NameStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


public class NameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -922034547054981789L;


	public NameListPanel(final Dialog parentDialog, final FLEFModel model){
		super(parentDialog, "Names*", model);
	}


	@Override
	protected String getDisplay(final FLEFRecord nameRecord){
		if(nameRecord == null)
			return "[empty]";

		final FLEFRecord valueRecord = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		final String text = (valueRecord != null? valueRecord.getValue(): null);
		final String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");

		final StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.isNotBlank(text)? text: "[unnamed]");
		if(StringUtils.isNotBlank(type))
			sb.append(" (").append(type).append(")");
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, null);
		dialog.setVisible(true);
		return (dialog.isSaved()? dialog.getNameRecord(): null);
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord existing){
		final NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, existing);
		dialog.setVisible(true);
		return (dialog.isSaved()? dialog.getNameRecord(): null);
	}

	public void loadFromRecord(final FLEFRecord parentRecord){
		final List<FLEFRecord> names = new ArrayList<>();
		for(final FLEFRecord child : parentRecord.getChildren())
			if("NAME".equals(child.getTag()))
				names.add(child);
		setItems(names);
	}

	public void saveToRecord(final FLEFRecord parentRecord){
		final List<FLEFRecord> toRemove = new ArrayList<>();
		for(final FLEFRecord child : parentRecord.getChildren())
			if("NAME".equals(child.getTag()))
				toRemove.add(child);
		for(final FLEFRecord child : toRemove)
			parentRecord.removeChild(child);
		for(final FLEFRecord name : getItems()){
			name.setTag("NAME");
			parentRecord.addChild(name);
		}
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
