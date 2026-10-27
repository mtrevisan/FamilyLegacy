# Dimensione temporale e cronologia eventi (Timeline / Life Map)

FLEF memorizza eventi datati (nascite, matrimoni, migrazioni, carriere, decessi) e sequenze temporali. Questo documento
elenca cosa il codice copre oggi e cosa manca. Legenda: DONE = verificato nel codice, TODO = non presente,
PARZIALE = presente solo in parte o altrove.

Stato verificato sul codice (ottobre 2026).


## Stato della General Temporal Projection

Il package `temporal` contiene tutti i layer (modello, estrattore, indici, layout, renderer, asse, zoom, toolbar,
`TemporalProjectionPanel`). **Il pannello però non è collegato a nessuna schermata**: `ProjectionType` espone solo
`BIOLOGICAL_TREE`, `SUGIYAMA_GRAPH` ed `EGO_NETWORK`, e `TemporalProjectionPanel` non è istanziato da nessuna parte.
Finché non viene aggiunto un `ProjectionType` (o un'altra via d'accesso) la proiezione è codice non raggiungibile.

TODO collegare `TemporalProjectionPanel` al `ProjectionSwitcherPanel` (nuovo `ProjectionType`, voce di menu, shortcut).

Viste temporali già raggiungibili dall'utente, che coprono parte di questo punto:
- `MultiLifespanStripPanel` (barra comprimibile sotto albero ed ego network): una riga per individuo, barra dal primo
  all'ultimo evento datato, marker per evento, bande per gli attributi con validità, sfondo a secoli, hover che evidenzia le
  vite sovrapposte, click per modificare il record.
