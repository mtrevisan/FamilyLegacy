/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.grammar;

import io.github.mtrevisan.familylegacy.grammar.exceptions.GedcomGrammarParseException;

import java.util.List;


/**
 * A store structure has a structure name (like FAMILY_EVENT_STRUCTURE), a link to the store where it has been parsed and a
 * {@link GedcomStoreBlock} which contains all the first level lines of the structure.<br>
 * <br>
 * Hierarchy:
 * <pre><code>
 * |---StoreStructure----------------------------------|
 * |                                                   |
 * | A structure contains one block                    |
 * |                                                   |
 * | |---StoreBlock---------------------------------|  |
 * | |                                              |  |
 * | |  A block contains multiple lines             |  |
 * | |                                              |  |
 * | |  |---StoreLine---------------------------|   |  |
 * | |  |                                       |   |  |
 * | |  | A line may contain one sub block      |   |  |
 * | |  |                                       |   |  |
 * | |  | |---StoreBlock---------------------|  |   |  |
 * | |  | | If a line has sub-lines (on a    |  |   |  |
 * | |  | | higher level), those lines are   |  |   |  |
 * | |  | | wrapped in a block.              |  |   |  |
 * | |  | |----------------------------------|  |   |  |
 * | |  |---------------------------------------|   |  |
 * | |                                              |  |
 * | |  |---StoreLine---------------------------|   |  |
 * | |  | ...                                   |   |  |
 * | |  |---------------------------------------|   |  |
 * | |                                              |  |
 * | |----------------------------------------------|  |
 * |---------------------------------------------------|
 * </code></pre>
 */
final class GedcomStoreStructure{

	/** The name of this structure, like FAMILY_EVENT_STRUCTURE etc. **/
	private final String structureName;
	/** The starting block in the structure. **/
	private final GedcomStoreBlock storeBlock = new GedcomStoreBlock();


	public static GedcomStoreStructure create(final String structureName, final List<String> block) throws GedcomGrammarParseException{
		return new GedcomStoreStructure(structureName, block);
	}

	private GedcomStoreStructure(final String structureName, final List<String> block) throws GedcomGrammarParseException{
		this.structureName = structureName;
		storeBlock.parse(block);
	}

	/**
	 * @return	Starting store block of this structure.
	 */
	public GedcomStoreBlock getStoreBlock(){
		return storeBlock;
	}

	/**
	 * @return	Name of this structure.
	 */
	public String getStructureName(){
		return structureName;
	}

}
