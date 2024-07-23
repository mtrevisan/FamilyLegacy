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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;


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
	static final int NAVIGATION_ARROW_SEPARATION = 2;

	static final int NAVIGATION_ARROW_HEIGHT = ICON_PARTNER_PREVIOUS_ENABLED.getIconHeight() + NAVIGATION_ARROW_SEPARATION;

	private static final String KEY_ENABLED = "enabled";

	static final Stroke CONNECTION_STROKE = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f);
	static final Stroke CONNECTION_STROKE_ADOPTED = new BasicStroke(1.f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.f,
		new float[]{2.f}, 0.f);

	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_GROUP = "group";


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


	static GroupPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		return new GroupPanel(store, boxType);
	}


	private GroupPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final BoxPanelType boxType){
		this.store = store;
		this.boxType = boxType;
	}


	void initComponents(){
		partner1Panel = PersonPanel.create(store, boxType);
		EventBusService.subscribe(partner1Panel);
		partner2Panel = PersonPanel.create(store, boxType);
		EventBusService.subscribe(partner2Panel);

		unionPanel.setBackground(Color.WHITE);

		setLayout(new MigLayout("insets 0",
			"[grow]" + HALF_PARTNER_SEPARATION + "[center,grow]" + HALF_PARTNER_SEPARATION + "[grow]",
			"[]0[]"));
		add(partner1PreviousLabel, "split 2,alignx right,gapright 10,gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(partner1NextLabel, "gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(new JLabel());
		add(partner2PreviousLabel, "split 2,gapbottom " + NAVIGATION_ARROW_SEPARATION);
		add(partner2NextLabel, "gapright 10,gapbottom " + NAVIGATION_ARROW_SEPARATION + ",wrap");
		add(partner1Panel, "growx 50,alignx center");
		add(unionPanel, "aligny bottom,gapbottom " + GROUP_EXITING_HEIGHT);
		add(partner2Panel, "growx 50,alignx center");

		setOpaque(false);
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


	void loadData(final Map<String, Object> group){
		loadData(group, Collections.emptyMap(), Collections.emptyMap());
	}

	void loadData(final Map<String, Object> group, Map<String, Object> partner1, Map<String, Object> partner2){
		if((partner1.isEmpty() || partner2.isEmpty()) && !group.isEmpty()){
			//extract the first two persons from the group:
			final List<Integer> personIDs = getPersonIDsInGroup(group);
			final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
			final int size = personIDs.size();
			if(partner1.isEmpty())
				partner1 = persons.getOrDefault((size > 0? personIDs.get(0): null), Collections.emptyMap());
			if(partner2.isEmpty())
				partner2 = persons.getOrDefault((size > 1? personIDs.get(1): null), Collections.emptyMap());
		}

		this.group = group;
		this.partner1 = partner1;
		this.partner2 = partner2;

		loadData();
	}

	private void loadData(){
		partner1Panel.loadData(partner1, SelectedNodeType.PARTNER1);
		partner2Panel.loadData(partner2, SelectedNodeType.PARTNER2);

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

		final boolean hasGroups = !getRecords(TABLE_NAME_GROUP).isEmpty();
		final boolean isRecordEmpty = group.isEmpty();
		editGroupItem.setEnabled(!isRecordEmpty);
		linkGroupItem.setEnabled(isRecordEmpty && hasGroups);
		unlinkGroupItem.setEnabled(!isRecordEmpty);
		removeGroupItem.setEnabled(!isRecordEmpty);
	}

	private void updatePreviousNextPartnerIcons(final Map<String, Object> group, final Map<String, Object> otherPartner,
			final JLabel partnerPreviousLabel, final JLabel partnerNextLabel){
		//list the groupIDs for the unions og the `other partner`
		final Integer otherPartnerID = extractRecordID(otherPartner);
		final List<Integer> otherPartnerUnionIDs = getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(otherPartnerID, extractRecordReferenceID(entry)))
			.map(GroupPanel::extractRecordGroupID)
			.toList();

		//find current union in list
		int currentGroupIndex = -1;
		final Integer groupID = extractRecordID(group);
		final int otherPartnerUnionsCount = otherPartnerUnionIDs.size();
		for(int i = 0; i < otherPartnerUnionsCount; i ++){
			final Integer otherUnionID = otherPartnerUnionIDs.get(i);

			if(Objects.equals(groupID, otherUnionID)){
				currentGroupIndex = i;
				break;
			}
		}
		final boolean hasMoreFamilies = (otherPartnerUnionsCount > 1);

		final boolean partnerPreviousEnabled = (currentGroupIndex > 0);
		partnerPreviousLabel.putClientProperty(KEY_ENABLED, partnerPreviousEnabled);
		partnerPreviousLabel.setCursor(Cursor.getPredefinedCursor(partnerPreviousEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		ImageIcon icon = null;
		if(hasMoreFamilies)
			icon = (partnerPreviousEnabled? ICON_PARTNER_PREVIOUS_ENABLED: ICON_PARTNER_PREVIOUS_DISABLED);
		partnerPreviousLabel.setIcon(icon);

		final boolean partnerNextEnabled = (currentGroupIndex < otherPartnerUnionsCount - 1);
		partnerNextLabel.putClientProperty(KEY_ENABLED, partnerNextEnabled);
		partnerNextLabel.setCursor(Cursor.getPredefinedCursor(partnerNextEnabled? Cursor.HAND_CURSOR: Cursor.DEFAULT_CURSOR));
		if(hasMoreFamilies)
			icon = (partnerNextEnabled? ICON_PARTNER_NEXT_ENABLED: ICON_PARTNER_NEXT_DISABLED);
		partnerNextLabel.setIcon(icon);
	}


	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final TreeMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
			final Integer filterReferenceID){
		return getRecords(tableName).entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceTable, extractRecordReferenceTable(entry.getValue())))
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private List<Integer> getPersonIDsInGroup(final Map<String, Object> group){
		final Integer groupID = extractRecordID(group);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.map(GroupPanel::extractRecordReferenceID)
			.toList();
	}


	final Point getGroupPaintingPartner1EnterPoint(){
		final Point p = partner1Panel.getPersonPaintingEnterPoint();
		final Point origin = getLocation();
		//FIXME "-2"... WTF?? (see 'FIXME "+2"... WTF??')
		return new Point(origin.x + p.x, origin.y + p.y - 2);
	}

	final Point getGroupPaintingPartner2EnterPoint(){
		final Point p = partner2Panel.getPersonPaintingEnterPoint();
		final Point origin = getLocation();
		//FIXME "-2"... WTF?? (see 'FIXME "+2"... WTF??')
		return new Point(origin.x + p.x, origin.y + p.y - 2);
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
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		persons.put((Integer)person3.get("id"), person3);

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

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put("group", groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put("group_junction", groupJunctions);
		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("id", 1);
		groupJunction11.put("group_id", 1);
		groupJunction11.put("reference_table", "person");
		groupJunction11.put("reference_id", 1);
		groupJunction11.put("role", "partner");
		groupJunctions.put((Integer)groupJunction11.get("id"), groupJunction11);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunction2.put("role", "partner");
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("id", 3);
		groupJunction13.put("group_id", 2);
		groupJunction13.put("reference_table", "person");
		groupJunction13.put("reference_id", 1);
		groupJunction13.put("role", "partner");
		groupJunctions.put((Integer)groupJunction13.get("id"), groupJunction13);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 4);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "person");
		groupJunction3.put("reference_id", 3);
		groupJunction3.put("role", "partner");
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);

		final BoxPanelType boxType = BoxPanelType.PRIMARY;
//		final BoxPanelType boxType = BoxPanelType.SECONDARY;

		final GroupListenerInterface unionListener = new GroupListenerInterface(){
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
			public void onPersonFocus(final PersonPanel boxPanel, final SelectedNodeType type, final Map<String, Object> person){
				System.out.println("onFocusPerson " + person.get("id") + ", type is " + type);
			}

			@Override
			public void onPersonLink(final PersonPanel boxPanel, final SelectedNodeType type){
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
			final GroupPanel panel = create(store, boxType);
			panel.initComponents();
			panel.loadData(group1);
			panel.setGroupListener(unionListener);
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
