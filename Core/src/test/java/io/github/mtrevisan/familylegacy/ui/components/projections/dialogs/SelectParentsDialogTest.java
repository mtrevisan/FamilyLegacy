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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/** The dialog of a new child asks the type of the first parent and who the other parent is, with its type. */
class SelectParentsDialogTest{

	private static final String[] TYPES = RelationshipReader.INDIVIDUAL_TO_INDIVIDUAL_CHILD_TYPES;
	private static final RelationshipTypeSelectionDialog.Item PARENT = new RelationshipTypeSelectionDialog.Item("P", "Parent", TYPES[0]);
	private static final RelationshipTypeSelectionDialog.Item PARTNER_1 = new RelationshipTypeSelectionDialog.Item("A", "Partner A", TYPES[0]);
	private static final RelationshipTypeSelectionDialog.Item PARTNER_2 = new RelationshipTypeSelectionDialog.Item("B", "Partner B", TYPES[0]);


	private static RelationshipTypeSelectionDialog.ParentsChoice run(final Supplier<RelationshipTypeSelectionDialog.Item> picker,
			final Consumer<RelationshipTypeSelectionDialog> beforeOk){
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

		return RelationshipTypeSelectionDialog.selectParents(null, PARENT,
			new RelationshipTypeSelectionDialog.OtherParentRow(List.of(PARTNER_1, PARTNER_2), picker), TYPES, new FLEFModel());
	}

	@SuppressWarnings("unchecked")
	private static <T extends Component> T find(final Container root, final Class<T> type, final java.util.function.Predicate<T> filter){
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
	private static JComboBox<Object> otherParentCombo(final RelationshipTypeSelectionDialog dialog){
		return find(dialog.getContentPane(), JComboBox.class,
			c -> c.getItemCount() > 0 && I18N.t("dialog.other_parent.none").equals(c.getItemAt(c.getItemCount() - 1).toString()));
	}


	@Test
	void theFirstPartnerIsPreselected(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.ParentsChoice choice = run(() -> null, dialog -> {});

		Assertions.assertEquals("A", choice.otherParent().id());
		Assertions.assertEquals(TYPES[0], choice.otherType());
		Assertions.assertEquals(TYPES[0], choice.parentType());
	}

	@Test
	void nobodyLeavesTheOtherParentOut() throws Exception{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.ParentsChoice choice = run(() -> null, dialog -> {
			final JComboBox<Object> combo = otherParentCombo(dialog);
			combo.setSelectedIndex(combo.getItemCount() - 1);
		});

		Assertions.assertNull(choice.otherParent());
		Assertions.assertNull(choice.otherType());
	}

	@Test
	void anotherIndividualIsPickedAndKept(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.Item picked = new RelationshipTypeSelectionDialog.Item("Z", "Somebody", TYPES[0]);
		final RelationshipTypeSelectionDialog.ParentsChoice choice = run(() -> picked, dialog -> {
			final JComboBox<Object> combo = otherParentCombo(dialog);
			combo.setSelectedIndex(combo.getItemCount() - 2);
		});

		Assertions.assertEquals("Z", choice.otherParent().id());
	}

	@Test
	void cancellingThePickReturnsToThePreviousChoice(){
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

		final RelationshipTypeSelectionDialog.ParentsChoice choice = run(() -> null, dialog -> {
			final JComboBox<Object> combo = otherParentCombo(dialog);
			combo.setSelectedIndex(1);
			combo.setSelectedIndex(combo.getItemCount() - 2);
		});

		Assertions.assertEquals("B", choice.otherParent().id());
	}

}
