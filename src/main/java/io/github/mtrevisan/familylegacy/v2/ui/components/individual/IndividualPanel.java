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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.TwoLineLabel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.PartnersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.partners.Side;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.RelationClipboard;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * A panel that displays an individual's information (name, birth/death, photo) in a genealogical box.
 */
public class IndividualPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -300117824230109203L;


	private static final String NO_DATA = "?";

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
	private static final int PREFERRED_IMAGE_WIDTH = 48;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final Dimension BOX_DIMENSION_PRIMARY = new Dimension(270, 90);
	private static final Dimension BOX_DIMENSION_SECONDARY = new Dimension(130, 65);

	private static final int NAME_IMAGE_GAP = 5;

	// Fonts
	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 15);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 12);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	private static final String TAG_TYPE = "type";
	private static final String TAG_TARGET = "target";
	private static final String TAG_SEX = "sex";

	private static final String ENUM_TYPE_ENDS_WITH_CHILD = "child";
	private static final String ENUM_SEX_MALE = "male";
	private static final String ENUM_SEX_FEMALE = "female";


	// UI components
	private final TwoLineLabel individualNameLabel = new TwoLineLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel imageLabel = new JLabel();

	// Menu items
	private final JMenuItem editIndividualItem = new JMenuItem("Edit Individual…", 'E');
	private final JMenuItem addIndividualItem = new JMenuItem("Add Individual…", 'A');
	private final JMenuItem linkIndividualItem = new JMenuItem("Link Individual…", 'L');
	private final JMenuItem moveIndividualItem = new JMenuItem("Move Individual", 'M');
	private final JMenuItem pasteIndividualItem = new JMenuItem("Paste Individual", 'P');
	private final JMenuItem removeIndividualItem = new JMenuItem("Remove Individual", 'R');
	private final JMenuItem unlinkRelationshipsItem = new JMenuItem("Unlink Relationships…", 'U');

	// State
	private FLEFRecord father;
	private FLEFRecord mother;
	private final BoxPanelType boxType;

	private final FLEFModel model;

	private IndividualData data;

	private String preferredImageKey;

	// Listener
	private IndividualListener listener;


	public static IndividualPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new IndividualPanel(boxType, model);
	}


	private IndividualPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		initComponents();

		installMouseListeners();
	}


	private void initComponents(){
		infoLabel.setForeground(BIRTH_DEATH_AGE_COLOR);

		imageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		final double shrinkFactor = (isPrimaryBox()? 1.: 2.);
		setPreferredSize(imageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO, shrinkFactor);

		setBoxPreferredSize();

		setLayout(new MigLayout("ins 7,gapx 5", "[grow,fill][grow 0,shrink 0]", "[]0[]10[]"));

		final int imageWidth = (int)(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		add(individualNameLabel, "cell 0 0,top,growx,width ::100%-" + imageWidth + ",hidemode 3");
		add(imageLabel, (isPrimaryBox()? "cell 1 0 1 3,top": "cell 1 0,top"));
		add(infoLabel, (isPrimaryBox()? "cell 0 2,growx": "cell 0 2 2 1,growx"));

		setOpaque(false);
	}

	@Override
	protected final void paintComponent(final Graphics g){
		if(g instanceof Graphics2D){
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			final int panelHeight = getHeight();
			final int panelWidth = getWidth();

			final Color startColor = getBackgroundColor();
			if(data != null){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				g2.setPaint(gradientPaint);
			}
			else
				g2.setColor(startColor);
			g2.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width, ARCS.height);

			g2.setColor(BORDER_COLOR);
			if(data == null){
				final Stroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
					10.f, new float[]{5.f}, 0.f);
				g2.setStroke(dashedStroke);
			}
			g2.drawRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width, ARCS.height);

			//for test purposes
