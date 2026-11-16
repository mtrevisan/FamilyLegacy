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
package io.github.mtrevisan.familylegacy.io.grammar.typedefinitions;


/**
 * The path of tags of a node that is being validated (e.g. {@code records.individual.name}), written only when it is needed.
 * <p>
 * The path is for the messages of the errors, and most nodes have none: building a string for every node of a model costs more than
 * the validation itself, so each node keeps its parent and its own segment, and the text is built (once) when an error asks for it.
 */
public final class ValidationPath{

	private ValidationPath parent;
	private final String segment;
	private String text;


	private ValidationPath(final ValidationPath parent, final String segment, final String text){
		this.parent = parent;
		this.segment = segment;
		this.text = text;
	}

	/**
	 * @param path the whole path (empty for the root)
	 * @return a path that is already text
	 */
	public static ValidationPath of(final String path){
		return new ValidationPath(null, null, path);
	}


	/**
	 * @param tag the tag of a field of the node
	 * @return the path of the field
	 */
	public ValidationPath child(final String tag){
		return new ValidationPath(this, tag, null);
	}

	/** @return the path as text: the tags separated by dots (no dot for an empty parent) */
	@Override
	public String toString(){
		if(text == null){
			final String parentText = parent.toString();
			text = (parentText.isEmpty()? segment: parentText + '.' + segment);
			//the parent is not needed anymore
			parent = null;
		}
		return text;
	}

}
