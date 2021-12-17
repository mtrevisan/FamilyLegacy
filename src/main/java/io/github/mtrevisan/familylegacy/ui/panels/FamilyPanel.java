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

import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


//http://www.miglayout.com/whitepaper.html
//http://www.miglayout.com/QuickStart.pdf
//https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf
//https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
public class FamilyPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BORDER_COLOR = Color.BLACK;

	private static final double PARTNER_PREV_NEXT_WIDTH = 12.;
	private static final double PARTNER_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension PARTNER_PREVIOUS_NEXT_SIZE = new Dimension((int)PARTNER_PREV_NEXT_WIDTH,
		(int)(PARTNER_PREV_NEXT_WIDTH * PARTNER_PREV_NEXT_ASPECT_RATIO));

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon PARTNER_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/previous.png", PARTNER_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon PARTNER_PREVIOUS_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(PARTNER_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon PARTNER_NEXT_ENABLED = ResourceHelper.getImage("/images/next.png", PARTNER_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon PARTNER_NEXT_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(PARTNER_NEXT_ENABLED.getImage()));

	/** Height of the marriage line from the bottom of the individual panel [px]. */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_PANEL_DIMENSION = new Dimension(13, 12);
	public static final int FAMILY_EXITING_HEIGHT = FAMILY_CONNECTION_HEIGHT - MARRIAGE_PANEL_DIMENSION.height / 2;
	public static final int HALF_PARTNER_SEPARATION = 10;
	static final int FAMILY_SEPARATION = HALF_PARTNER_SEPARATION + MARRIAGE_PANEL_DIMENSION.width + HALF_PARTNER_SEPARATION;
	/** Distance between navigation arrow and box. */
	static final int NAVIGATION_ARROW_SEPARATION = 3;

	static final int NAVIGATION_ARROW_HEIGHT = PARTNER_PREVIOUS_ENABLED.getIconHeight() + NAVIGATION_ARROW_SEPARATION;

	private static final String KEY_ENABLED = "enabled";

	public static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	public static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);


	private IndividualPanel partner1Panel;
	private IndividualPanel partner2Panel;
	private final JLabel partner1PreviousLabel = new JLabel();
	private final JLabel partner1NextLabel = new JLabel();
	private final JLabel partner2PreviousLabel = new JLabel();
	private final JLabel partner2NextLabel = new JLabel();
	private final JPanel marriagePanel = new JPanel();
	private final JMenuItem editFamilyItem = new JMenuItem("Edit Family…", 'E');
	private final JMenuItem linkFamilyItem = new JMenuItem("Link Family…", 'L');
	private final JMenuItem unlinkFamilyItem = new JMenuItem("Unlink Family…", 'U');
	private final JMenuItem removeFamilyItem = new JMenuItem("Remove Family…", 'R');

	private GedcomNode partner1;
	private GedcomNode partner2;
	private GedcomNode family;
	private final Flef store;
	private final BoxPanelType boxType;
	private final FamilyListenerInterface familyListener;
	private final IndividualListenerInterface individualListener;

	private GedcomNode childReference;


	public FamilyPanel(final GedcomNode partner1, final GedcomNode partner2, final GedcomNode family, final GedcomNode childReference,
			final Flef store, final BoxPanelType boxType, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.store = store;
		this.familyListener = familyListener;
		this.individualListener = individualListener;

		this.partner1 = (partner1 == null && family != null? store.getPartner1(family): partner1);
		this.partner2 = (partner2 == null && family != null? store.getPartner2(family): partner2);
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

		partner1Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL1, partner1, store, boxType, individualListener);
		partner1Panel.setChildReference(childReference);
		partner2Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL2, partner2, store, boxType, individualListener);
		partner2Panel.setChildReference(childReference);
		marriagePanel.setBackground(Color.WHITE);
		marriagePanel.setMaximumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setMinimumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setPreferredSize(MARRIAGE_PANEL_DIMENSION);

		if(familyListener != null){
			attachPopUpMenu(marriagePanel, family, familyListener);

			refresh(Flef.ACTION_COMMAND_FAMILY_COUNT);

			partner1PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousPartner(FamilyPanel.this, partner2, partner1, family);
				}
			});
			partner1NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextPartner(FamilyPanel.this, partner2, partner1, family);
				}
			});
			partner2PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousPartner(FamilyPanel.this, partner1, partner2, family);
				}
			});
			partner2NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextPartner(FamilyPanel.this, partner1, partner2, family);
				}
			});
		}

		final Dimension minimumSize = new Dimension(PARTNER_PREVIOUS_ENABLED.getIconWidth(), PARTNER_PREVIOUS_ENABLED.getIconHeight());
		if(boxType == BoxPanelType.PRIMARY){
			partner1PreviousLabel.setMinimumSize(minimumSize);
			partner1NextLabel.setMinimumSize(minimumSize);
			partner2PreviousLabel.setMinimumSize(minimumSize);
			partner2NextLabel.setMinimumSize(minimumSize);
		}

		final Dimension partnerPanelMaximumSize = partner1Panel.getMaximumSize();
		setMaximumSize(new Dimension(
			partnerPanelMaximumSize.width * 2 + MARRIAGE_PANEL_DIMENSION.width,
			partnerPanelMaximumSize.height + minimumSize.height
		));

		final int navigationArrowGap = (boxType == BoxPanelType.PRIMARY? NAVIGATION_ARROW_SEPARATION: 0);
		setLayout(new MigLayout("insets 0",
			"[grow]" + HALF_PARTNER_SEPARATION + "[]" + HALF_PARTNER_SEPARATION + "[grow]",
			"[]0[]"));
		add(partner1PreviousLabel, "split 2,alignx right,gapright 10,gapbottom " + navigationArrowGap);
		add(partner1NextLabel, "gapbottom " + navigationArrowGap);
		add(new JLabel());
		add(partner2PreviousLabel, "split 2,gapbottom " + navigationArrowGap);
		add(partner2NextLabel, "gapright 10,gapbottom " + navigationArrowGap + ",wrap");
		add(partner1Panel, "growx 50");
		add(marriagePanel, "aligny bottom,gapbottom " + FAMILY_EXITING_HEIGHT);
		add(partner2Panel, "growx 50");
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

		if(g instanceof Graphics2D && partner1Panel != null && partner2Panel != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = partner1Panel.getX() + partner1Panel.getWidth();
			final int xTo = partner2Panel.getX();
			final int yFrom = partner1Panel.getY() + partner1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT;
			//horizontal line between partners
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);

			graphics2D.dispose();
		}
	}

	public void loadData(final GedcomNode family){
		loadData(null, null, family);
	}

	public void loadData(final GedcomNode partner1, final GedcomNode partner2, final GedcomNode family){
		this.partner1 = (partner1 == null && family != null? store.getPartner1(family): partner1);
		this.partner2 = (partner2 == null && family != null? store.getPartner2(family): partner2);
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		partner1Panel.loadData(partner1);
		partner2Panel.loadData(partner2);

		if(boxType == BoxPanelType.PRIMARY){
			updatePreviousNextPartnerIcons(family, partner2, partner1PreviousLabel, partner1NextLabel);
			updatePreviousNextPartnerIcons(family, partner1, partner2PreviousLabel, partner2NextLabel);
		}

		marriagePanel.setBorder(family != null? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public void refresh(final Integer actionCommand){
		if(actionCommand != Flef.ACTION_COMMAND_FAMILY_COUNT)
			return;

		linkFamilyItem.setEnabled(family == null && store.hasFamilies());
	}

	public void updatePreviousNextPartnerIcons(final GedcomNode family, final GedcomNode otherPartner, final JLabel partnerPreviousLabel,
			final JLabel partnerNextLabel){
		//get list of marriages for the `other partner`
		final List<GedcomNode> otherMarriages = store.traverseAsList(otherPartner, "FAMILY_PARTNER[]");
		//find current marriage in list
		int currentFamilyIndex = -1;
		final int otherMarriagesCount = otherMarriages.size();
		for(int i = 0; i < otherMarriagesCount; i ++)
			if(otherMarriages.get(i).getXRef().equals(family.getID())){
				currentFamilyIndex = i;
				break;
			}
		final boolean hasMoreFamilies = (otherMarriagesCount > 1);

		final boolean partnerPreviousEnabled = (currentFamilyIndex > 0);
		partnerPreviousLabel.putClientProperty(KEY_ENABLED, partnerPreviousEnabled);
		partnerPreviousLabel.setCursor(Cursor.getPredefinedCursor(partnerPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreFamilies)
			icon = (partnerPreviousEnabled? PARTNER_PREVIOUS_ENABLED: PARTNER_PREVIOUS_DISABLED);
		partnerPreviousLabel.setIcon(icon);

		final boolean partnerNextEnabled = (currentFamilyIndex < otherMarriagesCount - 1);
		partnerNextLabel.putClientProperty(KEY_ENABLED, partnerNextEnabled);
		partnerNextLabel.setCursor(Cursor.getPredefinedCursor(partnerNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreFamilies)
			icon = (partnerNextEnabled? PARTNER_NEXT_ENABLED: PARTNER_NEXT_DISABLED);
		partnerNextLabel.setIcon(icon);
	}

	public static String extractEarliestMarriageYear(final GedcomNode family, final Flef store){
		int marriageYear = 0;
		String marriageDate = null;
		final List<GedcomNode> marriageEvents = extractTaggedEvents(family, "MARRIAGE", store);
		for(final GedcomNode node : marriageEvents){
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
		final List<GedcomNode> marriageEvents = extractTaggedEvents(family, "MARRIAGE", store);
		for(final GedcomNode node : marriageEvents){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(marriagePlace == null || my < marriageYear){
					GedcomNode place = store.traverse(node, "PLACE");
					if(!place.isEmpty()){
						place = store.getPlace(place.getXRef());
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
		}
		return marriagePlace;
	}

	private static List<GedcomNode> extractTaggedEvents(final GedcomNode node, final String eventType, final Flef store){
		final List<GedcomNode> events = store.traverseAsList(node, "EVENT[]");
		final List<GedcomNode> birthEvents = new ArrayList<>(events.size());
		for(GedcomNode event : events){
			event = store.getEvent(event.getXRef());
			if(eventType.equals(store.traverse(event, "TYPE").getValue()))
				birthEvents.add(event);
		}
		return birthEvents;
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
		return (placeValue != null || addressEarliest == null? placeValue: addressEarliest.getValue());
	}

	private static GedcomNode extractEarliestAddress(final GedcomNode place, final Flef store){
		final List<GedcomNode> addresses = store.traverseAsList(place, "ADDRESS[]");
		return (addresses.isEmpty()? null: addresses.get(0));
	}


	public GedcomNode getChildReference(){
		return childReference;
	}

	public Point getFamilyPaintingPartner1EnterPoint(){
		final Point p = partner1Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingPartner2EnterPoint(){
		final Point p = partner2Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingExitPoint(){
		//halfway between partner1 and partner2 boxes
		final int x = (partner1Panel.getX() + partner1Panel.getWidth() + partner2Panel.getX()) / 2;
		//the bottom point of the marriage panel (that is: bottom point of partner1 box minus the height of the horizontal connection line
		//plus half the size of the marriage panel box)
		final int y = partner1Panel.getY() + partner1Panel.getHeight() - FAMILY_EXITING_HEIGHT;
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
			public void onFamilyPreviousPartner(final FamilyPanel familyPanel, final GedcomNode thisPartner,
					final GedcomNode otherCurrentPartner, final GedcomNode currentFamily){
				System.out.println("onPrevPartnerFamily this: " + thisPartner.getID() + ", other: " + otherCurrentPartner.getID()
					+ ", family: " + currentFamily.getID());
			}

			@Override
			public void onFamilyNextPartner(final FamilyPanel familyPanel, final GedcomNode thisPartner, final GedcomNode otherCurrentPartner,
					final GedcomNode currentFamily){
				System.out.println("onNextPartnerFamily this: " + thisPartner.getID() + ", other: " + otherCurrentPartner.getID()
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