- `GlobalEventTimelinePanel` (sotto l'ego network): timeline globale degli eventi delle entità visibili.
- `TimelineViewDialog` (menu Eventi): timeline in dialogo.
- `ChronomapPanel` / `AgoraPanel`: mappa storica con asse temporale; Agorà mostra chi era vivo e dove in una data.
- Report: `IndividualLifeStorySection` / `IndividualNarrator` generano la biografia narrativa.


## Cosa FLEF fornisce

| Record | Campo | Semantica |
|---|---|---|
| `EventRecord` | `date: DateStructure` | punto o intervallo dell'evento |
| `IndividualAttributeRecord` | `valid_from` / `valid_to` | validità dell'attributo |
| `GroupAttributeRecord` | `valid_from` / `valid_to` | validità dell'attributo di gruppo |
| `RelationshipRecord` | `valid_from` / `valid_to` / `status` | validità e stato della relazione |
| `PlaceRelationshipRecord` | `valid_from` / `valid_to` | validità della giurisdizione |
| `CulturalNormRecord` | `valid_from` / `valid_to` | validità della norma |
| `HistoricEventRecord` | `date` | data dell'evento storico |
| `ContextImpactRecord` | (implicita, via `context`) | impatto contestuale |

Forme del valore temporale: `point` / `SingleDate` (`full_date`, `decade`, `century`, con `approximate` e `calendar`),
`bounded` / `BoundedDate` (`not_before` / `not_after`), `spanning` / `SpanningDate` (`from` / `to`).

Contesto: `ContextImpactRecord` collega `HistoricEventRecord` e `CulturalNormRecord` a un target qualsiasi con cinque tipi
di impatto (`explains`, `influences`, `constrains`, `motivates`, `causes`).


## Calendari

DONE Tutti i 12 calendari del protocollo (`gregorian`, `julian`, `islamic`, `hebrew`, `chinese`, `indian`, `buddhist`,
`french_republican`, `coptic`, `soviet_eternal`, `ethiopian`, `mayan`) sono implementati, più altri (armeno, bahai,
berbero, bizantino, egizio, giapponese, coreano, vietnamita). La conversione sta nel modulo Maven `Calendars`
(JPMS `io.github.mtrevisan.calendars`), non più in `CalendarConverter` di Core. I calendari astronomici (cinese, armeno)
richiedono il plugin Ephemeris (`Core/plugins/ephemeris.jar`); senza il plugin lanciano
`UnsupportedOperationException` con messaggio esplicito.

TODO conversione inversa JDN -> data nativa (rimandata per scelta).


## Cosa copre la General Temporal Projection (codice nel package `temporal`)

| Aspetto | Stato |
|---|---|
| Righe per individui, gruppi, luoghi | DONE |
| Track EVENT, ATTRIBUTE, CONTEXT | DONE |
| Connessioni individuo-individuo, individuo-gruppo, gruppo-gruppo, luogo-luogo | DONE |
| Bande di contesto e impact link | DONE |
| Normalizzazione date (point, bounded, spanning), precisione ridotta, approssimazione | DONE |
| Stato active/ended/unknown | DONE |
| Zoom, pan, auto-scaling dell'asse | DONE |
| Filtri per tipo di entità, toggle dei layer | DONE |
| Click su entry -> dialog di modifica | DONE |
| Raggiungibilità dall'interfaccia | TODO (vedi sopra) |


## Cosa manca ancora

1. PARZIALE **Lifespan bar.** Presente in `MultiLifespanStripPanel` (barra dal primo all'ultimo evento datato), non nella
   proiezione temporale. Non distingue nascita/morte né tratteggia la morte incerta.
2. TODO **Allineamento per generazione.** Né la proiezione né la strip raggruppano per generazione (le righe sono ordinate
   per tipo e label).
3. TODO **Età agli eventi.** Nessun calcolo dell'età a una data, nemmeno con "~X anni" per le precisioni ridotte.
4. TODO **Durata delle relazioni.** Non calcolata né mostrata sui connettori.
5. DONE **Analisi di sovrapposizione.** Hover della strip (vite sovrapposte evidenziate) e `AgoraPanel` ("chi era vivo
   in questa data e dove"). Manca una query per le relazioni attive in un anno.
6. PARZIALE **Confronto affiancato.** La strip confronta molte vite su un asse condiviso; non c'è un confronto di due
   proiezioni con assi sincronizzati.
7. **Heatmap temporale** (densità di eventi per anno).
8. TODO **Categorie di eventi** (vital, migration, career, military, legal, ...): un solo EVENT track, colore per tipo.
9. DONE **Biografia narrativa.** Generata nei report (`IndividualNarrator`, `IndividualLifeStorySection`). Manca il pannello
   con hover sincronizzato alla timeline.
10. TODO **Dettaglio on-hover del contesto** storico/normativo (via `ContextImpactRecord`) sui marker.
11. TODO **Query "historic events attivi tra due anni".**
12. DONE **Calendari completi** (vedi sopra).
13. PARZIALE **Time slider.** Esiste la timeline di `ChronomapPanel`; non c'è nella proiezione temporale.
14. TODO **Export tabellare** (CSV/Excel) della timeline.
15. DONE **Evidenza negativa.** `evidence_type = negative` è letto da `TemporalExtractor` (`evidence` di evento, attributo, impatto, evento storico, norma) e disegnato da `TemporalSpanRenderer`: corpo attenuato con tratteggio diagonale sulle barre e X sui marker puntuali; il tooltip lo segnala. Vale solo per la proiezione temporale (non ancora raggiungibile, vedi sopra); le altre viste non lo mostrano.


## Priorità suggerita

1. Collegare `TemporalProjectionPanel` a `ProjectionType` (senza questo il resto non è visibile).
2. Allineamento per generazione, età agli eventi e durate (estendono `TemporalProjectionLayout` / `TemporalExtractor`).
3. Categorie di eventi (solo colore, mantenendo le tre track).
4. Heatmap, confronto affiancato, export tabellare.

Classi previste per le estensioni, tutte ancora da scrivere: `LifespanResolver`, `GenerationAssigner`,
`LifespanBarRenderer`, `AgeCalculator`, `DurationCalculator`, `RelationshipDurationReport`, `TemporalOverlapQuery`,
`TemporalDensityAnalyzer`, `BiographyPanel` + `BiographyGenerator` (se si vuole la vista sincronizzata),
`TimelineComparisonPanel`, `TemporalEventCategory` + `TemporalEventClassifier`, `TemporalProjectionExporter`.


## Cosa NON fare

- Non ricostruire la General Temporal Projection: la struttura è completa.
- Non aggiungere motori di visualizzazione esterni (JFreeChart, TimelineJS, ...).
- Non moltiplicare le lane: mantenere tre track (EVENT, ATTRIBUTE, CONTEXT) e usare il colore per le categorie di evento.
