package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.Map;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		final Map<String, Object> header = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD");
		final Map<String, Object> source = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "SOUR");
		final Map<String, Object> submitter = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "SUBM");
		final Map<String, Object> copyright = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "COPR");
		final Map<String, Object> gedcom = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "GEDC");
		final Map<String, Object> charset = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "CHAR");

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
		TransformationHelper.mergeNote(context, "HEADER", "NOTE");
	}

	@Override
	public void from(final DocumentContext context){
		final Map<String, Object> header = (Map<String, Object>)TransformationHelper.getStructure(context, "HEADER");
		final Map<String, Object> source = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "SOURCE");
		final Map<String, Object> submitter = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "SUBMITTER");
		final Map<String, Object> copyright = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "COPYRIGHT");
		final Map<String, Object> protocolVersion = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "PROTOCOL_VERSION");
		final Map<String, Object> charset = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "CHARSET");
		final Map<String, Object> note = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "NOTE");

		header.put("tag", "HEAD");
		//remove place
		context.delete("$.children[?(@.tag=='CHANGE')]");
	}

}
