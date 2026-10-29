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
package io.github.mtrevisan.familylegacy.ui.dialogs.registry;

import io.github.mtrevisan.familylegacy.ui.dialogs.HeaderDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.PlaceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.RepositoryCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.CulturalNormRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.EventParticipationRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.EventRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.GroupAttributeRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.GroupRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.HistoricEventRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IdentityHypothesisRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualAttributeRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.MediaRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RepositoryRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchActivityRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchQuestionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchTaskRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.SourceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.AgentStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.ContactNameStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.ContactStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.NameStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.NoteStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.PartStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.PersonalNameStructureDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.structures.TextValueVariantStructureDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.AgentHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContactNameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HeaderHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogProvider;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.TextValueVariantHandler;


/**
 * Tells the handlers which editor edits which type of record. It is the only place that knows both the types and the editors, so that
 * the handlers do not depend on the editors.
 */
public final class RecordDialogCatalog implements RecordDialogProvider{

	@Override
	public void register(final RecordDialogs.Registry registry){
		registry.register(AgentHandler.TYPE, AgentStructureDialog::createNew, AgentStructureDialog::createEdit);
		registry.register(ConclusionHandler.TYPE, ConclusionRecordDialog::createNew, ConclusionRecordDialog::createEdit);
		registry.register(ContactHandler.TYPE, ContactStructureDialog::createNew, ContactStructureDialog::createEdit);
		registry.register(ContactNameHandler.TYPE, ContactNameStructureDialog::createNew, ContactNameStructureDialog::createEdit);
		registry.register(ContextImpactHandler.TYPE, ContextImpactRecordDialog::createNew, ContextImpactRecordDialog::createEdit);
		registry.register(CulturalNormHandler.TYPE, CulturalNormRecordDialog::createNew, CulturalNormRecordDialog::createEdit);
		registry.register(EventHandler.TYPE, EventRecordDialog::createNew, EventRecordDialog::createEdit);
		registry.register(EventParticipationHandler.TYPE, EventParticipationRecordDialog::createNew, EventParticipationRecordDialog::createEdit);
		registry.register(GroupAttributeHandler.TYPE, GroupAttributeRecordDialog::createNew, GroupAttributeRecordDialog::createEdit);
		registry.register(GroupHandler.TYPE, GroupRecordDialog::createNew, GroupRecordDialog::createEdit);
		registry.register(HeaderHandler.TYPE, (parent, model) -> new HeaderDialog(parent, model, null), (parent, model, record) -> new HeaderDialog(parent, model, null));
		registry.register(HistoricEventHandler.TYPE, HistoricEventRecordDialog::createNew, HistoricEventRecordDialog::createEdit);
		registry.register(IdentityHypothesisHandler.TYPE, IdentityHypothesisRecordDialog::createNew, IdentityHypothesisRecordDialog::createEdit);
		registry.register(IndividualAttributeHandler.TYPE, IndividualAttributeRecordDialog::createNew, IndividualAttributeRecordDialog::createEdit);
		registry.register(IndividualHandler.TYPE, IndividualRecordDialog::createNew, IndividualRecordDialog::createEdit);
		registry.register(MediaHandler.TYPE, MediaRecordDialog::createNew, MediaRecordDialog::createEdit);
		registry.register(NameHandler.TYPE, NameStructureDialog::createNew, NameStructureDialog::createEdit);
		registry.register(NoteHandler.TYPE, NoteStructureDialog::createNew, NoteStructureDialog::createEdit);
		registry.register(PartHandler.TYPE, PartStructureDialog::createNew, PartStructureDialog::createEdit);
		registry.register(PersonalNameHandler.TYPE, PersonalNameStructureDialog::createNew, PersonalNameStructureDialog::createEdit);
		registry.register(PlaceCitationHandler.TYPE, PlaceCitationDialog::createNew, PlaceCitationDialog::createEdit);
		registry.register(PlaceHandler.TYPE, PlaceRecordDialog::createNew, PlaceRecordDialog::createEdit);
		registry.register(PlaceRelationshipHandler.TYPE, PlaceRelationshipRecordDialog::createNew, PlaceRelationshipRecordDialog::createEdit);
		registry.register(RelationshipHandler.TYPE, RelationshipRecordDialog::createNew, RelationshipRecordDialog::createEdit);
		registry.register(RepositoryCitationHandler.TYPE, RepositoryCitationDialog::createNew, RepositoryCitationDialog::createEdit);
		registry.register(RepositoryHandler.TYPE, RepositoryRecordDialog::createNew, RepositoryRecordDialog::createEdit);
		registry.register(ResearchActivityHandler.TYPE, ResearchActivityRecordDialog::createNew, ResearchActivityRecordDialog::createEdit);
		registry.register(ResearchQuestionHandler.TYPE, ResearchQuestionRecordDialog::createNew, ResearchQuestionRecordDialog::createEdit);
		registry.register(ResearchTaskHandler.TYPE, ResearchTaskRecordDialog::createNew, ResearchTaskRecordDialog::createEdit);
		registry.register(SourceCitationHandler.TYPE, SourceCitationDialog::createNew, SourceCitationDialog::createEdit);
		registry.register(SourceHandler.TYPE, SourceRecordDialog::createNew, SourceRecordDialog::createEdit);
		registry.register(TextValueVariantHandler.TYPE, TextValueVariantStructureDialog::createNew, TextValueVariantStructureDialog::createEdit);
	}

}
