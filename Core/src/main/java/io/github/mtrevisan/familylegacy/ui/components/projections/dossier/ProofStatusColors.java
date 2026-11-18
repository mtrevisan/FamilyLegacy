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
package io.github.mtrevisan.familylegacy.ui.components.projections.dossier;

import io.github.mtrevisan.familylegacy.dossier.ProofStatus;

import java.awt.Color;


/**
 * The colors with which the dossier panels mark the proof status of an entry.
 */
final class ProofStatusColors{

	private static final Color PROVEN = new Color(80, 160, 80);
	private static final Color SUPPORTED = new Color(70, 120, 200);
	private static final Color CONFLICTING = new Color(200, 170, 60);
	private static final Color DISPROVEN = new Color(200, 80, 80);


	private ProofStatusColors(){}


	static Color of(final ProofStatus status){
		return switch(status){
			case PROVEN -> PROVEN;
			case SUPPORTED -> SUPPORTED;
			case CONFLICTING -> CONFLICTING;
			case DISPROVEN -> DISPROVEN;
		};
	}

}
