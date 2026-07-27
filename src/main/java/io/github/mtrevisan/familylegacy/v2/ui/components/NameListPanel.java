package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.NameStructureDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;
import java.io.Serial;


public class NameListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -922034547054981789L;


	public NameListPanel(FLEFModel model, Dialog parentDialog){
		super(model, parentDialog, "Names*");
	}

	@Override
	protected String getDisplay(FLEFRecord nameRecord){
		if(nameRecord == null){
			return "[empty]";
		}
		FLEFRecord valueRec = FLEFRecordUtils.findChild(nameRecord, "VALUE");
		String text = valueRec != null ? valueRec.getValue() : null;
		String type = FLEFRecordUtils.getChildValue(nameRecord, "TYPE");

		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.isNotBlank(text) ? text : "[unnamed]");
		if(StringUtils.isNotBlank(type)){
			sb.append(" (").append(type).append(")");
		}
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, null);
		dialog.setVisible(true);
		return (dialog.isSaved() ? dialog.getNameRecord() : null);
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		NameStructureDialog dialog = new NameStructureDialog(parentDialog, model, existing);
		dialog.setVisible(true);
		return (dialog.isSaved() ? dialog.getNameRecord() : null);
	}

}
