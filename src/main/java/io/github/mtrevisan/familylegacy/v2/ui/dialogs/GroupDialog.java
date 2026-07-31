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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.CulturalNormListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.GeneralRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.MemberRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
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
import java.util.List;


/* ONGOING */
/**
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupRecord {
 *   id: LocalID
 *   name*: NameStructure
 *   type?: enum { family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe } | Text
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   citation*: SourceCitation
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropCoord
 *   }
 *   restriction?: RestrictionStructure
 *   conclusion*: Xref&lt;ConclusionRecord&gt;
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class GroupDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_URI = "URI";
	private static final String TAG_CROP = "CROP";
	private static final String TAG_RESTRICTION = "RESTRICTION";
	private static final String TAG_CONCLUSION = "CONCLUSION";

	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";
	private static final String TAG_INDIVIDUAL = "INDIVIDUAL";
	private static final String TAG_OBJECT = "OBJECT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_SUBJECT = "SUBJECT";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new GroupHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final NameListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;

	private final JButton preferredImageButton = new JButton();
	private String preferredImageId;
	private String preferredImageCrop;

	private final RestrictionPanel restrictionPanel;
	private final ConclusionPanel conclusionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final MemberRelationshipListPanel memberPanel;
	private final GeneralRelationshipListPanel relationshipPanel;


	public static GroupDialog createNew(final Dialog parent, final FLEFModel model){
		return new GroupDialog(parent, model, null);
	}

	public static GroupDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new GroupDialog(parent, model, record);
	}


	private GroupDialog(Dialog parent, FLEFModel model, FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupHandler.TYPE));

		namePanel = new NameListPanel(TAG_NAME, this, model);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{StringUtils.EMPTY, "family", "household",
			"neighbourhood", "fraternity", "club", "research group", "literary society", "association", "organisation",
			"tribe"});
		culturalNormPanel = new CulturalNormListPanel(TAG_CULTURAL_NORM, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		conclusionPanel = new ConclusionPanel(TAG_CONCLUSION, model, this);
		modificationPanel = new ModificationPanel(this);
		memberPanel = new MemberRelationshipListPanel(this, model, this::getGroupId);
		relationshipPanel = new GeneralRelationshipListPanel(null, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		bindingManager.bind(typeCombo);

		setLayout(new MigLayout("fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]5[]10[]"));

		// preferred image
		preferredImageButton.setPreferredSize(new Dimension(80, 80));
		preferredImageButton.setIcon(createPlaceholderIcon());
		preferredImageButton.setToolTipText("Left-click to select an image, right-click for options");
		preferredImageButton.addActionListener(e -> selectAndCropImage());
		final JPopupMenu imagePopup = new JPopupMenu();
		final JMenuItem clearImageMenuItem = new JMenuItem("Clear");
		clearImageMenuItem.addActionListener(e -> clearImage());
		imagePopup.add(clearImageMenuItem);
		preferredImageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.isPopupTrigger())
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				if(e.isPopupTrigger())
					imagePopup.show(preferredImageButton, e.getX(), e.getY());
			}
		});
		panel.add(preferredImageButton, "growx, align center");

		// type
		final JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// names
		panel.add(namePanel, "growx");

		// members
		panel.add(memberPanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));
		panel.add(relationshipPanel, "growx");
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}

	private Icon createPlaceholderIcon(){
		final BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString("[No img]", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

	private void selectAndCropImage(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final String[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			this, model, sourceHandler, selectedId -> result[0] = selectedId);
		dialog.setVisible(true);

		final String sourceId = result[0];
		if(sourceId == null)
			return;

		final BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(this,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final ImageCropDialog cropDialog = new ImageCropDialog(this, image);
		cropDialog.setVisible(true);

		final Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			preferredImageId = sourceId;
			preferredImageCrop = cropRect.x + StringUtils.SPACE + cropRect.y + StringUtils.SPACE + cropRect.width + StringUtils.SPACE + cropRect.height;
			updateImageButton(sourceId);
		}
	}

	private BufferedImage loadImageFromSource(final String sourceId){
		final FLEFRecord source = model.getRecordById(sourceId);
		if(source == null)
			return null;

		final String filePath = FLEFRecordHelper.getChildValue(source, TAG_URI);
		if(filePath == null || filePath.isEmpty())
			return null;

		try{
			final File file = new File(filePath);
			if(!file.exists())
				return null;
			return ImageIO.read(file);
		}
		catch(final IOException e){
			e.printStackTrace();

			return null;
		}
	}

	private void updateImageButton(final String sourceId){
		final BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			final Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			preferredImageButton.setIcon(new ImageIcon(scaled));
		}
		else
			preferredImageButton.setIcon(createPlaceholderIcon());
	}

	private void clearImage(){
		preferredImageId = null;
		preferredImageCrop = null;
		preferredImageButton.setIcon(createPlaceholderIcon());
	}

	@Override
	protected void loadData(){
		bindingManager.load(record);

		// NAME_STRUCTURE
		final List<FLEFRecord> names = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if(TAG_NAME.equals(child.getTag()))
				names.add(child);
		namePanel.setItems(names);

		// Restriction
		restrictionPanel.load(record);

		// Conclusion
		conclusionPanel.load(record);

		// Clear all lists
		memberPanel.load(record);

		// Process children for lists
		final List<FLEFRecord> culturalNorms = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren()){
			final String tag = child.getTag();

			//FIXME
			/*if(TAG_RELATIONSHIP.equals(tag)){
				otherRelationshipRecords.add(child);
				relationshipListModel.addElement(getRelationshipDisplay(child));
				if(isMemberRelationship(child)){
					memberRelationshipRecords.add(child);
					memberListModel.addElement(getRelationshipDisplay(child));
				}
			}
			else*/ if(TAG_CULTURAL_NORM.equals(tag) && child.getValue() != null)
				culturalNorms.add(child);
		}

		culturalNormPanel.setItems(culturalNorms);
		notePanel.load(record);
		sourcePanel.load(record);

		// Preferred Image
		final FLEFRecord pref = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE);
		if(pref != null){
			preferredImageId = pref.getValue();
			preferredImageCrop = FLEFRecordHelper.getChildValue(pref, TAG_CROP);
			updateImageButton(preferredImageId);
		}
		else
			clearImage();

		// Modification
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(restrictionPanel.hasData() && !restrictionPanel.validateData())
			return false;

		if(conclusionPanel.hasData() && !conclusionPanel.validateData())
			return false;

		return true;
	}

	@Override
	protected void saveData(){
		// TYPE is saved by binding manager
		bindingManager.save(record);

		// NAME_STRUCTURE
		for(final FLEFRecord nameRec : namePanel.getItems()){
			nameRec.setTag(TAG_NAME);
			record.addChild(nameRec);
		}

		// CULTURAL_NORM
		culturalNormPanel.save(record);

		// NOTE
		notePanel.save(record);

		// SOURCE_CITATION
		sourcePanel.save(record);

		// PREFERRED_IMAGE
		if(preferredImageId != null && !preferredImageId.isEmpty()){
			final FLEFRecord pref = FLEFRecord.createChildWithValue(TAG_PREFERRED_IMAGE, preferredImageId);
			record.addChild(pref);
			if(preferredImageCrop != null && !preferredImageCrop.isEmpty())
				FLEFRecordHelper.updateChildValue(pref, TAG_CROP, preferredImageCrop);
		}

		// RESTRICTION
		if(restrictionPanel.hasData())
			restrictionPanel.save(record);

		// CONCLUSION
		if(conclusionPanel.hasData()){
			final FLEFRecord conclusion = conclusionPanel.save(null);
			if(conclusion != null){
				conclusion.setTag(TAG_CONCLUSION);
				record.addChild(conclusion);
			}
		}

		// RELATIONSHIP
		FLEFRecordHelper.removeChildren(record, TAG_RELATIONSHIP);
		memberPanel.save(record);
		relationshipPanel.save(record);

		// MODIFICATION
		modificationPanel.save(record);
	}

	private String getGroupId(){
		return (record != null? record.getId(): null);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final GroupDialog dialog = GroupDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
