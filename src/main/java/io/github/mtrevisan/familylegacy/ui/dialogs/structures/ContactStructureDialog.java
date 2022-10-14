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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


//TODO
public class ContactStructureDialog extends JDialog implements ActionListener{

	@Serial
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
	private final JLabel contactIDLabel = new JLabel("Contact:");
	private final JTextField contactIDField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type");
	private final JTextField typeField = new JTextField();
	private final JLabel callerIDLabel = new JLabel("Caller ID:");
	private final JTextField callerIDField = new JTextField();
	private final JMenuItem sendEmailItem = new JMenuItem("Send email…");
	private final JMenuItem testLinkItem = new JMenuItem("Test link");
	private final JMenuItem openLinkItem = new JMenuItem("Open link…");
	private final JButton noteButton = new JButton("Notes");
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<ContactStructureDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private volatile boolean updating;
	private int dataHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public ContactStructureDialog(final Flef store, final Frame parent){
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
				filterDebouncer.call(ContactStructureDialog.this);
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

		contactIDField.setEnabled(false);
		GUIHelper.bindLabelTextChangeUndo(contactIDLabel, contactIDField, evt -> {
			updateContactFieldMenuItems(contactIDField.getText());

			dataChanged();
		});
		//manage links
		attachOpenLinkPopUpMenu(contactIDField);

		typeField.setEnabled(false);
		GUIHelper.bindLabelTextChangeUndo(typeLabel, typeField, evt -> dataChanged());

		callerIDField.setEnabled(false);
		GUIHelper.bindLabelTextChangeUndo(callerIDLabel, callerIDField, evt -> dataChanged());

		noteButton.setEnabled(false);
		noteButton.addActionListener(evt -> {
			final int selectedRow = contactsTable.convertRowIndexToModel(contactsTable.getSelectedRow());
			final List<GedcomNode> contacts = store.traverseAsList(container, "CONTACT[]");
			final GedcomNode selectedContact = contacts.get(selectedRow);

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedContact));
		});

		restrictionCheckBox.setEnabled(false);
		restrictionCheckBox.addActionListener(evt -> {
			updateContactFieldMenuItems(contactIDField.getText());

			dataChanged();
		});

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

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(contactsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2,wrap paragraph");
		add(contactIDLabel, "align label,sizegroup label,split 2");
		add(contactIDField, "grow,wrap");
		add(typeLabel, "align label,sizegroup label,split 2");
		add(typeField, "grow,wrap");
		add(callerIDLabel, "align label,sizegroup label,split 2");
		add(callerIDField, "grow,wrap paragraph");
		add(noteButton, "grow,wrap paragraph");
		add(restrictionCheckBox, "wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void updateContactFieldMenuItems(final String contactID){
		final boolean enable = StringUtils.isNotBlank(contactID);
		final boolean enableEmail = (enable && EMAIL_VALIDATOR.isValid(contactID));
		final boolean enableLink = (enable && URL_VALIDATOR.isValid(contactID));

		sendEmailItem.setVisible(enableEmail);
		testLinkItem.setVisible(enableLink);
		openLinkItem.setVisible(enableLink);
	}

	private void attachOpenLinkPopUpMenu(final JTextField component){
		final JPopupMenu popupMenu = new JPopupMenu();

		sendEmailItem.addActionListener(event -> FileHelper.sendEmail(component.getText()));
		testLinkItem.addActionListener(event -> {
			final String url = component.getText();
			final boolean urlReachable = FileHelper.testURL(url);
			final String message = JavaHelper.textFormat((urlReachable? "Success, the link `{}` is reachable.":
				"The connection attempt to `{}` failed."), url);
			JOptionPane.showMessageDialog(this, message, "Test link result",
				(urlReachable? JOptionPane.INFORMATION_MESSAGE: JOptionPane.ERROR_MESSAGE));
		});
		openLinkItem.addActionListener(event -> FileHelper.browseURL(component.getText()));

		popupMenu.add(sendEmailItem);
		popupMenu.add(testLinkItem);
		popupMenu.add(openLinkItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	public void dataChanged(){
		if(!updating)
			okButton.setEnabled(calculateDataHash() != dataHash);
	}

	private int calculateDataHash(){
		final int contactIDHash = Objects.requireNonNullElse(contactIDField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int typeHash = Objects.requireNonNullElse(typeField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int callerIDHash = Objects.requireNonNullElse(callerIDField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int restrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		return contactIDHash ^ typeHash ^ callerIDHash ^ restrictionHash;
	}

	private void okAction(){
		final String contactID = contactIDField.getText();
		final String type = typeField.getText();
		final String callerID = callerIDField.getText();
		final String restriction = (restrictionCheckBox.isSelected()? "confidential": null);

		final int index = contactsTable.convertRowIndexToModel(contactsTable.getSelectedRow());
		final GedcomNode contactNode = container.getChildrenWithTag("CONTACT")
			.get(index);
		contactNode.withValue(contactID)
			.replaceChildValue("TYPE", type)
			.replaceChildValue("CALLER_ID", callerID)
			.replaceChildValue("RESTRICTION", restriction);
	}

	private void selectAction(final int selectedRow){
		final List<GedcomNode> contacts = store.traverseAsList(container, "CONTACT[]");
		final GedcomNode selectedContact = contacts.get(selectedRow);

		final String contactID = selectedContact.getValue();
		final String type = store.traverse(selectedContact, "TYPE").getValue();
		final String callerID = store.traverse(selectedContact, "CALLER_ID").getValue();
		final String restriction = store.traverse(selectedContact, "RESTRICTION").getValue();

		updating = true;

		contactIDField.setEnabled(true);
		contactIDField.setText(contactID);
		typeField.setEnabled(true);
		typeField.setText(type);
		callerIDField.setEnabled(true);
		callerIDField.setText(callerID);
		noteButton.setEnabled(true);
		restrictionCheckBox.setEnabled(true);
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		updating = false;

		dataHash = calculateDataHash();
		updateContactFieldMenuItems(contactID);
		dataChanged();
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

	private void filterTableBy(final ContactStructureDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(text, TABLE_INDEX_CONTACT_ID);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)contactsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)contactsTable.getModel();
			sorter = new TableRowSorter<>(model);
			contactsTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
	}


	private static class ContactTableModel extends DefaultTableModel{

		@Serial
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


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.8.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode repository = store.getRepositories().get(1);

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
					switch(editCommand.getType()){
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
							final GedcomNode noteCitation = editCommand.getContainer();
							dialog.setTitle(noteCitation.getID() != null
								? "Note citation " + noteCitation.getID() + " for repository " + repository.getID()
								: "New note citation for repository " + repository.getID());
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a note input dialog
								dialog.addAction();

							dialog.setSize(450, 260);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteRecordDialog dialog = NoteRecordDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle("Note for " + note.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final ContactStructureDialog dialog = new ContactStructureDialog(store, parent);
			dialog.loadData(repository, null);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(350, 430);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
