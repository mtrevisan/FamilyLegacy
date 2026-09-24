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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.EgoNetworkMutator;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Encapsulates record dialog invocations and relationship creation workflows
 * for the ego network component.
 */
public class EgoNetworkActionHandler{

	private static final String[] INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES = new String[]{
		"biological_child", "adoptive_child", "foster_child", "guarded_child", "step_child"
	};

	private final Component parentComponent;
	private final FLEFModel model;
	private final EgoNetworkMutator networkMutator;


	public EgoNetworkActionHandler(final Component parentComponent, final FLEFModel model,
			final EgoNetworkMutator networkMutator){
		this.parentComponent = parentComponent;
		this.model = model;
		this.networkMutator = networkMutator;
	}

	public void performRelationOperation(final Supplier<FLEFRecord> recordSupplier, final boolean isPaste,
			final String[] allowedTypes, final String currentEgoId, final EgoNode rootEgoNode,
			final IndividualListener listener){
		final FLEFRecord targetRecord = recordSupplier.get();
		if(targetRecord == null)
			return;

		performRelationOperationOnRecord(targetRecord, isPaste, allowedTypes, currentEgoId, rootEgoNode, listener);
	}

	public void performRelationOperationOnRecord(final FLEFRecord targetRecord, final boolean isPaste,
			final String[] allowedTypes, final String currentEgoId, final EgoNode rootEgoNode,
			final IndividualListener listener){
		if(currentEgoId == null || rootEgoNode == null)
			return;

		final FLEFRecord egoRecord = rootEgoNode.getEgoRecord();
		if(egoRecord == null)
			return;

		if(isPaste)
			networkMutator.unlinkRelationship(egoRecord, targetRecord, currentEgoId);

		final Window parent = SwingUtilities.getWindowAncestor(parentComponent);
		final String label = (GroupHandler.TYPE.equalsIgnoreCase(targetRecord.getTag())
			? GroupHandler.getInstance().getDisplayText(targetRecord, model)
			: IndividualHandler.getInstance().getDisplayText(targetRecord, model));

		final List<RelationshipTypeSelectionDialog.Item> items = List.of(
			new RelationshipTypeSelectionDialog.Item(targetRecord.getId(), label, allowedTypes[0])
		);

		final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(parent, items,
			allowedTypes, listener, model);
		if(selectedTypes == null || selectedTypes.isEmpty())
			return;

		final String selectedType = selectedTypes.getFirst();
		final String[] pair = resolveSubjectTarget(selectedType, egoRecord, targetRecord);
		networkMutator.createRelationship(pair[0], pair[1], selectedType);
		networkMutator.invalidateAndNotifyTreeChanged(currentEgoId);
	}


	private String[] resolveSubjectTarget(final String type, final FLEFRecord egoRecord, final FLEFRecord otherRecord){
		final String egoId = egoRecord.getId();
		final String otherId = otherRecord.getId();
		final boolean egoIsGroup = GroupHandler.TYPE.equalsIgnoreCase(egoRecord.getTag());

		if(isChildType(type))
			return new String[]{otherId, egoId};

		if("group_member".equals(type)){
			return (egoIsGroup
				? new String[]{otherId, egoId}
				: new String[]{egoId, otherId});
		}

		return new String[]{egoId, otherId};
	}

	public FLEFRecord showEditRecordDialog(final FLEFRecord record){
		final Window parent = SwingUtilities.getWindowAncestor(parentComponent);
		final RecordTypeHandler<?> handler = (IndividualHandler.TYPE.equalsIgnoreCase(record.getTag())
			? IndividualHandler.getInstance()
			: GroupHandler.getInstance());
		final BaseRecordDialog dialog = handler.createEditDialog(parent, model, record);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public FLEFRecord showCreateRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final Window parent = SwingUtilities.getWindowAncestor(parentComponent);
		final RecordTypeHandler<?> handler = resolveHandler(handlerClass);

		final BaseRecordDialog dialog = handler.createNewDialog(parent, model);
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	public FLEFRecord showSearchRecordDialog(final Class<? extends RecordTypeHandler<?>> handlerClass){
		final FLEFRecord[] result = {null};
		final Window parent = SwingUtilities.getWindowAncestor(parentComponent);

		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			parent, model,
			(record, handler) -> result[0] = record,
			handlerClass);
		dialog.setVisible(true);

		return result[0];
	}

	public UnlinkRelationshipsDialog showUnlinkRelationshipsDialog(final FLEFRecord record, final String currentEgoId,
			final EgoNetworkService networkService, final Set<EgoNode> parents, final Set<EgoNode> associates,
			final Set<FLEFRecord> groups, final Set<EgoNode> children){
		final Set<EgoNode> relParents;
		final Set<EgoNode> relAssociates;
		final Set<FLEFRecord> relGroups;
		final Set<EgoNode> relChildren;

		if(Objects.equals(record.getId(), currentEgoId)){
			relParents = parents;
			relAssociates = associates;
			relGroups = groups;
			relChildren = children;
		}
		else{
			final EgoNode node = networkService.buildEgoNetwork(record.getId());
			relParents = node.getRelatedNodes(EgoNode.RelationshipCategory.PARENT);
			relAssociates = node.getRelatedNodes(EgoNode.RelationshipCategory.ASSOCIATE);
			relGroups = node.getGroupRecords();
			relChildren = node.getRelatedNodes(EgoNode.RelationshipCategory.CHILD);
		}
		final Window parent = SwingUtilities.getWindowAncestor(parentComponent);
		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(parent, model, record.getId(),
			extractNodeIds(relParents), extractNodeIds(relAssociates), extractRecordIds(relGroups),
			extractNodeIds(relChildren));
		dialog.setVisible(true);

		return dialog;
	}

	private static Set<String> extractNodeIds(final Set<EgoNode> nodes){
		return nodes.stream().map(EgoNode::getEgoId).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private static Set<String> extractRecordIds(final Set<FLEFRecord> records){
		return records.stream().map(FLEFRecord::getId).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private static boolean isChildType(final String type){
		return ArrayUtils.contains(INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES, type.toLowerCase(Locale.ROOT));
	}

	private static RecordTypeHandler<?> resolveHandler(final Class<? extends RecordTypeHandler<?>> handlerClass){
		if(GroupHandler.class.equals(handlerClass))
			return GroupHandler.getInstance();
		if(IndividualHandler.class.equals(handlerClass))
			return IndividualHandler.getInstance();

		throw new IllegalArgumentException("Unsupported handler class: " + handlerClass);
	}

}
