# Viste e funzionalità dell'interfaccia: stato e backlog

Elenco di ciò che manca per sfruttare appieno il protocollo FLEF. Legenda: DONE = verificato nel codice, TODO = non
presente, PARZIALE = presente solo in parte o altrove. Per le viste specializzate vedere anche
`temporal/README.txt` (punto 1) e `social/README.txt` (punto 2).

Stato verificato sul codice (ottobre 2026).


## Viste oggi raggiungibili

`ProjectionType` espone tre proiezioni: albero biologico, grafo Sugiyama, ego network.
Altre viste raggiungibili: dossier individuo/gruppo, chronomap + Agorà, strip delle vite (`MultiLifespanStripPanel`),
timeline globale degli eventi (sotto l'ego network), gerarchia dei luoghi (`PlaceHierarchyPanel`), kinship dialog e
pedigree collapse (albero).

**Scollegate:** `TemporalProjectionPanel` (punto 1) e `SocialNetworkPanel` (punto 2) sono implementate ma non istanziate
da nessuna parte. TODO aggiungerle a `ProjectionType` / `ProjectionSwitcherPanel`.


## Punto 4 - Viste di gruppo (Group / Community View)

TODO Non iniziato. Esistono solo `GroupPanel`, `GroupData`, `GroupListener`, il dossier di gruppo (`GroupDossier*`) e gli
strumenti di gestione dei gruppi. Nessuna delle classi `Group*Timeline`, `Group*Network`, `IndividualCareer*` esiste.

FLEF fornisce già tutto il necessario: `GroupRecord`, appartenenza (`RelationshipRecord` `group_member` con `role`,
`valid_from` / `valid_to` / `status`), gerarchia (`part_of`), associazioni (`associate`), `GroupAttributeRecord`,
partecipazione a eventi, `ContextImpactRecord` sui gruppi.

Cosa manca (tutto TODO): appartenenza nel tempo, multi-appartenenza di un individuo, gerarchia `part_of` con date,
interazioni tra gruppi (`associate`), ruoli nel tempo (carriera), eventi del gruppo, attributi del gruppo nel tempo,
contesto storico del gruppo.

Architettura proposta, tre viste:
- **A. Group Timeline**: per un gruppo, una timeline con righe per membri (barre colorate per ruolo), sottogruppi,
  eventi, attributi e bande di contesto. Riusa `TemporalAxis`, `TemporalSpan`, `TemporalProjectionLayout`.
- **B. Group Network**: nodi = gruppi, archi = `part_of` (gerarchico, linea spessa) e `associate` (tratteggiato), badge con
  il numero di membri, filtro per anno. Layout gerarchico. Va sempre filtrata.
- **C. Individual Group Career**: le appartenenze di una persona nel tempo, con il ruolo di ogni periodo.

Classi previste (nessuna esiste ancora):
- enum: `GroupViewMode`, `GroupTimelineRowType`, `GroupRelationCategory`;
- dati: `GroupMemberRef`, `GroupSubgroupRef`, `GroupEventRef`, `GroupAttributeRef`, `GroupAssociationRef`;
- aggregati: `GroupProfile`, `GroupTimelineRow`, `GroupTimelineEntry`, `GroupNetworkModel`, `GroupNetworkEdge`,
  `IndividualCareerModel`;
- servizi: `GroupIndices`, `GroupProfileService`, `GroupTimelineBuilder`, `GroupNetworkService`,
  `IndividualCareerService`, `GroupFilters`;
- layout/renderer/UI: `GroupTimelineLayout`, `GroupNetworkLayout`, renderer per riga/entry/nodo/arco, `GroupExplorerPanel`,
  toolbar e interaction handler, `GroupTimelinePanel`, `GroupNetworkPanel`, `IndividualCareerPanel`.

Ordine consigliato: enum e record -> `GroupIndices` -> `GroupFilters` -> `GroupProfileService` -> timeline (Vista A, la
più riusabile) -> carriera (Vista C) -> rete (Vista B) -> pannello contenitore.

Da non fare: JTree/JTable come vista di esplorazione, duplicare l'indicizzazione di `TemporalIndices`, un motore di layout
generico di grafi, mostrare tutti i gruppi insieme.


## Punto 5 - Parentela estesa ed endogamia (Pedigree Collapse / Consanguinity)

PARZIALE.

DONE
- Albero con layout Sugiyama (`individualtree/layout/sugiyama`).
- Rilevamento del pedigree collapse (`PedigreeCollapseDetector`, `PedigreeCollapseDialog`, `PedigreeCollapseController`).
- Calcolo della parentela fra due individui: `KinshipCalculator`, `KinshipResult` (catene fino all'antenato comune più
  recente), `KinshipDialog`, `KinshipCalculatorTool`.

TODO
- Coefficiente di inbreeding di Wright `F` e coefficiente di relazione `R` (nessun `InbreedingCalculator` /
  `RelationshipCalculator`).
- Report delle unioni consanguinee (`ConsanguineousUnionDetector`, `ConsanguinityReport*`).
- Grafo DAG unificato con archi di unione, heatmap dell'endogamia, anelli di consanguineità.
- Vista di ascendenza proiettata con duplicati collegati ("stesso individuo").
- Segnalazione delle unioni consanguinee nell'ego network.

Indicazioni di progetto (ancora valide):
- F e R si calcolano con una passata topologica (antenati prima dei discendenti) e memoizzazione delle distanze di
  generazione; troncare i contributi oltre `(1/2)^8` circa.
- Per `F` e `R` contano solo i legami `biological_child`; adozione, affido, tutela e figliastri vanno esclusi di default.
- Valori di verifica: fratelli `R = 1/2`, zio-nipote `R = 1/4`, cugini primi `R = 1/8`.
- Mai mostrare l'intero DAG: filtrare per individuo focale, profondità o coefficiente. Nessuna libreria esterna di graph
  drawing.

Classi previste (non esistono): `InbreedingCalculator`, `RelationshipCalculator`, `AncestorResolver`, `KinshipIndices`,
`ConsanguineousUnionDetector`, `PedigreeGraphService`, `ConsanguinityReportService`, `PedigreeFilters`, i relativi record
(`InbreedingResult`, `RelationshipResult`, `CommonAncestor`, `PedigreeNode`, ...), `PedigreeGraphPanel`,
`AncestryFocusPanel`, `ConsanguinityReportPanel`.

Ordine consigliato: indici -> `AncestorResolver` -> `InbreedingCalculator` -> `RelationshipCalculator` (testabili senza
Swing, sui valori classici) -> detector -> report -> viste.


## Visualizzazione sintetica di eventi, attributi, fonti, luoghi, contesto (idee)

Stato rispetto alle idee originali:
- Eventi: DONE timeline globale (`GlobalEventTimelinePanel`, `TimelineViewDialog`), life map per individuo
  (`MultiLifespanStripPanel`).
- Attributi: PARZIALE bande di validità nella strip; TODO carriera (Gantt), matrice comparativa.
- Fonti: TODO source tree, provenance delle citazioni, galleria documenti con crop, citation network
  (esistono gli editor, `CitationAnalysisTool` e le gestioni di fonti/repository/media).
- Luoghi: DONE albero della gerarchia (`PlaceHierarchyPanel`, `PlaceHierarchyService`); TODO timeline delle giurisdizioni.
- Contesto: DONE vista ad albero (`ContextBoardDialog`); TODO impact network, timeline contestuale.
- Meta-informazioni: TODO badge di evidenza (`EvidenceQualifiers`), icone di privacy, tooltip di audit sui box.


## Per coprire il protocollo FLEF

1. DONE Editor per `SourceRecord`, `RepositoryRecord`, citazioni e parti di documento (`SourceRecordDialog`,
   `RepositoryRecordDialog`, `SourceCitationDialog`, `MediaPartListPanel`, `ImageCropDialog`).
2. DONE Vista "Research": `ResearchBoardDialog` (menu Ricerca > "Research Board") mostra question -> activity -> task con le
   conclusioni accanto, filtro testuale, dettaglio del record e apertura dell'editor (doppio click); l'albero è costruito da
   `ResearchTreeBuilder` (testato). I record senza domanda stanno sotto "(no question)". Non c'è ancora la creazione di un
   record già collegato alla domanda selezionata né la timeline delle attività.
3. DONE Vista "Place" (`PlaceHierarchyPanel`, `PlaceRelationshipRecordDialog`).
4. DONE Vista "Context": `ContextBoardDialog` (menu Eventi > "Context Board") mostra eventi storici e norme culturali con
   gli impatti di ciascuno (`ContextImpactRecord`, con bersaglio e tipo d'impatto), filtro testuale, dettaglio del record e
   apertura dell'editor (`HistoricEventRecordDialog`, `CulturalNormRecordDialog`, `ContextImpactRecordDialog`) con doppio
   click; "New > Impact of the selected context" apre un nuovo impatto con il contesto già compilato. L'albero è costruito da
   `ContextTreeBuilder` (testato); gli impatti senza contesto esistente stanno sotto "(no context)". Non c'è ancora una
   timeline contestuale né una vista per bersaglio (impact network).
5. DONE Editor di `IdentityHypothesis`: confronto affiancato dei due candidati (`RecordDiffDialog`) e gestione delle
   conclusioni nel tab Research: "Create New" sulla lista delle conclusioni (a ipotesi già salvata) apre una conclusione con
   l'ipotesi già in `resolves`, issue `identity` e le domande di ricerca che hanno l'ipotesi come target già collegate.
6. DONE Tipi embedded: `ApproximatePanel`, `BoundedDatePanel`, `SpanningDatePanel`, `DateStructurePanel`,
   `ContactStructureDialog`, `ImageCropDialog`.
7. DONE Privacy enforcement: `PrivacyResolver` (package `privacy`, testato) calcola il livello effettivo del
   protocollo (privacy dichiarata e non scaduta, ereditarietà da individui e gruppi referenziati, dai partecipanti per gli
   eventi, dal proprietario per l'immagine preferita; individuo che può essere vivo = restricted; `national_id` =
   confidential); `PrivacyService` tiene la policy globale (menu Visualizza > Privacy, default `SHOW_ALL`, salvata in
   `AppPreferences`) e la cache del resolver (invalidata a ogni salvataggio di un record); la policy è applicata a report,
   ricerca/selezione (`SearchService`), albero e grafo (`GenealogyRepository`), ego network, dossier di individui e gruppi; un
   individuo nascosto resta come box mascherato; indicatore a lucchetto (`PrivacyBadge`) sul box dell'individuo (pieno =
   dichiarata, vuoto = ereditata o presunta). Filtro applicato anche a timeline e strip (`EventIndex`, `TemporalIndices`,
   `TemporalProjectionService`), gerarchia dei luoghi, chronomap, social network, kinship, Research Board e Context Board; box
   del gruppo mascherato come quello dell'individuo; badge a lucchetto nelle liste di record dei dialog (eventi,
   partecipazioni, attributi, ...), nel dossier, negli alberi delle due board e dei luoghi, nel nome della strip e nei
   tooltip di strip e timeline; filtro anche negli strumenti di statistiche (`StatisticsCalculator`) e nei bookmark
   (`BookmarkStore.visible`: il menu ora si ricostruisce a ogni apertura); `ExportFlefTool` chiede la policy del file (di
   default quella globale) e `PrivacyExporter` ne produce la copia filtrata: omette i record nascosti, le strutture annidate
   con privacy nascosta, i riferimenti ai record omessi, i link `flef:confidential` e quelli verso record omessi nelle note,
   e i contatti del submitter non esplicitamente pubblici. Anello ambra/rosso sui segni degli eventi (strip, timeline) e sui
   marker del chronomap (`PrivacyBadge.paintRing`); filtro anche negli helper degli strumenti di gestione (individui,
   gruppi, eventi, fonti) e nella ricerca dei duplicati: per gestire i record nascosti si porta la policy su "Show All"
   (gli editor aperti esplicitamente mostrano sempre tutto; consistenza e menu restano non filtrati). TODO: contatore dei
   record nascosti nella barra di stato. Il filtro non è una misura di sicurezza: il file resta in chiaro.
