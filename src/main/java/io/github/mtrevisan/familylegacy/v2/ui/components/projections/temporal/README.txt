1. Dimensione temporale e cronologia eventi (Timeline / Life Map)
FLEF memorizza eventi datati (nascite, matrimoni, migrazioni, carriere, decessi) e sequenze temporali. Nessuna delle
due viste mostra l'evoluzione nel tempo: manca una vista Timeline/Gantt per sovrapporre le vite, vedere eventi
concomitanti e durate di unioni o carriere.


Ecco l'analisi del punto 1, tenendo conto che **la General Temporal Projection è già stata implementata** nei layer 1–7. L'analisi quindi distingue tra ciò che è coperto e ciò che manca ancora.

---

## Cosa FLEF fornisce (ripasso)

Il protocollo è estremamente ricco sul piano temporale.

**Assertion temporali per entità:**

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

**Forme del valore temporale:**
- `point` / `SingleDate`: `full_date` (ISO 8601), `decade` (start_year), `century` (ordinal + part), ciascuno con `approximate` (basis + margin) e `calendar`.
- `bounded` / `BoundedDate`: `not_before` / `not_after` (incertezza).
- `spanning` / `SpanningDate`: `from` / `to` (durata reale).

**Calendari:** 12 valori (`gregorian`, `julian`, `islamic`, `hebrew`, `chinese`, `indian`, `buddhist`, `french_republican`, `coptic`, `soviet_eternal`, `ethiopian`, `mayan`).

**Contesto:** `ContextImpactRecord` collega `HistoricEventRecord` e `CulturalNormRecord` a un target qualsiasi (individual, group, place, event, relationship, …) con cinque tipi di impatto (`explains`, `influences`, `constrains`, `motivates`, `causes`).

---

## Cosa copre già la General Temporal Projection

| Aspetto | Coperto | Dove |
|---|---|---|
| Righe per individui, gruppi, luoghi | ✓ | `TemporalProjectionService.buildRows` |
| EVENT track (partecipazioni a eventi) | ✓ | `TemporalExtractor.extractEventEntries` |
| ATTRIBUTE track (validità attributi) | ✓ | `TemporalExtractor.buildAttributeEntry` |
| CONTEXT track (impatti contestuali) | ✓ | `TemporalExtractor.collectContextImpactEntries` |
| Connections individual↔individual | ✓ | `buildRelationshipConnections` |
| Connections individual↔group | ✓ | idem |
| Connections group↔group | ✓ | idem |
| Connections place↔place | ✓ | `buildPlaceConnections` |
| Bande di contesto (historic events, norms) | ✓ | `buildBands` |
| Impact links | ✓ | `buildImpactLinks` |
| Normalizzazione date (point, bounded, spanning) | ✓ | `DateNormalizer` |
| Calendari non gregoriani (5 supportati) | ✓ | `CalendarConverter` |
| Precisione ridotta (decade, secolo) | ✓ | `NormalizedDate` + renderer |
| Approssimazione con alone | ✓ | `TemporalSpanRenderer` |
| Stato active/ended/unknown | ✓ | `TemporalSpan.status` + stroke |
| Zoom e pan | ✓ | `TemporalAxis` + `TemporalZoomController` |
| Auto-scaling dell'asse | ✓ | `TemporalAxis.deriveZoomLevel` |
| Filtri per tipo di entità | ✓ | `TemporalProjectionToolbar.EntityFilter` |
| Toggle layer (bands, connections, impacts) | ✓ | `TemporalProjectionToolbar` |
| Click su entry → edit dialog | ✓ | `TemporalProjectionPanel.openEditDialogFor` |
| Multi-partecipante su singolo evento | ✓ | Una entry per partecipante, connesse allo stesso evento |
| Evidenza negativa | ✗ | Non distinta visivamente |

---

## Cosa manca ancora

1. **Lifespan bar.** Ogni individuo non ha una barra continua da nascita a morte. Ha singoli marker (nascita, eventi, morte). Una vera "life map" dovrebbe mostrare la vita come una linea continua, con gli eventi come marker sopra di essa. Questo permette di vedere a colpo d'occhio "quanto è vissuto" e "quando è successo cosa".

2. **Allineamento per generazione.** Le righe sono ordinate per tipo e label, non per generazione. Una timeline genealogica tipicamente raggruppa gli individui per generazione (nonni, genitori, figli), così da vedere sovrapposizioni tra generazioni.

3. **Età agli eventi.** "A che età si è sposato?", "quanti anni aveva quando è emigrato?". Il calcolo non esiste. Ogni evento puntuale su una persona potrebbe mostrare l'età della persona a quella data, calcolata dalla nascita.

