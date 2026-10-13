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
package io.github.mtrevisan.familylegacy.ui.components.projections.relationshipgraph;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.components.projections.individual.IndividualListener;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.repository.EgoNetworkMutator;
import io.github.mtrevisan.familylegacy.ui.components.projections.repository.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDispatcher;
import io.github.mtrevisan.familylegacy.ui.tools.individuals.UnlinkIndividualRelationshipsTool;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.List;
import java.util.function.Supplier;


/**
 * Encapsulates record dialog invocations and relationship creation
 * workflows for the ego network component.
 * <p>
 * <b>Unlink.</b> The unlink workflow is delegated to
 * {@link UnlinkIndividualRelationshipsTool}, the same tool used by the popup menu
 * and by the individual tree. The tool builds the full relationship set
 * from the model (parents, associates, groups, children), opens the
 * dialog, and applies the removal through the mutator wired in the
 * {@link ToolContext}. The handler only has to provide a
 * {@link ToolContext} with the ego network's
 * {@link EgoNetworkMutator}, so cache invalidation goes through the same
 * path as every other projection.
 */
public class EgoNetworkActionHandler{

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

		if(RelationshipReader.isTypeChild(type))
			return new String[]{otherId, egoId};

		if(RelationshipReader.isTypeGroupMember(type)){
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

	/**
	 * Opens the "Unlink Relationships" dialog for the given entity and,
	 * on confirmation, removes the selected relationships from the model.
	 * <p>
	 * The whole workflow is delegated to
	 * {@link UnlinkIndividualRelationshipsTool}, the same tool invoked by the
	 * popup menu and by the individual tree. The tool computes the full
	 * relationship set from the model via {@code IndividualHelper}, so
	 * the caller does not need to pre-compute parents, associates,
	 * groups, or children, and it does not need to know whether the
	 * entity is the current ego or a peripheral node. After the
	 * removal the tool invalidates the network through the mutator
	 * wired in the {@link ToolContext}.
	 *
	 * @param record the entity whose relationships should be unlinked;
	 *               must not be {@code null}
	 */
	public void performUnlinkRelationships(final FLEFRecord record){
		if(record == null)
			return;

		final ToolContext context = ToolContexts.custom(model, record.getId(), parentComponent,
			new ToolDispatcher(){
				@Override
				public void removeEntity(final String id){
					// Not used by UnlinkRelationshipsTool.
				}

				@Override
				public void loadRoot(final String id){
					// Not used by UnlinkRelationshipsTool.
				}

				@Override
				public ProjectionMutator getMutator(){
					return networkMutator;
				}
			});
		new UnlinkIndividualRelationshipsTool()
			.run(context);
	}

	private static RecordTypeHandler<?> resolveHandler(final Class<? extends RecordTypeHandler<?>> handlerClass){
		if(GroupHandler.class.equals(handlerClass))
			return GroupHandler.getInstance();
		if(IndividualHandler.class.equals(handlerClass))
			return IndividualHandler.getInstance();

		throw new IllegalArgumentException("Unsupported handler class: " + handlerClass);
	}

}
