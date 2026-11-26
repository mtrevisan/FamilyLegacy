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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ProjectInfo;
import io.github.mtrevisan.familylegacy.util.PluginLoader;
import io.github.mtrevisan.updater.Component;
import io.github.mtrevisan.updater.ComponentManager;
import io.github.mtrevisan.updater.InstallerDownloader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;


/**
 * Help &gt; Optional Components: the fonts and the plugins that the program does not carry and that the catalog (a file kept in the
 * repository of the application, see {@link io.github.mtrevisan.updater.ComponentCatalog}) lists. The user ticks the ones to install; they
 * are downloaded, checked against their SHA-256 and put in the {@code fonts} or {@code plugins} folder. A component is added to the list
 * by changing the catalog, not the application.
 * <p>
 * The network work is done off the event thread; the downloads are shown, one at a time, in a dialog that Escape cancels.
 * </p>
 */
public final class ComponentsDialog extends JDialog{

	private static final int NAME = 1;
	private static final int DESCRIPTION = 2;
	private static final int SIZE = 3;
	private static final int STATE = 4;
	private static final double MEGABYTE = 1024. * 1024.;


	private final ComponentManager manager;
	private final EntriesModel model = new EntriesModel();
	private final JLabel status = new JLabel(" ");
	private final JButton install = new JButton(I18N.t("dialog.components.install"));
	private final JButton uninstall = new JButton(I18N.t("dialog.components.uninstall"));
	private final JButton refresh = new JButton(I18N.t("dialog.components.refresh"));


	private ComponentsDialog(final Window owner){
		this(owner, ComponentManager.application(directories()));
	}

	ComponentsDialog(final Window owner, final ComponentManager manager){
		super(owner, I18N.t("dialog.components.title"), ModalityType.APPLICATION_MODAL);

		this.manager = manager;

		GUIHelper.installEscapeToClose(this);

		final JTable table = new SortableTable(model);
		table.setRowHeight(table.getRowHeight() + 6);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		//the size is shown as text ("1.5 MB"): it is sorted as a number
		((TableRowSorter<?>)table.getRowSorter()).setComparator(SIZE,
			Comparator.comparingDouble(value -> NumberUtils.toDouble(StringUtils.substringBefore((String)value, " "), -1.)));
		table.getTableHeader().setReorderingAllowed(false);
		column(table, 0, 40);
		column(table, NAME, 220);
		column(table, DESCRIPTION, 380);
		column(table, SIZE, 70);
		column(table, STATE, 130);
		model.addTableModelListener(e -> updateButtons());

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(800, 300));

		//(a fixed width makes the text wrap: otherwise the label is as wide as its longest line, and so is the window)
		final JLabel intro = new JLabel("<html><body style='width: 760px'>" + I18N.t("dialog.components.intro") + "</body></html>");
		final JPanel north = new JPanel(new BorderLayout());
		north.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
		north.add(intro, BorderLayout.CENTER);

