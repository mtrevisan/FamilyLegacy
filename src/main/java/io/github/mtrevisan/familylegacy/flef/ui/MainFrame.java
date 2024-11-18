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
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.AssertionDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.CulturalNormDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.EventDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GenealogicalDialogInterface;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.MediaDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PersonDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.PlaceDialog;
import io.github.mtrevisan.familylegacy.flef.ui.dialogs.ResearchStatusDialog;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.panels.BelongsToGroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.GroupPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.panels.PersonPanel;
import io.github.mtrevisan.familylegacy.flef.ui.panels.TreePanel;
import io.github.mtrevisan.familylegacy.flef.ui.tree.GenealogicalTree;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
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
			.withOnCloseGracefully(modifiedRecords -> {
				for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
					addGroup(groupPanel, upsertedRecord);

					treePanel.refresh();
				}
			});
		dialog.showNewRecord();

		dialog.showDialog();
	}

	@Override
	public void onGroupLink(final GroupPanel groupPanel){
		final PersonPanel partner1 = groupPanel.getPartner1();
		final PersonPanel partner2 = groupPanel.getPartner2();
		final Map<String, Object> group = groupPanel.getUnion();
		LOGGER.debug("onLinkPersonToSiblingGroup (partner 1: {}, partner 2: {}, group: {}", extractRecordID(partner1.getPerson()),
			extractRecordID(partner2.getPerson()), extractRecordID(group));

		final GroupDialog dialog = GroupDialog.createSelectOnly(this)
			.withCategory(EntityManager.NODE_PERSON)
			.withOnCloseGracefully(modifiedRecords -> {
				for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
					addGroup(groupPanel, upsertedRecord);

					treePanel.refresh();

					break;
				}
			});
		dialog.loadData();

		dialog.showDialog();
	}

	private void addGroup(final GroupPanel groupPanel, final Map<String, Object> upsertedRecord){
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

		final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_GROUP);

		final PersonPanel partner1 = groupPanel.getPartner1();
		final Map<String, Object> partner1Person = partner1.getPerson();
		if(!partner1Person.isEmpty()){
			final Map<String, Object> groupRelationship = new HashMap<>();
			insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_PARTNER);
			//TODO ask for belongs_to relationship data
			Repository.upsertRelationship(EntityManager.NODE_PERSON, extractRecordID(partner1Person),
				EntityManager.NODE_GROUP, upsertedRecordID,
				EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
				GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}
		final PersonPanel partner2 = groupPanel.getPartner2();
		final Map<String, Object> partner2Person = partner2.getPerson();
		if(!partner2Person.isEmpty()){
			final Map<String, Object> groupRelationship = new HashMap<>();
			insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_PARTNER);
			//TODO ask for belongs_to relationship data
			Repository.upsertRelationship(EntityManager.NODE_PERSON, extractRecordID(partner2Person),
				EntityManager.NODE_GROUP, upsertedRecordID,
				EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
				GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}
		for(final PersonPanel child : children){
			final Map<String, Object> groupRelationship = new HashMap<>();
			insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_CHILD);
			//TODO ask for belongs_to relationship data
			Repository.upsertRelationship(EntityManager.NODE_PERSON, extractRecordID(child.getPerson()),
				EntityManager.NODE_GROUP, upsertedRecordID,
				EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
				GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}
	}

	@Override
	public void onGroupRemove(final GroupPanel groupPanel){
		final Map<String, Object> group = groupPanel.getUnion();
		final Integer groupID = extractRecordID(group);
		LOGGER.debug("onRemoveGroup {}", groupID);

		//TODO remove children from removed group
		final int index = treePanel.genealogicalTree.getIndexOf(groupPanel);
		final int childIndex = GenealogicalTree.getParent(index);
		final boolean isPartner1 = (index == GenealogicalTree.getLeftChild(childIndex));
		final GroupPanel treeGroupPanel = treePanel.genealogicalTree.get(childIndex);
		final PersonPanel child = (isPartner1? treeGroupPanel.getPartner1(): treeGroupPanel.getPartner2());
		//TODO remove last attached relationship
		Repository.deleteRelationship(EntityManager.NODE_PERSON, extractRecordID(child.getPerson()),
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO);


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
		//TODO save
		dialog.loadData();

		dialog.showDialog();
	}

	@Override
	public void onPersonAdd(final PersonPanel personPanel){
		LOGGER.debug("onAddPerson");

		final PersonDialog dialog = PersonDialog.createEditOnly(this)
			.withOnCloseGracefully(modifiedRecords -> {
				for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
					final int index = treePanel.genealogicalTree.getIndexOf(personPanel);
					if(index == GenealogicalTree.LAST_GENERATION_CHILD){
						//add as child
						final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(0);
						final Map<String, Object> currentParents = treeUnionPanel.getUnion();
						final Integer unionID = extractRecordID(currentParents);

						final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_PERSON_NAME);
						final Map<String, Object> groupRelationship = new HashMap<>();
						insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_CHILD);
						//TODO ask for belongs_to relationship data
						Repository.upsertRelationship(EntityManager.NODE_PERSON, upsertedRecordID,
							EntityManager.NODE_GROUP, unionID,
							EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
							GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					}
					else{
						final GroupPanel treeUnionPanel = treePanel.genealogicalTree.get(index);
						final Integer unionID = extractRecordID(treeUnionPanel.getUnion());
						if(unionID == null){
							LOGGER.warn("Missing group, cannot create a person (FIXME hide popup menu entry)");

							return;
						}
						final List<Integer> partnerIDs = getPersonIDsInGroup(unionID, EntityManager.GROUP_ROLE_PARTNER);

						Map<String, Object> groupRelationship = new HashMap<>();
						insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_PARTNER);
						//TODO ask for belongs_to relationship data
						Repository.upsertRelationship(EntityManager.NODE_PERSON, extractRecordID(upsertedRecord),
							EntityManager.NODE_GROUP, unionID,
							EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
							GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						for(final Integer partnerID : partnerIDs){
							groupRelationship = new HashMap<>();
							insertRecordRole(groupRelationship, EntityManager.GROUP_ROLE_PARTNER);
							//TODO ask for belongs_to relationship data
							Repository.upsertRelationship(EntityManager.NODE_PERSON, partnerID,
								EntityManager.NODE_GROUP, unionID,
								EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship,
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
					}

					treePanel.refresh();
				}
			});
		dialog.showNewRecord();

		dialog.showDialog();
	}

	private List<Integer> getPersonIDsInGroup(final Integer groupID){
		return Repository.findReferencingNodes(EntityManager.NODE_PERSON,
				EntityManager.NODE_GROUP, groupID,
				EntityManager.RELATIONSHIP_BELONGS_TO).stream()
			.map(EntityManager::extractRecordID)
			.collect(Collectors.toList());
	}

	private List<Integer> getPersonIDsInGroup(final Integer groupID, final String role){
		return Repository.findReferencingNodes(EntityManager.NODE_PERSON,
				EntityManager.NODE_GROUP, groupID,
				EntityManager.RELATIONSHIP_BELONGS_TO, EntityManager.PROPERTY_ROLE, role).stream()
			.map(EntityManager::extractRecordID)
			.collect(Collectors.toList());
	}

	@Override
	public void onPersonRemove(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onRemovePerson {}", extractRecordID(person));

		Repository.deleteNode(EntityManager.NODE_PERSON, extractRecordID(person));

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

		final String roleType = EntityManager.GROUP_ROLE_CHILD;
		final Integer personID = extractRecordID(person);
		removePersonFromUnion(unionID, roleType, personID);

		treePanel.refresh();
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

		final String roleType = EntityManager.GROUP_ROLE_PARTNER;
		final Integer personID = extractRecordID(person);
		removePersonFromUnion(unionID, roleType, personID);

		treePanel.refresh();
	}

	private static void removePersonFromUnion(final Integer unionID, final String roleType, final Integer personID){
		Repository.deleteRelationship(EntityManager.NODE_PERSON, personID,
			EntityManager.NODE_GROUP, unionID,
			EntityManager.RELATIONSHIP_OF, EntityManager.PROPERTY_ROLE, roleType
		);
	}

	@Override
	public void onPersonAddPreferredImage(final PersonPanel personPanel){
		final Map<String, Object> person = personPanel.getPerson();
		LOGGER.debug("onAddPreferredImage {}", extractRecordID(person));

		final Integer personID = extractRecordID(person);
		final MediaDialog photoDialog = MediaDialog.createForPhoto(this)
			//FIXME add path of flef file as base path
			.withBasePath(FileHelper.documentsDirectory())
			.withReference(EntityManager.NODE_PERSON, personID)
			.withOnCloseGracefully(modifiedRecords -> {
				for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
					final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
					Repository.upsertRelationship(EntityManager.NODE_PERSON, personID,
						EntityManager.NODE_MEDIA, upsertedRecordID,
						EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
						GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

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
			.withOnCloseGracefully(modifiedRecords -> {
				for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
					final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
					Repository.upsertRelationship(EntityManager.NODE_PERSON, extractRecordID(person),
						EntityManager.NODE_MEDIA, upsertedRecordID,
						EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
						GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

					treePanel.refresh();
				}
			});
		final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_PERSON, extractRecordID(person));
		final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
		photoDialog.loadData(photoID);

		photoDialog.showDialog();
	}


	@EventHandler
	public void refresh(final EditEvent editCommand){
		final Map<String, Object> container = editCommand.getContainer();
		final GenealogicalDialogInterface dialog = editCommand.getDialog();
		final String tableName = dialog.getTableName();
		final int recordID = extractRecordID(container);
		final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_GROUP, recordID);
		final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
		switch(editCommand.getType()){
			case PHOTO -> {
				final MediaDialog photoDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_PHOTO_BUTTON)
						? MediaDialog.createSelectOnlyForPhoto(this)
						: MediaDialog.createForPhoto(this))
					.withBasePath(FileHelper.documentsDirectory())
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
							Repository.upsertRelationship(tableName, recordID,
								EntityManager.NODE_MEDIA, upsertedRecordID,
								EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(tableName, recordID,
								EntityManager.NODE_MEDIA, deletedIDs.get(i),
								EntityManager.RELATIONSHIP_DEPICTED_BY);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				photoDialog.loadData();
				boolean selected = false;
				if(photoID != null)
					selected = photoDialog.selectData(photoID);
				if(!selected)
					photoDialog.showNewRecord();

				photoDialog.showDialog();
			}

			case NOTE -> {
				final NoteDialog noteDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_NOTE_BUTTON)
						? NoteDialog.createSelectOnly(this)
						: NoteDialog.create(this))
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_NOTE);
							Repository.upsertRelationship(EntityManager.NODE_NOTE, upsertedRecordID,
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(EntityManager.NODE_NOTE, deletedIDs.get(i),
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				noteDialog.loadData();

				noteDialog.showDialog();
			}

			case CULTURAL_NORM -> {
				final CulturalNormDialog culturalNormDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_CULTURAL_NORM_BUTTON)
						? CulturalNormDialog.createSelectOnly(this)
						: CulturalNormDialog.create(this))
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_CULTURAL_NORM);
							Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, upsertedRecordID,
								tableName, recordID,
								EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(EntityManager.NODE_CULTURAL_NORM, deletedIDs.get(i),
								tableName, recordID,
								EntityManager.RELATIONSHIP_SUPPORTED_BY);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				culturalNormDialog.loadData();

				culturalNormDialog.showDialog();
			}

			case MEDIA -> {
				final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_MEDIA_BUTTON)
						? MediaDialog.createSelectOnlyForMedia(this)
						: MediaDialog.createForMedia(this))
					.withBasePath(FileHelper.documentsDirectory())
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
							Repository.upsertRelationship(EntityManager.NODE_MEDIA, upsertedRecordID,
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(EntityManager.NODE_MEDIA, deletedIDs.get(i),
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				mediaDialog.loadData();

				mediaDialog.showDialog();
			}

			case ASSERTION -> {
				final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_ASSERTION_BUTTON)
						? AssertionDialog.createSelectOnly(this)
						: AssertionDialog.create(this))
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_ASSERTION);
							Repository.upsertRelationship(tableName, recordID,
								EntityManager.NODE_ASSERTION, upsertedRecordID,
								EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(tableName, recordID,
								EntityManager.NODE_ASSERTION, deletedIDs.get(i),
								EntityManager.RELATIONSHIP_SUPPORTED_BY);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				assertionDialog.loadData();

				assertionDialog.showDialog();
			}

			case EVENT -> {
				final EventDialog eventDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_EVENT_BUTTON)
						? EventDialog.createSelectOnly(this)
						: EventDialog.create(this))
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_EVENT);
							Repository.upsertRelationship(EntityManager.NODE_EVENT, upsertedRecordID,
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(EntityManager.NODE_EVENT, deletedIDs.get(i),
								tableName, recordID,
								EntityManager.RELATIONSHIP_FOR);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				eventDialog.loadData();

				eventDialog.showDialog();
			}

			case GROUP -> {
				//FIXME wrong
				final GroupDialog groupDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_GROUP_BUTTON)
						? GroupDialog.createSelectOnly(this)
						: GroupDialog.create(this))
					.withReference(tableName, recordID)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, tableName);
							//TODO ask for belongs_to relationship data
							Repository.upsertRelationship(tableName, recordID,
								tableName, upsertedRecordID,
								EntityManager.RELATIONSHIP_BELONGS_TO, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(tableName, deletedIDs.get(i),
								tableName, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO);

						//update UI
						if(!deletedIDs.isEmpty())
							dialog.refreshButtonStates(recordID);
					});
				groupDialog.loadData();

				groupDialog.showDialog();
			}

			case PERSON_GROUP -> {
				final PersonDialog personDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_PERSON_GROUP_BUTTON)
						? PersonDialog.createCollectionViewOnly(recordID, BelongsToGroupPanel::create, this)
						: PersonDialog.createCollection(recordID, BelongsToGroupPanel::create, this))
					.withOnCloseGracefully(modifiedRecords -> {
						final Set<Integer> currentPersonIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_PERSON,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO).stream()
							.map(EntityManager::extractRecordID)
							.collect(Collectors.toSet());
						final Map<Integer, Map<String, Object>> newPersonInGroup = modifiedRecords.getCollection();
						//extract the intersection between `currentPersonIDInGroup` and `newPersonIDInGroup`
						final Set<Integer> intersection = new HashSet<>(currentPersonIDInGroup);
						intersection.retainAll(newPersonInGroup.keySet());
						//retain only difference
						currentPersonIDInGroup.removeAll(intersection);
						for(final Integer newPersonID : intersection)
							newPersonInGroup.remove(newPersonID);
						//remove `currentPersonIDInGroup`
						for(final Integer oldPersonID : currentPersonIDInGroup)
							Repository.deleteRelationship(EntityManager.NODE_PERSON, oldPersonID,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO);
						//add `newPersonIDInGroup`
						for(final Map.Entry<Integer, Map<String, Object>> newPerson : newPersonInGroup.entrySet())
							Repository.upsertRelationship(EntityManager.NODE_PERSON, newPerson.getKey(),
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO, newPerson.getValue(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					});
				personDialog.loadDataWithCollection(recordID);

				personDialog.showDialog();
			}

			case GROUP_GROUP -> {
				final GroupDialog groupDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_GROUP_GROUP_BUTTON)
						? GroupDialog.createCollectionViewOnly(recordID, BelongsToGroupPanel::create, this)
						: GroupDialog.createCollection(recordID, BelongsToGroupPanel::create, this))
					.withCategory(EntityManager.NODE_GROUP)
					.withOnCloseGracefully(modifiedRecords -> {
						final Set<Integer> currentGroupIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_GROUP,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO).stream()
							.map(EntityManager::extractRecordID)
							.collect(Collectors.toSet());
						final Map<Integer, Map<String, Object>> newGroupInGroup = modifiedRecords.getCollection();
						//extract the intersection between `currentGroupIDInGroup` and `newGroupIDInGroup`
						final Set<Integer> intersection = new HashSet<>(currentGroupIDInGroup);
						intersection.retainAll(newGroupInGroup.keySet());
						//retain only difference
						currentGroupIDInGroup.removeAll(intersection);
						for(final Integer newGroupID : intersection)
							newGroupInGroup.remove(newGroupID);
						//remove `currentGroupIDInGroup`
						for(final Integer oldGroupID : currentGroupIDInGroup)
							Repository.deleteRelationship(EntityManager.NODE_GROUP, oldGroupID,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO);
						//add `newGroupIDInGroup`
						for(final Map.Entry<Integer, Map<String, Object>> newGroup : newGroupInGroup.entrySet())
							Repository.upsertRelationship(EntityManager.NODE_GROUP, newGroup.getKey(),
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO, newGroup.getValue(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					});
				groupDialog.loadDataWithCollection(recordID);

				groupDialog.showDialog();
			}

			case PLACE_GROUP -> {
				final PlaceDialog placeDialog = (dialog.isViewOnlyComponent(GenealogicalDialogInterface.COMPONENT_ID_PLACE_GROUP_BUTTON)
						? PlaceDialog.createCollectionViewOnly(recordID, BelongsToGroupPanel::create, this)
						: PlaceDialog.createCollection(recordID, BelongsToGroupPanel::create, this))
					.withOnCloseGracefully(modifiedRecords -> {
						final Set<Integer> currentPlaceIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_PLACE,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO).stream()
							.map(EntityManager::extractRecordID)
							.collect(Collectors.toSet());
						final Map<Integer, Map<String, Object>> newPlaceInGroup = modifiedRecords.getCollection();
						//extract the intersection between `currentPlaceIDInGroup` and `newPlaceIDInGroup`
						final Set<Integer> intersection = new HashSet<>(currentPlaceIDInGroup);
						intersection.retainAll(newPlaceInGroup.keySet());
						//retain only difference
						currentPlaceIDInGroup.removeAll(intersection);
						for(final Integer newPlaceID : intersection)
							newPlaceInGroup.remove(newPlaceID);
						//remove `currentPlaceIDInGroup`
						for(final Integer oldPlaceID : currentPlaceIDInGroup)
							Repository.deleteRelationship(EntityManager.NODE_PLACE, oldPlaceID,
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO);
						//add `newPlaceIDInGroup`
						for(final Map.Entry<Integer, Map<String, Object>> newPlace : newPlaceInGroup.entrySet())
							Repository.upsertRelationship(EntityManager.NODE_PLACE, newPlace.getKey(),
								EntityManager.NODE_GROUP, recordID,
								EntityManager.RELATIONSHIP_BELONGS_TO, newPlace.getValue(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
					});
				placeDialog.loadDataWithCollection(recordID);

				placeDialog.showDialog();
			}

			case MODIFICATION_HISTORY_SHOW -> {
				final Integer noteID = (Integer)container.get("noteID");
				final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(this);
				final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
				changeNoteDialog.setTitle("Show modification note for " + title + " " + recordID);
				changeNoteDialog.loadData();
				changeNoteDialog.selectData(noteID);

				changeNoteDialog.showDialog();
			}
			case MODIFICATION_HISTORY_EDIT -> {
				final Integer noteID = (Integer)container.get("noteID");
				final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(this);
				final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
				changeNoteDialog.setTitle("Edit modification note for " + title + " " + recordID);
				changeNoteDialog.loadData();
				changeNoteDialog.selectData(noteID);

				changeNoteDialog.showDialog();
			}

			case RESEARCH_STATUS_SHOW -> {
				final Integer researchStatusID = (Integer)container.get("researchStatusID");
				final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(this);
				final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
				researchStatusDialog.setTitle("Show research status for " + title + " " + recordID);
				researchStatusDialog.loadData();
				researchStatusDialog.selectData(researchStatusID);

				researchStatusDialog.showDialog();
			}
			case RESEARCH_STATUS_EDIT -> {
				final Integer researchStatusID = (Integer)container.get("researchStatusID");
				final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(this);
				final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
				researchStatusDialog.setTitle("Edit research status for " + title + " " + recordID);
				researchStatusDialog.loadData();
				researchStatusDialog.selectData(researchStatusID);

				researchStatusDialog.showDialog();
			}
			case RESEARCH_STATUS_NEW -> {
				final int parentRecordID = extractRecordID(((ResearchStatusDialog)dialog).getSelectedRecord());
				final Integer researchStatusID = extractRecordID(container);
				final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(this)
					.withOnCloseGracefully(modifiedRecords -> {
						for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
							final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_RESEARCH_STATUS);
							Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, upsertedRecordID,
								tableName, parentRecordID,
								EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
						}
						final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
						for(int i = 0, length = deletedIDs.size(); i < length; i ++)
							Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, deletedIDs.get(i),
								tableName, parentRecordID,
								EntityManager.RELATIONSHIP_FOR);

						//refresh research status table
						((ResearchStatusDialog)dialog).reloadResearchStatusTable();
					});
				final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
				researchStatusDialog.setTitle("New research status for " + title + " " + parentRecordID);
				researchStatusDialog.loadData();
				researchStatusDialog.selectData(researchStatusID);

				researchStatusDialog.showDialog();
			}
		}
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person2ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person3ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person4ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person5ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person6ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "family");
		int group2ID = Repository.upsert(group2, EntityManager.NODE_GROUP);
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("type", "family");
		int group3ID = Repository.upsert(group3, EntityManager.NODE_GROUP);
		final Map<String, Object> group4 = new HashMap<>();
		group4.put("type", "new");
		int group4ID = Repository.upsert(group4, EntityManager.NODE_GROUP);

		final Map<String, Object> groupRelationship11 = new HashMap<>();
		groupRelationship11.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship11,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person2ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship13 = new HashMap<>();
		groupRelationship13.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship13,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship3 = new HashMap<>();
		groupRelationship3.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person3ID,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship3,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship4 = new HashMap<>();
		groupRelationship4.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person4ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship4,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship5 = new HashMap<>();
		groupRelationship5.put("role", "child");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person5ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship5,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship6 = new HashMap<>();
		groupRelationship6.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person4ID,
			EntityManager.NODE_GROUP, group3ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship6,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship7 = new HashMap<>();
		groupRelationship7.put("role", "adoptee");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person5ID,
			EntityManager.NODE_GROUP, group3ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship7,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship8 = new HashMap<>();
		groupRelationship8.put("role", "partner");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person6ID,
			EntityManager.NODE_GROUP, group4ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship8,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> event1 = new HashMap<>();
		int event1ID = Repository.upsert(event1, EntityManager.NODE_EVENT);

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "adoption");
		eventType1.put("category", "adoption");
		int eventType1ID = Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PERSON, person5ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);


		//create and display the form
		EventQueue.invokeLater(() -> new MainFrame(group1));
	}

}
