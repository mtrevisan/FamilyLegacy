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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.bookmarks;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.List;
import java.util.function.Consumer;


/**
 * Swing menu that lists the saved bookmarks and lets the user apply,
 * create, and manage them.
 * <p>
 * The menu is rebuilt every time it is selected, so it always reflects
 * the current state of the store.
 */
public final class BookmarkMenu extends JMenu{

	@Serial
	private static final long serialVersionUID = 5129384756102938475L;


	/** Callback that captures the current view as a bookmark. */
	public interface Capture{
		Bookmark capture(String name);
	}


	private final BookmarkStore store;
	private final Capture capture;
	private final Consumer<Bookmark> applier;
	private final Window owner;


	public BookmarkMenu(final Window owner, final BookmarkStore store, final Capture capture,
			final Consumer<Bookmark> applier){
		super("Bookmarks");

		this.owner = owner;
		this.store = store;
		this.capture = capture;
		this.applier = applier;

		addMenuListener(new MenuListener(){
			@Override
			public void menuSelected(final MenuEvent e){
				rebuild();
			}

			@Override
			public void menuDeselected(final MenuEvent e){}

			@Override
			public void menuCanceled(final MenuEvent e){}
		});

		rebuild();
	}


	private void rebuild(){
		removeAll();

		final JMenuItem save = new JMenuItem("Bookmark current view…");
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		save.addActionListener(this::onSave);
		add(save);

		addSeparator();

		final List<Bookmark> all = store.all();
		if(all.isEmpty()){
			final JMenuItem empty = new JMenuItem("(no bookmarks yet)");
			empty.setEnabled(false);
			add(empty);
		}
		else
			for(final Bookmark b : all){
				final JMenuItem item = new JMenuItem(b.name());
				item.setToolTipText(b.type().displayName() + " — " + b.rootId());
				item.addActionListener(e -> applier.accept(b));
				add(item);
			}

		addSeparator();

		final JMenuItem manage = new JMenuItem("Manage bookmarks…");
		manage.addActionListener(e -> new BookmarkManagerDialog(owner, store).setVisible(true));
		add(manage);
	}

	private void onSave(final ActionEvent e){
		final String name = JOptionPane.showInputDialog(this, "Bookmark name:",
			"Save bookmark", JOptionPane.PLAIN_MESSAGE);
		if(name == null || name.isBlank())
			return;

		final Bookmark b = capture.capture(name.trim());
		if(b == null){
			JOptionPane.showMessageDialog(this,
				"No view to bookmark. Load an individual or a group first.",
				"Nothing to bookmark", JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		store.add(b);
	}

}