8. DONE Validation report: `ValidateFileTool` + `ValidationReportDialog` con "Open record" per ogni violazione.
9. DONE Audit viewer: `AuditPanel` nei dialog dei record (creation e update).


## Per usabilità generale

1. PARZIALE Ricerca globale: `JumpToIndividualDialog` (Ctrl+J) cerca solo individui e gruppi; non c'è Ctrl+F né ricerca
   per luogo, fonte, id su tutti i tipi di record.
2. DONE Cronologia di navigazione (`NavigationHistory`, Ctrl+Left / Ctrl+Right, "Cancella cronologia").
3. DONE Segnalibri / viste salvate (`bookmarks`, menu Segnalibri).
4. DONE Undo/redo applicativo: oggi Ctrl+Z / Ctrl+Y agiscono solo dentro ai dialog dei record (`UndoController` in
   `BaseRecordDialog`); le voci del menu Modifica non annullano le modifiche al modello.
5. DONE Pannello "Modifiche recenti" (menu Modifica, `RecentChangesDialog`): elenco non modale dei record toccati a ogni passo di
   `ModelHistory` (aggiunto/modificato/rimosso, ora, applicato/annullato); doppio clic apre l'editor del record.
6. DONE Confronto affiancato di due record (`RecordDiffDialog`, `MergeFieldComparisonDialog`).
7. TODO? Filtri sul dossier (sezioni, tipo di evento, intervallo, presenza di fonte).
8. PARZIALE Tooltip arricchiti: presenti in `IndividualPanel`, strip, timeline, ego network (layout); da verificare su
   albero e Sugiyama.
9. DONE Scorciatoie da tastiera: F2 modifica, Canc elimina, Ctrl+X sposta, Ctrl+Z/Y, Ctrl+1/2/3 cambio proiezione, Ctrl+L,
   Ctrl+T, Ctrl+J, F11, F1, frecce per navigare nel grafo (`SpatialNavigation`). Mancano Ctrl+F e Ctrl+S (c'è solo
   "Salva con nome").
10. DONE tooltip sui campi non ovvi di Confidence, EvidenceQualifiers, ContextImpact, Conclusion (preferred assertion),
   IdentityHypothesis, CulturalNorm, ResearchActivity (result, search_scope, ...), ResearchQuestion/Task, e "quando usare
   questo record?" sulla prima scheda dei dialog e nella lista dei tipi di `RecordSelectionDialog` (chiavi `record.usage.*`).
   Meccanismo: i `BoundComboBox` con prefisso i18n mostrano `<prefisso>.tooltip` sul campo, `<prefisso>.<codice>.tooltip`
   per ogni voce (e per quella selezionata); `GUIHelper.withTooltip` per gli altri campi, le etichette riprendono il tooltip
   del campo. Solo inglese.


## Per le viste grafiche

1. PARZIALE Albero: il collasso del pedigree c'è; manca filtri/espansione di rami.
2. TODO EgoNetwork: filtro per tipo di relazione e toggle gruppi.
3. DONE Lifespan strip: bande per secolo, evidenziazione delle vite sovrapposte all'hover, click su banda/marker.
4. DONE Chronomap: layer multipli (`ChronomapLayerManager`, mappe storiche), traiettorie (great circle) e marker animato.
   Il filtro per tipo di evento non è stato verificato.
