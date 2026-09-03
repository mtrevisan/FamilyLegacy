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
import io.github.mtrevisan.familylegacy.v2.ui.components.TwoLineLabel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMenuAdapter;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.PopupMouseAdapter;
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
	private static final double PREFERRED_IMAGE_WIDTH = 58.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	// Fonts
	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 15);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 12);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	// UI components
	private final TwoLineLabel individualNameLabel = new TwoLineLabel();
	private final JLabel infoLabel = new JLabel();
	private final JLabel imageLabel = new JLabel();

	// Menu items
	private final JMenuItem editIndividualItem = new JMenuItem("Edit Individual…", 'E');
	private final JMenuItem addIndividualItem = new JMenuItem("Add Individual…", 'A');
	private final JMenuItem linkIndividualItem = new JMenuItem("Link Individual…", 'L');
	private final JMenuItem removeIndividualItem = new JMenuItem("Remove Individual", 'R');
	private final JMenuItem unlinkFromParentsItem = new JMenuItem("Unlink from parents", 'U');
	private final JMenuItem unlinkFromPartnerItem = new JMenuItem("Unlink from partner", 'P');

	// State
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
		setPreferredSize(imageLabel, 48., IMAGE_ASPECT_RATIO, shrinkFactor);

		setBoxPreferredSize();

		setLayout(new MigLayout("ins 7", "[grow]0[]", "[]0[]10[]"));

		final int shrink = (int)Math.round(PREFERRED_IMAGE_WIDTH + 21);
		add(individualNameLabel, "cell 0 0,top,growx,width ::100%-" + shrink + ",hidemode 3");
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
			if(data != null){
				final Paint gradientPaint = new GradientPaint(0, 0, startColor, 0, panelHeight, BACKGROUND_COLOR_FADE_TO);
				graphics2D.setPaint(gradientPaint);
			}
			else
				graphics2D.setColor(startColor);
			graphics2D.fillRoundRect(1, 1,
				panelWidth - 2, panelHeight - 2,
				ARCS.width - 5, ARCS.height - 5);

			graphics2D.setColor(BORDER_COLOR);
			if(data == null){
				final Stroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					10.f, new float[]{5.f}, 0.f);
				graphics2D.setStroke(dashedStroke);
			}
			graphics2D.drawRoundRect(0, 0,
				panelWidth - 1, panelHeight - 1,
				ARCS.width, ARCS.height);


			//for test purposes
//			pointTest(graphics2D);


			graphics2D.dispose();
		}
	}

	private void pointTest(final Graphics2D graphics2D){
		final Point enterPoint = getPaintingEnterPoint();
		GUIHelper.drawX(graphics2D, enterPoint);
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

	public IndividualPanel withIndividualData(final IndividualData data){
		this.data = data;

		setBoxPreferredSize();

		updateIndividualData();

		return this;
	}

	private void setBoxPreferredSize(){
		final Dimension size = (isPrimaryBox()
			? new Dimension(270, 90)
			: new Dimension(170, 65));
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

			// Register the current key on the panel and start the asynchronous
			preferredImageKey = data.getPreferredImageKey();
			data.loadPreferredImageAsync((key, images) -> {
				if(images != null && Objects.equals(preferredImageKey, key))
					imageLabel.setIcon(boxType == BoxPanelType.PRIMARY? images[0]: images[1]);
			});
		}
		else
			preferredImageKey = null;

		individualNameLabel.setVisible(hasData);
		infoLabel.setVisible(hasData);
		imageLabel.setVisible(hasData);

//		refresh(ActionCommand.ACTION_COMMAND_PERSON);
	}

	private void updateIndividualMenu(){
		final boolean hasData = (data != null && !data.isEmpty());
		final boolean hasIndividuals = model.hasRecordsByType(IndividualHandler.TYPE);
		final boolean hasParentGroup = (hasData && data.hasParents());
		final boolean hasPartner = (hasData && data.hasPartner());
		editIndividualItem.setEnabled(hasData);
		addIndividualItem.setEnabled(!hasData);
		linkIndividualItem.setEnabled(!hasData && hasIndividuals);
		removeIndividualItem.setEnabled(hasData);
		unlinkFromParentsItem.setEnabled(hasData && hasParentGroup);
		unlinkFromPartnerItem.setEnabled(hasData && hasPartner);
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	// ------------------------------------------------------------------------
	// Event handling
	// ------------------------------------------------------------------------

	private void installMouseListeners(){
		if(boxType == BoxPanelType.SECONDARY){
			final MouseAdapter selectedAdapter = new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && listener != null && data != null){
						final String individualId = data.getIndividualId();
						final FLEFRecord individual = model.getRecordById(individualId);
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
					final String individualId = data.getIndividualId();
					final FLEFRecord individual = model.getRecordById(individualId);
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
				updateIndividualMenu();
			}
		});

		// Add items using helper method
		addMenuItem(popup, editIndividualItem, listener::onIndividualEdit);
		addMenuItem(popup, addIndividualItem, listener::onIndividualAdd);
		addMenuItem(popup, linkIndividualItem, listener::onIndividualLink);
		addMenuItem(popup, removeIndividualItem, listener::onIndividualRemove);
		popup.addSeparator();
		addMenuItem(popup, unlinkFromParentsItem, listener::onIndividualUnlinkFromParentGroup);
		popup.addSeparator();
		addMenuItem(popup, unlinkFromPartnerItem, listener::onIndividualUnlinkFromPartner);

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
			if(listener != null && data != null){
				final String individualId = data.getIndividualId();
				if(individualId != null){
					final FLEFRecord individual = model.getRecordById(individualId);
					action.accept(individual);
				}
			}
		});
		popup.add(item);
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
