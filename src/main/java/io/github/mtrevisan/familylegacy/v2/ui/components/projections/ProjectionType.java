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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;


/**
 * The three projection views hosted by the {@link ProjectionSwitcherPanel}.
 * <p>
 * The order of the constants matches the order in which the user cycles
 * through the views with the switch shortcut. The enum is used by the
 * navigation history and by the bookmark system to identify a view
 * without holding a reference to the panel.
 */
public enum ProjectionType{

	/** Ancestor tree. */
	TREE("Tree"),

	/** Sugiyama pedigree graph. */
	GRAPH("Sugiyama graph"),

	/** Ego-centric network. */
	EGO_NETWORK("Ego network");


	private final String displayName;


	ProjectionType(final String displayName){
		this.displayName = displayName;
	}


	/** Human-readable name, used in bookmark labels. */
	public String displayName(){
		return displayName;
	}

}
