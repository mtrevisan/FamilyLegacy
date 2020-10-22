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
package io.github.mtrevisan.familylegacy.gedcom;

import java.util.List;


/**
 * A grammar structure has a structure name (like FAMILY_EVENT_STRUCTURE), a link to the grammar where it has been parsed and a
 * {@link GedcomGrammarBlock} which contains all the first level lines of the structure.<br>
 * <br>
 * Hierarchy:
 * <pre><code>
 * |---GrammarStructure--------------------------------|
 * |                                                   |
 * | A structure contains one block                    |
 * |                                                   |
 * | |---GrammarBlock-------------------------------|  |
 * | |                                              |  |
 * | |  A block contains multiple lines             |  |
 * | |                                              |  |
 * | |  |---GrammarLine-------------------------|   |  |
 * | |  |                                       |   |  |
 * | |  | A line may contain one sub block      |   |  |
 * | |  |                                       |   |  |
 * | |  | |---GrammarBlock-------------------|  |   |  |
 * | |  | | If a line has sub-lines (on a    |  |   |  |
 * | |  | | higher level), those lines are   |  |   |  |
 * | |  | | wrapped in a block.              |  |   |  |
 * | |  | |----------------------------------|  |   |  |
 * | |  |---------------------------------------|   |  |
 * | |                                              |  |
 * | |  |---GrammarLine-------------------------|   |  |
 * | |  | ...                                   |   |  |
 * | |  |---------------------------------------|   |  |
 * | |                                              |  |
 * | |----------------------------------------------|  |
 * |---------------------------------------------------|
 * </code></pre>
 */
final class GedcomGrammarStructure{

	/** The name of this structure, like FAMILY_EVENT_STRUCTURE etc. **/
	private final String structureName;
	/** The starting block in the structure. **/
	private final GedcomGrammarBlock grammarBlock = new GedcomGrammarBlock();


	public static GedcomGrammarStructure create(final String structureName, final List<String> block) throws GedcomGrammarParseException{
		return new GedcomGrammarStructure(structureName, block);
	}

	private GedcomGrammarStructure(final String structureName, final List<String> block) throws GedcomGrammarParseException{
		this.structureName = structureName;
		grammarBlock.parse(block);
	}

	/**
	 * @return	Starting grammar block of this structure.
	 */
	public GedcomGrammarBlock getGrammarBlock(){
		return grammarBlock;
	}

	/**
	 * @return	Name of this structure.
	 */
	public String getStructureName(){
		return structureName;
	}

}
