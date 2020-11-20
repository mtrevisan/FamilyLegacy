package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class FamilyPanel extends JPanel{

	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BACKGROUND_COLOR = Color.WHITE;
	private static final Color BORDER_COLOR = Color.BLACK;

	private static final ImageIcon SPOUSE_PREVIOUS = ResourceHelper.getImage("/images/previous.png");
	private static final ImageIcon SPOUSE_NEXT = ResourceHelper.getImage("/images/next.png");
	private static final double SPOUSE_PREV_NEXT_WIDTH = 12.;
	private static final double SPOUSE_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;

	/** Height of the marriage line from the botton of the individual panel [px] */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_PANEL_DIMENSION = new Dimension(13, 12);
	public static final int HALF_SPOUSE_SEPARATION = 10;
	static final int SPOUSE_SEPARATION = HALF_SPOUSE_SEPARATION + MARRIAGE_PANEL_DIMENSION.width + HALF_SPOUSE_SEPARATION;

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private IndividualPanel spouse1Panel;
	private IndividualPanel spouse2Panel;
	private final JLabel spouse1PreviousLabel = new JLabel();
	private final JLabel spouse1NextLabel = new JLabel();
	private final JLabel spouse2PreviousLabel = new JLabel();
	private final JLabel spouse2NextLabel = new JLabel();
	private JPanel marriagePanel;
	private JMenuItem editFamilyItem;
	private JMenuItem linkFamilyItem;
	private JMenuItem addChildItem;

	private final BoxPanelType boxType;
	private final GedcomNode family;
	private final GedcomNode spouse1;
	private final GedcomNode spouse2;
	private final Flef store;


	public FamilyPanel(final GedcomNode family, final Flef store, final BoxPanelType boxType, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.boxType = boxType;
		this.family = family;
		this.store = store;

		spouse1 = (family != null? store.getIndividual(TRANSFORMER.traverse(family, "SPOUSE1").getXRef()): null);
		spouse2 = (family != null? store.getIndividual(TRANSFORMER.traverse(family, "SPOUSE2").getXRef()): null);

		initComponents(family, familyListener, individualListener);

		loadData();
	}

	private void initComponents(final GedcomNode family, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		setBackground(null);
		setOpaque(false);

		spouse1Panel = new IndividualPanel(spouse1, store, boxType, individualListener);
		spouse2Panel = new IndividualPanel(spouse2, store, boxType, individualListener);
		marriagePanel = new JPanel();
		marriagePanel.setBackground(Color.WHITE);
		marriagePanel.setFocusable(false);
		marriagePanel.setInheritsPopupMenu(false);
		marriagePanel.setMaximumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setMinimumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setPreferredSize(MARRIAGE_PANEL_DIMENSION);

		if(familyListener != null){
			attachPopUpMenu(marriagePanel, family, familyListener);

			addPreviousNextSpouseIcons(spouse1, spouse1PreviousLabel, spouse1NextLabel, familyListener);

			addPreviousNextSpouseIcons(spouse2, spouse2PreviousLabel, spouse2NextLabel, familyListener);
		}

		//TODO add arrows to switch across multiple spouses
		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addGroup(layout.createSequentialGroup()
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(spouse1PreviousLabel)
				.addGap(10)
				.addComponent(spouse1NextLabel)
				.addGap(MARRIAGE_PANEL_DIMENSION.width + HALF_SPOUSE_SEPARATION * 2)
				.addComponent(spouse2PreviousLabel)
				.addGap(10)
				.addComponent(spouse2NextLabel)
				.addGap(0, 0, Short.MAX_VALUE)
			)
			.addGroup(layout.createSequentialGroup()
				.addComponent(spouse1Panel)
				.addGap(HALF_SPOUSE_SEPARATION)
				.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_PANEL_DIMENSION.width, GroupLayout.PREFERRED_SIZE)
				.addGap(HALF_SPOUSE_SEPARATION)
				.addComponent(spouse2Panel)
			)
		);
		final int marriagePanelGapHeight = FAMILY_CONNECTION_HEIGHT - MARRIAGE_PANEL_DIMENSION.height / 2;
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
				.addComponent(spouse1PreviousLabel)
				.addComponent(spouse1NextLabel)
				.addComponent(spouse2PreviousLabel)
				.addComponent(spouse2NextLabel)
			)
			.addGap(3)
			.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
				.addGroup(layout.createParallelGroup()
					.addComponent(spouse1Panel)
					.addComponent(spouse2Panel)
				)
				.addGroup(layout.createSequentialGroup()
					.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_PANEL_DIMENSION.height, GroupLayout.PREFERRED_SIZE)
					.addGap(marriagePanelGapHeight, marriagePanelGapHeight, marriagePanelGapHeight)
				)
			)
		);
	}

	private void addPreviousNextSpouseIcons(final GedcomNode spouse, final JLabel spousePreviousLabel, final JLabel spouseNextLabel,
			final FamilyListenerInterface familyListener){
		setPreferredSize(spousePreviousLabel, SPOUSE_PREV_NEXT_WIDTH, SPOUSE_PREV_NEXT_ASPECT_RATIO);
		spousePreviousLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		spousePreviousLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent evt){
				familyListener.onFamilyPreviousSpouse(FamilyPanel.this, spouse);
			}
		});

		setPreferredSize(spouseNextLabel, SPOUSE_PREV_NEXT_WIDTH, SPOUSE_PREV_NEXT_ASPECT_RATIO);
		spouseNextLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		spouseNextLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent evt){
				familyListener.onFamilyNextSpouse(FamilyPanel.this, spouse);
			}
		});

		ImageIcon icon = ResourceHelper.getImage(SPOUSE_PREVIOUS, spousePreviousLabel.getPreferredSize());
		spousePreviousLabel.setIcon(icon);

		icon = ResourceHelper.getImage(SPOUSE_NEXT, spouseNextLabel.getPreferredSize());
		spouseNextLabel.setIcon(icon);
	}

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	private void attachPopUpMenu(final JComponent component, final GedcomNode family, final FamilyListenerInterface familyListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editFamilyItem = new JMenuItem("Edit Family…", 'E');
		editFamilyItem.setEnabled(family != null);
		editFamilyItem.addActionListener(e -> familyListener.onFamilyEdit(this, family));
		popupMenu.add(editFamilyItem);

		addChildItem = new JMenuItem("Add Child…", 'C');
		addChildItem.addActionListener(e -> familyListener.onFamilyAddChild(this, family));
		addChildItem.setEnabled(family != null);
		popupMenu.add(addChildItem);

		linkFamilyItem = new JMenuItem("Link Family…", 'L');
		linkFamilyItem.addActionListener(e -> familyListener.onFamilyLink(this));
		linkFamilyItem.setEnabled(family == null);
		popupMenu.add(linkFamilyItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final int xFrom = spouse1Panel.getX() + spouse1Panel.getWidth();
			final int xTo = spouse2Panel.getX();
			final int yFrom = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT;
			if(family == null)
				graphics2D.setStroke(new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
					new float[]{1.f}, 0.f));
			//horizontal line between spouses
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);
			if(family == null){
				graphics2D.setColor(BACKGROUND_COLOR);
				graphics2D.setColor(IndividualPanel.BORDER_COLOR);
				graphics2D.setStroke(new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.f,
					new float[]{5.f}, 0.f));
			}

			graphics2D.dispose();
		}
	}

	public void loadData(){
		spouse1Panel.loadData();
		spouse2Panel.loadData();

		final boolean hasFamily = (family != null);
		marriagePanel.setBorder(hasFamily? BorderFactory.createLineBorder(BORDER_COLOR): BorderFactory.createDashedBorder(BORDER_COLOR));
	}


	public Point getFamilyPaintingExitPoint(){
		//halfway between spouse1 and spouse2 boxes
		final int x = (spouse1Panel.getX() + spouse1Panel.getWidth() + spouse2Panel.getX()) / 2;
		//the bottom point of the marriage panel (that is: bottom point of spouse1 box minus the height of the horizontal connection line
		//plus half the size of the marriage panel box)
		final int y = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT + MARRIAGE_PANEL_DIMENSION.height / 2;
		return new Point(x, y);
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();
		final GedcomNode family = storeFlef.getFamilies().get(0);
//		GedcomNode family = null;
		final BoxPanelType boxType = BoxPanelType.PRIMARY;

		final FamilyListenerInterface familyListener = new FamilyListenerInterface(){
			@Override
			public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onEditFamily " + family.getID());
			}

			@Override
			public void onFamilyFocus(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onFocusFamily " + family.getID());
			}

			@Override
			public void onFamilyLink(final FamilyPanel boxPanel){
				System.out.println("onLinkFamily");
			}

			@Override
			public void onFamilyAddChild(final FamilyPanel familyPanel, final GedcomNode family){
				System.out.println("onAddChildFamily " + family.getID());
			}

			@Override
			public void onFamilyPreviousSpouse(final FamilyPanel familyPanel, final GedcomNode spouse){
				System.out.println("onPrevSpouseFamily " + spouse.getID());
			}

			@Override
			public void onFamilyNextSpouse(final FamilyPanel familyPanel, final GedcomNode spouse){
				System.out.println("onNextSpouseFamily " + spouse.getID());
			}
		};
		final IndividualListenerInterface individualListener = new IndividualListenerInterface(){
			@Override
			public void onIndividualEdit(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onEditIndividual " + individual.getID());
			}

			@Override
			public void onIndividualFocus(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID());
			}

			@Override
			public void onIndividualNew(final IndividualPanel boxPanel){
				System.out.println("onNewIndividual");
			}

			@Override
			public void onIndividualLink(final IndividualPanel boxPanel){
				System.out.println("onLinkIndividual");
			}

			@Override
			public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final FamilyPanel panel = new FamilyPanel(family, storeFlef, boxType, familyListener, individualListener);

			final JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
