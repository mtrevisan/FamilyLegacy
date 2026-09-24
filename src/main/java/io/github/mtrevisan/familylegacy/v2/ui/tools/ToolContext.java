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
package io.github.mtrevisan.familylegacy.v2.ui.tools;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JFrame;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * The state passed to every {@link ToolOperation}.
 * <p>
 * A tool receives the model it operates on, the frame that owns its
 * dialogs, and a small set of callbacks that let it interact with the
 * application without depending on the concrete frame class. The
 * callbacks are the only channel through which a tool can query or
 * change the application state: a tool that only needs to read the
 * model simply ignores them.
 * <p>
 * <b>Fields.</b>
 * <ul>
 *   <li>{@link #model()} — the FLEF model; never {@code null};</li>
 *   <li>{@link #owner()} — the frame that owns the tool's dialogs;
 *       never {@code null};</li>
 *   <li>{@link #editCurrentSelection()} — opens the edit dialog for the
 *       entity currently selected in the active projection;</li>
 *   <li>{@link #currentSelectionId()} — the id of the entity currently
 *       selected in the active projection, or {@code null};</li>
 *   <li>{@link #selectedEntityId()} — convenience alias for
 *       {@link #currentSelectionId()}, provided because most tools
 *       operate on individuals;</li>
 *   <li>{@link #loadRoot(String)} — loads an individual as the root of
 *       the active projection;</li>
 *   <li>{@link #replaceModel(FLEFModel)} — replaces the whole model,
 *       rebuilding the application around it; used by the import tools
 *       and by the file operations;</li>
 *   <li>{@link #currentView()} — the panel of the currently visible
 *       projection; used by the print tool.</li>
 * </ul>
 * <p>
 * <b>Default values.</b> Every callback has a sensible default so that
 * a tool can be run in a context that does not support it (for example,
 * a headless test, or a menu entry that is not wired to a frame):
 * <ul>
 *   <li>{@code editCurrentSelection} defaults to a no-op;</li>
 *   <li>{@code currentSelectionId} defaults to a supplier that always
 *       returns {@code null};</li>
 *   <li>{@code loadRoot} defaults to a consumer that ignores its
 *       argument;</li>
 *   <li>{@code replaceModel} defaults to a consumer that ignores its
 *       argument;</li>
 *   <li>{@code currentView} defaults to a supplier that returns
 *       {@code null}.</li>
 * </ul>
 * A tool that needs one of these callbacks and finds the default is
 * expected to fail gracefully: it should tell the user that the action
 * is not available rather than throw.
 * <p>
 * The record is immutable. Every field is set at construction and never
 * changes. A tool that needs a different context should build a new one
 * rather than mutate the existing one.
 */
public record ToolContext(
	FLEFModel model,
	JFrame owner,
	Runnable editCurrentSelection,
	Supplier<String> currentSelectionId,
	Consumer<String> loadRoot,
	Consumer<FLEFModel> replaceModel,
	Supplier<Component> currentViewSupplier){


	/**
	 * Full constructor.
	 * <p>
	 * The model and the owner are required. The five callbacks are
	 * optional: when {@code null}, they are replaced with their default
	 * implementations, so the fields are never {@code null} and the
	 * accessors below can call them without a null check.
	 */
	public ToolContext{
		Objects.requireNonNull(model, "model must not be null");
		Objects.requireNonNull(owner, "owner must not be null");

		if(editCurrentSelection == null)
			editCurrentSelection = () -> {};
		if(currentSelectionId == null)
			currentSelectionId = () -> null;
		if(loadRoot == null)
			loadRoot = id -> {};
		if(replaceModel == null)
			replaceModel = m -> {};
		if(currentViewSupplier == null)
			currentViewSupplier = () -> null;
	}

	/**
	 * Convenience constructor for tools that only need the model and the
	 * owner. Every callback is set to its default.
	 */
	public ToolContext(final FLEFModel model, final JFrame owner){
		this(model, owner, null, null, null, null, null);
	}

	/**
	 * Convenience constructor for tools that need the model, the owner,
	 * and the current selection, but do not change the root or the model.
	 */
	public ToolContext(final FLEFModel model, final JFrame owner,
		final Runnable editCurrentSelection,
		final Supplier<String> currentSelectionId){
		this(model, owner, editCurrentSelection, currentSelectionId,
			null, null, null);
	}

	/**
	 * Convenience constructor for tools that need the model, the owner,
	 * the current selection, and the root navigation, but do not replace
	 * the model or query the current view.
	 */
	public ToolContext(final FLEFModel model, final JFrame owner,
		final Runnable editCurrentSelection,
		final Supplier<String> currentSelectionId,
		final Consumer<String> loadRoot){
		this(model, owner, editCurrentSelection, currentSelectionId,
			loadRoot, null, null);
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	/**
	 * Opens the edit dialog for the entity currently selected in the
	 * active projection. The definition of "selected" depends on the
	 * projection: the individual with the red border in the tree, the
	 * root of the Sugiyama graph, the entity with the red border in the
	 * ego network. When nothing is selected, the call is a no-op.
	 */
	public void getEditCurrentSelection(){
		editCurrentSelection.run();
	}

	/**
	 * Returns the id of the entity currently selected in the active
	 * projection, or {@code null} when nothing is selected.
	 */
	public String getCurrentSelectionId(){
		return currentSelectionId.get();
	}

	/**
	 * Returns the id of the individual currently selected in the active
	 * projection, or {@code null}.
	 * <p>
	 * This is an alias for {@link #currentSelectionId()}, named for the
	 * common case. The two methods are intentionally identical: a tool
	 * that operates on groups or places uses {@code currentSelectionId}
	 * directly, and a tool that operates on individuals can use either.
	 */
	public String selectedEntityId(){
		return currentSelectionId.get();
	}

	/**
	 * Loads the given individual as the root of the active projection.
	 * The call behaves like a click on the individual's name in the
	 * current view: the projection is rebuilt around the new root and
	 * the navigation is pushed into the history.
	 *
	 * @param individualId the individual id; {@code null} is ignored
	 */
	public void loadRoot(final String individualId){
		if(individualId != null)
			loadRoot.accept(individualId);
	}

	/**
	 * Replaces the current model with a new one, rebuilding the
	 * application around it. Called by the import tools and by the file
	 * operations.
	 * <p>
	 * When the replacement is not supported by the enclosing context,
	 * the call is a no-op: the caller is expected to verify the
	 * capability before relying on it, or to accept the silent failure
	 * when the tool is invoked from a context that does not support
	 * model replacement.
	 *
	 * @param newModel the new model; {@code null} is ignored
	 */
	public void replaceModel(final FLEFModel newModel){
		if(newModel != null)
			replaceModel.accept(newModel);
	}

	/**
	 * Returns the panel of the currently visible projection, or
	 * {@code null} when no view is active. Used by the print tool to
	 * determine what to print.
	 */
	public Component currentView(){
		return currentViewSupplier.get();
	}


	/* ======================================================================
	 *                          Helper methods for isEnabled
	 * ====================================================================== */

	/**
	 * Returns whether a model is currently loaded and available.
	 */
	public boolean hasModel(){
		return (model != null);
	}

	/**
	 * Returns whether an entity is currently selected in the active projection.
	 */
	public boolean hasSelection(){
		final String id = getCurrentSelectionId();
		return StringUtils.isNotEmpty(id);
	}

	/**
	 * Returns whether the selected entity exists in the model.
	 */
	public boolean hasSelectedEntity(){
		final String id = selectedEntityId();
		return (id != null && model.hasRecord(id));
	}

	/**
	 * Returns whether the selected entity exists in the model and is a Group.
	 */
	public boolean hasSelectedGroup(){
		final String id = getCurrentSelectionId();
		return (id != null && model.hasRecord(id) && id.startsWith(GroupHandler.ID_PREFIX));
	}

	/**
	 * Returns whether the current model contains at least one place.
	 */
	public boolean hasAnyPlaces(){
		return !model.getRecordsByType(PlaceHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one place.
	 */
	public boolean hasAtLeastPlaces(final int count){
		return (model.getRecordsByType(PlaceHandler.TYPE)
			.size() >= count);
	}

	/**
	 * Returns whether the selected entity exists in the model and is a Source.
	 */
	public boolean hasSelectedSource(){
		final String id = getCurrentSelectionId();
		return (id != null && model.hasRecord(id) && id.startsWith(SourceHandler.ID_PREFIX));
	}

	/**
	 * Returns whether the selected entity exists in the model and is an Event.
	 */
	public boolean hasSelectedEvent(){
		final String id = getCurrentSelectionId();
		return (id != null && model.hasRecord(id) && id.startsWith(EventHandler.ID_PREFIX));
	}

	/**
	 * Returns whether the current model contains at least one individual.
	 */
	public boolean hasAnyIndividuals(){
		return !model.getRecordsByType(IndividualHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least `count` individuals.
	 */
	public boolean hasAtLeastIndividuals(final int count){
		return (model.getRecordsByType(IndividualHandler.TYPE)
			.size() >= count);
	}

	/**
	 * Returns whether the current model contains at least one group.
	 */
	public boolean hasAnyGroups(){
		return !model.getRecordsByType(GroupHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one group.
	 */
	public boolean hasAtLeastGroups(final int count){
		return (model.getRecordsByType(GroupHandler.TYPE)
			.size() >= count);
	}

	/**
	 * Returns whether the current model contains at least one source.
	 */
	public boolean hasAnySources(){
		return !model.getRecordsByType(SourceHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one document.
	 */
	public boolean hasAnyDocuments(){
		return !model.getRecordsByType(DocumentHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one repository.
	 */
	public boolean hasAnyRepositories(){
		return !model.getRecordsByType(RepositoryHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one event.
	 */
	public boolean hasAnyEvents(){
		return !model.getRecordsByType(EventHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one conclusion.
	 */
	public boolean hasAnyConclusions(){
		return !model.getRecordsByType(ConclusionHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one identity hypothesis.
	 */
	public boolean hasAnyIdentityHypotheses(){
		return !model.getRecordsByType(IdentityHypothesisHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one research activity.
	 */
	public boolean hasAnyResearchActivities(){
		return !model.getRecordsByType(ResearchActivityHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one research question.
	 */
	public boolean hasAnyResearchQuestions(){
		return !model.getRecordsByType(ResearchQuestionHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least one research task.
	 */
	public boolean hasAnyResearchTasks(){
		return !model.getRecordsByType(ResearchTaskHandler.TYPE)
			.isEmpty();
	}

}
