package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Place} menu.
 * <p>
 * Adding a new tool means adding one line here and one class in this
 * package: the menu bar itself does not change.
 */
public final class PlaceToolRegistry{

	private PlaceToolRegistry(){
	}


	/** Tools that perform real work, in menu order. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new ManagePlacesTool(),
			new NewPlaceTool(),
			new PlaceHierarchyTool(),
			new PlaceRelationshipsTool()
		);
	}

	public static List<ToolOperation> geoAndNormalizationTools(){
		return List.of(
			new ShowOnMapTool(),
			new NormalizePlaceNamesTool()
		);
	}

}
