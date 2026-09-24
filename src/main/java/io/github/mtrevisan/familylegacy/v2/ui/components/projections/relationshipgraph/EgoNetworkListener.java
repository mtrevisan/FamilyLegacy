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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Handles interaction events and relationship operations for the Ego Network projection.
 */
public final class EgoNetworkListener implements IndividualListener, GroupListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(EgoNetworkListener.class);


	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child"
	};
	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES = new String[]{
		"civil_spouse", "religious_spouse", "customary_spouse", "cohabiting_partner", "engaged_partner", "associate"
	};
	private static final String[] INDIVIDUAL_TO_GROUP_TYPES = new String[]{"group_member", "associate"};
	private static final String[] GROUP_TO_GROUP_TYPES = new String[]{"part_of", "associate"};


	/**
	 * Provider interface to decouple EgoNetworkListener from direct panel state.
	 */
	public interface ContextProvider{
		String getCurrentEgoId();

		EgoNode getRootEgoNode();

		Set<EgoNode> getParents();

		Set<EgoNode> getAssociates();

		Set<FLEFRecord> getGroups();

		Set<EgoNode> getChildren();

	}

	public record SelectionCallbacks(
		Consumer<String> onIndividualSelected,
		Consumer<String> onGroupSelected
	){}

	private final Component parentComponent;
	private final FLEFModel model;
	private final EgoNetworkService networkService;
	private final EgoNetworkMutator networkMutator;
	private final EgoNetworkActionHandler actionHandler;
	private final ContextProvider contextProvider;
	private final SelectionCallbacks selectionCallbacks;


	public EgoNetworkListener(final Component parentComponent, final FLEFModel model,
			final EgoNetworkService networkService, final EgoNetworkMutator networkMutator,
			final EgoNetworkActionHandler actionHandler, final ContextProvider contextProvider,
			final SelectionCallbacks selectionCallbacks){
		this.parentComponent = parentComponent;
		this.model = model;
		this.networkService = networkService;
		this.networkMutator = networkMutator;
		this.actionHandler = actionHandler;
		this.contextProvider = contextProvider;
		this.selectionCallbacks = selectionCallbacks;
	}

	@Override
	public void onEntityEdit(final FLEFRecord record){
		if(record == null)
			return;

		final FLEFRecord editedRecord = actionHandler.showEditRecordDialog(record);
		if(editedRecord != null){
			LOGGER.debug("Entity edited: {}", editedRecord.getId());

			networkMutator.invalidateAndNotifyTreeChanged(contextProvider.getCurrentEgoId());
		}
	}

	@Override
	public void onIndividualSelected(final IndividualPanel selectedPanel, final FLEFRecord individual){
		if(individual != null && individual.getId() != null && selectionCallbacks != null
				&& selectionCallbacks.onIndividualSelected() != null)
			selectionCallbacks.onIndividualSelected().accept(individual.getId());
	}

	@Override
	public void onGroupSelected(final GroupPanel selectedPanel, final FLEFRecord group){
		if(group != null && group.getId() != null && selectionCallbacks != null
				&& selectionCallbacks.onGroupSelected() != null)
			selectionCallbacks.onGroupSelected().accept(group.getId());
	}

	@Override
	public void onRootEntitySelected(final FLEFRecord record){
		if(record != null && record.getId() != null)
			networkMutator.navigateToEgo(record.getId());
	}

	@Override
	public void onEntityRemove(final FLEFRecord record){
		if(record == null)
			return;

		final String name = GroupHandler.TYPE.equalsIgnoreCase(record.getTag())
			? GroupHandler.getInstance().getDisplayText(record, model)
			: IndividualHandler.getInstance().getDisplayText(record, model);

		final int confirm = JOptionPane.showConfirmDialog(
			parentComponent, "Are you sure you want to remove " + name + "?", "Confirm Removal",
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
		);
		if(confirm == JOptionPane.YES_OPTION)
			networkMutator.removeEntity(record, contextProvider.getCurrentEgoId());
	}

	@Override
	public void onIndividualAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(IndividualHandler.class)
				: () -> actionHandler.showSearchRecordDialog(IndividualHandler.class)),
			false, INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES, contextProvider.getCurrentEgoId(), contextProvider.getRootEgoNode());
	}

	@Override
	public void onChildAddOrConnect(final IndividualPanel selectedPanel, final TreeOperation operation){
		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(IndividualHandler.class)
				: () -> actionHandler.showSearchRecordDialog(IndividualHandler.class)),
			false, INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES, contextProvider.getCurrentEgoId(), contextProvider.getRootEgoNode());
	}

	@Override
	public void onGroupAddOrConnect(final TreeOperation operation){
		final EgoNode rootEgoNode = contextProvider.getRootEgoNode();
		final FLEFRecord egoRecord = (rootEgoNode != null ? rootEgoNode.getEgoRecord() : null);
		final String[] allowedTypes = (egoRecord != null && GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag())
			? GROUP_TO_GROUP_TYPES
			: INDIVIDUAL_TO_GROUP_TYPES);

		actionHandler.performRelationOperation(
			(operation == TreeOperation.ADD
				? () -> actionHandler.showCreateRecordDialog(GroupHandler.class)
				: () -> actionHandler.showSearchRecordDialog(GroupHandler.class)),
			false, allowedTypes, contextProvider.getCurrentEgoId(), rootEgoNode);
	}

	@Override
	public void onIndividualUnlink(final IndividualPanel selectedPanel, final FLEFRecord record){
		handleUnlink(record);
	}

	@Override
	public void onGroupUnlink(final GroupPanel selectedPanel, final FLEFRecord record){
		handleUnlink(record);
	}

	private void handleUnlink(final FLEFRecord record){
		if(record == null)
			return;

		final UnlinkRelationshipsDialog dialog = actionHandler.showUnlinkRelationshipsDialog(record, contextProvider.getCurrentEgoId(),
			networkService, contextProvider.getParents(), contextProvider.getAssociates(), contextProvider.getGroups(), contextProvider.getChildren());
		final List<String> toRemove = dialog.getSelectedRelationshipIds();
		if(!toRemove.isEmpty()){
			networkMutator.removeRelationships(toRemove);
			networkMutator.invalidateAndNotifyTreeChanged(contextProvider.getCurrentEgoId());
		}
	}

	@Override
	public void onEntityRelocate(final FLEFRecord record){
		if(record == null)
			return;

		LOGGER.debug("Relocate entity {} to clipboard", record.getId());

		RelationClipboard.getInstance().setRecord(record);
	}

	@Override
	public void onIndividualPaste(final IndividualPanel selectedPanel){
		onEntityPaste();
	}

	@Override
	public void onGroupPaste(){
		onEntityPaste();
	}

	public void onEntityPaste(){
		final RelationClipboard clipboard = RelationClipboard.getInstance();
		final EgoNode rootEgoNode = contextProvider.getRootEgoNode();
		if(!clipboard.hasRecord() || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		final FLEFRecord source = clipboard.getRecord();
		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());
		final boolean sourceIsGroup = GroupHandler.TYPE.equalsIgnoreCase(source.getTag());

		final String[] allowedTypes;
		if(egoIsGroup && sourceIsGroup)
			allowedTypes = GROUP_TO_GROUP_TYPES;
		else if(!egoIsGroup && !sourceIsGroup)
			allowedTypes = INDIVIDUAL_TO_INDIVIDUAL_SOCIAL_TYPES;
		else
			allowedTypes = INDIVIDUAL_TO_GROUP_TYPES;

		actionHandler.performRelationOperationOnRecord(source, true, allowedTypes, contextProvider.getCurrentEgoId(), rootEgoNode);
		clipboard.clear();
	}

}
