package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.utils.ImageItem;
import io.github.mtrevisan.familylegacy.v2.ui.utils.SelectedImage;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Dialog used to select an image and optionally define a crop area.
 */
public class ImageSelectionDialog extends JDialog{

	private SelectedImage selectedImage;

	private final DefaultListModel<ImageItem> listModel = new DefaultListModel<>();
	private final JList<ImageItem> imageList = new JList<>(listModel);

	private final JLabel previewLabel = new JLabel();


	public ImageSelectionDialog(final Frame parent, final List<ImageItem> availableImages){
		super(parent, "Select Image", true);

		if(availableImages != null){
			for(ImageItem image : availableImages){
				listModel.addElement(image);
			}
		}

		initComponents();

		setSize(900, 600);
		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(5, 5));

		// Configure image list
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		imageList.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting()){
				updatePreview();
			}
		});

		// Configure preview area
		previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		previewLabel.setVerticalAlignment(SwingConstants.CENTER);

		JScrollPane previewScrollPane = GUIHelper.createScrollPane(previewLabel);

		// Split pane: image list on the left, preview on the right
		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(imageList),
			previewScrollPane
		);
		splitPane.setResizeWeight(0.30);

		add(splitPane, BorderLayout.CENTER);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton selectButton = new JButton("Select");
		JButton cancelButton = new JButton("Cancel");

		buttonPanel.add(selectButton);
		buttonPanel.add(cancelButton);

		add(buttonPanel, BorderLayout.SOUTH);

		selectButton.addActionListener(e -> confirmSelection());
		cancelButton.addActionListener(e -> {
			selectedImage = null;
			dispose();
		});

		// Double-click selects image
		imageList.addMouseListener(new java.awt.event.MouseAdapter(){
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e){
				if(e.getClickCount() == 2){
					confirmSelection();
				}
			}
		});

		// Select first image if available
		if(!listModel.isEmpty()){
			imageList.setSelectedIndex(0);
		}
	}


	/**
	 * Updates image preview.
	 */
	private void updatePreview(){
		ImageItem item = imageList.getSelectedValue();

		if(item == null){
			previewLabel.setIcon(null);
			return;
		}

		BufferedImage image = item.getImage();

		if(image == null){
			previewLabel.setIcon(null);
			return;
		}

		int maxWidth = 500;
		int maxHeight = 500;

		double scale = Math.min(
			(double)maxWidth / image.getWidth(),
			(double)maxHeight / image.getHeight()
		);

		scale = Math.min(scale, 1.0);

		int width = (int)(image.getWidth() * scale);
		int height = (int)(image.getHeight() * scale);

		Image preview = image.getScaledInstance(
			width,
			height,
			Image.SCALE_SMOOTH
		);

		previewLabel.setIcon(new ImageIcon(preview));
	}


	/**
	 * Opens crop dialog and stores selection.
	 */
	private void confirmSelection(){
		ImageItem item = imageList.getSelectedValue();

		if(item == null){
			JOptionPane.showMessageDialog(
				this,
				"Please select an image.",
				"No Selection",
				JOptionPane.WARNING_MESSAGE
			);
			return;
		}

		Rectangle crop = null;

		BufferedImage image = item.getImage();

		if(image != null){
			ImageCropDialog cropDialog =
				new ImageCropDialog(
					(Frame)getOwner(),
					image
				);

			cropDialog.setVisible(true);

			crop = cropDialog.getCrop();
		}

		selectedImage = new SelectedImage(
			item.getResourceUri(),
			image,
			crop
		);

		dispose();
	}


	/**
	 * Returns the selected image information.
	 */
	public SelectedImage getSelectedImage(){
		return selectedImage;
	}

}