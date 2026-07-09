package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.Objects;


/**
 * Definition of a single tag within a record or structure.
 * It may be a simple tag (with a value) or a reference to a nested structure.
 */
public final class TagDefinition{

	private final String name;
	private final Cardinality cardinality;
	private final boolean isStructure;
	private final String structureName; // only meaningful if isStructure == true

	private TagDefinition(String name, Cardinality cardinality, boolean isStructure, String structureName){
		this.name = Objects.requireNonNull(name);
		this.cardinality = Objects.requireNonNull(cardinality);
		this.isStructure = isStructure;
		this.structureName = structureName;
	}

	/**
	 * Creates a simple tag definition (not a structure).
	 */
	public static TagDefinition simple(String name, Cardinality cardinality){
		return new TagDefinition(name, cardinality, false, null);
	}

	/**
	 * Creates a structure tag definition (points to a sub-structure).
	 *
	 * @param name          the tag name (e.g., "NAME", "EVENT")
	 * @param cardinality   the cardinality
	 * @param structureName the name of the structure definition (e.g., "PERSONAL_NAME_STRUCTURE")
	 */
	public static TagDefinition structure(String name, Cardinality cardinality, String structureName){
		return new TagDefinition(name, cardinality, true, structureName);
	}

	public String getName(){
		return name;
	}

	public Cardinality getCardinality(){
		return cardinality;
	}

	public boolean isStructure(){
		return isStructure;
	}

	public String getStructureName(){
		return structureName;
	}

	@Override
	public String toString(){
		return (isStructure? "<<" + structureName + ">>": name) + " " + cardinality;
	}

}
