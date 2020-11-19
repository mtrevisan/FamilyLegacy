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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class FamilyPanel extends JPanel{

	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BACKGROUND_COLOR = Color.WHITE;
	private static final Color BORDER_COLOR = Color.BLACK;
	private static final Color BACKGROUND_COLOR_INFO_PANEL = new Color(230, 230, 230);

	/** [px] */
	private static final int FAMILY_CONNECTION_DEPTH = 15;
	private static final Dimension MARRIAGE_ICON_DIMENSION = new Dimension(13, 12);

	private static final Transformer TRANSFORMER = new Transformer(Protocol.FLEF);


	private IndividualPanel spouse1Panel;
	private IndividualPanel spouse2Panel;
	private JLabel marriageLabel;
	private JLabel addFamilyLabel;
	private JLabel linkFamilyLabel;

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

		final String individualXRef1 = TRANSFORMER.traverse(family, "SPOUSE1")
			.getXRef();
		final String individualXRef2 = TRANSFORMER.traverse(family, "SPOUSE2")
			.getXRef();
		spouse1 = store.getIndividual(individualXRef1);
		spouse2 = store.getIndividual(individualXRef2);

		initComponents(family, store, familyListener, individualListener);

		loadData();
	}

	private void initComponents(final GedcomNode family, final Flef store, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		spouse1Panel = new IndividualPanel(spouse1, store, boxType, individualListener);
		spouse2Panel = new IndividualPanel(spouse2, store, boxType, individualListener);
		marriageLabel = new JLabel();
		addFamilyLabel = new JLabel();

		setBackground(null);
		setOpaque(false);
		if(familyListener != null){
			marriageLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					if(family == null)
						familyListener.onFamilyLink(FamilyPanel.this);
				}
			});
		}

		marriageLabel.setBackground(Color.WHITE);
//		marriageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/marriageType.unknown.png"))); // NOI18N
		marriageLabel.setFocusable(false);
		marriageLabel.setInheritsPopupMenu(false);
		marriageLabel.setMaximumSize(MARRIAGE_ICON_DIMENSION);
		marriageLabel.setMinimumSize(MARRIAGE_ICON_DIMENSION);
		marriageLabel.setPreferredSize(MARRIAGE_ICON_DIMENSION);
		if(familyListener != null)
			marriageLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					final int clickCount = evt.getClickCount();
					if(clickCount == 2 && family != null)
						familyListener.onFamilyEdit(FamilyPanel.this, family);
				}
			});

		addFamilyLabel.setText("Add Family");
		addFamilyLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		addFamilyLabel.setFocusable(false);
		if(familyListener != null)
			addFamilyLabel.addMouseListener(new MouseAdapter(){
				public void mouseClicked(final MouseEvent evt){
					familyListener.onFamilyNew(FamilyPanel.this);
				}
			});
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new Insets(4, 4, 4, 4);
		marriageLabel.add(addFamilyLabel, gridBagConstraints);

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(spouse1Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(marriageLabel, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(spouse2Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE)
				)
				.addGroup(layout.createSequentialGroup()
					.addGap(100, 100, 100)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(marriageLabel, GroupLayout.PREFERRED_SIZE, 621, GroupLayout.PREFERRED_SIZE)
					)
					.addGap(100, 100, 100)
				)
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGap(0, 0, 0)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(marriageLabel, GroupLayout.PREFERRED_SIZE, 12, GroupLayout.PREFERRED_SIZE)
							.addGap(10, 10, 10))
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(spouse1Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(spouse2Panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						)
					)
					.addGap(0, 0, 0)
					.addComponent(marriageLabel, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
					.addGap(1, 1, 1)
				)
		);
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			//info-panel-width = 1.5 * individual-box-width = 622 px
			//info-panel-location = ((container-width - info-panel-width) / 2, info-panel-y)
			final int xFrom = spouse1Panel.getX() + spouse1Panel.getWidth();
			final int xTo = spouse2Panel.getX();
			final int yFrom = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_DEPTH;
			Stroke dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f, new float[]{1.f}, 0.f);
			graphics2D.setStroke(dashedStroke);
			//horizontal line between spouses
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);
			if(boxType == BoxPanelType.PRIMARY){
				final int yTo = marriageLabel.getY();
				//vertical line down to infoPanel
				graphics2D.drawLine((xFrom + xTo) / 2, yFrom, (xFrom + xTo) / 2, yTo);
			}
