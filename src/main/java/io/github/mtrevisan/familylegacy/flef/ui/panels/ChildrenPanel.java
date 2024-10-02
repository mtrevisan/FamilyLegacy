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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTypeID;


public class ChildrenPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = -1250057284416778781L;

	private static final double UNION_HEIGHT = 12.;
	private static final double UNION_ASPECT_RATIO = 3501. / 2662.;
	private static final Dimension UNION_SIZE = new Dimension((int)(UNION_HEIGHT / UNION_ASPECT_RATIO), (int)UNION_HEIGHT);

	private static final ImageIcon ICON_UNION = ResourceHelper.getImage("/images/union.png", UNION_SIZE);

	private static final int CHILD_SEPARATION = 15;
	static final int UNION_ARROW_HEIGHT = ICON_UNION.getIconHeight() + GroupPanel.NAVIGATION_UNION_ARROW_SEPARATION;


	private PersonPanel[] childBoxes;
	private boolean[] adoptions;
	private PersonListenerInterface personListener;


	static ChildrenPanel create(){
		return new ChildrenPanel();
	}


	private ChildrenPanel(){
		initComponents();
	}


	private void initComponents(){
		setOpaque(false);
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
		final Map<String, Object>[] children = extractChildren(unionID);

		//for each child, scan its events and collect all that have type "adoption"
		final Set<Integer> adoptionEventIDs = extractAdoptionEventIDs(unionID);
		adoptions = new boolean[children.length];
		for(int i = 0, length = adoptions.length; i < length; i ++)
			adoptions[i] = adoptionEventIDs.contains(extractRecordID(children[i]));

		setLayout(new MigLayout("insets 0", "[]0[]"));
		if(children.length > 0){
			childBoxes = new PersonPanel[children.length + 1];
			for(int i = 0, length = children.length; i < length; i ++){
				final Map<String, Object> child = children[i];

				final Integer childID = extractRecordID(child);
				final boolean hasChildUnion = !getGroups(childID).isEmpty();
				final JPanel box = createChildPanel(hasChildUnion);
				final PersonPanel childBox = createChildPersonPanel(childID);
				box.add(childBox);
				add(box, "gapright " + CHILD_SEPARATION);
				childBoxes[i] = childBox;
			}
		}

		//add empty child
		final JPanel box = createChildPanel(false);
		final PersonPanel childBox = createChildPersonPanel(null);
		box.add(childBox);
		add(box);
		childBoxes[children.length] = childBox;

		if(personListener != null)
			setPersonListener(personListener);
	}

	private PersonPanel createChildPersonPanel(final Integer childID){
		final PersonPanel childBox = PersonPanel.create(BoxPanelType.SECONDARY);
		childBox.loadData(childID);

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

	private Set<Integer> extractAdoptionEventIDs(final Integer unionID){
		final Map<Integer, Map<String, Object>> eventTypes = Repository.findAllNavigable(EntityManager.NODE_EVENT_TYPE);
		final Set<String> eventTypesAdoptions = getEventTypes(EntityManager.EVENT_TYPE_CATEGORY_ADOPTION);
		return Repository.findReferencingNodes(EntityManager.NODE_EVENT,
				EntityManager.NODE_GROUP, unionID,
				EntityManager.RELATIONSHIP_FOR).stream()
			.filter(entry -> {
				final Integer recordTypeID = extractRecordTypeID(entry);
				final String recordType = extractRecordType(eventTypes.get(recordTypeID));
				return eventTypesAdoptions.contains(recordType);
			})
			.map(EntityManager::extractRecordID)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	private List<Map<String, Object>> getGroups(final Integer personID){
		return Repository.findReferencingNodes(EntityManager.NODE_GROUP,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_PARTNER);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object>[] extractChildren(final Integer unionID){
		final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_PERSON);
		return Repository.findReferencingNodes(EntityManager.NODE_PERSON,
				EntityManager.NODE_GROUP, unionID,
				EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, EntityManager.GROUP_ROLE_CHILD).stream()
			.map(entry -> persons.get(extractRecordID(entry)))
			.toArray(Map[]::new);
	}

	public PersonPanel[] getChildBoxes(){
		return childBoxes;
	}

	boolean isChildAdopted(final int index){
		return (index < adoptions.length && adoptions[index]);
	}

	private Set<String> getEventTypes(final String category){
		return Repository.findAll(EntityManager.NODE_EVENT_TYPE)
			.stream()
			.filter(entry -> Objects.equals(category, extractRecordCategory(entry)))
			.map(EntityManager::extractRecordType)
			.collect(Collectors.toSet());
	}


	Point[] getPaintingEnterPoints(){
		final Component[] components = getComponents();
		final Point[] enterPoints = new Point[components.length];
		for(int i = 0, length = components.length; i < length; i ++){
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


		GraphDatabaseManager.clearDatabase();
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "family");
		Repository.upsert(group2, EntityManager.NODE_GROUP);

		final Map<String, Object> groupRelationship11 = new HashMap<>();
		groupRelationship11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 2,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship13 = new HashMap<>();
		groupRelationship13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 1,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship3 = new HashMap<>();
		groupRelationship3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 3,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship4 = new HashMap<>();
		groupRelationship4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 4,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship5 = new HashMap<>();
		groupRelationship5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 5,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship6 = new HashMap<>();
		groupRelationship6.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 6,
			EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship6, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship7 = new HashMap<>();
		groupRelationship7.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, 4,
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship7, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("type_id", 1);
event1.put("reference_table", "person");
event1.put("reference_id", 5);
		Repository.upsert(event1, EntityManager.NODE_EVENT);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "adoption");
		eventType1.put("category", "adoption");
		Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);

		final PersonListenerInterface personListener = new PersonListenerInterface(){
			@Override
			public void onPersonFocus(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onFocusPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonEdit(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPerson " + extractRecordID(person));
			}

			@Override
			public void onPersonLink(final PersonPanel personPanel){
				System.out.println("onLinkPerson");
			}

			@Override
			public void onPersonAdd(final PersonPanel personPanel){
				System.out.println("onAddPerson");
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
			public void onPersonAddPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onAddPreferredImage " + extractRecordID(person));
			}

			@Override
			public void onPersonEditPreferredImage(final PersonPanel personPanel){
				final Map<String, Object> person = personPanel.getPerson();
				System.out.println("onEditPreferredImage " + extractRecordID(person));
			}
		};


		EventQueue.invokeLater(() -> {
			final ChildrenPanel panel = create();
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