5. DONE Vista "Agorà" (`AgoraPanel`).


## Import/export

1. PARZIALE Export PDF: i report (`PdfReportRenderer`: antenati, discendenti, vita individuale, gruppi, fonti, ...); non
   c'è un export diretto del dossier.
2. TODO Export immagine (PNG/SVG) di una vista.
3. TODO Export GEDCOM (`ExportGedcom551Tool` / `ExportGedcom7Tool` sono commentati in `FileToolRegistry`); esiste solo
   l'import (GEDCOM, Gramps XML) e l'export FLEF.
4. TODO Export CSV di sottoinsiemi.
5. TODO Stampa di dossier o viste.


## Priorità

1. Collegare `TemporalProjectionPanel` e `SocialNetworkPanel` allo switcher: il lavoro c'è, manca l'accesso.
2. Privacy enforcement e undo/redo applicativo: sono i due casi in cui l'app può mostrare o produrre dati incoerenti.
3. Ricerca globale (Ctrl+F) su tutti i tipi di record.
4. Calcolo di F/R e report delle unioni consanguinee (logica pura, testabile senza UI).
5. Viste di gruppo (Vista A, poi C, poi B).


## Separazione fra dati e presentazione (ordine di figli e partner)

- Le asserzioni genealogiche rappresentano prove e conclusioni. L'ordine in cui figli e partner sono elencati NON e' una prova: e'
  una scelta di presentazione, e utenti diversi possono preferire ordini diversi per lo stesso file.
- Quindi l'ordine e' calcolato al momento del rendering (`PresentationOrder`, `UnionChronology`) a partire dalle evidenze e dalla
  preferenza dell'utente (`OrderPreferences`, salvata in `AppPreferences`, mai nei record): non crea, modifica o elimina record,
  non viene esportato, il formato del file non cambia. Per questo non esistono comandi "Ordina figli" / "Ordina partner".
- Figli: per data di nascita (default, asserzione preferita, indipendentemente dal tipo di relazione; senza nascita in coda, nel
  loro ordine), alfabetico per nome, oppure ordine del modello (`ChildrenOrder`). Partner: per data dell'unione (default:
  matrimonio condiviso, poi il primo evento matrimoniale condiviso, poi `valid_from` della relazione), alfabetico, oppure ordine
  del modello (`PartnerOrder`). Menu Vista > Ordine di figli e partner.
- Punto unico: `GenealogyRepository.getChildren/getPartnerIds` e `SiblingsData` (usato da `SiblingsPanel`); ogni nuova lista di
  figli o partner deve passare da `PresentationOrder`, e non salvare mai l'ordine nei record.


## Dialogs and ESC

- Every dialog closes with ESC, and only itself: the dialogs below it (the ones it was opened from, one from the other) stay as they
  are. The record dialogs and the structures get it from `GUIHelper.createButtonPanel` (ESC is the Cancel button, so the usual
  discard logic runs), the dialogs of the tools from `ToolDialogs.installEscapeToClose`, the others bind the key themselves on the root
  pane (`WHEN_IN_FOCUSED_WINDOW`, so wherever the focus is in the window). A new dialog must do one of these: a test reads the sources
  and fails otherwise (`EveryDialogClosesWithEscTest`). The only exception is `dialogs/ProgressDialog`, which shows a long operation
  that cannot be canceled.
- One press closes one dialog: a key that is held down repeats its press, and the repetitions would reach the dialog below the one that
  just closed, and the one below that. `DialogEscapeSupport` (installed once by the main frame) consumes the repeated presses, so the key
  has to be released and pressed again to close the next dialog. A release that is lost (the window lost the focus with the key down)
  does not block the key: after 1.5 s a press counts as a new one.
- The tests press ESC for real on the dialogs (the action the key is bound to) over a parent dialog, and check that only the dialog
  closes and the model does not change: all the record dialogs, the structures and citations, the tools' helper, the dialogs of the
  assistants (`DialogsEscapeTest`, `AssistantDialogsEscapeTest`); the held key is tested on its own (`DialogEscapeSupportTest`).
- Not verified by a test: the dialogs of the tools one by one (they need arguments: only the helper they use is tested, and the scan
  checks that they use it), and a real key press on a real focus (the tests run the bound action, and the gate on synthetic events).

## TODO Improvements

### IN PROGRESS Implement a global "Research Assistant" system for the genealogy application.

Status: the framework and the first assistant (dates) are implemented, in `ui/assistant`.
- `ResearchAssistantService`: registry of the assistants, search, diagnostics, global shortcut.
- `ResearchAssistantPalette`: the palette (Ctrl+Shift+A, also Tools > Research Assistant...).
- `AssistantAction` / `AssistantContributor` / `DiagnosticProvider` / `Diagnostic`, `AssistantContext`, `AssistantCategory`.
- `FuzzyMatcher`, `DateAssistantContributor` (the Historical Date Assistant, see below).
- `DiagnosticAction`: what the user can do about a finding (Enter on it in the palette), e.g. start the draft of a conclusion.
- `DiagnosticActions`, `OpenRecordsAction`, `RecordChooserDialog`: every finding about records can open them. Enter runs the first
  action of the finding (the one it carries, e.g. the draft of a conclusion, otherwise the opening of its records), Ctrl+Enter
  (Cmd+Enter on a Mac) the second one; the keys are shown under the explanation. A finding about one record opens its usual editor
  (`HandlerRegistry`, `createEditDialog`); about several, a list that stays open while an editor is, to open them one after the
  other and compare them. ESC closes the list. Records that no longer exist are left out. Nothing changes unless the user edits a
  record and saves it in its editor.
- A finding can carry more than one action of its own (`Diagnostic.alternatives`, after the main one): the keys are Enter, Ctrl+Enter and
  Ctrl+Shift+Enter, and the opening of the records is always the last. `Diagnostic.rank` orders the findings of the same severity (the
  higher first); the severity still comes first.
- `AssistantIndicator`, `FindingCounts`: the indicator of the main window, a button at the right end of the toolbar: "Assistant" and
  the findings to look at by severity (`✖ 1  ⚠ 3  ℹ 12`, or a tick when there are none, `…` while it is checking); the tooltip says it
  in words and gives the shortcut; a click opens the palette. The ignored findings are not counted. It exists because the assistant is
  always available with Ctrl+Shift+A, and the way to learn it is to see that it has something to say; an icon in every dialog (grey
  where it does not apply) was not done: the assistant is available everywhere, what varies is the actions, and a grey icon says
  "disabled".
  - Up to date without getting in the way: the findings are computed away from the event dispatch thread, only when the model
    changes (`ModelHistory.addChangeListener`: a step recorded, an undo, a redo, another file), 2 s after the last change, never while a
    modal dialog is open, one computation at a time (a change that comes meanwhile asks for another afterward). A computation that took
    long is not repeated at once: the wait is 4 times how long it took, at least 5 s, so on a big file it does not keep the processor
    busy. Ignoring a finding (or showing it again) only recounts the findings already computed (`IgnoredFindings.addListener`).
