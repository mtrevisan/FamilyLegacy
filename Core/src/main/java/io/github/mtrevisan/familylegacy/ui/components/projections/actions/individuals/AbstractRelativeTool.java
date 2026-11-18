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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.individuals;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.CoParentSex;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.strategies.IndividualSearchStrategy;
import io.github.mtrevisan.familylegacy.tree.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.projections.dialogs.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ParentSexGuard;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Links an individual to the selected individual as a parent, a partner or a child. The individual is either a new one (created in
 * the "new individual" dialog), an existing one (picked from a search dialog) or the one in the clipboard (see Relocate): in the last
 * case every link that it had with other individuals is removed first, as every paste does. The relationship type is asked to the
 * user and the relationship is created through the mutator of the current view.
 * <p>
 * When the selected individual has a parent of a known sex, the new parent must be of the opposite sex: a new individual is created
 * with that sex (and the editor does not save another one), an existing one of the same sex is refused.
 */
abstract class AbstractRelativeTool implements ToolOperation{

	enum Role{PARENT, PARTNER, CHILD}

	enum Source{CREATE, SEARCH, CLIPBOARD}


	private final Role role;
	private final Source source;
	private final String[] allowedTypes;


	AbstractRelativeTool(final Role role, final Source source, final String[] allowedTypes){
		this.role = role;
		this.source = source;
		this.allowedTypes = allowedTypes;
	}


	@Override
	public void run(final ToolContext context){
		final ProjectionMutator mutator = context.mutator();
		final FLEFModel model = context.model();
		final String selectedId = context.selectedEntityId();
		if(mutator == null || selectedId == null)
			return;

		final Window owner = context.owner();
		final SexType requiredSex = (role == Role.PARENT? requiredParentSex(selectedId, model): null);
		final FLEFRecord other = switch(source){
			case CREATE -> showCreateDialog(owner, model, selectedId, requiredSex);
			case SEARCH -> showSearchDialog(owner, model, requiredSex);
			case CLIPBOARD -> context.clippedRecord();
		};
		if(other == null || selectedId.equals(other.getId()) || !IndividualHandler.isOfType(other))
			return;

		if(requiredSex != null && PreferredSex.of(other, model) == oppositeOf(requiredSex)){
			JOptionPane.showMessageDialog(owner, parentSexMessage(selectedId, requiredSex, model), I18N.t("validation.title"),
				JOptionPane.ERROR_MESSAGE);

			return;
		}

		final String label = IndividualHandler.getInstance()
			.getDisplayText(other, model);
		final List<RelationshipTypeSelectionDialog.Item> items = List.of(
			new RelationshipTypeSelectionDialog.Item(other.getId(), label, allowedTypes[0]));
		final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(owner, items, allowedTypes, null, model);
		if(selectedTypes == null || selectedTypes.isEmpty()){
			//a new individual stays in the file, even if it is not linked yet
			mutator.invalidateAndNotifyTreeChanged(selectedId);

			return;
		}

		//the subject of a child type is the child, the target is the parent; the other types have the selected individual as subject
		final String type = selectedTypes.getFirst();
		final String subjectId = (role == Role.CHILD? other.getId(): selectedId);
		final String targetId = (role == Role.CHILD? selectedId: other.getId());
		if(role != Role.PARTNER && !ParentSexGuard.confirm(owner, CoParentSex.findClashingParent(subjectId, targetId, type, null, model),
				targetId, model))
			return;

		if(source == Source.CLIPBOARD){
			//moving a person is: detach from everyone, then reattach in the new place
			mutator.removeRelationships(individualRelationshipIds(other.getId(), model));
			context.clearClippedRecord();
		}

		mutator.createRelationship(subjectId, targetId, type);
		mutator.invalidateAndNotifyTreeChanged(selectedId);
	}

	/**
	 * @return	The sex that a new parent of the child must have, because the child already has a parent of the other sex ({@code null} if
	 * 	there is no constraint).
	 */
	static SexType requiredParentSex(final String childId, final FLEFModel model){
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			if(!childId.equals(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE))
					|| !RelationshipReader.isTypeChild(RelationshipReader.extractType(relationship)))
				continue;

			final String parentId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			final SexType parentSex = (parentId != null? PreferredSex.of(model.getRecordById(parentId), model): SexType.UNKNOWN);
			if(parentSex == SexType.MALE || parentSex == SexType.FEMALE)
				return oppositeOf(parentSex);
		}
		return null;
	}

	private static SexType oppositeOf(final SexType sex){
		return (sex == SexType.MALE? SexType.FEMALE: SexType.MALE);
	}

	private static String parentSexMessage(final String childId, final SexType requiredSex, final FLEFModel model){
		final IndividualHandler handler = IndividualHandler.getInstance();
		return I18N.tf("validation.parent_sex_required", handler.getDisplayText(model.getRecordById(childId), model),
			I18N.t("enum.individual.sex." + oppositeOf(requiredSex).name().toLowerCase(Locale.ROOT)),
			I18N.t("enum.individual.sex." + requiredSex.name().toLowerCase(Locale.ROOT)));
	}

	/** @return	The relationships between the individual and other individuals (the links to groups are not touched). */
	private static List<String> individualRelationshipIds(final String individualId, final FLEFModel model){
		final List<String> ids = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String targetId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subjectId != null && targetId != null && (individualId.equals(subjectId) || individualId.equals(targetId)))
				ids.add(relationship.getId());
		}
		return ids;
	}

	private static FLEFRecord showCreateDialog(final Window owner, final FLEFModel model, final String childId,
			final SexType requiredSex){
		final RecordDialog dialog = RecordDialogs.createNew(IndividualReader.TYPE, owner, model);
		if(requiredSex != null)
			dialog.requireSex(requiredSex, parentSexMessage(childId, requiredSex, model));
		dialog.setVisible(true);

		return (dialog.isSaved()? dialog.getRecord(): null);
	}

	private static FLEFRecord showSearchDialog(final Window owner, final FLEFModel model, final SexType requiredSex){
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(owner, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		if(requiredSex != null)
			dialog.withFilter(IndividualSearchStrategy.KEY_SEX, I18N.t("enum.individual.sex." + requiredSex.name().toLowerCase(Locale.ROOT)));
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		if(context == null || !context.hasSelectedIndividual() || context.mutator() == null)
			return false;

		return switch(source){
			case CREATE -> true;
			case SEARCH -> context.model().hasRecordsByType(IndividualHandler.TYPE);
			case CLIPBOARD -> (context.canPaste() && IndividualHandler.isOfType(context.clippedRecord())
				&& !context.selectedEntityId().equals(context.clippedRecord().getId())
				&& !isAlreadyLinked(context.selectedEntityId(), context.clippedRecord().getId(), context.model()));
		};
	}

	/** @return	Whether the other individual already has, with the selected one, the very role this tool would give (nothing would change). */
	private boolean isAlreadyLinked(final String selectedId, final String otherId, final FLEFModel model){
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String targetId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subjectId == null || targetId == null)
				continue;

			final String type = RelationshipReader.extractType(relationship);
			final boolean linked = switch(role){
				case PARTNER -> RelationshipReader.isTypePartner(type)
					&& (selectedId.equals(subjectId) && otherId.equals(targetId) || selectedId.equals(targetId) && otherId.equals(subjectId));
				case PARENT -> RelationshipReader.isTypeChild(type) && selectedId.equals(subjectId) && otherId.equals(targetId);
				case CHILD -> RelationshipReader.isTypeChild(type) && otherId.equals(subjectId) && selectedId.equals(targetId);
			};
			if(linked)
				return true;
		}
		return false;
	}

}