4. **Durata delle relazioni.** "Quanto è durato il matrimonio?" Non calcolato. Un'unione con `valid_from` e `valid_to` ha una durata esplicita.

5. **Analisi di sovrapposizione.** "Chi era vivo nel 1850?", "quali eventi erano simultanei?", "chi era presente alla nascita di X?". Query non implementate.

6. **Confronto affiancato.** Due individui (o due rami) con assi sincronizzati. L'attuale vista è un singolo focus.

7. **Heatmap temporale.** Istogramma della densità di eventi nel tempo. "In quali anni ci sono stati più eventi?" Non esiste.

8. **Gruppi di eventi per categoria.** Attualmente tutto è un unico EVENT track. Una separazione per categoria (vital events, migration events, career events, military events, legal events) renderebbe la vista più leggibile.

9. **Biografia narrativa.** Generare un testo sequenziale dal modello temporale: "Mario nacque nel 1850 a Treviso. Nel 1866 si arruolò. Nel 1870 sposò Anna. Nel 1880 emigrò in Argentina. Nel 1920 morì." Non esiste.

10. **Dettaglio on-hover del contesto.** Passando il mouse su un marker, vedere il contesto storico/normativo che lo influenza (via `ContextImpactRecord`). Il tooltip attuale mostra solo dati dell'evento.

11. **Confronto con historic events.** "Cosa succedeva nel mondo quando X è nato?" Il layer delle bande mostra il contesto, ma non c'è una query esplicita "mostra tutti gli historic events attivi tra il 1850 e il 1860".

12. **Supporto calendari completi.** Mancano `hebrew`, `chinese`, `indian`, `buddhist`, `french_republican`, `soviet_eternal`, `mayan`. Attualmente `CalendarConverter` lancia `UnsupportedOperationException`. Almeno `french_republican` è rilevante per genealogie italiane del periodo napoleonico.

13. **Time slider.** Uno slider che scorre l'asse temporale senza cambiare lo zoom, con un indicatore verticale mobile. Utile per "cosa era visibile in questo istante".

14. **Export in formato tabellare.** Non c'è modo di estrarre la timeline come CSV/Excel per analisi esterne.

15. **Evidenza negativa.** `EvidenceQualifiers.evidence_type = negative` non è visualizzato. Un "nessun evento in questo intervallo" dovrebbe essere rappresentato come spazio vuoto attivo, non come semplice assenza di barra.

---

## Cosa è ancora utile implementare

Su questi 15 punti, la maggior parte è **estensione** della General Temporal Projection, non una nuova vista. Ecco la priorità che suggerirei.

### Estensione A — Lifespan bar e allineamento per generazione

Modifica di `TemporalProjectionLayout` e `TemporalExtractor` per:
- aggiungere alla riga di un individuo una **barra dello span di vita** (da `birth` a `death`), disegnata come sfondo della riga;
- raggruppare le righe per generazione, con un header di generazione tra i gruppi.

**Nuove classi:**
1. `LifespanResolver` — class. Calcola lo `span` di vita di un individuo dai suoi eventi `birth` e `death`.
2. `GenerationAssigner` — class. Assegna ogni individuo a una generazione tramite analisi dell'albero biologico (già discusso per il Pedigree Graph).
3. `LifespanBarRenderer` — class. Disegna la barra della vita come sfondo della riga, con distinzione tra nascita e morte (marker), e tratteggio per lo span di vita quando la data di morte è incerta (`BOUNDED`).

### Estensione B — Età agli eventi e durate

**Nuove classi:**
4. `AgeCalculator` — class. Dato un individuo e una data, calcola l'età. Usa la data di nascita (evento `birth`) come riferimento. Gestisce precisione ridotta (decade, secolo) con output "~X anni".
5. `DurationCalculator` — class. Data una coppia di date, restituisce la durata in anni/mesi/giorni.
6. `RelationshipDurationReport` — record. Per un `RelationshipRecord` con `valid_from` e `valid_to`, contiene durata e status.

**Estensione al renderer:** ogni entry può mostrare l'età dell'individuo accanto al marker, e ogni connection può mostrare la durata dell'unione al centro dell'arco.

### Estensione C — Analisi di sovrapposizione

**Nuove classi:**
7. `TemporalOverlapQuery` — class. Dato un anno e un tipo di entità, restituisce tutti gli individui vivi in quell'anno (o tutte le relazioni attive). Costruita sui `TemporalSpan` già esistenti.
8. `TemporalDensityAnalyzer` — class. Calcola la densità di eventi per unità di tempo (anno, decennio), per la heatmap.

