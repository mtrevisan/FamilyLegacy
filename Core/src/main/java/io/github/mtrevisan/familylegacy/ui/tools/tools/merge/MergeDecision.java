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
package io.github.mtrevisan.familylegacy.ui.tools.tools.merge;


/**
 * What to do with a field that exists on both the source and the target
 * record and whose instances are not identical.
 */
public enum MergeDecision{

	/** The target's instances win; the source's are discarded. */
	KEEP_TARGET("Keep target"),

	/** The source's instances replace the target's. */
	KEEP_SOURCE("Keep source"),

	/**
	 * The target's instances are kept and the source's are added after
	 * them. Only meaningful for fields that can appear multiple times
	 * (notes, sources, names, events, ...). For single-valued fields it
	 * produces a schema-invalid record.
	 */
	KEEP_BOTH("Keep both");


	private final String label;


	MergeDecision(final String label){
		this.label = label;
	}


	public String label(){
		return label;
	}

}
