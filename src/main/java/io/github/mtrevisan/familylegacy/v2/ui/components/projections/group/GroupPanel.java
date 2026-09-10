package io.github.mtrevisan.familylegacy.v2.ui.components.projections.group;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.MultiLineLabel;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.TreeOperation;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * A panel that displays a group's information (name, type, photo) in a genealogical box.
 */
public class GroupPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -712849120391204812L;


	// Colors
	private static final Color BACKGROUND_COLOR_NO_ENTITY = Color.WHITE;
	private static final Color BACKGROUND_COLOR_FADE_TO = Color.WHITE;
	private static final Color BACKGROUND_COLOR = new Color(225, 230, 240);
	private static final Color BORDER_COLOR = new Color(150, 160, 180);
	private static final Color TYPE_COLOR = new Color(100, 100, 110);
	private static final Color IMAGE_LABEL_BORDER_COLOR = Color.WHITE;

	// Dimensions
	private static final Dimension ARCS = new Dimension(10, 10);
	private static final int PREFERRED_IMAGE_WIDTH = 48;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final Dimension BOX_DIMENSION_PRIMARY = new Dimension(260, 210);
	private static final Dimension BOX_DIMENSION_SECONDARY = new Dimension(130, 100);

	private static final int NAME_IMAGE_GAP = 5;

	// Fonts
	private static final Font FONT_PRIMARY = new Font("Tahoma", Font.BOLD, 15);
	private static final Font FONT_SECONDARY = new Font("Tahoma", Font.PLAIN, 12);
	private static final float INFO_FONT_SIZE_FACTOR = 0.8f;

	// UI components
	private final MultiLineLabel nameLabel = new MultiLineLabel(3);
	private final JLabel typeLabel = new JLabel();
	private final JLabel imageLabel = new JLabel();

	// Menu items
	private final JMenuItem editItem = new JMenuItem("Edit Group…", 'E');
	private final JMenuItem addItem = new JMenuItem("Add Group…", 'A');
	private final JMenuItem connectItem = new JMenuItem("Connect Group…", 'C');
	private final JMenuItem relocateItem = new JMenuItem("Relocate Group", 'R');
	private final JMenuItem pasteItem = new JMenuItem("Paste Group", 'P');
	private final JMenuItem deleteItem = new JMenuItem("Delete Group", 'D');
	private final JMenuItem unlinkRelationshipsItem = new JMenuItem("Unlink Relationships…", 'U');


	// State
	private final BoxPanelType boxType;

	private final FLEFModel model;

	private GroupData data;

	private String preferredImageKey;

	// Listener
	private GroupListener listener;


	public static GroupPanel create(final BoxPanelType boxType, final FLEFModel model){
		return new GroupPanel(boxType, model);
	}


	private GroupPanel(final BoxPanelType boxType, final FLEFModel model){
		this.boxType = boxType;

		this.model = model;

		initComponents();

		installMouseListeners();
	}


	private void initComponents(){
		typeLabel.setForeground(TYPE_COLOR);

		imageLabel.setBorder(BorderFactory.createLineBorder(IMAGE_LABEL_BORDER_COLOR));
		final double shrinkFactor = (isPrimaryBox()? 1.: 2.);
		setPreferredSize(imageLabel, PREFERRED_IMAGE_WIDTH, IMAGE_ASPECT_RATIO, shrinkFactor);

		setBoxPreferredSize();

		setLayout(new MigLayout("ins 7,gapx 5,aligny center", "[grow,fill][grow 0,shrink 0]", "[]0[]10[]"));

		final int imageWidth = (int)(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		add(nameLabel, "cell 0 0,aligny center,growx,width ::100%-" + imageWidth + ",hidemode 3");
		add(imageLabel, (isPrimaryBox()? "cell 1 0 1 3,aligny center": "cell 1 0,aligny center"));
		add(typeLabel, (isPrimaryBox()? "cell 0 2,aligny center,growx": "cell 0 2 2 1,aligny center,growx"));

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

			g2.dispose();
		}
	}

	private Color getBackgroundColor(){
		return (data == null? BACKGROUND_COLOR_NO_ENTITY : BACKGROUND_COLOR);
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

	public final Point getPaintingVerticalEnterPoint(){
		return new Point(getWidth() / 2, 0);
	}

	public final Point getPaintingHorizontalEnterPoint(){
		return new Point(0, (getHeight() - 1) / 2);
	}


	public GroupPanel withListener(final GroupListener listener){
		this.listener = listener;

		if(listener != null)
			attachPopupMenu();

		return this;
	}

	public GroupPanel withGroupData(final GroupData data){
		this.data = data;

		setBoxPreferredSize();

		updateData();

		return this;
	}

	private void setBoxPreferredSize(){
		final Dimension size = (isPrimaryBox()? BOX_DIMENSION_PRIMARY: BOX_DIMENSION_SECONDARY);
		setPreferredSize(size);
		setMaximumSize(size);
	}

	private void updateData(){
		Font font = (isPrimaryBox()? FONT_PRIMARY: FONT_SECONDARY);
		final Font infoFont = deriveInfoFont(font);
		if(!isPrimaryBox()){
			@SuppressWarnings("unchecked")
			final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>)font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
			font = font.deriveFont(attributes);
		}
		final Cursor cursor = Cursor.getPredefinedCursor(isPrimaryBox()? Cursor.DEFAULT_CURSOR: Cursor.HAND_CURSOR);
		nameLabel.setFont(font);
		nameLabel.setCursor(cursor);
		typeLabel.setFont(infoFont);

		final boolean hasData = (data != null && !data.isEmpty());
		if(hasData){
			nameLabel.setFormattedText(data.getNameText());
			nameLabel.setToolTipText(data.getNameTooltip());

			typeLabel.setText(data.getYype());

			// Set the default image/placeholder
			imageLabel.setIcon(boxType == BoxPanelType.PRIMARY
				? data.getImagePrimary()
				: data.getImageSecondary());

			// Calculate the maximum width for the text panel
			final int boxWidth = (isPrimaryBox()? BOX_DIMENSION_PRIMARY.width: BOX_DIMENSION_SECONDARY.width);
			// gap + insets
			final int maxTextWidth = Math.max(10, boxWidth - imageLabel.getIcon().getIconWidth() - NAME_IMAGE_GAP - 14);

			// Set the maximum width on the TwoLineLabel
			nameLabel.setMaxWidth(maxTextWidth);

			// Register the current key on the panel and start the asynchronous
			preferredImageKey = data.getPreferredImageKey();
			data.loadPreferredImageAsync((key, images) -> {
				if(images != null && Objects.equals(preferredImageKey, key))
					imageLabel.setIcon(boxType == BoxPanelType.PRIMARY? images[0]: images[1]);
			});
		}
		else{
			preferredImageKey = null;

			nameLabel.setMaxWidth(-1);
		}

		nameLabel.setVisible(hasData);
		typeLabel.setVisible(hasData);
		imageLabel.setVisible(hasData);
	}

	private void updateGroupMenu(){
		final boolean hasData = (data != null && !data.isEmpty());
		final boolean hasGroups = model.hasRecordsByType(GroupHandler.TYPE);

		final boolean canPaste = (!hasData && RelationClipboard.getInstance().hasRecord());
		if(canPaste){
			final FLEFRecord clippedRecord = RelationClipboard.getInstance()
				.getRecord();
			final String clippedName = GroupHandler.getInstance()
				.getDisplayText(clippedRecord, model);
			pasteItem.setText("Paste " + clippedName + " Here");
			pasteItem.setEnabled(true);
		}
		else
			pasteItem.setEnabled(false);

		// Enable or disable options depending on panel state and clipboard contents
		editItem.setEnabled(hasData);
		addItem.setEnabled(!hasData);
		// Allow connecting either when the box is empty and candidates exist OR when pasting from clipboard
		connectItem.setEnabled(!hasData && hasGroups);
		relocateItem.setEnabled(hasData);
		deleteItem.setEnabled(hasData);
		unlinkRelationshipsItem.setEnabled(hasData);
	}

	private static Font deriveInfoFont(final Font baseFont){
		return baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * INFO_FONT_SIZE_FACTOR);
	}

	private void installMouseListeners(){
		if(boxType == BoxPanelType.SECONDARY){
			final MouseAdapter selectedAdapter = new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent e){
					if(SwingUtilities.isLeftMouseButton(e) && listener != null && data != null){
						final FLEFRecord group = getRecordFromData();
						if(group != null)
							listener.onEntitySelected(group);
					}
				}
			};
			nameLabel.addMouseListener(selectedAdapter);
		}

		// Double-click to edit
		addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && listener != null && data != null){
					final FLEFRecord group = getRecordFromData();
					if(group != null)
						listener.onEntityEdit(group);
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
					listener.onPanelSelected(GroupPanel.this);

				updateGroupMenu();
			}
		});

		addMenuItem(popup, editItem, listener::onEntityEdit);
		addMenuItem(popup, addItem, record -> listener.onGroupAddOrConnect(TreeOperation.ADD));
		addMenuItem(popup, connectItem, record -> listener.onGroupAddOrConnect(TreeOperation.CONNECT));
		addMenuItem(popup, relocateItem, listener::onEntityRelocate);
		addMenuItem(popup, deleteItem, listener::onEntityRemove);
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
		return (data != null && data.getId() != null
			? model.getRecordById(data.getId())
			: null);
	}

	public GroupData getData(){
		return data;
	}


	public static void main(String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		String modelUri = "/tests/TGMZ.flef";
		String recordId = "G1";

		final String content;
		try(final InputStream is = GroupPanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);


		SwingUtilities.invokeLater(() -> {
			final GroupPanel panel = GroupPanel.create(BoxPanelType.PRIMARY, model);
			final FLEFRecord groupRecord = model.getRecordById(recordId);
			final GroupData data = GroupData.create(groupRecord);
			panel.withGroupData(data);

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
