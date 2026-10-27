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
4. TODO Undo/redo applicativo: oggi Ctrl+Z / Ctrl+Y agiscono solo dentro ai dialog dei record (`UndoController` in
   `BaseRecordDialog`); le voci del menu Modifica non annullano le modifiche al modello.
5. TODO Pannello "Modifiche recenti".
6. DONE Confronto affiancato di due record (`RecordDiffDialog`, `MergeFieldComparisonDialog`).
7. TODO Filtri sul dossier (sezioni, tipo di evento, intervallo, presenza di fonte).
8. PARZIALE Tooltip arricchiti: presenti in `IndividualPanel`, strip, timeline, ego network (layout); da verificare su
   albero e Sugiyama.
9. DONE Scorciatoie da tastiera: F2 modifica, Canc elimina, Ctrl+X sposta, Ctrl+Z/Y, Ctrl+1/2/3 cambio proiezione, Ctrl+L,
   Ctrl+T, Ctrl+J, F11, F1, frecce per navigare nel grafo (`SpatialNavigation`). Mancano Ctrl+F e Ctrl+S (c'è solo
   "Salva con nome").


## Per le viste grafiche

1. PARZIALE Albero: il collasso del pedigree c'è; mancano filtri/espansione di rami ed evidenziazione del percorso tra
   due individui.
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
