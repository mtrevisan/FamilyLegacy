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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.RelationClipboard;
import org.apache.commons.lang3.StringUtils;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.io.File;


/**
 * The state passed to every {@link ToolOperation}.
 */
public record ToolContext(
	FLEFModel model,
	String entityIdSupplier,
	Component selectedComponentSupplier,
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
		if(dispatcher == null)
			dispatcher = new ToolDispatcher(){};
	}


	public Window owner(){
		return (selectedComponentSupplier != null
			? SwingUtilities.getWindowAncestor(selectedComponentSupplier)
			: null);
	}

	public String selectedEntityId(){
		return entityIdSupplier;
	}

	public Component selectedComponent(){
		return selectedComponentSupplier;
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
			return StringUtils.EMPTY;

		return (GroupHandler.isOfType(record)
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

	public void openImported(final FLEFModel importedModel, final File source){
		dispatcher.openImported(importedModel, source);
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
	 * Returns whether the selected entity exists in the model and is an Individual.
	 */
	public boolean hasSelectedIndividual(){
		final FLEFRecord selectedEntity = getSelectedEntity();
		return IndividualHandler.isOfType(selectedEntity);
	}

	/**
	 * Returns whether the selected entity exists in the model and is a Group.
	 */
	public boolean hasSelectedGroup(){
		final FLEFRecord selectedEntity = getSelectedEntity();
		return GroupHandler.isOfType(selectedEntity);
	}

	public boolean isSelectedEntityARoot(){
		final FLEFRecord selectedEntity = getSelectedEntity();
		if(selectedEntity == null || dispatcher == null)
			return false;

		final String selectedEntityId = selectedEntityId();
		final String[] rootIds = dispatcher.getRootIds();
		return (selectedEntityId.equalsIgnoreCase(rootIds[0]) || selectedEntityId.equalsIgnoreCase(rootIds[1]));
	}

	/**
	 * Returns the selected Individual record, falling back to the first available Individual in the model.
	 */
	public FLEFRecord getSelectedEntity(){
		final String selectedEntityId = selectedEntityId();
		return model.getRecordById(selectedEntityId);
	}

	/**
	 * Returns whether the current model contains sources or documents.
	 */
	public boolean hasSourcesOrDocuments(){
		return (hasAnySources() || hasAnyDocuments());
	}

	/**
	 * Returns whether the current model contains research questions or tasks.
	 */
	public boolean hasResearchData(){
		return (hasAnyResearchQuestions() || hasAnyResearchTasks());
	}

	/**
	 * Returns whether the current model contains at least one place.
	 */
	public boolean hasAnyPlaces(){
		return !model.getRecordsByType(PlaceHandler.TYPE)
			.isEmpty();
	}

	/**
	 * Returns whether the current model contains at least `count` places.
	 */
	public boolean hasAtLeastPlaces(final int count){
		return (model.getRecordsByType(PlaceHandler.TYPE)
			.size() >= count);
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
	 * Returns whether the current model contains at least `count` groups.
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
		return !model.getRecordsByType(MediaHandler.TYPE)
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
