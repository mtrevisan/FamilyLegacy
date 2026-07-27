package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.IndividualDialog;

import java.awt.Frame;


public class IndividualHandler implements RecordTypeHandler<IndividualDialog>{

	public static final String TYPE = "INDIVIDUAL";
	public static final String ID_PREFIX = "I";


	@Override
	public String getLabel(){
		return "Individual";
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
	public String getDisplayName(FLEFRecord record){
		// Try to get name from NAME structure
		String given = null, family = null, id = record.getId();
		for(FLEFRecord child : record.getChildren()){
			if("NAME".equals(child.getTag())){
				given = getChildValue(child, "INDIVIDUAL_NAME");
				family = getChildValue(child, "FAMILY_NAME");
				break;
			}
		}
		if(given != null && family != null){
			return given + " " + family + " (" + id + ")";
		}
		else if(given != null){
			return given + " (" + id + ")";
		}
		else if(family != null){
			return family + " (" + id + ")";
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
	public IndividualDialog createEditDialog(Frame parent, FLEFModel model, FLEFRecord record){
		return IndividualDialog.createEdit(parent, model, record);
	}

	@Override
	public IndividualDialog createNewDialog(Frame parent, FLEFModel model){
		return IndividualDialog.createNew(parent, model);
	}

}