//			if(family == null){
//				graphics2D.setColor(BACKGROUND_COLOR);
//				final int x = marriageTypeLabel.getX();
//				final int y = marriageTypeLabel.getY();
//				graphics2D.fillRect(x, y, MARRIAGE_TYPE_ICON_DIMENSION.width, MARRIAGE_TYPE_ICON_DIMENSION.height);
//
//				graphics2D.setColor(IndividualPanel.BORDER_COLOR);
//				dashedStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.f, new float[]{5.f}, 0.f);
//				graphics2D.setStroke(dashedStroke);
//
//				graphics2D.drawRect(x, y, MARRIAGE_TYPE_ICON_DIMENSION.width, MARRIAGE_TYPE_ICON_DIMENSION.height);
//
//				if(boxType == BoxPanelType.PRIMARY){
//					final Point loc = infoPanel.getLocation();
//					final Dimension size = infoPanel.getPreferredSize();
////TODO draw line above infoPanel
////					graphics2D.drawRect(loc.x, loc.y, (int)(spouse1Panel.getWidth() * 1.5), size.height - 1);
////TODO fix the magic number 622
//					graphics2D.drawRect(loc.x - 1, loc.y - 1, 622 + 1, size.height + 1);
//				}
//			}

			graphics2D.dispose();
		}
	}

	private void loadData(){
		spouse1Panel.loadData(boxType);
		spouse2Panel.loadData(boxType);

		final boolean hasFamily = (family != null);
		marriageLabel.setBorder(hasFamily? BorderFactory.createLineBorder(BORDER_COLOR): null);
		marriageLabel.setBackground(boxType == BoxPanelType.PRIMARY && hasFamily? BACKGROUND_COLOR_INFO_PANEL: BACKGROUND_COLOR);
		addFamilyLabel.setVisible(!hasFamily);
//		if(hasFamily){
//			final MarriageType marriageType = getMarriageType(family);
//			final ImageIcon marriageTypeIcon = marriageType.getIcon();
//			marriageTypeLabel.setIcon(marriageTypeIcon);
//			statusLabel.setIcon(marriageTypeIcon);
//			statusLabel.setText(marriageType.getDescription());
//			final List<FamilyEvent> events = Collections.<FamilyEvent>emptyList();
//			if(family != null){
//				events = family.getEventsOfType(FamilyEventType.MARRIAGE);
//				if(events.isEmpty())
//					events = family.getEventsOfType(FamilyEventType.MARRIAGE_CONTRACT);
//			}
//			if(!events.isEmpty()){
//				final FamilyEvent marriageEvent = events.get(0);
//				final String marriageDate = Optional.ofNullable(marriageEvent)
//					.map(FamilyEvent::getDate)
//					.map(StringWithCustomFacts::getValue)
//					.orElse(null);
//				infoDateLabel.setVisible(marriageDate != null);
//				dateLabel.setText(DateParser.formatDate(marriageDate));
//				final String marriagePlace = Optional.ofNullable(marriageEvent)
//					.map(FamilyEvent::getPlace)
//					.map(Place::getPlaceName)
//					.orElse(null);
//				infoPlaceLabel.setVisible(marriagePlace != null);
//				placeLabel.setText(marriagePlace);
//			}
//			else{
//				infoDateLabel.setVisible(false);
//				dateLabel.setText(null);
//				infoPlaceLabel.setVisible(false);
//				placeLabel.setText(null);
//			}
//			final String childrenNumber = Optional.ofNullable(family)
//				.map(Family::getChildrenNumber)
//				.map(StringWithCustomFacts::getValue)
//				.orElse("0");
//			childrenLabel.setText(childrenNumber);
//		}
//		else
//			marriageTypeLabel.setIcon(null);
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
