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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.MediaDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PersonDialog;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonPanel;
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

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordGroupID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordRole;


public final class MainFrame extends JFrame implements GroupListenerInterface, PersonListenerInterface{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);


	private final TreePanel treePanel;


	private MainFrame(final Map<String, Object> homeGroup){
		treePanel = TreePanel.create(4, this);
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
				System.out.println(Repository.logDatabase());

				System.exit(0);
			}
		});
		frame.setSize(1200, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		EventBusService.subscribe(this);
	}


	private List<Map<String, Object>> extractChildren(final Integer unionID){
		final Map<Integer, Map<String, Object>> persons = Repository.findAllNavigable(EntityManager.NODE_NAME_PERSON);
		return Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(unionID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_CHILD, extractRecordRole(entry)))
			.map(entry -> persons.get(extractRecordReferenceID(entry)))
			.filter(Objects::nonNull)
			.toList();
	}


	@Override
	public void onGroupEdit(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getUnion();
		LOGGER.debug("onEditGroup {}", extractRecordID(group));

		final GroupDialog groupDialog = GroupDialog.createEditOnly(this);
		groupDialog.loadData(extractRecordID(group));

		groupDialog.showDialog();
	}

	@Override
	public void onGroupAdd(final GroupPanel groupPanel){
		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		LOGGER.debug("onAddGroup (partner 1: {}, partner 2: {})", extractRecordID(partner1.getPerson()),
			extractRecordID(partner2.getPerson()));

		final GroupDialog dialog = GroupDialog.createEditOnly(this)
			.withOnCloseGracefully((record, recordID) -> {
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
					final Map<String, Object> partner1Person = partner1.getPerson();
					if(!partner1Person.isEmpty()){
						final Map<String, Object> groupJunction = new HashMap<>();
						insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_PARTNER);
						Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, groupID,
							EntityManager.NODE_NAME_PERSON, extractRecordID(partner1Person),
							EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					}
					final Map<String, Object> partner2Person = partner2.getPerson();
					if(!partner2Person.isEmpty()){
						final Map<String, Object> groupJunction = new HashMap<>();
						insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_PARTNER);
						Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, groupID,
							EntityManager.NODE_NAME_PERSON, extractRecordID(partner2Person),
							EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					}
					for(final PersonPanel child : children){
						final Map<String, Object> groupJunction = new HashMap<>();
						insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_CHILD);
						Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, groupID,
							EntityManager.NODE_NAME_PERSON, extractRecordID(child.getPerson()),
							EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					}

					treePanel.refresh();
				}
			});
		dialog.showNewRecord();

		dialog.showDialog();
	}

	private static int extractNextRecordID(final NavigableMap<Integer, Map<String, Object>> records){
		return (records.isEmpty()? 1: records.lastKey() + 1);
	}

	@Override
	public void onGroupLink(final GroupPanel groupPanel){
		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		final Map<String, Object> group = groupPanel.getUnion();
		LOGGER.debug("onLinkPersonToSiblingGroup (partner 1: {}, partner 2: {}, group: {}", extractRecordID(partner1.getPerson()),
			extractRecordID(partner2.getPerson()), extractRecordID(group));

		final PersonDialog dialog = PersonDialog.createSelectOnly(this);
		dialog.loadData();

		dialog.showDialog();

		//TODO save
	}

	@Override
	public void onGroupRemove(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getUnion();
		LOGGER.debug("onRemoveGroup {}", extractRecordID(group));

		//remove group
		Repository.deleteNode(EntityManager.NODE_NAME_GROUP, extractRecordID(group));

		treePanel.refresh();
	}

	@Override
	public void onPersonChangeParents(final GroupPanel groupPanel, final PersonPanel personPanel, final Map<String, Object> newParents){
		final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
		final boolean isPartner1 = Objects.equals(extractRecordID(groupPanel.getPartner1().getPerson()),
			extractRecordID(personPanel.getPerson()));
		final int parentIndex = (isPartner1? GenealogicalTree.getLeftChild(index): GenealogicalTree.getRightChild(index));
		final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(parentIndex);
		final Map<String, Object> currentParents = treeUnionPanel.getUnion();
		LOGGER.debug("onGroupChangeParents person: {}, current parents: {}, new parents: {}", extractRecordID(personPanel.getPerson()),
			extractRecordID(currentParents), extractRecordID(newParents));

		//TODO
	}

	@Override
	public void onPersonChangeUnion(final GroupPanel groupPanel, final PersonPanel oldPartner, final Map<String, Object> newPartner,
			final Map<String, Object> newUnion){
		final Map<String, Object> oldUnion = groupPanel.getUnion();
		LOGGER.debug("onPersonChangeUnion old partner: {}, old union: {}, new partner: {}, new union: {}",
			extractRecordID(oldPartner.getPerson()), extractRecordID(oldUnion), extractRecordID(newPartner), extractRecordID(newUnion));

		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		if(extractRecordID(partner1.getPerson()).equals(extractRecordID(oldPartner.getPerson())))
			treePanel.loadData(newUnion, newPartner, partner2.getPerson());
		else
			treePanel.loadData(newUnion, partner1.getPerson(), newPartner);
	}


	@Override
	public void onPersonFocus(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onFocusPerson {}", extractRecordID(person));

		treePanel.loadDataFromPerson(person);
	}

	@Override
	public void onPersonEdit(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onEditPerson {}", extractRecordID(person));

		final PersonDialog personDialog = PersonDialog.createEditOnly(this);
		personDialog.loadData(extractRecordID(person));

		personDialog.showDialog();
	}

	@Override
	public void onPersonLink(final PersonPanel personPanel){
		LOGGER.debug("onLinkPerson");

		final PersonDialog dialog = PersonDialog.createSelectOnly(this);
		dialog.loadData();

		dialog.showDialog();

		//TODO save
	}

	@Override
	public void onPersonAdd(final PersonPanel personPanel){
		LOGGER.debug("onAddPerson");

		final PersonDialog dialog = PersonDialog.createEditOnly(this)
			.withOnCloseGracefully((record, recordID) -> {
				if(record != null){
					final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
					if(index == GenealogicalTree.LAST_GENERATION_CHILD){
						//add as child
						final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(0);
						final Map<String, Object> currentParents = treeUnionPanel.getUnion();
						final Integer unionID = extractRecordID(currentParents);

						final Map<String, Object> groupJunction = new HashMap<>();
						insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_CHILD);
						Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, unionID,
							EntityManager.NODE_NAME_PERSON, extractRecordID(record),
							EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					}
					else{
						final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(index);
						final Integer unionID = extractRecordID(treeUnionPanel.getUnion());
						if(unionID == null){
							LOGGER.warn("Missing group, cannot create a person (FIXME hide popup menu entry)");

							return;
						}
						final List<Integer> partnerIDs = getPersonIDsInGroup(unionID);

						Map<String, Object> groupJunction = new HashMap<>();
						insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_PARTNER);
						Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, unionID,
							EntityManager.NODE_NAME_PERSON, extractRecordID(record),
							EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						for(final Integer partnerID : partnerIDs){
							groupJunction = new HashMap<>();
							insertRecordRole(groupJunction, EntityManager.GROUP_ROLE_PARTNER);
							Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, unionID,
								EntityManager.NODE_NAME_PERSON, partnerID,
								EntityManager.RELATIONSHIP_NAME_OF, groupJunction, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
					}

					treePanel.refresh();
				}
			});
		dialog.showNewRecord();

		dialog.showDialog();
	}

	private List<Integer> getPersonIDsInGroup(final Integer groupID){
		return Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordReferenceID)
			.filter(Objects::nonNull)
			.toList();
	}

	private List<Integer> getBiologicalAndAdoptingParentsIDs(final Integer childID){
		return Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(childID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_CHILD, extractRecordRole(entry))
				|| Objects.equals(EntityManager.GROUP_ROLE_ADOPTEE, extractRecordRole(entry)))
			.map(EntityManager::extractRecordGroupID)
			.filter(Objects::nonNull)
			.toList();
	}

	@Override
	public void onPersonRemove(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onRemovePerson {}", extractRecordID(person));

		Repository.deleteNode(EntityManager.NODE_NAME_PERSON, extractRecordID(person));

		treePanel.refresh();
	}

	@Override
	public void onPersonUnlinkFromParentGroup(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onUnlinkPersonFromParentGroup {}", extractRecordID(person));

		final GroupPanel groupPanel;
		final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
		if(index == GenealogicalTree.LAST_GENERATION_CHILD)
			groupPanel = treePanel.genealogicalTree.get(0);
		else{
			final GroupPanel unionPanel = treePanel.genealogicalTree.get(index);
			final boolean isPartner1 = (unionPanel.getPartner1() == personPanel);
			//extract union between `parent1Index` and `parent2Index`
			groupPanel = treePanel.genealogicalTree.get(isPartner1
				? GenealogicalTree.getLeftChild(index)
				: GenealogicalTree.getRightChild(index));
		}
		final Map<String, Object> union = groupPanel.getUnion();
		final Integer unionID = extractRecordID(union);

		final Integer personID = extractRecordID(person);
		//remove person from union
		final List<Integer> ids = Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> unionID.equals(extractRecordGroupID(entry))
				&& EntityManager.GROUP_ROLE_CHILD.equals(extractRecordRole(entry))
				&& EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)))
			.map(EntityManager::extractRecordID)
			.toList();
		Repository.deleteNodes(EntityManager.NODE_NAME_CITATION, ids);

		treePanel.refresh();
	}

	private List<Integer> getGroupIDs(final Integer personID){
		return Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry)))
			.filter(entry -> Objects.equals(personID, extractRecordReferenceID(entry)))
			.filter(entry -> Objects.equals(EntityManager.GROUP_ROLE_PARTNER, extractRecordRole(entry)))
			.map(EntityManager::extractRecordGroupID)
			.filter(Objects::nonNull)
			.toList();
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
		final Map<String, Object> union = treeUnionPanel.getUnion();
		final Integer unionID = extractRecordID(union);

		final Integer personID = extractRecordID(person);
		//remove person from union
		final List<Integer> ids = Repository.findAll(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.stream()
			.filter(entry -> unionID.equals(extractRecordGroupID(entry))
				&& EntityManager.GROUP_ROLE_PARTNER.equals(extractRecordRole(entry))
				&& EntityManager.NODE_NAME_PERSON.equals(extractRecordReferenceTable(entry))
				&& Objects.equals(personID, extractRecordReferenceID(entry)))
			.map(EntityManager::extractRecordID)
			.toList();
		Repository.deleteNodes(EntityManager.NODE_NAME_CITATION, ids);

		treePanel.refresh();
	}

	@Override
	public void onPersonAddPreferredImage(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onAddPreferredImage {}", extractRecordID(person));

		final Integer personID = extractRecordID(person);
		final MediaDialog photoDialog = MediaDialog.createForPhoto(this)
			//FIXME add path of flef file as base path
			.withBasePath(FileHelper.documentsDirectory())
			.withReference(EntityManager.NODE_NAME_PERSON, personID)
			.withOnCloseGracefully((record, recordID) -> {
				if(record != null){
					final Integer newPhotoID = extractRecordID(record);
					insertRecordPhotoID(person, newPhotoID);

					treePanel.refresh();
				}
			});
		photoDialog.loadData();
		photoDialog.showNewRecord();

		photoDialog.showDialog();
	}

	@Override
	public void onPersonEditPreferredImage(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onEditPreferredImage {}", extractRecordID(person));

		final MediaDialog photoDialog = MediaDialog.createEditOnly(this)
			//FIXME add path of flef file as base path
			.withBasePath(FileHelper.documentsDirectory())
			.withOnCloseGracefully((record, recordID) -> {
				if(record != null){
					final Integer newPhotoID = extractRecordID(record);
					insertRecordPhotoID(person, newPhotoID);

					treePanel.refresh();
				}
			});
		final Integer photoID = extractRecordPhotoID(person);
		photoDialog.loadData(photoID);

		photoDialog.showDialog();
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		Repository.save(EntityManager.NODE_NAME_PERSON, person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		Repository.save(EntityManager.NODE_NAME_PERSON, person2);
		final Map<String, Object> person3 = new HashMap<>();
		person3.put("id", 3);
		Repository.save(EntityManager.NODE_NAME_PERSON, person3);
		final Map<String, Object> person4 = new HashMap<>();
		person4.put("id", 4);
		Repository.save(EntityManager.NODE_NAME_PERSON, person4);
		final Map<String, Object> person5 = new HashMap<>();
		person5.put("id", 5);
		Repository.save(EntityManager.NODE_NAME_PERSON, person5);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		Repository.save(EntityManager.NODE_NAME_GROUP, group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "family");
		Repository.save(EntityManager.NODE_NAME_GROUP, group2);
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("id", 3);
		group3.put("type", "family");
		Repository.save(EntityManager.NODE_NAME_GROUP, group3);

		final Map<String, Object> groupJunction11 = new HashMap<>();
		groupJunction11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction11, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 2,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction13 = new HashMap<>();
		groupJunction13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group2), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction13, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group2), EntityManager.NODE_NAME_PERSON, 3,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction4 = new HashMap<>();
		groupJunction4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 4,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction4, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction5 = new HashMap<>();
		groupJunction5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group1), EntityManager.NODE_NAME_PERSON, 5,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction5, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction6 = new HashMap<>();
		groupJunction6.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group3), EntityManager.NODE_NAME_PERSON, 4,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction6, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction7 = new HashMap<>();
		groupJunction7.put("role", "adoptee");
		Repository.upsertRelationship(EntityManager.NODE_NAME_GROUP, extractRecordID(group3), EntityManager.NODE_NAME_PERSON, 5,
			EntityManager.RELATIONSHIP_NAME_OF, groupJunction7, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("id", 1);
		event1.put("type_id", 1);
		event1.put("reference_table", "person");
		event1.put("reference_id", 5);
		Repository.save(EntityManager.NODE_NAME_EVENT, event1);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("type", "adoption");
		eventType1.put("category", "adoption");
		Repository.save(EntityManager.NODE_NAME_EVENT_TYPE, eventType1);


		//create and display the form
		EventQueue.invokeLater(() -> new MainFrame(group1));
	}

}
