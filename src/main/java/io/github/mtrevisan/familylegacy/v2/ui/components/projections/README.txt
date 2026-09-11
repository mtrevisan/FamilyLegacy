# Viste di gruppo e strutture collettive (Group / Community View)

FLEF supporta il tag Group (casate, corporazioni, ordini, associazioni). Manca una vista dedicata che mostri
l'appartenenza di più individui a un ente nel tempo e le interazioni tra gruppi diversi.


## Cosa FLEF fornisce

Il protocollo è già strutturato per una vista di gruppo completa.

**Entità gruppo.**
`GroupRecord` ha `name*` (multi-valore, `NameStructure` con type: official, legal, colonial, indigenous, religious, administrative, archival, …), `type` (family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe, oppure testo custom), `preferred_image`, `source`, `note`, `privacy`, `audit`.

**Appartenenza.**
`RelationshipRecord` con `type = group_member` collega Individual → Group. Il campo `role` è libero e il protocollo ne suggerisce alcuni: `member`, `president`, `secretary`, `treasurer`, `resident`, `head_of_household`, `tribal_leader`, `elder`, `custodian`. `valid_from` / `valid_to` / `status` danno la dimensione temporale.

**Gerarchia tra gruppi.**
`part_of` collega Group → Group (sottogruppo → sopragruppo). Stessa struttura temporale.

**Associazioni tra gruppi.**
`associate` è ammesso anche Group → Group e Group → Individual. Non ha semantica specifica, ma è la base per modellare "questa confraternita è in corrispondenza con quest'altra".

**Attributi di gruppo.**
`GroupAttributeRecord` con `type`: residence, member_count, children_count, social_class, ethnicity, religion, language, wealth, land_holding, primary_income_source, oppure custom. Con `value`, `valid_from` / `valid_to`, `place`.

**Eventi di gruppo.**
`EventRecord` + `EventParticipationRecord` con `participant` che può essere un `GroupRecord`. Permette di modellare fondazioni, fusioni, scioglimenti, assemblee, processioni, trasferimenti. Ruoli come `grantor`, `grantee`, `officiant`, `witness`, `landlord`, `tenant` sono già nel protocollo.

**Contesto.**
`ContextImpactRecord` può avere come target un `GroupRecord` o un `GroupAttributeRecord`. Un historic event (guerra, epidemia, riforma) può "spiegare" o "influenzare" l'esistenza o l'evoluzione di un gruppo.


## Cosa manca

1. **Vista di appartenenza nel tempo.**
   Nessuna vista mostra la linea temporale di un gruppo con i suoi membri che entrano ed escono. Il `GroupViewPanel` che abbiamo abbozzato è una JTree + JTable, che copre l'elenco statico ma non il tempo.

2. **Multi-appartenenza di un individuo.**
   Un individuo può appartenere a più gruppi contemporaneamente. Nessuna vista mostra "tutte le appartenenze di questa persona nel tempo".

3. **Gerarchia tra gruppi.**
   `part_of` è un DAG con date. Nessuna vista mostra "questo gruppo contiene questi sottogruppi in questo periodo".

4. **Interazioni tra gruppi.**
   `associate` tra gruppi non è visualizzato. Non è possibile vedere "quali gruppi erano in contatto".

5. **Ruoli nel tempo.**
   Un individuo può essere `member` nel 1780, `secretary` nel 1785, `president` nel 1790. Il ruolo non è statico. Nessuna vista lo mostra come carriera.

6. **Eventi del gruppo.**
   Fondazione, fusione, scioglimento, assemblee, processioni: sono in `EventRecord` ma non c'è una vista che li collochi sulla storia del gruppo.

7. **Attributi di gruppo nel tempo.**
   `member_count` che passa da 20 a 200 in cinquant'anni, `residence` che si sposta, `wealth` che cambia: sono dati registrati ma invisibili.

8. **Contesto storico-territoriale del gruppo.**
   Le riforme napoleoniche hanno sciolto confraternite; le leggi razziali hanno chiuso associazioni; un'epidemia ha ridotto una comunità. `ContextImpactRecord` permette di modellarlo, nessuna vista lo mostra.


## Architettura proposta

Tre viste complementari. La logica è simile a quella della Temporal Projection, ma applicata ai gruppi.

### Vista A — Group Timeline

Per un singolo gruppo: una timeline orizzontale in cui ogni riga è un **aspetto del gruppo** e ogni elemento è un evento con data.

Righe:
- **Appartenenze**: una riga per membro, con una barra dalla data di ingresso a quella di uscita, colorata per ruolo.
- **Gerarchia**: una riga per sottogruppo, con una barra dalla data di ingresso all'uscita.
- **Eventi del gruppo**: punto per ogni evento (fondazione, assemblee, fusioni, scioglimenti).
- **Attributi**: una riga per attributo, con una barra dalla `valid_from` alla `valid_to`.
- **Contesto**: bande di sfondo per gli historic events e le norme culturali che impattano il gruppo.

Riutilizza quasi tutto di `TemporalProjectionPanel`. La differenza è che il **soggetto** non è un'entità singola, ma un gruppo con le sue diramazioni.

### Vista B — Group Network

Per un insieme di gruppi: una rete in cui i nodi sono gruppi e gli archi sono `part_of` e `associate`. Ogni gruppo ha un badge che mostra il numero di membri. Click su un nodo → apre la Vista A. Filtro temporale per mostrare solo le relazioni attive in un anno.

