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
import java.util.ArrayList;
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

	private static final double PARENT_PREV_NEXT_WIDTH = 12.;
	private static final double PARENT_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension PARENT_PREVIOUS_NEXT_SIZE = new Dimension((int)PARENT_PREV_NEXT_WIDTH,
		(int)(PARENT_PREV_NEXT_WIDTH * PARENT_PREV_NEXT_ASPECT_RATIO));

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon PARENT_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/previous.png", PARENT_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon PARENT_PREVIOUS_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(PARENT_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon PARENT_NEXT_ENABLED = ResourceHelper.getImage("/images/next.png", PARENT_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon PARENT_NEXT_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(PARENT_NEXT_ENABLED.getImage()));

	/** Height of the marriage line from the bottom of the individual panel [px]. */
	private static final int FAMILY_CONNECTION_HEIGHT = 15;
	private static final Dimension MARRIAGE_PANEL_DIMENSION = new Dimension(13, 12);
	public static final int FAMILY_EXITING_HEIGHT = FAMILY_CONNECTION_HEIGHT - MARRIAGE_PANEL_DIMENSION.height / 2;
	public static final int HALF_PARENT_SEPARATION = 10;
	static final int FAMILY_SEPARATION = HALF_PARENT_SEPARATION + MARRIAGE_PANEL_DIMENSION.width + HALF_PARENT_SEPARATION;
	/** Distance between navigation arrow and box. */
	static final int NAVIGATION_ARROW_SEPARATION = 3;

	static final int NAVIGATION_ARROW_HEIGHT = PARENT_PREVIOUS_ENABLED.getIconHeight() + NAVIGATION_ARROW_SEPARATION;

	private static final String KEY_ENABLED = "enabled";

	public static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);


	private IndividualPanel parent1Panel;
	private IndividualPanel parent2Panel;
	private final JLabel parent1PreviousLabel = new JLabel();
	private final JLabel parent1NextLabel = new JLabel();
	private final JLabel parent2PreviousLabel = new JLabel();
	private final JLabel parent2NextLabel = new JLabel();
	private final JPanel marriagePanel = new JPanel();
	private final JMenuItem editFamilyItem = new JMenuItem("Edit Family…", 'E');
	private final JMenuItem linkFamilyItem = new JMenuItem("Link Family…", 'L');
	private final JMenuItem unlinkFamilyItem = new JMenuItem("Unlink Family…", 'U');
	private final JMenuItem removeFamilyItem = new JMenuItem("Remove Family…", 'R');

	private GedcomNode parent1;
	private GedcomNode parent2;
	private GedcomNode family;
	private final Flef store;
	private final BoxPanelType boxType;
	private final FamilyListenerInterface familyListener;
	private final IndividualListenerInterface individualListener;

	private GedcomNode childReference;


	public FamilyPanel(final GedcomNode parent1, final GedcomNode parent2, final GedcomNode family, final GedcomNode childReference,
			final Flef store, final BoxPanelType boxType, final FamilyListenerInterface familyListener,
			final IndividualListenerInterface individualListener){
		this.store = store;
		this.familyListener = familyListener;
		this.individualListener = individualListener;

		this.parent1 = (parent1 == null && family != null? store.getParent1(family): parent1);
		this.parent2 = (parent2 == null && family != null? store.getParent2(family): parent2);
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

		parent1Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL1, parent1, store, boxType, individualListener);
		parent1Panel.setChildReference(childReference);
		parent2Panel = new IndividualPanel(SelectedNodeType.INDIVIDUAL2, parent2, store, boxType, individualListener);
		parent2Panel.setChildReference(childReference);
		marriagePanel.setBackground(Color.WHITE);
		marriagePanel.setMaximumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setMinimumSize(MARRIAGE_PANEL_DIMENSION);
		marriagePanel.setPreferredSize(MARRIAGE_PANEL_DIMENSION);

		if(familyListener != null){
			attachPopUpMenu(marriagePanel, family, familyListener);

			refresh(Flef.ACTION_COMMAND_FAMILY_COUNT);

			parent1PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)parent1PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousParent(FamilyPanel.this, parent2, parent1, family);
				}
			});
			parent1NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)parent1NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextParent(FamilyPanel.this, parent2, parent1, family);
				}
			});
			parent2PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)parent2PreviousLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyPreviousParent(FamilyPanel.this, parent1, parent2, family);
				}
			});
			parent2NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)parent2NextLabel.getClientProperty(KEY_ENABLED))
						familyListener.onFamilyNextParent(FamilyPanel.this, parent1, parent2, family);
				}
			});
		}

		final Dimension minimumSize = new Dimension(PARENT_PREVIOUS_ENABLED.getIconWidth(), PARENT_PREVIOUS_ENABLED.getIconHeight());
		if(boxType == BoxPanelType.PRIMARY){
			parent1PreviousLabel.setMinimumSize(minimumSize);
			parent1NextLabel.setMinimumSize(minimumSize);
			parent2PreviousLabel.setMinimumSize(minimumSize);
			parent2NextLabel.setMinimumSize(minimumSize);
		}

		final Dimension parentPanelMaximumSize = parent1Panel.getMaximumSize();
		setMaximumSize(new Dimension(
			parentPanelMaximumSize.width * 2 + MARRIAGE_PANEL_DIMENSION.width,
			parentPanelMaximumSize.height + minimumSize.height
		));

		final int navigationArrowGap = (boxType == BoxPanelType.PRIMARY? NAVIGATION_ARROW_SEPARATION: 0);
		setLayout(new MigLayout("insets 0",
			"[grow]" + HALF_PARENT_SEPARATION + "[]" + HALF_PARENT_SEPARATION + "[grow]",
			"[]0[]"));
		add(parent1PreviousLabel, "split 2,alignx right,gapright 10,gapbottom " + navigationArrowGap);
		add(parent1NextLabel, "gapbottom " + navigationArrowGap);
		add(new JLabel());
		add(parent2PreviousLabel, "split 2,gapbottom " + navigationArrowGap);
		add(parent2NextLabel, "gapright 10,gapbottom " + navigationArrowGap + ",wrap");
		add(parent1Panel, "growx 50");
		add(marriagePanel, "aligny bottom,gapbottom " + FAMILY_EXITING_HEIGHT);
		add(parent2Panel, "growx 50");
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

		if(g instanceof Graphics2D && parent1Panel != null && parent2Panel != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = parent1Panel.getX() + parent1Panel.getWidth();
			final int xTo = parent2Panel.getX();
			final int yFrom = parent1Panel.getY() + parent1Panel.getHeight() - FAMILY_CONNECTION_HEIGHT;
			//horizontal line between parents
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);

			graphics2D.dispose();
		}
	}

	public void loadData(final GedcomNode family){
		loadData(null, null, family);
	}

	public void loadData(final GedcomNode parent1, final GedcomNode parent2, final GedcomNode family){
		this.parent1 = (parent1 == null && family != null? store.getParent1(family): parent1);
		this.parent2 = (parent2 == null && family != null? store.getParent2(family): parent2);
		this.family = family;

		loadData();

		repaint();
	}

	private void loadData(){
		parent1Panel.loadData(parent1);
		parent2Panel.loadData(parent2);

		if(boxType == BoxPanelType.PRIMARY){
			updatePreviousNextParentIcons(family, parent2, parent1PreviousLabel, parent1NextLabel);
			updatePreviousNextParentIcons(family, parent1, parent2PreviousLabel, parent2NextLabel);
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

	public void updatePreviousNextParentIcons(final GedcomNode family, final GedcomNode otherParent, final JLabel parentPreviousLabel,
			final JLabel parentNextLabel){
		//get list of marriages for the `other parent`
		final List<GedcomNode> otherMarriages = store.traverseAsList(otherParent, "FAMILY_PARENT[]");
		//find current marriage in list
		int currentFamilyIndex = -1;
		final int otherMarriagesCount = otherMarriages.size();
		for(int i = 0; i < otherMarriagesCount; i ++)
			if(otherMarriages.get(i).getXRef().equals(family.getID())){
				currentFamilyIndex = i;
				break;
			}
		final boolean hasMoreFamilies = (otherMarriagesCount > 1);

		final boolean parentPreviousEnabled = (currentFamilyIndex > 0);
		parentPreviousLabel.putClientProperty(KEY_ENABLED, parentPreviousEnabled);
		parentPreviousLabel.setCursor(Cursor.getPredefinedCursor(parentPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreFamilies)
			icon = (parentPreviousEnabled? PARENT_PREVIOUS_ENABLED: PARENT_PREVIOUS_DISABLED);
		parentPreviousLabel.setIcon(icon);

		final boolean parentNextEnabled = (currentFamilyIndex < otherMarriagesCount - 1);
		parentNextLabel.putClientProperty(KEY_ENABLED, parentNextEnabled);
		parentNextLabel.setCursor(Cursor.getPredefinedCursor(parentNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreFamilies)
			icon = (parentNextEnabled? PARENT_NEXT_ENABLED: PARENT_NEXT_DISABLED);
		parentNextLabel.setIcon(icon);
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
		final List<GedcomNode> birthEvents = new ArrayList<>();
		for(GedcomNode event : store.traverseAsList(node, "EVENT[]")){
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

	public Point getFamilyPaintingParent1EnterPoint(){
		final Point p = parent1Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingParent2EnterPoint(){
		final Point p = parent2Panel.getIndividualPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	public Point getFamilyPaintingExitPoint(){
		//halfway between parent1 and parent2 boxes
		final int x = (parent1Panel.getX() + parent1Panel.getWidth() + parent2Panel.getX()) / 2;
		//the bottom point of the marriage panel (that is: bottom point of parent1 box minus the height of the horizontal connection line
		//plus half the size of the marriage panel box)
		final int y = parent1Panel.getY() + parent1Panel.getHeight() - FAMILY_EXITING_HEIGHT;
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
			public void onFamilyPreviousParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
					final GedcomNode currentFamily){
				System.out.println("onPrevParentFamily this: " + thisParent.getID() + ", other: " + otherCurrentParent.getID()
					+ ", family: " + currentFamily.getID());
			}

			@Override
			public void onFamilyNextParent(final FamilyPanel familyPanel, final GedcomNode thisParent, final GedcomNode otherCurrentParent,
					final GedcomNode currentFamily){
				System.out.println("onNextParentFamily this: " + thisParent.getID() + ", other: " + otherCurrentParent.getID()
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
