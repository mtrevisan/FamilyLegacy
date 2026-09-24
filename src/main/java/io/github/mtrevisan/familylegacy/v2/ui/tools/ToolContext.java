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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.ProjectionMutator;
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
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.Objects;
import java.util.function.Supplier;


/**
 * The state passed to every {@link ToolOperation}.
 */
public record ToolContext(
	FLEFModel model,
	Supplier<String> entityIdSupplier,
	Supplier<Component> selectedComponentSupplier,
	ToolDispatcher dispatcher
){

	/**
	 * Full constructor.
	 * <p>
	 * The model and the owner are required. The callbacks are
	 * optional: when {@code null}, they are replaced with their default
	 * implementations, so the fields are never {@code null} and the
	 * accessors below can call them without a null check.
	 */
	public ToolContext{
		Objects.requireNonNull(model, "model must not be null");

		if(entityIdSupplier == null)
			entityIdSupplier = () -> null;
		if(selectedComponentSupplier == null)
			selectedComponentSupplier = () -> null;
		if(dispatcher == null)
			dispatcher = new ToolDispatcher(){};
	}


	public Window owner(){
		return SwingUtilities.getWindowAncestor(selectedComponentSupplier.get());
	}

	public String selectedEntityId(){
		return entityIdSupplier.get();
	}

	public Component selectedComponent(){
		return selectedComponentSupplier.get();
	}

	public FLEFRecord clippedRecord(){
		return RelationClipboard.getInstance()
			.getRecord();
	}

	public void setClippedRecord(final FLEFRecord record){
		RelationClipboard.getInstance()
			.setRecord(record);
	}

	public void clearClippedRecord(){
		RelationClipboard.getInstance()
			.clear();
	}

	public String getClippedRecordDisplayText(){
		final FLEFRecord record = clippedRecord();
		if(record == null || model == null)
			return "";

		return (GroupHandler.TYPE.equalsIgnoreCase(record.getTag())
			? GroupHandler.getInstance().getDisplayText(record, model)
			: IndividualHandler.getInstance().getDisplayText(record, model));
	}

	public void performPaste(){
		dispatcher.paste();
	}

	public void performRemove(final String id){
		dispatcher.removeEntity(id);
	}

	public void loadRoot(final String id){
		dispatcher.loadRoot(id);
	}

	public void replaceModel(final FLEFModel newModel){
		dispatcher.replaceModel(newModel);
	}

	public void performEdit(final String id){
		dispatcher.editEntity(id);
	}

	public ProjectionMutator mutator(){
		return dispatcher.getMutator();
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
		final String id = selectedEntityId();
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
	 * Returns whether the relation clipboard currently holds an entity record.
	 */
	public boolean canPaste(){
		return RelationClipboard.getInstance().hasRecord();
	}

	/**
	 * Returns whether the selected entity exists in the model and is a Group.
	 */
	public boolean hasSelectedGroup(){
		final String id = selectedEntityId();
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
		final String id = selectedEntityId();
		return (id != null && model.hasRecord(id) && id.startsWith(SourceHandler.ID_PREFIX));
	}

	/**
	 * Returns whether the selected entity exists in the model and is an Event.
	 */
	public boolean hasSelectedEvent(){
		final String id = selectedEntityId();
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
