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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;

import java.util.Set;


/** Reports the lines of a GEDCOM record that no converter consumed. */
public final class UnconsumedTags{

	private UnconsumedTags(){}


	/**
	 * Reports each child of a node that is not consumed.
	 *
	 * @param node	The node whose children are checked.
	 * @param consumedTags	The tags that the converter reads.
	 * @param path	The tag path of the node inside the record, empty for the record itself.
	 */
	public static void report(final GEDCOMNode node, final Set<String> consumedTags, final String path){
		for(final GEDCOMNode child : node.getChildren())
			reportChild(child, consumedTags, path);
	}

	/**
	 * Reports a line that is not consumed: its tag is not among the consumed, technical, or identifier ones, and it holds a
	 * value or a subtree.
	 */
	public static void reportChild(final GEDCOMNode child, final Set<String> consumedTags, final String parentPath){
		if(isUnmapped(child, consumedTags))
			GEDCOMImportContext.discard(ImportReasons.UNMAPPED_TAG, child, parentPath);
	}

	/**
	 * Whether a line is not consumed: its tag is not among the consumed, technical, or identifier ones, and it holds a value or a
	 * subtree.
	 */
	public static boolean isUnmapped(final GEDCOMNode child, final Set<String> consumedTags){
		final String tag = child.getTag();
		return (!consumedTags.contains(tag) && !GEDCOMMapper.TECHNICAL_TAGS.contains(tag) && !GEDCOMMapper.IDENTIFIER_TAGS.contains(tag)
			&& GEDCOMImportContext.isSignificant(child));
	}

}
