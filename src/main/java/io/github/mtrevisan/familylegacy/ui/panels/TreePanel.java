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

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private FamilyPanel spouse1ParentsPanel;
	private FamilyPanel spouse2ParentsPanel;
	private FamilyPanel homeFamilyPanel;
	private final JScrollPane childrenScrollPane = new JScrollPane();
	private ChildrenPanel childrenPanel;

	private final GedcomNode homeFamily;
	private final Flef store;
	private final IndividualListenerInterface individualListener;
	private final FamilyListenerInterface familyListener;


	public TreePanel(final GedcomNode homeFamily, final Flef store, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.homeFamily = homeFamily;
		this.store = store;
		this.individualListener = individualListener;
		this.familyListener = familyListener;

		initComponents(homeFamily, store);

		loadData();
	}

	//FIXME remove horizontal scrollbar, enable drag
	private void initComponents(final GedcomNode family, final Flef store){
		//TODO extract spouse1 parents
		GedcomNode spouse1Parents = null;
		//TODO extract spouse2 parents
		GedcomNode spouse2Parents = null;

		spouse1ParentsPanel = new FamilyPanel(spouse1Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		spouse2ParentsPanel = new FamilyPanel(spouse2Parents, store, BoxPanelType.SECONDARY, familyListener, individualListener);
		homeFamilyPanel = new FamilyPanel(homeFamily, store, BoxPanelType.PRIMARY, familyListener, individualListener);
		childrenPanel = new ChildrenPanel(homeFamily, store, individualListener);

		setBackground(BACKGROUND_COLOR_APPLICATION);

		childrenScrollPane.setBorder(null);
		childrenScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		childrenScrollPane.setAutoscrolls(true);
		childrenScrollPane.setPreferredSize(new java.awt.Dimension(0, 105));

		childrenPanel.setBackground(BACKGROUND_COLOR_APPLICATION);
		childrenPanel.setBorder(null);

		final GroupLayout childrenPanelLayout = new GroupLayout(childrenPanel);
		childrenPanel.setLayout(childrenPanelLayout);
		childrenPanelLayout.setHorizontalGroup(
			childrenPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 0, Short.MAX_VALUE)
		);
		childrenPanelLayout.setVerticalGroup(
			childrenPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 109, Short.MAX_VALUE)
		);

		childrenScrollPane.setViewportView(childrenPanel);

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(homeFamilyPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGroup(layout.createSequentialGroup()
							.addComponent(spouse1ParentsPanel, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE)
							.addGap(18, 18, 18)
							.addComponent(spouse2ParentsPanel, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE)
							.addGap(0, 0, Short.MAX_VALUE)
						)
						.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
							.addGap(0, 0, Short.MAX_VALUE)
							.addComponent(childrenScrollPane, GroupLayout.PREFERRED_SIZE, 821, GroupLayout.PREFERRED_SIZE)
							.addGap(0, 0, Short.MAX_VALUE)
						)
					)
					.addContainerGap()
				)
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(spouse1ParentsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(spouse2ParentsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					)
					.addGap(18, 18, 18)
					.addComponent(homeFamilyPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18, 18, 18)
					.addComponent(childrenScrollPane, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				)
		);
	}

	@Override
	protected void paintComponent(Graphics g){
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

			if(spouse1ParentsPanel.isVisible())
				graphics2D.drawLine(husbandParentsExitingPoint.x, husbandParentsExitingPoint.y,
					husbandParentsExitingPoint.x, homeFamilyEnteringPoint.y);
			if(spouse2ParentsPanel.isVisible())
				graphics2D.drawLine(wifeParentsExitingPoint.x, wifeParentsExitingPoint.y,
					wifeParentsExitingPoint.x, homeFamilyEnteringPoint.y);

//FIXME
graphics2D.setColor(Color.RED);
Point p = getChildrenPaintingExitPoint();
graphics2D.drawLine(p.x, p.y, p.x - 20, p.y - 20);
			graphics2D.dispose();
		}
	}

	public void loadData(){
		//FIXME really needed?
//		removeAll();

//		final GroupLayout layout = new GroupLayout(this);
//		sequentialGroup = layout.createSequentialGroup();
//		parallelGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
//		layout.setHorizontalGroup(sequentialGroup);
//		layout.setVerticalGroup(parallelGroup);
//		setLayout(layout);
//
//		final List<GedcomNode> children = TRANSFORMER.traverseAsList(homeFamily, "CHILD[]");
//		if(!children.isEmpty()){
//			sequentialGroup.addGap(0, 0, Short.MAX_VALUE);
//
//			final Iterator<GedcomNode> itr = children.iterator();
//			while(itr.hasNext()){
//				final String individualXRef = itr.next().getXRef();
//				final GedcomNode individual = store.getIndividual(individualXRef);
//				final IndividualPanel individualBox = new IndividualPanel(individual, store, BoxPanelType.SECONDARY, individualListener);
//
//				sequentialGroup.addComponent(individualBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
//				if(itr.hasNext())
//					sequentialGroup.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
//				parallelGroup.addComponent(individualBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
//			}
//
//			revalidate();
//			repaint();
//		}
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


	public static void main(String args[]) throws GedcomParseException, GedcomGrammarParseException{
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception e){}

		Store storeGedcom = new Gedcom();
		Flef storeFlef = (Flef)storeGedcom.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged")
			.transform();
		GedcomNode family = storeFlef.getFamilies().get(0);
//		GedcomNode family = null;
		BoxPanelType boxType = BoxPanelType.PRIMARY;

		FamilyListenerInterface familyListener = new FamilyListenerInterface(){
			@Override
			public void onFamilyEdit(FamilyPanel boxPanel, GedcomNode family){
				System.out.println("onEditFamily " + family.getID());
			}

			@Override
			public void onFamilyFocus(FamilyPanel boxPanel, GedcomNode family){
				System.out.println("onFocusFamily " + family.getID());
			}

			@Override
			public void onFamilyNew(FamilyPanel boxPanel){
				System.out.println("onNewFamily");
			}

			@Override
			public void onFamilyLink(FamilyPanel boxPanel){
				System.out.println("onLinkFamily");
			}

			@Override
			public void onFamilyAddChild(FamilyPanel familyPanel, GedcomNode family){
				System.out.println("onAddChildFamily");
			}
		};
		IndividualListenerInterface individualListener = new IndividualListenerInterface(){
			@Override
			public void onIndividualEdit(IndividualPanel boxPanel, GedcomNode individual){
				System.out.println("onEditIndividual " + individual.getID());
			}

			@Override
			public void onIndividualFocus(IndividualPanel boxPanel, GedcomNode individual){
				System.out.println("onFocusIndividual " + individual.getID());
			}

			@Override
			public void onIndividualNew(IndividualPanel boxPanel){
				System.out.println("onNewIndividual");
			}

			@Override
			public void onIndividualLink(IndividualPanel boxPanel){
				System.out.println("onLinkIndividual");
			}

			@Override
			public void onIndividualAddPreferredImage(IndividualPanel boxPanel, GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			TreePanel panel = new TreePanel(family, storeFlef, familyListener, individualListener);

			JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(WindowEvent e){
					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
