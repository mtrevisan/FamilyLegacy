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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.PathBound;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;

import javax.swing.JPanel;
import java.util.EnumMap;
import java.util.Map;


/**
 * Centralized container for all common UI panels used in record dialogs.
 * Handles loading and saving of data for all panels.
 */
public final class RecordDialogComponents{

	private final BindingManager bindingManager = new BindingManager();
	private final Map<PanelKey, JPanel> panels = new EnumMap<>(PanelKey.class);
	private final BaseRecordDialog owner;

	RecordDialogComponents(final RecordDialogBuilder builder){
		this.owner = builder.owner;

		final FLEFModel model = builder.model;
		final FLEFRecord record = builder.record;

		// Initialize each panel using builder configuration or key self-creation
		for(final Map.Entry<PanelKey, RecordDialogBuilder.EntityReferenceConfig> entry : builder.configs.entrySet()){
			final PanelKey key = entry.getKey();
			final RecordDialogBuilder.EntityReferenceConfig cfg = entry.getValue();

			// Delegates panel instantiation to the factory mapped to the key or builder custom factory
			final JPanel panel = cfg.factory() != null
				? cfg.factory().createPanel(owner, cfg, model, record)
				: key.createPanel(owner, cfg, model, record);

			panels.put(key, panel);
		}
	}

	public JPanel getPanel(final PanelKey key){
		return panels.get(key);
	}

	/**
	 * Registers a bound component.
	 *
	 * @param component the component to register.
	 */
	public void bind(final PathBound component){
		bindingManager.bind(component);
	}

	/**
	 * Loads data from the given record into all panels.
	 */
	public void load(final FLEFRecord record){
		if(!record.hasChildren())
			return;

		bindingManager.load(record);

		loadReferenceIfPresent(PanelKey.INDIVIDUAL_ATTRIBUTE, elp -> elp.loadReference(record.getId()));
		loadReferenceIfPresent(PanelKey.GROUP_ATTRIBUTE, elp -> elp.loadReference(record.getId()));

		loadReferenceIfPresent(PanelKey.RELATIONSHIP_ON_SUBJECT, elp -> elp.loadReferenceWithType(record.getId(), "SUBJECT"));
		loadReferenceIfPresent(PanelKey.RELATIONSHIP_ON_TARGET, elp -> elp.loadReferenceWithType(record.getId(), "TARGET"));

		loadReferenceIfPresent(PanelKey.PLACE_RELATIONSHIP_ON_SUBJECT, elp -> elp.loadReferenceWithType(record.getId(), "SUBJECT"));
		loadReferenceIfPresent(PanelKey.PLACE_RELATIONSHIP_ON_TARGET, elp -> elp.loadReferenceWithType(record.getId(), "TARGET"));

		loadReferenceIfPresent(PanelKey.EVENT_PARTICIPATION_ON_PARTICIPANT, elp -> elp.loadReferenceWithType(record.getId(), "PARTICIPANT"));
		loadReferenceIfPresent(PanelKey.EVENT_PARTICIPATION_ON_EVENT, elp -> elp.withParentEntity(record).loadCitationsWithType(record.getId(), "EVENT"));

		loadReferenceIfPresent(PanelKey.CONTEXT_IMPACT_ON_TARGET, elp -> elp.loadReferenceWithType(record.getId(), "TARGET"));
		loadReferenceIfPresent(PanelKey.CONTEXT_IMPACT_ON_CONTEXT, elp -> elp.loadReferenceWithType(record.getId(), "CONTEXT"));

		loadReferenceIfPresent(PanelKey.CONCLUSION_ON_RESOLVES, elp -> elp.loadReferenceWithType(record.getId(), "RESOLVES"));
		loadReferenceIfPresent(PanelKey.CONCLUSION_ON_RESEARCH, elp -> elp.withParentEntity(record).loadCitationsWithType(record.getId(), "RESEARCH"));
		loadReferenceIfPresent(PanelKey.IDENTITY_HYPOTHESIS_ON_IDENTITY, elp -> elp.loadReferenceWithType(record.getId(), "IDENTITY"));
		loadReferenceIfPresent(PanelKey.RESEARCH_QUESTION_ON_TARGET, elp -> elp.loadReferenceWithType(record.getId(), "TARGET"));
		loadReferenceIfPresent(PanelKey.RESEARCH_ACTIVITY_ON_QUESTION, elp -> elp.withParentEntity(record).loadCitationsWithType3(record.getId(), "QUESTION"));
		loadReferenceIfPresent(PanelKey.RESEARCH_ACTIVITY_ON_SOURCE, elp -> elp.withParentEntity(record).loadCitationsWithType2(record.getId(), "SOURCE"));

		loadReferenceIfPresent(PanelKey.PLACE, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.REPOSITORY, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.SOURCE, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.SOURCE_ON_REPOSITORY, elp -> elp.withParentEntity(record).loadCitationsWithType2(record.getId(), "REPOSITORY"));
		loadReferenceIfPresent(PanelKey.SOURCE_ON_DOCUMENT, elp -> elp.withParentEntity(record).loadCitationsWithType3(record.getId(), "DOCUMENT"));

		loadReferenceIfPresent(PanelKey.DOCUMENT, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.RESEARCH_QUESTION, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.RESEARCH_TASK_ON_QUESTION, elp -> elp.withParentEntity(record).loadCitationsWithType(record.getId(), "QUESTION"));
		loadReferenceIfPresent(PanelKey.NOTE, elp -> elp.load(record));
		loadReferenceIfPresent(PanelKey.TASK, elp -> elp.load(record));

		final EvidenceQualifiersPanel evidence = ((EvidenceQualifiersPanel)getPanel(PanelKey.EVIDENCE));
		if(evidence != null)
			evidence.load(record);

		final PrivacyPanel privacy = ((PrivacyPanel)getPanel(PanelKey.PRIVACY));
		if(privacy != null)
			privacy.load(record);

		final AuditPanel audit = ((AuditPanel)getPanel(PanelKey.AUDIT));
		if(audit != null)
			audit.load(record);
	}

	private void loadReferenceIfPresent(final PanelKey key, final java.util.function.Consumer<EntityListPanel> consumer){
		final JPanel panel = getPanel(key);
		if(panel instanceof EntityListPanel elp)
			consumer.accept(elp);
	}

	/**
	 * Saves data from all panels into the given record.
	 */
	public void save(final FLEFRecord record){
		bindingManager.save(record);

		for(final JPanel panel : panels.values()){
			if(panel instanceof EntityListPanel elp)
				elp.save(record);
			else if(panel instanceof EvidenceQualifiersPanel eqp)
				eqp.save(record);
			else if(panel instanceof PrivacyPanel pp)
				pp.save(record);
			else if(panel instanceof AuditPanel ap)
				ap.save(record);
		}
	}

}
