package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.SourceDialog;

import java.awt.*;


public class SourceHandler implements RecordTypeHandler<SourceDialog>{

	@Override
	public String getType(){
		return "SOURCE";
	}

	@Override
	public String getDisplayName(FLEFRecord record){
		String title = getChildValue(record, "TITLE");
		String id = record.getId();
		if(title != null && !title.isEmpty()){
			return title + " (" + id + ")";
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
	public SourceDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new SourceDialog(parent, model, record);
	}

	@Override
	public SourceDialog createNewDialog(Frame parent, FLEFModel model){
		return new SourceDialog(parent, model);
	}

}