Complementare alla rete sociale del punto 2, ma con i gruppi come nodi anziché gli individui.

### Vista C — Individual Group Career

Per un individuo: una timeline che mostra tutte le sue appartenenze ai gruppi nel tempo, con il ruolo per ogni periodo. Risponde a "cosa ha fatto questa persona nella sua vita associativa". Utile per capire il contesto sociale di un individuo.


## Elenco classi

### Layer 1 — Enumerazioni

1. **`GroupViewMode`** — enum: `TIMELINE`, `NETWORK`, `CAREER`.
2. **`GroupTimelineRowType`** — enum: `MEMBER`, `SUBGROUP`, `EVENT`, `ATTRIBUTE`, `CONTEXT`.
3. **`GroupRelationCategory`** — enum: `HIERARCHY`, `ASSOCIATION`, `MEMBERSHIP`.

### Layer 2 — Dati di base

4. **`GroupMemberRef`** — record: `individual` (`TemporalEntityRef`), `role` (String), `span` (`TemporalSpan`), `status` (String), `sourceRecord` (`FLEFRecord`).
5. **`GroupSubgroupRef`** — record: `subgroup` (`TemporalEntityRef`), `role` (String), `span` (`TemporalSpan`), `status`, `sourceRecord`.
6. **`GroupEventRef`** — record: `event` (`TemporalEntityRef`), `span` (`TemporalSpan`), `eventType` (String), `participants` (`List<TemporalEntityRef>`), `sourceRecord`.
7. **`GroupAttributeRef`** — record: `type` (String), `value` (String), `span` (`TemporalSpan`), `placeRef` (`TemporalEntityRef`, nullable), `sourceRecord`.
8. **`GroupAssociationRef`** — record: `otherGroup` (`TemporalEntityRef`), `role` (String), `span` (`TemporalSpan`), `sourceRecord`.

### Layer 3 — Aggregati

9. **`GroupProfile`** — record: `entity` (`TemporalEntityRef`), `name`, `type`, `members` (`List<GroupMemberRef>`), `subgroups` (`List<GroupSubgroupRef>`), `supergroups` (`List<GroupSubgroupRef>`), `events` (`List<GroupEventRef>`), `attributes` (`List<GroupAttributeRef>`), `associations` (`List<GroupAssociationRef>`), `domainStart` / `domainEnd` (`NormalizedDate`, nullable).
10. **`GroupTimelineRow`** — record: `type` (`GroupTimelineRowType`), `label`, `entries` (`List<GroupTimelineEntry>`), `sortOrder`.
11. **`GroupTimelineEntry`** — record: `span` (`TemporalSpan`), `label`, `role`, `category`, `sourceRecord`.
12. **`GroupNetworkModel`** — record: `nodes` (`List<GroupProfile>`), `edges` (`List<GroupNetworkEdge>`), `snapshotYear` (Integer, nullable).
13. **`GroupNetworkEdge`** — record: `source` (`TemporalEntityRef`), `target`, `type` (`part_of` / `associate`), `span` (`TemporalSpan`), `sourceRecord`.
14. **`IndividualCareerModel`** — record: `individual` (`TemporalEntityRef`), `memberships` (`List<GroupMemberRef>`), `groupProfiles` (`Map<String, GroupProfile>` per risolvere i nomi).

### Layer 4 — Servizi

15. **`GroupIndices`** — class. Indici pre-calcolati:
    - `membersByGroupId` (`group_member` con target = gruppo)
    - `groupsByMemberId` (`group_member` con subject = individuo)
    - `subgroupsByParentId` (`part_of` con target = gruppo)
    - `parentsBySubgroupId` (`part_of` con subject = gruppo)
    - `associationsByGroupId` (`associate` con uno dei due endpoint = gruppo)
    - `groupParticipationsByGroupId` (`EventParticipationRecord` con participant = gruppo)
    - `groupAttributesByGroupId`
    - `contextImpactsByGroupId`
    Stessa struttura di `TemporalIndices`, filtrata per tipo di relazione e tipo di partecipante.
16. **`GroupProfileService`** — class. Costruisce un `GroupProfile` per un dato gruppo combinando tutti i dati da `GroupIndices`, applicando i filtri.
17. **`GroupTimelineBuilder`** — class. Da un `GroupProfile`, costruisce le righe di una timeline, ordinate e pronte per il rendering.
18. **`GroupNetworkService`** — class. Costruisce il `GroupNetworkModel` (nodi = gruppi, archi = `part_of` + `associate`), con filtro temporale opzionale.
19. **`IndividualCareerService`** — class. Costruisce l'`IndividualCareerModel` per un individuo, unendo le sue appartenenze ai gruppi con i profili dei gruppi coinvolti.
20. **`GroupFilters`** — record: `categories` (`Set<GroupRelationCategory>`), `roles` (`Set<String>`), `groupTypes` (`Set<String>`), `minDate` / `maxDate` (`NormalizedDate`), `includeInactive` (`boolean`). Unico punto di verità per i filtri.

### Layer 5 — Layout e proiezione

