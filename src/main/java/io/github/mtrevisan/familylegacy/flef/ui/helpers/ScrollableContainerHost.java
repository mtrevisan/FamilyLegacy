/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;


public final class ScrollableContainerHost extends JPanel implements Scrollable{

	public enum ScrollType{HORIZONTAL, VERTICAL, BOTH}


	private final ScrollType scrollType;


	public ScrollableContainerHost(final Component component, final ScrollType scrollType){
		super(new MigLayout("insets 0", "[grow]", "[grow]"));

		setOpaque(false);

		this.scrollType = scrollType;

		final JPanel intermediatePanel = new JPanel(new MigLayout("insets 0", "[grow]", "[grow]"));
		intermediatePanel.add(component, "grow");
		intermediatePanel.setOpaque(false);
		add(intermediatePanel, "grow");
	}


	@Override
	public Dimension getPreferredScrollableViewportSize(){
		final Dimension preferredSize = getPreferredSize();
		if(getParent() instanceof JViewport){
			if(scrollType != ScrollType.VERTICAL)
				preferredSize.height += ((JScrollPane)getParent().getParent()).getHorizontalScrollBar().getPreferredSize().height;
			if(scrollType != ScrollType.HORIZONTAL)
				preferredSize.width += ((JScrollPane)getParent().getParent()).getVerticalScrollBar().getPreferredSize().width;
		}
		return preferredSize;
	}

	@Override
	public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction){
		return 32;
	}

	@Override
	public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction){
		return 32;
	}

	@Override
	public boolean getScrollableTracksViewportWidth(){
		return (getParent() instanceof JViewport && getPreferredSize().width < getParent().getWidth());
	}

	@Override
	public boolean getScrollableTracksViewportHeight(){
		return (getParent() instanceof JViewport && getPreferredSize().height < getParent().getHeight());
	}

}
