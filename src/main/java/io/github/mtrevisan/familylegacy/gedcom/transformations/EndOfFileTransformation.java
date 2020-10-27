package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;


public class EndOfFileTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		GedcomTransformationHelper.transformValue(context, "$.children[?(@.tag=='TRLR')]", "tag", "EOF");
	}

	@Override
	public void from(final DocumentContext context){
		GedcomTransformationHelper.transformValue(context, "$.children[?(@.tag=='EOF')]", "tag", "TRLR");
	}

}
