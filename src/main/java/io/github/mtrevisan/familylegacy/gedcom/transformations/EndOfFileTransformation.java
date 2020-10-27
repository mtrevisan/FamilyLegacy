package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.List;
import java.util.Map;


public class EndOfFileTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		final Map<String, Object> header = getEndOfFile(context, "TRLR");
		header.put("tag", "EOF");
	}

	@Override
	public void from(final DocumentContext context){
		final Map<String, Object> header = getEndOfFile(context, "EOF");
		header.put("tag", "TRLR");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getEndOfFile(final DocumentContext context, final String key){
		final List<Object> elements = context.read("$.children[?(@.tag=='" + key + "')]");
		if(elements.size() > 1)
			throw new IllegalArgumentException("Selected has to select at most one element, was selected " + elements.size());

		return (Map<String, Object>)elements.get(0);
	}

}
