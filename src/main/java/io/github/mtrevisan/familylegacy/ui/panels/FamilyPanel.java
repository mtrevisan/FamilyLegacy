package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;


public class FamilyPanel extends JPanel{

	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BORDER_COLOR = Color.BLACK;

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon SPOUSE_PREVIOUS = ResourceHelper.getImage("/images/previous.png");
	private static final ImageIcon SPOUSE_NEXT = ResourceHelper.getImage("/images/next.png");
	private static final double SPOUSE_PREV_NEXT_WIDTH = 12.;
	private static final double SPOUSE_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;

	/** Height of the marriage line from the bottom of the individual panel [px] */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_PANEL_DIMENSION = new Dimension(13, 12);
	public static final int HALF_SPOUSE_SEPARATION = 10;
	static final int SPOUSE_SEPARATION = HALF_SPOUSE_SEPARATION + MARRIAGE_PANEL_DIMENSION.width + HALF_SPOUSE_SEPARATION;


	private IndividualPanel spouse1Panel;
	private IndividualPanel spouse2Panel;
	private final JLabel spouse1PreviousLabel = new JLabel();
	private final JLabel spouse1NextLabel = new JLabel();
	private final JLabel spouse2PreviousLabel = new JLabel();
	private final JLabel spouse2NextLabel = new JLabel();
	private JPanel marriagePanel;

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

		spouse1 = (family != null? store.getIndividual(store.traverse(family, "SPOUSE1").getXRef()): null);
		spouse2 = (family != null? store.getIndividual(store.traverse(family, "SPOUSE2").getXRef()): null);

		initComponents(family, familyListener, individualListener);

