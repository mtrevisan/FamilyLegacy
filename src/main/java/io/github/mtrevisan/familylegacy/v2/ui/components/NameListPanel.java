package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NameStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.io.Serial;


public class NameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -922034547054981789L;


	private final String path;


	public NameListPanel(final String path, final Dialog parentDialog, final FLEFModel model){
		super(parentDialog, "Names*", model);

		this.path = path;
	}


	@Override
	protected String getDisplay(final FLEFRecord nameRecord){
		if(nameRecord == null)
			return "[empty]";

		final FLEFRecord valueRecord = FLEFRecordHelper.findChild(nameRecord, "VALUE");
		final String text = (valueRecord != null? valueRecord.getValue(): null);
		final String type = FLEFRecordHelper.getChildValue(nameRecord, "TYPE");

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

	public void load(final FLEFRecord parentRecord){
		setItems(FLEFRecordHelper.findChildren(parentRecord, path));
	}

	public void save(final FLEFRecord parentRecord){
		FLEFRecordHelper.removeChildren(parentRecord, path);

		for(final FLEFRecord name : getItems()){
			name.setTag("NAME");
			parentRecord.addChild(name);
		}
	}

	public boolean hasData(){
		return !isEmpty();
	}

}
