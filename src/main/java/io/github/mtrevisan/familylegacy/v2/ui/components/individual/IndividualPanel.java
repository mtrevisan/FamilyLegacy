/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.components.individual;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures.NoteStructureDialog;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;


/**
 * A panel that displays an individual's information (name, birth/death, photo) in a genealogical box.
 */
public class IndividualPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";
	private static final String[] NO_NAME = {NO_DATA, NO_DATA};

	private static final int SECONDARY_MAX_HEIGHT = 65;

	// Colors
	private static final Color BACKGROUND_COLOR_NO_INDIVIDUAL = Color.WHITE;
	private static final Color BACKGROUND_COLOR_FADE_TO = Color.WHITE;
	private static final Color BACKGROUND_COLOR_INDIVIDUAL = new Color(221, 221, 221);
	private static final Color BORDER_COLOR = new Color(165, 165, 165);
	private static final Color BORDER_COLOR_SHADOW = new Color(131, 131, 131, 130);
	private static final Color BORDER_COLOR_SHADOW_SELECTED = Color.BLACK;
	private static final Color BIRTH_DEATH_AGE_COLOR = new Color(110, 110, 110);
	private static final Color IMAGE_LABEL_BORDER_COLOR = Color.WHITE;

	// Dimensions
	//double values for Horizontal and Vertical radius of corner arcs
	private static final Dimension ARCS = new Dimension(10, 10);
	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 14);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 11);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	// UI components
	private final JLabel individualNameLabel = new JLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel imageLabel = new JLabel();

	// Menu items
	private final JMenuItem editIndividualItem = new JMenuItem("Edit Individual…", 'E');
	private final JMenuItem addIndividualItem = new JMenuItem("Add Individual…", 'A');
	private final JMenuItem linkIndividualItem = new JMenuItem("Link Individual…", 'L');
	private final JMenuItem removeIndividualItem = new JMenuItem("Remove Individual", 'R');
	private final JMenuItem unlinkFromParentGroupItem = new JMenuItem("Unlink from parent Group", 'U');
	private final JMenuItem addToNewSiblingGroupItem = new JMenuItem("Add to new sibling Group…", 'S');
	private final JMenuItem unlinkFromSiblingGroupItem = new JMenuItem("Unlink from sibling Group", 'G');

	// State
	private final BoxPanelType boxType;

	private FLEFRecord individual;
	private final FLEFModel model;
	private IndividualData data;
	private String preferredImageKey;
	private IndividualListener listener;


	public static IndividualPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new IndividualPanel(boxType, model);
	}


	private IndividualPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		initComponents();

//		attachPopupMenu();

