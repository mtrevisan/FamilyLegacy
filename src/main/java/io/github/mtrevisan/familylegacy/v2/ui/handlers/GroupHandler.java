package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GroupDialog2;

import java.awt.Frame;


public class GroupHandler implements RecordTypeHandler<GroupDialog2>{

	public static final String TYPE = "GROUP";
	public static final String ID_PREFIX = "G";


	@Override
	public String getLabel(){
		return "Group";
	}

	@Override
	public String getType(){
		return TYPE;
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
	public GroupDialog2 createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new GroupDialog2(parent, model, record);
	}

	@Override
	public GroupDialog2 createNewDialog(Frame parent, FLEFModel model){
		return new GroupDialog2(parent, model);
	}

}