21. **`GroupTimelineAxis`** — class. Come `TemporalAxis`, ma condivisa da tutte le righe della timeline di gruppo. Riusa la logica di `TemporalAxis` (probabilmente si può estrarre una superclasse, oppure si può direttamente riusare `TemporalAxis` — è già viewport-indipendente).
22. **`GroupTimelineLayout`** — class. Come `TemporalProjectionLayout`: assegna Y alle righe, gestisce lane per sovrapposizioni, calcola altezza totale.
23. **`GroupNetworkLayout`** — class. Layout per la Vista B. Due opzioni:
    - **Gerarchico**: `part_of` come albero, `associate` come archi trasversali. Deterministico, semplice.
    - **Anelli concentrici**: come il Social Network del punto 2, con i gruppi "radice" al centro.
    Raccomandato: gerarchico, perché `part_of` è già una gerarchia e va mostrato come tale.

### Layer 6 — Renderer

24. **`GroupTimelineRowRenderer`** — class. Disegna una riga della timeline di gruppo. Simile a `TemporalRowRenderer`, ma con palette per ruolo.
25. **`GroupTimelineEntryRenderer`** — class. Disegna una barra di appartenenza con il ruolo come testo interno quando lo spazio lo permette.
26. **`GroupNetworkNodeRenderer`** — class. Disegna un nodo-gruppo con nome, tipo, badge del numero di membri. Riusa `GroupPanel` quando possibile.
27. **`GroupNetworkEdgeRenderer`** — class. Disegna gli archi `part_of` (gerarchici, linee spesse) e `associate` (trasversali, linee sottili tratteggiate).
28. **`GroupTimelineRenderer`** — class. Orchestratore della Vista A.
29. **`GroupNetworkRenderer`** — class. Orchestratore della Vista B.
30. **`IndividualCareerRenderer`** — class. Orchestratore della Vista C.

### Layer 7 — UI

31. **`GroupExplorerPanel`** — class. Il `JPanel` principale, che ospita le tre viste e permette di passare dall'una all'altra.
32. **`GroupExplorerToolbar`** — class. Controlli: mode selector (timeline/network/career), slider temporale, filtri per tipo di gruppo e ruolo, campo di ricerca.
33. **`GroupExplorerInteractionHandler`** — class. Hit-testing, tooltip (nome, ruolo, date, fonte), doppio-click per aprire la dialog del record.
34. **`GroupTimelinePanel`** — class. Implementazione della Vista A.
35. **`GroupNetworkPanel`** — class. Implementazione della Vista B.
36. **`IndividualCareerPanel`** — class. Implementazione della Vista C.
37. **`GroupSelectionDialog`** — class. `JDialog` per scegliere un gruppo da visualizzare, riusando `RecordSelectionDialog` con filtro su `GroupHandler`.
38. **`GroupEditorDialog`** — class. Dialog per creare/modificare un `GroupRecord` con nome, tipo, immagine, note. Può essere il `GroupRecordDialog` già esistente nel codebase — verificare.


## Coerenza con l'esistente

**Riuso diretto:**
- `TemporalAxis`, `TemporalSpan`, `NormalizedDate`, `TemporalEntityRef`, `TemporalSpanRenderer` — tutta la Temporal Projection è riutilizzabile per la Vista A.
- `TemporalProjectionLayout` come modello per `GroupTimelineLayout`.
- `TemporalIndices` come modello per `GroupIndices`.
- `GroupPanel`, `GroupData`, `GroupListener` — già esistenti.
- `IndividualPanel` per i nodi-individuo nella timeline.
- Il pattern service + layout + renderer + panel + toolbar + interaction handler è identico.

**Nuovo:**
- La semantica dei **ruoli di gruppo** (member, president, secretary, treasurer, …) e la loro resa visiva.
- La gestione della **gerarchia `part_of`** come layout ad albero.
- La gestione delle **carriere individuali** (Vista C), che è una vista nuova ma banale da costruire con gli stessi strumenti.

**Cosa NON fare:**
- Non usare JTree/JTable — già discusso: rompe il linguaggio visivo del codebase.
- Non duplicare la logica di indicizzazione: `GroupIndices` estende `TemporalIndices` con i suoi tipi, oppure è una classe separata che condivide lo stesso pattern.
- Non creare un motore di layout di grafi generico: la gerarchia `part_of` è già strutturata, e il layout ad albero è sufficiente.
- Non tentare di mostrare tutti i gruppi in una volta: con centinaia di gruppi il grafo diventa illeggibile. La Vista B deve sempre essere filtrata (per tipo, per anno, per appartenenza a un individuo).


## Cosa copre esattamente

| Aspetto FLEF | Vista A (Timeline) | Vista B (Network) | Vista C (Career) |
|---|---|---|---|
| Membri del gruppo nel tempo | ✓ | — | — |
| Ruoli dei membri | ✓ | — | ✓ |
| Sottogruppi (`part_of`) | ✓ | ✓ | — |
| Gruppi associati (`associate`) | ✓ | ✓ | — |
| Eventi del gruppo | ✓ | — | — |
| Attributi del gruppo | ✓ | — | — |
| Contesto (`ContextImpact`) | ✓ | — | — |
| Appartenenze di un individuo | — | — | ✓ |
| Carriera nei ruoli di un individuo | — | — | ✓ |
| Relazioni tra gruppi | — | ✓ | — |
| Snapshot temporale della rete | — | ✓ | — |

Le tre viste insieme coprono l'intero punto 4.


## Ordine di implementazione consigliato

