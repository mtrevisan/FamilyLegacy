package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class TreePanel extends JPanel{

	private static final long serialVersionUID = 4700955059623460223L;

	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);

	private static final int GENERATION_SEPARATOR_SIZE = 20;


	private FamilyPanel spouse1Grandparents1Panel;
	private FamilyPanel spouse1Grandparents2Panel;
	private FamilyPanel spouse2Grandparents1Panel;
	private FamilyPanel spouse2Grandparents2Panel;
	private FamilyPanel spouse1ParentsPanel;
	private FamilyPanel spouse2ParentsPanel;
	private FamilyPanel homeFamilyPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;

	private GedcomNode spouse1;
	private GedcomNode spouse2;
	private GedcomNode homeFamily;
	private int generations;
	private final Flef store;
	private final IndividualListenerInterface individualListener;
	private final FamilyListenerInterface familyListener;


	public TreePanel(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode homeFamily, final int generations,
			final Flef store, final FamilyListenerInterface familyListener, final IndividualListenerInterface individualListener){
		this.homeFamily = homeFamily;
		this.generations = generations;
		this.store = store;
		this.individualListener = individualListener;
		this.familyListener = familyListener;

		if(generations <= 3)
			initComponents3Generations(spouse1, spouse2, homeFamily);
		else
			initComponents4Generations(spouse1, spouse2, homeFamily);

		loadData();
	}

	//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
	//TODO remove duplicated code
	private void initComponents3Generations(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode family){
		this.spouse1 = (spouse1 == null && family != null? store.getIndividual(store.traverse(family, "SPOUSE1").getXRef()): null);
		this.spouse2 = (spouse2 == null && family != null? store.getIndividual(store.traverse(family, "SPOUSE2").getXRef()): null);

		final GedcomNode spouse1Parents = extractParents(null, family, "SPOUSE1");
		final GedcomNode spouse2Parents = extractParents(null, family, "SPOUSE2");

		spouse1ParentsPanel = new FamilyPanel(null, null, spouse1Parents, store, BoxPanelType.SECONDARY, familyListener,
			individualListener);
		spouse2ParentsPanel = new FamilyPanel(null, null, spouse2Parents, store, BoxPanelType.SECONDARY, familyListener,
			individualListener);
		homeFamilyPanel = new FamilyPanel(this.spouse1, this.spouse2, homeFamily, store, BoxPanelType.PRIMARY, familyListener,
			individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);

		setLayout(new MigLayout("insets 0",
			"[grow,center]" + FamilyPanel.SPOUSE_SEPARATION + "[grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(spouse1ParentsPanel, "growx 50");
		add(spouse2ParentsPanel, "growx 50,wrap");
		add(homeFamilyPanel, "span 2,wrap");
		add(childrenScrollPane, "span 2");
	}

	//TODO remove duplicated code
	private void initComponents4Generations(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode family){
		this.spouse1 = (spouse1 == null && family != null? store.getIndividual(store.traverse(family, "SPOUSE1").getXRef()): null);
		this.spouse2 = (spouse2 == null && family != null? store.getIndividual(store.traverse(family, "SPOUSE2").getXRef()): null);

		final GedcomNode spouse1Parents = extractParents(null, family, "SPOUSE1");
		final GedcomNode spouse2Parents = extractParents(null, family, "SPOUSE2");
		final GedcomNode spouse1Grandparents1 = extractParents(spouse1, spouse1Parents, "SPOUSE1");
		final GedcomNode spouse1Grandparents2 = extractParents(spouse1, spouse1Parents, "SPOUSE2");
		final GedcomNode spouse2Grandparents1 = extractParents(spouse2, spouse2Parents, "SPOUSE1");
		final GedcomNode spouse2Grandparents2 = extractParents(spouse2, spouse2Parents, "SPOUSE2");

		spouse1Grandparents1Panel = new FamilyPanel(null, null, spouse1Grandparents1, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		spouse1Grandparents2Panel = new FamilyPanel(null, null, spouse1Grandparents2, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		spouse2Grandparents1Panel = new FamilyPanel(null, null, spouse2Grandparents1, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		spouse2Grandparents2Panel = new FamilyPanel(null, null, spouse2Grandparents2, store, BoxPanelType.SECONDARY,
			familyListener, individualListener);
		spouse1ParentsPanel = new FamilyPanel(null, null, spouse1Parents, store, BoxPanelType.SECONDARY, familyListener,
			individualListener);
		spouse2ParentsPanel = new FamilyPanel(null, null, spouse2Parents, store, BoxPanelType.SECONDARY, familyListener,
			individualListener);
		homeFamilyPanel = new FamilyPanel(spouse1, spouse2, homeFamily, store, BoxPanelType.PRIMARY, familyListener, individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel));
		childrenScrollPane.setOpaque(false);
		childrenScrollPane.getViewport().setOpaque(false);
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);

		setLayout(new MigLayout("insets 0",
			"[grow,center][grow,center][grow,center][grow,center]",
			"[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]" + GENERATION_SEPARATOR_SIZE + "[]"));
		add(spouse1Grandparents1Panel, "growx 25");
		add(spouse1Grandparents2Panel, "growx 25");
		add(spouse2Grandparents1Panel, "growx 25");
		add(spouse2Grandparents2Panel, "growx 25,wrap");
		add(spouse1ParentsPanel, "span 2,growx 50");
		add(spouse2ParentsPanel, "span 2,growx 50,wrap");
		add(homeFamilyPanel, "span 4,wrap");
		add(childrenScrollPane, "span 4,alignx center");
	}

	private GedcomNode extractParents(GedcomNode child, final GedcomNode family, final String spouseTag){
		if(child == null && family != null)
			child = store.getIndividual(store.traverse(family, spouseTag).getXRef());
		return (child != null && !child.isEmpty()? store.getFamily(store.traverse(child, "FAMILY_CHILD").getXRef()): null);
	}

//	@Override
	protected void paintComponent2(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			final Point husbandParentsExitingPoint = spouse1ParentsPanel.getFamilyPaintingExitPoint();
			final Point wifeParentsExitingPoint = spouse2ParentsPanel.getFamilyPaintingExitPoint();
			final Point homeFamilyEnteringPoint = homeFamilyPanel.getLocation();

			final Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
				new float[]{1}, 0);
			graphics2D.setStroke(dashedStroke);

//			if(spouse1ParentsPanel.isVisible())
//				graphics2D.drawLine(husbandParentsExitingPoint.x, husbandParentsExitingPoint.y,
//					husbandParentsExitingPoint.x, homeFamilyEnteringPoint.y);
//			if(spouse2ParentsPanel.isVisible())
//				graphics2D.drawLine(wifeParentsExitingPoint.x, wifeParentsExitingPoint.y,
//					wifeParentsExitingPoint.x, homeFamilyEnteringPoint.y);

//FIXME
graphics2D.setColor(Color.RED);
final Point p = getChildrenPaintingExitPoint();
graphics2D.drawLine(p.x, p.y, p.x - 20, p.y - 20);
			graphics2D.dispose();
		}
	}

//	public void loadData(Individual individual, boolean treeHasIndividuals){
//		java.util.List<FamilySpouse> familySpouses = individual.getFamiliesWhereSpouse();
//		Family homeFamily = Optional.ofNullable(familySpouses)
//			.filter(list -> !list.isEmpty())
//			//FIXME remember which family was before
//			.map(list -> list.get(0))
//			.map(FamilySpouse::getFamily)
//			.orElse(null);
//		if(homeFamily != null)
//			loadData(homeFamily);
//		else{
//			Sex sex = individual.getSexAsEnum();
//			switch(sex){
//				case MALE:
//				case UNKNOWN: {
//					homeFamilyBoxPanel.loadData(null, individual, null, treeHasIndividuals);
//					java.util.List<FamilyChild> familiesChild = individual.getFamiliesWhereChild();
//					Family individualParents = Optional.ofNullable(familiesChild)
//						.filter(list -> !list.isEmpty())
//						//FIXME remember which family was before
//						.map(list -> list.get(0))
//						.map(FamilyChild::getFamily)
//						.orElse(null);
//					husbandParentsBoxPanel.loadData(individualParents);
//					wifeParentsBoxPanel.loadData(null);
//					childrenBoxPanel.loadData(null);
//
//					husbandParentsBoxPanel.setVisible(true);
//					wifeParentsBoxPanel.setVisible(false);
//				} break;
//
//				case FEMALE: {
//					homeFamilyBoxPanel.loadData(null, null, individual, treeHasIndividuals);
//					java.util.List<FamilyChild> familiesChild = individual.getFamiliesWhereChild();
//					Family individualParents = Optional.ofNullable(familiesChild)
//						.filter(list -> !list.isEmpty())
//						//FIXME remember which family was before
//						.map(list -> list.get(0))
//						.map(FamilyChild::getFamily)
//						.orElse(null);
//					husbandParentsBoxPanel.loadData(null);
//					wifeParentsBoxPanel.loadData(individualParents);
//					childrenBoxPanel.loadData(null);
//
//					husbandParentsBoxPanel.setVisible(false);
//					wifeParentsBoxPanel.setVisible(true);
//				}
//			}
//
//
//			revalidate();
//			repaint();
//		}
//	}

	public void loadData(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode homeFamily){
		this.spouse1 = spouse1;
		this.spouse2 = spouse2;
		this.homeFamily = homeFamily;

		loadData();
	}

	private void loadData(){
		spouse1 = (spouse1 == null && homeFamily != null? store.getIndividual(store.traverse(homeFamily, "SPOUSE1").getXRef()): spouse1);
		spouse2 = (spouse2 == null && homeFamily != null? store.getIndividual(store.traverse(homeFamily, "SPOUSE2").getXRef()): spouse2);

		final GedcomNode spouse1Parents = extractParents(spouse1, homeFamily, "SPOUSE1");
		final GedcomNode spouse2Parents = extractParents(spouse2, homeFamily, "SPOUSE2");

		if(generations <= 3){
			spouse1ParentsPanel.loadData(null, null, spouse1Parents);
			spouse2ParentsPanel.loadData(null, null, spouse2Parents);
			homeFamilyPanel.loadData(spouse1, spouse2, homeFamily);
			childrenPanel.loadData(homeFamily);
		}
		else{
			final GedcomNode spouse1Grandparents1 = extractParents(spouse1, spouse1Parents, "SPOUSE1");
			final GedcomNode spouse1Grandparents2 = extractParents(spouse1, spouse1Parents, "SPOUSE2");
			final GedcomNode spouse2Grandparents1 = extractParents(spouse2, spouse2Parents, "SPOUSE1");
			final GedcomNode spouse2Grandparents2 = extractParents(spouse2, spouse2Parents, "SPOUSE2");

			spouse1Grandparents1Panel.loadData(null, null, spouse1Grandparents1);
			spouse1Grandparents2Panel.loadData(null, null, spouse1Grandparents2);
			spouse2Grandparents1Panel.loadData(null, null, spouse2Grandparents1);
			spouse2Grandparents2Panel.loadData(null, null, spouse2Grandparents2);
			spouse1ParentsPanel.loadData(null, null, spouse1Parents);
			spouse2ParentsPanel.loadData(null, null, spouse2Parents);
			homeFamilyPanel.loadData(spouse1, spouse2, homeFamily);
			childrenPanel.loadData(homeFamily);
		}


//		revalidate();
//		repaint();
	}


	//TODO
	public Point[] getChildrenPaintingEntryPoints(){
		//halfway between spouse1 and spouse2 boxes
//		final int x = (spouse1Panel.getX() + spouse1Panel.getWidth() + spouse2Panel.getX()) / 2;
//		//the bottom point of the marriage panel (that is: bottom point of spouse1 box minus the height of the horizontal connection line
//		//plus half the size of the marriage panel box)
//		final int y = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT + MARRIAGE_PANEL_DIMENSION.height / 2;
//		return new Point(x, y);
		return null;
	}

	public Point getChildrenPaintingExitPoint(){
		final int x = getX() + getWidth() / 2;
		final int y = getY();
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
//		final GedcomNode family = storeFlef.getFamilies().get(0);
//		final GedcomNode family = storeFlef.getFamilies().get(4);
//		final GedcomNode family = storeFlef.getFamilies().get(9);
//		final GedcomNode family = storeFlef.getFamilies().get(64);
		final GedcomNode family = storeFlef.getFamilies().get(75);
//		GedcomNode family = null;

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
			final TreePanel panel = new TreePanel(null, null, family, 4, storeFlef, familyListener,
				individualListener);

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
			frame.setSize(new Dimension(1000, 470));
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
