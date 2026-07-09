package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Definition of a record or structure: a list of allowed tags with their cardinalities.
 */
public final class RecordDefinition{

	private final String name;
	private final List<TagDefinition> tags = new ArrayList<>();
	private String rootTag; // only meaningful for structures


	public RecordDefinition(String name){
		this.name = name;
	}


	public String getName(){
		return name;
	}

	public void addTag(TagDefinition tag){
		tags.add(tag);
	}

	public List<TagDefinition> getTags(){
		return tags;
	}

	public String getRootTag(){
		return rootTag;
	}

	public void setRootTag(String rootTag){
		this.rootTag = rootTag;
	}

	/**
	 * Finds the definition for a given tag name.
	 */
	public Optional<TagDefinition> getTagDefinition(String tagName){
		return tags.stream()
			.filter(td -> td.getName().equals(tagName))
			.findFirst();
	}

	@Override
	public String toString(){
		return name + " := " + tags;
	}

}