- `IgnoredFindings`, `DiagnosticKeys`: Ctrl+D (Cmd+D on a Mac) on a finding ignores it, Ctrl+I shows the ignored findings (Ctrl+D
  there shows one again); the palette tells how many are ignored. The keys are not Delete (it edits the search field). Ignoring is a
  presentation choice of the user, like the order of the children: it lives in the user preferences (`AppPreferences`, one entry per
  file, by the path of the file) and NEVER in the records: nothing is created, modified, deleted or exported. A finding is ignored
  through its `Diagnostic.key()`, built from its kind, its records and the data it reports (names, dates as written, counts: never
  localized text), so it is the same in every language and it comes back by itself when the data changes (the date is corrected and the
  finding is still there, a record is added to the conflict), but not when only the suggestion changes. The preferences keep digests
  (16 hex characters of a SHA-256) of the path and of the keys, not names or dates. A file never saved has no identity: its ignored
  findings last for the session. The tests use an in-memory store, never the preferences of whoever runs them.
- `ResearchAssistantService.recalculate`, `reopenIfChanged`: after the user runs an action on a finding (the editors it opens are
  modal) and finishes with it, the findings are computed again away from the event dispatch thread and compared with the previous ones by
  `identity` (the key, or title and records for a finding without one). If they changed, the palette opens again where it was (same
  search, same view, the same finding selected or the one that took its place) with a notice "N resolved, M new"; if nothing changed,
  nothing opens and the user is left to their work. Ignoring (Ctrl+D) is not an edit and does not recalculate. What the user ignored
  stays ignored across the recalculation, because the ignored findings are told by their key.

Design decisions that refine the text below:
- The shortcut is a global KeyEventDispatcher, not a menu accelerator: the editors of the dates live in modal dialogs that have no
  menu bar, so an accelerator would not fire exactly where the assistant is most useful.
- The context is a snapshot (`AssistantContext`: window, focused component, model) captured BEFORE the palette takes the focus.
  An action finds the editor with `focusedAncestor(SomeInterface.class)`: the editor implements a small host interface (e.g.
  `HistoricalDateAssistantHost`), so the assistant never depends on the Swing panels.
- Three extension points: actions, context (through the host interfaces) and diagnostics. The built-in assistants are registered in
  `ResearchAssistantService.createDefault()` (one line each, like `ToolToolRegistry`); the other ones are `ServiceLoader` providers
  (`uses AssistantContributor` in module-info), so a plugin never changes the code of the assistant.
- Diagnostics are computed away from the event dispatch thread (a provider can inspect the whole model, and must not touch Swing);
  a provider that fails is skipped.
- Search is fuzzy over title, category name and description, so typing a category name filters by category. Without a query the
  order is the relevance in the context, with a query it is the match score.
- Tools that are variants of one another are one action with a selector: the Venetian, Florentine and Pisan converters are the
  `Calendar Conversion` tool of the Historical Date Assistant (a calendar combo), not separate entries. The Roman calendar
  converter will come with the natural-language interpreter.
- Every action must respect the data safety rules below; a test cannot enforce this, so it is a review rule.

- Showing the record in its view (selecting the individual in the tree) is not done: only the editors are opened.
- The indicator computes all the assistants at each change (the identity one compares the whole model): it is not measured on a big file,
  and it cannot be turned off, nor limited to some assistants. The model is read away from the event dispatch thread (see above) while the
  user may be editing: it waits for a modal dialog to close, but not for an edit made without one. It is not colored by severity, and it
  does not blink on a new error.
- Not tested: the real click that opens the palette, and the look of the button (the wiring in the window, the counting, the pacing, the
  ignoring and the model signal are: `AssistantIndicatorTest`, `AssistantIndicatorInFrameTest`, `ModelHistoryListenerTest`).
- The palette opens again whenever the findings changed after an action, even if the edit was not about the finding the user acted on
  (e.g. they fixed another record in the list): there is no preference to turn it off. The reopening itself (the notice, the selection)
  is not tested, it needs the interface; the comparison is (`RecalculationTest`).
- The findings are not refreshed by edits made while the palette is closed: they are computed at each opening. They are computed once
  after an action, not live.
- Ignoring is by finding: there is no "ignore all the findings of this kind" nor "of this individual". The ignored findings are per
  file path (a renamed or moved file loses them) and per user (not shared between machines).
- The adapter of `AppPreferences` (two methods) and the keys of the palette (Ctrl+D, Ctrl+I) are not tested: the first would write the
  preferences of whoever runs the tests, the second needs the interface. The logic is tested with an in-memory store.

Open the records of a finding: done for every assistant (see `OpenRecordsAction` above).
Still to do: none of the five assistants listed here; what remains is in the "Still to do" of each, the diagnostic providers of those (the
Chronology, Conclusion, Relationship, Identity and Research Gap assistants are done), the selected record in the context (the views do
not expose it yet), a visible category filter.

Goal
----
Provide a context-sensitive command palette that exposes genealogical tools and diagnostics without cluttering the UI with buttons.

The Research Assistant should behave similarly to:
- VS Code Command Palette
- JetBrains Search Everywhere
- Copilot contextual actions

Activation
----------
- Keyboard shortcut: Ctrl+Shift+A.
- Accessible from anywhere in the application.
- Opens as a modal/palette overlay.
- Focus is immediately placed in a search input.
- User can navigate entirely with the keyboard.

Core Principle
--------------
The Research Assistant is context-sensitive.

It must inspect the current application context, including:
- active view/panel
- selected record
- active editor
- focused field
- current record type
- current selection

and automatically offer only the most relevant actions.

Architecture
------------
Create a pluggable action framework.

Each assistant action should declare:
- id
- title
- description
- applicable contexts
- execution handler

The assistant must dynamically assemble the list of available actions.

Categories
----------
Actions should be grouped by category:

- Dates
- Identity
- Places
- Sources
- Relationships
- Research
- Conclusions
- Context
- Diagnostics

The assistant should support:
- fuzzy search
- keyboard navigation
- category filtering
- action ranking based on context relevance

Diagnostics Section
-------------------
The assistant must also surface passive findings.

Examples:

Error:
- Child born before parent.
- Event after death.

Warning:
- Multiple birth events.
- Possible duplicate individual.
- Missing death information.

Information:
- Possible CulturalNorm applies.
- Existing unresolved identity hypothesis.

Diagnostics must be searchable alongside actions.

Context Examples
----------------

When a DateStructure editor is active:

Suggested actions:
- Historical Date Assistant
- Liturgical Date Calculator
- Roman Calendar Converter
- Venetian Calendar Converter
- Florentine Calendar Converter
- Pisan Calendar Converter
- Relative Date Calculator
- Age Estimator

When an IndividualRecord is selected:

Suggested actions:
- Find Possible Duplicates
- Check Chronology
- Find Research Gaps
- Find Conflicting Assertions
- Create Conclusion Draft

When a PlaceRecord is selected:

Suggested actions:
- Find Duplicate Places
- Suggest Hierarchy
- Suggest Successor Place
- Suggest Place Relationships

When a SourceCitation editor is active:

Suggested actions:
- Build Citation
- Generate Extract
- Open Crop Tool
- Analyze Contradictory Evidence

When a ConclusionRecord is selected:

Suggested actions:
- Analyze Evidence
- Review Preferred Assertion
- Find Related Conflicts

Initial Assistant Actions
-------------------------

Implement the following as independent actions:

1. Historical Date Assistant
2. Identity Assistant
3. Research Gap Assistant
4. Chronology Assistant
5. Conclusion Assistant
6. Research Planner Assistant
7. Place Assistant
8. Source Citation Assistant
9. Context Assistant
10. Relationship Assistant

User Experience
---------------
The assistant should present two sections:

Actions
- commands the user can execute

Diagnostics
- problems, warnings, and suggestions

Example:

Research Assistant
────────────────────────

Actions
✓ Find Possible Duplicates
✓ Create Conclusion Draft
✓ Estimate Birth Year

