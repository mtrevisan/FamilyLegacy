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
package io.github.mtrevisan.familylegacy.ui.components.projections.menus;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.individual.IndividualSummary;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.tree.EntityListener;
import io.github.mtrevisan.familylegacy.tree.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.PopupMenuHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDispatcher;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.AddChildTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.AddIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.ConnectChildTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.ConnectIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.ConsanguinityLoopsTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.DeleteIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.EditIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.IndividualCareerTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.PasteIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.RelocateIndividualTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals.UnlinkIndividualRelationshipsTool;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance.ProvenanceTool;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.EntityPopupMenuFactory;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;


/**
 * Creates the popup menu for individual boxes used within the biological tree projection ({@code IndividualTreePanel}).
 */
public class EntityTreePopupMenuFactory implements EntityPopupMenuFactory<IndividualPanel, IndividualListener>{

	@Override
	public JPopupMenu createPopupMenu(final IndividualPanel panel, final IndividualListener listener,
			final FLEFModel model){
		final JMenuItem editItem = new JMenuItem(I18N.t("menu.individual.edit.individual"), 'E');
		editItem.setAccelerator(ShortcutRegistry.EDIT_SELECTION_INDIVIDUAL.keyStroke());
		final JMenuItem addItem = new JMenuItem(I18N.t("menu.individual.add.individual"), 'A');
		final JMenuItem connectItem = new JMenuItem(I18N.t("menu.individual.connect.individual"));
		final JMenuItem addChildItem = new JMenuItem(I18N.t("menu.individual.add.child"), 'C');
		final JMenuItem connectChildItem = new JMenuItem(I18N.t("menu.individual.connect.child"));
		final JMenuItem relocateItem = new JMenuItem(I18N.t("menu.individual.relocate"), 'R');
		relocateItem.setAccelerator(ShortcutRegistry.EDIT_RELOCATE_INDIVIDUAL.keyStroke());
		final JMenuItem pasteItem = new JMenuItem(I18N.t("menu.individual.paste"), 'P');
		final JMenuItem deleteItem = new JMenuItem(I18N.t("menu.individual.delete"), 'D');
		deleteItem.setAccelerator(ShortcutRegistry.EDIT_DELETE_INDIVIDUAL.keyStroke());
		final JMenuItem unlinkItem = new JMenuItem(I18N.t("menu.individual.unlink.relationships"), 'U');
		final JMenuItem careerItem = new JMenuItem(I18N.t("menu.individual.group_career"));
		final JMenuItem provenanceItem = new JMenuItem(I18N.t("menu.source_citation.provenance"));
		final JMenuItem loopsItem = new JMenuItem(I18N.t("menu.individual.consanguinity_loops"));

		final JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				//the provenance (citation by citation, with stance and evidence) is offered from the standard level, as in the menu bar
				provenanceItem.setVisible(FeatureVisibilityService.shared().isVisible(new ProvenanceTool().minimumLevel()));
				final IndividualSummary data = panel.getData();
				final boolean hasData = (data != null && !data.isEmpty());
				final boolean hasIndividuals = model.hasRecordsByType(IndividualHandler.TYPE);
				final boolean hasParents = (hasData && data.hasParents());
				final boolean hasPartner = (hasData && data.hasPartner());
				final boolean hasChildren = (hasData && data.hasChildren());
				final boolean hasRelations = (hasParents || hasPartner || hasChildren);

				final ToolContext context = ToolContexts.withMutator(model, listener, panel, null);

				final boolean canPaste = (!hasData && PopupMenuHelper.isPasteAllowed(model, panel) && context.canPaste());
				pasteItem.setEnabled(canPaste);
				if(canPaste){
					final String clippedName = context.getClippedRecordDisplayText();
					pasteItem.setText(I18N.tf("menu.individual.paste.here", clippedName));
				}
				else
					pasteItem.setText(I18N.tf("menu.individual.paste"));

				editItem.setEnabled(hasData);
				addItem.setEnabled(!hasData);
				connectItem.setEnabled(!hasData && hasIndividuals);
				addChildItem.setEnabled(hasData && panel.isEnableAddChildMenu());
				connectChildItem.setEnabled(hasData && hasIndividuals && panel.isEnableAddChildMenu());
				relocateItem.setEnabled(hasData);
				deleteItem.setEnabled(hasData);
				//the same condition of the Individual menu: anything that refers to the individual, not only what the box shows
				unlinkItem.setEnabled(hasData && (hasRelations || UnlinkIndividualRelationshipsTool.hasLinks(model, data.getId())));
				careerItem.setEnabled(hasData);
			}
		});

		// Add menu items with their bound callbacks delegating directly to ToolOperations
		PopupMenuHelper.addMenuItem(popup, editItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new EditIndividualTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, addItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new AddIndividualTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, connectItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, (record != null? record.getId(): null));
			new ConnectIndividualTool()
				.run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, addChildItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new AddChildTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, connectChildItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, (record != null? record.getId(): null));
			new ConnectChildTool()
				.run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, relocateItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new RelocateIndividualTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, pasteItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, null);
			new PasteIndividualTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, deleteItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new DeleteIndividualTool()
				.run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, unlinkItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new UnlinkIndividualRelationshipsTool()
				.run(context);
		});
		popup.addSeparator();
		PopupMenuHelper.addMenuItem(popup, careerItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new IndividualCareerTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, provenanceItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new ProvenanceTool()
				.run(context);
		});
		PopupMenuHelper.addMenuItem(popup, loopsItem, panel, record -> {
			final ToolContext context = ToolContexts.withMutator(model, listener, panel, record.getId());
			new ConsanguinityLoopsTool()
				.run(context);
		});

		return popup;
	}

	/**
	 * Builds a {@link ToolContext} whose dispatcher exposes the projection
	 * mutator obtained from the listener. Tools that mutate the
	 * relationship graph ({@code UnlinkRelationshipsTool},
	 * {@code DeleteIndividualTool}, {@code PasteIndividualTool}, ...) rely
	 * on this dispatcher to invalidate the shared repository and tree
	 * caches after the mutation; without it, they fall back to a direct
	 * model mutation that leaves the caches stale and the tree drawing
	 * outdated connections.
	 */
	static ToolContext toolContext(final FLEFModel model, final EntityListener listener, final JPanel panel,
			final String entityId){
		return new ToolContext(model, entityId, panel, new ToolDispatcher(){
			@Override
			public ProjectionMutator getMutator(){
				return listener.getMutator();
			}
		});
	}

}
