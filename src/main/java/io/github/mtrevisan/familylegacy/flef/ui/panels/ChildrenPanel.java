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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;


public class ChildrenPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -1250057284416778781L;

	private static final double UNION_HEIGHT = 12.;
	private static final double UNION_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension UNION_SIZE = new Dimension((int)(UNION_HEIGHT / UNION_ASPECT_RATIO), (int)UNION_HEIGHT);

	private static final ImageIcon ICON_UNION = ResourceHelper.getImage("/images/union.png", UNION_SIZE);

	private static final int CHILD_SEPARATION = 15;
	static final int UNION_ARROW_HEIGHT = ICON_UNION.getIconHeight() + GroupPanel.NAVIGATION_UNION_ARROW_SEPARATION;

	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_EVENT = "event";


	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private Map<String, Object>[] children;
	private PersonPanel[] childBoxes;
	private boolean[] adoptions;
	private PersonListenerInterface personListener;


	static ChildrenPanel create(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		return new ChildrenPanel(store);
	}


	private ChildrenPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.store = store;
	}


	void initComponents(){
		setOpaque(false);
	}


	final Map<String, Object>[] getChildren(){
		return children;
	}

	public void setPersonListener(final PersonListenerInterface personListener){
		this.personListener = personListener;

		for(int i = 0, length = (childBoxes != null? childBoxes.length: 0); i < length; i ++)
			childBoxes[i].setPersonListener(personListener);
	}

