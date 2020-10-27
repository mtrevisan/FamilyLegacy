package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.List;
import java.util.Map;


final class GedcomTransformationHelper{

	private GedcomTransformationHelper(){}

	static void transformValue(final DocumentContext context, final String selector, final String key, final String newValue){
		final List<Object> elements = context.read(selector);
		if(elements.size() > 1)
			throw new IllegalArgumentException("Selected has to select at most one element, was selected " + elements.size());

		@SuppressWarnings("unchecked")
		final Map<String, Object> node = (Map<String, Object>)elements.get(0);
		node.put(key, newValue);
	}

}
