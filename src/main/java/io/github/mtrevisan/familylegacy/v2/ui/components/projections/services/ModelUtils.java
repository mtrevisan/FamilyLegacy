package io.github.mtrevisan.familylegacy.v2.ui.components.projections.services;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;


public final class ModelUtils{

	private ModelUtils(){}

	public static String findLowestIndividualId(final FLEFModel model){
		String bestId = null;
		long bestNumber = Long.MAX_VALUE;
		for(final FLEFRecord record : model.getRecords()){
			if(!IndividualHandler.TYPE.equalsIgnoreCase(record.getTag()))
				continue;

			final String id = record.getId();
			if(id == null)
				continue;

			final long number = numericSuffix(id);
			if(number < bestNumber){
				bestNumber = number;
				bestId = id;
			}
		}
		return bestId;
	}

	private static long numericSuffix(final String id){
		int i = id.length();
		while(i > 0 && Character.isDigit(id.charAt(i - 1)))
			i --;

		if(i == id.length())
			return Long.MAX_VALUE;

		try{
			return Long.parseLong(id.substring(i));
		}
		catch(final NumberFormatException ex){
			return Long.MAX_VALUE;
		}
	}

}
