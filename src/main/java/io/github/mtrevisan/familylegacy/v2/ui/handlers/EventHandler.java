package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.EventDialog;

import java.awt.*;


public class EventHandler implements RecordTypeHandler<EventDialog>{

	@Override
	public String getType(){
		return "EVENT";
	}

	@Override
	public String getDisplayName(FLEFRecord record){
		String type = getChildValue(record, "TYPE");
		String date = getChildValue(record, "DATE");
		String id = record.getId();
		StringBuilder sb = new StringBuilder();
		if(type != null) sb.append(type);
		if(date != null){
			if(sb.length() > 0) sb.append(" ");
			sb.append("(").append(date).append(")");
		}
		if(sb.length() == 0) sb.append("Event");
		sb.append(" (").append(id).append(")");
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
	public EventDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new EventDialog(parent, model, record);
	}

	@Override
	public EventDialog createNewDialog(Frame parent, FLEFModel model){
		return new EventDialog(parent, model);
	}

}
