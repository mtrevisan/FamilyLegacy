package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;


public class SubmissionRecordTransformation implements Transformation{

	@Override
	public void to(final DocumentContext context){
		context.delete("$.children[?(@.tag=='SUBN')]");
	}

	@Override
	public void from(final DocumentContext context){
		//information is lost, cannot recover
	}

}
