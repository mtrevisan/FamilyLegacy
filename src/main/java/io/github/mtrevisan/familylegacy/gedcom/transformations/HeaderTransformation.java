package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		final Map<String, Object> header = getHeader(context, "HEAD");
		final Map<String, Object> source = getHeaderStructure(context, "HEAD", "SOUR");
		final Map<String, Object> submitter = getHeaderStructure(context, "HEAD", "SUBM");
		final Map<String, Object> copyright = getHeaderStructure(context, "HEAD", "COPR");
		final Map<String, Object> gedcom = getHeaderStructure(context, "HEAD", "GEDC");
		final Map<String, Object> charset = getHeaderStructure(context, "HEAD", "CHAR");
		final Map<String, Object> note = getHeaderStructure(context, "HEAD", "NOTE");

		header.put("tag", "HEADER");
		//remove destination
		context.delete("$.children[?(@.tag=='DEST')]");
		//remove date
		context.delete("$.children[?(@.tag=='DATE')]");
		//remove submissions
		context.delete("$.children[?(@.tag=='SUBN')]");
		//remove file
		context.delete("$.children[?(@.tag=='FILE')]");
		//remove language
		context.delete("$.children[?(@.tag=='LANG')]");
		//remove place
		context.delete("$.children[?(@.tag=='PLAC')]");
	}

	@Override
	public void from(final DocumentContext context){
		final Map<String, Object> header = getHeader(context, "HEADER");
		final Map<String, Object> source = getHeaderStructure(context, "HEAD", "SOURCE");
		final Map<String, Object> submitter = getHeaderStructure(context, "HEAD", "SUBMITTER");
		final Map<String, Object> copyright = getHeaderStructure(context, "HEAD", "COPYRIGHT");
		final Map<String, Object> protocolVersion = getHeaderStructure(context, "HEAD", "PROTOCOL_VERSION");
		final Map<String, Object> charset = getHeaderStructure(context, "HEAD", "CHARSET");
		final Map<String, Object> note = getHeaderStructure(context, "HEAD", "NOTE");

		header.put("tag", "HEAD");
		//remove place
		context.delete("$.children[?(@.tag=='CHANGE')]");
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getHeader(final DocumentContext context, final String key){
		final List<Object> elements = context.read("$.children[?(@.tag=='" + key + "')]");
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		return (Map<String, Object>)elements.get(0);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getHeaderStructure(final DocumentContext context, final String key, final String subkey){
		final List<Object> elements = context.read("$.children[?(@.tag=='" + key + "')].children[?(@.tag=='" + subkey + "')]");
		if(elements.size() > 1)
			throw new IllegalArgumentException("Has to select at most one element, was selected " + elements.size());

		return (!elements.isEmpty()? (Map<String, Object>)elements.get(0): Collections.emptyMap());
	}

}
