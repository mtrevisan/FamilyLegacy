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
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.SpatialNavigation;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.group.GroupPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individual.IndividualPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Manages entity selection state and spatial keyboard navigation within the ego network.
 */
class EgoNetworkSelectionModel{

	private String selectedIndividualId;
	private String selectedGroupId;


	public String getSelectedIndividualId(){
		return selectedIndividualId;
	}

	public void setSelectedIndividualId(final String selectedIndividualId){
		this.selectedIndividualId = selectedIndividualId;
		this.selectedGroupId = null;
	}

	public String getSelectedGroupId(){
		return selectedGroupId;
	}

	public void setSelectedGroupId(final String selectedGroupId){
		this.selectedGroupId = selectedGroupId;
		this.selectedIndividualId = null;
	}

	public String getSelectedEntityId(){
		return (selectedIndividualId != null? selectedIndividualId: selectedGroupId);
	}

	public void clearSelection(){
		selectedIndividualId = null;
		selectedGroupId = null;
	}

	public void applySelection(final Map<EgoNode, JPanel> nodeToPanelMap, final Map<FLEFRecord, JPanel> groupToPanelMap){
		for(final Map.Entry<EgoNode, JPanel> entry : nodeToPanelMap.entrySet()){
			final EgoNode node = entry.getKey();
			final boolean isSelected = (selectedIndividualId != null && selectedIndividualId.equals(node.getEgoId()));
			applyIndividualSelectionToPanel(entry.getValue(), isSelected);
		}
		for(final Map.Entry<FLEFRecord, JPanel> entry : groupToPanelMap.entrySet()){
			final FLEFRecord group = entry.getKey();
			final boolean isSelected = (selectedGroupId != null && selectedGroupId.equals(group.getId()));
			applyGroupSelectionToPanel(entry.getValue(), isSelected);
		}
	}

	public void moveSelection(final SpatialNavigation.Direction direction, final String currentEgoId,
			final FLEFModel model, final Map<EgoNode, JPanel> nodeToPanelMap,
			final Map<FLEFRecord, JPanel> groupToPanelMap){
		final Map<String, Rectangle> bounds = collectVisibleBounds(nodeToPanelMap, groupToPanelMap);
		if(bounds.isEmpty())
			return;

		String current = getSelectedEntityId();
		if(current == null || !bounds.containsKey(current))
			current = currentEgoId;
		if(current == null || !bounds.containsKey(current))
			return;

		final String next = SpatialNavigation.next(bounds, current, direction);
		if(next == null)
			return;

		final FLEFRecord record = model.getRecordById(next);
		if(record != null && GroupHandler.TYPE.equalsIgnoreCase(record.getTag())){
			if(next.equals(selectedGroupId))
				return;

			setSelectedGroupId(next);
		}
		else{
			if(next.equals(selectedIndividualId))
				return;

			setSelectedIndividualId(next);
		}

		applySelection(nodeToPanelMap, groupToPanelMap);
	}

	private Map<String, Rectangle> collectVisibleBounds(final Map<EgoNode, JPanel> nodeToPanelMap,
			final Map<FLEFRecord, JPanel> groupToPanelMap){
		final Map<String, Rectangle> result = new LinkedHashMap<>();
		for(final Map.Entry<EgoNode, JPanel> entry : nodeToPanelMap.entrySet()){
			final String id = entry.getKey().getEgoId();
			if(id != null)
				addBounds(id, entry.getValue(), result);
		}
		for(final Map.Entry<FLEFRecord, JPanel> entry : groupToPanelMap.entrySet()){
			final String id = entry.getKey().getId();
			if(id != null)
				addBounds(id, entry.getValue(), result);
		}
		return result;
	}

	private static void addBounds(final String id, final Component component, final Map<String, Rectangle> out){
		try{
			final Point p = component.getLocationOnScreen();
			out.putIfAbsent(id, new Rectangle(p.x, p.y, component.getWidth(), component.getHeight()));
		}
		catch(final java.awt.IllegalComponentStateException ignored){
			// Panel not showing
		}
	}

	private static void applyIndividualSelectionToPanel(final Component panel, final boolean isSelected){
		if(panel instanceof IndividualPanel individualPanel)
			individualPanel.withSelected(isSelected);
		if(panel instanceof Container container)
			for(final Component child : container.getComponents())
				applyIndividualSelectionToPanel(child, isSelected);
	}

	private static void applyGroupSelectionToPanel(final Component panel, final boolean isSelected){
		if(panel instanceof GroupPanel groupPanel)
			groupPanel.withSelected(isSelected);
		if(panel instanceof Container container)
			for(final Component child : container.getComponents())
				applyGroupSelectionToPanel(child, isSelected);
	}

}
