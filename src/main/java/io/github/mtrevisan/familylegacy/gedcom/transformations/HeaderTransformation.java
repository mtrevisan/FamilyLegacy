package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;


public class HeaderTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		GedcomTransformationHelper.transformValue(context, "$.children[?(@.tag=='HEAD')]", "tag", "HEADER");
	}

	@Override
	public void from(final DocumentContext context){
		GedcomTransformationHelper.transformValue(context, "$.children[?(@.tag=='HEADER')]", "tag", "HEAD");
	}

}