//			pointTest(g2);

			g2.dispose();
		}
	}

	private void pointTest(final Graphics2D g2){
		final Point enterPoint = getPaintingEnterPoint();
		GUIHelper.drawX(g2, enterPoint);
	}

	private Color getBackgroundColor(){
		return (data == null? BACKGROUND_COLOR_NO_INDIVIDUAL: BACKGROUND_COLOR_INDIVIDUAL);
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
		return new Point(getWidth() / 2, 0);
	}


	public IndividualPanel withListener(final IndividualListener listener){
		this.listener = listener;

		if(listener != null)
			attachPopupMenu();

		return this;
	}

	public IndividualPanel withParent(final FLEFRecord father, final FLEFRecord mother){
		this.father = father;
		this.mother = mother;

		return this;
	}

	public IndividualPanel withIndividualData(final IndividualData data){
		this.data = data;

		setBoxPreferredSize();

		updateIndividualData();

		return this;
	}

	private void setBoxPreferredSize(){
		final Dimension size = (isPrimaryBox()? BOX_DIMENSION_PRIMARY: BOX_DIMENSION_SECONDARY);
		setPreferredSize(size);
		setMaximumSize(size);
	}

	private void updateIndividualData(){
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

		final boolean hasData = (data != null && !data.isEmpty());
		if(hasData){
			individualNameLabel.setFormattedText(data.getIndividualNameText());
			individualNameLabel.setToolTipText(data.getIndividualNameTooltip());

			infoLabel.setText(data.getInfoText());
			infoLabel.setToolTipText(data.getInfoTooltip());

			// Set the default image/placeholder
			imageLabel.setIcon(boxType == BoxPanelType.PRIMARY
				? data.getIndividualImagePrimary()
				: data.getIndividualImageSecondary());

			// Calculate the maximum width for the text panel
			final int boxWidth = (isPrimaryBox()? BOX_DIMENSION_PRIMARY.width: BOX_DIMENSION_SECONDARY.width);
			// gap + insets
			final int maxTextWidth = Math.max(10, boxWidth - imageLabel.getIcon().getIconWidth() - NAME_IMAGE_GAP - 14);

			// Set the maximum width on the TwoLineLabel
			individualNameLabel.setMaxWidth(maxTextWidth);

			// Register the current key on the panel and start the asynchronous
			preferredImageKey = data.getPreferredImageKey();
			data.loadPreferredImageAsync((key, images) -> {
				if(images != null && Objects.equals(preferredImageKey, key))
					imageLabel.setIcon(boxType == BoxPanelType.PRIMARY? images[0]: images[1]);
			});
		}
		else{
			preferredImageKey = null;

			individualNameLabel.setMaxWidth(-1);
		}

		individualNameLabel.setVisible(hasData);
		infoLabel.setVisible(hasData);
		imageLabel.setVisible(hasData);
	}

	private void updateIndividualMenu(){
		final boolean hasData = (data != null && !data.isEmpty());
		final boolean hasIndividuals = model.hasRecordsByType(IndividualHandler.TYPE);
		final boolean hasParents = (hasData && data.hasParents());
		final boolean hasPartner = (hasData && data.hasPartner());
		final boolean hasChildren = (hasData && hasChildren());
		final boolean hasRelations = (hasParents || hasPartner || hasChildren);
		final boolean hasClippedRecord = RelationClipboard.getInstance()
			.hasRecord();

		// Update menu items labels based on clipboard state
		pasteIndividualItem.setEnabled(false);
		if(!hasData && hasClippedRecord){
			final FLEFRecord clippedRecord = RelationClipboard.getInstance()
				.getRecord();

			// Check if we are in a PartnersPanel (and on which side)
			final PartnersPanel partnersPanel = PartnersPanel.findContainingPartnersPanel(getParent());
			boolean canPaste = (partnersPanel == null);
			if(partnersPanel != null){
				final Side side = partnersPanel.getSideOf(this);
				if(side != null){
					// Get the other side panel
					final IndividualPanel otherPanel = (side == Side.LEFT
						? partnersPanel.getMotherPanel()
						: partnersPanel.getFatherPanel());
					// If the other side contains an individual, check the gender
					final IndividualData otherData = otherPanel.getData();
					if(otherData != null && !otherData.isEmpty()){
						final String otherSex = otherData.getIndividualSex().name().toLowerCase();
						final String clippedSex = FLEFRecordHelper.getChildValue(clippedRecord, TAG_SEX);
						// The gender required for the glued one is the opposite of the other
						final String requiredSex = otherSex.equals("male")? "female": "male";
						if(requiredSex.equals(clippedSex))
							canPaste = true;
					}
					else
						canPaste = true;
				}
			}
			if(canPaste){
				final String clippedName = IndividualHandler.getInstance()
					.getDisplayText(clippedRecord, model);
				pasteIndividualItem.setText("Paste " + clippedName + " Here");
				pasteIndividualItem.setEnabled(true);
			}
		}

		// Enable or disable options depending on panel state and clipboard contents
		editIndividualItem.setEnabled(hasData);
		addIndividualItem.setEnabled(!hasData);
		// Allow linking either when the box is empty and candidates exist OR when pasting from clipboard
		linkIndividualItem.setEnabled(!hasData && hasIndividuals);
		moveIndividualItem.setEnabled(hasData);
		removeIndividualItem.setEnabled(hasData);
		unlinkRelationshipsItem.setEnabled(hasRelations);
	}

	/**
	 * Checks if pasting is allowed based on gender compatibility if inside a PartnersPanel.
	 */
	private boolean isPasteAllowed(){
		final boolean hasData = (data != null && !data.isEmpty());
		final boolean hasClippedRecord = RelationClipboard.getInstance()
			.hasRecord();
		if(!hasData && hasClippedRecord){
			// Check if we are in a PartnersPanel (and on which side)
			final PartnersPanel partnersPanel = PartnersPanel.findContainingPartnersPanel(getParent());
			if(partnersPanel == null)
				// no restriction if not in a partner panel
				return true;

			final Side side = partnersPanel.getSideOf(this);
			if(side == null)
				return true;

			// Get the other side panel
			final IndividualPanel otherPanel = (side == Side.LEFT
				? partnersPanel.getMotherPanel()
				: partnersPanel.getFatherPanel());
			// If the other side contains an individual, check the gender
			final IndividualData otherData = otherPanel.getData();
			if(otherData == null || otherData.isEmpty())
				// no restriction if other side is empty
				return true;

			final FLEFRecord clipped = RelationClipboard.getInstance()
				.getRecord();
			if(clipped == null)
				return false;

			final String clippedSex = FLEFRecordHelper.getChildValue(clipped, TAG_SEX);
			// The gender required for the glued one is the opposite of the other
			final String otherSex = otherData.getIndividualSex()
				.name()
				.toLowerCase();
			final String requiredSex = (otherSex.equals(ENUM_SEX_MALE)? ENUM_SEX_FEMALE: ENUM_SEX_MALE);
			return requiredSex.equals(clippedSex);
		}
		return false;
	}

	private boolean hasChildren(){
		if(data == null || data.getIndividualId() == null)
			return false;

		final String individualId = data.getIndividualId();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = FLEFRecordHelper.getChildValue(relationship, TAG_TYPE);
			if(type != null && type.endsWith(ENUM_TYPE_ENDS_WITH_CHILD)){
				final String targetId = relationship.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
				if(individualId.equals(targetId)){
					return true;
				}
			}
		}
		return false;
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	private void installMouseListeners(){
		if(boxType == BoxPanelType.SECONDARY){
			final MouseAdapter selectedAdapter = new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && listener != null && data != null){
						final FLEFRecord individual = getRecordFromData();
						if(individual != null)
							listener.onIndividualSelected(individual);
					}
				}
			};
			individualNameLabel.addMouseListener(selectedAdapter);
		}

		// Double-click to edit
		addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2
						&& SwingUtilities.isLeftMouseButton(e) && listener != null && data != null){
					final FLEFRecord individual = getRecordFromData();
					if(individual != null)
						listener.onIndividualEdit(individual);
				}
			}
		});
	}

	private void attachPopupMenu(){
		final JPopupMenu popup = new JPopupMenu();

		// Re-evaluate state right before opening the popup
		popup.addPopupMenuListener(new PopupMenuAdapter(){
			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e){
				if(listener != null)
					listener.onPanelSelected(IndividualPanel.this);

				updateIndividualMenu();
			}
		});

		addMenuItem(popup, editIndividualItem, listener::onIndividualEdit);
		addMenuItem(popup, addIndividualItem,
			record -> listener.onIndividualAddOrLink(IndividualOperation.ADD, father, mother));
		addMenuItem(popup, linkIndividualItem,
			record -> listener.onIndividualAddOrLink(IndividualOperation.LINK, father, mother));
		addMenuItem(popup, moveIndividualItem, listener::onIndividualMove);
		addMenuItem(popup, pasteIndividualItem,
			record -> listener.onIndividualPaste(father, mother));
		addMenuItem(popup, removeIndividualItem, listener::onIndividualRemove);
		popup.addSeparator();
		addMenuItem(popup, unlinkRelationshipsItem, listener::showUnlinkDialog);


		// Register the popup listener recursively on this and all child components
		attachMouseListenerRecursively(this, new PopupMouseAdapter(popup, this));
	}

	private static void attachMouseListenerRecursively(final Component component, final MouseListener listener){
		component.addMouseListener(listener);

		if(component instanceof Container container)
			for(final Component child : container.getComponents())
				attachMouseListenerRecursively(child, listener);
	}

	/**
	 * Helper method to register an action listener and attach a JMenuItem to the popup menu.
	 */
	private void addMenuItem(final JPopupMenu popup, final JMenuItem item, final Consumer<FLEFRecord> action){
		item.addActionListener(e -> {
			if(listener != null){
				final FLEFRecord record = getRecordFromData();
				action.accept(record);
			}
		});
		popup.add(item);
	}

	private FLEFRecord getRecordFromData(){
		return (data != null && data.getIndividualId() != null
			? model.getRecordById(data.getIndividualId())
			: null);
	}

	public IndividualData getData(){
		return data;
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "I1";

		final String content;
		try(final InputStream is = IndividualPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		SwingUtilities.invokeLater(() -> {
			final IndividualPanel panel = IndividualPanel.create(BoxPanelType.PRIMARY, model);
//			panel.withIndividualData(recordId);

			final JFrame frame = new JFrame();
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
