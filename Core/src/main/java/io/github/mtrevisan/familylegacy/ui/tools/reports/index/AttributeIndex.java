package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public final class AttributeIndex{

	private final Map<String, List<FLEFRecord>> personToAttrsMap = new HashMap<>();

	public AttributeIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(int i = 0, size = attributes.size(); i < size; i++){
			final FLEFRecord attr = attributes.get(i);
			if(!filter.test(attr))
				continue;

			final String indId = FLEFRecordHelper.getChildValue(attr, IndividualHandler.TYPE);
			if(indId != null){
				personToAttrsMap.computeIfAbsent(indId, k -> new ArrayList<>()).add(attr);
			}
		}
	}

	public List<FLEFRecord> attributesOf(final FLEFRecord person){
		if(person == null || person.getId() == null)
			return Collections.emptyList();
		return personToAttrsMap.getOrDefault(person.getId(), Collections.emptyList());
	}

}
