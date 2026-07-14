package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;


public class GUIHelper{

	private GUIHelper(){}


	public static void installBehaviour(final JComponent component,
			final Supplier<Boolean> hasSelection,
			final Runnable newAction,
			final Runnable addAction,
			final Runnable editAction,
			final Runnable clearAction,
			final Runnable notesAction){
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem newItem;
		if(newAction != null){
			newItem = new JMenuItem("Create New...");
			newItem.addActionListener(e -> newAction.run());
			popupMenu.add(newItem);
		}

		final JMenuItem addItem;
		if(addAction != null){
			addItem = new JMenuItem("Add Existing...");
			addItem.addActionListener(e -> addAction.run());
			popupMenu.add(addItem);
		}

		final JMenuItem editItem;
		if(editAction != null){
			editItem = new JMenuItem("Edit...");
			editItem.addActionListener(e -> editAction.run());
			popupMenu.add(editItem);

			editItem.setEnabled(false);
		}
		else
			editItem = null;

		final JMenuItem clearItem;
		if(clearAction != null){
			clearItem = new JMenuItem("Clear");
			clearItem.addActionListener(e -> clearAction.run());
			popupMenu.add(clearItem);

			clearItem.setEnabled(false);
		}
		else
			clearItem = null;

		final JMenuItem notesItem;
		if(notesAction != null){
			notesItem = new JMenuItem("Notes...");
			notesItem.addActionListener(e -> notesAction.run());
			popupMenu.addSeparator();
			popupMenu.add(notesItem);

			notesItem.setEnabled(false);
		}
		else
			notesItem = null;

		// Mouse handling
		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent me){
				if(me.getClickCount() == 2
						&& (!(component instanceof JList<?> list) || list.getSelectedIndex() >= 0)
						&& editAction != null)
					editAction.run();
			}

			@Override
			public void mousePressed(final MouseEvent me){
				processEvent(me);
			}

			@Override
			public void mouseReleased(final MouseEvent me){
				processEvent(me);
			}

			private void processEvent(final MouseEvent me){
				if(!component.isEnabled() || !me.isPopupTrigger())
					return;

				if(component instanceof JList<?> list){
					final int index = list.locationToIndex(me.getPoint());
					if(index >= 0 && !list.isSelectedIndex(index))
						list.setSelectedIndex(index);
				}

				updateMenuState(hasSelection, editItem, clearItem, notesItem);
				popupMenu.show(component, me.getX(), me.getY());
			}
		});

		// INSERT
		if(newAction != null)
			addAction(component, "new-item", KeyEvent.VK_INSERT, newAction);

		// DELETE
		if(clearAction != null && component instanceof JList<?> list)
			addAction(component, "delete-item", KeyEvent.VK_DELETE, () -> {
				if(list.getSelectedIndex() >= 0)
					clearAction.run();
			});
	}

	private static void updateMenuState(final Supplier<Boolean> hasSelection,
			final JMenuItem editItem, final JMenuItem deleteItem, final JMenuItem notesItem){
		final boolean enabled = hasSelection.get();
		if(editItem != null)
			editItem.setEnabled(enabled);
		if(deleteItem != null)
			deleteItem.setEnabled(enabled);
		if(notesItem != null)
			notesItem.setEnabled(enabled);
	}

	private static void addAction(final JComponent component, final String key, final int keyCode,
			final Runnable action){
		component.getInputMap().put(
			KeyStroke.getKeyStroke(keyCode, 0),
			key
		);

		component.getActionMap().put(
			key,
			new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					action.run();
				}
			}
		);
	}

}
