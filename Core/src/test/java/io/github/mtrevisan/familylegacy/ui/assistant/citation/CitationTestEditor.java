package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import io.github.mtrevisan.familylegacy.ui.dialogs.citations.CitationHost;

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
