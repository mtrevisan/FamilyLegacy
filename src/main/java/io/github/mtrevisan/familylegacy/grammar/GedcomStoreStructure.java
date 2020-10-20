/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

//	@Override
//	public String toString(){
//		return GedcomStorePrinter.preparePrint(storeBlock, 1, false).toString();
//	}

}
