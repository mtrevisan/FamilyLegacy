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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.pedigree;


/**
 * One path from the root individual of the ancestor tree to an occurrence
 * of an ancestor.
 * <p>
 * The path is encoded as a string of {@code F} (father) and {@code M}
 * (mother) letters, read from the root downward. For example:
 * <ul>
 *   <li>{@code "F"} — the father of the root;</li>
 *   <li>{@code "M"} — the mother of the root;</li>
 *   <li>{@code "FM"} — the mother of the father of the root;</li>
 *   <li>{@code "MMF"} — the father of the mother of the mother of the
 *       root.</li>
 * </ul>
 * The {@code generation} field is the length of the code, i.e. the number
 * of ancestor steps between the root and the individual.
 */
public record PedigreePath(String code, int generation){

	/**
	 * Compact constructor with validation.
	 */
	public PedigreePath{
		if(code == null)
			throw new IllegalArgumentException("Code must not be null");
		if(generation < 0)
			throw new IllegalArgumentException("Generation must not be negative");
	}


	/**
	 * Returns a human-readable rendering of the path, using the Italian
	 * genealogical terminology.
	 *
	 * @return a readable string such as {@code "father > mother"}
	 */
	public String describe(){
		if(code.isEmpty())
			return "root";

		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < code.length(); i ++){
			if(i > 0)
				sb.append(" > ");
			sb.append(code.charAt(i) == 'F'? "father": "mother");
		}
		return sb.toString();
	}


	@Override
	public String toString(){
		return code + " (" + describe() + ")";
	}

}
