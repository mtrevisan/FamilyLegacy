package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Protocol;
import io.github.mtrevisan.familylegacy.gedcom.transformations.Transformer;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class TreePanel extends JPanel{

	private static final long serialVersionUID = 4700955059623460223L;

	private static final Color BACKGROUND_COLOR_APPLICATION = new Color(242, 238, 228);

	private static final int GENERATION_SEPARATOR_SIZE = 20;

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private FamilyPanel spouse1Parent1ParentsPanel;
	private FamilyPanel spouse1Parent2ParentsPanel;
	private FamilyPanel spouse2Parent1ParentsPanel;
	private FamilyPanel spouse2Parent2ParentsPanel;
	private FamilyPanel spouse1ParentsPanel;
	private FamilyPanel spouse2ParentsPanel;
	private FamilyPanel homeFamilyPanel;
	private JScrollPane childrenScrollPane;
	private ChildrenPanel childrenPanel;

	private final GedcomNode homeFamily;
	private final Flef store;
	private final IndividualListenerInterface individualListener;
	private final FamilyListenerInterface familyListener;


	public TreePanel(final GedcomNode homeFamily, final int generations, final Flef store, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.homeFamily = homeFamily;
		this.store = store;
		this.individualListener = individualListener;
		this.familyListener = familyListener;

		if(generations <= 3)
			initComponents3Generations(homeFamily);
		else
			initComponents4Generations(homeFamily);

		loadData();
	}

	//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
	//TODO remove duplicated code
	private void initComponents3Generations(final GedcomNode family){
		final GedcomNode spouse1Parents = extractParents(family, "SPOUSE1");
		final GedcomNode spouse2Parents = extractParents(family, "SPOUSE2");

		spouse1ParentsPanel = new FamilyPanel(spouse1Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse2ParentsPanel = new FamilyPanel(spouse2Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		homeFamilyPanel = new FamilyPanel(homeFamily, store, BoxPanelType.PRIMARY, familyListener, individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel));
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		childrenScrollPane.setPreferredSize(new Dimension(0, 90));

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
			.addGroup(layout.createSequentialGroup()
				.addComponent(spouse1ParentsPanel)
				.addGap(FamilyPanel.SPOUSE_SEPARATION)
				.addComponent(spouse2ParentsPanel)
			)
			.addComponent(homeFamilyPanel)
			.addComponent(childrenScrollPane)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(spouse1ParentsPanel)
				.addComponent(spouse2ParentsPanel)
			)
			.addGap(GENERATION_SEPARATOR_SIZE)
			.addComponent(homeFamilyPanel)
			.addGap(GENERATION_SEPARATOR_SIZE)
			.addComponent(childrenScrollPane)
		);
	}

	//TODO remove duplicated code
	private void initComponents4Generations(final GedcomNode family){
		final GedcomNode spouse1Parents = extractParents(family, "SPOUSE1");
		final GedcomNode spouse2Parents = extractParents(family, "SPOUSE2");
		final GedcomNode spouse1Parent1Parents = extractParents(spouse1Parents, "SPOUSE1");
		final GedcomNode spouse1Parent2Parents = extractParents(spouse1Parents, "SPOUSE2");
		final GedcomNode spouse2Parent1Parents = extractParents(spouse2Parents, "SPOUSE1");
		final GedcomNode spouse2Parent2Parents = extractParents(spouse2Parents, "SPOUSE2");

		spouse1Parent1ParentsPanel = new FamilyPanel(spouse1Parent1Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse1Parent2ParentsPanel = new FamilyPanel(spouse1Parent2Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse2Parent1ParentsPanel = new FamilyPanel(spouse2Parent1Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse2Parent2ParentsPanel = new FamilyPanel(spouse2Parent2Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse1ParentsPanel = new FamilyPanel(spouse1Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse2ParentsPanel = new FamilyPanel(spouse2Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		homeFamilyPanel = new FamilyPanel(homeFamily, store, BoxPanelType.PRIMARY, familyListener, individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane = new JScrollPane(new ScrollableContainerHost(childrenPanel));
		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		childrenScrollPane.setPreferredSize(new Dimension(0, 90));

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
			.addGroup(layout.createSequentialGroup()
				.addComponent(spouse1Parent1ParentsPanel)
				.addGap(FamilyPanel.SPOUSE_SEPARATION)
				.addComponent(spouse1Parent2ParentsPanel)
				.addGap(FamilyPanel.SPOUSE_SEPARATION)
				.addComponent(spouse2Parent1ParentsPanel)
				.addGap(FamilyPanel.SPOUSE_SEPARATION)
				.addComponent(spouse2Parent2ParentsPanel)
			)
			.addGroup(layout.createSequentialGroup()
				.addComponent(spouse1ParentsPanel)
				.addGap(FamilyPanel.SPOUSE_SEPARATION)
				.addComponent(spouse2ParentsPanel)
			)
			.addComponent(homeFamilyPanel)
			.addComponent(childrenScrollPane)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(spouse1Parent1ParentsPanel)
				.addComponent(spouse1Parent2ParentsPanel)
				.addComponent(spouse2Parent1ParentsPanel)
				.addComponent(spouse2Parent2ParentsPanel)
			)
			.addGap(GENERATION_SEPARATOR_SIZE)
			.addGroup(layout.createParallelGroup()
				.addComponent(spouse1ParentsPanel)
				.addComponent(spouse2ParentsPanel)
			)
			.addGap(GENERATION_SEPARATOR_SIZE)
			.addComponent(homeFamilyPanel)
			.addGap(GENERATION_SEPARATOR_SIZE)
			.addComponent(childrenScrollPane)
		);
	}

	private GedcomNode extractParents(final GedcomNode family, final String spouseTag){
		GedcomNode parents = null;
		if(family != null){
			final GedcomNode spouse = store.getIndividual(TRANSFORMER.traverse(family, spouseTag).getXRef());
			if(!spouse.isEmpty())
				parents = store.getFamily(TRANSFORMER.traverse(spouse, "FAMILY_CHILD").getXRef());
		}
		return parents;
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

	public void loadData(){
//		Family husbandParents = Optional.ofNullable(family)
//			.map(Family::getHusband)
//			.map(IndividualReference::getIndividual)
//			.map(Individual::getFamiliesWhereChild)
//			.filter(list -> !list.isEmpty())
//			//FIXME remember which family was before
//			.map(list -> list.get(0))
//			.map(FamilyChild::getFamily)
//			.orElse(null);
//		husbandParentsBoxPanel.loadData(husbandParents);
//
//		Family wifeParents = Optional.ofNullable(family)
//			.map(Family::getWife)
//			.map(IndividualReference::getIndividual)
//			.map(Individual::getFamiliesWhereChild)
//			.filter(list -> !list.isEmpty())
//			//FIXME remember which family was before
//			.map(list -> list.get(0))
//			.map(FamilyChild::getFamily)
//			.orElse(null);
//		wifeParentsBoxPanel.loadData(wifeParents);
//
//		homeFamilyBoxPanel.loadData(family);
//
//		List<IndividualReference> children = family.getChildren();
//		childrenBoxPanel.loadData(children);
//
//		husbandParentsBoxPanel.setVisible(Optional.ofNullable(family).map(Family::getHusband).isPresent());
//		wifeParentsBoxPanel.setVisible(Optional.ofNullable(family).map(Family::getWife).isPresent());
//
//
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
		final GedcomNode family = storeFlef.getFamilies().get(4);
//		GedcomNode family = null;

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
			final TreePanel panel = new TreePanel(family, 3, storeFlef, familyListener, individualListener);

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
