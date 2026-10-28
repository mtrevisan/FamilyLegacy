package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

import javax.swing.JPanel;


/** An editor of a citation, for the tests. */
public class CitationTestEditor extends JPanel implements CitationHost{

	@Override
	public String locator(){
		return "";
	}

	@Override
	public void applyLocator(final String locator){}

	@Override
	public FLEFRecord citedSource(){
		return null;
	}

}