//	@Override
//	protected final void paintComponent(final Graphics g){
//		super.paintComponent(g);
//
//		if(g instanceof Graphics2D){
//			final Graphics2D graphics2D = (Graphics2D)g.create();
//			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//
//
//			//for test purposes
//			final Point[] enterPoints = getPaintingEnterPoints();
//			final Point origin = getLocation();
//			//vertical line connecting the children
//			graphics2D.setColor(Color.RED);
//			for(int i = 0; i < enterPoints.length; i ++){
//				final Point point = enterPoints[i];
//
//				final boolean isAdopted = isChildAdopted(i);
//				if(isAdopted)
//					graphics2D.setStroke(GroupPanel.CONNECTION_STROKE_ADOPTED);
//
//				graphics2D.drawLine(point.x - 10, point.y - 10, point.x + 10, point.y + 10);
//				graphics2D.drawLine(point.x + 10, point.y - 10, point.x - 10, point.y + 10);
//
//				if(isAdopted)
//					graphics2D.setStroke(GroupPanel.CONNECTION_STROKE);
//			}
//			graphics2D.setColor(Color.BLACK);
//
//
//			graphics2D.dispose();
//		}
//	}


	public void loadData(final Integer unionID){
		//clear panel
		removeAll();

		//extract the children from the union
		children = extractChildren(unionID);

		//for each child, scan its events and collect all that have type "adoption"
		final Set<Integer> adoptionEventIDs = extractAdoptionEventIDs();
		adoptions = new boolean[children.length];
		for(int i = 0; i < adoptions.length; i ++)
			adoptions[i] = adoptionEventIDs.contains(extractRecordID(children[i]));

		setLayout(new MigLayout("insets 0", "[]0[]"));
		if(children.length > 0){
			childBoxes = new PersonPanel[children.length + 1];
			for(int i = 0, length = children.length; i < length; i ++){
				final Map<String, Object> child = children[i];

				final Integer childID = extractRecordID(child);
				final boolean hasChildUnion = hasUnion(childID);
				final JPanel box = createChildPanel(hasChildUnion);
				final PersonPanel childBox = createChildPersonPanel(child);
				box.add(childBox);
				add(box, "gapright " + CHILD_SEPARATION);
				childBoxes[i] = childBox;
			}
		}

		//add empty child
		final JPanel box = createChildPanel(false);
		final PersonPanel childBox = createChildPersonPanel(Collections.emptyMap());
		box.add(childBox);
		add(box);
		childBoxes[children.length] = childBox;

		if(personListener != null)
			setPersonListener(personListener);
	}

	private PersonPanel createChildPersonPanel(final Map<String, Object> child){
		final PersonPanel childBox = PersonPanel.create(BoxPanelType.SECONDARY, SelectedNodeType.CHILD, store);
		childBox.initComponents();
		childBox.loadData(child);

		EventBusService.subscribe(childBox);

		return childBox;
	}

	private static JPanel createChildPanel(final boolean hasChildUnion){
		final JPanel box = new JPanel();
		box.setOpaque(false);
		box.setLayout(new MigLayout("flowy,insets 0", "[]",
			"[]" + GroupPanel.NAVIGATION_UNION_ARROW_SEPARATION + "[]"));
		final JLabel unionLabel = new JLabel();
		unionLabel.setPreferredSize(new Dimension(ICON_UNION.getIconWidth(), ICON_UNION.getIconHeight()));
		if(hasChildUnion)
			unionLabel.setIcon(ICON_UNION);
		box.add(unionLabel, "right");
		return box;
	}

	private Set<Integer> extractAdoptionEventIDs(){
		return getRecords(TABLE_NAME_EVENT)
			.values().stream()
			.filter(entry -> Objects.equals("adoption", extractRecordType(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.map(ChildrenPanel::extractRecordReferenceID)
			.collect(Collectors.toSet());
	}

	private boolean hasUnion(final Integer childID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(childID, extractRecordReferenceID(entry)))
			.anyMatch(entry -> Objects.equals("partner", extractRecordRole(entry)));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object>[] extractChildren(final Integer unionID){
		final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals("child", extractRecordRole(entry)))
			.map(entry -> persons.get(extractRecordReferenceID(entry)))
			.toArray(Map[]::new);
	}

	public PersonPanel[] getChildBoxes(){
		return childBoxes;
	}

	boolean isChildAdopted(final int index){
		return (index < adoptions.length && adoptions[index]);
	}

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final TreeMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
			final Integer filterReferenceID){
		return getRecords(tableName)
			.entrySet().stream()
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

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}


	Point[] getPaintingEnterPoints(){
		final Component[] components = getComponents();
		final Point[] enterPoints = new Point[components.length];
		for(int i = 0; i < components.length; i ++){
			final Component component = components[i];

			enterPoints[i] = new Point(component.getX() + component.getWidth() / 2, component.getY() + UNION_ARROW_HEIGHT);
		}
		return enterPoints;
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
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		persons.put((Integer)person4.get("id"), person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		persons.put((Integer)person5.get("id"), person5);
		final Map<String, Object> person6 = new HashMap<>();
		person6.put("id", 6);
		persons.put((Integer)person6.get("id"), person6);

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
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("id", 5);
		groupJunction4.put("group_id", 1);
		groupJunction4.put("reference_table", "person");
		groupJunction4.put("reference_id", 4);
		groupJunction4.put("role", "child");
		groupJunctions.put((Integer)groupJunction4.get("id"), groupJunction4);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("id", 6);
		groupJunction5.put("group_id", 1);
		groupJunction5.put("reference_table", "person");
		groupJunction5.put("reference_id", 5);
		groupJunction5.put("role", "child");
		groupJunctions.put((Integer)groupJunction5.get("id"), groupJunction5);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("id", 7);
		groupJunction6.put("group_id", 1);
		groupJunction6.put("reference_table", "person");
		groupJunction6.put("reference_id", 6);
		groupJunction6.put("role", "child");
		groupJunctions.put((Integer)groupJunction6.get("id"), groupJunction6);
		final Map<String, Object> groupJunction7 = new HashMap<>();
		groupJunction7.put("id", 8);
		groupJunction7.put("group_id", 2);
		groupJunction7.put("reference_table", "person");
		groupJunction7.put("reference_id", 4);
		groupJunction7.put("role", "partner");
		groupJunctions.put((Integer)groupJunction7.get("id"), groupJunction7);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type", "adoption");
		event1.put("reference_table", "person");
		event1.put("reference_id", 5);
		events.put((Integer)event1.get("id"), event1);

		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonFocus(final PersonPanel personPanel, final SelectedNodeType type){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onFocusPerson " + extractRecordID(person) + ", type is " + type);
			}

			@Override
			public void onPersonEdit(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonLink(final PersonPanel personPanel, final SelectedNodeType type){
				System.out.println("onLinkPerson, type is " + type);
			}

			@Override
			public void onPersonAdd(final PersonPanel personPanel, final SelectedNodeType type){
				System.out.println("onAddPerson, type is " + type);
			}

			@Override
			public void onPersonRemove(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onRemovePerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onUnlinkPersonFromParentGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddToSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onAddToSiblingGroupPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonUnlinkFromSiblingGroup(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onUnlinkPersonFromSiblingGroup " + extractRecordID(person));
			}

			@Override
			public void onPersonAddImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onAddPreferredImage " + extractRecordID(person));
			}
		};

		EventQueue.invokeLater(() -> {
			final ChildrenPanel panel = create(store);
			panel.initComponents();
			panel.loadData(extractRecordID(group1));
			panel.setPersonListener(personListener);

			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
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
