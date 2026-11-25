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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.function.IntConsumer;
import java.util.function.Supplier;


/**
 * Small helpers shared by the tool dialogs.
 * <p>
 * The class exists so that the escape-to-close behaviour is defined in
 * one place and applied uniformly to every dialog in the {@code tools}
 * packages. Without it, each dialog would carry its own copy of the
 * binding, and a change to the behaviour would have to be replicated
 * everywhere.
 */
public final class ToolDialogs{

	private static final String ACTION_CLOSE = "closeDialog";


	/** Color of the verdict line when there is nothing to fix. */
	public static final Color OK_COLOR = new Color(0x1A7F1A);
	/** Color of the verdict line when there is something to look at. */
	public static final Color WARN_COLOR = new Color(0xB06A00);
	/** Color of the verdict line (and of the cells) that flag an error. */
	public static final Color ERROR_COLOR = new Color(0xB00020);


	private ToolDialogs(){}


	/**
	 * Creates a read-only table with the look shared by the tool dialogs (row height, single selection).
	 *
	 * @param model	The model of the table.
	 * @param onOpen	What to do when a row is double-clicked, given the model index of the row; {@code null} for none.
	 */
	public static JTable createTable(final RowTableModel model, final IntConsumer onOpen){
		final JTable table = new SortableTable(model);
		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoCreateRowSorter(true);
		if(onOpen != null)
			table.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.getSelectedRow() >= 0)
						onOpen.accept(table.convertRowIndexToModel(table.getSelectedRow()));
				}
			});
		return table;
	}

	/**
	 * Makes a column of the table whose cells are texts that start with a number ("12.5 %", "1,234") sort by that number and not as
	 * text; a cell that does not start with a number comes before all the others.
	 * <p>
	 * To be called after the model is set, as the row sorter belongs to the model (a table whose model is replaced creates a new one).
	 *
	 * @param table	The table, with a {@link TableRowSorter}.
	 * @param column	The index of the column in the model.
	 */
	public static void sortColumnAsNumber(final JTable table, final int column){
		final Comparator<Object> byNumber = Comparator.comparingDouble(value -> {
			final String text = StringUtils.remove(String.valueOf(value), ',');
			final int end = StringUtils.indexOfAnyBut(text, "0123456789.-");
			final String number = (end >= 0? text.substring(0, end): text);
			try{
				return Double.parseDouble(number);
			}
			catch(final NumberFormatException ignored){
				return Double.NEGATIVE_INFINITY;
			}
		});
		((TableRowSorter<?>)table.getRowSorter()).setComparator(column, byNumber);
	}

	/**
	 * Creates a tab made of a one-line verdict (green if there is nothing to fix, orange otherwise) and, if there is something
	 * to show, the table.
	 */
	public static JPanel createVerdictTab(final boolean ok, final String okMessage, final String warnMessage, final JTable table){
		final JPanel panel = createVerdictPanel(ok, ok? okMessage: warnMessage);
		if(!ok)
			panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

	/** Creates a panel whose north is a one-line verdict colored as OK or as a warning; the caller adds the content. */
	public static JPanel createVerdictPanel(final boolean ok, final String message){
		return createHeaderPanel(message, (ok? OK_COLOR: WARN_COLOR));
	}

	/**
	 * Creates a panel whose north is a one-line message in the given color ({@code null} for the default one); the caller adds the
	 * content. Use it when the message is neither good nor bad news.
	 */
	public static JPanel createHeaderPanel(final String message, final Color color){
		final JLabel verdict = new JLabel(message);
		if(color != null)
			verdict.setForeground(color);

		final JPanel header = new JPanel(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		header.add(verdict, BorderLayout.WEST);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		return panel;
	}

	/** Creates the footer of a tool dialog: a summary on the left, the buttons on the right. */
	public static JPanel createFooter(final String summary, final JButton... buttons){
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		for(final JButton button : buttons)
			buttonPanel.add(button);

		final JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 6));
		footer.add(new JLabel(summary), BorderLayout.WEST);
		footer.add(buttonPanel, BorderLayout.EAST);
		return footer;
	}

	/** Creates the "Copy all" button, that puts the given text (computed when the button is pressed) in the clipboard. */
	public static JButton createCopyButton(final Supplier<String> text){
		final JButton copy = new JButton(I18N.t("button.copy_all"));
		copy.addActionListener(e -> {
			final StringSelection selection = new StringSelection(text.get());
			Toolkit.getDefaultToolkit()
				.getSystemClipboard()
				.setContents(selection, selection);
		});
		return copy;
	}

	/**
	 * Creates the "Copy all" button of a table, enabled only while the model of the table has at least one row (also when the model is
	 * replaced or its rows change).
	 */
	public static JButton createCopyButton(final Supplier<String> text, final JTable table){
		final JButton copy = createCopyButton(text);
		final TableModelListener listener = e -> copy.setEnabled(table.getModel().getRowCount() > 0);
		table.getModel().addTableModelListener(listener);
		table.addPropertyChangeListener("model", e -> {
			((TableModel)e.getOldValue()).removeTableModelListener(listener);
			((TableModel)e.getNewValue()).addTableModelListener(listener);
			listener.tableChanged(null);
		});
		listener.tableChanged(null);
		return copy;
	}

	/** Creates the "Copy all" button of a text area, enabled only while the text is not blank. */
	public static JButton createCopyButton(final JTextComponent component){
		final JButton copy = createCopyButton(component::getText);
		final Runnable update = () -> copy.setEnabled(!component.getText().isBlank());
		component.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				update.run();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				update.run();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				update.run();
			}
		});
		update.run();
		return copy;
	}

	/** Opens the edit dialog of the record, with the handler of its type. */
	public static void openRecordEditor(final JDialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			return;

		final String tag = record.getTag();
		final RecordTypeHandler<?> handler = (tag != null? HandlerRegistry.getHandler(tag): null);
		if(handler == null){
			MessageDialogs.show(parent, I18N.tf("message.no_editor_for_type", tag), parent.getTitle(),
				JOptionPane.WARNING_MESSAGE);

			return;
		}

		final RecordDialog dialog = handler.createEditDialog(parent, model, record);
		dialog.setVisible(true);
	}


	/**
	 * Installs an escape-to-close binding on the given dialog.
	 * <p>
	 * The binding is registered on the dialog's root pane with
	 * {@link JComponent#WHEN_IN_FOCUSED_WINDOW}, so it fires regardless
	 * of which child component has the focus. When the user presses
	 * escape, the dialog is disposed exactly as if the Cancel button had
	 * been clicked: any unsaved change is discarded, and the code that
	 * opened the dialog resumes as if the user had cancelled.
	 * <p>
	 * The call is idempotent: calling it twice on the same dialog
	 * replaces the binding with an equivalent one, without side effects.
	 *
	 * @param dialog the dialog to install the binding on; must not be
	 *               {@code null}
	 */
	public static void installEscapeToClose(final JDialog dialog){
		GUIHelper.installEscapeToClose(dialog);
	}

}
