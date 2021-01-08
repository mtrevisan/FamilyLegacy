/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.validators.PhoneNumberValidator;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class ContactDialog extends JDialog implements ActionListener{

	private static final long serialVersionUID = 942804701975315042L;

	private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
	private static final UrlValidator URL_VALIDATOR = UrlValidator.getInstance();

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int TABLE_INDEX_CONTACT_ID = 0;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable contactsTable = new JTable(new ContactTableModel());
	private final JScrollPane contactsScrollPane = new JScrollPane(contactsTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel contactLabel = new JLabel("Contact");
	private final JTextField contactField = new JTextField();
	private final JMenuItem callItem = new JMenuItem("Call phone");
	private final JMenuItem sendEmailItem = new JMenuItem("Send email");
	private final JMenuItem testLinkItem = new JMenuItem("Test link");
	private final JMenuItem openLinkItem = new JMenuItem("Open link…");
	private final JLabel descriptionLabel = new JLabel("Description:");
	private final JTextField descriptionField = new JTextField();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton notesButton = new JButton("Notes");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<ContactDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private volatile boolean updating;
	private int dataHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public ContactDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Contacts");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(ContactDialog.this);
			}
		});

		contactsTable.setAutoCreateRowSorter(true);
		contactsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		contactsTable.setGridColor(GRID_COLOR);
		contactsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		contactsTable.setDragEnabled(true);
		contactsTable.setDropMode(DropMode.INSERT_ROWS);
		contactsTable.setTransferHandler(new TableTransferHandle(contactsTable));
		contactsTable.getTableHeader().setFont(contactsTable.getFont().deriveFont(Font.BOLD));
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(contactsTable.getModel());
		sorter.setComparator(TABLE_INDEX_CONTACT_ID, Comparator.naturalOrder());
		contactsTable.setRowSorter(sorter);
		//clicking on a line links it to current contact
		contactsTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = contactsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(contactsTable.convertRowIndexToModel(selectedRow));
		});
		contactsTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		contactsTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}
			});
		contactsTable.setPreferredScrollableViewportSize(new Dimension(contactsTable.getPreferredSize().width,
			contactsTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> addAction());

		contactLabel.setLabelFor(contactField);
		contactField.setEnabled(false);
		contactField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void changedUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}

			@Override
			public void removeUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}

			@Override
			public void insertUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}
		});
		//manage links
		attachOpenLinkPopUpMenu(contactField);

		descriptionLabel.setLabelFor(descriptionField);
		descriptionField.setEnabled(false);
		descriptionField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void changedUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}

			@Override
			public void removeUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}

			@Override
			public void insertUpdate(final DocumentEvent evt){
				updateContactFieldMenuItems();
			}
		});
		JavaHelper.addUndoCapability(descriptionField);

		notesButton.setEnabled(false);
		notesButton.addActionListener(evt -> {
			final int selectedRow = contactsTable.convertRowIndexToModel(contactsTable.getSelectedRow());
			final List<GedcomNode> contacts = store.traverseAsList(container, "CONTACT[]");
			final GedcomNode selectedContact = contacts.get(selectedRow);

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedContact));
		});

		restrictionCheckBox.setEnabled(false);
		restrictionCheckBox.addActionListener(evt -> updateContactFieldMenuItems());

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced contacts!

			dispose();
		});
		getRootPane().registerKeyboardAction(this, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		cancelButton.addActionListener(this::actionPerformed);

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(contactsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(contactLabel, "align label,sizegroup label,split 3");
		add(contactField, "grow,wrap");
		add(descriptionLabel, "align label,sizegroup label,split 2");
		add(descriptionField, "grow,wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(notesButton, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void updateContactFieldMenuItems(){
		final String contact = contactField.getText();
		final boolean enable = StringUtils.isNotBlank(contact);
		final boolean enablePhoneNumber = (enable && PhoneNumberValidator.isValid(contact));
		final boolean enableEmail = (enable && EMAIL_VALIDATOR.isValid(contact));
		final boolean enableLink = (enable && URL_VALIDATOR.isValid(contact));

		callItem.setEnabled(enablePhoneNumber);
		sendEmailItem.setEnabled(enableEmail);
		testLinkItem.setEnabled(enableLink);
		openLinkItem.setEnabled(enableLink);
	}

	private void attachOpenLinkPopUpMenu(final JTextField component){
		final JPopupMenu popupMenu = new JPopupMenu();

		testLinkItem.addActionListener(event -> {
			final String url = component.getText();
			final boolean urlReachable = FileHelper.testURL(url);
			final String message = JavaHelper.format((urlReachable? "Success, the link `{}` is reachable.":
				"The connection attempt to `{}` failed."), url);
			JOptionPane.showMessageDialog(this, message, "Test link result",
				(urlReachable? JOptionPane.INFORMATION_MESSAGE: JOptionPane.ERROR_MESSAGE));
		});
		openLinkItem.addActionListener(event -> FileHelper.browseURL(component.getText()));
		popupMenu.add(testLinkItem);
		popupMenu.add(openLinkItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	private int calculateDataHash(){
		final int idHash = Objects.requireNonNullElse(contactField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int descriptionHash = Objects.requireNonNullElse(descriptionField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int restrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		return idHash ^ descriptionHash ^ restrictionHash;
	}

	private void okAction(){
		final String id = contactField.getText();
		final String description = descriptionField.getText();
		final String restriction = (restrictionCheckBox.isSelected()? "confidential": null);

		final int index = contactsTable.convertRowIndexToModel(contactsTable.getSelectedRow());
		final GedcomNode contactNode = container.getChildrenWithTag("CONTACT")
			.get(index);
		final GedcomNode extractNode = store.traverse(contactNode, "EXTRACT");
		contactNode.withValue(id)
			.replaceChildValue("DESCRIPTION", description)
			.removeChildrenWithTag("EXTRACT")
			.addChild(extractNode)
			.replaceChildValue("RESTRICTION", restriction);
	}

	private void selectAction(final int selectedRow){
		final List<GedcomNode> contacts = store.traverseAsList(container, "CONTACT[]");
		final GedcomNode selectedContact = contacts.get(selectedRow);

		final String id = selectedContact.getValue();
		final String description = store.traverse(selectedContact, "DESCRIPTION").getValue();
		final GedcomNode extractNode = store.traverse(selectedContact, "EXTRACT");
		final String extract = extractNode.getValue();
		final String extractType = store.traverse(extractNode, "TYPE").getValue();
		final String extractLanguageTag = store.traverse(extractNode, "LOCALE").getValue();
		final String restriction = store.traverse(selectedContact, "RESTRICTION").getValue();

		updating = true;

		contactField.setText(id);
		descriptionField.setEnabled(true);
		descriptionField.setText(description);
		restrictionCheckBox.setEnabled(true);
		restrictionCheckBox.setSelected("confidential".equals(restriction));
		notesButton.setEnabled(true);

		updating = false;

		dataHash = calculateDataHash();
	}

	private void addAction(){
		final GedcomNode newContact = store.create("CONTACT");
		container.forceAddChild(newContact);

		//refresh group list
		loadData();

		final int lastRowIndex = contactsTable.getModel().getRowCount() - 1;
		contactsTable.setRowSelectionInterval(lastRowIndex, lastRowIndex);
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)contactsTable.getModel();
		final int index = contactsTable.convertRowIndexToModel(contactsTable.getSelectedRow());
		model.removeRow(index);
	}

	public void loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> contacts = store.traverseAsList(container, "CONTACT[]");
		final int size = contacts.size();

		if(size > 0){
			final DefaultTableModel contactsModel = (DefaultTableModel)contactsTable.getModel();
			contactsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode contact = contacts.get(row);

				contactsModel.setValueAt(contact.getValue(), row, TABLE_INDEX_CONTACT_ID);
			}
		}
	}

	private void filterTableBy(final ContactDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_CONTACT_ID);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)contactsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)contactsTable.getModel();
			sorter = new TableRowSorter<>(model);
			contactsTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	private RowFilter<DefaultTableModel, Object> createTextFilter(final String text, final int... columnIndexes){
		if(StringUtils.isNotBlank(text)){
			try{
				//split input text around spaces and commas
				final String[] components = StringUtils.split(text, " ,");
				final Collection<RowFilter<DefaultTableModel, Object>> andFilters = new ArrayList<>(components.length);
				for(final String component : components)
					andFilters.add(RowFilter.regexFilter("(?i)(?:" + Pattern.quote(component) + ")", columnIndexes));
				return RowFilter.andFilter(andFilters);
			}
			catch(final PatternSyntaxException ignored){
				//current expression doesn't parse, ignore it
			}
		}
		return null;
	}

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
	}


	private static class ContactTableModel extends DefaultTableModel{

		private static final long serialVersionUID = -2887467453297082858L;


		ContactTableModel(){
			super(new String[]{"Contact"}, 0);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.6.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getSources().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					JDialog dialog = null;
					switch(editCommand.getType()){
						case NOTE_CITATION:
							dialog = new NoteCitationDialog(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer());

							dialog.setSize(450, 260);
							break;

						case NOTE:
							dialog = new NoteDialog(store, parent);
							((NoteDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
					}
					if(dialog != null){
						dialog.setLocationRelativeTo(parent);
						dialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final ContactDialog dialog = new ContactDialog(store, parent);
			dialog.loadData(container, null);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 650);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
