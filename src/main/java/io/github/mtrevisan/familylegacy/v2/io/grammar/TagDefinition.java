/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.io.grammar;

import java.util.Objects;


/**
 * Definition of a single tag within a record or structure.
 * It may be a simple tag (with a value) or a reference to a nested structure.
 */
public final class TagDefinition{

	private final String name;
	private final Cardinality cardinality;

	private String structureName;


	/**
	 * Creates a simple tag definition (not a structure).
	 */
	public static TagDefinition simple(final String name, final Cardinality cardinality){
		return new TagDefinition(name, cardinality);
	}

	/**
	 * Creates a structure tag definition (points to a sub-structure).
	 *
	 * @param name	The tag name (e.g., "NAME", "EVENT")
	 * @param cardinality	The cardinality
	 * @param structureName	The name of the structure definition (e.g., "PERSONAL_NAME_STRUCTURE")
	 */
	public static TagDefinition structure(final String name, final Cardinality cardinality, final String structureName){
		final TagDefinition tagDefinition = new TagDefinition(name, cardinality);
		tagDefinition.structureName = structureName;
		return tagDefinition;
	}


	private TagDefinition(final String name, final Cardinality cardinality){
		this.name = Objects.requireNonNull(name);
		this.cardinality = Objects.requireNonNull(cardinality);
	}


	public String getName(){
		return name;
	}

	public Cardinality getCardinality(){
		return cardinality;
	}

	public boolean isStructure(){
		return (structureName != null);
	}

	public String getStructureName(){
		return structureName;
	}

	@Override
	public String toString(){
		return (isStructure()? "<<" + structureName + ">>": name) + " " + cardinality;
	}

}