//		installMouseListeners();
	}

	private void initComponents(){
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		imageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		final double shrinkFactor = (isPrimaryBox()? 1.: 2.);
		setPreferredSize(imageLabel, 48., IMAGE_ASPECT_RATIO, shrinkFactor);

		setLayout(new MigLayout("ins 7", "[grow]0[]", "[]0[]10[]"));

		int shrink = (int)Math.round(PREFERRED_IMAGE_WIDTH + 21);
		add(individualNameLabel, "cell 0 0,top,width ::100%-" + shrink + ",hidemode 3");
		add(imageLabel, "cell 1 0 1 3,top");
		add(infoLabel, "cell 0 2");

		setOpaque(false);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			final int panelHeight = getHeight();
			final int panelWidth = getWidth();

			final Color startColor = getBackgroundColor();
			if(individual != null){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(individual == null){
				final Stroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					10.f, new float[]{5.f}, 0.f);
				graphics2D.setStroke(dashedStroke);
			}
			graphics2D.drawRoundRect(0, 0,
				panelWidth - 1, panelHeight - 1,
				ARCS.width, ARCS.height);


			//for test purposes
//			final Point enterPoint = getPaintingEnterPoint();
//			graphics2D.setColor(Color.RED);
//			graphics2D.drawLine(enterPoint.x - 10, enterPoint.y - 10, enterPoint.x + 10, enterPoint.y + 10);
//			graphics2D.drawLine(enterPoint.x + 10, enterPoint.y - 10, enterPoint.x - 10, enterPoint.y + 10);
//			graphics2D.setColor(Color.BLACK);


			graphics2D.dispose();
		}
	}

	private Color getBackgroundColor(){
		return (individual == null? BACKGROUND_COLOR_NO_INDIVIDUAL: BACKGROUND_COLOR_INDIVIDUAL);
	}

	private static void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio,
			final double shrinkFactor){
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	private boolean isPrimaryBox(){
		return (boxType == BoxPanelType.PRIMARY);
	}

	public final Point getPaintingEnterPoint(){
		return new Point(getX() + getWidth() / 2, getY());
	}


	public void withIndividual(final String individualId){
		individual = model.getRecordById(individualId);

		final Dimension size = (isPrimaryBox()
			? new Dimension(260, 90)
			: new Dimension(170, SECONDARY_MAX_HEIGHT));
		setPreferredSize(size);

		updateIndividualData();
	}

	private void updateIndividualData(){
		data = new IndividualData(individual, boxType, model);


		Font font = (isPrimaryBox()? FONT_PRIMARY: FONT_SECONDARY);
		final Font infoFont = deriveInfoFont(font);
		if(!isPrimaryBox()){
			//add underline to mark this person as eligible for primary position
			@SuppressWarnings("unchecked")
			final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
			font = font.deriveFont(attributes);
		}
		final Cursor cursor = Cursor.getPredefinedCursor(isPrimaryBox()? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR);
		individualNameLabel.setFont(font);
		individualNameLabel.setCursor(cursor);
		infoLabel.setFont(infoFont);

		individualNameLabel.setText(data.getIndividualNameText());
		individualNameLabel.setToolTipText(data.getIndividualNameTooltip());

		infoLabel.setText(data.getInfoText());
		infoLabel.setToolTipText(data.getInfoTooltip());

		// Set the default image/placeholder
		imageLabel.setIcon(data.getIndividualImage());

		final boolean hasData = (individual != null && !individual.isEmpty());
		if(hasData){
			// Register the current key on the panel and start the asynchronous
			preferredImageKey = data.getPreferredImageKey();
			data.loadPreferredImageAsync((key, image) -> {
				if(image != null && Objects.equals(preferredImageKey, key))
					imageLabel.setIcon(image);
			});
		}
		else
			preferredImageKey = null;

		individualNameLabel.setVisible(hasData);
		infoLabel.setVisible(hasData);
		imageLabel.setVisible(hasData);

//		refresh(ActionCommand.ACTION_COMMAND_PERSON);
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

/*	private void updateIndividualUI(){
		// Load image asynchronously
		if(hasData && displayInfo.getPreferredImageUri() != null){
			ImageLoader.loadImageAsync(
				displayInfo.getPreferredImageUri(),
				displayInfo.getCrop(),
				imageLabel.getPreferredSize(),
				icon -> {
					if(icon != null)
						imageLabel.setIcon(icon);
					else
						imageLabel.setIcon(createPlaceholderIcon());
				}
			);
		}
		else
			imageLabel.setIcon(createPlaceholderIcon());

		setCursor(hasData && boxType == BoxPanelType.SECONDARY
			? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
			: Cursor.getDefaultCursor());

		revalidate();
		repaint();
	}

	// ------------------------------------------------------------------------
	// Event handling
	// ------------------------------------------------------------------------

	private void installMouseListeners(){
		if(boxType == BoxPanelType.SECONDARY){
			MouseAdapter focusAdapter = new MouseAdapter(){
				@Override
				public void mouseClicked(MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && listener != null){
						listener.onIndividualFocus(IndividualPanel.this);
					}
				}
			};
			individualNameLabel.addMouseListener(focusAdapter);
		}

		// Double-click to edit
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
					&& !displayInfo.isEmpty() && listener != null){
					listener.onIndividualEdit(IndividualPanel.this);
				}
			}
		});

		// Image click
		imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		imageLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(SwingUtilities.isLeftMouseButton(e)){
					if(displayInfo.getPreferredImageUri() == null){
						if(listener != null) listener.onIndividualAddPreferredImage(IndividualPanel.this);
					}
					else{
						if(listener != null) listener.onIndividualEditPreferredImage(IndividualPanel.this);
					}
				}
				else if(SwingUtilities.isRightMouseButton(e) && !displayInfo.isEmpty()){
					int response = JOptionPane.showConfirmDialog(IndividualPanel.this,
						"Remove preferred photo?", "Warning", JOptionPane.YES_NO_OPTION);
					if(response == JOptionPane.YES_OPTION){
						// This would need to update the FLEF model and reload.
						// In a real implementation, we'd call a service to remove the photo.
						// For now, we just clear the local display.
						displayInfo = new IndividualDisplayInfo.Builder()
							.preferredImageUri(null)
							.crop(null)
							.build();
						updateUI();
					}
				}
			}
		});
	}

	private void attachPopupMenu(){
		JPopupMenu popup = new JPopupMenu();

		editIndividualItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualEdit(IndividualPanel.this);
		});
		popup.add(editIndividualItem);

		addIndividualItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualAdd(IndividualPanel.this);
		});
		popup.add(addIndividualItem);

		linkIndividualItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualLink(IndividualPanel.this);
		});
		popup.add(linkIndividualItem);

		removeIndividualItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualRemove(IndividualPanel.this);
		});
		popup.add(removeIndividualItem);

		popup.addSeparator();
		unlinkFromParentGroupItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualUnlinkFromParentGroup(IndividualPanel.this);
		});
		popup.add(unlinkFromParentGroupItem);

		popup.addSeparator();
		addToNewSiblingGroupItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualAddToSiblingGroup(IndividualPanel.this);
		});
		popup.add(addToNewSiblingGroupItem);

		unlinkFromSiblingGroupItem.addActionListener(e -> {
			if(listener != null) listener.onIndividualUnlinkFromSiblingGroup(IndividualPanel.this);
		});
		popup.add(unlinkFromSiblingGroupItem);

		addMouseListener(new PopupMouseAdapter(popup, this));
	}
*/

	// ------------------------------------------------------------------------
	// Main for testing
	// ------------------------------------------------------------------------

	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "I1";

		final String content;
		try(final InputStream is = NoteStructureDialog.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		SwingUtilities.invokeLater(() -> {
			IndividualPanel panel = IndividualPanel.create(BoxPanelType.PRIMARY, model);
			panel.withIndividual(recordId);
//			panel.setListener(new IndividualListener(){
//				// implement dummy methods
//				@Override
//				public void onIndividualFocus(IndividualPanel p){
//					System.out.println("focus");
//				}
//
//				@Override
//				public void onIndividualEdit(IndividualPanel p){
//					System.out.println("edit");
//				}
//
//				@Override
//				public void onIndividualAdd(IndividualPanel p){
//					System.out.println("add");
//				}
//
//				@Override
//				public void onIndividualLink(IndividualPanel p){
//					System.out.println("link");
//				}
//
//				@Override
//				public void onIndividualRemove(IndividualPanel p){
//					System.out.println("remove");
//				}
//
//				@Override
//				public void onIndividualUnlinkFromParentGroup(IndividualPanel p){
//					System.out.println("unlink parent");
//				}
//
//				@Override
//				public void onIndividualAddToSiblingGroup(IndividualPanel p){
//					System.out.println("add sibling");
//				}
//
//				@Override
//				public void onIndividualUnlinkFromSiblingGroup(IndividualPanel p){
//					System.out.println("unlink sibling");
//				}
//
//				@Override
//				public void onIndividualAddPreferredImage(IndividualPanel p){
//					System.out.println("add image");
//				}
//
//				@Override
//				public void onIndividualEditPreferredImage(IndividualPanel p){
//					System.out.println("edit image");
//				}
//			});

			JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
