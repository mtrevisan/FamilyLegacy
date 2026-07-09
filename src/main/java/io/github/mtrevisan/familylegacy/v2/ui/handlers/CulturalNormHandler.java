package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.CulturalNormDialog;

import java.awt.*;


public class CulturalNormHandler implements RecordTypeHandler<CulturalNormDialog>{

	@Override
	public String getType(){
		return "CULTURAL_NORM";
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
	public CulturalNormDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return new CulturalNormDialog(parent, model, record);
	}

	@Override
	public CulturalNormDialog createNewDialog(Frame parent, FLEFModel model){
		return new CulturalNormDialog(parent, model);
	}

}