Diagnostics
⚠ Multiple Birth Events
⚠ Missing Death Event
⚠ Unresolved Identity Hypothesis

Data Safety Rules
-----------------
The assistant must never:

- automatically merge records
- automatically create conclusions
- automatically modify assertions
- automatically resolve conflicts

The assistant may:
- generate drafts
- propose fixes
- create suggestions
- open dedicated workflows

All genealogical decisions remain under researcher control.

Implementation Notes
--------------------
- Create a reusable ResearchAssistantService.
- Context providers should be extensible.
- Action providers should be extensible.
- Diagnostics providers should be extensible.
- Future assistants must be registerable without modifying core assistant code.
- Add clear comments explaining that the assistant is a research aid and never an authority.

The long-term objective is to make the Research Assistant the primary entry point for advanced genealogy tools while keeping the main user interface simple and uncluttered.

### DONE (except the natural-language interpreter) Create a Historical Date Assistant accessible from every DateStructure editor.

Status: `HistoricalDateAssistantDialog` (Core, `ui/components`) over the engine in `io.github.mtrevisan.calendars.assistant`
(Calendars module). It opens from the Research Assistant (Ctrl+Shift+A in a date editor; category Dates, one action per
tool). Tools: calendar conversion, liturgical calendar (movable feasts, feast references, distance from Easter, Julian computus,
Gregorian reform by country), sunrise/sunset with equal and temporal hours and the readings of the day boundary, relative date,
date relative to an event of the tree, age to estimated birth (completed years or Nth year of life; range or exact).
Every result is a `DateSuggestion` (meaning, assumptions, calendar, steps, a date or a not-before/not-after range): nothing is
applied unless the user asks, the replacement of an existing date is confirmed, and the explanation can be copied.
Still to do: natural-language interpreter (Roman Kalends/Nones/Ides, "more veneto", "one hour after sunset" as text), age with a known
birth to estimate the death date, regnal years.

Goal:
Help genealogists interpret historical date expressions and convert them into normalized FLEF dates.

Capabilities:
- Liturgical date interpretation.
- Feast day interpretation.
- Roman calendar expressions (Kalendae, Nonae, Idus).
- Historical calendar conversion.
- Venetian, Florentine and Pisan style support.
- Easter-based calculations.
- Relative date calculations.
- Sunrise/sunset reasoning.
- Age-based birth/death estimation.

Requirements:
- Never modify data automatically.
- Present one or more candidate interpretations.
- Explain assumptions and reasoning.
- Allow applying the result to the current DateStructure.
- Allow copying the explanation into notes.

The assistant is an aid for historical interpretation, not an authority.

### DONE (core checks) Create an Identity Assistant for duplicate detection.

Status: `ui/assistant/identity`. A diagnostic provider of the Research Assistant (category Identity): `IdentityChecker` (pure: turns
the pairs into findings), `IdentityAssistantContributor`, `CreateIdentityHypothesisDraftAction`, `CompareRecordsAction`. It does not
have a comparison of its own: it uses `DuplicateFinderService`, the one of Tools > Detect Duplicates (names, parents, children, spouses,
siblings, years of birth and death with a tolerance, events, attributes, sources, places; blocking by name, sex as a hard filter).
Done:
- Proposes the pairs of individuals with a similarity score of 60 or more, the 100 best. A pair of 90 or more is a warning, the others
  information; the findings are listed by score (`Diagnostic.rank`). A pair that already has an identity hypothesis is not proposed
  again.
- Each finding gives the score, the confidence it suggests (90 and more high, 70 and more medium, else low: conventions), what makes
  the two look alike (feature by feature, with its weight) and what makes them differ, and the two records. It says that it is a
  possibility, not a probability, and that nothing is merged.
- Three actions (Enter, Ctrl+Enter, Ctrl+Shift+Enter): "Create Identity Hypothesis Draft" (the editor of a new IdentityHypothesisRecord
  with the two individuals, the reasons as the start of the rationale and the confidence; only a draft: nothing is saved unless the
  researcher confirms, cancelling leaves the model as it was; once saved the pair is not proposed again, and the palette, reopened by
  the recalculation, shows the finding as resolved), "Compare Side by Side" (the read-only comparison of the application), and the
  opening of the two records.
- Ignore the candidate: Ctrl+D, like every finding. The key is the pair of records, not the score: a pair the user ignored does not come
  back because a source raised its score. To say that two records are different people for good, the protocol's way is a conclusion that
  disproves the hypothesis (a pair disproven is never proposed again by the merger).
- Fixed on the way (a bug of the duplicate finder, not of the assistant): the years of birth and death were never read from the dates of
  the protocol (it looked for a field called `value` holding a string, but a date is value > point > value > full_date), so the finder
  scored the name only and ignored 25 points of dates, in the assistant and in Tools > Detect Duplicates. It now uses the date normalizer
  (the text reading stays as a fallback). `IdentityAssistantTest` has the regression.
Still to do:
- Only individuals: the other kinds a hypothesis can be about (groups, places, sources, repositories, media, cultural norms, historic
  events) need their own comparison (places are for the Place Assistant).
- The relatives, the places, the sources and the attributes are compared by the records they share, not by similarity: two imports of the
  same tree share none, so a duplicated tree is scored on the name and the dates only (65 at most with the names alone and the dates).
  A second pass that compares the parents and the spouses by name, the places by name and the occupations by text would find them, and
  the matcher of the merger (`EntityMatcher`, `TextSimilarity`) already knows how.
- Names: no historical spellings and no variants by language (Giovanni, Johann, Jean), no patronymics.
- The thresholds (score 60, 100 pairs, 90 and 70 for the confidence) are not configurable, and are not calibrated on real data; the whole
  model is compared at each opening and after each action (the blocking keeps it cheap, but it is not measured on a big file).
- The draft copies neither the sources of the two individuals nor an explanation by feature in the narrative of a conclusion; a "different
  people" action that starts the disproving conclusion is not there.
- "Compare Side by Side" is not tested (it is a modal dialog); the rest of the actions is.

Goal:
Help the researcher discover possible identity hypotheses.

Capabilities:
- Compare names.
- Compare locations.
- Compare dates.
- Compare relationships.
- Compare occupations and attributes.
- Compare source citations.

Output:
- Confidence score.
- Similarity explanation.
- Candidate records.

Actions:
- Create IdentityHypothesisRecord.
- Ignore candidate.
- Open side-by-side comparison.

The assistant must never merge records automatically.
The researcher always decides.

### DONE (core checks) Create a Research Gap Assistant.

Status: `ui/assistant/gap`. A diagnostic provider of the Research Assistant (category Research): `GapChecker` (pure rules, with the
date of today as an input), `GapDataLoader` (reads the model, leaves out what privacy hides), `GapAssistantContributor`,
`CreateResearchDraftAction`. Tested on hand-built data, on a model converted from GEDCOM with research records added by hand, and on the
two real editors.
What the other assistants report is not repeated: an identity hypothesis with no conclusion is a finding of the Conclusion Assistant.
Done:
- Missing from the tree, with the draft of a research QUESTION (what to find out, about whom; the question is only started):
  - no death for an individual born 110 years ago or more (certainly dead, usefulness 85) or 90 years ago or more (probably, 55);
  - no birth for an individual with children or events (45, 55 with children, +5 with events);
  - children whose two parents are linked by no union and no marriage event (45 + 5 for each child, 70 at most).
