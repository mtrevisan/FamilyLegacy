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
package io.github.mtrevisan.familylegacy.ui.components.projections.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.quickentry.QuickEntryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;


/** The dialog of a new partner asks the type of the relationship and, for the unions that began with an event, when and where. */
class UnionDialogTest{

	private static final String[] TYPES = RelationshipReader.PARTNER_TYPES.toArray(String[]::new);
	private static final RelationshipTypeSelectionDialog.Item PARTNER = new RelationshipTypeSelectionDialog.Item("P", "Partner", "spouse");


	private static RelationshipTypeSelectionDialog.UnionChoice run(final Consumer<RelationshipTypeSelectionDialog> beforeOk){
		//polls until the dialog is on screen; whatever goes wrong, the dialog is disposed so that the test never hangs
		final Timer timer = new Timer(100, null);
		timer.addActionListener(e -> {
			for(final Window window : Window.getWindows())
				if(window instanceof RelationshipTypeSelectionDialog dialog && dialog.isShowing()){
					timer.stop();
					try{
						beforeOk.accept(dialog);
						find(dialog, JButton.class, b -> I18N.t("button.ok").equals(b.getText())).doClick();
					}
					finally{
						dialog.dispose();
					}
				}
		});
		timer.start();
		final Timer watchdog = new Timer(15000, e -> {
			timer.stop();
			for(final Window window : Window.getWindows())
				if(window instanceof RelationshipTypeSelectionDialog)
					window.dispose();
		});
		watchdog.setRepeats(false);
		watchdog.start();

		return RelationshipTypeSelectionDialog.selectUnion(null, PARTNER, TYPES, new FLEFModel());
	}

	@SuppressWarnings("unchecked")
	private static <T extends Component> T find(final Container root, final Class<T> type, final Predicate<T> filter){
		for(final Component child : root.getComponents()){
			if(type.isInstance(child) && filter.test((T)child))
				return (T)child;
			if(child instanceof Container container){
				final T found = find(container, type, filter);
				if(found != null)
					return found;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static JComboBox<Object> typeCombo(final RelationshipTypeSelectionDialog dialog){
		return find(dialog.getContentPane(), JComboBox.class, c -> c.getItemCount() == TYPES.length);
	}

	private static List<JTextField> fields(final RelationshipTypeSelectionDialog dialog){
		final List<JTextField> fields = new ArrayList<>();
		collect(dialog.getContentPane(), fields);
		return fields;
	}

	private static void collect(final Container container, final List<JTextField> fields){
		for(final Component component : container.getComponents()){
			if(component instanceof JTextField field)
				fields.add(field);
			if(component instanceof Container child)
				collect(child, fields);
		}
	}


	@Test
	void aMarriageHasItsDateAndItsPlace(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.UnionChoice choice = run(dialog -> {
			fields(dialog).get(0).setText("5 May 1875");
			fields(dialog).get(1).setText("Padova");
		});

		Assertions.assertEquals("spouse", choice.type());
		Assertions.assertNotNull(choice.fact().date());
		Assertions.assertEquals("Padova", choice.fact().place().newName());
	}

	@Test
	void nothingTypedIsNoFact(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.UnionChoice choice = run(dialog -> {});

		Assertions.assertEquals("spouse", choice.type());
		Assertions.assertTrue(choice.fact().isEmpty());
	}

	@Test
	void aTypeThatBeganWithNoEventHasNoDateToAsk(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.UnionChoice choice = run(dialog -> {
			fields(dialog).get(0).setText("1875");
			typeCombo(dialog).setSelectedItem(RelationshipReader.ENUM_TYPE_COHABITING_PARTNER);

			Assertions.assertFalse(fields(dialog).get(0).isEnabled());
			Assertions.assertFalse(fields(dialog).get(1).isEnabled());
		});

		Assertions.assertEquals(RelationshipReader.ENUM_TYPE_COHABITING_PARTNER, choice.type());
		Assertions.assertTrue(choice.fact().isEmpty());
		Assertions.assertNull(QuickEntryService.unionEventType(choice.type()));
	}

	@Test
	void aTypeThatBeginsWithAnEventAsksForItAgain(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.UnionChoice choice = run(dialog -> {
			typeCombo(dialog).setSelectedItem(RelationshipReader.ENUM_TYPE_COHABITING_PARTNER);
			typeCombo(dialog).setSelectedItem(RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER);

			Assertions.assertTrue(fields(dialog).get(0).isEnabled());
			fields(dialog).get(0).setText("1874");
		});

		Assertions.assertEquals(RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER, choice.type());
		Assertions.assertNotNull(choice.fact().date());
	}

}
