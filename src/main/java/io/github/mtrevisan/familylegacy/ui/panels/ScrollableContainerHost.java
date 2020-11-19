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
package io.github.mtrevisan.familylegacy.ui.panels;

import javax.swing.*;
import java.awt.*;


public class ScrollableContainerHost extends JPanel implements Scrollable{

	public ScrollableContainerHost(final JPanel panel){
		super(new BorderLayout());

		add(panel);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize(){
		final Dimension preferredSize = getPreferredSize();
		if(getParent() instanceof JViewport)
			preferredSize.width += ((JScrollPane)getParent().getParent()).getHorizontalScrollBar().getPreferredSize().width;
		return preferredSize;
	}

	@Override
	public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction){
		return 64;
	}

	@Override
	public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction){
		return 128;
	}

	@Override
	public boolean getScrollableTracksViewportWidth(){
		return (getParent() instanceof JViewport && getPreferredSize().height < getParent().getHeight());
	}

	@Override
	public boolean getScrollableTracksViewportHeight(){
		return true;
	}

}
