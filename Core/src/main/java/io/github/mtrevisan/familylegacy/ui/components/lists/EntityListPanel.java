/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.MenuBuilder;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionTargetHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Panel for managing a list of entity references, structures, or citations.
 * <p>
 * This panel supports four modes, controlled by {@link ListType}:
 * <ul>
 *   <li><strong>ENTITY_REFERENCE</strong>: simple reference (stores only the ID of the target entity)</li>
 *   <li><strong>STRUCTURE</strong>: stores the entire structure (embedded record) as a child</li>
 *   <li><strong>CITATION_WRAPPER</strong>: stores a citation record that wraps a target entity</li>
 *   <li><strong>ONEOF_REFERENCE</strong>: polymorphic reference (stores both tag and ID)</li>
 * </ul>
 * <p>
 * It also supports:
 * <ul>
 *   <li>Multiple handler types (for polymorphic lists)</li>
 *   <li>Parent entity linking for relationship dialogs</li>
 *   <li>Reverse lookup via {@link #loadReferenceWithType} and {@link #loadCitationsWithType}</li>
 *   <li>Special save modes: {@link ConclusionTargetHandler}</li>
 * </ul>
 */
public class EntityListPanel extends AbstractListPanel<FLEFRecord>{

	private static final String TAG_RESOLVES = "resolves";


	public enum ListType{
		/** Simple reference: stores only the ID of the referenced entity. */
		ENTITY_REFERENCE,
		/** Structure: stores the entire embedded structure. */
		STRUCTURE,
		/** Citation wrapper: stores a citation record that contains a reference to the target entity. */
		CITATION_WRAPPER,
		/** One‑of reference: stores both the type tag and the ID of the referenced entity. */
		ONEOF_REFERENCE
	}

	public enum ActorType{
		SUBJECT,
		OBJECT,

		EVENT,
		PARTICIPANT
	}


	private final String path;
	private final ListType type;
	// Used for relationship dialogs (SUBJECT/OBJECT etc.)
	private final ActorType actorType;
	private List<Class<? extends RecordTypeHandler<?>>> handlerTypes;
	private FLEFRecord subjectEntity;
	private FLEFRecord objectEntity;
	private FLEFRecord parentEntity;

	// Used to reload the list after editing
	private FLEFRecord parentRecord;
	// True if loaded via reverse lookup (loadReference*)
	private boolean isReference;

	// Flag to enable the conclusion logic only where requested
	private boolean supportsConclusions = false;
	// Cache to avoid scanning the model on every repaint
	private final Map<String, FLEFRecord> conclusionCache = new HashMap<>();


	public static EntityListPanel createForEntityReference(final String path, final Window parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ENTITY_REFERENCE, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForStructure(final String path, final Window parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.STRUCTURE, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForCitationWrapper(final String path, final Window parent,
			final String panelTitle, final FLEFModel model, final Class<? extends RecordTypeHandler<?>> handlerType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.CITATION_WRAPPER, null)
			.withHandlerTypes(handlerType);
	}

	public static EntityListPanel createForOneOfReference(final String path, final Window parent,
			final String panelTitle, final FLEFModel model){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ONEOF_REFERENCE, null);
	}

	public static EntityListPanel createForOneOfReference(final String path, final Window parent,
			final String panelTitle, final FLEFModel model, final ActorType actorType){
		return new EntityListPanel(path, parent, panelTitle, model, ListType.ONEOF_REFERENCE, actorType);
	}


	protected EntityListPanel(final String path, final Window parent, final String panelTitle, final FLEFModel model,
			final ListType type, final ActorType actorType){
		super(parent, panelTitle, model);

		this.path = path;
		this.type = type;
		this.actorType = actorType;
		this.handlerTypes = Collections.emptyList();
	}


	/**
	 * Sets the allowed handler types. Must be called before the panel is used.
	 *
	 * @param handlerTypes the record types this list can accept
	 * @return this panel (for chaining)
	 */
	@SafeVarargs
	public final EntityListPanel withHandlerTypes(final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		this.handlerTypes = List.of(handlerTypes);

		initComponents();

		return this;
	}

	/**
	 * Enables conclusion icon support for this panel.
	 * When enabled, records that have an associated ConclusionRecord
	 * will display a clickable icon to open it.
	 *
	 * @return this panel (for chaining)
	 */
	public EntityListPanel withConclusions(){
		this.supportsConclusions = true;

		return this;
	}

	/**
	 * Sets a custom cell renderer for the underlying list component.
	 *
	 * @param renderer the cell renderer to use
	 * @return this panel (for chaining)
	 */
	public JPanel withCellRenderer(final ListCellRenderer<? super FLEFRecord> renderer){
		if(renderer != null)
			list.setCellRenderer(renderer);

		return this;
	}

	/**
	 * Sets a subject entity that will be automatically linked when creating new records
	 */
	public EntityListPanel withSubjectEntity(final FLEFRecord subjectEntity){
		this.subjectEntity = subjectEntity;

		return this;
	}

	/**
	 * Sets an object entity that will be automatically linked when creating new records
	 */
	public EntityListPanel withObjectEntity(final FLEFRecord objectEntity){
		this.objectEntity = objectEntity;

		return this;
	}

	/**
	 * Sets a parent entity that will be automatically linked when creating new records
	 */
	public EntityListPanel withParentEntity(final FLEFRecord parentEntity){
		this.parentEntity = parentEntity;

		return this;
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		// If conclusions are supported, add the click listener for the icon
		if(supportsConclusions){
			list.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					final int index = list.locationToIndex(e.getPoint());
					if(index < 0)
						return;

					final Rectangle bounds = list.getCellBounds(index, index);
					// The icon is on the left. Clickable area: the first 24 pixels.
					if(e.getX() >= bounds.x && e.getX() <= bounds.x + 24){
						final FLEFRecord item = listModel.get(index);
						final FLEFRecord conclusion = conclusionCache.get(item.getId());
						if(conclusion != null)
							openConclusionDialog(conclusion);
					}
				}
			});
		}

		final Consumer<MenuBuilder> menuItems;
		if(type == ListType.CITATION_WRAPPER)
			menuItems = createMenuItemsForCitationWrapper();
		else
			// ENTITY_REFERENCE, STRUCTURE, ONEOF_REFERENCE: default menu
			menuItems = createMenuItemsForDefault();

		BindingsHelper.installBehavior(list,
			this::editItem,
			(type == ListType.CITATION_WRAPPER)? this::editTargetItem: null,
			this::createNewItem, this::removeItem,
			menuItems);
	}

	private Consumer<MenuBuilder> createMenuItemsForDefault(){
		return builder -> {
			builder.item(I18N.t("popupmenu.create_new"), this::createNewItem);
			// Only show "Add Existing" for ENTITY_REFERENCE and ONEOF_REFERENCE (not for STRUCTURE)
			if(type != ListType.STRUCTURE)
				builder.item(I18N.t("popupmenu.add_existing"), this::addItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit"), this::editItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
		};
	}

	private Consumer<MenuBuilder> createMenuItemsForCitationWrapper(){
		return builder -> {
			builder.item(I18N.t("popupmenu.create_new"), this::createNewItem);
			builder.item(I18N.t("popupmenu.add_existing"), this::addItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit.record"), this::editTargetItem);
			builder.selectionSensitiveItem(I18N.t("popupmenu.edit.citation"), this::editItem);
			builder.separator();
			builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
		};
	}


	@Override
	protected String getDisplayText(final FLEFRecord record){
		if(record == null)
			return "--";

		final RecordTypeHandler<?> handler = findHandler(record);
		return handler.getDisplayText(record, model);
	}

	@Override
	protected Icon getItemIcon(final FLEFRecord item){
		if(item == null)
			return null;

		//a padlock signals a restricted or confidential record
		final Icon privacyIcon = PrivacyBadge.of(PrivacyService.resolver(model)
			.describe(item));

		Icon conclusionIcon = null;
		if(supportsConclusions){
			final FLEFRecord conclusion = conclusionCache.computeIfAbsent(item.getId(), this::findConclusionFor);
			conclusionIcon = new ConclusionIcon(conclusion != null);
		}
		return CompoundIcon.of(privacyIcon, conclusionIcon);
	}

	private void openConclusionDialog(final FLEFRecord conclusion){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(ConclusionHandler.class);
		if(handler != null){
			final RecordDialog dialog = handler.createEditDialog(parent, model, conclusion);
			dialog.setVisible(true);
		}
	}

	private FLEFRecord findConclusionFor(final String recordId){
		if(recordId == null)
			return null;

		for(final FLEFRecord c : model.getRecordsByType(ConclusionHandler.TYPE)){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(c, TAG_RESOLVES);
			for(final FLEFRecord r : resolves)
				if(containsId(r, recordId))
					return c;
		}
		return null;
	}

	private boolean containsId(final FLEFRecord node, final String id){
		if(node.getValue() != null && node.getValue().equals(id))
			return true;

		for(final FLEFRecord child : node.getChildren())
			if(containsId(child, id))
				return true;

		return false;
	}


	@Override
	protected FLEFRecord showAddDialog(){
		final List<Class<? extends RecordTypeHandler<?>>> cleaned = extractParentHandlers();
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
				(record, handler) -> {
					// For citation wrapper, we need to create a citation around the selected entity.
					if(type == ListType.CITATION_WRAPPER)
						addExistingCitation(record);
					else
						addItemDirectly(record);
				},
				cleaned.toArray(Class[]::new))
			.withSetupDialog(getDialogSetup());
		dialog.setVisible(true);

		// Result is handled via listener
		return null;
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		if(type == ListType.CITATION_WRAPPER)
			// Create a new target entity and then a citation for it.
			return createNewCitation();
		else{
			// For ENTITY_REFERENCE, STRUCTURE, ONEOF_REFERENCE:
			// Use the first handler (or if multiple, a selection dialog).
			if(handlerTypes.size() == 1){
				final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerTypes.getFirst());
				final RecordDialog dialog = handler.createNewDialog(parent, model);
				//only the editors of a relationship have a subject and a target
				if(subjectEntity != null)
					dialog.link(LinkRole.SUBJECT, subjectEntity);
				if(objectEntity != null)
					dialog.link(LinkRole.TARGET, objectEntity);
				getDialogSetup().accept(dialog);
				dialog.setVisible(true);

				return (dialog.isSaved()? dialog.getRecord(): null);
			}
			else{
				// Multiple types: show selection dialog and then create based on selection
				final FLEFRecord[] result = {null};
				@SuppressWarnings("unchecked")
				final RecordSelectionDialog selectionDialog = RecordSelectionDialog.createWithAllowRecordCreation(parent,
						model,
						(record, handler) -> result[0] = record,
						handlerTypes.toArray(Class[]::new))
					.withSetupDialog(getDialogSetup());
				selectionDialog.setVisible(true);

				return result[0];
			}
		}
	}

	/**
	 * Creates a new target entity and its citation (for CITATION_WRAPPER mode).
	 */
	private FLEFRecord createNewCitation(){
		final RecordTypeHandler<?> citationHandler = HandlerRegistry.getHandler(handlerTypes.getFirst());
		final RecordTypeHandler<?> targetHandler = citationHandler.getParentHandler();
		final RecordDialog targetDialog = targetHandler.createNewDialog(parent, model);
		getDialogSetup().accept(targetDialog);
		targetDialog.setVisible(true);

		if(!targetDialog.isSaved())
			return null;

		final FLEFRecord newTarget = targetDialog.getRecord();
		final String targetId = newTarget.getId();

		// Now create the citation
		final FLEFRecord citation = FLEFRecord.createEmpty();
		final String citedType = citationHandler.getCitedType();
		FLEFRecordHelper.updateChildValue(citation, (citedType != null? citedType: path), targetId);

		final RecordDialog citationDialog = citationHandler.createEditDialog(parent, model, citation);
		citationDialog.setVisible(true);

		if(citationDialog.isSaved()){
			final FLEFRecord newCitation = citationDialog.getRecord();
			addItemDirectly(newCitation);

			return newCitation;
		}
		else{
			// Rollback: remove the target entity
			model.removeRecord(targetId);

			return null;
		}
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent,
				I18N.t("error.record_not_found"),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final RecordTypeHandler<?> handler = findHandler(record);
		final RecordDialog dialog = handler.createEditDialog(parent, model, record);
		getDialogSetup().accept(dialog);
		final FLEFRecord before = FLEFRecord.createEmpty();
		record.deepCopyTo(before);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			if(parentRecord != null){
				reloadFromParent();

				//make the change undoable from the dialog that hosts this list
				if(parent instanceof RecordDialog parentDialog){
					final FLEFRecord after = FLEFRecord.createEmpty();
					record.deepCopyTo(after);
					parentDialog.addNestedEdit(record, before, after, this::reloadFromParent);
				}
			}

			// Check if the record is still in the list; if not, it was removed.
			if(!listModel.contains(record))
				return null;
		}

		// Return the same record (it was updated in place)
		return record;
	}

	private void reloadFromParent(){
		// Use the stored parent record info to reload
		if(type == ListType.ENTITY_REFERENCE || type == ListType.ONEOF_REFERENCE)
			loadReferenceWithType(parentRecord.getId(), StringUtils.split(parentRecord.getTag(), '|'));
			// For STRUCTURE or CITATION_WRAPPER, we might need a different reload
			// Fallback: reload from the parent record if available
		else
			load(parentEntity);
	}

	/**
	 * Edits the target entity (for CITATION_WRAPPER mode, with Shift+click).
	 */
	private void editTargetItem(){
		final int index = list.getSelectedIndex();
		if(index < 0)
			return;

		final FLEFRecord citation = listModel.get(index);
		final RecordTypeHandler<?> citationHandler = findHandler(citation);
		final String targetId = FLEFRecordHelper.getChildValue(citation, citationHandler.getCitedType());
		final FLEFRecord target = model.getRecordById(targetId);
		final RecordTypeHandler<?> targetHandler = citationHandler.getParentHandler();
		final RecordDialog dialog = targetHandler.createEditDialog(parent, model, target);
		getDialogSetup().accept(dialog);
		dialog.setVisible(true);
	}

	/**
	 * For CITATION_WRAPPER mode: adds an existing entity by creating a citation around it.
	 */
	private void addExistingCitation(final FLEFRecord entity){
		if(entity == null)
			return;

		final String entityId = entity.getId();
		final FLEFRecord citation = FLEFRecord.createEmpty();
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerTypes.getFirst());
		final String citedType = handler.getCitedType();
		if(citedType != null)
			FLEFRecordHelper.updateChildValue(citation, citedType, entityId);
		else
			// Fallback: use the path as the tag
			FLEFRecordHelper.updateChildValue(citation, path, entityId);

		final RecordDialog citationDialog = handler.createEditDialog(parent, model, citation);
		citationDialog.setVisible(true);

		if(citationDialog.isSaved()){
			final FLEFRecord newCitation = citationDialog.getRecord();
			addItemDirectly(newCitation);
		}
	}


	/**
	 * Loads the list from a parent record (normal forward loading).
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		parentRecord = FLEFRecord.createMainRecord(record.getId(), record.getTag());

		List<FLEFRecord> items;
		if(type == ListType.CITATION_WRAPPER)
			items = FLEFRecordHelper.extractStructuresWithReference(record, path);
		else if(type == ListType.ONEOF_REFERENCE)
			items = FLEFRecordHelper.extractRecordsFromOneOfReference(record, path, model);
		else if(type == ListType.STRUCTURE)
			items = FLEFRecordHelper.extractStructures(record, path);
		else
			// ENTITY_REFERENCE
			items = FLEFRecordHelper.extractRecordsFromReference(record, path, model);
		setItems(items);

		// Set parent entity for new records
		withParentEntity(record);
	}

	/**
	 * Loads references by scanning the model for records that reference the given entity.
	 * Uses the handler's findReferences method (must be implemented by handlers).
	 */
	public void loadReference(final String recordId){
		clear();

		if(recordId == null)
			return;

		parentRecord = model.getRecordById(recordId);
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			references.addAll(handler.findReferences(model, recordId, parentEntity.getTag()));
		}
		setItems(references);
	}

	/**
	 * Loads references by scanning the model for records that reference the given entity,
	 * filtering by specific actor tags (e.g., "SUBJECT", "TARGET").
	 */
	public void loadReferenceWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = model.getRecordById(recordId);
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromOneOfReference(reference, actorTag, model);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
//							final String tag = actor.getTag();
							final String id = actor.getId();
							if(/*Strings.CI.equals(parentEntityTag, tag) &&*/ recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Loads citations by scanning the model for records that reference the given entity,
	 * using a different extraction method (extractRecordsFromReference).
	 */
	public void loadCitationsWithType(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractRecordsFromReference(reference, actorTag, model);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getId();
							if(Strings.CI.equals(parentEntity.getTag(), tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Variant of loadCitationsWithType that uses extractStructures on the reference.
	 */
	public void loadCitationsWithType2(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractStructures(reference, actorTag);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final FLEFRecord ref = FLEFRecordHelper.findChild(actor, parentEntity.getTag());
							if(ref == null)
								continue;

							final String tag = ref.getTag();
							final String id = ref.getValue();
							if(Strings.CI.equals(parentEntity.getTag(), tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Variant of loadCitationsWithType3 that uses value directly from the actor.
	 */
	public void loadCitationsWithType3(final String recordId, final String... actorTags){
		clear();

		if(recordId == null)
			return;

		parentRecord = FLEFRecord.createMainRecord(recordId, StringUtils.join(actorTags, '|'));
		isReference = true;

		final List<FLEFRecord> references = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handler : handlerTypes){
			final String type = HandlerRegistry.getHandlerType(handler);
			final List<FLEFRecord> handlerReferences = model.getRecordsByType(type).stream()
				.filter(reference -> {
					for(final String actorTag : actorTags){
						final List<FLEFRecord> actors = FLEFRecordHelper.extractStructures(reference, actorTag);
						if(actors.isEmpty())
							continue;

						for(final FLEFRecord actor : actors){
							final String tag = actor.getTag();
							final String id = actor.getValue();
							if(Strings.CI.equals(parentEntity.getTag(), tag) && recordId.equals(id))
								return true;
						}
					}
					return false;
				})
				.toList();
			references.addAll(handlerReferences);
		}
		setItems(references);
	}

	/**
	 * Saves the current list to the given record. The behavior depends on {@link #type}.
	 */
	public void save(final FLEFRecord record){
		if(isReference)
			return;

		clearRecord(record);

		if(isEmpty())
			return;

		if(type == ListType.ENTITY_REFERENCE){
			for(final FLEFRecord item : getItems())
				FLEFRecordHelper.addChildValue(record, path, item.getFormattedId());
		}
		else if(type == ListType.ONEOF_REFERENCE){
			for(final FLEFRecord item : getItems())
				record.addChild(FLEFRecord.createChildWithTag(path)
					.addChild(FLEFRecord.createChildWithTagAndValue(item.getTag(), item.getFormattedId()))
				);
		}
		else if(type == ListType.CITATION_WRAPPER){
			// Citation wrapper: items are already full citation records; add them under the path.
			addItemsToPath(record, path);
		}
		else if(type == ListType.STRUCTURE){
			if(handlerTypes.size() == 1 && ConclusionTargetHandler.class.equals(handlerTypes.getFirst())){
				final FLEFRecord parentRecord = FLEFRecordHelper.getOrCreateTargetNode(record, TAG_RESOLVES);
				addItemsToPath(parentRecord, path);
			}
			else
				addItemsToPath(record, path);
		}
	}

	/**
	 * Helper to add items under a specific path.
	 */
	private void addItemsToPath(final FLEFRecord record, final String path){
		if(StringUtils.isEmpty(path)){
			for(final FLEFRecord item : getItems())
				record.addChild(item);
		}
		else{
			final int lastDotIndex = path.lastIndexOf('.');
			final String parentPath = (lastDotIndex >= 0? path.substring(0, lastDotIndex): null);
			final String lastChildTag = (lastDotIndex >= 0? path.substring(lastDotIndex + 1): path);
			final FLEFRecord parent = FLEFRecordHelper.getOrCreateTargetNode(record, parentPath);

			for(final FLEFRecord item : getItems())
				parent.addChildWithTag(lastChildTag, item);
		}
	}

	/**
	 * Clears all tags from the record before saving.
	 *
	 * @param record the FLEF record to clean
	 */
	void clearRecord(final FLEFRecord record){
		if(type == ListType.ENTITY_REFERENCE || type == ListType.ONEOF_REFERENCE){
			if(StringUtils.isNotEmpty(path))
				FLEFRecordHelper.removeChildren(record, path);
		}
		else if(type == ListType.CITATION_WRAPPER || type == ListType.STRUCTURE){
			final boolean isConclusionTarget = (handlerTypes.size() == 1
				&& ConclusionTargetHandler.class.equals(handlerTypes.getFirst()));

			if(isConclusionTarget)
				FLEFRecordHelper.removeChildren(record, TAG_RESOLVES);
			else if(StringUtils.isNotEmpty(path))
				FLEFRecordHelper.removeChildren(record, path);
			else{
				// If path is empty, clear all tags associated with the handlers
				for(final Class<? extends RecordTypeHandler<?>> handlerClass : handlerTypes){
					final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerClass);
					if(handler != null){
						FLEFRecordHelper.removeChildren(record, handler.getType());
						final RecordTypeHandler<?> parent = handler.getParentHandler();
						if(parent != null)
							FLEFRecordHelper.removeChildren(record, parent.getType());
					}
				}
			}
		}
	}


	private Consumer<RecordDialog> getDialogSetup(){
		return dialog -> {
			if(parentRecord == null || (type != ListType.ONEOF_REFERENCE && type != ListType.ENTITY_REFERENCE))
				return;

			//the editors take the parts they have (a relationship has a subject and a target, an event participation a participant and an
			//event...), and ignore the other ones
			if(actorType == ActorType.SUBJECT)
				dialog.link(LinkRole.SUBJECT, parentRecord);
			else if(actorType == ActorType.OBJECT)
				dialog.link(LinkRole.TARGET, parentRecord);
			else if(actorType == ActorType.PARTICIPANT)
				dialog.link(LinkRole.PARTICIPANT, parentRecord);
			else if(actorType == ActorType.EVENT)
				dialog.link(LinkRole.EVENT, parentRecord);
			dialog.link(LinkRole.IDENTITY, parentRecord);
			dialog.link(LinkRole.RESOLVED_TARGET, parentRecord);
		};
	}

	private RecordTypeHandler<?> findHandler(final FLEFRecord record){
		// If only one handler, use it directly
		if(handlerTypes.size() == 1)
			return HandlerRegistry.getHandler(handlerTypes.getFirst());

		final String recordTag = record.getTag();
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			if(Strings.CI.equals(handler.getType(), recordTag))
				return handler;

			// Also check parent handler
			final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
			if(parentHandler != null && Strings.CI.equals(parentHandler.getType(), recordTag))
				return handler;
		}
		return null;
	}

	/**
	 * Extracts the parent handlers (used for "Add Existing" when the handler is a citation wrapper).
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends RecordTypeHandler<?>>> extractParentHandlers(){
		final List<Class<? extends RecordTypeHandler<?>>> cleaned = new ArrayList<>();
		for(final Class<? extends RecordTypeHandler<?>> handlerType : handlerTypes){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(handlerType);
			final RecordTypeHandler<?> parentHandler = handler.getParentHandler();
			cleaned.add((Class<? extends RecordTypeHandler<?>>)(parentHandler != null
				? parentHandler.getClass()
				: handler.getClass()));
		}
		return cleaned;
	}


	@Override
	public String toString(){
		return "EntityListPanel{"
			+ "path = '" + path + '\''
			+ ", type = " + type
			+ ", items = " + listModel.size()
			+ '}';
	}


	/**
	 * Custom Icon to draw a small badge (full for conclusion present, faded for absent).
	 */
	private static class ConclusionIcon implements Icon{
		private final boolean active;
		private final int size = 14;

		public ConclusionIcon(final boolean active){
			this.active = active;
		}

		@Override
		public void paintIcon(final Component c, final Graphics g, final int x, final int y){
			final Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if(active){
				g2.setColor(new Color(0, 120, 215)); // Blue for active
				g2.fillOval(x, y, size, size);
				g2.setColor(Color.WHITE);
			}
			else{
				g2.setColor(new Color(220, 220, 220)); // Light gray for inactive
				g2.fillOval(x, y, size, size);
				g2.setColor(new Color(150, 150, 150));
			}

			g2.setFont(new Font("SansSerif", Font.BOLD, 10));
			final FontMetrics fm = g2.getFontMetrics();
			final int stringWidth = fm.stringWidth("C");
			g2.drawString("C", x + (size - stringWidth) / 2, y + (size + fm.getAscent()) / 2 - 1);

			g2.dispose();
		}

		@Override
		public int getIconWidth(){ return size; }

		@Override
		public int getIconHeight(){ return size; }
	}

}
