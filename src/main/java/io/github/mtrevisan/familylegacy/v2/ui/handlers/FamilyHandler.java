package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.FamilyDialog;

import java.awt.Frame;


public class FamilyHandler implements RecordTypeHandler<FamilyDialog>{

	@Override
	public String getLabel(){
		return "Family";
	}

	@Override
	public String getType(){
		return "FAMILY";
	}

	@Override
	public String getDisplayName(FLEFRecord record){
		String id = record.getId();
		String p1 = getChildValue(record, "PARTNER1");
		String p2 = getChildValue(record, "PARTNER2");
		if(p1 != null && p2 != null){
			return p1 + " + " + p2 + " (" + id + ")";
		}
		else if(p1 != null){
			return p1 + " (with partner) (" + id + ")";
		}
		else if(p2 != null){
			return p2 + " (with partner) (" + id + ")";
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
	public FamilyDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return FamilyDialog.createEdit(parent, model, record);
	}

	@Override
	public FamilyDialog createNewDialog(Frame parent, FLEFModel model){
		return FamilyDialog.createNew(parent, model);
	}

}
