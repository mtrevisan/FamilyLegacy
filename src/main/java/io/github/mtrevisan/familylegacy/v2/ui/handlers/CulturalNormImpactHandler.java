package io.github.mtrevisan.familylegacy.v2.ui.handlers;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogstodo._CulturalNormImpactRecordDialog;

import java.awt.Dialog;


public class CulturalNormImpactHandler implements RecordTypeHandler<_CulturalNormImpactRecordDialog>{

	public static final String TYPE = "cultural_norm_impact";


	@Override
	public String getType(){
		return TYPE;
	}

	@Override
	public String getIdPrefix(){
		return "";
	}

	@Override
	public String getLabel(){
		return "Cultural Norm Impact";
	}

	@Override
	public String getDisplayText(FLEFRecord record, FLEFModel model){
		// Return a meaningful display, e.g., the norm reference or significance
		String normRef = FLEFRecordHelper.getChildValue(record, "CULTURAL_NORM");
		String sig = FLEFRecordHelper.getChildValue(record, "SIGNIFICANCE");
		if(normRef != null && sig != null) return normRef + " — " + sig;
		return normRef != null? normRef: "(unnamed)";
	}

	@Override
	public _CulturalNormImpactRecordDialog createNewDialog(Dialog parent, FLEFModel model){
		return _CulturalNormImpactRecordDialog.createNew(parent, model);
	}

	@Override
	public _CulturalNormImpactRecordDialog createEditDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		return _CulturalNormImpactRecordDialog.createEdit(parent, model, record);
	}

}