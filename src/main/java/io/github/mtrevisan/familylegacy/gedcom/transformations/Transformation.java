package io.github.mtrevisan.familylegacy.gedcom.transformations;

import com.jayway.jsonpath.DocumentContext;


public interface Transformation{

	void to(final DocumentContext context);

	void from(final DocumentContext context);

}