		loadData();
	}

	private void initComponents(final GedcomNode family, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
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

		boolean hasSpouse2MoreFamilies = false;
		boolean hasSpouse1MoreFamilies = false;
		if(familyListener != null){
			attachPopUpMenu(marriagePanel, family, familyListener);

			hasSpouse2MoreFamilies = addPreviousNextSpouseIcons(family, spouse1, spouse2, spouse1PreviousLabel, spouse1NextLabel, familyListener);

			hasSpouse1MoreFamilies = addPreviousNextSpouseIcons(family, spouse2, spouse1, spouse2PreviousLabel, spouse2NextLabel, familyListener);
		}

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		final GroupLayout.ParallelGroup horizontalGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
		final GroupLayout.SequentialGroup verticalGroup = layout.createSequentialGroup();
		showArrows(hasSpouse2MoreFamilies, hasSpouse1MoreFamilies, layout, horizontalGroup, verticalGroup);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
			.addGroup(horizontalGroup
				.addGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(spouse1Panel)
					.addGap(HALF_SPOUSE_SEPARATION)
					.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_PANEL_DIMENSION.width, GroupLayout.PREFERRED_SIZE)
					.addGap(HALF_SPOUSE_SEPARATION)
					.addComponent(spouse2Panel)
					.addGap(0, 0, Short.MAX_VALUE)
				)
			)
		);
		layout.setVerticalGroup(verticalGroup
			.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(spouse1Panel)
					.addComponent(spouse2Panel)
				)
				.addGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_PANEL_DIMENSION.height, GroupLayout.PREFERRED_SIZE)
					.addGap(FAMILY_CONNECTION_HEIGHT - MARRIAGE_PANEL_DIMENSION.height / 2)
				)
			)
		);
	}

	private void showArrows(final boolean hasSpouse2MoreFamilies, final boolean hasSpouse1MoreFamilies, final GroupLayout layout,
			final GroupLayout.ParallelGroup horizontalGroup, final GroupLayout.SequentialGroup verticalGroup){
		if(hasSpouse2MoreFamilies && hasSpouse1MoreFamilies){
			horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(spouse1PreviousLabel)
				.addGap(10)
				.addComponent(spouse1NextLabel)
				.addGap(MARRIAGE_PANEL_DIMENSION.width + HALF_SPOUSE_SEPARATION * 2)
				.addComponent(spouse2PreviousLabel)
				.addGap(10)
				.addComponent(spouse2NextLabel)
				.addGap(0, 0, Short.MAX_VALUE)
			);
			verticalGroup
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(spouse1PreviousLabel)
					.addComponent(spouse1NextLabel)
					.addComponent(spouse2PreviousLabel)
					.addComponent(spouse2NextLabel)
				)
				.addGap(3);
		}
		else if(hasSpouse2MoreFamilies){
			horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(spouse1PreviousLabel)
				.addGap(10)
				.addComponent(spouse1NextLabel)
				.addGap((int)(SPOUSE_PREV_NEXT_WIDTH * 3 + HALF_SPOUSE_SEPARATION * 2 + 10))
				.addGap(0, 0, Short.MAX_VALUE)
			);
			verticalGroup
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addComponent(spouse1PreviousLabel)
					.addComponent(spouse1NextLabel)
					.addGap((int)(SPOUSE_PREV_NEXT_WIDTH * SPOUSE_PREV_NEXT_ASPECT_RATIO))
				)
				.addGap(3);
		}
		else if(hasSpouse1MoreFamilies){
			horizontalGroup.addGroup(layout.createSequentialGroup()
				.addGap(0, 0, Short.MAX_VALUE)
				.addGap((int)(SPOUSE_PREV_NEXT_WIDTH * 3 + HALF_SPOUSE_SEPARATION * 2 + 10))
				.addComponent(spouse2PreviousLabel)
				.addGap(10)
				.addComponent(spouse2NextLabel)
				.addGap(0, 0, Short.MAX_VALUE)
			);
			verticalGroup
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
					.addGap((int)(SPOUSE_PREV_NEXT_WIDTH * SPOUSE_PREV_NEXT_ASPECT_RATIO))
					.addComponent(spouse2PreviousLabel)
					.addComponent(spouse2NextLabel)
				)
				.addGap(3);
		}
		else
			verticalGroup
				.addGap((int)(SPOUSE_PREV_NEXT_WIDTH * SPOUSE_PREV_NEXT_ASPECT_RATIO + 3));
	}

	private boolean addPreviousNextSpouseIcons(final GedcomNode family, final GedcomNode thisSpouse, final GedcomNode otherSpouse,
			final JLabel spousePreviousLabel, final JLabel spouseNextLabel, final FamilyListenerInterface familyListener){
		//get list of marriages for the `other spouse`
		final List<GedcomNode> otherMarriages = store.traverseAsList(otherSpouse, "FAMILY_SPOUSE[]");
		//find current marriage in list
		int currentFamilyIndex = -1;
		final int otherMarriagesCount = otherMarriages.size();
		for(int i = 0; i < otherMarriagesCount; i ++)
			if(otherMarriages.get(i).getXRef().equals(family.getID())){
				currentFamilyIndex = i;
				break;
			}
		final boolean hasMoreFamilies = (otherMarriagesCount > 1);

		spousePreviousLabel.setVisible(hasMoreFamilies);
		setPreferredSize(spousePreviousLabel, SPOUSE_PREV_NEXT_WIDTH, SPOUSE_PREV_NEXT_ASPECT_RATIO);
		spouseNextLabel.setVisible(hasMoreFamilies);
		setPreferredSize(spouseNextLabel, SPOUSE_PREV_NEXT_WIDTH, SPOUSE_PREV_NEXT_ASPECT_RATIO);
		if(hasMoreFamilies){
			final boolean spousePreviousEnabled = (currentFamilyIndex > 0);
			final boolean spouseNextEnabled = (currentFamilyIndex < otherMarriagesCount - 1);

			spousePreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(spousePreviousEnabled)
						familyListener.onFamilyPreviousSpouse(FamilyPanel.this, otherSpouse, thisSpouse);
				}
			});
			spouseNextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(spouseNextEnabled)
						familyListener.onFamilyNextSpouse(FamilyPanel.this, otherSpouse, thisSpouse);
				}
			});

			changePreviousNextSpouseIcons(spousePreviousEnabled, spouseNextEnabled, spousePreviousLabel, spouseNextLabel);
		}

		return hasMoreFamilies;
	}

	private void setPreferredSize(final JComponent component, final double baseWidth, final double aspectRatio){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(baseWidth / shrinkFactor);
		final int height = (int)Math.ceil(baseWidth * aspectRatio / shrinkFactor);
		component.setPreferredSize(new Dimension(width, height));
	}

	private void changePreviousNextSpouseIcons(final boolean spousePreviousEnabled, final boolean spouseNextEnabled,
			final JLabel spousePreviousLabel, final JLabel spouseNextLabel){
		spousePreviousLabel.setCursor(new Cursor(spousePreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		final Dimension size = new Dimension((int)SPOUSE_PREV_NEXT_WIDTH, (int)(SPOUSE_PREV_NEXT_WIDTH * SPOUSE_PREV_NEXT_ASPECT_RATIO));
		ImageIcon icon = ResourceHelper.getImage(SPOUSE_PREVIOUS, size);
		spousePreviousLabel.setIcon(spousePreviousEnabled? icon: new ImageIcon(GrayFilter.createDisabledImage(icon.getImage())));

		spouseNextLabel.setCursor(new Cursor(spouseNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		icon = ResourceHelper.getImage(SPOUSE_NEXT, size);
		spouseNextLabel.setIcon(spouseNextEnabled? icon: new ImageIcon(GrayFilter.createDisabledImage(icon.getImage())));

		//TODO add links to arrows to switch across multiple spouses
	}

	private void attachPopUpMenu(final JComponent component, final GedcomNode family, final FamilyListenerInterface familyListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem editFamilyItem = new JMenuItem("Edit Family…", 'E');
		editFamilyItem.setEnabled(family != null);
		editFamilyItem.addActionListener(e -> familyListener.onFamilyEdit(this, family));
		popupMenu.add(editFamilyItem);

		final JMenuItem addChildItem = new JMenuItem("Add Child…", 'C');
		addChildItem.addActionListener(e -> familyListener.onFamilyAddChild(this, family));
		addChildItem.setEnabled(family != null);
		popupMenu.add(addChildItem);

		final JMenuItem linkFamilyItem = new JMenuItem("Link Family…", 'L');
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
//		final GedcomNode family = storeFlef.getFamilies().get(9);
//		final GedcomNode family = storeFlef.getFamilies().get(64);
//		final GedcomNode family = storeFlef.getFamilies().get(75);
//		GedcomNode family = null;
		final BoxPanelType boxType = BoxPanelType.PRIMARY;

		final FamilyListenerInterface familyListener = new FamilyListenerInterface(){
			@Override
			public void onFamilyEdit(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onEditFamily " + family.getID());
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
			public void onFamilyPreviousSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse){
				System.out.println("onPrevSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID());
			}

			@Override
			public void onFamilyNextSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse){
				System.out.println("onNextSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID());
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
