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
package io.github.mtrevisan.familylegacy.ui.components.projections.bookmarks;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Swing menu that lists the saved bookmarks and lets the user apply,
 * create, and manage them.
 * <p>
 * The menu is rebuilt every time it is selected, so it always reflects
 * the current state of the store.
 */
public final class BookmarkMenu extends JMenu{

	/** Callback that captures the current view as a bookmark. */
	public interface Capture{
		Bookmark capture(String name);
	}


	private final BookmarkStore store;
	private final Capture capture;
	private final Consumer<Bookmark> applier;
	private final Window owner;
	private final Supplier<FLEFModel> modelSupplier;
	private final Supplier<String> filePathSupplier;


	/**
	 * @param filePathSupplier	Gives the path of the file of the document, that the bookmarks listed are tied to ({@code null} if it has none).
	 */
	public BookmarkMenu(final Window owner, final BookmarkStore store, final Supplier<FLEFModel> modelSupplier,
			final Supplier<String> filePathSupplier, final Capture capture, final Consumer<Bookmark> applier){
		super(I18N.t("menu.bookmarks"));

		this.owner = owner;
		this.store = store;
		this.modelSupplier = modelSupplier;
		this.filePathSupplier = filePathSupplier;
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

		final JMenuItem save = new JMenuItem(I18N.t("menu.item.bookmark_current_view"));
		save.setAccelerator(ShortcutRegistry.BOOKMARK_CURRENT_VIEW.keyStroke());
		save.addActionListener(this::onSave);
		add(save);

		addSeparator();

		//the bookmarks of the records hidden by the privacy policy are not listed
		final FLEFModel model = modelSupplier.get();
		final List<Bookmark> all = store.visible(model, filePathSupplier.get());
		if(all.isEmpty()){
			final JMenuItem empty = new JMenuItem(I18N.t("menu.item.no_bookmarks"));
			empty.setEnabled(false);
			add(empty);
		}
		else
			for(final Bookmark b : all){
				final JMenuItem item = new JMenuItem(b.name());
				item.setToolTipText(b.type().displayName() + " — " + b.rootLabel(model));
				item.addActionListener(e -> applier.accept(b));
				add(item);
			}

		addSeparator();

		final JMenuItem manage = new JMenuItem(I18N.t("menu.item.manage_bookmarks"));
		manage.addActionListener(e -> new BookmarkManagerDialog(owner, store, modelSupplier, filePathSupplier).setVisible(true));
		add(manage);
	}

	private void onSave(final ActionEvent e){
		if(filePathSupplier.get() == null){
			JOptionPane.showMessageDialog(this, I18N.t("dialog.bookmark.no_file.message"), I18N.t("dialog.bookmark.empty.title"),
				JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		final String name = JOptionPane.showInputDialog(this, I18N.l("dialog.bookmark.save.prompt"),
			I18N.t("dialog.bookmark.save.title"), JOptionPane.PLAIN_MESSAGE);
		//a name of spaces is no name
		if(StringUtils.isBlank(name))
			return;

		final String trimmed = name.trim();
		final Bookmark b = capture.capture(trimmed);
		if(b == null){
			JOptionPane.showMessageDialog(this,
				I18N.t("dialog.bookmark.empty.message"), I18N.t("dialog.bookmark.empty.title"), JOptionPane.INFORMATION_MESSAGE);

			return;
		}

		//two bookmarks with one name could not be told apart in the menu
		final Bookmark same = store.visible(modelSupplier.get(), filePathSupplier.get()).stream()
			.filter(existing -> existing.name().equalsIgnoreCase(trimmed))
			.findFirst()
			.orElse(null);
		if(same != null){
			final int replace = JOptionPane.showConfirmDialog(this, I18N.tf("dialog.bookmark.replace.message", same.name()),
				I18N.t("dialog.bookmark.replace.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(replace != JOptionPane.YES_OPTION)
				return;

			store.remove(same.id());
		}

		store.add(b);
	}

}
