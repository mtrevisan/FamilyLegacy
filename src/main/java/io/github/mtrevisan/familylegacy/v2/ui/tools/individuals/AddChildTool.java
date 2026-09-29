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
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.ProjectionMutator;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Opens the "add child" workflow for the currently selected individual.
 */
public final class AddChildTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.individual.add.child");
	}

	@Override
	public void run(final ToolContext context){
		final Window owner = context.owner();
		final FLEFModel model = context.model();
		final var dialog = IndividualHandler.getInstance()
			.createNewDialog(owner, model);
		dialog.setVisible(true);

		if(!dialog.isSaved())
			return;

		final FLEFRecord childRecord = dialog.getRecord();
		if(childRecord == null)
			return;

		final Component sourcePanel = context.selectedComponent();
		final String targetId = context.selectedEntityId();
		final ProjectionMutator mutator = context.mutator();
		executeAddChildWorkflow(owner, model, sourcePanel, targetId, childRecord, mutator);
	}

	public static void executeAddChildWorkflow(final Window owner, final FLEFModel model,
			final Component sourcePanel, final String targetId, final FLEFRecord childRecord,
			final ProjectionMutator mutator){
		if(childRecord == null || childRecord.getId() == null)
			return;

		final String childId = childRecord.getId();
		if(childId.equals(targetId)){
			JOptionPane.showMessageDialog(owner,
				I18N.t("confirmation.add.child.message"),
				I18N.t("confirmation.add.child.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>();
		final FLEFRecord targetRecord = model.getRecordById(targetId);
		final String targetLabel = (targetRecord != null
			? IndividualHelper.displayName(targetRecord) + " [" + targetId + "]"
			: targetId);
		items.add(new RelationshipTypeSelectionDialog.Item(targetId, targetLabel,
			IndividualHelper.CHILD_RELATION_TYPES.getFirst()));

		final String partnerId = findShownPartner(sourcePanel, targetId);
		if(partnerId != null){
			final FLEFRecord partnerRecord = model.getRecordById(partnerId);
			final String partnerLabel = (partnerRecord != null
				? IndividualHelper.displayName(partnerRecord) + " [" + partnerId + "]"
				: partnerId);
			items.add(new RelationshipTypeSelectionDialog.Item(partnerId, partnerLabel,
				IndividualHelper.CHILD_RELATION_TYPES.getFirst()));
		}

		final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(
			owner, items, IndividualHelper.CHILD_RELATION_TYPES.toArray(String[]::new), null, model);
		if(selectedTypes == null || selectedTypes.isEmpty())
			return;

		// Route the mutation through the projection mutator, so that the
		// shared repository and the tree service caches are invalidated
		// (notifyRelationshipAdded) and the incremental update path is
		// exercised. Writing through the model directly would leave the
		// caches stale and the tree would keep showing the old structure.
		final String firstType = selectedTypes.get(0);
		if(mutator != null)
			mutator.createRelationship(childId, targetId, firstType);
		else
			IndividualHelper.createRelationship(model, childId, targetId, firstType,
				RelationshipHandler.ID_PREFIX);

		if(partnerId != null && selectedTypes.size() > 1){
			final String secondType = selectedTypes.get(1);
			mutator.createRelationship(childId, partnerId, secondType);
		}

		// Force the tree to rebuild from the updated model. The focus id is
		// the target that the user acted on; passing it keeps the current
		// root and scroll position stable.
		mutator.invalidateAndNotifyTreeChanged(targetId);
	}

	/**
	 * Returns the id of the partner of the target individual, as currently
	 * displayed in the tree panel.
	 * <p>
	 * The partner is the other box in the same {@link PartnersPanel}, when
	 * the target panel belongs to one. Returns {@code null} when the target
	 * is shown alone: a lone box, a child in a {@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel}, or a
	 * panel whose other half is empty. In those cases the caller must not
	 * offer a second relationship to create.
	 *
	 * @param sourcePanel the panel from which the popup menu was opened
	 * @param targetId    the id of the target individual
	 * @return the partner id, or {@code null} when none is shown
	 */
	private static String findShownPartner(final Component sourcePanel, final String targetId){
		if(!(sourcePanel instanceof IndividualPanel targetPanel))
			return null;

		final PartnersPanel partnersPanel = PartnersPanel.findContainingPartnersPanel(
			targetPanel.getParent());
		if(partnersPanel == null)
			return null;

		final IndividualPanel otherPanel = (partnersPanel.getFatherPanel() == targetPanel
			? partnersPanel.getMotherPanel()
			: partnersPanel.getFatherPanel());
		if(otherPanel == null)
			return null;

		final IndividualData otherData = otherPanel.getData();
		if(otherData == null || otherData.isEmpty())
			return null;

		final String otherId = otherData.getId();
		return (otherId != null && !otherId.equals(targetId)? otherId: null);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedEntity());
	}

}
