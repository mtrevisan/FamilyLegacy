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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;


/**
 * Creates the popup menu tailored for an {@link GroupPanel} within an {@code EgoNetworkPanel} projection.
 */
public class EgoNetworkGroupPopupMenuFactory implements EntityPopupMenuFactory<GroupPanel, GroupListener>{


	@Override
	public JPopupMenu createPopupMenu(final GroupPanel panel, final GroupListener listener,
			final FLEFModel model){
		final JMenuItem editItem = new JMenuItem("Edit Group…", 'E');
		final JMenuItem deleteItem = new JMenuItem("Delete Group", 'D');
		final JMenuItem unlinkItem = new JMenuItem("Unlink Relationship", 'U');

		// Evaluate item states right before display
		final JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				if(listener != null)
					listener.onEntitySelected(panel);

				final GroupData data = panel.getData();
				final boolean hasData = (data != null && !data.isEmpty());

				// Update paste item state and title based on clipboard content
				final boolean canPaste = (!hasData && PopupMenuHelper.isPasteAllowed(panel));
				if(canPaste){
					final FLEFRecord clippedRecord = RelationClipboard.getInstance()
						.getRecord();
					final String clippedName = GroupHandler.getInstance()
						.getDisplayText(clippedRecord, model);
				}

				// Enable/disable menu options depending on entity existence and capabilities
				editItem.setEnabled(hasData);
				deleteItem.setEnabled(hasData);
			}
		});

		// Add menu items with their bound callbacks
		PopupMenuHelper.addMenuItem(popup, editItem, panel, record -> listener.onEntityEdit(record));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, deleteItem, panel, record -> listener.onEntityRemove(record));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, unlinkItem, panel, record -> listener.onEntityUnlink(record));

		return popup;
	}

}
