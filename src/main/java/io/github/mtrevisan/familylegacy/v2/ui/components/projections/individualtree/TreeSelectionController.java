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

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualData;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.TreeNode;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.siblings.SiblingsPanel;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Manages the visual selection of the tree: which individual carries the
 * red border, and how that selection changes.
 * <p>
 * The selection is independent from the root of the tree. The user can
 * move the red border with the arrow keys and confirm it with Enter,
 * without re-rooting the tree until confirmation.
 * <p>
 * The controller has no dependency on the model. It receives the
 * currently displayed panels through {@link #updateTree} and updates
 * their visual state when the selection changes. The panel that owns
 * the controller wires the two callbacks: one for selection changes
 * (so the enclosing frame can populate a dossier) and one for
 * confirmation (so the panel can re-root the tree).
 */
public final class TreeSelectionController{

	/** Id of the currently selected individual, or {@code null}. */
	private String selectedId;

	/** Currently displayed couple panels, keyed by tree node. */
	private Map<TreeNode, PartnersPanel> nodeToPanelMap = Map.of();
	/** Currently displayed sibling panel, or {@code null}. */
	private SiblingsPanel childrenPanel;

	private final Consumer<String> onSelectionChanged;
	private final Consumer<String> onRootConfirmed;


	public TreeSelectionController(final Consumer<String> onSelectionChanged, final Consumer<String> onRootConfirmed){
		this.onSelectionChanged = onSelectionChanged;
		this.onRootConfirmed = onRootConfirmed;
	}


	/**
	 * Updates the references to the panels whose border must be tracked.
	 * Called after every rebuild, because the panels are recreated on
	 * every {@code refreshTree}.
	 */
	public void updateTree(final Map<TreeNode, PartnersPanel> nodeToPanelMap, final SiblingsPanel childrenPanel){
		this.nodeToPanelMap = (nodeToPanelMap != null? nodeToPanelMap: Map.of());
		this.childrenPanel = childrenPanel;
	}

	public String selectedId(){
		return selectedId;
	}

	/**
	 * Returns the {@link IndividualPanel} matching the currently selected ID,
	 * or {@code null} if no panel is selected or matched.
	 */
	public IndividualPanel getSelectedPanel(){
		if(selectedId == null)
			return null;

		for(final Map.Entry<TreeNode, PartnersPanel> entry : nodeToPanelMap.entrySet()){
			final PartnersPanel partners = entry.getValue();
			if(partners == null)
				continue;

			final IndividualPanel father = partners.getFatherPanel();
			if(father != null){
				final IndividualData fatherData = father.getData();
				if(fatherData != null && selectedId.equals(fatherData.getId()))
					return father;
			}

			final IndividualPanel mother = partners.getMotherPanel();
			if(mother != null){
				final IndividualData motherData = mother.getData();
				if(motherData != null && selectedId.equals(motherData.getId()))
					return mother;
			}
		}

		if(childrenPanel != null){
			for(final IndividualPanel siblingBox : childrenPanel.getSiblingBoxes()){
				if(siblingBox != null){
					final IndividualData siblingData = siblingBox.getData();
					if(siblingData != null && selectedId.equals(siblingData.getId()))
						return siblingBox;
				}
			}
		}

		return null;
	}

	/** Clears the selection without notifying the callback. */
	public void clear(){
		selectedId = null;
	}

	/**
	 * Sets the selection in response to a user action (click on a panel,
	 * navigation via the spatial keys) and notifies the callback.
	 */
	public void onUserSelected(final String id){
		if(id == null)
			return;

		selectedId = id;

		apply();

		if(onSelectionChanged != null)
			onSelectionChanged.accept(id);
	}

	/**
	 * Sets the selection programmatically, without notifying the
	 * callback. Used after a rebuild to restore the previous selection
	 * or to fall back to the current root.
	 */
	public void setSelectedId(final String id){
		if(Objects.equals(selectedId, id))
			return;

		selectedId = id;

		apply();
	}

	/**
	 * Applies the visual state to every visible panel: the one whose
	 * individual id matches the selection carries the red border, the
	 * others do not.
	 */
	public void apply(){
		for(final Map.Entry<TreeNode, PartnersPanel> entry : nodeToPanelMap.entrySet()){
			final PartnersPanel partners = entry.getValue();
			applyToSlot(partners.getFatherPanel());
			applyToSlot(partners.getMotherPanel());
		}
		if(childrenPanel != null)
			for(final IndividualPanel siblingBox : childrenPanel.getSiblingBoxes())
				applyToSlot(siblingBox);
	}

	private void applyToSlot(final IndividualPanel panel){
		if(panel == null)
			return;

		final IndividualData data = panel.getData();
		final boolean isSelected = (data != null && data.getId() != null && data.getId().equals(selectedId));
		panel.withSelected(isSelected);
	}

	/**
	 * Moves the selection to the closest individual in the given
	 * direction, based on the on-screen bounds of the visible panels.
	 *
	 * @param direction   the direction to move
	 * @param treeCanvas  the canvas to walk to compute the bounds
	 * @param rootFallback the id to start from when nothing is selected
	 */
	public void move(final SpatialNavigation.Direction direction, final Component treeCanvas, final String rootFallback){
		if(direction == null || treeCanvas == null)
			return;

		final Map<String, Rectangle> bounds =
			IndividualPanelFinder.collectScreenBounds(treeCanvas);
		if(bounds.isEmpty())
			return;

		String current = selectedId;
		if(current == null || !bounds.containsKey(current))
			current = rootFallback;
		if(current == null || !bounds.containsKey(current))
			return;

		final String next = SpatialNavigation.next(bounds, current, direction);
		if(next == null || next.equals(selectedId))
			return;

		selectedId = next;
		apply();
	}

	/**
	 * Confirms the current selection by notifying the confirmation
	 * callback. The callback is responsible for re-rooting the tree.
	 */
	public void confirm(){
		if(selectedId == null)
			return;

		if(onRootConfirmed != null)
			onRootConfirmed.accept(selectedId);
	}

}
