package io.github.mtrevisan.familylegacy.ui.panels;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


class PopupMouseAdapter extends MouseAdapter{

	private final JPopupMenu popupMenu;
	private final JComponent component;


	PopupMouseAdapter(final JPopupMenu popupMenu, final JComponent component){
		this.popupMenu = popupMenu;
		this.component = component;
	}

	@Override
	public void mouseClicked(final MouseEvent e){
		processMouseEvent(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e){
		processMouseEvent(e);
	}

	private void processMouseEvent(final MouseEvent e){
		if(e.isPopupTrigger()){
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
			popupMenu.setInvoker(component);
		}
	}

}
