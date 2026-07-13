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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CalendarHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.FamilyHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.function.Consumer;

import javax.swing.ImageIcon;


/**
 * Dialog for editing a FAMILY_RECORD according to FLEF 0.0.9.
 */
public class FamilyDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212971L;

	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new FamilyHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new GroupCitationHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new CalendarHandler());
	}

	// Basic fields
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	// Partner1
	private final JTextField partner1DisplayField = new JTextField(20);
	private String partner1Id;
	private final DefaultListModel<String> partner1NoteModel = new DefaultListModel<>();
	private final JList<String> partner1NoteList = new JList<>(partner1NoteModel);
	private final List<String> partner1NoteIds = new ArrayList<>();
	private final Map<String, String> partner1NoteDisplayMap = new HashMap<>();

	// Partner2
	private final JTextField partner2DisplayField = new JTextField(20);
	private String partner2Id;
	private final DefaultListModel<String> partner2NoteModel = new DefaultListModel<>();
	private final JList<String> partner2NoteList = new JList<>(partner2NoteModel);
	private final List<String> partner2NoteIds = new ArrayList<>();
	private final Map<String, String> partner2NoteDisplayMap = new HashMap<>();

	// Children
	private final DefaultListModel<String> childListModel = new DefaultListModel<>();
	private final JList<String> childList = new JList<>(childListModel);
	private final List<ChildEntry> childEntries = new ArrayList<>();

	// Events
	private final DefaultListModel<String> eventListModel = new DefaultListModel<>();
	private final JList<String> eventList = new JList<>(eventListModel);
	private final List<String> eventIds = new ArrayList<>();

	// Group Citations
	private final DefaultListModel<String> groupCitationListModel = new DefaultListModel<>();
	private final JList<String> groupCitationList = new JList<>(groupCitationListModel);
	private final List<FLEFRecord> groupCitationRecords = new ArrayList<>();

	// Cultural Norms
	private final DefaultListModel<String> culturalNormListModel = new DefaultListModel<>();
	private final JList<String> culturalNormList = new JList<>(culturalNormListModel);
	private final List<String> culturalNormIds = new ArrayList<>();

	// Notes
	private final DefaultListModel<String> noteListModel = new DefaultListModel<>();
	private final JList<String> noteList = new JList<>(noteListModel);
	private final List<String> noteIds = new ArrayList<>();
	private final Map<String, String> noteDisplayMap = new HashMap<>();

	// Source Citations
	private final DefaultListModel<String> sourceCitationListModel = new DefaultListModel<>();
	private final JList<String> sourceCitationList = new JList<>(sourceCitationListModel);
	private final List<FLEFRecord> sourceCitationRecords = new ArrayList<>();

	// Preferred Image
	private String preferredImageId;
	private String cropString;
	private final JButton imageButton = new JButton();

	// Modification
	private final ModificationPanel modificationPanel;

	// Conclusion
	private final ConclusionPanel conclusionPanel;

	private final JButton saveButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Cancel");

	private final RecordTypeHandler<?> individualHandler = HandlerRegistry.getHandler("INDIVIDUAL");
	private final RecordTypeHandler<?> groupHandler = HandlerRegistry.getHandler("GROUP");
	private final RecordTypeHandler<?> groupCitationHandler = HandlerRegistry.getHandler("GROUP_CITATION");
	private final RecordTypeHandler<?> eventHandler = HandlerRegistry.getHandler("EVENT");
	private final RecordTypeHandler<?> culturalNormHandler = HandlerRegistry.getHandler("CULTURAL_NORM");
	private final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler("SOURCE");
	private final RecordTypeHandler<?> noteHandler = HandlerRegistry.getHandler("NOTE");


	private static class ChildEntry{
		String childId;
		List<String> noteIds;

		ChildEntry(String childId, List<String> noteIds){
			this.childId = childId;
			this.noteIds = noteIds != null? noteIds: new ArrayList<>();
		}

		@Override
		public String toString(){
			return childId + (noteIds.isEmpty()? "": " (" + noteIds.size() + " notes)");
		}
	}


	public static FamilyDialog createNew(Frame parent, FLEFModel model){
		return new FamilyDialog(parent, model, null);
	}

	public static FamilyDialog createEdit(Frame parent, FLEFModel model, FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new FamilyDialog(parent, model, record);
	}


	private FamilyDialog(Frame parent, FLEFModel model, FLEFRecord record){
		super(parent, model, buildTitle(model, record), record);

		this.modificationPanel = new ModificationPanel(model, this);
		this.conclusionPanel = new ConclusionPanel(model, this);
		initComponents();
		loadData();
		setMinimumSize(new Dimension(500, 550));
		pack();
		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFModel model, final FLEFRecord record){
		return (record == null
			? "New Family - " + FLEFRecordUtils.generateNewId(model, "FAMILY", "F") + "*"
			: "Edit Family - " + record.getId());
	}


	@Override
	protected void initComponents(){
		setLayout(new MigLayout("fillx"));

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Modification", modificationPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);

		add(tabbedPane, "growx,push");

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		saveButton.addActionListener(e -> save());
		cancelButton.addActionListener(e -> dispose());
	}


	// ==================== Preferred Image Management ====================

	/**
	 * Creates a placeholder icon for the image button.
	 *
	 * @return the placeholder icon
	 */
	private Icon createPlaceholderIcon(){
		BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("No img", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

	/**
	 * Opens the source selection dialog and, if a source is selected,
	 * loads the image and opens the crop dialog.
	 */
	private void selectAndCropImage(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(this, "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String[] result = {null};
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, sourceHandler,
			selectedId -> result[0] = selectedId);
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

		//FIXME
		ImageCropDialog cropDialog = new ImageCropDialog(getParentFrame(), image);
		cropDialog.setVisible(true);

		Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			cropString = cropRect.x + " " + cropRect.y + " " + cropRect.width + " " + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	/**
	 * Loads the image from a SOURCE_RECORD.
	 *
	 * @param sourceId the ID of the source record
	 * @return the loaded BufferedImage, or null if not found
	 */
	private BufferedImage loadImageFromSource(String sourceId){
		FLEFRecord source = model.getRecordById(sourceId);
		if(source == null)
			return null;

		FLEFRecord doc = FLEFRecordUtils.findChild(source, "DOCUMENT_STRUCTURE");
		if(doc == null)
			return null;

		String filePath = FLEFRecordUtils.getChildValue(doc, "FILE");
		if(filePath == null || filePath.isEmpty())
			return null;

		try{
			File file = new File(filePath);
			if(!file.exists())
				return null;
			return ImageIO.read(file);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Updates the image button with a thumbnail of the selected image.
	 *
	 * @param sourceId the ID of the source record
	 */
	private void updateImageButton(String sourceId){
		BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			imageButton.setIcon(new ImageIcon(scaled));
		}
		else{
			imageButton.setIcon(createPlaceholderIcon());
		}
	}

	/**
	 * Clears the preferred image (resets state and updates UI).
	 */
	private void clearImage(){
		preferredImageId = null;
		cropString = null;
		imageButton.setIcon(createPlaceholderIcon());
	}


	// ==================== Internal Image Crop Dialog ====================

	/**
	 * A dialog that allows the user to select a rectangular crop area on an image.
	 */
	private static class ImageCropDialog extends JDialog{
		private final BufferedImage image;
		private Rectangle cropRect;
		private final CropPanel cropPanel;

		public ImageCropDialog(Frame parent, BufferedImage image){
			super(parent, "Select Crop Area", true);
			this.image = image;
			cropPanel = new CropPanel(image);
			initComponents();
			pack();
			setSize(600, 500);
			setLocationRelativeTo(parent);
		}

		private void initComponents(){
			setLayout(new BorderLayout(5, 5));

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JButton okBtn = new JButton("OK");
			JButton cancelBtn = new JButton("Cancel");
			buttonPanel.add(okBtn);
			buttonPanel.add(cancelBtn);

			add(cropPanel, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);

			okBtn.addActionListener(e -> {
				cropRect = cropPanel.getCropRect();
				dispose();
			});
			cancelBtn.addActionListener(e -> {
				cropRect = null;
				dispose();
			});
		}

		public Rectangle getCrop(){
			return cropRect;
		}

		/**
		 * Panel that displays an image and allows the user to draw a crop rectangle.
		 */
		private static class CropPanel extends JPanel{
			private final BufferedImage image;
			private final int imgWidth;
			private final int imgHeight;
			private Rectangle rect;
			private Point start;
			private boolean drawing;

			public CropPanel(BufferedImage image){
				this.image = image;
				this.imgWidth = image.getWidth();
				this.imgHeight = image.getHeight();
				setPreferredSize(new Dimension(500, 400));
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

				MouseAdapter adapter = new MouseAdapter(){
					@Override
					public void mousePressed(MouseEvent e){
						start = e.getPoint();
						rect = null;
						drawing = true;
						repaint();
					}

					@Override
					public void mouseDragged(MouseEvent e){
						if(start != null && drawing){
							int x = Math.min(start.x, e.getX());
							int y = Math.min(start.y, e.getY());
							int w = Math.abs(e.getX() - start.x);
							int h = Math.abs(e.getY() - start.y);
							rect = new Rectangle(x, y, w, h);
							repaint();
						}
					}

					@Override
					public void mouseReleased(MouseEvent e){
						drawing = false;
					}
				};
				addMouseListener(adapter);
				addMouseMotionListener(adapter);
			}

			@Override
			protected void paintComponent(Graphics g){
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g;

				int panelWidth = getWidth();
				int panelHeight = getHeight();
				double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
				int scaledW = (int)(imgWidth * scale);
				int scaledH = (int)(imgHeight * scale);
				int x = (panelWidth - scaledW) / 2;
				int y = (panelHeight - scaledH) / 2;
				g2.drawImage(image, x, y, scaledW, scaledH, null);

				if(rect != null){
					g2.setColor(Color.RED);
					g2.drawRect(rect.x, rect.y, rect.width, rect.height);
					g2.setColor(new Color(255, 0, 0, 50));
					g2.fillRect(rect.x, rect.y, rect.width, rect.height);
				}
			}

			public Rectangle getCropRect(){
				if(rect == null) return null;
				int panelWidth = getWidth();
				int panelHeight = getHeight();
				double scale = Math.min((double)panelWidth / imgWidth, (double)panelHeight / imgHeight);
				int scaledW = (int)(imgWidth * scale);
				int scaledH = (int)(imgHeight * scale);
				int offsetX = (panelWidth - scaledW) / 2;
				int offsetY = (panelHeight - scaledH) / 2;

				int imgX = (int)((rect.x - offsetX) / scale);
				int imgY = (int)((rect.y - offsetY) / scale);
				int imgW = (int)(rect.width / scale);
				int imgH = (int)(rect.height / scale);

				imgX = Math.clamp(imgX, 0, imgWidth - 1);
				imgY = Math.clamp(imgY, 0, imgHeight - 1);
				imgW = Math.min(imgW, imgWidth - imgX);
				imgH = Math.min(imgH, imgHeight - imgY);
				return new Rectangle(imgX, imgY, imgW, imgH);
			}
		}
	}


	// ==================== Partners & Children Panel ====================
	private JPanel createMainPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10, fillx, wrap 1", "[grow]", "[]10[]10[]10[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		imageButton.setPreferredSize(new Dimension(80, 80));
		imageButton.setIcon(createPlaceholderIcon());
		imageButton.setToolTipText("Left-click to select an image, right-click for options");

		imageButton.addActionListener(e -> selectAndCropImage());

		JPopupMenu imagePopup = new JPopupMenu();
		JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);

		imageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(imageButton, e.getX(), e.getY());
				}
			}
			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					imagePopup.show(imageButton, e.getX(), e.getY());
				}
			}
		});

		panel.add(imageButton, "growx, align center");

		JPanel partner1Panel = createPartnerPanel("Partner 1:", partner1DisplayField,
			partner1NoteList, partner1NoteModel, partner1NoteIds, partner1NoteDisplayMap,
			this::addPartner1Note, this::editPartner1Note, this::deletePartner1Note,
			this::createNewPartner1, this::browsePartner1, this::clearPartner1);
		panel.add(partner1Panel, "growx");

		JPanel partner2Panel = createPartnerPanel("Partner 2:", partner2DisplayField,
			partner2NoteList, partner2NoteModel, partner2NoteIds, partner2NoteDisplayMap,
			this::addPartner2Note, this::editPartner2Note, this::deletePartner2Note,
			this::createNewPartner2, this::browsePartner2, this::clearPartner2);
		panel.add(partner2Panel, "growx");

		JPanel childrenPanel = createChildrenPanel();
		panel.add(childrenPanel, "growx");

		panel.add(restrictionCheckBox, "growx,align left");

		return panel;
	}


	// ==================== Children Panel ====================
	private JPanel createChildrenPanel(){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder("Children"));
		childList.setVisibleRowCount(4);
		childList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// ---- Popup menu ----
		JPopupMenu popup = new JPopupMenu();
		JMenuItem addChildItem = new JMenuItem("Add Existing...");
		JMenuItem newChildItem = new JMenuItem("Create New...");
		JMenuItem editChildItem = new JMenuItem("Edit");
		JMenuItem notesChildItem = new JMenuItem("Notes...");
		JMenuItem deleteChildItem = new JMenuItem("Delete");
		popup.add(addChildItem);
		popup.add(newChildItem);
		popup.addSeparator();
		popup.add(editChildItem);
		popup.add(notesChildItem);
		popup.add(deleteChildItem);

		// ---- Mouse listener ----
		childList.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = childList.locationToIndex(e.getPoint());
					if(index != -1 && !childList.isSelectedIndex(index)){
						childList.setSelectedIndex(index);
					}
					popup.show(childList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = childList.locationToIndex(e.getPoint());
					if(index != -1 && !childList.isSelectedIndex(index)){
						childList.setSelectedIndex(index);
					}
					popup.show(childList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editChild();
				}
			}
		});

		// ---- Keyboard shortcuts ----
		childList.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					createNewChild();
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteChild();
					e.consume();
				}
			}
		});

		// ---- Scroll pane ----
		JScrollPane scrollPane = new JScrollPane(childList);
		scrollPane.setPreferredSize(childList.getPreferredScrollableViewportSize());
		panel.add(scrollPane, "growx,wrap");

		// ---- Enable/disable menu items based on selection ----
		childList.addListSelectionListener(e -> {
			boolean selected = childList.getSelectedIndex() != -1;
			editChildItem.setEnabled(selected);
			notesChildItem.setEnabled(selected);
			deleteChildItem.setEnabled(selected);
		});
		editChildItem.setEnabled(false);
		notesChildItem.setEnabled(false);
		deleteChildItem.setEnabled(false);

		// ---- Actions ----
		addChildItem.addActionListener(e -> addChild());
		newChildItem.addActionListener(e -> createNewChild());
		editChildItem.addActionListener(e -> editChild());
		notesChildItem.addActionListener(e -> {
			int idx = childList.getSelectedIndex();
			if(idx != -1){
				ChildEntry existing = childEntries.get(idx);
				ChildEntry updated = showChildNotesDialog(existing.childId, existing);
				if(updated != null){
					childEntries.set(idx, updated);
					childListModel.set(idx, updated.toString());
				}
			}
		});
		deleteChildItem.addActionListener(e -> deleteChild());

		return panel;
	}


	// ==================== Partner Panel ====================
	private JPanel createPartnerPanel(String label, JTextField displayField,
			JList<String> noteList, DefaultListModel<String> noteModel,
			List<String> noteIds, Map<String, String> noteDisplayMap,
			Runnable addNote, Runnable editNote, Runnable deleteNote,
			Runnable actionNew, Runnable actionBrowse, Runnable actionClear){
		noteList.setVisibleRowCount(4);

		JPanel panel = new JPanel(new MigLayout("insets n n 0 n", "[right]rel[grow][][]", "[]5[]"));
		panel.setBorder(new TitledBorder(label));

		displayField.setEditable(false);
		displayField.setBackground(UIManager.getColor("TextField.background"));

		JPanel idPanel = new JPanel(new MigLayout("ins 0,gap 0,fill", "[grow]", ""));
		idPanel.add(displayField, "grow");
		panel.add(idPanel, "span 3,growx,wrap");

		JPopupMenu popup = new JPopupMenu();
		JMenuItem newItem = new JMenuItem("New");
		JMenuItem browseItem = new JMenuItem("Browse...");
		JMenuItem clearItem = new JMenuItem("Clear");
		JMenuItem notesItem = new JMenuItem("Notes...");
		popup.add(newItem);
		popup.add(browseItem);
		popup.add(clearItem);
		popup.addSeparator();
		popup.add(notesItem);

		displayField.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					popup.show(displayField, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					popup.show(displayField, e.getX(), e.getY());
				}
			}
		});

		newItem.addActionListener(e -> actionNew.run());
		browseItem.addActionListener(e -> actionBrowse.run());
		clearItem.addActionListener(e -> actionClear.run());
		notesItem.addActionListener(e -> addNote.run());

		return panel;
	}

	// ==================== Generic List Panel ====================
	private JPanel createListPanel(String title, JList<String> list, DefaultListModel<String> model,
			Runnable addAction, Runnable editAction, Runnable deleteAction){
		JPanel panel = new JPanel(new MigLayout("fillx"));
		panel.setBorder(new TitledBorder(title));

		list.setVisibleRowCount(4);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add Existing...");
		JMenuItem newItem = new JMenuItem("Create New...");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(addItem);
		popup.add(newItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editAction.run();
				}
			}
		});

		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					createNewItemForList(list, model);
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteAction.run();
					e.consume();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		panel.add(scrollPane, "growx,wrap");

		list.addListSelectionListener(e -> {
			boolean selected = list.getSelectedIndex() != -1;
			editItem.setEnabled(selected);
			deleteItem.setEnabled(selected);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		addItem.addActionListener(e -> addAction.run());
		newItem.addActionListener(e -> createNewItemForList(list, model));
		editItem.addActionListener(e -> editAction.run());
		deleteItem.addActionListener(e -> deleteAction.run());

		return panel;
	}


	// ==================== References Panel ====================
	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 5, fillx, wrap 1", "[grow]", "[]5[]5[]5[]5"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(createListPanel("Events",
				eventList, eventListModel,
				this::addEvent, this::editEvent, this::deleteEvent),
			"growx");

		panel.add(createListPanel("Group Citations",
				groupCitationList, groupCitationListModel,
				this::addGroupCitation, this::editGroupCitation, this::deleteGroupCitation),
			"growx");

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


	// ==================== Partner methods ====================
	private void createNewPartner1(){
		createNewPartner(id -> {
			partner1Id = id;
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null && individualHandler != null)
				partner1DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				partner1DisplayField.setText(id);
		});
	}

	private void browsePartner1(){
		browsePartner(selectedId -> {
			partner1Id = selectedId;
			FLEFRecord rec = model.getRecordById(selectedId);
			if(rec != null && individualHandler != null)
				partner1DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				partner1DisplayField.setText(selectedId);
		});
	}

	private void clearPartner1(){
		partner1Id = null;
		partner1DisplayField.setText("");
	}

	private void createNewPartner2(){
		createNewPartner(id -> {
			partner2Id = id;
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null && individualHandler != null)
				partner2DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				partner2DisplayField.setText(id);
		});
	}

	private void browsePartner2(){
		browsePartner(selectedId -> {
			partner2Id = selectedId;
			FLEFRecord rec = model.getRecordById(selectedId);
			if(rec != null && individualHandler != null)
				partner2DisplayField.setText(individualHandler.getDisplayName(rec));
			else
				partner2DisplayField.setText(selectedId);
		});
	}

	private void clearPartner2(){
		partner2Id = null;
		partner2DisplayField.setText("");
	}

	private void createNewPartner(Consumer<String> onCreated){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = individualHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty())
			onCreated.accept(newId);
	}

	private void browsePartner(Consumer<String> onSelected){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, onSelected);
		dialog.setVisible(true);
	}


	// ==================== Partner note methods ====================
	private String getNoteDisplayName(String id){
		if(noteHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null)
				return noteHandler.getDisplayName(rec);
		}
		return id;
	}

	private void addPartner1Note(){
		addNoteToList(partner1NoteModel, partner1NoteIds, partner1NoteDisplayMap);
	}

	private void editPartner1Note(){
		editNoteFromList(partner1NoteList, partner1NoteModel, partner1NoteIds, partner1NoteDisplayMap);
	}

	private void deletePartner1Note(){
		deleteNoteFromList(partner1NoteList, partner1NoteModel, partner1NoteIds, partner1NoteDisplayMap);
	}

	private void addPartner2Note(){
		addNoteToList(partner2NoteModel, partner2NoteIds, partner2NoteDisplayMap);
	}

	private void editPartner2Note(){
		editNoteFromList(partner2NoteList, partner2NoteModel, partner2NoteIds, partner2NoteDisplayMap);
	}

	private void deletePartner2Note(){
		deleteNoteFromList(partner2NoteList, partner2NoteModel, partner2NoteIds, partner2NoteDisplayMap);
	}

	private void addNoteToList(DefaultListModel<String> defaultListModel, List<String> ids, Map<String, String> displayMap){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		RecordBrowserDialog<?> dialog = new RecordBrowserDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !ids.contains(selectedId)){
				ids.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				displayMap.put(selectedId, display);
				defaultListModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNoteFromList(JList<String> list, DefaultListModel<String> defaultListModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		String id = ids.get(idx);
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		displayMap.put(id, newDisplay);
		defaultListModel.set(idx, newDisplay);
	}

	private void deleteNoteFromList(JList<String> list, DefaultListModel<String> defaultListModel,
		List<String> ids, Map<String, String> displayMap){
		int idx = list.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this note?")) return;
		String removedId = ids.remove(idx);
		displayMap.remove(removedId);
		defaultListModel.remove(idx);
	}

	private void createNewNote(DefaultListModel<String> defaultListModel, List<String> ids, Map<String, String> displayMap){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>(ids);
		JDialog dialog = noteHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		for(FLEFRecord rec : model.getRecordsByType("NOTE")){
			String id = rec.getId();
			if(id != null && !before.contains(id) && !ids.contains(id)){
				ids.add(id);
				String display = getNoteDisplayName(id);
				displayMap.put(id, display);
				defaultListModel.addElement(display);
				break;
			}
		}
	}


	// ==================== Child methods ====================
	private void addChild(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, individualHandler, selectedId -> {
			if(selectedId != null){
				ChildEntry entry = showChildNotesDialog(selectedId, null);
				if(entry != null){
					childEntries.add(entry);
					childListModel.addElement(entry.toString());
				}
			}
		});
		dialog.setVisible(true);
	}

	private void createNewChild(){
		if(individualHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Individual handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = individualHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("INDIVIDUAL")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			ChildEntry entry = showChildNotesDialog(newId, null);
			if(entry != null){
				childEntries.add(entry);
				childListModel.addElement(entry.toString());
			}
			else{
				model.removeRecord(newId);
				JOptionPane.showMessageDialog(getParentFrame(),
					"Child creation cancelled. The individual record has been removed.",
					"Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void editChild(){
		int idx = childList.getSelectedIndex();
		if(idx == -1) return;
		ChildEntry existing = childEntries.get(idx);
		ChildEntry updated = showChildNotesDialog(existing.childId, existing);
		if(updated != null){
			childEntries.set(idx, updated);
			childListModel.set(idx, updated.toString());
		}
	}

	private void deleteChild(){
		int idx = childList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this child?")) return;
		childEntries.remove(idx);
		childListModel.remove(idx);
	}

	private ChildEntry showChildNotesDialog(String childId, ChildEntry existing){
		JDialog dialog = new JDialog(this, "Child Notes", true);
		dialog.setLayout(new MigLayout("fillx"));
		JPanel panel = new JPanel(new MigLayout(StringUtils.EMPTY, "[right]rel[grow]"));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(new JLabel("Child ID:"), "align label");
		panel.add(new JLabel(childId), "growx,wrap");

		DefaultListModel<String> noteModel = new DefaultListModel<>();
		JList<String> noteList = new JList<>(noteModel);
		List<String> noteIds = new ArrayList<>(existing != null? existing.noteIds: new ArrayList<>());
		Map<String, String> displayMap = new HashMap<>();
		for(String id : noteIds){
			displayMap.put(id, getNoteDisplayName(id));
			noteModel.addElement(displayMap.get(id));
		}

		JPanel notePanel = new JPanel(new MigLayout("fillx"));
		JScrollPane scrollPane = new JScrollPane(noteList);
		scrollPane.setPreferredSize(noteList.getPreferredScrollableViewportSize());
		notePanel.add(scrollPane, "growx,wrap");

		JPopupMenu notePopup = new JPopupMenu();
		JMenuItem addNoteItem = new JMenuItem("Add Existing...");
		JMenuItem newNoteItem = new JMenuItem("Create New...");
		JMenuItem editNoteItem = new JMenuItem("Edit");
		JMenuItem deleteNoteItem = new JMenuItem("Delete");
		notePopup.add(addNoteItem);
		notePopup.add(newNoteItem);
		notePopup.addSeparator();
		notePopup.add(editNoteItem);
		notePopup.add(deleteNoteItem);

		noteList.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = noteList.locationToIndex(e.getPoint());
					if(index != -1 && !noteList.isSelectedIndex(index)){
						noteList.setSelectedIndex(index);
					}
					notePopup.show(noteList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = noteList.locationToIndex(e.getPoint());
					if(index != -1 && !noteList.isSelectedIndex(index)){
						noteList.setSelectedIndex(index);
					}
					notePopup.show(noteList, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					editNoteFromList(noteList, noteModel, noteIds, displayMap);
				}
			}
		});
		noteList.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					createNewNote(noteModel, noteIds, displayMap);
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteNoteFromList(noteList, noteModel, noteIds, displayMap);
					e.consume();
				}
			}
		});
		noteList.addListSelectionListener(e -> {
			boolean selected = noteList.getSelectedIndex() != -1;
			editNoteItem.setEnabled(selected);
			deleteNoteItem.setEnabled(selected);
		});
		editNoteItem.setEnabled(false);
		deleteNoteItem.setEnabled(false);

		addNoteItem.addActionListener(e -> addNoteToList(noteModel, noteIds, displayMap));
		newNoteItem.addActionListener(e -> createNewNote(noteModel, noteIds, displayMap));
		editNoteItem.addActionListener(e -> editNoteFromList(noteList, noteModel, noteIds, displayMap));
		deleteNoteItem.addActionListener(e -> deleteNoteFromList(noteList, noteModel, noteIds, displayMap));

		panel.add(notePanel, "growx,wrap");
		dialog.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		dialog.add(btnPanel, BorderLayout.SOUTH);

		final ChildEntry[] result = {null};
		okBtn.addActionListener(e -> {
			result[0] = new ChildEntry(childId, new ArrayList<>(noteIds));
			dialog.dispose();
		});
		cancelBtn.addActionListener(e -> dialog.dispose());

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		return result[0];
	}


	// ==================== Event methods ====================
	private void addEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, eventHandler, selectedId -> {
			if(selectedId != null && !eventIds.contains(selectedId)){
				eventIds.add(selectedId);
				eventListModel.addElement(getEventDisplayName(selectedId));
			}
		});
		dialog.setVisible(true);
	}

	private void editEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1) return;
		String id = eventIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = eventHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		eventListModel.set(idx, getEventDisplayName(id));
	}

	private void deleteEvent(){
		int idx = eventList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this event?")) return;
		eventIds.remove(idx);
		eventListModel.remove(idx);
	}

	private String getEventDisplayName(String id){
		if(eventHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null) return eventHandler.getDisplayName(rec);
		}
		return id;
	}

	private void createNewEvent(){
		if(eventHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Event handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = eventHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("EVENT")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty() && !eventIds.contains(newId)){
			eventIds.add(newId);
			eventListModel.addElement(getEventDisplayName(newId));
		}
	}


	// ==================== Group Citation methods ====================
	private void addGroupCitation(){
		addGroupCitation(null);
	}

	private boolean addGroupCitation(String preSelectedGroupId){
		if(groupCitationHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		GroupCitationDialog dialog;
		if(preSelectedGroupId != null && !preSelectedGroupId.isEmpty()){
			FLEFRecord citationRecord = new FLEFRecord();
			citationRecord.setValue(preSelectedGroupId);
			citationRecord.setLevel(1);
			citationRecord.setTag("GROUP_CITATION");
			dialog = (GroupCitationDialog)groupCitationHandler.createEditDialog(getParentFrame(), model, citationRecord);
		}
		else{
			dialog = (GroupCitationDialog)groupCitationHandler.createNewDialog(getParentFrame(), model);
		}
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("GROUP_CITATION");
				groupCitationRecords.add(citation);
				groupCitationListModel.addElement(getGroupCitationDisplay(citation));
				return true;
			}
		}
		return false;
	}

	private void editGroupCitation(){
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = groupCitationRecords.get(idx);
		if(groupCitationHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GroupCitationDialog dialog = (GroupCitationDialog)groupCitationHandler.createEditDialog(getParentFrame(), model, existing);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord updated = dialog.getCitationRecord();
			if(updated != null){
				groupCitationRecords.set(idx, updated);
				groupCitationListModel.set(idx, getGroupCitationDisplay(updated));
			}
		}
	}

	private void deleteGroupCitation(){
		int idx = groupCitationList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this group citation?")) return;
		groupCitationRecords.remove(idx);
		groupCitationListModel.remove(idx);
	}

	private String getGroupCitationDisplay(FLEFRecord citation){
		String groupId = citation.getValue();
		if(groupId != null){
			FLEFRecord rec = model.getRecordById(groupId);
			if(rec != null && groupHandler != null)
				return groupHandler.getDisplayName(rec);
			return groupId;
		}
		return "[empty]";
	}

	private void createNewGroup(){
		if(groupHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Group handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("GROUP")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = groupHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newGroupId = null;
		for(FLEFRecord rec : model.getRecordsByType("GROUP")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newGroupId = id;
				break;
			}
		}
		if(newGroupId != null && !newGroupId.isEmpty()){
			boolean saved = addGroupCitation(newGroupId);
			if(!saved){
				model.removeRecord(newGroupId);
				JOptionPane.showMessageDialog(getParentFrame(),
					"Group creation cancelled. The group record has been removed.",
					"Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}


	// ==================== Cultural Norm methods ====================
	private void addCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, culturalNormHandler, selectedId -> {
			if(selectedId != null && !culturalNormIds.contains(selectedId)){
				culturalNormIds.add(selectedId);
				culturalNormListModel.addElement(getCulturalNormDisplayName(selectedId));
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
		JDialog dialog = culturalNormHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		culturalNormListModel.set(idx, getCulturalNormDisplayName(id));
	}

	private void deleteCulturalNorm(){
		int idx = culturalNormList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this cultural norm?")) return;
		culturalNormIds.remove(idx);
		culturalNormListModel.remove(idx);
	}

	private String getCulturalNormDisplayName(String id){
		if(culturalNormHandler != null){
			FLEFRecord rec = model.getRecordById(id);
			if(rec != null) return culturalNormHandler.getDisplayName(rec);
		}
		return id;
	}

	private void createNewCulturalNorm(){
		if(culturalNormHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Cultural Norm handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = culturalNormHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("CULTURAL_NORM")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty() && !culturalNormIds.contains(newId)){
			culturalNormIds.add(newId);
			culturalNormListModel.addElement(getCulturalNormDisplayName(newId));
		}
	}


	// ==================== Note methods ====================
	private void addNote(){
		if(noteHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Note handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			getParentFrame(), model, noteHandler, selectedId -> {
			if(selectedId != null && !noteIds.contains(selectedId)){
				noteIds.add(selectedId);
				String display = getNoteDisplayName(selectedId);
				noteDisplayMap.put(selectedId, display);
				noteListModel.addElement(display);
			}
		});
		dialog.setVisible(true);
	}

	private void editNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		String id = noteIds.get(idx);
		FLEFRecord rec = model.getRecordById(id);
		if(rec == null) return;
		JDialog dialog = noteHandler.createEditDialog(getParentFrame(), model, rec);
		dialog.setVisible(true);
		String newDisplay = getNoteDisplayName(id);
		noteDisplayMap.put(id, newDisplay);
		noteListModel.set(idx, newDisplay);
	}

	private void deleteNote(){
		int idx = noteList.getSelectedIndex();
		if(idx == -1) return;
		if(!showConfirm("Confirm", "Remove this note reference?")) return;
		String id = noteIds.remove(idx);
		noteDisplayMap.remove(id);
		noteListModel.remove(idx);
	}

	private void createNewNote(){
		createNewNote(noteListModel, noteIds, noteDisplayMap);
	}


	// ==================== Source Citation methods ====================
	private void addSourceCitation(){
		addSourceCitation(null);
	}

	private boolean addSourceCitation(String preSelectedSourceId){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source Citation handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		FLEFRecord citationRecord;
		if(preSelectedSourceId != null && !preSelectedSourceId.isEmpty()){
			citationRecord = new FLEFRecord();
			citationRecord.setValue(preSelectedSourceId);
			citationRecord.setLevel(1);
			citationRecord.setTag("SOURCE_CITATION");
		}
		else{
			citationRecord = null;
		}
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, citationRecord);
		dialog.setVisible(true);
		if(dialog.isSaved()){
			FLEFRecord citation = dialog.getCitationRecord();
			if(citation != null){
				citation.setLevel(1);
				citation.setTag("SOURCE_CITATION");
				sourceCitationRecords.add(citation);
				sourceCitationListModel.addElement(getSourceCitationDisplay(citation));
				return true;
			}
		}
		return false;
	}

	private void editSourceCitation(){
		int idx = sourceCitationList.getSelectedIndex();
		if(idx == -1) return;
		FLEFRecord existing = sourceCitationRecords.get(idx);
		SourceCitationDialog dialog = new SourceCitationDialog(getParentFrame(), model, existing);
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

	private String getSourceCitationDisplay(FLEFRecord citation){
		String sourceId = citation.getValue();
		if(sourceId != null){
			FLEFRecord rec = model.getRecordById(sourceId);
			if(rec != null && sourceHandler != null)
				return sourceHandler.getDisplayName(rec);
			return sourceId;
		}
		return "[empty]";
	}

	private void createNewSource(){
		if(sourceHandler == null){
			JOptionPane.showMessageDialog(getParentFrame(), "Source handler not registered!", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null) before.add(id);
		}
		JDialog dialog = sourceHandler.createNewDialog(getParentFrame(), model);
		dialog.setVisible(true);
		String newId = null;
		for(FLEFRecord rec : model.getRecordsByType("SOURCE")){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}
		if(newId != null && !newId.isEmpty()){
			addSourceCitation(newId);
		}
	}


	// ==================== createNewItemForList ====================
	private void createNewItemForList(JList<String> list, DefaultListModel<String> model){
		if(list == eventList){
			createNewEvent();
		}
		else if(list == groupCitationList){
			createNewGroup();
		}
		else if(list == culturalNormList){
			createNewCulturalNorm();
		}
		else if(list == noteList){
			createNewNote();
		}
		else if(list == sourceCitationList){
			createNewSource();
		}
	}


	// ==================== Load Data ====================
	@Override
	protected void loadData(){
		setTitle(buildTitle(model, record));

		restrictionCheckBox.setSelected("confidential".equals(FLEFRecordUtils.getChildValue(record, "RESTRICTION")));

		// Partner1
		FLEFRecord p1 = FLEFRecordUtils.findChild(record, "PARTNER1");
		if(p1 != null){
			String id = p1.getValue();
			if(id != null && !id.isEmpty()){
				partner1Id = id;
				FLEFRecord rec = model.getRecordById(id);
				if(rec != null && individualHandler != null)
					partner1DisplayField.setText(individualHandler.getDisplayName(rec));
				else
					partner1DisplayField.setText(id);
			}
			loadPartnerNotes(p1, partner1NoteModel, partner1NoteIds, partner1NoteDisplayMap);
		}

		// Partner2
		FLEFRecord p2 = FLEFRecordUtils.findChild(record, "PARTNER2");
		if(p2 != null){
			String id = p2.getValue();
			if(id != null && !id.isEmpty()){
				partner2Id = id;
				FLEFRecord rec = model.getRecordById(id);
				if(rec != null && individualHandler != null)
					partner2DisplayField.setText(individualHandler.getDisplayName(rec));
				else
					partner2DisplayField.setText(id);
			}
			loadPartnerNotes(p2, partner2NoteModel, partner2NoteIds, partner2NoteDisplayMap);
		}

		// Children
		childEntries.clear();
		childListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CHILD".equals(child.getTag()) && child.getValue() != null){
				String childId = child.getValue();
				List<String> notes = new ArrayList<>();
				for(FLEFRecord noteChild : child.getChildren()){
					if("NOTE".equals(noteChild.getTag()) && noteChild.getValue() != null){
						notes.add(noteChild.getValue());
					}
				}
				childEntries.add(new ChildEntry(childId, notes));
				childListModel.addElement(childEntries.getLast().toString());
			}
		}

		// Events
		eventIds.clear();
		eventListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("EVENT".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				eventIds.add(id);
				eventListModel.addElement(getEventDisplayName(id));
			}
		}

		// Group Citations
		groupCitationRecords.clear();
		groupCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("GROUP_CITATION".equals(child.getTag())){
				groupCitationRecords.add(child);
				groupCitationListModel.addElement(getGroupCitationDisplay(child));
			}
		}

		// Cultural Norms
		culturalNormIds.clear();
		culturalNormListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("CULTURAL_NORM".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				culturalNormIds.add(id);
				culturalNormListModel.addElement(getCulturalNormDisplayName(id));
			}
		}

		// Notes
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

		// Source Citations
		sourceCitationRecords.clear();
		sourceCitationListModel.clear();
		for(FLEFRecord child : record.getChildren()){
			if("SOURCE_CITATION".equals(child.getTag())){
				sourceCitationRecords.add(child);
				sourceCitationListModel.addElement(getSourceCitationDisplay(child));
			}
		}

		// Preferred Image
		FLEFRecord pref = FLEFRecordUtils.findChild(record, "PREFERRED_IMAGE");
		if(pref != null){
			preferredImageId = pref.getValue();
			cropString = FLEFRecordUtils.getChildValue(pref, "CROP");
			updateImageButton(preferredImageId);
		}
		else{
			clearImage();
		}

		// Modification
		modificationPanel.loadFromRecord(record);

		// Conclusion
		FLEFRecord conclusion = FLEFRecordUtils.findChild(record, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);
	}

	private void loadPartnerNotes(FLEFRecord partnerRec, DefaultListModel<String> model,
		List<String> ids, Map<String, String> displayMap){
		model.clear();
		ids.clear();
		displayMap.clear();
		for(FLEFRecord child : partnerRec.getChildren()){
			if("NOTE".equals(child.getTag()) && child.getValue() != null){
				String id = child.getValue();
				ids.add(id);
				String display = getNoteDisplayName(id);
				displayMap.put(id, display);
				model.addElement(display);
			}
		}
	}


	// ==================== Validation ====================
	@Override
	protected boolean validateData(){
		if(!modificationPanel.hasData()){
			JOptionPane.showMessageDialog(this,
				"Modification is required for a family.\nPlease add a CREATION date.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(!modificationPanel.validateRequiredFields()){
			return false;
		}
		return (!conclusionPanel.hasData() || conclusionPanel.validateRequiredFields());
	}


	// ==================== Save ====================
	@Override
	protected void saveRecord(){
		record.getChildren().clear();

		// RESTRICTION
		FLEFRecordUtils.updateChildValue(record, "RESTRICTION",
			restrictionCheckBox.isSelected()? "confidential": null);

		// PARTNER1
		if(partner1Id != null && !partner1Id.isEmpty()){
			FLEFRecord p1 = new FLEFRecord();
			p1.setLevel(1);
			p1.setTag("PARTNER1");
			p1.setValue(partner1Id);
			record.addChild(p1);
			for(String noteId : partner1NoteIds){
				FLEFRecordUtils.addChild(p1, "NOTE", 2, noteId);
			}
		}

		// PARTNER2
		if(partner2Id != null && !partner2Id.isEmpty()){
			FLEFRecord p2 = new FLEFRecord();
			p2.setLevel(1);
			p2.setTag("PARTNER2");
			p2.setValue(partner2Id);
			record.addChild(p2);
			for(String noteId : partner2NoteIds){
				FLEFRecordUtils.addChild(p2, "NOTE", 2, noteId);
			}
		}

		// CHILDREN
		for(ChildEntry entry : childEntries){
			FLEFRecord child = new FLEFRecord();
			child.setLevel(1);
			child.setTag("CHILD");
			child.setValue(entry.childId);
			record.addChild(child);
			for(String noteId : entry.noteIds){
				FLEFRecordUtils.addChild(child, "NOTE", 2, noteId);
			}
		}

		// EVENTS
		for(String id : eventIds){
			FLEFRecordUtils.addChild(record, "EVENT", 1, id);
		}

		// GROUP CITATIONS
		for(FLEFRecord citation : groupCitationRecords){
			citation.setLevel(1);
			citation.setTag("GROUP_CITATION");
			record.addChild(citation);
		}

		// CULTURAL NORMS
		for(String id : culturalNormIds){
			FLEFRecordUtils.addChild(record, "CULTURAL_NORM", 1, id);
		}

		// NOTES
		for(String id : noteIds){
			FLEFRecordUtils.addChild(record, "NOTE", 1, id);
		}

		// SOURCE CITATIONS
		for(FLEFRecord citation : sourceCitationRecords){
			citation.setLevel(1);
			citation.setTag("SOURCE_CITATION");
			record.addChild(citation);
		}

		// PREFERRED IMAGE
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			FLEFRecord pref = new FLEFRecord();
			pref.setLevel(1);
			pref.setTag("PREFERRED_IMAGE");
			pref.setValue(preferredImageId);
			record.addChild(pref);
			if(cropString != null && !cropString.isEmpty()){
				FLEFRecordUtils.updateChildValue(pref, "CROP", cropString);
			}
		}

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		// CONCLUSION
		if(conclusionPanel.hasData()){
			FLEFRecord conclusion = conclusionPanel.saveToRecord(null);
			if(conclusion != null){
				conclusion.setLevel(1);
				conclusion.setTag("CONCLUSION");
				record.addChild(conclusion);
			}
		}

		if(isNew){
			model.addRecord(record);
		}
		dispose();
	}


	// ==================== Overrides ====================
	@Override
	protected FLEFRecord createNewRecord(){
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("FAMILY");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId(){
		return FLEFRecordUtils.generateNewId(model, "FAMILY", "F");
	}

	private Frame getParentFrame(){
		Container parent = getParent();
		while(parent != null && !(parent instanceof Frame)){
			parent = parent.getParent();
		}
		return (Frame)parent;
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){
		}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Test Family Dialog");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new FlowLayout());
			frame.setSize(400, 150);
			frame.setLocationRelativeTo(null);

			JButton btn = new JButton("New Family");
			btn.addActionListener(e -> {
				FamilyDialog dialog = FamilyDialog.createNew(frame, model);
				dialog.setVisible(true);
				System.out.println("Family saved.");
			});

			frame.add(btn);
			frame.setVisible(true);
		});
	}

}
