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

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Composes the chronomap overlay layers.
 * <p>
 * Layers are stored in insertion order and painted back-to-front. A
 * disabled layer is skipped. The manager itself is a
 * {@link Painter} so it can be installed directly on the
 * {@link JXMapViewer} as the overlay painter.
 */
public final class ChronomapLayerManager implements Painter<JXMapViewer>{

	private final List<ChronomapLayer> layers = new ArrayList<>();


	/** Appends a layer at the top of the stack. */
	public void addLayer(final ChronomapLayer layer){
		if(layer != null)
			layers.add(layer);
	}

	/** Removes a layer from the stack. */
	public void removeLayer(final ChronomapLayer layer){
		layers.remove(layer);
	}

	/** Returns an immutable snapshot of the layers, in painting order. */
	public List<ChronomapLayer> layers(){
		return List.copyOf(layers);
	}

	/** Enables or disables the given layer and repaints on the next cycle. */
	public void setVisible(final ChronomapLayer layer, final boolean visible){
		if(layer != null)
			layer.setVisible(visible);
	}


	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int w, final int h){
		for(final ChronomapLayer layer : layers)
			if(layer.isVisible())
				layer.paint(g, map, w, h);
	}

}
