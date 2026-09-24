package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.help.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.AddChildTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.AddIndividualTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.DeleteIndividualTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.EditIndividualTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.PasteIndividualTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.RelocateIndividualTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.individuals.UnlinkRelationshipsTool;

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
		final JMenuItem connectItem = new JMenuItem("Connect Individual…");
		final JMenuItem addChildItem = new JMenuItem("Add Child…", 'C');
		final JMenuItem connectChildItem = new JMenuItem("Connect Child…");
		final JMenuItem relocateItem = new JMenuItem("Relocate Individual", 'R');
		relocateItem.setAccelerator(ShortcutRegistry.EDIT_RELOCATE.keyStroke());
		final JMenuItem pasteItem = new JMenuItem("Paste Individual", 'P');
		pasteItem.setAccelerator(ShortcutRegistry.EDIT_PASTE.keyStroke());
		final JMenuItem deleteItem = new JMenuItem("Delete Individual", 'D');
		deleteItem.setAccelerator(ShortcutRegistry.EDIT_DELETE.keyStroke());
		final JMenuItem unlinkItem = new JMenuItem("Unlink Relationships…", 'U');

		final JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				final IndividualData data = panel.getData();
				final boolean hasData = (data != null && !data.isEmpty());
				final boolean hasIndividuals = model.hasRecordsByType(IndividualHandler.TYPE);
				final boolean hasParents = (hasData && data.hasParents());
				final boolean hasPartner = (hasData && data.hasPartner());
				final boolean hasChildren = (hasData && data.hasChildren());
				final boolean hasRelations = (hasParents || hasPartner || hasChildren);

				final ToolContext context = new ToolContext(model, null, () -> panel, null);

				final boolean canPaste = (!hasData && new PasteIndividualTool().isEnabled(context));
				if(canPaste){
					final String clippedName = context.getClippedRecordDisplayText();
					pasteItem.setText("Paste " + clippedName + " Here");
					pasteItem.setEnabled(true);
				}
				else{
					pasteItem.setText("Paste Individual");
					pasteItem.setEnabled(false);
				}

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

		// Add menu items with their bound callbacks delegating directly to ToolOperations
		PopupMenuHelper.addMenuItem(popup, editItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new EditIndividualTool().run(context);
		});
		PopupMenuHelper.addMenuItem(popup, addItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new AddIndividualTool().run(context);
		});
		PopupMenuHelper.addMenuItem(popup, connectItem, panel, record -> listener.onIndividualAddOrConnect(panel, TreeOperation.CONNECT));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, addChildItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new AddChildTool().run(context);
		});
		PopupMenuHelper.addMenuItem(popup, connectChildItem, panel, record -> listener.onChildAddOrConnect(panel, TreeOperation.CONNECT));
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, relocateItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new RelocateIndividualTool().run(context);
		});
		PopupMenuHelper.addMenuItem(popup, pasteItem, panel, record -> {
			final ToolContext context = new ToolContext(model, null, () -> panel, null);
			new PasteIndividualTool().run(context);
		});
		PopupMenuHelper.addMenuItem(popup, deleteItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new DeleteIndividualTool().run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, unlinkItem, panel, record -> {
			final ToolContext context = new ToolContext(model, record::getId, () -> panel, null);
			new UnlinkRelationshipsTool().run(context);
		});

		return popup;
	}

}
