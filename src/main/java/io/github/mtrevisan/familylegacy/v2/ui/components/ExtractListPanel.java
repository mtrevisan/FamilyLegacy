package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/* ONGOING */
public class ExtractListPanel extends AbstractListPanel2{

	@Serial
	private static final long serialVersionUID = -259585503419013969L;


	private static final String TAG_OPEN_SQUARE_BRACKET = "[";
	private static final String TAG_CLOSE_SQUARE_BRACKET = "]";

	private static final String DOT = ".";

	private static final String TAG_EXTRACT = "EXTRACT";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_CROP = "CROP";
	private static final String TAG_TEXT = "TEXT";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_NOTE = "NOTE";


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final String path;


	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler(NoteHandler.TYPE);


	public ExtractListPanel(final String path, final Dialog parent, final FLEFModel model){
		super(parent, "Extracts", model);

		this.path = path;
	}

	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editItem,
			this::createNewItem,
			this::removeItem,
			builder -> {
				builder.item("Create New...", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit...", this::editItem);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			});
	}

	@Override
	protected String getDisplay(final FLEFRecord item){
		final String text = FLEFRecordHelper.getChildValue(item, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(item, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(item, TAG_LOCALE);

		final StringBuilder sb = new StringBuilder();
		if(!StringUtils.isEmpty(locale))
			sb.append("[")
				.append(locale)
				.append("] ");
		if(!StringUtils.isEmpty(text))
			sb.append(text.length() > 50? text.substring(0, 47) + "...": text);
		if(!StringUtils.isEmpty(type))
			sb.append(" (")
				.append(type)
				.append(")");
		return sb.toString();
	}

	@Override
	protected FLEFRecord showAddDialog(){
		return null;
	}

	/**
	 * Creates a new extract and adds it to the list.
	 */
	@Override
	protected FLEFRecord showCreateNewDialog(){
		return showExtractDialog(null);
	}

	@Override
	protected FLEFRecord showEditDialog(FLEFRecord existing){
		return showExtractDialog(existing);
	}

	//FIXME note:
	private final DefaultListModel<String> noteModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	private FLEFRecord showExtractDialog(FLEFRecord initial){
		final JDialog dialog = new JDialog(parent, initial == null? "Add Extract": "Edit Extract", true);
		dialog.setLayout(new MigLayout("ins 10,fillx", "[right]rel[grow]", "[]10[]"));

		final String text = FLEFRecordHelper.getChildValue(initial, TAG_TEXT);
		final String type = FLEFRecordHelper.getChildValue(initial, TAG_TYPE);
		final String locale = FLEFRecordHelper.getChildValue(initial, TAG_LOCALE);

		final BoundTextArea textArea = new BoundTextArea(TAG_TEXT, 3, 25);
		if(initial != null)
			textArea.setText(text);
		dialog.add(new JLabel("Text*:"), "align label,top");
		dialog.add(GUIHelper.createScrollPane(textArea), "growx,wrap");

		final BoundComboBox<String> typeCombo = new BoundComboBox<>(TAG_TYPE,
			new String[]{"transcript", "extract", "abstract"});
		if(initial != null && !StringUtils.isEmpty(type))
			typeCombo.setSelectedItem(type);
		dialog.add(new JLabel("Type*:"), "align label");
		dialog.add(typeCombo, "growx,wrap");

		final BoundComboBox<String> localeCombo = new BoundComboBox<>(TAG_LOCALE,
			new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		if(initial != null && !StringUtils.isEmpty(locale))
			localeCombo.setSelectedItem(locale);
		dialog.add(new JLabel("Locale:"), "align label");
		dialog.add(localeCombo, "growx,wrap");

		dialog.add(createNotePanel(), "span 2,growx,wrap");

		final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okBtn = new JButton("OK");
		final JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, "span 2,growx");

		final FLEFRecord[] result = {null};
		okBtn.addActionListener(e -> {
			if(StringUtils.isEmpty(textArea.getValue())){
				GUIHelper.showValidationErrorAndFocus(dialog, "Extract value cannot be empty.",
					null, null, textArea);

				return;
			}
			if(StringUtils.isEmpty((String)typeCombo.getSelectedItem())){
				GUIHelper.showValidationErrorAndFocus(dialog, "Extract type cannot be empty.",
					null, null, textArea);

				return;
			}
			final FLEFRecord res = FLEFRecord.createEmpty();
			res.addChild(FLEFRecord.createChildWithValue(TAG_TEXT, textArea.getValue()));
			if(!StringUtils.isEmpty((String)localeCombo.getSelectedItem()))
				res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, (String)localeCombo.getSelectedItem()));
			result[0] = res;
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return result[0];
	}

	private JPanel createNotePanel(){
		JPanel panel = new JPanel(new BorderLayout(3, 3));

		JScrollPane scrollPane = GUIHelper.createScrollPane(noteList);
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		JButton addBtn = new JButton("Add");
		JButton newBtn = new JButton("New");
		JButton editBtn = new JButton("Edit");
		JButton deleteBtn = new JButton("Delete");
		btnPanel.add(addBtn);
		btnPanel.add(newBtn);
		btnPanel.add(editBtn);
		btnPanel.add(deleteBtn);
		panel.add(btnPanel, BorderLayout.SOUTH);

		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNote();
				}
			}
		});
		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editBtn.setEnabled(selected);
			deleteBtn.setEnabled(selected);
		});
		editBtn.setEnabled(false);
		deleteBtn.setEnabled(false);

		addBtn.addActionListener(e -> addNote());
		newBtn.addActionListener(e -> createNewNote());
		editBtn.addActionListener(e -> editNote());
		deleteBtn.addActionListener(e -> deleteNote());

		return panel;
	}

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null)
			return noteHandler.getDisplayText(rec, model);
		return id;
	}

	private void addNote(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			null,
			model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteModel.addElement(display);
			}
		}
		);
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;

		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null){
			JOptionPane.showMessageDialog(null, "Note not found: " + id, "Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		JDialog dialog = noteHandler.createEditDialog(null, model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1)
			return;
		int confirm = JOptionPane.showConfirmDialog(null, "Remove this note reference?", "Confirm", JOptionPane.YES_NO_OPTION);
		if(confirm == JOptionPane.YES_OPTION){
			String removedId = noteIds.remove(idx);
			noteDisplayMap.remove(removedId);
			noteModel.remove(idx);
		}
	}

	private void createNewNote(){
		Set<String> before = new HashSet<>(noteIds);
		JDialog dialog = noteHandler.createNewDialog(null, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !noteIds.contains(id)){
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
				break;
			}
		}
	}

	public void load(final FLEFRecord record){
		final List<FLEFRecord> extracts = new ArrayList<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(record, path)){
			final String extractText = FLEFRecordHelper.getChildValue(child, TAG_TEXT);
			final String extractType = FLEFRecordHelper.getChildValue(child, TAG_TYPE);
			final String extractLocale = FLEFRecordHelper.getChildValue(child, TAG_LOCALE);
			if(StringUtils.isNotEmpty(extractText)){
				final FLEFRecord res = FLEFRecord.createEmpty();
				res.addChild(FLEFRecord.createChildWithValue(TAG_TEXT, extractText));
				res.addChild(FLEFRecord.createChildWithValue(TAG_TYPE, extractType));
				if(extractLocale != null && !extractLocale.isEmpty())
					res.addChild(FLEFRecord.createChildWithValue(TAG_LOCALE, extractLocale));
				extracts.add(res);
			}
		}
		setItems(extracts);


		noteModel.clear();
		noteIds.clear();
		noteDisplayMap.clear();

		// NOTE
		for(FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteModel.addElement(display);
			}
		}
	}

	public void save(final FLEFRecord record){
		super.save(record, path);

		// NOTE
		for(final String id : noteIds)
			FLEFRecordHelper.addChild(record, TAG_EXTRACT + DOT + TAG_NOTE, XRefHelper.formatXRef(id));
	}

}
