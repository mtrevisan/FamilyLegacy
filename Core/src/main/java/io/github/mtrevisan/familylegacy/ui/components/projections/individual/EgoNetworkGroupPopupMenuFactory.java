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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupData;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.tools.groups.AddMemberTool;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;


/**
 * Creates the popup menu tailored for an {@link GroupPanel} within an {@code EgoNetworkPanel} projection.
 */
public class EgoNetworkGroupPopupMenuFactory implements EntityPopupMenuFactory<GroupPanel, GroupListener>{

	@Override
	public JPopupMenu createPopupMenu(final GroupPanel panel, final GroupListener listener, final FLEFModel model){
		final JMenuItem editItem = new JMenuItem(I18N.t("menu.group.edit.group"), 'E');
		editItem.setAccelerator(ShortcutRegistry.EDIT_SELECTION_GROUP.keyStroke());
		final JMenuItem addMemberItem = new JMenuItem(I18N.t("menu.group.add.members"), 'A');
		final JMenuItem deleteItem = new JMenuItem(I18N.t("menu.group.delete"), 'D');
		final JMenuItem pasteItem = new JMenuItem(I18N.t("menu.group.paste"), 'P');
		final JMenuItem unlinkItem = new JMenuItem(I18N.t("menu.group.unlink.relationships"), 'U');

		final JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				final GroupData data = panel.getData();
				final boolean hasData = (data != null && !data.isEmpty());

				final ToolContext context = EntityTreePopupMenuFactory.toolContext(model, listener, panel, null);

				final boolean canPaste = (!hasData && PopupMenuHelper.isPasteAllowed(model, panel) && context.canPaste());
				pasteItem.setEnabled(canPaste);
				if(canPaste){
					final String clippedName = context.getClippedRecordDisplayText();
					pasteItem.setText(I18N.tf("menu.group.paste.here", clippedName));
				}
				else
					pasteItem.setText(I18N.tf("menu.group.paste"));

				editItem.setEnabled(hasData);
				addMemberItem.setEnabled(hasData);
				deleteItem.setEnabled(hasData);
				unlinkItem.setEnabled(hasData);
			}
		});

		PopupMenuHelper.addMenuItem(popup, editItem, panel, listener::onEntityEdit);
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, addMemberItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new AddMemberTool()
				.run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, deleteItem, panel, listener::onEntityRemove);
		popup.addSeparator();
		popup.add(pasteItem);
		pasteItem.addActionListener(e -> listener.onGroupPaste(panel));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, unlinkItem, panel, record -> listener.onGroupUnlink(panel, record));

		return popup;
	}

}
