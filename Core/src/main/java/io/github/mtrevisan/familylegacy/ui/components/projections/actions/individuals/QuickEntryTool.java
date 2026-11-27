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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quickentry.FamilyQuickEntry;
import io.github.mtrevisan.familylegacy.tree.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.quickentry.FamilyQuickEntryDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import io.github.mtrevisan.familylegacy.ui.helpers.StatusMessages;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Opens the quick entry of a family: a person (the selected individual, or a new one), the partner, the union and the children, in one
 * window. The people, their births and the union event are created, then the relationships that link them through the view, so that it
 * is up to date.
 */
public final class QuickEntryTool implements ToolOperation{

	@Override
	public String getName(){
		return ShortcutRegistry.INDIVIDUAL_QUICK_ENTRY.action();
	}

	@Override
	public KeyStroke getAccelerator(){
		return ShortcutRegistry.INDIVIDUAL_QUICK_ENTRY.keyStroke();
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		final String selectedId = (context.hasSelectedIndividual()? context.selectedEntityId(): null);
		final FLEFRecord selected = (selectedId != null? model.getRecordById(selectedId): null);
		final FamilyQuickEntry.Input input = FamilyQuickEntryDialog.show(context.owner(), model, selected);
		if(input == null)
			return;

		final FamilyQuickEntry.Created created;
		try{
			created = FamilyQuickEntry.create(model, input);
		}
		catch(final IllegalArgumentException iae){
			MessageDialogs.show(context.owner(), iae.getMessage(), I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

			return;
		}
		if(created == null)
			return;

		//the relationships are created through the view, which keeps what it needs to know about them
		final ProjectionMutator mutator = context.mutator();
		if(mutator != null){
			for(final FamilyQuickEntry.Relationship relationship : created.relationships())
				mutator.createRelationship(relationship.subjectId(), relationship.targetId(), relationship.type());
			mutator.invalidateAndNotifyTreeChanged(created.firstId());
		}
		else
			FamilyQuickEntry.addRelationships(model, created.relationships());

		//a new family is shown, an existing individual stays where it is
		if(selected == null)
			context.loadRoot(created.firstId());

		StatusMessages.shared()
			.show(summary(model, created));
	}

	/**
	 * @param model	The model that the records were added to.
	 * @param created	What was created.
	 * @return	What the quick entry created in words (the people, the events, the places, the relationships), and that Undo takes it back.
	 */
	static String summary(final FLEFModel model, final FamilyQuickEntry.Created created){
		int people = 0;
		int events = 0;
		int places = 0;
		for(final String id : created.recordIds()){
			final FLEFRecord record = model.getRecordById(id);
			if(record == null || record.getTag() == null)
				continue;

			switch(record.getTag().toLowerCase(Locale.ROOT)){
				case "individual" -> people ++;
				case "event" -> events ++;
				case "place" -> places ++;
				default -> {}
			}
		}

		final List<String> parts = new ArrayList<>(4);
		if(people > 0)
			parts.add(I18N.tf("message.quick_entry.count.people", people));
		if(events > 0)
			parts.add(I18N.tf("message.quick_entry.count.events", events));
		if(places > 0)
			parts.add(I18N.tf("message.quick_entry.count.places", places));
		if(!created.relationships().isEmpty())
			parts.add(I18N.tf("message.quick_entry.count.relationships", created.relationships().size()));
		return I18N.tf("message.quick_entry.created", String.join(", ", parts), ShortcutRegistry.EDIT_UNDO.displayKeys());
	}

}