- Where the research is thin, with the draft of a research TASK:
  - vital events (birth, death, marriage) that no source cites: one finding for all (70);
  - individuals in a census with no residence recorded: one finding for all (40);
  - an open question that no activity works on (75; 45 if on hold); an open question whose activities are all over, with no task
    open and no conclusion (65); a completed search that found nothing, was inconclusive or could not be done, with no follow-up and no
    activity that originated from it (50); the completed activities with no result: one finding for all (25, no draft);
  - a task not done and past its due date (a warning, 70, +15 high priority, +5 normal; no draft: the task is there already).
- Each finding has an estimated usefulness (0-100, high from 70, medium from 45), given in the explanation with the warning that it is
  by rules of thumb and not a judgement of the research, and used as the rank that orders the findings.
- The gaps that are about each individual (no death, no birth, no marriage) are one finding for each up to 25, and one finding for all
  beyond: an imported tree with hundreds of ancestors with no death would bury every other finding and fill the indicator. The aggregate
  has the usefulness of the most useful, lists up to 200 records and has a task for all.
- "Create Research Question Draft" / "Create Research Task Draft": the editor of a new ResearchQuestionRecord (title, question, the
  individuals it is about) or ResearchTaskRecord (description, priority, status open, the questions it supports), through
  `ResearchQuestionRecordDialog.withDraft` and `ResearchTaskRecordDialog.withDraft`. Only a draft: nothing is saved unless the researcher
  confirms, cancelling leaves the model as it was. The gap stays until the research fills it: a question is not an answer.
- Fixed on the way (a defect that the gap messages showed, not specific to them): a year given to a message as a number came out with
  the separator of the thousands ("born 1,900"). Years are given as text.
Still to do:
- Missing in the example of this section and not done: "a place is known but the birth record is missing" (needs the places of the
  residences and events against the birth), and a residence history that is incomplete beyond having none at all (the years between
  two censuses).
- Parents unknown (the brick walls) are not reported: there are as many as ancestors at the edge of the tree; it is the job of the
  Research Planner Assistant to choose which to work on.
- The usefulness is a rule of thumb: the weights are not calibrated on real research and not configurable, and it does not know how
  important the individual is (a direct ancestor of the root, or a collateral), the period or the archives that exist.
- The age for a missing death (110 and 90 years) is a convention; living people could be taken for dead in a tree with no dates of
  death, and a person with no year of birth is never reported.
- The sources of the vital events are looked for on the event only (not on the participation, nor on the individual), and baptisms and
  burials are not vital events here.
- The drafts are in English and plain: no suggestion of where to look (repositories, sources, places), no due date, no link of a
  question to the tasks and activities that would follow.
- The thresholds of the aggregation (25 findings, 200 listed) are not configurable.

Goal:
Identify likely missing information and incomplete research.

Examples:
- Person has birth but no death.
- Children exist but no marriage is known.
- Place is known but birth record is missing.
- Census exists but residence history is incomplete.
- Research question has no activities.
- Identity hypothesis has no conclusion.

Output:
- Ranked list of potential research gaps.
- Explanation for each suggestion.
- Estimated usefulness.

Actions:
- Create ResearchTaskRecord.
- Create ResearchQuestionRecord.
- Ignore suggestion.

This assistant must never create data automatically.

### DONE (core) Create a Source Citation Assistant.

Goal:
Reduce manual effort when creating citations.

Capabilities:
- Suggest repository citations.
- Suggest locators.
- Extract citation information from media metadata.
- Build source citations from archival references.

Examples:
- Volume / page.
- Film / frame.
- Shelf mark.
- Folio.
- Entry number.

Output:
A pre-filled SourceCitation draft.

Done (package `ui.assistant.citation`):
- It is a contextual action (Ctrl+Shift+A in the editor of a source or repository citation, `CitationHost`), not a diagnostic provider.
- `ArchivalReferenceParser` turns free text (vol. 3, p. 45, n. 12 / Libro 2 pag. 143 atto 17 / FHL film 1234567, frame 245 / busta 123, fol. 12v /
  Registro dei nati 1850, n. 12 / segnatura A.3.12 / pp. 45-47) into the locator "Label: value, ..." in a canonical order. Keywords in
  Italian, English, French, German, Spanish and Latin; recto/verso normalized; leading zeros dropped except for film, box and file;
  "c. 1850" is read as circa; duplicate labels keep the first and say so; what is not understood is shown, never guessed.
- `CitationHints`: the file names and descriptions of the media of the cited source are read as references (film1234567_frame245.jpg);
  repositories named in the reference (by name or acronym) are offered to a repository citation.
- `CitationAssistantDialog`: reference text (starting from the current locator), editable locator draft, clues, repositories; it is
  written in the editor only with Apply (asks before replacing a locator), ESC closes it and only it; tests in `CitationAssistantTest`.
Still to do: media metadata (EXIF, PDF properties, ISBN), suggesting whole repository citations (repository + locator) from the
reference, a locator per kind of source (tuned on the media_type), learning the abbreviations of the user, a draft of the whole
SourceCitation (extracts, stance).

The user always confirms before saving.

### DONE (core checks) Create a Relationship Assistant.

Status: `ui/assistant/relationship`. A diagnostic provider of the Research Assistant (category Relationships): `RelationshipChecker`
(pure rules), `RelationshipDataLoader` (reads the model, leaves out what privacy hides), `RelationshipAssistantContributor`,
`CreateRelationshipDraftAction`. Tested on hand-built data, on a model converted from GEDCOM with relationships added by hand, and on
the real editor.
What the other assistants already report is not repeated here: conflicting parentage, more than two biological parents and descent
cycles are findings of the Conclusion Assistant, the ages of the parents (and the dates of the events) of the Chronology Assistant.
Done:
- Suggests the type from the evidence, says what the evidence is, and leaves the choice to the researcher (the suggested type is only
  text in the explanation; the type is changed in the usual editor, opened with Enter or Ctrl+Enter):
  - a generic `child` whose parent took part, with the child, in an `adoption` event: adoptive_child; recorded as `biological_child`
    in the same case: a conflict (warning);
  - a generic `child` whose parent is married to another (biological) parent of the child, with a date of the marriage certainly after
    the birth of the child: step_child;
  - the generic `spouse` (what an import from GEDCOM gives a marriage without a type): religious_spouse if the couple has marriage
    banns and no license, civil_spouse if it has a license and no banns; both or none do not decide;
  - the generic types that the evidence does not help to specify are ONE finding each (spouse, child), not one for each, with the
    first 200 relationships listed.
- Missing: a `marriage` event of exactly two individuals (the principals; a witness does not count) that no relationship of marriage
  (spouse, civil, religious, customary) links, one finding per couple, with "Create Relationship Draft": the editor of a new
  relationship with the two individuals already in it (and the type, only if the banns or the license point to one and it is valid for
  the participants). Only a draft: nothing is saved unless the researcher confirms, cancelling leaves the model as it was
  (`RelationshipRecordDialog.withParticipants`).
- Inconsistent: a relationship of an individual with itself (error); a type that does not fit what it links (a spouse that is a group,
  a membership between individuals, a group that is part of an individual); the same relationship more than once (a couple written
  from both sides is the same relationship; a civil and a religious marriage of the same couple are not); a relationship that ends
  before it begins (error); a role on a relationship of the family, where the type says it all; an engagement with no end although
  the couple married.
- Every finding explains why, lists its records, can be opened, ignored and is recalculated like the others.
Still to do:
- More evidence for the type: the officiant (a priest) and the place (a church) of the marriage for religious_spouse, the evidence
  qualifiers of the sources (a parish register or a civil registry), the ages of the parents (too young or too old to be biological),
  the names (the surname of the father), customary marriages, foster and guarded children (nothing in the research tells them).
- The banns mean religious and the license civil: it is a convention that changes with country and period, there is no way to
  tune it yet.