1. `GroupViewMode` + `GroupTimelineRowType` + `GroupRelationCategory`
2. `GroupMemberRef` + `GroupSubgroupRef` + `GroupEventRef` + `GroupAttributeRef` + `GroupAssociationRef`
3. `GroupIndices` (dipende solo dal modello FLEF)
4. `GroupFilters`
5. `GroupProfileService` + `GroupProfile`
6. `GroupTimelineEntry` + `GroupTimelineRow` + `GroupTimelineBuilder`
7. `IndividualCareerService` + `IndividualCareerModel`
8. `GroupNetworkEdge` + `GroupNetworkService` + `GroupNetworkModel`
9. `GroupTimelineLayout` + `GroupTimelineAxis` (riuso di `TemporalAxis`)
10. `GroupTimelineEntryRenderer` + `GroupTimelineRowRenderer` + `GroupTimelineRenderer`
11. `GroupTimelinePanel` (Vista A)
12. `IndividualCareerRenderer` + `IndividualCareerPanel` (Vista C)
13. `GroupNetworkLayout` + `GroupNetworkNodeRenderer` + `GroupNetworkEdgeRenderer` + `GroupNetworkRenderer`
14. `GroupNetworkPanel` (Vista B)
15. `GroupExplorerToolbar` + `GroupExplorerInteractionHandler` + `GroupExplorerPanel`

La Vista A è la più completa e la più simile alla Temporal Projection: implementandola per prima si riusa al massimo. La Vista C è la più semplice e può essere fatta in parallelo. La Vista B è indipendente e può essere l'ultima.


## Nota sul `GroupViewPanel` precedente

Il `GroupViewPanel` che abbiamo abbozzato con JTree+JTable può essere conservato come vista **amministrativa** (dentro un dialog di configurazione o come pannello secondario per editing rapido di più gruppi). Non è la vista di esplorazione genealogica, ma non è inutile: è un modo compatto per gestire molti gruppi quando si vuole fare editing in serie.

Se vuoi, la prossima cosa che possiamo fare è partire dall'implementazione della Vista A, che è quella che copre più aspetti del punto 4.

---

5. Parentela estesa ed endogamia (Pedigree Collapse / Consanguinity Diagram)
FLEF traccia re-incroci e matrimoni tra consanguinei. L'albero standard duplica i nodi o va in crisi visiva in caso di
pedigree collapse. Manca un grafo unificato che mostri i loop di consanguineità e i coefficienti di parentela senza
duplicazione.

Ecco l'analisi del punto 5.


## Cosa FLEF fornisce

Il protocollo non ha un tipo "consanguineità", ma la struttura per calcolarla è completa.

**Relazioni di filiazione.**
`RelationshipRecord` con `type = biological_child` (Individual → Individual) è la base per costruire l'albero di ascendenza. Esiste anche `adoptive_child`, `foster_child`, `guarded_child`, `step_child`, ma per il calcolo della consanguineità genetica contano solo i legami biologici.

**Unioni.**
`civil_spouse`, `religious_spouse`, `customary_spouse`, `cohabiting_partner`, `engaged_partner` sono i legami tra due individui che possono essere consanguinei tra loro. Il protocollo non fa distinzione tra "matrimonio tra consanguinei" e "matrimonio tra estranei": è sempre lo stesso tipo di relazione, la consanguineità va **calcolata** dagli alberi.

**Fonti sui legami.**
Ogni `RelationshipRecord` porta `source`, `note`, `evidence`, `valid_from` / `valid_to`, `status`. La consanguineità può quindi essere dibattuta, ipotizzata, provata.

**Dimensione temporale.**
`valid_from` / `valid_to` permettono di distinguere matrimoni storici da unioni recenti. Rilevante per la consanguineità: in una popolazione chiusa, i matrimoni tra consanguinei si concentrano in certi periodi.


## Cosa manca

1. **Nessun calcolo di consanguineità.**
   Non esiste né il coefficiente di inbreeding di Wright `F` per un individuo, né il coefficiente di relazione `R` tra due individui.

2. **Nessuna identificazione di antenati comuni.**
   "Chi è l'antenato comune più recente tra A e B?" non è calcolato da nessuna parte.

3. **Nessuna gestione visiva del pedigree collapse.**
   L'albero genealogico standard duplica i nodi quando un individuo compare più volte nella stessa ascendenza. Con l'endogamia, la duplicazione esplode: un individuo che ha 6 generazioni di endogamia può avere alberi "virtuali" di milioni di nodi, ma solo poche centinaia di individui distinti.

4. **Nessun grafo unificato.**
   Servirebbe un DAG in cui ogni individuo è un nodo singolo, con i legami di filiazione come archi diretti. La consanguineità diventa visibile come **convergenza di cammini**: due sposi che condividono un antenato sono collegati attraverso due percorsi che si ricongiungono.

5. **Nessuna vista dei loop.**
   La consanguineità produce "anelli": A è figlio di B e C; B e C sono cugini; quindi A ha un antenato che appare due volte nel suo albero. Nessuna vista mostra questi anelli.

6. **Nessuna vista delle unioni consanguinee.**
   Nella rete sociale, un matrimonio tra cugini è solo un matrimonio. Non viene segnalato come consanguineo.

7. **Nessun coefficiente visibile.**
   Se due coniugi sono cugini primi, il coefficiente di relazione `R = 1/8`. Se sono zio-nipote, `R = 1/4`. Se sono fratelli, `R = 1/2`. Questi valori non sono calcolati né mostrati.

