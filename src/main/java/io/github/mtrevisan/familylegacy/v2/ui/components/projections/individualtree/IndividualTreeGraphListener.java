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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipOperationCoordinator;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeMutator;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Implements the {@link IndividualListener} contract for the individual
 * tree.
 * <p>
 * The listener owns the interaction logic that was previously inlined in
 * the panel: it opens edit and search dialogs, mutates the model through
 * the {@link TreeMutator}, and coordinates the parent and child
 * relationship operations. The panel no longer implements the listener
 * directly; it creates one instance and hands it to the layout builder,
 * which installs it on every created panel.
 * <p>
 * The listener receives the panel component as a reference only for
 * dialog parenting and for the {@link SwingUtilities#getWindowAncestor}
 * call: it does not access the panel's internal state. Everything else
 * is provided through suppliers and consumers, so the listener stays
 * testable and the panel stays focused on composition.
 */
public final class IndividualTreeGraphListener implements IndividualListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualTreeGraphListener.class);


	private final FLEFModel model;
	private final TreeService treeService;
	private final TreeMutator treeMutator;
	private final IndividualDialogProvider dialogProvider;
	private final RelationshipOperationCoordinator operationCoordinator;

	private final Component component;
	private final Supplier<String> currentRootId;
	private final Supplier<Map<TreeNode, PartnersPanel>> nodeToPanelMapSupplier;
	private final Consumer<String> onSelectionChanged;


	public IndividualTreeGraphListener(final FLEFModel model, final TreeService treeService, final TreeMutator treeMutator,
			final IndividualDialogProvider dialogProvider, final RelationshipOperationCoordinator operationCoordinator,
			final Component component, final Supplier<String> currentRootId,
			final Supplier<Map<TreeNode, PartnersPanel>> nodeToPanelMapSupplier,
			final Consumer<String> onSelectionChanged){
		this.model = model;
		this.treeService = treeService;
		this.treeMutator = treeMutator;
		this.dialogProvider = dialogProvider;
		this.operationCoordinator = operationCoordinator;
		this.component = component;
		this.currentRootId = currentRootId;
		this.nodeToPanelMapSupplier = nodeToPanelMapSupplier;
		this.onSelectionChanged = onSelectionChanged;
	}


	/* ======================================================================
	 *                          Edit, remove, select
	 * ====================================================================== */

	@Override
	public void onEntityEdit(final FLEFRecord individual){
		if(individual == null)
			return;

		final Window parent = SwingUtilities.getWindowAncestor(component);
		final FLEFRecord edited = dialogProvider.showEditDialog(parent, individual);
		if(edited != null){
			LOGGER.debug("Individual edited: {}", edited.getId());

			treeMutator.invalidateAndNotifyTreeChanged(currentRootId.get());
		}
	}

	@Override
	public void onIndividualSelected(final IndividualPanel selectedPanel, final FLEFRecord individual){
		if(individual == null || individual.getId() == null)
			return;

		if(onSelectionChanged != null)
			onSelectionChanged.accept(individual.getId());
	}

	@Override
	public void onRootEntitySelected(final FLEFRecord individual){
		if(individual == null || individual.getId() == null)
			return;

		treeMutator.navigateToRoot(individual.getId());
	}

	@Override
	public void onEntityRemove(final FLEFRecord individual){
		if(individual == null)
			return;

		final String displayText = IndividualHandler.getInstance()
			.getDisplayText(individual, model);

		// Count the relationship records that the deletion will also remove.
		// getRelationshipIdsForIndividual returns the ids of every
		// relationship where the individual is either subject or target,
		// which is exactly the set removed by TreeMutator.removeIndividual.
		final int linkCount = (individual.getId() != null
			? treeService.getRelationshipIdsForIndividual(individual.getId()).size()
			: 0);

		final String message;
		if(linkCount == 0)
			message = "Are you sure you want to remove individual " + displayText + "?\n"
				+ "This individual has no relationship links.";
		else
			message = "Are you sure you want to remove individual " + displayText + "?\n"
				+ linkCount + (linkCount == 1
				? " relationship link will also be removed."
				: " relationship links will also be removed.");

		final int confirm = JOptionPane.showConfirmDialog(component,
			message,
			"Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			LOGGER.debug("Individual remove {}", individual.getId());

			treeMutator.removeIndividual(individual, currentRootId.get());
		}
	}


	/* ======================================================================
	 *                          Add and connect
	 * ====================================================================== */

	@Override
	public void onIndividualAddOrConnect(final IndividualPanel selectedPanel,
		final TreeOperation operation){
		final Function<SexType, FLEFRecord> fn = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog: this::showSearchIndividualDialog);

		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel,
			nodeToPanelMapSupplier.get());
		if(ctx == null || ctx.isChildContext() || !ctx.hasPartnerPanel())
			return;

		final SexType sex = (ctx.side == Side.LEFT? SexType.MALE: SexType.FEMALE);
		final FLEFRecord individual = fn.apply(sex);
		if(individual == null)
			return;

		performParentRelationOperation(individual, ctx, false, currentRootId.get());
	}

	@Override
	public void onChildAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel,
			nodeToPanelMapSupplier.get());
		if(ctx == null || ctx.isChildContext() || !ctx.hasPartnerPanel())
			return;

		final Function<SexType, FLEFRecord> fn = (operation == TreeOperation.ADD
			? this::showCreateIndividualDialog: this::showSearchIndividualDialog);
		final FLEFRecord child = fn.apply(null);
		if(child == null)
			return;

		final IndividualPanel fatherPanel = ctx.partnerPanel.getFatherPanel();
		final IndividualPanel motherPanel = ctx.partnerPanel.getMotherPanel();
		final IndividualData fatherData = (fatherPanel != null? fatherPanel.getData(): null);
		final IndividualData motherData = (motherPanel != null? motherPanel.getData(): null);
		final FLEFRecord targetFather = (fatherData != null? fatherData.getIndividual(): null);
		final FLEFRecord targetMother = (motherData != null? motherData.getIndividual(): null);
		if(targetFather == null && targetMother == null)
			return;

		performChildRelationOperation(child, targetFather, targetMother, false, currentRootId.get());
		onRootEntitySelected(targetFather != null? targetFather: targetMother);
	}


	/* ======================================================================
	 *                          Unlink, relocate, paste
	 * ====================================================================== */

	@Override
	public void onIndividualUnlink(final IndividualPanel selectedPanel, final FLEFRecord individual){
		if(individual == null || selectedPanel == null)
			return;

		final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(selectedPanel,
			nodeToPanelMapSupplier.get());
		if(ctx == null || !ctx.hasPartnerPanel())
			return;

		final IndividualPanel parentPanel = (ctx.side == Side.LEFT
			? ctx.partnerPanel.getFatherPanel(): ctx.partnerPanel.getMotherPanel());
		if(parentPanel == null)
			return;

		final FLEFRecord fatherRecord = parentPanel.getFather();
		final FLEFRecord motherRecord = parentPanel.getMother();
		final String targetFatherId = (fatherRecord != null? fatherRecord.getId(): null);
		final String targetMotherId = (motherRecord != null? motherRecord.getId(): null);

		final Window parent = SwingUtilities.getWindowAncestor(component);
		final Set<String> parentsIds = new LinkedHashSet<>();
		if(targetFatherId != null)
			parentsIds.add(targetFatherId);
		if(targetMotherId != null)
			parentsIds.add(targetMotherId);
		final Set<String> childrenIds = new LinkedHashSet<>(ctx.childrenId);
		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(parent, model,
			individual.getId(), parentsIds, Collections.emptySet(), Collections.emptySet(), childrenIds);
		dialog.setVisible(true);

		final List<String> selectedIds = dialog.getSelectedRelationshipIds();
		if(selectedIds.isEmpty())
			return;

		final int confirm = JOptionPane.showConfirmDialog(component,
			"Are you sure you want to remove the selected relationships?",
			"Confirm Unlink", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm == JOptionPane.YES_OPTION){
			treeMutator.removeRelationships(selectedIds);
			treeMutator.invalidateAndNotifyTreeChanged(currentRootId.get());
		}
	}

	@Override
	public void onEntityRelocate(final FLEFRecord individual){
		if(individual == null)
			return;

		LOGGER.debug("Relocate individual {} to clipboard", individual.getId());

		RelationClipboard.getInstance().setRecord(individual);
	}

	@Override
	public void onIndividualPaste(final IndividualPanel selectedPanel){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		if(!clipboard.hasRecord())
			return;

		final FLEFRecord source = clipboard.getRecord();
		final FLEFRecord father = selectedPanel.getFather();
		final FLEFRecord mother = selectedPanel.getMother();

		if(father != null || mother != null)
			performChildRelationOperation(source, father, mother, true, currentRootId.get());
		else{
			final TreeContextHelper.Context ctx = TreeContextHelper.determineContext(
				selectedPanel, nodeToPanelMapSupplier.get());
			if(ctx != null && ctx.isParentContext())
				performParentRelationOperation(source, ctx, true, currentRootId.get());
		}
		clipboard.clear();
	}


	/* ======================================================================
	 *                          Private helpers
	 * ====================================================================== */

	private void performChildRelationOperation(final FLEFRecord child, final FLEFRecord father, final FLEFRecord mother,
			final boolean isPaste, final String rootId){
		if(child == null)
			return;

		if(isPaste){
			final List<String> relationshipIds =
				treeService.getRelationshipIdsForIndividual(child.getId());
			treeMutator.removeRelationships(relationshipIds);
		}
		final Window parent = SwingUtilities.getWindowAncestor(component);
		operationCoordinator.performChildOperation(parent, child, father, mother);
		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	private void performParentRelationOperation(final FLEFRecord individual, final TreeContextHelper.Context ctx,
			final boolean isPaste, final String rootId){
		if(individual == null || ctx == null)
			return;

		if(isPaste){
			final List<String> relIds =
				treeService.getRelationshipIdsForIndividual(individual.getId());
			treeMutator.removeRelationships(relIds);
		}
		final Window parent = SwingUtilities.getWindowAncestor(component);
		operationCoordinator.performParentOperation(parent, individual, ctx);
		treeMutator.invalidateAndNotifyTreeChanged(rootId);
	}

	private FLEFRecord showCreateIndividualDialog(final SexType sex){
		return dialogProvider.showCreateDialog(
			SwingUtilities.getWindowAncestor(component), sex);
	}

	private FLEFRecord showSearchIndividualDialog(final SexType sex){
		return dialogProvider.showSearchDialog(
			SwingUtilities.getWindowAncestor(component), sex);
	}

}
