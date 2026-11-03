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
package io.github.mtrevisan.familylegacy.ui.tools;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionMutator;

import java.io.File;


/**
 * Single dispatcher interface for executing UI mutations or high-level application actions.
 */
public interface ToolDispatcher{

	default String[] getRootIds(){
		return new String[2];
	}

	default void editEntity(final String id){}

	default void paste(){}

	/** Connects an existing individual to the selected (empty) individual box. */
	default void connectIndividual(){}

	/** Connects an existing individual as a child of the selected individual. */
	default void connectChild(){}

	default void removeEntity(final String id){}

	default void loadRoot(final String id){}

	default void replaceModel(final FLEFModel newModel){}

	/**
	 * Replaces the model with the content of a backup: unlike {@link #replaceModel(FLEFModel)} the document loses the name of its file
	 * (a save must not overwrite it with the restored data) and counts as having unsaved changes.
	 */
	default void restoreModel(final FLEFModel restoredModel){
		replaceModel(restoredModel);
	}

	/**
	 * Asks the user what to do with the unsaved changes of the open document, before something replaces it.
	 *
	 * @return	{@code false} if the user cancelled (or saving failed): the operation must not go on.
	 */
	default boolean confirmDiscardChanges(){
		return true;
	}

	/**
	 * Saves a model imported from another format as a new FLEF file, chosen by the user, and opens it.
	 *
	 * @param source	The file the model was imported from.
	 */
	default void openImported(final FLEFModel importedModel, final File source){}

	/**
	 * Returns the ProjectionMutator for the active projection view, if available.
	 */
	default ProjectionMutator getMutator(){
		return null;
	}

}
