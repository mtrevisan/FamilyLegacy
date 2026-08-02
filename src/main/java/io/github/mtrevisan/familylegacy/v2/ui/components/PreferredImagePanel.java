package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.GenericSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ImageCropDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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


/**
 * Panel for selecting and managing a preferred image associated with a record.
 * The image is referenced via a Source record, and can be cropped.
 */
public class PreferredImagePanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6086547520717314054L;


	private static final String TAG_URI = "URI";
	private static final String TAG_CROP = "CROP";
	private static final String SPACE = " ";


	private final Dialog parent;
	private final FLEFModel model;

	private final String path;

	private final JButton imageButton;
	private String imageId;
	private String cropString;


	/**
	 * Constructs a PreferredImagePanel.
	 *
	 * @param parent the parent dialog (for showing modal dialogs)
	 * @param model  the FLEF model
	 */
	public PreferredImagePanel(final String path, final Dialog parent, final FLEFModel model){
		this.parent = parent;
		this.model = model;

		this.path = path;

		this.imageButton = new JButton();

		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx", "[grow,align center]"));

		imageButton.setPreferredSize(new Dimension(80, 80));
		imageButton.setIcon(createPlaceholderIcon());
		imageButton.setToolTipText("Left-click to select an image, right-click for options");

		// Left click: select and crop
		imageButton.addActionListener(e -> selectAndCropImage());

		// Right click: popup menu with "Clear"
		final JPopupMenu popup = new JPopupMenu();
		final JMenuItem clearItem = new JMenuItem("Clear");
		clearItem.addActionListener(e -> clearImage());
		popup.add(clearItem);

		imageButton.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.isPopupTrigger())
					popup.show(imageButton, e.getX(), e.getY());
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				if(e.isPopupTrigger())
					popup.show(imageButton, e.getX(), e.getY());
			}
		});

		add(imageButton, "growx");
	}

	/**
	 * Loads the preferred image data from the given record.
	 *
	 * @param record the record containing the PREFERRED_IMAGE child
	 */
	public void load(final FLEFRecord record){
		final FLEFRecord pref = FLEFRecordHelper.findChild(record, path);
		if(pref != null){
			imageId = pref.getValue();
			cropString = FLEFRecordHelper.getChildValue(pref, TAG_CROP);
			updateImageButton(imageId);
		}
		else
			clearImage();
	}

	/**
	 * Saves the preferred image data to the given record.
	 * If no image is selected, does nothing.
	 *
	 * @param record the record to save into
	 */
	public void save(final FLEFRecord record){
		FLEFRecordHelper.removeChildren(record, path);

		if(imageId != null && !imageId.isEmpty()){
			final FLEFRecord pref = FLEFRecordHelper.findChild(record, path);
			pref.setValue(imageId);
			if(cropString != null && !cropString.isEmpty())
				FLEFRecordHelper.updateChildValue(pref, TAG_CROP, cropString);

			record.addChild(pref);
		}
	}

	/**
	 * Returns whether an image is currently selected.
	 *
	 * @return {@code true} if an image is selected, {@code false} otherwise
	 */
	public boolean hasImage(){
		return (imageId != null && !imageId.isEmpty());
	}

	private void selectAndCropImage(){
		final RecordTypeHandler<?> sourceHandler = HandlerRegistry.getHandler(SourceHandler.TYPE);
		final String[] result = {null};
		final GenericSelectionDialog<?> dialog = new GenericSelectionDialog<>(
			parent, model, sourceHandler, selectedId -> result[0] = selectedId);
		dialog.setVisible(true);

		final String sourceId = result[0];
		if(sourceId == null)
			return;

		final BufferedImage image = loadImageFromSource(sourceId);
		if(image == null){
			JOptionPane.showMessageDialog(parent,
				"Could not load image from the selected source.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		final ImageCropDialog cropDialog = new ImageCropDialog(parent, image);
		cropDialog.setVisible(true);

		final Rectangle cropRect = cropDialog.getCrop();
		if(cropRect != null){
			imageId = sourceId;
			cropString = cropRect.x + SPACE + cropRect.y + SPACE + cropRect.width + SPACE + cropRect.height;
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
		catch(final IOException ioe){
			ioe.printStackTrace();

			return null;
		}
	}

	private void updateImageButton(final String sourceId){
		final BufferedImage img = loadImageFromSource(sourceId);
		if(img != null){
			final Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
			imageButton.setIcon(new ImageIcon(scaled));
		}
		else
			imageButton.setIcon(createPlaceholderIcon());
	}

	private void clearImage(){
		imageId = null;
		cropString = null;
		imageButton.setIcon(createPlaceholderIcon());
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

}
