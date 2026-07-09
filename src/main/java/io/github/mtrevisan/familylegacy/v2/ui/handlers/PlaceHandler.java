package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.PlaceDialog;

import java.awt.*;


public class PlaceHandler implements RecordTypeHandler<PlaceDialog>{

	@Override
	public String getType(){
		return "PLACE";
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
	public PlaceDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new PlaceDialog(parent, model, record);
	}

	@Override
	public PlaceDialog createNewDialog(Frame parent, FLEFModel model){
		return new PlaceDialog(parent, model);
	}

}