- Drafts for other missing relationships (a child of the spouse of a parent, the parents named in a birth record); the draft does not
  copy the date of the marriage into valid_from nor the sources of the event.
- "Change the type" is the opening of the editor: there is no action that opens it with the suggested type already chosen.
- Other checks: siblings and half-siblings that contradict the parents, the spouse of a parent that is not linked, the memberships of a
  group against the lifespan of the member, the associates.
- The generic `spouse` is accepted between individuals, but `RelationshipReader.getValidTypes` (what the editor offers) does not list
  it: an imported marriage with the generic type has to be given a type to be saved again from the editor.

Goal:
Help researchers choose the correct relationship type.

Capabilities:
- Analyze participating individuals.
- Analyze supporting evidence.
- Detect possible conflicts.
- Suggest child vs biological_child vs adoptive_child.
- Suggest spouse vs civil_spouse vs religious_spouse.

Warnings:
- Existing conflicting parentage.
- More than two biological parents.
- Chronological inconsistencies.

The assistant only suggests.
It never creates relationships automatically.

### DONE (core checks) Create a Chronology Assistant.

Status: `ui/assistant/chronology`. It is a diagnostic provider of the Research Assistant (category Diagnostics): `ChronologyChecker`
(pure rules, tested without a model), `ChronologyDataLoader` (reads the model, leaves out the individuals hidden by privacy),
`DateRange` (the days a date may stand for), `ChronologyAssistantContributor`. Tested on hand-built data and on a model converted
from GEDCOM.
Done:
- Child born before the parent (biological links only: an adopted, foster, guarded or step child can be older).
- Parent too young (mother 12, father 14) and too old (mother 55, father 90): warnings, the limits are conventions (`Rules`).
- Child born after the death of the mother, or more than 300 days after the death of the father.
- Event after the death (posthumous types are exempt: death, burial, cremation, coroner report, probate, honor, pardon, deed,
  escrow, chancery) and event before the birth, marriage events reported as "marriage before birth".
- Union (relationship `valid_from`) before the birth, or after the death, of a partner.
- Death before birth, lifespan over 120 years.
- Overlapping marriages (information, not error: polygamy exists), one marriage per couple, an end known without a date is not a
  claim.
- A finding is reported only when it holds for every day the dates may stand for (year precision, approximate margin, bounded
  and open dates): an uncertain date never raises a false alarm. Every finding explains its reasoning and lists the records.
Still to do:
- Other overlapping incompatible events (residence, military service, prison, hospitalization) and overlapping attributes.
- Children of the same mother born less than about 9 months apart (twins excepted), a mother who gives birth after her death in
  childbirth, age at marriage too young, a spouse older than the lifespan.
- A relationship `valid_to` before its `valid_from`; events of groups; events whose participant is not an individual.
- The "Check Chronology" action for a selected individual (needs the selected record in `AssistantContext`) and the filtering of the
  findings on it; for now the whole model is checked each time the palette opens.
- Age limits and the gestation limit configurable by the user (preferences); today only `Rules.DEFAULT`.
- Dates in a calendar the normalizer cannot read are skipped silently; they could be reported as "date not checked".
- The model is read away from the event dispatch thread while the user may be editing it: a provider that fails is skipped, but a
  snapshot (or a read lock) would be safer. No cache: a large file is read again at each opening.
- Messages are English only (the UI bundle has no per-locale files yet).

Goal:
Detect chronological inconsistencies.

Checks:
- Child born before parent.
- Parent too young.
- Event after death.
- Marriage before birth.
- Impossible timelines.
- Overlapping incompatible events.

Output:
- Error.
- Warning.
- Informational notice.

The assistant must explain the reasoning behind each finding.

### DONE (core checks) Create a Place Assistant.

Goal:
Help normalize, deduplicate, and contextualize places.

Capabilities:
- Detect possible duplicate places.
- Suggest place relationships.
- Suggest administrative hierarchies.
- Suggest successor_of relationships.
- Suggest place citations.

Examples:
- Treviso -> Veneto.
- Parish -> Diocese.
- Historical jurisdiction changes.

The assistant never modifies PlaceRecords automatically.

