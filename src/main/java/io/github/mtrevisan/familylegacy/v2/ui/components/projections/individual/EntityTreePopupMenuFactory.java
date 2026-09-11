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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;


/**
 * Creates the popup menu for individual boxes used within the biological tree projection ({@code IndividualTreePanel}).
 */
public class EntityTreePopupMenuFactory implements EntityPopupMenuFactory<IndividualPanel, IndividualListener>{

	@Override
	public JPopupMenu createPopupMenu(final IndividualPanel panel, final IndividualListener listener,
			final FLEFModel model){
		final JMenuItem editItem = new JMenuItem("Edit Individual…", 'E');
		final JMenuItem addItem = new JMenuItem("Add Individual…", 'A');
		final JMenuItem connectItem = new JMenuItem("Connect Individual…", 'C');
		final JMenuItem addChildItem = new JMenuItem("Add Child…", 'C');
		final JMenuItem connectChildItem = new JMenuItem("Connect Child…", 'C');
		final JMenuItem relocateItem = new JMenuItem("Relocate Individual", 'R');
		final JMenuItem pasteItem = new JMenuItem("Paste Individual", 'P');
		final JMenuItem deleteItem = new JMenuItem("Delete Individual", 'D');
		final JMenuItem unlinkItem = new JMenuItem("Unlink Relationships…", 'U');

		// Evaluate item states dynamically right before displaying the menu
		final JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				if(listener != null)
					listener.onEntitySelected(panel);

				final IndividualData data = panel.getData();
				final boolean hasData = (data != null && !data.isEmpty());
				final boolean hasIndividuals = model.hasRecordsByType(IndividualHandler.TYPE);
				final boolean hasParents = (hasData && data.hasParents());
				final boolean hasPartner = (hasData && data.hasPartner());
				final boolean hasChildren = (hasData && data.hasChildren());
				final boolean hasRelations = (hasParents || hasPartner || hasChildren);

				// Update paste item state and title based on clipboard content
				final boolean canPaste = (!hasData && PopupMenuHelper.isPasteAllowed(panel));
				if(canPaste){
					final FLEFRecord clippedRecord = RelationClipboard.getInstance().getRecord();
					final String clippedName = IndividualHandler.getInstance().getDisplayText(clippedRecord, model);
					pasteItem.setText("Paste " + clippedName + " Here");
					pasteItem.setEnabled(true);
				}
				else
					pasteItem.setEnabled(false);

				// Enable/disable menu options depending on entity existence and capabilities
				editItem.setEnabled(hasData);
				addItem.setEnabled(!hasData);
				connectItem.setEnabled(!hasData && hasIndividuals);
				addChildItem.setEnabled(hasData && panel.isEnableAddChildMenu());
				connectChildItem.setEnabled(hasData && hasIndividuals && panel.isEnableAddChildMenu());
				relocateItem.setEnabled(hasData);
				deleteItem.setEnabled(hasData);
				unlinkItem.setEnabled(hasRelations);
			}
		});

		// Add menu items with their bound callbacks
		PopupMenuHelper.addMenuItem(popup, editItem, panel, record -> listener.onEntityEdit(record));
		PopupMenuHelper.addMenuItem(popup, addItem, panel, record -> listener.onIndividualAddOrConnect(TreeOperation.ADD));
		PopupMenuHelper.addMenuItem(popup, connectItem, panel, record -> listener.onIndividualAddOrConnect(TreeOperation.CONNECT));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, addChildItem, panel, record -> listener.onChildAddOrConnect(TreeOperation.ADD));
		PopupMenuHelper.addMenuItem(popup, connectChildItem, panel, record -> listener.onChildAddOrConnect(TreeOperation.CONNECT));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, relocateItem, panel, record -> listener.onEntityRelocate(record));
		PopupMenuHelper.addMenuItem(popup, pasteItem, panel, record -> listener.onIndividualPaste(panel.getFather(), panel.getMother()));
		PopupMenuHelper.addMenuItem(popup, deleteItem, panel, record -> listener.onEntityRemove(record));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, unlinkItem, panel, record -> listener.onEntityUnlink(record));

		return popup;
	}

}
