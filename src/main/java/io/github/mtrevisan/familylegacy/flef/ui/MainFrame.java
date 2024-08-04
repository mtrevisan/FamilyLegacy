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
package io.github.mtrevisan.familylegacy.flef.ui;

import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PersonDialog;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.SelectedNodeType;
import io.github.mtrevisan.familylegacy.flef.ui.panels.TreePanel;
import io.github.mtrevisan.familylegacy.flef.ui.tree.GenealogicalTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;


public final class MainFrame extends JFrame implements GroupListenerInterface, PersonListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_GROUP = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_NOTE = "note";
	private static final String TABLE_NAME_MEDIA_JUNCTION = "media_junction";
	private static final String TABLE_NAME_EVENT = "event";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_RESTRICTION = "restriction";


	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private final TreePanel treePanel;


	private MainFrame(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Map<String, Object> homeGroup){
		this.store = store;

		treePanel = TreePanel.create(4, store);
		treePanel.initComponents();
		treePanel.loadDataFromUnion(homeGroup);
		treePanel.setUnionListener(this);
		treePanel.setPersonListener(this);

		final JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(treePanel, BorderLayout.NORTH);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent e){
				System.exit(0);
			}
		});
		frame.setSize(1200, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		EventBusService.subscribe(this);
	}


	private List<Map<String, Object>> extractChildren(final Integer unionID){
		final TreeMap<Integer, Map<String, Object>> persons = getRecords(TABLE_NAME_PERSON);
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals("child", extractRecordRole(entry)))
			.map(entry -> persons.get(extractRecordReferenceID(entry)))
			.toList();
	}

	private TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	private static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
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


	@Override
	public void onGroupEdit(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onEditGroup {}", extractRecordID(group));

		final GroupDialog groupDialog = GroupDialog.createRecordOnly(store, this);
		groupDialog.initComponents();
		groupDialog.loadData(group);

		groupDialog.setLocationRelativeTo(treePanel);
		groupDialog.setVisible(true);
	}

	@Override
	public void onGroupAdd(final GroupPanel groupPanel){
		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		LOGGER.debug("onAddGroup (partner 1: {}, partner 2: {})", extractRecordID(partner1.getPerson()),
			extractRecordID(partner2.getPerson()));

		final GroupDialog dialog = GroupDialog.createRecordOnly(store, this)
			.withOnCloseGracefully(record -> {
				if(record != null){
					PersonPanel[] children = new PersonPanel[0];
					final int index = treePanel.genealogicalTree.getIndexOf(groupPanel);
					if(index == 0)
						children = treePanel.genealogicalTree.getChildren();
					else if(index > 0){
						final int childIndex = GenealogicalTree.getParent(index);
						final boolean isPartner1 = (index == GenealogicalTree.getLeftChild(childIndex));
						final GroupPanel treeGroupPanel = treePanel.genealogicalTree.get(childIndex);
						children = new PersonPanel[]{isPartner1? treeGroupPanel.getPartner1(): treeGroupPanel.getPartner2()};
					}

					final Integer groupID = extractRecordID(record);
					final TreeMap<Integer, Map<String, Object>> groupJunctions = getRecords(TABLE_NAME_GROUP_JUNCTION);
					final Map<String, Object> partner1Person = partner1.getPerson();
					if(!partner1Person.isEmpty()){
						final Map<String, Object> groupJunction = new HashMap<>();
						groupJunction.put("id", extractNextRecordID(groupJunctions));
						groupJunction.put("group_id", groupID);
						groupJunction.put("reference_table", TABLE_NAME_PERSON);
						groupJunction.put("reference_id", extractRecordID(partner1Person));
						groupJunction.put("role", "partner");
						groupJunctions.put(extractRecordID(groupJunction), groupJunction);
					}
					final Map<String, Object> partner2Person = partner2.getPerson();
					if(!partner2Person.isEmpty()){
						final Map<String, Object> groupJunction = new HashMap<>();
						groupJunction.put("id", extractNextRecordID(groupJunctions));
						groupJunction.put("group_id", groupID);
						groupJunction.put("reference_table", TABLE_NAME_PERSON);
						groupJunction.put("reference_id", extractRecordID(partner2Person));
						groupJunction.put("role", "partner");
						groupJunctions.put(extractRecordID(groupJunction), groupJunction);
					}
					for(final PersonPanel child : children){
						final Map<String, Object> groupJunction = new HashMap<>();
						groupJunction.put("id", extractNextRecordID(groupJunctions));
						groupJunction.put("group_id", groupID);
						groupJunction.put("reference_table", TABLE_NAME_PERSON);
						groupJunction.put("reference_id", extractRecordID(child.getPerson()));
						groupJunction.put("role", "child");
						groupJunctions.put(extractRecordID(groupJunction), groupJunction);
					}

					treePanel.refresh();
				}
			});
		dialog.initComponents();
		dialog.showNewRecord();

		dialog.setLocationRelativeTo(treePanel);
		dialog.setVisible(true);
	}

	protected static int extractNextRecordID(final NavigableMap<Integer, Map<String, Object>> records){
		return (records.isEmpty()? 1: records.lastKey() + 1);
	}

	@Override
	public void onGroupLink(final GroupPanel groupPanel){
		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onLinkPersonToSiblingGroup (partner 1: " + extractRecordID(partner1.getPerson())
			+ ", partner 2: " + extractRecordID(partner2.getPerson()) + "group: " + extractRecordID(group));

		final PersonDialog dialog = PersonDialog.createSelectOnly(store, this);
		dialog.initComponents();
		dialog.loadData();

		dialog.setLocationRelativeTo(treePanel);
		dialog.setVisible(true);

		//TODO save
	}

	@Override
	public void onGroupRemove(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getGroup();
		LOGGER.debug("onRemoveGroup {}", extractRecordID(group));

		final Integer groupID = extractRecordID(group);
		//remove group
		getRecords(TABLE_NAME_GROUP)
			.remove(groupID);
		//remove group associates
		getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values()
			.removeIf(entry -> Objects.equals(groupID, extractRecordGroupID(entry)));
		getRecords(TABLE_NAME_ASSERTION)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_NOTE)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_MEDIA_JUNCTION)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_EVENT)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_CULTURAL_NORM_JUNCTION)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_RESTRICTION)
			.values()
			.removeIf(entry -> TABLE_NAME_GROUP.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(groupID, extractRecordReferenceID(entry)));

		treePanel.refresh();
	}

	@Override
	public void onPersonChangeParents(final GroupPanel groupPanel, final PersonPanel person, final Map<String, Object> newParents){
		final Map<String, Object> currentParents = groupPanel.getGroup();
		LOGGER.debug("onGroupChangeParents person: " + extractRecordID(person.getPerson())
			+ ", current parents: " + extractRecordID(currentParents) + ", new: " + extractRecordID(newParents));

		//TODO
//		setPersonListener etc...
	}

	@Override
	public void onPersonChangeUnion(final GroupPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
			final Map<String, Object> newUnion){
		final Map<String, Object> oldUnion = groupPanel.getGroup();
		LOGGER.debug("onPersonChangeUnion old partner: " + extractRecordID(oldPartner.getPerson()) + ", old union: " + extractRecordID(oldUnion)
			+ ", new partner: " + extractRecordID(newPartner) + ", new union: " + extractRecordID(newUnion));

		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		if(extractRecordID(partner1.getPerson()).equals(extractRecordID(oldPartner.getPerson())))
			treePanel.loadData(newUnion, newPartner, partner2.getPerson());
		else
			treePanel.loadData(newUnion, partner1.getPerson(), newPartner);
	}


	@Override
	public void onPersonFocus(final PersonPanel personPanel, final SelectedNodeType type){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onFocusPerson {}, type is {}", extractRecordID(person), type);

		treePanel.loadDataFromPerson(person);
	}

	@Override
	public void onPersonEdit(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onEditPerson {}", extractRecordID(person));

		final PersonDialog personDialog = PersonDialog.createRecordOnly(store, this);
		personDialog.initComponents();
		personDialog.loadData(person);

		personDialog.setLocationRelativeTo(treePanel);
		personDialog.setVisible(true);

		//TODO save
	}

	@Override
	public void onPersonLink(final PersonPanel personPanel, final SelectedNodeType type){
		LOGGER.debug("onLinkPerson, type is " + type);

		final PersonDialog dialog = PersonDialog.createSelectOnly(store, this);
		dialog.initComponents();
		dialog.loadData();

		dialog.setLocationRelativeTo(treePanel);
		dialog.setVisible(true);

		//TODO save
	}

	@Override
	public void onPersonAdd(final PersonPanel personPanel, final SelectedNodeType type){
		LOGGER.debug("onAddPerson, type is {}", type);

		final PersonDialog dialog = PersonDialog.createRecordOnly(store, this)
			.withOnCloseGracefully(record -> {
				if(record != null){
					final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
					final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(index);
					final Integer unionID = extractRecordID(treeUnionPanel.getGroup());
					final List<Integer> partnerIDs = getPersonIDsInGroup(unionID);

					final TreeMap<Integer, Map<String, Object>> groupJunctions = getRecords(TABLE_NAME_GROUP_JUNCTION);
					Map<String, Object> groupJunction = new HashMap<>();
					groupJunction.put("id", extractNextRecordID(groupJunctions));
					groupJunction.put("group_id", unionID);
					groupJunction.put("reference_table", TABLE_NAME_PERSON);
					groupJunction.put("reference_id", extractRecordID(record));
					groupJunction.put("role", "partner");
					groupJunctions.put(extractRecordID(groupJunction), groupJunction);
					for(final Integer partnerID : partnerIDs){
						groupJunction = new HashMap<>();
						groupJunction.put("id", extractNextRecordID(groupJunctions));
						groupJunction.put("group_id", unionID);
						groupJunction.put("reference_table", TABLE_NAME_PERSON);
						groupJunction.put("reference_id", partnerID);
						groupJunction.put("role", "partner");
						groupJunctions.put(extractRecordID(groupJunction), groupJunction);
					}

					treePanel.refresh();
				}
			});
		dialog.initComponents();
		dialog.showNewRecord();

		dialog.setLocationRelativeTo(treePanel);
		dialog.setVisible(true);
	}

	private List<Integer> getPersonIDsInGroup(final Integer groupID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals("partner", extractRecordRole(entry)))
			.filter(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.map(MainFrame::extractRecordReferenceID)
			.toList();
	}

	private List<Integer> getBiologicalAndAdoptingParentsIDs(final Integer childID){
		return getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(entry -> Objects.equals(TABLE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(childID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals("child", extractRecordRole(entry)) || Objects.equals("adoptee", extractRecordRole(entry)))
			.map(MainFrame::extractRecordGroupID)
			.toList();
	}

	@Override
	public void onPersonRemove(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onRemovePerson {}", extractRecordID(person));

		final Integer personID = extractRecordID(person);
		//remove person
		getRecords(TABLE_NAME_GROUP)
			.remove(personID);
		//remove person associates
		getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_ASSERTION)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_NOTE)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_MEDIA_JUNCTION)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_EVENT)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));
		getRecords(TABLE_NAME_RESTRICTION)
			.values()
			.removeIf(entry -> TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));

		treePanel.refresh();
	}

	@Override
	public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onUnlinkPersonFromParentGroup {}", extractRecordID(person));

		//TODO
	}

	@Override
	public void onPersonAddToSiblingGroup(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onAddToSiblingGroupPerson {}", extractRecordID(person));

		//TODO
	}

	@Override
	public void onPersonUnlinkFromSiblingGroup(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onUnlinkPersonFromSiblingGroup {}", extractRecordID(person));

		final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
		final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(index);
		final Map<String, Object> union = treeUnionPanel.getGroup();
		final Integer unionID = extractRecordID(union);

		final Integer personID = extractRecordID(person);
		//remove person from union
		getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values()
			.removeIf(entry -> unionID.equals(extractRecordGroupID(entry))
				&& "partner".equals(extractRecordRole(entry))
				&& TABLE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)));

		//TODO verify
		treePanel.refresh();
	}

	@Override
	public void onPersonAddImage(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onAddPreferredImage {}", extractRecordID(person));

		//TODO
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
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("id", 3);
		group3.put("type", "family");
		groups.put((Integer)group3.get("id"), group3);

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
		groupJunction6.put("group_id", 3);
		groupJunction6.put("reference_table", "person");
		groupJunction6.put("reference_id", 4);
		groupJunction6.put("role", "partner");
		groupJunctions.put((Integer)groupJunction6.get("id"), groupJunction6);

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type", "adoption");
		event1.put("reference_table", "person");
		event1.put("reference_id", 5);
		events.put((Integer)event1.get("id"), event1);


		//create and display the form
		EventQueue.invokeLater(() -> new MainFrame(store, group1));
	}

}
