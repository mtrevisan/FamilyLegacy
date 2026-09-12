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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeContextHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeMutator;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Coordinates the creation of parent-child and partner relationships from
 * the ancestor tree UI.
 * <p>
 * The coordinator is responsible for:
 * <ul>
 *   <li>asking the user which FLEF relationship type to use for each
 *       affected pair, when more than one type is applicable;</li>
 *   <li>invoking the correct {@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeMutator} methods with the resolved
 *       types;</li>
 *   <li>creating a companion spouse relationship when a second parent is
 *       added to a couple that already has one parent.</li>
 * </ul>
 * The coordinator does not touch the model directly; all mutations go
 * through {@link io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeMutator}. Instances are immutable and not
 * thread-safe (they are meant to be used from the Swing Event Dispatch
 * Thread, like the rest of the UI layer).
 */
public final class RelationshipOperationCoordinator{

	private final FLEFModel model;
	private final TreeMutator mutator;
	private final String[] allowedTypes;


	/**
	 * Constructor.
	 *
	 * @param model        the FLEF model (must not be {@code null})
	 * @param mutator      the tree mutator (must not be {@code null})
	 * @param allowedTypes the relationship types permitted by the current
	 *                     tree type (must not be {@code null} or empty)
	 */
	public RelationshipOperationCoordinator(final FLEFModel model, final TreeMutator mutator, final String[] allowedTypes){
		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		if(mutator == null)
			throw new IllegalArgumentException("Mutator must not be null");
		if(allowedTypes == null || allowedTypes.length == 0)
			throw new IllegalArgumentException("Allowed types must not be empty");

		this.model = model;
		this.mutator = mutator;
		this.allowedTypes = allowedTypes.clone();
	}


	/**
	 * Associates an existing or newly-created individual to a set of
	 * parents. If both parents are present, the user may be prompted twice
	 * (once per parent) when more than one relationship type is allowed.
	 *
	 * @param parent the parent window for the type-selection dialog; may be {@code null}
	 * @param child  the child to link (must not be {@code null})
	 * @param father the father, or {@code null}
	 * @param mother the mother, or {@code null}
	 */
	public void performChildOperation(final Window parent, final FLEFRecord child, final FLEFRecord father,
		final FLEFRecord mother){
		if(child == null || father == null && mother == null)
			return;

		final List<String> selectedTypes = selectChildTypes(parent, father, mother);
		if(selectedTypes == null || selectedTypes.isEmpty())
			return;

		// At least one of father/mother is non-null, so the list is not
		// empty. The type is used only for the non-null parent(s).
		final String fatherType = (father != null? selectedTypes.getFirst(): null);
		final String motherType = (mother != null
			? selectedTypes.get(Math.min(1, selectedTypes.size() - 1))
			: null);
		final String fatherId = (father != null? father.getId(): null);
		final String motherId = (mother != null? mother.getId(): null);

		mutator.addChildToParents(fatherId, motherId, child, fatherType, motherType);
	}

	/**
	 * Associates an existing or newly-created individual as a parent to
	 * all children identified by the given context. If the context also
	 * carries an existing opposite parent, a companion spouse relationship
	 * is created between the two parents.
	 *
	 * @param parent     the parent window for the type-selection dialog; may be {@code null}
	 * @param individual the parent to link (must not be {@code null})
	 * @param ctx        the context describing which children to link (must not be {@code null} and must carry at least
	 *                   one child id)
	 */
	public void performParentOperation(final Window parent, final FLEFRecord individual,
		final TreeContextHelper.Context ctx){
		if(individual == null || ctx == null || ctx.childrenId == null || ctx.childrenId.isEmpty())
			return;

		final List<String> selectedTypes = selectParentTypes(parent, ctx.childrenId);
		if(selectedTypes == null || selectedTypes.isEmpty())
			return;

		mutator.addParentToChild(ctx.childrenId, individual, selectedTypes);
	}


	/* ======================================================================
	 *                          Type selection helpers
	 * ====================================================================== */

	private List<String> selectChildTypes(final Window parent, final FLEFRecord father, final FLEFRecord mother){
		if(allowedTypes.length == 1)
			return Collections.nCopies(2, allowedTypes[0]);

		final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>(2);
		final IndividualHandler handler = IndividualHandler.getInstance();

		final String fatherLabel = (father != null
			? handler.getDisplayText(father, model)
			: "Father");
		final String motherLabel = (mother != null
			? handler.getDisplayText(mother, model)
			: "Mother");

		items.add(new RelationshipTypeSelectionDialog.Item(fatherLabel, allowedTypes[0]));
		items.add(new RelationshipTypeSelectionDialog.Item(motherLabel, allowedTypes[0]));

		return RelationshipTypeSelectionDialog.showIfNeeded(parent, items, allowedTypes);
	}

	private List<String> selectParentTypes(final Window parent, final List<String> childrenIds){
		if(allowedTypes.length == 1)
			return Collections.nCopies(childrenIds.size(), allowedTypes[0]);

		final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>(childrenIds.size());
		final IndividualHandler handler = IndividualHandler.getInstance();
		for(final String childId : childrenIds){
			final FLEFRecord child = model.getRecordById(childId);
			if(child == null)
				continue;

			final String label = handler.getDisplayText(child, model);
			items.add(new RelationshipTypeSelectionDialog.Item(label, allowedTypes[0]));
		}
		if(items.isEmpty())
			return null;

		return RelationshipTypeSelectionDialog.showIfNeeded(parent, items, allowedTypes);
	}

}
