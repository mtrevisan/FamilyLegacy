package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._EventDialog;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dialog;


public class EventHandler implements RecordTypeHandler<_EventDialog>{

	public static final String TYPE = "EVENT";
	public static final String ID_PREFIX = "E";


	@Override
	public String getLabel(){
		return "Event";
	}

	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIDPrefix(){
		return ID_PREFIX;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		String type = getChildValue(record, "TYPE");
		String date = getChildValue(record, "DATE");
		String id = record.getId();
		StringBuilder sb = new StringBuilder();
		if(type != null)
			sb.append(type);
		if(date != null){
			if(!sb.isEmpty())
				sb.append(StringUtils.SPACE);
			sb.append("(")
				.append(date)
				.append(")");
		}
		if(sb.isEmpty())
			sb.append("Event");
		sb.append(" (")
			.append(id)
			.append(")");
		return sb.toString();
	}

	private String getChildValue(FLEFRecord parent, String tag){
		for(FLEFRecord child : parent.getChildren()){
			if(tag.equals(child.getTag())){
				return child.getValue();
			}
		}
		return null;
	}

	@Override
	public _EventDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return new _EventDialog(parent, model, record);
	}

	@Override
	public _EventDialog createNewDialog(Dialog parent, FLEFModel model){
		return new _EventDialog(parent, model);
	}

}
