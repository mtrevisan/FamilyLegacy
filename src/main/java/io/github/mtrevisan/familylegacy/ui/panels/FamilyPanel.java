/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.gedcom.parsers.calendars.DateParser;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.enums.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.enums.SelectedNodeType;
import io.github.mtrevisan.familylegacy.ui.interfaces.FamilyListenerInterface;
import io.github.mtrevisan.familylegacy.ui.interfaces.IndividualListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;


//http://www.miglayout.com/whitepaper.html
//http://www.miglayout.com/QuickStart.pdf
//https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf
//https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
public class FamilyPanel extends JPanel{

	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BORDER_COLOR = Color.BLACK;

	private static final double SPOUSE_PREV_NEXT_WIDTH = 12.;
	private static final double SPOUSE_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension SPOUSE_PREVIOUS_NEXT_SIZE = new Dimension((int)SPOUSE_PREV_NEXT_WIDTH,
		(int)(SPOUSE_PREV_NEXT_WIDTH * SPOUSE_PREV_NEXT_ASPECT_RATIO));

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon SPOUSE_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/previous.png", SPOUSE_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon SPOUSE_PREVIOUS_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(SPOUSE_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon SPOUSE_NEXT_ENABLED = ResourceHelper.getImage("/images/next.png", SPOUSE_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon SPOUSE_NEXT_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(SPOUSE_NEXT_ENABLED.getImage()));

	/** Height of the marriage line from the bottom of the individual panel [px]. */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_PANEL_DIMENSION = new Dimension(13, 12);
	public static final int FAMILY_EXITING_HEIGHT = FAMILY_CONNECTION_HEIGHT - MARRIAGE_PANEL_DIMENSION.height / 2;
	public static final int HALF_SPOUSE_SEPARATION = 10;
	static final int FAMILY_SEPARATION = HALF_SPOUSE_SEPARATION + MARRIAGE_PANEL_DIMENSION.width + HALF_SPOUSE_SEPARATION;
	/** Distance between navigation arrow and box. */
	static final int NAVIGATION_ARROW_SEPARATION = 3;

	static final int NAVIGATION_ARROW_HEIGHT = SPOUSE_PREVIOUS_ENABLED.getIconHeight() + NAVIGATION_ARROW_SEPARATION;

	private static final String KEY_ENABLED = "enabled";

