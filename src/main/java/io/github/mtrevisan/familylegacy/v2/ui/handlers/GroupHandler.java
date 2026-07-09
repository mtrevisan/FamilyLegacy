package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupDialog;

import java.awt.*;


public class GroupHandler implements RecordTypeHandler<GroupDialog>{

	@Override
	public String getType(){
		return "GROUP";
	}

	@Override
	public String getDisplayName(FLEFRecord record){
		String name = getChildValue(record, "NAME");
		String id = record.getId();
		if(name != null && !name.isEmpty()){
			return name + " (" + id + ")";
		}
		else{
			return id;
		}
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
	public GroupDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new GroupDialog(parent, model, record);
	}

	@Override
	public GroupDialog createNewDialog(Frame parent, FLEFModel model){
		return new GroupDialog(parent, model);
	}

}