8. **Nessuna query di parentela.**
   "Come sono imparentati A e B?" non ha risposta nell'applicazione attuale.

9. **Nessuna gestione dei pedigree collapse reali.**
   Le genealogie nobiliari e le comunità chiuse (villaggi alpini, isole, comunità religiose) hanno collassi profondi. Servono strutture dati che non esplodano in memoria.


## Il problema tecnico centrale

Il calcolo di `F` e `R` può essere fatto in modi diversi.

**Naive**: enumerare tutti i cammini tra due individui attraverso gli antenati comuni. Esplode con il pedigree collapse.

**Corretto**: usare le formule di Wright con memoizzazione.

**Coefficiente di inbreeding `F(X)`** per un individuo X:
```
F(X) = Σ [ (1/2)^(n1+n2+1) × (1 + F(A)) ]
```
dove la somma è su tutte le coppie di cammini dal padre e dalla madre di X a un antenato comune A, `n1` e `n2` sono le distanze in generazioni dai genitori ad A, e `F(A)` è il coefficiente di A.

**Coefficiente di relazione `R(A, B)`** tra due individui:
```
R(A, B) = 2 × F(figlio ipotetico di A e B)
```

Entrambi si calcolano con una **passata topologica** sugli individui, dai più vecchi ai più giovani. Poiché gli antenati di X sono sempre più vecchi di X, `F(X)` dipende solo da individui già processati.

Con memoizzazione delle "distanze di generazione" (mappa `individualId → mappa(ancestorId → Set<int>)`), il calcolo è polinomiale nel numero di individui e nel numero di cammini per coppia di genitori. In pratica, su genealogie reali (anche con collasso profondo), è dell'ordine di secondi.

**Il collo di bottiglia reale è il numero di cammini** tra un individuo e un suo antenato quando ci sono endogamie multiple. In questi casi la somma include molte coppie di cammini e il coefficiente è dominato dai termini più corti. Si può troncare la somma oltre una soglia (es. `(1/2)^8`), che corrisponde a un contributo < 0.4%, ed è la prassi standard nelle genealogie.


## Architettura proposta

Tre viste complementari, più una vista di report.

### Vista A — Pedigree Graph (DAG unificato)

Un grafo in cui ogni individuo compare **una sola volta**. Gli archi sono di due tipi:
- **verticali** (solidi, diretti verso il basso): `biological_child`
- **orizzontali** (tratteggiati): unioni (`civil_spouse`, `religious_spouse`, `customary_spouse`, `cohabiting_partner`)

La consanguineità appare come **convergenza**: due sposi che condividono un antenato sono collegati attraverso due percorsi verticali che si ricongiungono.

Sugli archi verticali può essere mostrata la "doppia discendenza": quando un individuo A ha due figli B e C che si sposano tra loro, il grafo mostra le due discese da A come due linee distinte che si incontrano di nuovo sui figli di B e C.

Layout: **Sugiyama** (DAG stratificato), il classico per genealogie unificate. Deterministico, preserva le generazioni, gestisce naturalmente i ricongiungimenti.

### Vista B — Ancestry Focus

Data una persona, mostra la sua ascendenza **proiettata** sul DAG. Le duplicazioni diventano visibili: lo stesso individuo appare in posizioni diverse della proiezione ad albero, ma è lo stesso nodo del DAG (evidenziato in un colore dedicato).

Il coefficiente di inbreeding `F(X)` è mostrato accanto al nome. Le linee che rappresentano "lo stesso individuo raggiunto da più cammini" sono evidenziate come anelli chiusi.

### Vista C — Consanguinity Report

Una tabella delle unioni consanguinee rilevate nel modello:
- coniuge A, coniuge B
- grado di parentela (fratelli, zio-nipote, cugini primi, cugini secondi, …)
- coefficiente di relazione `R`
- coefficiente di inbreeding `F` di ciascun figlio nato dall'unione
- antenato/i comune/i più recenti
- fonte delle relazioni usate per il calcolo

Ordinabile per coefficiente, per periodo storico, per ramo familiare.

### Vista D — Kinship Query

Dialogo che prende due individui e risponde a "come sono imparentati?". Mostra:
- il cammino minimo attraverso l'albero (numero di passi)
- l'antenato comune più recente (MRCA)
- il coefficiente di relazione `R`
- le fonti dei legami usati


## Elenco classi

### Layer 1 — Enumerazioni

1. **`KinshipRelationType`** — enum: `SELF`, `PARENT`, `CHILD`, `SIBLING`, `GRANDPARENT`, `GRANDCHILD`, `GREAT_GRANDPARENT`, `UNCLE_AUNT`, `NEPHEW_NIECE`, `FIRST_COUSIN`, `FIRST_COUSIN_ONCE_REMOVED`, `SECOND_COUSIN`, `THIRD_COUSIN`, `DISTANT`, `UNRELATED`. Serve per etichettare in linguaggio naturale il risultato di una query.
2. **`ConsanguinityLevel`** — enum: `NONE` (F=0), `DISTANT` (F<1/128), `MODERATE` (1/128≤F<1/32), `CLOSE` (1/32≤F<1/8), `VERY_CLOSE` (F≥1/8). Serve per la colorazione visiva.
3. **`PedigreeLayoutMode`** — enum: `SUGIYAMA`, `COMPACT_HIERARCHY`, `FOCUSED_SUBGRAPH`.
4. **`PedigreeEdgeType`** — enum: `BIOLOGICAL`, `ADOPTIVE`, `FOSTER`, `GUARDIAN`, `STEP`, `UNION`. Determina lo stile dell'arco.