**Estensione UI:** uno slider temporale in `TemporalProjectionToolbar` che imposta un anno di interesse, e un comando "Mostra solo attivi al [anno]" che filtra la vista.

### Estensione D — Vista biografica

**Nuova vista complementare:**
9. `BiographyPanel` — class. Per un individuo, mostra la sequenza cronologica dei suoi eventi in formato narrativo. Ogni riga è un paragrafo: "1850 — Nato a Treviso, da Giovanni e Maria". Con hover sincronizzato con la timeline: passare il mouse su un evento nella timeline evidenzia il paragrafo corrispondente, e viceversa.
10. `BiographyGenerator` — class. Costruisce il testo biografico dai dati temporali, con template per ogni tipo di evento.

### Estensione E — Confronto affiancato

**Nuova vista:**
11. `TimelineComparisonPanel` — class. Due `TemporalProjectionPanel` con un asse condiviso. La selezione su uno dei due sincronizza la selezione sull'altro. Utile per confrontare coniugi, fratelli, generazioni.

### Estensione F — Categorie di eventi separate

**Estensione al layer 3:**
12. `TemporalEventCategory` — enum. `VITAL`, `MIGRATION`, `CAREER`, `MILITARY`, `LEGAL`, `RELIGIOUS`, `FINANCIAL`, `OTHER`.
13. `TemporalEventClassifier` — class. Mappa un `EventRecord.type` del protocollo a una `TemporalEventCategory`.

**Estensione al layout:** la EVENT track si divide in sotto-track per categoria, come già avviene per il Social Network con le categorie.

### Estensione G — Calendari aggiuntivi

**Estensione a `CalendarConverter`:**
14. Implementare `french_republican` (calendario rivoluzionario francese, rilevante per genealogie italiane 1797–1805).
15. Implementare `hebrew` (calendario ebraico, rilevante per genealogie ebraiche).

Gli altri (`chinese`, `indian`, `buddhist`, `soviet_eternal`, `mayan`) richiedono dati astronomici o sono ambigui; possono restare `UnsupportedOperationException` con messaggio esplicito.

### Estensione H — Export

**Nuova classe:**
16. `TemporalProjectionExporter` — class. Esporta la proiezione in CSV o Excel: una riga per entry con colonne `entity_id`, `entity_label`, `track`, `type`, `role`, `start_jdn`, `start_display`, `end_jdn`, `end_display`, `status`, `source_id`.

---

## Cosa NON fare

- **Non ricostruire la General Temporal Projection.** È completa nella sua struttura.
- **Non aggiungere motori di visualizzazione esterni** (JFreeChart, TimelineJS, ecc.). Il codebase ha già tutto il necessario.
- **Non complicare il layout con troppe lane.** Se aggiungi categorie di eventi, il numero di track per riga può esplodere. Meglio mantenere la struttura a tre track (EVENT, ATTRIBUTE, CONTEXT) e usare il **colore** per distinguere le categorie dentro EVENT.
- **Non tentare di supportare calendari astronomici.** Il protocollo lo permette, ma la complessità non è giustificata per una genealogia europea.

---

## Riepilogo

| Aspetto del punto 1 | Stato | Priorità |
|---|---|---|
| Timeline multi-entità con eventi, attributi, contesti | ✓ completo | — |
| Calendari (5 su 12) | parziale | alta (french_republican, hebrew) |
| Precisione ridotta (decade, secolo) | ✓ completo | — |
| Approssimazione | ✓ completo | — |
| Bounded / spanning | ✓ completo | — |
| Stato active/ended/unknown | ✓ completo | — |
| **Lifespan bar** | ✗ mancante | alta |
| **Allineamento per generazione** | ✗ mancante | alta |
| **Età agli eventi** | ✗ mancante | media |
| **Durata delle relazioni** | ✗ mancante | media |
| **Analisi sovrapposizione** | ✗ mancante | media |
| **Biografia narrativa** | ✗ mancante | media |
| **Confronto affiancato** | ✗ mancante | bassa |
| **Categorie di eventi** | ✗ mancante | media |
| **Export tabellare** | ✗ mancante | bassa |
| **Heatmap temporale** | ✗ mancante | bassa |
| **Evidenza negativa** | ✗ mancante | bassa |

Il punto 1 è coperto **strutturalmente** dalla General Temporal Projection, ma mancano le estensioni che la rendono una **Life Map** completa (lifespan bar, età, generazioni) e gli strumenti di **analisi** (sovrapposizione, densità, biografia).

Se vuoi procedere, suggerirei di partire dall'**Estensione A** (lifespan bar + generazioni), perché è quella che più cambia la percezione della vista: trasforma un grafico di eventi in una vera mappa delle vite.
