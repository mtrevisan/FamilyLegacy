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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;


/**
 * A single overlay layer of the chronomap.
 * <p>
 * Every layer is a {@link Painter} for the underlying {@link JXMapViewer},
 * plus a name and a visibility flag. The layers are composed by a
 * {@link ChronomapLayerManager}, which paints them in insertion order.
 */
public interface ChronomapLayer extends Painter<JXMapViewer>{

	/** Human-readable name, used in the layer control menu. */
	String getName();

	/** Returns {@code true} when the layer is currently painted. */
	boolean isVisible();

	/** Enables or disables the layer. */
	void setVisible(boolean visible);

}
