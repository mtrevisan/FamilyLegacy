package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * Abstract adapter class for receiving popup menu events.
 * The methods in this class are empty to allow overriding only required events.
 */
public abstract class PopupMenuAdapter implements PopupMenuListener{

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e){}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e){}

}