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
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import io.github.mtrevisan.familylegacy.search.strategies.IndividualSearchStrategy;
import io.github.mtrevisan.familylegacy.tree.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.projections.dialogs.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ParentSexGuard;

import javax.swing.JOptionPane;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
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
			case SEARCH -> showSearchDialog(owner, model, requiredSex, selectedId);
			case CLIPBOARD -> context.clippedRecord();
		};
		if(other == null || selectedId.equals(other.getId()) || !IndividualHandler.isOfType(other))
			return;

		if(requiredSex != null && PreferredSex.of(other, model) == oppositeOf(requiredSex)){
			MessageDialogs.show(owner, parentSexMessage(selectedId, requiredSex, model), I18N.t("validation.title"),
				JOptionPane.ERROR_MESSAGE);

			return;
		}

		final IndividualHandler handler = IndividualHandler.getInstance();
		final String label = handler.getDisplayText(other, model);
		//for a new or an existing child of an individual with partners the same dialog also asks who the other parent is
		final List<FLEFRecord> partners = (role == Role.CHILD && source != Source.CLIPBOARD? partnersOf(selectedId, other.getId(), model): List.of());
		final String type;
		//when and where the union began, for a partner
		QuickEntryService.LifeFact union = QuickEntryService.LifeFact.NONE;
		String otherParentId = null;
		String otherParentType = null;
		if(!partners.isEmpty()){
			final RelationshipTypeSelectionDialog.Item parent = new RelationshipTypeSelectionDialog.Item(selectedId,
				handler.getDisplayText(model.getRecordById(selectedId), model), allowedTypes[0]);
			//the other parent must be of the opposite sex of the first one (when this is known)
			final SexType parentSex = PreferredSex.of(model.getRecordById(selectedId), model);
			final SexType otherSex = (parentSex == SexType.MALE || parentSex == SexType.FEMALE? oppositeOf(parentSex): null);
			final RelationshipTypeSelectionDialog.OtherParentRow otherRow = new RelationshipTypeSelectionDialog.OtherParentRow(
				partners.stream()
						.map(partner -> new RelationshipTypeSelectionDialog.Item(partner.getId(), handler.getDisplayText(partner, model),
						allowedTypes[0]))
					.toList(),
				biological -> pickOtherParent(owner, model, other.getId(), selectedId, (biological? otherSex: null)),
				item -> otherSex == null || PreferredSex.of(model.getRecordById(item.id()), model) != parentSex);
			final RelationshipTypeSelectionDialog.ParentsChoice choice = RelationshipTypeSelectionDialog.selectParents(owner, parent,
				otherRow, allowedTypes, model);
			if(choice == null){
				//a new individual stays in the file, even if it is not linked yet
				mutator.invalidateAndNotifyTreeChanged(selectedId);

				return;
			}

			type = choice.parentType();
			if(choice.otherParent() != null){
				otherParentId = choice.otherParent().id();
				otherParentType = choice.otherType();
			}
		}
		else if(role == Role.PARTNER){
			final RelationshipTypeSelectionDialog.UnionChoice choice = RelationshipTypeSelectionDialog.selectUnion(owner,
				new RelationshipTypeSelectionDialog.Item(other.getId(), label, allowedTypes[0]), allowedTypes, model);
			if(choice == null){
				//a new individual stays in the file, even if it is not linked yet
				mutator.invalidateAndNotifyTreeChanged(selectedId);

				return;
			}

			type = choice.type();
			union = choice.fact();
		}
		else if(role == Role.PARENT && source == Source.CREATE && !typeRequested()){
			//almost every parent is a biological one: the type is asked only on request (Shift), and can be changed in the relationship
			type = RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD;
		}
		else{
			final List<RelationshipTypeSelectionDialog.Item> items = List.of(
				new RelationshipTypeSelectionDialog.Item(other.getId(), label, allowedTypes[0]));
			final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(owner, items, allowedTypes, null, model);
			if(selectedTypes == null || selectedTypes.isEmpty()){
				//a new individual stays in the file, even if it is not linked yet
				mutator.invalidateAndNotifyTreeChanged(selectedId);

				return;
			}

			type = selectedTypes.getFirst();
		}

		//the subject of a child type is the child, the target is the parent; the other types have the selected individual as subject
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

		if(otherParentId != null && !ParentSexGuard.confirm(owner,
				CoParentSex.findClashBetween(subjectId, targetId, type, otherParentId, otherParentType, model), targetId, model))
			return;

		mutator.createRelationship(subjectId, targetId, type);
		//the relationship and the event that began it are different records: the event is added to the relationship, not instead of it
		if(role == Role.PARTNER)
			QuickEntryService.addUnionEvent(model, selectedId, other.getId(), type, union);
		if(otherParentId != null && ParentSexGuard.confirm(owner,
				CoParentSex.findClashingParent(subjectId, otherParentId, otherParentType, null, model), otherParentId, model))
			mutator.createRelationship(subjectId, otherParentId, otherParentType);
		mutator.invalidateAndNotifyTreeChanged(selectedId);
	}

	/** @return	Whether the user asked to choose the type of the relationship: the item of the menu was chosen holding Shift. */
	private static boolean typeRequested(){
		final AWTEvent event = EventQueue.getCurrentEvent();
		return (event instanceof ActionEvent actionEvent && (actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0
			|| event instanceof InputEvent inputEvent && inputEvent.isShiftDown());
	}

	/** @return	The individual picked as the other parent of the child, as a row of the dialog ({@code null} if none or not valid). */
	private static RelationshipTypeSelectionDialog.Item pickOtherParent(final Window owner, final FLEFModel model, final String childId,
			final String parentId, final SexType requiredSex){
		final FLEFRecord picked = showSearchDialog(owner, model, requiredSex, childId);
		if(!IndividualHandler.isOfType(picked) || childId.equals(picked.getId()) || parentId.equals(picked.getId()))
			return null;

		return new RelationshipTypeSelectionDialog.Item(picked.getId(), IndividualHandler.getInstance().getDisplayText(picked, model),
			null);
	}

	/** @return	The partners of the individual, the child excluded. */
	static List<FLEFRecord> partnersOf(final String individualId, final String childId, final FLEFModel model){
		final List<FLEFRecord> partners = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			if(!RelationshipReader.isTypePartner(RelationshipReader.extractType(relationship)))
				continue;

			final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String targetId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(subjectId == null || targetId == null)
				continue;

			final String partnerId = (individualId.equals(subjectId)? targetId: (individualId.equals(targetId)? subjectId: null));
			if(partnerId == null || partnerId.equals(childId) || partners.stream().anyMatch(p -> partnerId.equals(p.getId())))
				continue;

			final FLEFRecord partner = model.getRecordById(partnerId);
			if(partner != null)
				partners.add(partner);
		}
		return partners;
	}

	/**
	 * @return	The sex that a new parent of the child must have, because the child already has a parent of the other sex ({@code null} if
	 * 	there is no constraint).
	 */
	static SexType requiredParentSex(final String childId, final FLEFModel model){
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipReader.TYPE)){
			if(!childId.equals(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE))
					|| !RelationshipReader.isTypeBiologicalChild(RelationshipReader.extractType(relationship)))
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

	private static FLEFRecord showSearchDialog(final Window owner, final FLEFModel model, final SexType requiredSex,
			final String childId){
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(owner, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		if(requiredSex != null)
			dialog.withSetupDialog(newDialog -> newDialog.requireSex(requiredSex, parentSexMessage(childId, requiredSex, model)));
		if(requiredSex != null)
			dialog.withFilter(IndividualSearchStrategy.KEY_SEX, I18N.t("enum.individual.sex." + requiredSex.name().toLowerCase(Locale.ROOT)))
				.withLockedSex(requiredSex.name().toLowerCase(Locale.ROOT));
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
