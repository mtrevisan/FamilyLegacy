package io.github.mtrevisan.familylegacy.v2.ui.tools;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.ProjectionMutator;


/**
 * Single dispatcher interface for executing UI mutations or high-level application actions.
 */
public interface ToolDispatcher{

	default void paste(){}

	default void removeEntity(final String id){}

	default void loadRoot(final String id){}

	default void replaceModel(final FLEFModel newModel){}

	default void editEntity(final String id){}

	/**
	 * Returns the ProjectionMutator for the active projection view, if available.
	 */
	default ProjectionMutator getMutator(){
		return null;
	}

}