### Layer 2 — Dati di base

5. **`KinshipPath`** — record: `individuals` (`List<TemporalEntityRef>`, la sequenza di individui lungo il cammino), `edges` (`List<FLEFRecord>`, le relazioni usate), `length` (int, numero di passi). Immutabile.
6. **`CommonAncestor`** — record: `ancestor` (`TemporalEntityRef`), `distanceFromA` (int, generazioni), `distanceFromB` (int), `pathFromA` (`KinshipPath`), `pathFromB` (`KinshipPath`), `contribution` (double, il termine `(1/2)^(n1+n2+1)`).
7. **`InbreedingResult`** — record: `individual` (`TemporalEntityRef`), `coefficient` (double, Wright's F), `level` (`ConsanguinityLevel`), `contributingPairs` (`List<CommonAncestor>`, le coppie di cammini che contribuiscono).
8. **`RelationshipResult`** — record: `individualA` (`TemporalEntityRef`), `individualB` (`TemporalEntityRef`), `coefficient` (double, Wright's R), `kinshipType` (`KinshipRelationType`), `mostRecentCommonAncestors` (`List<CommonAncestor>`), `shortestPath` (`KinshipPath`).

### Layer 3 — Aggregati

9. **`PedigreeNode`** — record: `entity` (`TemporalEntityRef`), `generation` (int, dalla radice del DAG), `inbreedingCoefficient` (double), `consanguinityLevel` (`ConsanguinityLevel`), `isCollapsed` (boolean, true se questo individuo appare più volte nell'ascendenza di qualcun altro nel grafo).
10. **`PedigreeEdge`** — record: `source` (`TemporalEntityRef`), `target`, `type` (`PedigreeEdgeType`), `span` (`TemporalSpan`), `sourceRecord` (`FLEFRecord`).
11. **`PedigreeGraphModel`** — record: `nodes` (`List<PedigreeNode>`), `edges` (`List<PedigreeEdge>`), `focusIndividual` (`TemporalEntityRef`, nullable), `maxGeneration` (int). Immutabile.
12. **`ConsanguinityReport`** — record: `unions` (`List<ConsanguineousUnion>`), `individuals` (`List<InbreedingResult>`), `domainStart` / `domainEnd` (`NormalizedDate`).
13. **`ConsanguineousUnion`** — record: `spouseA` (`TemporalEntityRef`), `spouseB`, `relationshipCoefficient` (double), `kinshipType` (`KinshipRelationType`), `childrenInbreeding` (`List<InbreedingResult>`), `commonAncestors` (`List<CommonAncestor>`), `unionRecord` (`FLEFRecord`).

### Layer 4 — Servizi

14. **`KinshipIndices`** — class. Indici pre-calcolati:
    - `parentsByIndividualId` (`Map<String, List<FLEFRecord>>`, relazioni `biological_child` in entrata)
    - `childrenByIndividualId` (relazioni in uscita)
    - `unionsByIndividualId` (relazioni spouse)
    - `topologicalOrder` (`List<String>`, ordinamento dagli antenati ai discendenti)
    Costruiti una volta sola, in una passata lineare sul modello.
15. **`AncestorResolver`** — class. Calcola, per un individuo, la mappa `ancestorId → List<KinshipPath>` (tutti i cammini verso ogni antenato). Usa BFS con memoizzazione. Tronca oltre una profondità configurabile.
16. **`InbreedingCalculator`** — class. Calcola il coefficiente di Wright `F` per un individuo o per tutti gli individui in una passata topologica. Usa `AncestorResolver` e `KinshipIndices`.
17. **`RelationshipCalculator`** — class. Calcola il coefficiente `R` tra due individui, l'MRCA, il tipo di parentela in linguaggio naturale, il cammino minimo. Usa `AncestorResolver` per entrambi e interseca i risultati.
18. **`ConsanguineousUnionDetector`** — class. Trova tutte le unioni tra individui che condividono almeno un antenato. Per ogni unione, produce un `ConsanguineousUnion` completo.
19. **`PedigreeGraphService`** — class. Costruisce il `PedigreeGraphModel` a partire dal modello FLEF:
    - nodi = tutti gli individui che compaiono in almeno una relazione `biological_child` o spouse;
    - archi verticali = `biological_child`;
    - archi orizzontali = spouse;
    - assegna la generazione a ogni nodo con una passata topologica;
    - marca i nodi che compaiono in più ascendenza (`isCollapsed`);
    - delega a `InbreedingCalculator` il calcolo di `F` per ogni nodo;
    - delega a `ConsanguineousUnionDetector` la marcatura degli archi di unione consanguinei;
    - filtra opzionalmente per un individuo focale (solo i suoi antenati + il suo ramo).
20. **`ConsanguinityReportService`** — class. Produce il `ConsanguinityReport` completo, con tutte le unioni consanguinee del modello.
21. **`KinshipQueryService`** — class. Data una coppia di individui, restituisce un `RelationshipResult`.
22. **`PedigreeFilters`** — record: `includeAdoptive` (boolean), `includeFoster` (boolean), `includeGuardian` (boolean), `includeStep` (boolean), `maxPathDepth` (int), `minCoefficient` (double), `focusIndividualId` (String, nullable), `maxGenerations` (int). Unico punto di verità per i filtri.

### Layer 5 — Layout

23. **`PedigreeGraphLayout`** — class. Layout Sugiyama:
    - **layer assignment**: assegna un livello a ogni nodo in base alla generazione (già calcolata dal service);
    - **ordering**: riduce gli incroci con l'euristica del baricentro (barycenter) applicata iterativamente;
    - **coordinate assignment**: assegna X e Y a ogni nodo rispettando l'ordine;
    - **edge routing**: archi verticali dritti, archi orizzontali curvi;
    - produce una mappa `TemporalEntityRef → Rectangle`.
24. **`PedigreeViewport`** — class. Finestra visibile con pan e zoom. Simile a `TemporalAxis` e `GeoMapViewport`, ma per un DAG con estensione su entrambi gli assi.
25. **`CollapseDetector`** — class. Dato il `PedigreeGraphModel`, identifica i "punti di collasso": individui che compaiono più volte nell'ascendenza di un focus individual. Produce un `Map<TemporalEntityRef, List<TemporalEntityRef>>` (antenato → discendenti che lo duplicano).

### Layer 6 — Renderer

26. **`PedigreeNodeRenderer`** — class. Disegna un nodo: card compatta con nome, anno di nascita/morte, e — se ha `isCollapsed` o `F > 0` — un badge con il coefficiente. Colore del bordo in base a `ConsanguinityLevel`.
27. **`PedigreeEdgeRenderer`** — class. Disegna un arco verticale o orizzontale, con stile in base al tipo. Gli archi "doppi" (dove la stessa coppia di genitori ha più figli o dove due coniugi hanno antenati comuni) sono evidenziati.
28. **`KinshipPathHighlighter`** — class. Evidenzia un `KinshipPath` sul grafo, con colore dedicato e numerazione dei passi.
29. **`ConsanguinityOverlayRenderer`** — class. Disegna gli "anelli" di consanguineità: per ogni coppia di coniugi con antenati comuni, disegna un anello chiuso che passa per i coniugi e i loro MRCAs.
30. **`InbreedingHeatmapRenderer`** — class. Colora i nodi in base al valore di `F`, con palette a gradiente. Utile per vedere "dove si concentra l'endogamia".
31. **`PedigreeGraphRenderer`** — class. Orchestratore: nodi, archi, path evidenziati, anelli, heatmap.

### Layer 7 — UI

32. **`PedigreeGraphController`** — class. Pan, zoom, fit-to-bounds, centra-su-individuo.
33. **`PedigreeGraphInteractionHandler`** — class. Hit-testing dei nodi, tooltip (nome, dati, `F`, genitori, figli), doppio-click per aprire `IndividualRecordDialog`, click su un arco per aprire `RelationshipRecordDialog`.
34. **`PedigreeGraphToolbar`** — class. Controlli: zoom, fit, mode (DAG completo / focus individuale / subgraph), toggle per tipi di arco, slider per profondità massima, campo di ricerca individuo, toggle heatmap consanguineità.
35. **`PedigreeGraphPanel`** — class. Il `JPanel` principale della Vista A.
36. **`AncestryFocusPanel`** — class. Vista B. Proiezione dell'ascendenza di un individuo con duplicazioni evidenziate.
37. **`ConsanguinityReportPanel`** — class. Vista C. Tabella ordinabile delle unioni consanguinee.
38. **`KinshipQueryDialog`** — class. Vista D. Due `RecordSelectionDialog` per scegliere gli individui, poi il risultato in un pannello.
39. **`ConsanguinityLegendPanel`** — class. Legenda dei coefficienti e della scala di colori.


## Algoritmi chiave in dettaglio

**Assegnazione della generazione.**
Un individuo senza genitori noti è a generazione 0. Un figlio è a `max(generazione_padre, generazione_madre) + 1`. Con un pedigree collapse, questo va calcolato in modo **topologico**, non ricorsivo, per evitare ricorsioni infinite. In pratica: ordinamento topologico dei nodi per "chi non ha genitori → primo", poi propagazione.

**Calcolo di F.**
Passata topologica. Per ogni individuo X:
1. Trova tutti i cammini da `padre(X)` verso l'alto e da `madre(X)` verso l'alto.
2. Interseca gli insiemi di antenati raggiunti.
3. Per ogni antenato comune A, somma `(1/2)^(n1 + n2 + 1) × (1 + F(A))`, dove `n1` e `n2` sono le distanze in generazioni.
4. `F(A)` è già stato calcolato perché A è più vecchio di X.

La memoizzazione dei cammini (per individuo) rende il calcolo polinomiale. Il caso peggiore è esponenziale, ma su genealogie reali è gestibile fino a profondità ~12 generazioni con endogamia moderata.

**Calcolo di R.**
`R(A, B) = 2 × F(figlio ipotetico di A e B)`. Si costruisce un individuo virtuale con A e B come genitori e si applica il calcolo di F. L'MRCA è l'antenato comune con `n1 + n2` minimo.

**Rilevamento delle unioni consanguinee.**
Per ogni coppia (A, B) coniugi, calcola `R(A, B)`. Se `R > 0`, l'unione è consanguinea. Il grado di parentela è derivato dalle distanze dagli MRCAs.

**Visualizzazione del collasso.**
Il DAG è naturalmente senza duplicazioni. La Vista B (focus individuale) proietta l'ascendenza come albero; quando un individuo viene raggiunto due volte nella proiezione, viene disegnato **due volte** nella proiezione ma **collegato visivamente** con una linea che dice "stesso individuo". Questo è il modo più chiaro per mostrare il collasso senza perdere né l'uno né l'altro punto di vista.


## Coerenza con l'esistente

**Riuso:**
- `TemporalEntityRef`, `TemporalSpan`, `NormalizedDate` — già presenti.
- `TemporalIndices` come modello per `KinshipIndices`.
- Il pattern service + layout + renderer + panel + toolbar + interaction handler è identico.
- `IndividualPanel` può essere riusato per i nodi quando il layout lo permette.
- `IndividualRecordDialog` e `RelationshipRecordDialog` per l'editing.

**Nuovo:**
- Calcolo dei coefficienti di Wright — unico punto di verità in `InbreedingCalculator` e `RelationshipCalculator`.
- Layout Sugiyama — unico layout non ancora presente nel codebase.
- Il concetto di "nodo collassato" — unico in `CollapseDetector`.
- Il concetto di "anello di consanguineità" — unico in `ConsanguinityOverlayRenderer`.

**Cosa NON fare:**
- Non mostrare il DAG completo con centinaia di migliaia di nodi. Filtra sempre per focus, per profondità, o per coefficiente.
- Non tentare di calcolare `F` per un individuo con 20 generazioni di endogamia senza troncatura. Il numero di cammini esplode. Tronca a una profondità ragionevole (es. 8-10 generazioni) o a un contributo minimo.
- Non mischiare consanguineità **biologica** e **legale**: `adoptive_child`, `step_child` non contano per `F` e `R` di Wright. Vanno esclusi di default.
- Non usare una libreria di graph drawing esterna: il codebase ha già il pattern di layout scritti a mano.


## Cosa copre esattamente

| Aspetto FLEF | Vista A | Vista B | Vista C | Vista D |
|---|---|---|---|---|
| DAG senza duplicazione | ✓ | — | — | — |
| Proiezione albero con duplicazioni evidenziate | — | ✓ | — | — |
| Coefficiente di inbreeding `F` | ✓ (badge) | ✓ (badge) | ✓ | — |
| Coefficiente di relazione `R` | — | — | ✓ | ✓ |
| MRCA tra due individui | — | — | ✓ | ✓ |
| Anelli di consanguineità | ✓ | ✓ | — | — |
| Heatmap endogamia | ✓ | — | — | — |
| Report ordinabile | — | — | ✓ | — |
| Query "come sono imparentati?" | — | — | — | ✓ |
| Dimensione temporale | ✓ | ✓ | ✓ | ✓ |

Le quattro viste insieme coprono l'intero punto 5.


## Ordine di implementazione consigliato

1. `KinshipRelationType` + `ConsanguinityLevel` + `PedigreeEdgeType` + `PedigreeLayoutMode`
2. `KinshipPath` + `CommonAncestor` + `InbreedingResult` + `RelationshipResult`
3. `KinshipIndices` (una passata lineare, testabile subito)
4. `AncestorResolver` (BFS con memoizzazione, testabile senza UI)
5. `InbreedingCalculator` (testabile su piccole genealogie note)
6. `RelationshipCalculator` (testabile sui valori classici: fratelli → R=1/2, cugini primi → R=1/8)
7. `ConsanguineousUnionDetector`
8. `PedigreeNode` + `PedigreeEdge` + `PedigreeGraphModel` + `ConsanguineousUnion` + `ConsanguinityReport`
9. `PedigreeGraphService` + `ConsanguinityReportService` + `KinshipQueryService`
10. `PedigreeFilters`
11. `PedigreeGraphLayout` (Sugiyama, la parte più complessa)
12. `PedigreeViewport` + `CollapseDetector`
13. `PedigreeNodeRenderer` + `PedigreeEdgeRenderer` + `PedigreeGraphRenderer`
14. `PedigreeGraphController` + `PedigreeGraphInteractionHandler`
15. `PedigreeGraphToolbar` + `PedigreeGraphPanel`
16. `KinshipPathHighlighter` + `ConsanguinityOverlayRenderer` + `InbreedingHeatmapRenderer`
17. `AncestryFocusPanel`
18. `ConsanguinityReportPanel` + `ConsanguinityLegendPanel`
19. `KinshipQueryDialog`

I punti 1–10 sono logica pura, testabili senza Swing. Il punto 11 (Sugiyama) è il più delicato: può essere implementato in versione semplificata (layer assignment + barycenter, senza ottimizzazioni avanzate) e raffinato dopo.

Il punto 5 è il più complesso dei cinque: richiede algoritmi di graph drawing e calcoli di probabilità. Ma è anche quello che copre un aspetto che **nessuna** delle viste attuali tocca: la consanguineità e la struttura nascosta delle genealogie endogamiche.

---

In sintesi: albero biologico + Ego Network sono complementari ma parziali. Per sfruttare appieno FLEF servono viste specializzate aggiuntive: timeline, grafo sociale, mappa GIS, vista di gruppo e grafo di parentela estesa.
