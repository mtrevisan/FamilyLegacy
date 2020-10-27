package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.List;
import java.util.Map;


final class TransformationHelper{

	private TransformationHelper(){}

	public static Object getStructure(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());
		return (!elements.isEmpty()? elements.get(0): null);
	}

	public static List<Object> getStructures(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		return context.read(selector.toString());
	}

	@SuppressWarnings("unchecked")
	public static void mergeNote(final DocumentContext context, final String... keys){
		final StringBuilder selector = composeSelector(keys);
		final List<Object> elements = context.read(selector.toString());
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		if(!elements.isEmpty()){
			final Map<String, Object> note = (Map<String, Object>)elements.get(0);

			final StringBuilder sb = new StringBuilder((String)note.get("value"));
			selector.append(".children[?(@.tag in ['CONC','CONT'])]");
			final List<Object> noteChildren = context.read(selector.toString());
			for(final Object noteChild : noteChildren){
				if(((CharSequence)((Map<String, Object>)noteChild).get("tag")).charAt(3) == 'T')
					sb.append("\\n");
				sb.append(((Map<String, Object>)noteChild).get("value"));
			}
			note.put("value", sb.toString());
			context.delete(selector.toString());
		}
	}

	private static StringBuilder composeSelector(final String[] keys){
		final StringBuilder selector = new StringBuilder("$");
		for(final String key : keys)
			selector.append(".children[?(@.tag=='").append(key).append("')]");
		return selector;
	}

}
