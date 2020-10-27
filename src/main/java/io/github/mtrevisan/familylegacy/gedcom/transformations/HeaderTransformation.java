package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;

import java.util.Map;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		TransformationHelper.moveValueOfKey("tag", "HEADER", context, "HEAD");
		TransformationHelper.moveValueOfKey("tag", "SOURCE", context, "HEAD", "SOUR");
		TransformationHelper.moveValueOfKey("tag", "VERSION", context, "HEADER", "SOURCE", "VERS");
		TransformationHelper.moveValueOfKey("tag", "CORPORATE", context, "HEADER", "SOURCE", "CORP");
		//TODO extract place from sourceCorporate
		final Map<String, Object> sourceCorporatePlace = TransformationHelper.extractPlace(context, "HEADER", "SOURCE", "CORPORATE", "ADDR");

		TransformationHelper.deleteKey(context, "HEAD", "DEST");
		TransformationHelper.deleteKey(context, "HEAD", "DATE");
		final Map<String, Object> submitter = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "SUBM");
		TransformationHelper.deleteKey(context, "HEAD", "SUBN");
		TransformationHelper.deleteKey(context, "HEAD", "FILE");
		final Map<String, Object> copyright = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "COPR");
		final Map<String, Object> gedcom = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "GEDC");
		final Map<String, Object> charset = (Map<String, Object>)TransformationHelper.getStructure(context, "HEAD", "CHAR");
		TransformationHelper.deleteKey(context, "HEAD", "LANG");
		TransformationHelper.deleteKey(context, "HEAD", "PLACE");
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
