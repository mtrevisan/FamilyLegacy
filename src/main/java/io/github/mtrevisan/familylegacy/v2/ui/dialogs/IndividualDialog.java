package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PersonalNamePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dialog for editing an {@code INDIVIDUAL_RECORD} according to FLEF 0.0.9.
 * <p>
 * Structure:
 * <pre>
 * INDIVIDUAL_RECORD :=
 * n @<XREF:INDIVIDUAL>@ INDIVIDUAL    {1:1}
 *   +1 <<PERSONAL_NAME_STRUCTURE>>    {0:M}
 *   +1 SEX <SEX_VALUE>    {0:1}
 *   +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:M}
 *   +1 NOTE @<XREF:NOTE>@    {0:M}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 PREFERRED_IMAGE <RESOURCE_URI>    {0:1}
 *     +2 CROP <CROP_COORDINATES>    {0:1}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class IndividualDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212974L;

	// Handlers
	private final CulturalNormHandler culturalNormHandler = new CulturalNormHandler();
	private final NoteHandler noteHandler = new NoteHandler();
	private final SourceHandler sourceHandler = new SourceHandler();

	private final BindingManager bindingManager = new BindingManager();

	private Dialog parent;

	// Preferred Image
	private String preferredImageId;
	private String preferredImageCrop;
	private final JButton preferredImageButton = new JButton();

	// UI components (simple fields are now bound)
	private final BoundComboBox sexCombo;

	// Panels (complex, handled manually)
	private PersonalNamePanel namePanel;
	private RestrictionPanel restrictionPanel;
	private ConclusionPanel conclusionPanel;
	private ModificationPanel modificationPanel;

	// Cultural Norms
	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();
	private final Map<String, String> culturalNormDisplayMap = new HashMap<>();

	// Notes (top-level)
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// ----- Factory methods -----
	public static IndividualDialog createNew(Dialog parent, FLEFModel model){
		return new IndividualDialog(parent, model, null);
	}

	public static IndividualDialog createEdit(Dialog parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");
		return new IndividualDialog(parent, model, record);
	}

	// ----- Constructor -----
	private IndividualDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IndividualHandler.TYPE));

		this.parent = parent;

		// Initialize bound components before using them
		sexCombo = new BoundComboBox("SEX",
			new String[]{StringUtils.EMPTY, "MALE", "FEMALE", "UNKNOWN"});

		initComponents();
		loadData();
		setMinimumSize(new Dimension(550, 600));
		pack();
		setLocationRelativeTo(parent);
	}

	// ----- Initialisation -----
	protected void initComponents(){
		namePanel = new PersonalNamePanel(model, this);
		restrictionPanel = new RestrictionPanel("RESTRICTION", this);
		conclusionPanel = new ConclusionPanel("CONCLUSION", model, this);
		modificationPanel = new ModificationPanel(this);

		bindingManager.bind(sexCombo);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		setLayout(new MigLayout("fillx"));
		add(tabbedPane, "growx,push");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]5[]10[]"));

		// Preferred Image
		preferredImageButton.setPreferredSize(new Dimension(80, 80));
		preferredImageButton.setIcon(createPlaceholderIcon());
		preferredImageButton.setToolTipText("Left-click to select an image, right-click for options");
		preferredImageButton.addActionListener(e -> selectAndCropImage());

		JPopupMenu imagePopup = new JPopupMenu();
		JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);

		preferredImageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()) imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()) imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}
		});
		panel.add(preferredImageButton, "growx, align center");

		// Names (PersonalNamePanel)
		panel.add(namePanel, "growx");

		// Sex – now using the bound combo box
		JPanel sexPanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		sexPanel.add(new JLabel("Sex:"), "align label");
		sexPanel.add(sexCombo, "growx");
		panel.add(sexPanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5,fillx,wrap 1", "[grow]", "[]5[]5[]"));

		panel.add(createListPanel("Cultural Norms",
				culturalNormList, culturalNormListModel,
				this::addCulturalNorm, this::editCulturalNorm, this::deleteCulturalNorm),
			"growx");

		panel.add(createListPanel("Notes",
				noteList, noteListModel,
				this::addNote, this::editNote, this::deleteNote),
			"growx");

		panel.add(createListPanel("Source Citations",
				sourceCitationList, sourceCitationListModel,
				this::addSourceCitation, this::editSourceCitation, this::deleteSourceCitation),
			"growx");

		return panel;
	}

	// ----- Generic list panel builder -----
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
		Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		GUIHelper.installStandardBehavior(list,
			() -> list.getSelectedIndex() >= 0,
			() -> createNewItemForList(list, model),
			addAction,
			editAction,
			deleteAction,
			null);

		JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		panel.add(scrollPane, "growx,wrap");
		return panel;
	}

	// ----- Helper to create new item from list context -----
	private void createNewItemForList(JList<String> list, DefaultListModel<String> model){
		if(list == culturalNormList){
			createNewCulturalNorm();
		}
		else if(list == noteList){
			createNewNote();
		}
		else if(list == sourceCitationList){
			createNewSource();
		}
	}

	private Icon createPlaceholderIcon(){
		BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("[No img]", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

	private void selectAndCropImage(){
		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, sourceHandler, selectedId -> result[0] = selectedId);
		dialog.setVisible(true);
		String sourceId = result[0];
		if(sourceId == null) return;

		BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ImageCropDialog cropDialog = new ImageCropDialog(this, image);
		cropDialog.setVisible(true);

		Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + StringUtils.SPACE + cropRect.y + StringUtils.SPACE + cropRect.width + StringUtils.SPACE + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	private BufferedImage loadImageFromSource(String sourceId){
		FLEFRecord source = model.getRecordById(sourceId);
		if(source == null) return null;

		FLEFRecord doc = FLEFRecordUtils.findChild(source, "DOCUMENT_STRUCTURE");
		if(doc == null) return null;

		String filePath = FLEFRecordUtils.getChildValue(doc, "FILE");
		if(filePath == null || filePath.isEmpty()) return null;

		try{
			File file = new File(filePath);
			if(!file.exists()) return null;
			return ImageIO.read(file);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	private void updateImageButton(String sourceId){
		BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			preferredImageButton.setIcon(new ImageIcon(scaled));
		}
		else{
			preferredImageButton.setIcon(createPlaceholderIcon());
		}
	}

	private void clearImage(){
		preferredImageId = null;
		preferredImageCrop = null;
		preferredImageButton.setIcon(createPlaceholderIcon());
	}

	private void addCulturalNorm(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				String display = getCulturalNormDisplayName(selectedId);
				culturalNormDisplayMap.put(selectedId, display);
				culturalNormListModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;

		String id = culturalNormIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;

		JDialog dialog = culturalNormHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getCulturalNormDisplayName(id);
		culturalNormDisplayMap.put(id, newDisplay);
		culturalNormListModel.set(idx, newDisplay);
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this cultural norm?")) return;

		String removedId = culturalNormIds.remove(idx);
		culturalNormDisplayMap.remove(removedId);
		culturalNormListModel.remove(idx);
	}

	private void createNewCulturalNorm(){
		Set<String> before = new HashSet<>(culturalNormIds);
		JDialog dialog = culturalNormHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !culturalNormIds.contains(id)){
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormListModel.addElement(display);
				return;
			}
		}
	}

	private String getCulturalNormDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null) return culturalNormHandler.getDisplayText(rec);
		return id;
	}

	private String getNoteDisplayName(String id){
		FLEFRecord rec = model.getRecordById(id);
		if(rec != null) return noteHandler.getDisplayText(rec);
		return id;
	}

	private void addNoteToList(DefaultListModel<String> listModel, List<String> ids, Map<String, String> displayMap){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, noteHandler, selectedId -> {
			if(selectedId != null && !ids.contains(selectedId)){
				ids.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				displayMap.put(selectedId, display);
				listModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNoteFromList(JList<String> list, DefaultListModel<String> listModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;

		String id = ids.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;

		JDialog dialog = noteHandler.createEditDialog(this, model, rec);
		dialog.setVisible(true);

		String newDisplay = getNoteDisplayName(id);
		displayMap.put(id, newDisplay);
		listModel.set(idx, newDisplay);
	}

	private void deleteNoteFromList(JList<String> list, DefaultListModel<String> listModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this note?")) return;

		String removedId = ids.remove(idx);
		displayMap.remove(removedId);
		listModel.remove(idx);
	}

	private void createNewNoteForList(DefaultListModel<String> listModel, List<String> ids, Map<String, String> displayMap){
		Set<String> before = new HashSet<>(ids);
		JDialog dialog = noteHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !ids.contains(id)){
				ids.add(id);
				String display = getNoteDisplayName(id);
				displayMap.put(id, display);
				listModel.addElement(display);
				return;
			}
		}
	}

	private void addNote(){
		addNoteToList(noteListModel, noteIds, noteDisplayMap);
	}

	private void editNote(){
		editNoteFromList(noteList, noteListModel, noteIds, noteDisplayMap);
	}

	private void deleteNote(){
		deleteNoteFromList(noteList, noteListModel, noteIds, noteDisplayMap);
	}

	private void createNewNote(){
		createNewNoteForList(noteListModel, noteIds, noteDisplayMap);
	}

	private void addSourceCitation(){
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, sourceHandler, selectedId -> {
			if(selectedId != null){
				FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", selectedId);
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
			}
		});
		dialog.setVisible(true);
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;

		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(this, model, existing);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				sourceCitationRecords.set(idx, updated);
				sourceCitationListModel.set(idx, getSourceCitationDisplay(updated));
			}
		}
	}

	private void deleteSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;

		if(!showConfirm("Confirm", "Remove this source citation?")) return;

		sourceCitationRecords.remove(idx);
		sourceCitationListModel.remove(idx);
	}

	private void createNewSource(){
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}

		JDialog dialog = sourceHandler.createNewDialog(this, model);
		dialog.setVisible(true);

		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				FLEFRecord citation = FLEFRecord.createChildWithValue("SOURCE", id);
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
				return;
			}
		}
	}

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null){
				return sourceHandler.getDisplayText(rec);
			}
			return sourceId;
		}
		return "[empty]";
	}

	@Override
	protected void loadData(){
		// ---- Simple fields: load via binding manager ----
		bindingManager.load(record);

		// ---- Complex panels: manual load ----
		namePanel.loadFromRecord(record);

		// CULTURAL_NORM
		culturalNormIds.clear();
		culturalNormListModel.clear();
		culturalNormDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				String display = getCulturalNormDisplayName(id);
				culturalNormDisplayMap.put(id, display);
				culturalNormListModel.addElement(display);
			}
		}

		// NOTE (top-level)
		noteIds.clear();
		noteListModel.clear();
		noteDisplayMap.clear();
		for(FLEFRecord child : record.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				noteIds.add(id);
				String display = getNoteDisplayName(id);
				noteDisplayMap.put(id, display);
				noteListModel.addElement(display);
			}
		}

		// SOURCE_CITATION
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// Preferred Image
		FLEFRecord pref = FLEFRecordUtils.findChild(record, "PREFERRED_IMAGE");
		if(pref != null){
			preferredImageId = pref.getValue();
			preferredImageCrop = FLEFRecordUtils.getChildValue(pref, "CROP");
			updateImageButton(preferredImageId);
		}
		else{
			clearImage();
		}

		// RESTRICTION_STRUCTURE
		restrictionPanel.load(record);

		// CONCLUSION_STRUCTURE
		conclusionPanel.load(record);

		// MODIFICATION_STRUCTURE
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// Validate names
		if(!namePanel.validateData()){
			return false;
		}

		if(restrictionPanel.hasData() && !restrictionPanel.validateData()){
			return false;
		}

		return !conclusionPanel.hasData() || conclusionPanel.validateData();
	}

	@Override
	protected void saveData(){
		// ---- Save complex panels first ----
		namePanel.saveToRecord(record);

		// CULTURAL_NORM
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", id);
		}

		// NOTE (top-level)
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", id);
		}

		// SOURCE_CITATION
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// Preferred Image
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = FLEFRecord.createChildWithValue("PREFERRED_IMAGE", preferredImageId);
			record.addChild(pref);
			FLEFRecordUtils.updateChildValue(pref, "CROP", preferredImageCrop);
		}

		// RESTRICTION_STRUCTURE
		if(restrictionPanel.hasData())
			restrictionPanel.save(record);

		// CONCLUSION_STRUCTURE
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.save(null);
			if(conclusion != null){
				conclusion.setTag("CONCLUSION");
				record.addChild(conclusion);
			}
		}

		// MODIFICATION_STRUCTURE
		modificationPanel.save(record);

		// ---- Save simple fields via binding manager (must be after adding all other children) ----
		bindingManager.save(record);
	}

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Individual Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Individual");
			btn.addActionListener(e -> {
				IndividualDialog dialog = IndividualDialog.createNew(null, model);
				dialog.setVisible(true);
				System.out.println("Individual saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