		install.setEnabled(false);
		uninstall.setEnabled(false);
		install.addActionListener(e -> installSelected());
		uninstall.addActionListener(e -> uninstallSelected());
		refresh.addActionListener(e -> reload());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(install);
		buttons.add(uninstall);
		buttons.add(refresh);
		final JPanel south = new JPanel(new BorderLayout());
		south.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		south.add(status, BorderLayout.CENTER);
		south.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout(0, 8));
		add(north, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}

	/** The name in the language of the interface if the application has it (key {@code component.<target>.<id>.name}), else the catalog's. */
	static String nameOf(final Component component){
		return I18N.t("component." + component.target() + "." + component.id() + ".name", component.name(Locale.getDefault()));
	}

	/** The description in the language of the interface if the application has it (key {@code component.<target>.<id>.description}), else the catalog's. */
	static String descriptionOf(final Component component){
		return I18N.t("component." + component.target() + "." + component.id() + ".description", component.description(Locale.getDefault()));
	}

	private static void column(final JTable table, final int index, final int width){
		final TableColumn column = table.getColumnModel().getColumn(index);
		column.setPreferredWidth(width);
		if(index == 0 || index == SIZE)
			column.setMaxWidth(width);
	}

	/** Shows the dialog; it returns when the dialog is closed. */
	public static void show(final Window owner){
		final ComponentsDialog dialog = new ComponentsDialog(owner);
		dialog.reload();
		dialog.setVisible(true);
	}

	/** @return	The folder of each target of the components. */
	public static Map<String, Path> directories(){
		final Map<String, Path> directories = new HashMap<>();
		directories.put("fonts", PluginLoader.fontsDirectory());
		directories.put("plugins", PluginLoader.pluginsDirectory());
		return directories;
	}

	/** @return	Whether the fonts folder has fonts in it (installed by the user, or downloaded). */
	public static boolean hasFonts(){
		final Path fonts = PluginLoader.fontsDirectory();
		if(fonts == null || !Files.isDirectory(fonts))
			return false;

		try(final Stream<Path> files = Files.list(fonts)){
			return files.anyMatch(file -> {
				final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
				return (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc"));
			});
		}
		catch(final IOException ignored){
			return false;
		}
	}


	/** Reads the catalog and what is installed, in the background. */
	void reload(){
		status.setText(I18N.t("dialog.components.loading"));
		install.setEnabled(false);
		uninstall.setEnabled(false);
		refresh.setEnabled(false);
		final String version = ProjectInfo.getAppVersion();
		new SwingWorker<List<ComponentManager.Entry>, Void>(){
			@Override
			protected List<ComponentManager.Entry> doInBackground() throws IOException{
				return manager.list(version);
			}

			@Override
			protected void done(){
				refresh.setEnabled(true);
				try{
					model.set(get());
					status.setText(model.getRowCount() == 0? I18N.t("dialog.components.empty"): " ");
				}
				catch(final InterruptedException e){
					Thread.currentThread()
						.interrupt();
				}
				catch(final ExecutionException e){
					model.set(List.of());
					final Throwable cause = (e.getCause() != null? e.getCause(): e);
					status.setText("<html>" + I18N.tf("dialog.components.failed",
						StringUtils.replaceEach(String.valueOf(cause.getMessage()), new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"})) + "</html>");
				}
			}
		}.execute();
	}

	private void updateButtons(){
		install.setEnabled(!model.selectedToInstall().isEmpty());
		uninstall.setEnabled(!model.selectedToUninstall().isEmpty());
	}

	/** Removes the ticked components, and the installed ones that need them, after asking; then reads the state again. */
	private void uninstallSelected(){
		final List<ComponentManager.Entry> selected = model.withDependents(model.selectedToUninstall());
		final List<String> names = new ArrayList<>();
		for(final ComponentManager.Entry entry : selected)
			names.add(nameOf(entry.component()));
		if(selected.isEmpty() || MessageDialogs.confirm(this, I18N.tf("dialog.components.uninstall.confirm", String.join(", ", names)),
				I18N.t("dialog.components.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.OK_OPTION)
			return;

		final List<String> removed = new ArrayList<>();
		final List<String> failures = new ArrayList<>();
		for(final ComponentManager.Entry entry : selected){
			final String name = nameOf(entry.component());
			try{
				manager.uninstall(entry);
				removed.add(name);
			}
			catch(final IOException e){
				failures.add(I18N.tf("dialog.components.uninstall.error", name, e.getMessage()));
			}
		}

		reload();
		final StringBuilder message = new StringBuilder();
		if(!removed.isEmpty())
			message.append(I18N.tf("dialog.components.removed", String.join(", ", removed)))
				.append("\n\n").append(I18N.t("dialog.components.restart_removed"));
		for(final String failure : failures)
			message.append("\n\n").append(failure);
		if(!message.isEmpty())
			MessageDialogs.show(this, message.toString().strip(), I18N.t("dialog.components.title"),
				(failures.isEmpty()? JOptionPane.INFORMATION_MESSAGE: JOptionPane.ERROR_MESSAGE));
	}

	/** Installs the ticked components (and the ones they need) one after the other, each with its own progress dialog, then reads the state again. */
	private void installSelected(){
		final List<ComponentManager.Entry> selected = model.withRequirements(model.selectedToInstall());
		final List<String> installed = new ArrayList<>();
		final List<String> failures = new ArrayList<>();
		boolean restart = false;
		for(final ComponentManager.Entry entry : selected){
			final String name = nameOf(entry.component());
			try{
				if(!download(entry, name))
					break;

				installed.add(name);
				restart |= "plugins".equals(entry.component().target());
			}
			catch(final IOException e){
				failures.add(I18N.tf("dialog.components.error", name, e.getMessage()));
			}
		}

		reload();
		final StringBuilder message = new StringBuilder();
		if(!installed.isEmpty())
			message.append(I18N.tf("dialog.components.done", String.join(", ", installed)));
		if(restart)
			message.append("\n\n").append(I18N.t("dialog.components.restart"));
		for(final String failure : failures)
			message.append("\n\n").append(failure);
		if(!message.isEmpty())
			MessageDialogs.show(this, message.toString().strip(), I18N.t("dialog.components.title"),
				(failures.isEmpty()? JOptionPane.INFORMATION_MESSAGE: JOptionPane.ERROR_MESSAGE));
	}

	/**
	 * @return	Whether it was installed ({@code false} if the user canceled).
	 * @throws IOException	If it cannot be installed.
	 */
	private boolean download(final ComponentManager.Entry entry, final String name) throws IOException{
		final AtomicBoolean cancelled = new AtomicBoolean();
		final UpdateDownloadDialog progress = new UpdateDownloadDialog(this, I18N.t("dialog.components.title"),
			I18N.tf("dialog.components.downloading", name), () -> cancelled.set(true));
		final SwingWorker<Void, Void> worker = new SwingWorker<>(){
			@Override
			protected Void doInBackground() throws IOException{
				manager.install(entry, progress::progress, cancelled::get);
				return null;
			}

			@Override
			protected void done(){
				progress.close();
			}
		};
		worker.execute();
		//(modal: it returns once the worker has closed it)
		progress.setVisible(true);

		try{
			worker.get();
			return true;
		}
		catch(final InterruptedException e){
			Thread.currentThread()
				.interrupt();
			return false;
		}
		catch(final ExecutionException e){
			final Throwable cause = (e.getCause() != null? e.getCause(): e);
			if(cause instanceof InstallerDownloader.DownloadCancelledException)
				return false;
			if(cause instanceof final IOException io)
				throw io;
			throw new IOException(cause.getMessage(), cause);
		}
	}


	private static final class EntriesModel extends AbstractTableModel{

		private final List<ComponentManager.Entry> entries = new ArrayList<>();
		private boolean[] ticked = new boolean[0];


		void set(final List<ComponentManager.Entry> newEntries){
			entries.clear();
			entries.addAll(newEntries);
			ticked = new boolean[entries.size()];
			fireTableDataChanged();
		}

		List<ComponentManager.Entry> selectedToInstall(){
			final List<ComponentManager.Entry> selected = new ArrayList<>();
			for(int i = 0; i < entries.size(); i ++)
				if(ticked[i] && entries.get(i).canInstall())
					selected.add(entries.get(i));
			return selected;
		}

		List<ComponentManager.Entry> selectedToUninstall(){
			final List<ComponentManager.Entry> selected = new ArrayList<>();
			for(int i = 0; i < entries.size(); i ++)
				if(ticked[i] && entries.get(i).canUninstall())
					selected.add(entries.get(i));
			return selected;
		}

		/** @return	The entries, each one preceded by the ones it requires that are not installed yet (without duplicates). */
		List<ComponentManager.Entry> withRequirements(final List<ComponentManager.Entry> selected){
			final List<ComponentManager.Entry> result = new ArrayList<>();
			for(final ComponentManager.Entry entry : selected)
				addWithRequirements(entry, result, 0);
			return result;
		}

		private void addWithRequirements(final ComponentManager.Entry entry, final List<ComponentManager.Entry> result, final int depth){
			if(result.contains(entry) || depth > entries.size())
				return;

			for(final String id : entry.component().requires())
				for(final ComponentManager.Entry other : entries)
					if(other.component().id().equals(id) && other.component().target().equals(entry.component().target()) && other.canInstall())
						addWithRequirements(other, result, depth + 1);
			result.add(entry);
		}

		/** @return	The entries, each one preceded by the installed ones that require it (without duplicates). */
		List<ComponentManager.Entry> withDependents(final List<ComponentManager.Entry> selected){
			final List<ComponentManager.Entry> result = new ArrayList<>();
			for(final ComponentManager.Entry entry : selected)
				addWithDependents(entry, result, 0);
			return result;
		}

		private void addWithDependents(final ComponentManager.Entry entry, final List<ComponentManager.Entry> result, final int depth){
			if(result.contains(entry) || depth > entries.size())
				return;

			for(final ComponentManager.Entry other : entries)
				if(other.component().requires().contains(entry.component().id()) && other.component().target().equals(entry.component().target())
						&& other.canUninstall())
					addWithDependents(other, result, depth + 1);
			result.add(entry);
		}

		@Override
		public int getRowCount(){
			return entries.size();
		}

		@Override
		public int getColumnCount(){
			return 5;
		}

		@Override
		public String getColumnName(final int column){
			return switch(column){
				case 0 -> "";
				case NAME -> I18N.t("dialog.components.column.name");
				case DESCRIPTION -> I18N.t("dialog.components.column.description");
				case SIZE -> I18N.t("dialog.components.column.size");
				default -> I18N.t("dialog.components.column.state");
			};
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == 0? Boolean.class: String.class);
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return (column == 0 && (entries.get(row).canInstall() || entries.get(row).canUninstall()));
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final ComponentManager.Entry entry = entries.get(row);
			return switch(column){
				case 0 -> ticked[row];
				case NAME -> nameOf(entry.component());
				case DESCRIPTION -> descriptionOf(entry.component());
				case SIZE -> (entry.asset() != null && entry.asset().size() >= 0?
					String.format(Locale.ROOT, "%.1f MB", entry.asset().size() / MEGABYTE): "");
				default -> I18N.t("dialog.components.state." + entry.state().name().toLowerCase(Locale.ROOT));
			};
		}

		@Override
		public void setValueAt(final Object value, final int row, final int column){
			if(column == 0){
				ticked[row] = Boolean.TRUE.equals(value);
				fireTableCellUpdated(row, column);
			}
		}

	}

}
