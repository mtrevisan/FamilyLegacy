/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;


//http://www.miglayout.com/whitepaper.html
//http://www.miglayout.com/QuickStart.pdf
//https://www.oracle.com/technetwork/systems/ts-4928-159120.pdf
//https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
//https://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
public class GroupPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 6664809287767332824L;

	private static final Color BORDER_COLOR = Color.BLACK;

	private static final double PARTNER_PREV_NEXT_WIDTH = 12.;
	private static final double PARTNER_PREV_NEXT_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension PARTNER_PREVIOUS_NEXT_SIZE = new Dimension((int)PARTNER_PREV_NEXT_WIDTH,
		(int)(PARTNER_PREV_NEXT_WIDTH * PARTNER_PREV_NEXT_ASPECT_RATIO));

	//https://snappygoat.com/free-public-domain-images-app_application_arrow_back_0/
	private static final ImageIcon ICON_PARTNER_PREVIOUS_ENABLED = ResourceHelper.getImage("/images/previous.png",
		PARTNER_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARTNER_PREVIOUS_DISABLED = new ImageIcon(
		GrayFilter.createDisabledImage(ICON_PARTNER_PREVIOUS_ENABLED.getImage()));
	private static final ImageIcon ICON_PARTNER_NEXT_ENABLED = ResourceHelper.getImage("/images/next.png", PARTNER_PREVIOUS_NEXT_SIZE);
	private static final ImageIcon ICON_PARTNER_NEXT_DISABLED = new ImageIcon(GrayFilter.createDisabledImage(ICON_PARTNER_NEXT_ENABLED.getImage()));

	/** Height of the union line from the bottom of the person panel [px]. */
	private static final int GROUP_CONNECTION_HEIGHT = 15;
	private static final Dimension UNION_PANEL_DIMENSION = new Dimension(13, 12);
	static final int GROUP_EXITING_HEIGHT = GROUP_CONNECTION_HEIGHT - UNION_PANEL_DIMENSION.height / 2;
	private static final int HALF_PARTNER_SEPARATION = 6;
	static final int GROUP_SEPARATION = HALF_PARTNER_SEPARATION + UNION_PANEL_DIMENSION.width + HALF_PARTNER_SEPARATION;
	/** Distance between navigation arrow and box. */
	static final int NAVIGATION_ARROW_SEPARATION = 3;

	static final int NAVIGATION_ARROW_HEIGHT = ICON_PARTNER_PREVIOUS_ENABLED.getIconHeight() + NAVIGATION_ARROW_SEPARATION;

	private static final String KEY_ENABLED = "enabled";

	static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);


	private PersonPanel partner1Panel;
	private PersonPanel partner2Panel;
	private final JLabel partner1PreviousLabel = new JLabel();
	private final JLabel partner1NextLabel = new JLabel();
	private final JLabel partner2PreviousLabel = new JLabel();
	private final JLabel partner2NextLabel = new JLabel();
	private final JPanel unionPanel = new JPanel();
	private final JMenuItem editGroupItem = new JMenuItem("Edit Group…", 'E');
	private final JMenuItem linkGroupItem = new JMenuItem("Link Group…", 'L');
	private final JMenuItem unlinkGroupItem = new JMenuItem("Unlink Group…", 'U');
	private final JMenuItem removeGroupItem = new JMenuItem("Remove Group…", 'R');

	private Map<String, Object> group;
	private Map<String, Object> partner1;
	private Map<String, Object> partner2;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private final BoxPanelType boxType;


	private static GroupPanel create(final Map<String, Object> group, final Map<String, Object> partner1, final Map<String, Object> partner2,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		return new GroupPanel(group, partner1, partner2, store, boxType);
	}


	private GroupPanel(final Map<String, Object> group, final Map<String, Object> partner1, final Map<String, Object> partner2,
			final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		this.store = store;

		this.group = group;
		this.partner1 = partner1;
		this.partner2 = partner2;
		this.boxType = boxType;
	}


	private void initComponents(){
		partner1Panel = PersonPanel.create(SelectedNodeType.PARTNER1, partner1, store, boxType);
		EventBusService.subscribe(partner1Panel);
		partner2Panel = PersonPanel.create(SelectedNodeType.PARTNER2, partner2, store, boxType);
		EventBusService.subscribe(partner2Panel);
		unionPanel.setBackground(Color.WHITE);
		unionPanel.setMaximumSize(UNION_PANEL_DIMENSION);
		unionPanel.setMinimumSize(UNION_PANEL_DIMENSION);
		unionPanel.setPreferredSize(UNION_PANEL_DIMENSION);

		final Dimension minimumSize = new Dimension(ICON_PARTNER_PREVIOUS_ENABLED.getIconWidth(),
			ICON_PARTNER_PREVIOUS_ENABLED.getIconHeight());
		if(boxType == BoxPanelType.PRIMARY){
			partner1PreviousLabel.setMinimumSize(minimumSize);
			partner1NextLabel.setMinimumSize(minimumSize);
			partner2PreviousLabel.setMinimumSize(minimumSize);
			partner2NextLabel.setMinimumSize(minimumSize);
		}

		final Dimension partnerPanelMaximumSize = partner1Panel.getMaximumSize();
		setMaximumSize(new Dimension(
			partnerPanelMaximumSize.width * 2 + UNION_PANEL_DIMENSION.width,
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
		add(unionPanel, "aligny bottom,gapbottom " + GROUP_EXITING_HEIGHT);
		add(partner2Panel, "growx 50");
	}

	final void setGroupListener(final GroupListenerInterface groupListener){
		if(groupListener != null){
			unionPanel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
						groupListener.onGroupEdit(GroupPanel.this, group);
				}
			});

			attachPopUpMenu(unionPanel, groupListener);

			refresh(ActionCommand.ACTION_COMMAND_GROUP_COUNT);

			partner1PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1PreviousLabel.getClientProperty(KEY_ENABLED))
						groupListener.onGroupPreviousPartner(GroupPanel.this, partner2, partner1, group);
				}
			});
			partner1NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner1NextLabel.getClientProperty(KEY_ENABLED))
						groupListener.onGroupNextPartner(GroupPanel.this, partner2, partner1, group);
				}
			});
			partner2PreviousLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2PreviousLabel.getClientProperty(KEY_ENABLED))
						groupListener.onGroupPreviousPartner(GroupPanel.this, partner1, partner2, group);
				}
			});
			partner2NextLabel.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent evt){
					if(SwingUtilities.isLeftMouseButton(evt) && (Boolean)partner2NextLabel.getClientProperty(KEY_ENABLED))
						groupListener.onGroupNextPartner(GroupPanel.this, partner1, partner2, group);
				}
			});
		}
	}

	final void setPersonListener(final PersonListenerInterface personListener){
		partner1Panel.setPersonListener(personListener);
		partner2Panel.setPersonListener(personListener);
	}

	private void attachPopUpMenu(final JComponent component, final GroupListenerInterface groupListener){
		final JPopupMenu popupMenu = new JPopupMenu();

		editGroupItem.addActionListener(e -> groupListener.onGroupEdit(this, group));
		popupMenu.add(editGroupItem);

		linkGroupItem.addActionListener(e -> groupListener.onGroupLink(this));
		popupMenu.add(linkGroupItem);

		unlinkGroupItem.addActionListener(e -> groupListener.onGroupUnlink(this, group));
		popupMenu.add(unlinkGroupItem);

		removeGroupItem.addActionListener(e -> groupListener.onGroupRemove(this, group));
		popupMenu.add(removeGroupItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	@Override
	protected final void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(g instanceof Graphics2D && partner1Panel != null && partner2Panel != null){
			final Graphics2D graphics2D = (Graphics2D)g.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			graphics2D.setStroke(CONNECTION_STROKE);

			final int xFrom = partner1Panel.getX() + partner1Panel.getWidth();
			final int xTo = partner2Panel.getX();
			final int yFrom = partner1Panel.getY() + partner1Panel.getHeight() - GROUP_CONNECTION_HEIGHT;
			//horizontal line between partners
			graphics2D.drawLine(xFrom, yFrom, xTo, yFrom);

			graphics2D.dispose();
		}
	}

	public final void loadData(final Map<String, Object> group){
		loadData(group, Collections.emptyMap(), Collections.emptyMap());
	}

	public final void loadData(final Map<String, Object> group, final Map<String, Object> partner1, final Map<String, Object> partner2){
		this.partner1 = (partner1.isEmpty() && !group.isEmpty()? store.getPartner1(group): partner1);
		this.partner2 = (partner2.isEmpty() && !group.isEmpty()? store.getPartner2(group): partner2);
		this.group = group;

		loadData();

		//TODO
//		repaint();
	}

	private void loadData(){
		partner1Panel.loadData(partner1);
		partner2Panel.loadData(partner2);

		if(boxType == BoxPanelType.PRIMARY){
			updatePreviousNextPartnerIcons(group, partner2, partner1PreviousLabel, partner1NextLabel);
			updatePreviousNextPartnerIcons(group, partner1, partner2PreviousLabel, partner2NextLabel);
		}

		unionPanel.setBorder(!group.isEmpty()? BorderFactory.createLineBorder(BORDER_COLOR):
			BorderFactory.createDashedBorder(BORDER_COLOR));

		refresh(ActionCommand.ACTION_COMMAND_GROUP_COUNT);
	}

	/** Should be called whenever a modification on the store causes modifications on the UI. */
	@EventHandler
	@SuppressWarnings("NumberEquality")
	public final void refresh(final Integer actionCommand){
		if(actionCommand != ActionCommand.ACTION_COMMAND_GROUP_COUNT)
			return;

		editGroupItem.setEnabled(!group.isEmpty());
		linkGroupItem.setEnabled(group.isEmpty() && store.hasFamilies());
		unlinkGroupItem.setEnabled(!group.isEmpty());
		removeGroupItem.setEnabled(!group.isEmpty());
	}

	public List<Map<String, Object>> extractChildren(final Map<String, Object> group){
		return store.traverseAsList(group, "CHILD[]");
	}

	private void updatePreviousNextPartnerIcons(final Map<String, Object> group, final Map<String, Object> otherPartner,
			final JLabel partnerPreviousLabel, final JLabel partnerNextLabel){
		//get list of unions for the `other partner`
		final List<Map<String, Object>> otherUnions = store.traverseAsList(otherPartner, "FAMILY_PARTNER[]");
		//find current union in list
		int currentGroupIndex = -1;
		final int otherUnionsCount = otherUnions.size();
		for(int i = 0; i < otherUnionsCount; i ++)
			if(otherUnions.get(i).getXRef().equals(group.getID())){
				currentGroupIndex = i;
				break;
			}
		final boolean hasMoreFamilies = (otherUnionsCount > 1);

		final boolean partnerPreviousEnabled = (currentGroupIndex > 0);
		partnerPreviousLabel.putClientProperty(KEY_ENABLED, partnerPreviousEnabled);
		partnerPreviousLabel.setCursor(Cursor.getPredefinedCursor(partnerPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreFamilies)
			icon = (partnerPreviousEnabled? ICON_PARTNER_PREVIOUS_ENABLED: ICON_PARTNER_PREVIOUS_DISABLED);
		partnerPreviousLabel.setIcon(icon);

		final boolean partnerNextEnabled = (currentGroupIndex < otherUnionsCount - 1);
		partnerNextLabel.putClientProperty(KEY_ENABLED, partnerNextEnabled);
		partnerNextLabel.setCursor(Cursor.getPredefinedCursor(partnerNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreFamilies)
			icon = (partnerNextEnabled? ICON_PARTNER_NEXT_ENABLED: ICON_PARTNER_NEXT_DISABLED);
		partnerNextLabel.setIcon(icon);
	}

	public String extractEarliestUnionYear(final Map<String, Object> group){
		int unionYear = 0;
		String unionDate = null;
		final List<Map<String, Object>> unionEvents = extractTaggedEvents(group, "MARRIAGE");
		for(final Map<String, Object> node : unionEvents){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(unionDate == null || my < unionYear){
					unionYear = my;
					unionDate = DateParser.extractYear(dateValue);
				}
			}
		}
		return unionDate;
	}

	public String extractEarliestUnionPlace(final Map<String, Object> group){
		int unionYear = 0;
		String unionPlace = null;
		final List<Map<String, Object>> unionEvents = extractTaggedEvents(group, "MARRIAGE");
		for(final Map<String, Object> node : unionEvents){
			final String dateValue = store.traverse(node, "DATE").getValue();
			final LocalDate date = DateParser.parse(dateValue);
			if(date != null){
				final int my = date.getYear();
				if(unionPlace == null || my < unionYear){
					final Map<String, Object> place = store.getPlace(store.traverse(node, "PLACE").getXRef());
					if(place != null){
						final String placeValue = extractPlace(place);
						if(placeValue != null){
							unionYear = my;
							unionPlace = placeValue;
						}
					}
				}
			}
		}
		return unionPlace;
	}

	private List<Map<String, Object>> extractTaggedEvents(final Map<String, Object> node, final String eventType){
		final List<Map<String, Object>> events = store.traverseAsList(node, "EVENT[]");
		final List<Map<String, Object>> birthEvents = new ArrayList<>(events.size());
		for(final Map<String, Object> event : events){
			event = store.getEvent(event.getXRef());
			if(eventType.equals(store.traverse(event, "TYPE").getValue()))
				birthEvents.add(event);
		}
		return birthEvents;
	}

	private String extractPlace(final Map<String, Object> place){
		final Map<String, Object> addressEarliest = extractEarliestAddress(place);

		//extract place as town, county, state, country, otherwise from value
		String placeValue = place.getValue();
		if(!addressEarliest.isEmpty()){
			final Map<String, Object> town = store.traverse(addressEarliest, "TOWN");
			final Map<String, Object> city = store.traverse(addressEarliest, "CITY");
			final Map<String, Object> county = store.traverse(addressEarliest, "COUNTY");
			final Map<String, Object> state = store.traverse(addressEarliest, "STATE");
			final Map<String, Object> country = store.traverse(addressEarliest, "COUNTRY");
			final StringJoiner sj = new StringJoiner(", ");
			JavaHelper.addValueIfNotNull(sj, town);
			JavaHelper.addValueIfNotNull(sj, city);
			JavaHelper.addValueIfNotNull(sj, county);
			JavaHelper.addValueIfNotNull(sj, state);
			JavaHelper.addValueIfNotNull(sj, country);
			if(sj.length() > 0)
				placeValue = sj.toString();
		}
		return (placeValue != null || addressEarliest.isEmpty()? placeValue: addressEarliest.getValue());
	}

	private Map<String, Object> extractEarliestAddress(final Map<String, Object> place){
		final List<Map<String, Object>> addresses = store.traverseAsList(place, "ADDRESS[]");
		return (!addresses.isEmpty()? addresses.get(0): Collections.emptyMap());
	}


	final Point getGroupPaintingPartner1EnterPoint(){
		final Point p = partner1Panel.getPersonPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	final Point getGroupPaintingPartner2EnterPoint(){
		final Point p = partner2Panel.getPersonPaintingEnterPoint();
		final Point origin = getLocation();
		return new Point(origin.x + p.x, origin.y + p.y);
	}

	final Point getGroupPaintingExitPoint(){
		//halfway between partner1 and partner2 boxes
		final int x = (partner1Panel.getX() + partner1Panel.getWidth() + partner2Panel.getX()) / 2;
		//the bottom point of the union panel (that is: bottom point of partner1 box minus the height of the horizontal connection line
		//plus half the size of the union panel box)
		final int y = partner1Panel.getY() + partner1Panel.getHeight() - GROUP_EXITING_HEIGHT;
		final Point origin = getLocation();
		return new Point(origin.x + x, origin.y + y);
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName11 = new HashMap<>();
		personName11.put("id", 1);
		personName11.put("person_id", 1);
		personName11.put("personal_name", "toni");
		personName11.put("family_name", "bruxatin");
		personName11.put("name_locale", "vec-IT");
		personName11.put("type", "birth name");
		personNames.put((Integer)personName11.get("id"), personName11);
		final Map<String, Object> personName12 = new HashMap<>();
		personName12.put("id", 2);
		personName12.put("person_id", 1);
		personName12.put("personal_name", "antonio");
		personName12.put("family_name", "bruciatino");
		personName12.put("name_locale", "it-IT");
		personName12.put("type", "death name");
		personNames.put((Integer)personName12.get("id"), personName12);
		final Map<String, Object> personName21 = new HashMap<>();
		personName21.put("id", 3);
		personName21.put("person_id", 2);
		personName21.put("personal_name", "bèpi");
		personName21.put("family_name", "marangon");
		personName21.put("name_locale", "vec-IT");
		personName21.put("type", "birth name");
		personNames.put((Integer)personName21.get("id"), personName21);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final GroupListenerInterface groupListener = new GroupListenerInterface(){
			@Override
			public void onGroupEdit(final GroupPanel boxPanel, final Map<String, Object> group){
				System.out.println("onEditGroup " + group.get("id"));
			}

			@Override
			public void onGroupLink(final GroupPanel boxPanel){
				System.out.println("onLinkGroup");
			}

			@Override
			public void onGroupUnlink(final GroupPanel boxPanel, final Map<String, Object> group){
				System.out.println("onUnlinkGroup " + group.get("id"));
			}

			@Override
			public void onGroupRemove(final GroupPanel boxPanel, final Map<String, Object> group){
				System.out.println("onRemoveGroup " + group.get("id"));
			}

			@Override
			public void onGroupPreviousPartner(final GroupPanel groupPanel, final Map<String, Object> thisPartner,
					final Map<String, Object> otherCurrentPartner, final Map<String, Object> currentGroup){
				System.out.println("onPrevPartnerGroup this: " + thisPartner.get("id") + ", other: " + otherCurrentPartner.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}

			@Override
			public void onGroupNextPartner(final GroupPanel groupPanel, final Map<String, Object> thisPartner,
					final Map<String, Object> otherCurrentPartner, final Map<String, Object> currentGroup){
				System.out.println("onNextPartnerGroup this: " + thisPartner.get("id") + ", other: " + otherCurrentPartner.get("id")
					+ ", current group: " + currentGroup.get("id"));
			}
		};
		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonEdit(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onEditPerson " + person.get("id"));
			}

			@Override
			public void onPersonFocus(final PersonPanel boxPanel, final io.github.mtrevisan.familylegacy.flef.ui.panels.SelectedNodeType type,
					final Map<String, Object> person){
				System.out.println("onFocusPerson " + person.get("id") + ", type is " + type);
			}

			@Override
			public void onPersonLink(final PersonPanel boxPanel, final io.github.mtrevisan.familylegacy.flef.ui.panels.SelectedNodeType type){
				System.out.println("onLinkPerson " + type);
			}

			@Override
			public void onPersonUnlink(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onUnlinkPerson " + person.get("id"));
			}

			@Override
			public void onPersonAdd(final PersonPanel boxPanel){
				System.out.println("onAddPerson");
			}

			@Override
			public void onPersonRemove(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onRemovePerson " + person.get("id"));
			}

			@Override
			public void onPersonAddImage(final PersonPanel boxPanel, final Map<String, Object> person){
				System.out.println("onAddPreferredImage " + person.get("id"));
			}
		};

		EventQueue.invokeLater(() -> {
			final GroupPanel panel = create(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), store, boxType);
			panel.initComponents();
			panel.loadData();
			panel.setGroupListener(groupListener);
			panel.setPersonListener(personListener);

			EventBusService.subscribe(panel);

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