Status: `ui/assistant/place`. A diagnostic provider of the Research Assistant (category Places): `PlaceChecker` (pure rules),
`PlaceDataLoader`, `PlaceAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM.
Done:
- Possible duplicate places: same name (ignoring case, accents and punctuation), same type (a missing type makes it only an
  information; a city and a province with the same name are different places), not already related, not in different places (same
  name in different parents, or coordinates more than 50 km apart, is not a duplicate). Ranked by how much the places are used.
  The action is the side-by-side comparison; nothing is ever merged.
- A place that is part of nothing: if its name says where it is (Treviso, Veneto) and one larger place has that name, the draft of the
  relationship names it; with no candidate it is reported only if events happen there (the draft leaves the target to choose).
- A parish that is part of no diocese (draft of an ecclesiastical_part_of; the diocese is proposed only if it is the only one).
- Territorial relationships (administrative, geographic) where the subject is larger than the target (a province in a city), by a
  scale of the types; the ones of ecclesiastical, judicial and cadastral kind and the types off the scale are not judged.
- A place related to itself (error); places that are part of each other in a circle (error) or that succeed each other in a circle
  (warning), reported once per circle.
- More than 25 findings of a kind: the most used are listed and one finding sums up the others.
- "Create Place Relationship Draft": Enter on a finding opens `PlaceRelationshipRecordDialog.withDraft`; saved only if confirmed.
Still to do: successor_of suggestions from the names and dates of places (historical jurisdiction changes), the validity dates of
relationships (valid_from after valid_to, relationships outside the lifetime of a place), place citations (places with no source),
places with no coordinates or type (suggest the type from the name: Diocesi di, Parrocchia di), the usage of places by other records
than events, hierarchy by the names written with several parts, suggestions of the parent from the places near it.

### DONE (core checks) Create a Conclusion Assistant.

Status: `ui/assistant/conclusion`. A diagnostic provider of the Research Assistant (category Conclusions): `ConclusionChecker` (pure
rules), `ConclusionDataLoader` (reads the model, leaves out the individuals hidden by privacy), `ConclusionAssistantContributor`.
Tested on hand-built data and on a model converted from GEDCOM, with a ConclusionRecord added to it.
Done:
- Several birth or death events of an individual: a warning if their dates cannot all be right (no common day), an information if
  they agree (corroboration or duplicates).
- Competing parentage: more than two biological parents, two biological mothers (or fathers); adoptive, foster, guarded and step
  links do not count.
- Conflicting relationships: the same child-parent pair both biological and adoptive/foster/guarded/step.
- An individual that is their own ancestor through the relationships of descent (an error; reported once per cycle).
- Conflicting attributes: more than one value of an attribute that can have only one (`national_id`), compared ignoring case,
  spaces and hyphens.
- Unresolved identity hypotheses between individuals (no conclusion evaluates them, whatever its outcome).
- A conflict is settled when a ConclusionRecord evaluates it: RESOLVES contains at least two of the competing records (events, or
  the participations of the individual in them, or relationships, or attributes) and it prefers one of them, or its proof status
  says that none prevails (conflicting_evidence) or that all are false (disproven). The assertions are never changed.
- Each finding lists the records of the conflict, explains why, and suggests the draft of a ConclusionRecord; for events it names the
  candidate preferred assertion (higher confidence, then more sources) only when one is better supported than all the others, and
  otherwise says there is none. It is a suggestion with its reason: the researcher judges the sources.
- "Create Conclusion Draft": Enter on a finding of the Conclusion Assistant (the palette shows the action in the detail line) opens
  the editor of a new ConclusionRecord, through `ConclusionRecordDialog.createNew(...).withDraft(...).withResolvedTarget(...)`: the
  competing records are already in RESOLVES (and the research questions that target them are linked), the issue is filled
  (`birth_date`, `parentage`, `relationship_type`, `national_id`, `identity`, ...), the narrative starts with the description of the
  conflict. It is only a draft: proof status `unresearched`, NO preferred assertion (the candidate is only named in the narrative),
  nothing saved unless the researcher completes it and confirms, cancelling leaves the model untouched. A draft saved as it is does not
  settle the conflict (a conclusion needs a preferred assertion, or conflicting_evidence or disproven). The mechanism is generic:
  `Diagnostic.action()` (`DiagnosticAction`) is what the user can do about any finding, run only when chosen.
Still to do:
- The draft is built from the model as it was when the palette opened: a record changed or deleted since is skipped, a record that is
  now settled is not detected. The narrative is English and is not translated.
- The editor is not a wizard: the preferred assertion, the proof status and the reasoning are chosen in the usual tabs. A draft for
  an identity hypothesis resolves one target only (a disproven conclusion normally has two).
- The candidate preferred assertion looks only at confidence and the number of sources: it should weigh the evidence qualifiers
  (information type, original or derivative source, direct or indirect evidence). It is given only for births and deaths.
- Other single-valued assertions: the place of birth or death that differs, the burial, the baptism, the sex (several values after a
  merge), the type of a relationship that differs between sources.
- Attributes that change in time (residence, occupation, citizenship, religion) need a comparison aware of `valid_from` and
  `valid_to`, to tell a change from a conflict; the reported `children_count` and `marriages_count` against the tree.
- Relationships: a child-parent pair that is also a spouse pair, civil and religious marriages of the same couple that differ in date,
  parentage of a child by different couples in different sources.
- Identity hypotheses: only "no conclusion" is checked; a hypothesis evaluated before new evidence arrived (a stale conclusion, by the
  audit dates), a candidate that is not an individual (place, source, repository).
- The consistency of the conclusions themselves: a `preferred` that is not in RESOLVES, `proven` with a single source, RESOLVES that
  point to records that no longer exist.
- "Settled" is defined here and in `ProtocolCleanup.isResolved` (the merger requires a preferred assertion, the assistant also accepts
  conflicting_evidence and disproven): the two should share one definition.
- Same limits as the Chronology Assistant: the whole model is read at each opening, away from the event dispatch thread while the
  user may be editing; English messages only.

Goal:
Help researchers identify unresolved conflicts.

Detect:
- Multiple birth events.
- Multiple death events.
- Competing parentage.
- Conflicting attributes.
- Conflicting relationships.
- Unresolved identity hypotheses.

Suggest:
- Creation of ConclusionRecord.
- Candidate preferred assertion.
- Records participating in the conflict.

The generated conclusion is always a draft.
The researcher must review and confirm it.

### DONE (core checks) Create a Context Assistant.

Goal:
Help researchers link cultural norms and historical events to conclusions.

Capabilities:
- Suggest relevant HistoricEventRecords.
- Suggest relevant CulturalNormRecords.
- Explain possible impacts.

Examples:
- War-related migrations.
- Napoleonic legal reforms.
- Naming conventions.
- Inheritance customs.

Suggest ContextImpactRecord drafts.

Never create records automatically.

Status: `ui/assistant/context`. A diagnostic provider of the Research Assistant (category Context): `ContextChecker` (pure rules),
`ContextDataLoader`, `ContextAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM with a historic event
and a cultural norm added by hand. The catalog of historic events and norms is the one of the model: the assistant does not bring any.
Done:
- A historic event or a cultural norm of the model that may have influenced an event of the tree: the dates overlap, the place is the
  same or contains the place of the event (by the territorial relationships, at any depth), the kind of context can influence that type
  of event (a war and a death or a conscription, an epidemic and a death, a marriage law and a marriage, a naming custom and a birth,
  an inheritance rule and a death or a will...). Usefulness 55 with the place, 35 without, +10 if the event is entirely inside the
  period. Events with no date, no bounded date or a date wider than 5 years are not used; a historic event needs a date, a norm a date
  limit or a place; an impact that exists already is not proposed again; 3 events at most for each context, 25 in all.
- Every finding says why (the period, the place) and how the context may matter (a hint for each kind), with the usefulness, and the
  draft of a ContextImpactRecord (the context, the event, the type of impact: explains, influences, constrains or motivates, and the start
  of the explanation) in the usual editor, through `ContextImpactRecordDialog.withDraft`. The sources are for the researcher to give.
- Several deaths (or burials) of different individuals in the same place within two years (3 or more), that no historic event of the
  model of the kind of a crisis (epidemic, pandemic, famine, war, natural disaster) covers in that place and period: a finding with the
  draft of a HistoricEventRecord (type epidemic as a first guess, title with the place and the years), through
  `HistoricEventRecordDialog.withDraft`.
Still to do: the date and the place of the draft of a historic event; migrations (an individual in two distant places) and the
historic events of the place of origin; naming conventions checked against the names of the tree (patronymics); the age rules of the
norms (minimum age of marriage against the ages of the spouses); events of the individual other than the events of the tree (attributes
and residences); a catalog of historic events and norms to import; the cultural norms that apply to a conclusion or a relationship;
the weights are not calibrated.

### DONE (core checks) Create a Research Planner Assistant.

Goal:
Help researchers plan next steps.

Input:
- ResearchQuestion.
- Existing sources.
- Existing conclusions.
- Existing activities.

Output:
Suggested ResearchTaskRecords.

Examples:
- Search marriage records.
- Review parish baptisms.
- Examine census records.
- Contact archive.

Tasks should be prioritized and justified.

Status: `ui/assistant/planner`. A diagnostic provider of the Research Assistant (category Research): `PlannerChecker` (pure rules, on the
data of the Gap Assistant: `GapData`, `GapDataLoader`, `GapDraft` and `CreateResearchDraftAction` are now public, and a question carries
its targets), `PlannerAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM with a question added.
Done:
- Next steps of an open question with no conclusion (the individuals it targets, or the principals of the events it targets, up to 3):
  the birth or baptism record (no birth: 80; no source cites it: 65), the marriage record that names the parents (parents unknown: 75;
  marriage unrecorded or uncited: 60), the baptisms of the years around the birth (parents unknown, year known: 55), the death or burial
  record (born 80 years ago or more, death missing or uncited: 55), the census (born 1700-1950, in none: 40), contacting the archive (two
  searches of the question completed with no positive result: 50). +5 if nobody started the question.
- A step is left out if a task or an activity of the question (not abandoned) already says it does it (by words, in several languages);
  at most 3 steps for each question and 25 in all, the others summed up in one finding.
- Brick walls: individuals with no known parents and at least one descendant, ranked by how many descendants depend on them (40 + 3 for
  each, at most 30; +10 year of birth, +5 events; 85 at most); not proposed if an open question already targets them; 10 at most, the
  others summed up in one finding. The draft is a research QUESTION ("Who were the parents of X, born Y?").
- Every suggestion says why and gives its usefulness (high, medium, low; a rule of thumb, as for the gaps); a step is the draft of a research
  TASK (description, priority from the usefulness, supports the question) in the usual editor, saved only if the researcher confirms.
Still to do: the places and the periods (which parish, which archive keeps the records, which registers exist for the years: needs
the repositories and the places of the individual), the sources already in the tree (the steps do not read the citations of the
individual beyond the events), the conclusions of the question, a due date, the order of the steps as a plan (dependencies: the marriage
before the baptisms), questions about places and families, the weights are not calibrated nor configurable.

