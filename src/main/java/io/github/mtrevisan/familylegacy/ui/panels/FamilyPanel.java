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
import java.awt.event.*;


public class FamilyPanel extends JPanel{

	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BACKGROUND_COLOR = Color.WHITE;
	private static final Color BORDER_COLOR = Color.BLACK;
	private static final Color BACKGROUND_COLOR_INFO_PANEL = new Color(230, 230, 230);

	/** Height of the marriage line from the botton of the individual panel [px] */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_ICON_DIMENSION = new Dimension(13, 12);

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private IndividualPanel spouse1Panel;
	private IndividualPanel spouse2Panel;
	private JPanel marriagePanel;
	private JMenuItem editFamilyItem;
	private JMenuItem newFamilyItem;
	private JMenuItem linkFamilyItem;

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

		initComponents(family, store, familyListener, individualListener);

		loadData();
	}

	private void initComponents(final GedcomNode family, final Flef store, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		spouse1Panel = new IndividualPanel(spouse1, store, boxType, individualListener);
		spouse2Panel = new IndividualPanel(spouse2, store, boxType, individualListener);
		marriagePanel = new JPanel();

		if(familyListener != null)
			attachPopUpMenu(marriagePanel, family, familyListener);

		setBackground(null);
		setOpaque(false);

		marriagePanel.setBackground(Color.WHITE);
		marriagePanel.setFocusable(false);
		marriagePanel.setInheritsPopupMenu(false);
		marriagePanel.setMaximumSize(MARRIAGE_ICON_DIMENSION);
		marriagePanel.setMinimumSize(MARRIAGE_ICON_DIMENSION);
		marriagePanel.setPreferredSize(MARRIAGE_ICON_DIMENSION);

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(spouse1Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_ICON_DIMENSION.width, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(spouse2Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				)
		);
		final int marriagePanelGapHeight = FAMILY_CONNECTION_HEIGHT - MARRIAGE_ICON_DIMENSION.height / 2;
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(spouse1Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(spouse2Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						)
						.addGroup(layout.createSequentialGroup()
							.addComponent(marriagePanel, GroupLayout.PREFERRED_SIZE, MARRIAGE_ICON_DIMENSION.height, GroupLayout.PREFERRED_SIZE)
							.addGap(marriagePanelGapHeight, marriagePanelGapHeight, marriagePanelGapHeight)
						)
					)
				)
		);
	}

	private void attachPopUpMenu(final JComponent component, final GedcomNode family, final FamilyListenerInterface familyListener){
		final JPopupMenu popupMenu = new JPopupMenu();
		editFamilyItem = new JMenuItem("Edit Family...", 'E');
		editFamilyItem.setEnabled(family != null);
		editFamilyItem.addActionListener(e -> familyListener.onFamilyEdit(this, family));
		popupMenu.add(editFamilyItem);
		newFamilyItem = new JMenuItem("New Family...", 'N');
		newFamilyItem.addActionListener(e -> familyListener.onFamilyNew(this));
		newFamilyItem.setEnabled(family == null);
		popupMenu.add(newFamilyItem);
		linkFamilyItem = new JMenuItem("Link Family...", 'L');
		linkFamilyItem.addActionListener(e -> familyListener.onFamilyLink(this));
		linkFamilyItem.setEnabled(family == null);
		popupMenu.add(linkFamilyItem);

		component.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				processMouseEvent(e);
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				processMouseEvent(e);
			}

			private void processMouseEvent(final MouseEvent e){
				if(e.isPopupTrigger()){
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
					popupMenu.setInvoker(component);
				}
			}
		});
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
		marriagePanel.setBackground(boxType == BoxPanelType.PRIMARY && hasFamily? BACKGROUND_COLOR_INFO_PANEL: BACKGROUND_COLOR);
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
			FamilyPanel panel = new FamilyPanel(family, storeFlef, boxType, familyListener, individualListener);

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
