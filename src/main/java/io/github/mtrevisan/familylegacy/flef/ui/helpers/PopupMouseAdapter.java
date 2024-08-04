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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class PopupMouseAdapter extends MouseAdapter{

	private final JPopupMenu popupMenu;
	private final JComponent component;


	public PopupMouseAdapter(final JPopupMenu popupMenu, final JComponent component){
		this.popupMenu = popupMenu;
		this.component = component;
	}


	@Override
	public void mouseClicked(final MouseEvent event){
		processMouseEvent(event);
	}

	@Override
	public void mouseReleased(final MouseEvent event){
		processMouseEvent(event);
	}

	private void processMouseEvent(final MouseEvent event){
		if(component.isEnabled() && event.isPopupTrigger() && hasPopupMenuVisibleItems()){
			popupMenu.show(event.getComponent(), event.getX(), event.getY());
			popupMenu.setInvoker(component);
		}
	}

	private boolean hasPopupMenuVisibleItems(){
		Component[] components = popupMenu.getComponents();
		for(int i = 0, length = components.length; i < length; i ++)
			if(components[i].isVisible())
				return true;
		return false;
	}

}