	public static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);


	private IndividualPanel spouse1Panel;
	private IndividualPanel spouse2Panel;
	private final JLabel spouse1PreviousLabel = new JLabel();
	private final JLabel spouse1NextLabel = new JLabel();
	private final JLabel spouse2PreviousLabel = new JLabel();
	private final JLabel spouse2NextLabel = new JLabel();
	private final JPanel marriagePanel = new JPanel();
	private final JMenuItem editFamilyItem = new JMenuItem("Edit Family…", 'E');
	private final JMenuItem linkFamilyItem = new JMenuItem("Link Family…", 'L');
	private final JMenuItem unlinkFamilyItem = new JMenuItem("Unlink Family…", 'U');
	private final JMenuItem removeFamilyItem = new JMenuItem("Remove Family…", 'R');

	private GedcomNode spouse1;
	private GedcomNode spouse2;
	private GedcomNode family;
	private final Flef store;
	private final BoxPanelType boxType;
	private final FamilyListenerInterface familyListener;
	private final IndividualListenerInterface individualListener;

	private GedcomNode childReference;


	public FamilyPanel(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode family, final GedcomNode childReference,
			final Flef store, final BoxPanelType boxType, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.store = store;
		this.familyListener = familyListener;
		this.individualListener = individualListener;

		//FIXME familyIndex 0
		int familyIndex = 0;
		this.spouse1 = (spouse1 == null && family != null? store.getSpouse1(family, familyIndex): spouse1);
		this.spouse2 = (spouse2 == null && family != null? store.getSpouse2(family, familyIndex): spouse2);
		this.family = family;
		this.childReference = childReference;
		if(family != null && this.childReference == null){
			final List<GedcomNode> children = store.traverseAsList(family, "CHILD[]");
			this.childReference = (!children.isEmpty()? children.get(0): null);
		}
		this.boxType = boxType;

		initComponents();

		loadData();

		EventBusService.subscribe(this);
	}

	private void initComponents(){
		setOpaque(false);
		if(familyListener != null)
			addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						familyListener.onFamilyEdit(FamilyPanel.this, family);
				}
			});

		spouse1Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL1, spouse1, store, boxType, individualListener);
		spouse1Panel.setChildReference(childReference);
		spouse2Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL2, spouse2, store, boxType, individualListener);
		spouse2Panel.setChildReference(childReference);
		marriagePanel.setBackground(Color.WHITE);
		marriagePanel.setMaximumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setMinimumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setPreferredSize(MARRIAGE_PANEL_DIMENSION);

		if(familyListener != null){
			attachPopUpMenu(marriagePanel, family, familyListener);

			refresh(Flef.ACTION_COMMAND_FAMILY_COUNT);

			spouse1PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)spouse1PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousSpouse(FamilyPanel.this, spouse2, spouse1, family);
				}
			});
			spouse1NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)spouse1NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextSpouse(FamilyPanel.this, spouse2, spouse1, family);
				}
			});
			spouse2PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)spouse2PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousSpouse(FamilyPanel.this, spouse1, spouse2, family);
				}
			});
			spouse2NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)spouse2NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextSpouse(FamilyPanel.this, spouse1, spouse2, family);
				}
			});
		}

		final Dimension minimumSize = new Dimension(SPOUSE_PREVIOUS_ENABLED.getIconWidth(), SPOUSE_PREVIOUS_ENABLED.getIconHeight());
		if(boxType == BoxPanelType.PRIMARY){
			spouse1PreviousLabel.setMinimumSize(minimumSize);
			spouse1NextLabel.setMinimumSize(minimumSize);
			spouse2PreviousLabel.setMinimumSize(minimumSize);
			spouse2NextLabel.setMinimumSize(minimumSize);
		}

		setMaximumSize(new Dimension(
			(spouse1Panel.getMaximumSize().width + FAMILY_CONNECTION_HEIGHT) * 2 + MARRIAGE_PANEL_DIMENSION.height,
			spouse1Panel.getMaximumSize().height + minimumSize.height
		));

		setLayout(new MigLayout("insets 0",
			"[grow]" + HALF_SPOUSE_SEPARATION + "[]" + HALF_SPOUSE_SEPARATION + "[grow]",
			"[]0[]"));
		add(spouse1PreviousLabel, "split 2,alignx right,gapright 10,gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(spouse1NextLabel, "gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(new JLabel());
		add(spouse2PreviousLabel, "split 2,gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(spouse2NextLabel, "gapright 10,gapbottom " + NAVIGATION_ARROW_SEPARATION + ",wrap");
		add(spouse1Panel, "growx 50");
		add(marriagePanel, "aligny bottom,gapbottom " + FAMILY_EXITING_HEIGHT);
		add(spouse2Panel, "growx 50");
	}

	private void attachPopUpMenu(final JComponent component, final GedcomNode family, final FamilyListenerInterface familyListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editFamilyItem.setEnabled(family != null);
		editFamilyItem.addActionListener(e -> familyListener.onFamilyEdit(this, family));
		popupMenu.add(editFamilyItem);

		linkFamilyItem.addActionListener(e -> familyListener.onFamilyLink(this));
//		linkFamilyItem.setEnabled(family == null);
		popupMenu.add(linkFamilyItem);

		unlinkFamilyItem.addActionListener(e -> familyListener.onFamilyUnlink(this, family));
		unlinkFamilyItem.setEnabled(family != null);
		popupMenu.add(unlinkFamilyItem);

		removeFamilyItem.addActionListener(e -> familyListener.onFamilyRemove(this, family));
		removeFamilyItem.setEnabled(family != null);
		popupMenu.add(removeFamilyItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D && spouse1Panel != null && spouse2Panel != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = spouse1Panel.getX() + spouse1Panel.getWidth();
			final int xTo = spouse2Panel.getX();
			final int yFrom = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT;
			//horizontal line between spouses
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);

			graphics2D.dispose();
		}
	}

	public void loadData(final GedcomNode family){
		loadData(null, null, family);
	}

	public void loadData(final GedcomNode spouse1, final GedcomNode spouse2, final GedcomNode family){
		//FIXME familyIndex 0
		int familyIndex = 0;
		this.spouse1 = (spouse1 == null && family != null? store.getSpouse1(family, familyIndex): spouse1);
		this.spouse2 = (spouse2 == null && family != null? store.getSpouse2(family, familyIndex): spouse2);
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		spouse1Panel.loadData(spouse1);
		spouse2Panel.loadData(spouse2);

		if(boxType == BoxPanelType.PRIMARY){
			updatePreviousNextSpouseIcons(family, spouse2, spouse1PreviousLabel, spouse1NextLabel);
			updatePreviousNextSpouseIcons(family, spouse1, spouse2PreviousLabel, spouse2NextLabel);
		}

		marriagePanel.setBorder(family != null? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	public void refresh(final Integer actionCommand){
		if(actionCommand != Flef.ACTION_COMMAND_FAMILY_COUNT)
			return;

		linkFamilyItem.setEnabled(family == null && store.hasFamilies());
	}

	public void updatePreviousNextSpouseIcons(final GedcomNode family, final GedcomNode otherSpouse, final JLabel spousePreviousLabel,
			final JLabel spouseNextLabel){
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

		final boolean spousePreviousEnabled = (currentFamilyIndex > 0);
		spousePreviousLabel.putClientProperty(KEY_ENABLED, spousePreviousEnabled);
		spousePreviousLabel.setCursor(Cursor.getPredefinedCursor(spousePreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreFamilies)
			icon = (spousePreviousEnabled? SPOUSE_PREVIOUS_ENABLED: SPOUSE_PREVIOUS_DISABLED);
		spousePreviousLabel.setIcon(icon);

		final boolean spouseNextEnabled = (currentFamilyIndex < otherMarriagesCount - 1);
		spouseNextLabel.putClientProperty(KEY_ENABLED, spouseNextEnabled);
		spouseNextLabel.setCursor(Cursor.getPredefinedCursor(spouseNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreFamilies)
			icon = (spouseNextEnabled? SPOUSE_NEXT_ENABLED: SPOUSE_NEXT_DISABLED);
		spouseNextLabel.setIcon(icon);
	}

	public static String extractEarliestMarriageYear(final GedcomNode family, final Flef store){
		int marriageYear = 0;
		String marriageDate = null;
		for(final GedcomNode node : store.traverseAsList(family, "EVENT{MARRIAGE}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(marriageDate == null || my < marriageYear){
					marriageYear = my;
					marriageDate = DateParser.extractYear(dateValue);
				}
			}
		}
		return marriageDate;
	}

	public static String extractEarliestMarriagePlace(final GedcomNode family, final Flef store){
		int marriageYear = 0;
		String marriagePlace = null;
		for(final GedcomNode node : store.traverseAsList(family, "EVENT{MARRIAGE}[]")){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(marriagePlace == null || my < marriageYear){
					final GedcomNode place = store.getPlace(store.traverse(node, "PLACE").getXRef());
					if(place != null){
						final String placeValue = extractPlace(place, store);
						if(placeValue != null){
							marriageYear = my;
							marriagePlace = placeValue;
						}
					}
				}
			}
		}
		return marriagePlace;
	}

	private static String extractPlace(final GedcomNode place, final Flef store){
		final GedcomNode addressEarliest = extractEarliestAddress(place, store);

		//extract place as town, county, state, country, otherwise from value
		String placeValue = place.getValue();
		if(addressEarliest != null){
			final GedcomNode town = store.traverse(addressEarliest, "TOWN");
			final GedcomNode city = store.traverse(addressEarliest, "CITY");
			final GedcomNode county = store.traverse(addressEarliest, "COUNTY");
			final GedcomNode state = store.traverse(addressEarliest, "STATE");
			final GedcomNode country = store.traverse(addressEarliest, "COUNTRY");
			final StringJoiner sj = new StringJoiner(", ");
			JavaHelper.addValueIfNotNull(sj, town);
			JavaHelper.addValueIfNotNull(sj, city);
			JavaHelper.addValueIfNotNull(sj, county);
			JavaHelper.addValueIfNotNull(sj, state);
			JavaHelper.addValueIfNotNull(sj, country);
			if(sj.length() > 0)
				placeValue = sj.toString();
		}
		return placeValue;
	}

	private static GedcomNode extractEarliestAddress(final GedcomNode place, final Flef store){
		int addressYear = 0;
		GedcomNode addressEarliest = null;
		final List<GedcomNode> addresses = store.traverseAsList(place, "ADDRESS[]");
		for(final GedcomNode address : addresses){
			final GedcomNode source = store.getSource(store.traverse(address, "SOURCE").getXRef());
			final String addressDateValue = store.traverse(source, "DATE").getValue();
			final LocalDate addressDate = DateParser.parse(addressDateValue);
			if(addressDate != null){
				final int ay = addressDate.getYear();
				if(addressEarliest == null || ay < addressYear){
					addressYear = ay;
					addressEarliest = address;
				}
			}
		}
		return addressEarliest;
	}


	public GedcomNode getChildReference(){
		return childReference;
	}

	public Point getFamilyPaintingSpouse1EnterPoint(){
		final Point p = spouse1Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingSpouse2EnterPoint(){
		final Point p = spouse2Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingExitPoint(){
		//halfway between spouse1 and spouse2 boxes
		final int x = (spouse1Panel.getX() + spouse1Panel.getWidth() + spouse2Panel.getX()) / 2;
		//the bottom point of the marriage panel (that is: bottom point of spouse1 box minus the height of the horizontal connection line
		//plus half the size of the marriage panel box)
		final int y = spouse1Panel.getY() + spouse1Panel.getHeight() - FAMILY_EXITING_HEIGHT;
		final Point origin = getLocation();
		return new Point(origin.x + x, origin.y + y);
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
//		final GedcomNode family = null;
		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

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
			public void onFamilyUnlink(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onUnlinkFamily " + family.getID());
			}

			@Override
			public void onFamilyRemove(final FamilyPanel boxPanel, final GedcomNode family){
				System.out.println("onRemoveFamily " + family.getID());
			}

			@Override
			public void onFamilyPreviousSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse,
					final GedcomNode currentFamily){
				System.out.println("onPrevSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID()
					+ ", family: " + currentFamily.getID());
			}

			@Override
			public void onFamilyNextSpouse(final FamilyPanel familyPanel, final GedcomNode thisSpouse, final GedcomNode otherSpouse,
					final GedcomNode currentFamily){
				System.out.println("onNextSpouseFamily this: " + thisSpouse.getID() + ", other: " + otherSpouse.getID()
					+ ", family: " + currentFamily.getID());
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
			public void onIndividualLink(final IndividualPanel boxPanel, final SelectedNodeType type){
				System.out.println("onLinkIndividual " + type);
			}

			@Override
			public void onIndividualUnlink(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onUnlinkIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAdd(final IndividualPanel boxPanel){
				System.out.println("onAddIndividual");
			}

			@Override
			public void onIndividualRemove(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onRemoveIndividual " + individual.getID());
			}

			@Override
			public void onIndividualAddPreferredImage(final IndividualPanel boxPanel, final GedcomNode individual){
				System.out.println("onAddPreferredImage " + individual.getID());
			}
		};

		EventQueue.invokeLater(() -> {
			final FamilyPanel panel = new FamilyPanel(null, null, family, null, storeFlef, boxType,
				familyListener, individualListener);

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


//			final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
////			final Runnable task = () -> panel.loadData(null, null, storeFlef.getFamilies().get(1), BoxPanelType.SECONDARY);
//			final Runnable task = () -> panel.loadData(null, null, storeFlef.getFamilies().get(1), BoxPanelType.PRIMARY);
////			final Runnable task = () -> panel.loadData(null, null, null, BoxPanelType.PRIMARY);
//			scheduler.schedule(task, 3, TimeUnit.SECONDS);
		});
	}

}
