# Viste e funzionalità dell'interfaccia: stato e backlog

Elenco di ciò che manca per sfruttare appieno il protocollo FLEF. Legenda: DONE = verificato nel codice, TODO = non
presente, PARZIALE = presente solo in parte o altrove. Per le viste specializzate vedere anche
`temporal/README.txt` (punto 1) e `social/README.txt` (punto 2).

Stato verificato sul codice. Revisione: DONE/TODO ricontrollati; per ogni TODO è indicato se vale la pena
farlo ("Utile", "Bassa priorità").


## TODO aperti, per priorità

Elenco di tutti e soli i TODO ancora aperti di questo file (i DONE e le scelte "non TODO" sono nelle sezioni sotto). Priorità: ALTA,
MEDIA, BASSA. Dove il testo originale la dichiarava ("Utile", "alta priorità", "Bassa priorità") è stata mantenuta; per gli altri è una
stima (marcata con "~"). I TODO specifici delle viste temporale e social stanno in `temporal/README.txt` e `social/README.txt`.

### Menu da verificarne la completezza
- Research
- Help | Keyboard Shortcuts, Help | Diagnostics

### ALTA
- Rete di parentela, ascendenza proiettata con duplicati collegati ("stesso individuo"): lo stesso antenato che compare più volte è
  marcato e collegato; `PedigreeCollapseDetector` dice già chi è duplicato. Partire dalla versione economica: evidenziare tutte le
  occorrenze al passaggio del mouse o al clic.

### MEDIA
- Ego network: filtro per tipo di relazione e toggle dei gruppi (valutare il riuso di `SocialFilters`).
- ~ Albero: filtri e espansione di rami (utile solo per alberi grandi).
- Export GEDCOM 5.5.1 / 7.0 (`ExportGedcom551Tool` / `ExportGedcom7Tool`, commentati in `FileToolRegistry`, classi inesistenti): oggi
  esistono solo import ed export FLEF. Va dichiarato cosa si perde (conclusioni, ricerca, contesto, privacy) e applicato
  `PrivacyExporter`. Sarà un plugin come gli importatori (`FileExporter`).
- Test di round-trip per l'export GEDCOM, una volta scritto.
- Export immagine (PNG/SVG) di una vista (`ExportImageTool` commentato, classe inesistente; per il PNG basta dipingere il pannello su
  un `BufferedImage`).
- Export diretto del dossier (basta un report che lo usi).
- Export CSV di sottoinsiemi (`ExportExcelTool` commentato; fare solo CSV).

### BASSA
- Rete di parentela, heatmap dell'endogamia: ascendenza a griglia (generazione x posizione) colorata per F o per numero di occorrenze
  dell'antenato; solo per un individuo e N generazioni, calcolo fuori EDT. Valore basso, il costo pesa sui file grandi.
- Rete di parentela, grafo DAG unificato con archi di unione: serve un layout per DAG con nodi di unione (il Sugiyama lo fa in parte);
  da fare solo come filtro sull'intorno di un individuo, mai sull'intero file, e solo se le altre viste non bastano.
- Consanguineità: rapporto di tutto il file e assistente lenti nel caso estremo (140 s, fuori EDT): sostituire gli insiemi di antenati di
  `ConsanguinityCalculator.ancestorsOf` con insiemi di bit su indici interi (serve solo con file chiusi su se stessi).
- ~ Consanguineità: soglie (1/256, 1/64, 1/16) non configurabili; nessuna distinzione fra parentela per sangue e per adozione/affido
  (esclusi); un genitore ignoto vale "non imparentato" (i valori sono limiti inferiori).
- Viste sulle fonti: source tree, galleria documenti con crop, citation network.
- Matrice comparativa degli attributi.
- Contesto: impact network (vista per bersaglio).
- Research Board: timeline delle attività.
- Dossier: filtri (solo tipo di evento e intervallo; il resto è già nelle sezioni).
- ~ Ricerca nei nomi di luoghi annidati.
- ~ Viste temporale e social (nascoste): riusare i filtri per tipo di relazione di `SocialFilters`; strip e timeline globale non
  compaiono sotto le due viste.
- Name usage classifications, if future experience shows a need to distinguish structured usage categories beyond the existing name types.
- Coverage information on sources, since archival coverage (e.g. a parish register covering baptisms from 1780 to 1820) is genealogically significant and currently resides only in notes.

### Assistente di ricerca (`ui/assistant`): "Still to do" di ogni assistente (tutti ~BASSA salvo dove indicato)
- Framework: ~MEDIA il record selezionato nel contesto (le viste non lo espongono ancora; serve anche a "Check Chronology" per un
  individuo); un filtro visibile per categoria; mostrare il record nella sua vista (oggi si aprono solo gli editor).
- Date (Historical Date Assistant): interprete del linguaggio naturale (calende/none/idi romane, "more veneto", "un'ora dopo il
  tramonto"), età con nascita nota per stimare la morte, anni di regno.
- Research Gap: "luogo noto ma manca l'atto di nascita"; storia delle residenze incompleta tra due censimenti; muri di mattoni (spetta
  al Planner); pesi non calibrati né configurabili e senza importanza dell'individuo/periodo/archivi; età per morte mancante (110/90) è
  una convenzione, chi non ha anno di nascita non è mai segnalato; fonti cercate solo sull'evento (non su partecipazione o
  individuo), battesimi e sepolture non sono eventi vitali; bozze in inglese, senza suggerimento di dove cercare, scadenza o
  collegamento ai compiti; soglie di aggregazione (25, 200) non configurabili.
- Source Citation: metadati dei media (EXIF, proprietà PDF, ISBN); repository citation intere (repository + locator) dal riferimento;
  locator per tipo di fonte (media_type); apprendere le abbreviazioni dell'utente; bozza dell'intera SourceCitation (extract, stance).
- Relationship (Assistant), per priorità:
  - MEDIA, più evidenza per il tipo: se l'evidenza non decide resta un finding generico. Soluzioni, una alla volta e solo come
    indizi dell'assistente (mai regole della validazione): officiante e luogo del matrimonio (il più affidabile, si parte da qui),
    qualificatori delle fonti, età dei genitori (con i limiti di Chronology), cognome del padre (debole, solo corrispondenza esatta,
    niente tabelle di nomi); matrimoni consuetudinari, affido e tutela non sono deducibili: solo una domanda al ricercatore.
- Chronology (Assistant): restano solo le convenzioni, come avvisi dell'assistente e mai della validazione: figli della stessa
  madre a meno di ~9 mesi (gemelli esclusi), età al matrimonio troppo bassa, coniuge più vecchio della durata della vita; eventi e
  attributi sovrapposti incompatibili (non dimostrabili: solo un avviso); date non leggibili come finding dell'assistente (la
  validazione le segnala già); "Check Chronology" per un individuo selezionato; limiti di età e gestazione configurabili; niente
  cache; messaggi solo in inglese. Già fatti nella validazione: `valid_to` prima di `valid_from` e il parto dopo la morte.
- Place: suggerimenti `successor_of` da nomi e date; date di validità delle relazioni (`valid_from` dopo `valid_to`, relazioni fuori
  dalla vita del luogo); luoghi senza fonte; luoghi senza coordinate o tipo (tipo dal nome: "Diocesi di", "Parrocchia di"); uso dei
  luoghi da record diversi dagli eventi; gerarchia dai nomi in più parti; genitore suggerito dai luoghi vicini.
- Conclusion: bozza costruita dal modello all'apertura della palette (un record cambiato o cancellato dopo è saltato, uno ora risolto
  non è rilevato), narrativa solo in inglese; l'editor non è un wizard, la bozza per un'ipotesi di identità risolve un solo bersaglio;
  il candidato preferito dovrebbe pesare i qualificatori di evidenza (e valere oltre nascite e morti); altre asserzioni a valore unico
  (luogo di nascita/morte, sepoltura, battesimo, sesso, tipo di relazione); attributi che cambiano nel tempo (residenza, occupazione,
  cittadinanza, religione) con `valid_from`/`valid_to` e `children_count`/`marriages_count` contro l'albero; relazioni (coppia
  figlio-genitore che è anche coppia di coniugi, matrimoni civile e religioso con date diverse, filiazione da coppie diverse in fonti
  diverse); ipotesi di identità: conclusione obsoleta (date di audit) e candidato non individuo; coerenza delle conclusioni (`preferred`
  fuori da RESOLVES, `proven` con una sola fonte, RESOLVES verso record inesistenti); modello letto intero a ogni apertura.
- Context: data e luogo della bozza di evento storico; migrazioni (individuo in due luoghi lontani) ed eventi storici del luogo di
  origine; convenzioni di nome contro i nomi dell'albero (patronimici); regole di età delle norme contro le età dei coniugi; eventi
  dell'individuo diversi da quelli dell'albero (attributi e residenze); catalogo importabile di eventi storici e norme; norme che si
  applicano a una conclusione o relazione; pesi non calibrati.
- Research Planner: luoghi e periodi (quale parrocchia, quale archivio, quali registri per gli anni: servono repository e luoghi
  dell'individuo); fonti già nell'albero (le citazioni dell'individuo oltre gli eventi); conclusioni della domanda; scadenza; ordine dei
  passi come piano con dipendenze; domande su luoghi e famiglie; pesi non calibrati né configurabili.
- Indicatore e ricalcolo (limiti noti, non lavori pianificati): nessuna opzione per spegnere la riapertura della palette, niente
  ignora "tutti di questo tipo / di questo individuo", i risultati ignorati sono per percorso del file e per utente; non testati: il
  clic reale che apre la palette, l'aspetto del pulsante, il riapertura con avviso, l'adattatore di `AppPreferences` e i tasti
  Ctrl+D / Ctrl+I.

### Test non verificati
- ESC sui dialog degli strumenti uno a uno (servono argomenti: è testato solo l'helper, e la scansione verifica che lo usino) e la
  pressione reale del tasto su un focus reale.


## Viste oggi raggiungibili

`ProjectionType` espone quattro proiezioni offerte all'utente: albero biologico (Ctrl+1), grafo Sugiyama (Ctrl+2), ego network (Ctrl+3),
chronomap (Ctrl+4). La proiezione temporale e il social network sono nello switcher (`ProjectionType.TEMPORAL` e
`SOCIAL_NETWORK`, `setProjection`, segnalibri) ma NASCOSTI per ora: niente bottone, voce di menu, Ctrl+5 / Ctrl+6 né aiuto
(su richiesta). Per riattivarli vedi il commento di `ProjectionType`.
Altre viste raggiungibili: dossier individuo/gruppo, Agorà, strip delle vite (`MultiLifespanStripPanel`),
timeline globale degli eventi (sotto l'ego network), gerarchia dei luoghi (`PlaceHierarchyPanel`), kinship dialog e
pedigree collapse (albero).

DONE `TemporalProjectionPanel` (punto 1) e `SocialNetworkPanel` (punto 2) sono collegati allo switcher: enum,
segnalibri, `TemporalAndSocialProjectionTest`; l'accesso dall'interfaccia (toolbar, menu, scorciatoie, aiuto) è tolto per ora. Vedi `temporal/README.txt` e
`social/README.txt`. Restano TODO: i filtri per tipo di relazione dell'ego network si possono riusare da `SocialFilters`; la
strip e la timeline globale non compaiono sotto le due nuove viste (hanno la loro asse).


## Punto 4 - Viste di gruppo (Group / Community View)

DONE: le tre viste A, C e B sono complete e raggiungibili dall'interfaccia (vedi sotto), con i TODO chiusi. Era il
lavoro più grosso. Esistono `GroupPanel`, `GroupData`, `GroupListener`, il dossier di gruppo (`GroupDossier*`), gli strumenti
di gestione dei gruppi e i package `grouptimeline` (Viste A e C) e `groupnetwork` (Vista B).

DONE Vista A (`grouptimeline`: `GroupTimelineBuilder`, `GroupTimelinePanel`; `GroupTimelineTest`). Non è una vista a sé: il builder
produce un `TemporalProjectionModel` e lo mostra `TemporalProjectionPanel` (nuovo `withModelSource`), quindi asse, zoom, pan,
tooltip e click sono quelli della proiezione temporale. Righe: il gruppo (eventi, attributi, contesto), i sottogruppi (`part_of`),
i membri (`group_member`), questi dal più antico. Sottogruppi e membri hanno un solo track `MEMBERSHIP` (nuovo in
`TemporalTrackType`) con una barra per appartenenza (da `valid_from` a `valid_to`), etichettata e colorata per ruolo
(`TemporalRoleColors`: stesso ruolo, stesso colore, senza maiuscole); chi ha più ruoli nel tempo ha più barre sulla stessa riga. Un'appartenenza
senza date non si scarta: barra su tutto l'asse, "(dates unknown)", e non allarga l'asse. Privacy applicata.
Scelta: nessuna banda di contesto (il contesto del gruppo sta nel suo track CONTEXT).
DONE Accesso dall'interfaccia (a differenza delle viste temporale e social, che restano nascoste): menu Gruppo > "Group Timeline…"
(`GroupTimelineTool`, nel registro `GroupToolRegistry.navigationTools`) e voce nel menu contestuale del gruppo nell'ego network
(`EgoNetworkGroupPopupMenuFactory`); dialogo `GroupViewDialog` (modale, ESC la chiude).
DONE Legenda dei ruoli (`GroupRoleLegend`, sotto le Viste A e C): un quadrato del colore e il nome per ogni ruolo presente nella
vista, in ordine alfabetico senza maiuscole (un ruolo scritto in più modi è uno, con la prima grafia), poi le appartenenze senza
ruolo; nascosta se non ci sono appartenenze. La palette dei colori è passata da 8 a 12, ma due ruoli possono ancora avere lo stesso
colore (il colore dipende dal ruolo, non dalla vista: stesso ruolo, stesso colore ovunque). Test in `GroupTimelineTest`.
DONE Filtro per ruolo dalla legenda (Viste A e C): ogni ruolo è una casella (quadrato pieno se mostrato, solo contorno se
nascosto) e togliere la spunta nasconde le barre di quel ruolo; "Show all" le rimette tutte. Una riga di membro o gruppo che resta
senza barre sparisce, la riga del gruppo o della persona (con eventi e attributi) resta; l'asse si ricalcola su ciò che rimane.
La legenda elenca sempre i ruoli dell'intera vista, anche quelli nascosti, per poterli riaccendere. Cambiando gruppo o persona il
filtro si azzera (altri ruoli). `GroupRoleLegend.filter` (statico, testato) e `withListener`; test in `GroupRoleFilterTest`.
Il filtro tiene il ruolo per chiave (senza maiuscole né spazi; `""` = senza ruolo).
DONE Stato `active/ended` (Viste A e C): `GroupTimelineBuilder.statusOf` legge `status` della relazione e lo mette nello span; nel
renderer (`TemporalSpanRenderer`, che prima ignorava lo stato) una barra `ended` è più tenue con contorno tratteggiato e l'etichetta
ha "(ended)"; `active` e `unknown` (anche tutti gli eventi e gli attributi) restano come prima, i punti non cambiano.
DONE Chiusura dei TODO delle Viste A e C:
- Filtro per anno (`GroupYearControl` sopra la vista, `GroupYearFilter`): una barra con date che non copre l'anno è tolta, una senza
  date o aperta da un lato resta; le righe rimaste senza barre spariscono (`GroupMembershipFilter`, lo stesso del filtro per ruolo);
  l'anno è gregoriano e resta cambiando gruppo o persona (è una scelta sul tempo), il filtro per ruolo no. Ordine: prima l'anno, poi i ruoli;
  la legenda elenca i ruoli delle barre dell'anno.
- Fine "oggi" (`GroupTimelineBuilder.ongoing`): un'appartenenza senza fine, non marcata `ended` e che non comincia dopo
  oggi è ancora in vigore, quindi la sua barra termina oggi davvero (la fine del `TemporalSpan` è oggi, non il bordo della finestra): con
  l'asse allontanato con lo zoom la barra non supera oggi (prima, una barra aperta o senza date arrivava al bordo della finestra, anche
  a 2900). Se ha un inizio l'etichetta aggiunge "ongoing"; senza alcuna data comincia al bordo sinistro della finestra (inizio
  sentinella) e dice "dates unknown". Un'appartenenza `ended` senza `valid_to` resta aperta (la fine non si sa). Ne segue che l'asse
  arriva a oggi da solo (la fine è una data) e che il filtro per un anno futuro toglie le barre ancora in corso. Solo nella Timeline di
  un gruppo e in quella delle giurisdizioni: la Vista C (carriera) tiene la regola "una fine mancante è la morte", perché legge la
  vita della persona.
- Asse sempre definito (`GroupTimelineBuilder.domain`): una appartenenza senza fine dura fino a oggi (o un anno dopo l'inizio se è nel
  futuro), una senza inizio comincia un secolo prima della fine, una vista senza alcuna data mostra l'ultimo secolo; prima, con le
  sole appartenenze aperte o senza date, la vista restava vuota.
- Timeline di un gruppo: oltre a sottogruppi e membri ci sono i gruppi di cui è parte (`part_of` come soggetto), una riga ciascuno,
  sotto la riga del gruppo.
- Il modello non si ricostruisce a ogni clic su un filtro (`GroupViewFilters`): si riusa quello costruito; si ricostruisce solo a
  `refresh()` o a un nuovo gruppo o persona.
DONE Correzione dello zoom (segnalata dall'uso): `TemporalAxis.spanToRect` chiamava `Math.clamp` con gli argomenti
scambiati e lanciava `IllegalArgumentException` appena una barra finiva oltre il bordo destro della finestra (con lo zoom succede
sempre), interrompendo il disegno: le barre non si ridimensionavano. Ora la barra è tagliata al bordo, una tutta fuori è omessa (prima
una tutta a sinistra diventava una barretta di 1 pixel sul bordo). Il difetto era nella proiezione temporale generale, non solo
nelle viste di gruppo. Inoltre: l'asse prosegue oltre l'inizio di una barra aperta (senza fine) di un quinto di ciò che è datato, almeno
un anno, altrimenti la barra restava fuori finestra; nelle viste di gruppo la barra degli strumenti nasconde "Show" (Bands,
Connections, Impacts) e "Entities", che non hanno effetto (`TemporalProjectionToolbar.setContentControlsVisible`). Test:
`GroupViewZoomTest` (asse, rotella Ctrl sul pannello vero in una finestra, disegno a ogni passo).
Scelta (non un TODO): la Vista C non propaga l'appartenenza lungo la gerarchia (chi è membro di un sottogruppo non risulta membro del
gruppo che lo contiene): sarebbe un'inferenza non scritta nei dati; i gruppi sono quelli dei `group_member` espliciti.

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

DONE Vista C, carriera di una persona nei gruppi (`IndividualCareerBuilder`, `IndividualCareerPanel`; `GroupTimelineTest`). Stessa
impostazione della A (modello temporale mostrato da `TemporalProjectionPanel`, stessi helper di `GroupTimelineBuilder`). Righe: la
persona (eventi, attributi, contesto: per leggere la carriera contro la vita), poi un gruppo per riga (`group_member`), il più
antico prima; più barre per più periodi o ruoli, colorate per ruolo (`TemporalRoleColors`); appartenenza senza date: barra su tutto
l'asse, "(dates unknown)". Se la persona non ha eventi o attributi datati né gruppi, la vista è vuota. Accesso: menu Individuo >
"Group Career…" (`IndividualCareerTool`, `IndividualToolRegistry.navigationTools`) e menu contestuali dell'individuo, nell'albero
(`EntityTreePopupMenuFactory`) e nell'ego network (`EgoNetworkIndividualPopupMenuFactory`).
Vista C: nessun TODO aperto (vedi la chiusura dei TODO sopra: filtro per anno, asse sempre definito).

DONE Vista B, rete dei gruppi (package `groupnetwork`: `GroupNetwork`, `GroupNetworkBuilder`, `GroupNetworkLayout`, `GroupNetworkPanel`;
`GroupNetworkTest`). Nodi = gruppi, archi `part_of` (linea spessa; il tutto sopra la parte) e `associate` (tratteggiata); badge col
numero di membri. È sempre filtrata: l'intorno di un gruppo, a "Levels" relazioni di distanza (2 di default, da 0 a 6) e, a
richiesta, per anno ("Year"): una relazione o appartenenza con date che non copre l'anno è esclusa, una senza date resta. Layout
`GroupNetworkLayout`, deterministico e non generico: livello dalle relazioni `part_of` (sotto il gruppo di cui è parte; un ciclo di
`part_of`, errore nei dati, non va in loop), un gruppo senza `part_of` prende il livello dell'associato più vicino al centro,
ordine sotto i genitori poi per nome, livelli centrati. Un clic seleziona un gruppo, un doppio clic lo mette al centro. Accesso:
menu Gruppo > "Group Network…" (`GroupNetworkTool`) e menu contestuale del gruppo nell'ego network; `GroupViewDialog.forNetwork`.
DONE Chiusura dei TODO della Vista B, `GroupNetworkFeaturesTest`:
- Legenda delle linee in fondo (linea spessa = parte di, tratteggiata = associato, badge = membri, riquadro giallo = centro).
- Zoom (pulsanti -, +, adatta; Ctrl + rotella, da 0.3 a 3) e pan (trascinamento, `ViewportPanSupport`); il clic e il tooltip
  tengono conto dello zoom.
- Etichette lunghe su due righe, spezzate allo spazio più vicino a metà; ciò che non entra è tagliato con "…" (il tooltip ha il nome intero).
- Filtro per tipo di relazione ("Part of", "Associated"): un tipo tolto non si disegna e non si segue nel calcolo dell'intorno.
- Il tooltip su un arco dice che cos'è ("X is part of Y" / "X is associated with Y"), il ruolo e le date (`GroupNetwork.Edge.dates`,
  da `DateService`); su un gruppo, nome, membri e identificativo.
- Casella "Members of the subgroups": il badge conta anche gli individui dei gruppi che ne sono parte, direttamente o no, ciascuno una
  volta (un ciclo non si segue); i `part_of` in effetto contano anche se il tipo è nascosto.
- Anno: stesso controllo delle Viste A e C (`GroupYearControl`), anche per il conteggio dei membri e per i `part_of`.
Scelte (non TODO): un gruppo parte di più gruppi sta un livello sotto il più basso dei suoi genitori; l'anno è gregoriano; un'appartenenza
non si propaga lungo la gerarchia (vedi sopra), il conteggio dei sottogruppi è un'opzione esplicita.

Da non fare: JTree/JTable come vista di esplorazione, duplicare l'indicizzazione di `TemporalIndices`, un motore di layout
generico di grafi, mostrare tutti i gruppi insieme.


## Punto 5 - Parentela estesa ed endogamia (Pedigree Collapse / Consanguinity)

PARZIALE (F, R, report e diagnostica fatti; restano le viste).

DONE
- Albero con layout Sugiyama (`individualtree/layout/sugiyama`).
- Rilevamento del pedigree collapse (`PedigreeCollapseDetector`, `PedigreeCollapseDialog`, `PedigreeCollapseController`).
- Calcolo della parentela fra due individui: `KinshipCalculator`, `KinshipResult` (catene fino all'antenato comune più
  recente), `KinshipDialog`, `KinshipCalculatorTool`.

- Coefficiente di inbreeding di Wright `F` e coefficiente di relazione `R`: package `consanguinity` (puro, senza UI):
  `AncestryGraph` (solo legami `child` / `biological_child`, al massimo due genitori, privacy applicata),
  `ConsanguinityCalculator` (`inbreeding`, `kinship`, `relationship`, `closestCommonAncestors`). Calcolo esatto con la
  ricorsione del coefficiente di parentela `phi` (tutti i cammini, anche l'inbreeding degli antenati; nessun troncamento a
  `(1/2)^8`, che non serve: i rami senza antenati in comune si scartano subito con gli insiemi di antenati in cache). Verificato
  su fratelli, genitore-figlio, zio-nipote, cugini primi (R = 1/2, 1/2, 1/4, 1/8), figli di fratelli (F = 1/4), di mezzi fratelli
  (1/8), di cugini primi (1/16), doppi cugini, e con l'inbreeding dell'antenato (R = 0.75 / sqrt(1.25)): `ConsanguinityCalculatorTest`.
- Report delle unioni consanguinee: `ConsanguineousUnionDetector` (coppie = unioni, fidanzamento escluso, + genitori dello stesso
  figlio anche senza unione; soglia di parentela 1/256 = cugini di terzo grado, di inbreeding 1/64) e `ConsanguinityReport`;
  testo in `ConsanguinityReportText`; menu Strumenti > "Consanguinity Report" (`ConsanguinityReportTool`,
  `ConsanguinityReportDialog`); nell'assistente di ricerca (categoria Relationships, `ui/assistant/consanguinity`): un
  avviso per le coppie di cugini primi o più vicini (kinship >= 1/16, convenzione), informazione per le altre, al massimo 25
  coppie una per una e poi una sintesi, una sintesi degli individui inbred. Mai modifiche ai dati: solo "possibilità da
  verificare". `ConsanguineousUnionDetectorTest`, `ConsanguinityCheckerTest`.

TODO
- DONE Allineare `KinshipResult` / `KinshipDialog`: il coefficiente `R` ora è quello esatto di `ConsanguinityCalculator`
  (`KinshipCalculator.relationshipCoefficient`, sugli antenati dei due individui, al massimo due genitori ciascuno; i genitori
  sono quelli del `ParentProvider`/`TreeService`, non solo biologici); il termine `(1/2)^(dA+dB)` per antenato resta solo come
  indicazione. `KinshipCalculatorTest`.
- DONE Misurato su un file grande (`LargeFileBenchmark`, a richiesta con `-Dfamilylegacy.benchmark=true`: 30000
  individui, 325000 record, una comunità chiusa che si sposa dentro il proprio villaggio, cioè il caso peggiore per `F` e `R`: 7200
  unioni e 11600 individui inbred). Il rapporto di tutto il file costa ~140-150 s (la parentela di due individui 0.1 s): su un file
  vero, meno chiuso, molto meno, ma è un lavoro da fuori EDT. Quindi: il dialogo `ConsanguinityReportTool` calcola in un
  `SwingWorker` dietro un `ProgressDialog` modale (mentre l'utente non può modificare il modello); l'ego network non calcola più il
  rapporto di tutto il file ma solo le coppie dell'ego (`ConsanguineousUnionDetector.unionsOf` / `couplesByIndividual`, un
  `ConsanguinityCalculator` tenuto fino a `invalidateIndices`): la prima apertura di un ego con partner è passata da 154 s sul
  thread dell'interfaccia a 0.9 s (il costo che resta è `AncestryGraph.of` e l'elenco delle coppie, lineare), le successive 0.1 s;
  `EgoNetworkConsanguinityTest` confronta ogni partner con il rapporto di tutto il file su un albero pieno di cugini sposati.
  TODO Il rapporto di tutto il file e l'assistente della consanguineità (140 s a ogni calcolo, fuori EDT) restano lenti nel caso
  estremo: `ConsanguinityCalculator.ancestorsOf` tiene per ogni individuo l'insieme (con mappa) di tutti gli antenati, che con molto
  collasso è la memoria e il tempo; insiemi di bit su indici interi lo renderebbero molto più leggero. Non fatto: serve solo con file
  chiusi su se stessi.
- TODO Le soglie (1/256, 1/64, 1/16 per l'avviso) non sono configurabili. Nessuna distinzione fra parentela per sangue e per
  adozione/affido (esclusi), e un genitore ignoto vale "non imparentato": i valori sono limiti inferiori.
- Rete di parentela (il pedigree collapse e il Sugiyama coprono già gran parte del bisogno), in quattro punti:
  1. DONE Anelli di consanguineità: menu Individuo > "Consanguinity Loops…" (`ConsanguinityLoopsTool`,
     `ConsanguinityLoopsDialog`, package `actions.individuals`) e menu contestuali dell'individuo (albero, ego network). Per un
     individuo con due genitori noti mostra perché è inbred: una riga per anello, cioè per antenato comune più vicino dei genitori
     (`ConsanguinityCalculator.closestCommonAncestors`) e per coppia di linee di discendenza che lo uniscono ai due genitori senza
     altri individui in comune (cammini di Wright); le linee sono scritte dall'antenato giù fino all'individuo, con generazioni e
     contributo `(1/2)^(n1+n2+1) (1+F(antenato))`, i più pesanti prima, al massimo 100 (`ConsanguinityLoops.MAX_LOOPS`, il dialogo dice
     se ce ne sono altri). Il coefficiente nell'intestazione è quello esatto del calcolatore: la somma delle righe può essere minore
     se contribuiscono anche antenati più lontani. Logica pura in `ConsanguinityLoops` (Domain), sola lettura; doppio clic o "Open
     ancestor" apre l'antenato. Test su figli di cugini primi, fratelli, zio-nipote, genitore-figlio, antenato inbred:
     `ConsanguinityLoopsTest`, `ConsanguinityLoopsDialogTest`. Non fatto (era nella proposta): l'evidenziazione degli anelli sul
     Sugiyama, che non ha un layout né un punto di aggancio per evidenziare una selezione di antenati.
  2. MEDIA Ascendenza con duplicati collegati (vedi la lista in cima).
  3. BASSA Heatmap dell'endogamia.
  4. BASSA Grafo DAG unificato con archi di unione.
- DONE Segnalazione delle unioni consanguinee nell'ego network: l'arco verso un partner imparentato con l'ego porta un badge
  "C" ambra (`EgoNetworkRenderer`), il tooltip del partner dà parentela, R e grado (`EgoNetworkLayoutBuilder`). Il rapporto
  `ConsanguineousUnionDetector.detect` è calcolato da `EgoNetworkService` alla prima apertura di un ego con partner e tenuto
  in cache fino a `invalidateIndices`; `EgoNode.getConsanguineousUnion`. `EgoNetworkConsanguinityTest`. Il calcolo è per ego (vedi
  sopra: non più sul rapporto di tutto il file); solo i partner con unione o figli in comune, non i fidanzamenti.

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
- Attributi: PARZIALE bande di validità nella strip; TODO carriera (Gantt: coperta dalla Vista C dei gruppi e dal
  `TemporalProjectionPanel` una volta collegato), TODO matrice comparativa (bassa priorità).
- Fonti: TODO source tree, galleria documenti con crop, citation network (esistono gli editor, `CitationAnalysisTool` e le
  gestioni di fonti/repository/media). Bassa priorità.
  DONE provenance delle citazioni: menu Fonte > "Provenance…" (`ProvenanceTool`, `ProvenanceDialog`, package `actions.provenance`: sta in `components` e non in `tools.sources` perché i menu
  contestuali lo usano, e `tools` -> `components` -> `tools` sarebbe un ciclo) e menu contestuali
  dell'individuo (albero, ego network). Una riga per citazione, con fonte (e repository che la conserva), locator, stance (una citazione
  che contraddice è in rosso), estratto ed evidenza scritta in testo. Dal record: le sue citazioni; per un individuo la casella "Include
  events, attributes and relationships" aggiunge quelle di eventi (partecipazioni), attributi e relazioni (off di default); per una
  fonte il verso opposto, tutto ciò che la cita. Logica pura in `ProvenanceService` (Domain, `sources`), privacy applicata, sola
  lettura; "Open record" / "Open source" aprono gli editor e la lista si rilegge. `ProvenanceTest`. Correzione fatta strada facendo:
  `SourceHelper.repositoryName` e `sourceTitle` passavano il nodo del nome invece del record a `extractPrimaryName` /
  `extractPrimaryTitle`, quindi il nome di un repository risultava sempre il suo identificatore (e un titolo con più figli
  pure). TODO (bassa priorità): provenance di un individuo a livello di dossier/report; evidenza come badge grafico (vedi sopra).
- Luoghi: DONE albero della gerarchia (`PlaceHierarchyPanel`, `PlaceHierarchyService`); DONE timeline delle giurisdizioni
  (`JurisdictionTimelineBuilder`, `JurisdictionTimelinePanel`, `JurisdictionTimelineTool`; `JurisdictionTimelineTest`). Stessa impostazione
  delle viste di gruppo: modello temporale mostrato da `TemporalProjectionPanel`, stessi filtri (`GroupViewFilters`: anno, contesto,
  legenda), stessi helper di `GroupTimelineBuilder`. Righe: il luogo (con il contesto storico), poi i luoghi di cui è parte o che ha
  succeduto (`place_relationship` con il luogo come soggetto), poi quelli collegati nell'altro verso (il luogo come bersaglio: barre
  "incoming"); una barra per relazione, da `valid_from` a `valid_to`, colorata e con etichetta per tipo (il tipo fa da "ruolo", quindi
  la legenda nasconde un tipo di giurisdizione); una relazione senza fine termina oggi ("ongoing" se ha un inizio, come nelle viste di
  gruppo: `GroupTimelineBuilder.ongoing`); una relazione senza date comincia al bordo sinistro e finisce oggi, "dates unknown". Il cambio di
  diocesi di una parrocchia sono due barre in due righe, una dopo l'altra. Accesso: menu Luogo > "Jurisdiction Timeline…" (il luogo
  selezionato, se la selezione è un luogo, altrimenti si sceglie con `RecordSelectionDialog`); `GroupViewDialog.forPlace`.
  Scelta (non TODO): i `place_relationship` hanno `valid_from`/`valid_to` ma non `status` né `role`, quindi nessuna barra "ended".
- Contesto: DONE vista ad albero (`ContextBoardDialog`); DONE timeline contestuale: le viste di gruppo (Group timeline e Group career)
  disegnano dietro le righe gli eventi storici e le norme culturali che si sovrappongono all'asse (`GroupTimelineBuilder.contextBands`),
  con il nome sulla banda, un tooltip (doppio clic apre l'editor) e la casella "Context" per nasconderle; non allargano l'asse.
  Senza filtro per luogo: una banda con luogo appare per chiunque. TODO impact network (bassa priorità).
- Meta-informazioni: DONE icone di privacy (`PrivacyBadge`); DONE tooltip di audit (creazione e ultima modifica) sul nome di
  individui e gruppi. Scelta (non TODO): l'evidenza (`EvidenceQualifiers`) non è nei tooltip dei box (andrebbe messa per ogni evento e
  diventerebbe illeggibile); si vede nel dossier (badge testuale `orig/prim/dir`) e in Provenance.


## Per coprire il protocollo FLEF

1. DONE Editor per `SourceRecord`, `RepositoryRecord`, citazioni e parti di documento (`SourceRecordDialog`,
   `RepositoryRecordDialog`, `SourceCitationDialog`, `MediaPartListPanel`, `ImageCropDialog`).
2. DONE Vista "Research": `ResearchBoardDialog` (menu Ricerca > "Research Board") mostra question -> activity -> task con le
   conclusioni accanto, filtro testuale, dettaglio del record e apertura dell'editor (doppio click); l'albero è costruito da
   `ResearchTreeBuilder` (testato). I record senza domanda stanno sotto "(no question)". DONE "New > Activity / Task / Conclusion of the
   selected question" apre il nuovo record con la domanda già collegata (come "Impact of the selected context" nella Context
   Board; `withQuestion` nei tre dialog). TODO Bassa priorità: timeline delle attività.
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
   (gli editor aperti esplicitamente mostrano sempre tutto; consistenza e menu restano non filtrati). Il filtro non è una
   misura di sicurezza: il file resta in chiaro.
8. DONE Validation report: `ValidateFileTool` + `ValidationReportDialog` con "Open record" per ogni violazione.
9. DONE Audit viewer: `AuditPanel` nei dialog dei record (creation e update).


## Per usabilità generale

1. DONE Ricerca globale: Ctrl+J (`JumpToIndividualDialog`) cerca individui e gruppi; Ctrl+F, menu Navigazione > "Find record"
   (`FindRecordDialog`, `FamilyLegacyFrame.openFindDialog`) cerca in tutti i tipi con una strategia di ricerca
   (`SearchService.searchableHandlers()`: 16 tipi, con i filtri propri di ciascuno, nel `RecordSelectionDialog` già esistente).
   Un individuo (e un gruppo nell'ego network) si mostra nelle viste, come Ctrl+J; ogni altro record si apre nel suo editor e, se
   salvato, le viste si ricostruiscono. `FindRecordTest`.
   DONE Ricerca nel testo di note e citazioni: casella "Search in notes and citations" del `RecordSelectionDialog`
   (quindi anche in Ctrl+F), `SearchMode.NOTES_AND_CITATIONS`. Con la casella accesa il testo cercato è il testo mostrato del record più
   quello delle sue note e citazioni (`AttachedTextExtractor`: le strutture `note` a qualunque profondità, di ciascuna titolo, testo e
   testo delle traduzioni, per un record NOTE il suo; le strutture `source`, di ciascuna il `locator` e il testo e le `remark` degli
   `extract`). Stesso matcher, quindi sottostringa / parola intera / fuzzy e il ranking per numero di token valgono anche qui.
   Si trova il record che possiede la nota o la citazione (non la nota): quelle di un altro record (gli eventi di un individuo) si
   trovano come record di quell'altro. Il risultato non dice dove c'è il testo (si apre l'editor e si guarda). Spenta di default.
   TODO Non cerca nei nomi di luoghi annidati (solo il testo che le strategie già confrontano).
2. DONE Cronologia di navigazione (`NavigationHistory`, Ctrl+Left / Ctrl+Right, "Cancella cronologia").
3. DONE Segnalibri / viste salvate (`bookmarks`, menu Segnalibri).
4. DONE Undo/redo applicativo: oggi Ctrl+Z / Ctrl+Y agiscono solo dentro ai dialog dei record (`UndoController` in
   `BaseRecordDialog`); le voci del menu Modifica non annullano le modifiche al modello.
5. DONE Pannello "Modifiche recenti" (menu Modifica, `RecentChangesDialog`): elenco non modale dei record toccati a ogni passo di
   `ModelHistory` (aggiunto/modificato/rimosso, ora, applicato/annullato); doppio clic apre l'editor del record.
6. DONE Confronto affiancato di due record (`RecordDiffDialog`, `MergeFieldComparisonDialog`).
7. TODO Bassa priorità: filtri sul dossier (sezioni, tipo di evento, intervallo, presenza di fonte). Se si fa, solo tipo di
   evento e intervallo; il resto è già nelle sezioni.
8. DONE Tooltip arricchiti: presenti in `IndividualPanel` (usato da albero e Sugiyama tramite `PartnersPanel`/`SiblingsPanel`),
   strip, timeline, ego network (layout); verificato su albero e Sugiyama. Resta commentato il tooltip dell'unione
   (`PartnersData.marriageTooltip`).
9. DONE Scorciatoie da tastiera: F2 modifica, Canc elimina, Ctrl+X sposta, Ctrl+Z/Y, Ctrl+1/2/3/4 cambio proiezione, Ctrl+L,
   Ctrl+T, Ctrl+J, Ctrl+Shift+A (assistente), F11, F1, frecce per navigare nel grafo (`SpatialNavigation`). DONE Ctrl+F (ricerca globale). Ctrl+Shift+S non è previsto (salvataggio automatico: scelta, non TODO).
10. DONE tooltip sui campi non ovvi di Confidence, EvidenceQualifiers, ContextImpact, Conclusion (preferred assertion),
   IdentityHypothesis, CulturalNorm, ResearchActivity (result, search_scope, ...), ResearchQuestion/Task, e "quando usare
   questo record?" sulla prima scheda dei dialog e nella lista dei tipi di `RecordSelectionDialog` (chiavi `record.usage.*`).
   Meccanismo: i `BoundComboBox` con prefisso i18n mostrano `<prefisso>.tooltip` sul campo, `<prefisso>.<codice>.tooltip`
   per ogni voce (e per quella selezionata); `GUIHelper.withTooltip` per gli altri campi, le etichette riprendono il tooltip
   del campo. Solo inglese.


## Per le viste grafiche

1. PARZIALE Albero: il collasso del pedigree c'è; TODO filtri/espansione di rami (Utile solo per alberi grandi).
2. TODO Utile: EgoNetwork, filtro per tipo di relazione e toggle gruppi (esiste `SocialFilters` nel social network: valutare
   se riusarlo).
3. DONE Lifespan strip: bande per secolo, evidenziazione delle vite sovrapposte all'hover, click su banda/marker.
4. DONE Chronomap (Ctrl+4, quarta proiezione): layer multipli (`ChronomapLayerManager`, mappe storiche), traiettorie (great circle) e marker animato.
   DONE filtro per tipo di evento (verificato e corretto): `ChronomapOverlayPainter.anchorsOf` è l'unico punto che applica
   il filtro, usato da marker 2D, globo 3D, clic, centratura e adattamento della vista (prima valeva solo per il disegno 2D); togliere
   la spunta a tutti i tipi non mostra più tutto ma nessun evento (restano gli attributi); il menu si ricostruisce dopo la geocodifica
   (nuovi tipi) tenendo i tipi già tolti. `EventTypeFilterTest`.
5. DONE Vista "Agorà" (`AgoraPanel`).


## Import/export

0. DONE Gli importatori sono plugin, come i formati dei report e il motore astronomico. L'SPI è `FileImporter` /
   `FileImporters` (Domain, package `importer`; la ricerca dei jar è in `util.PluginLoader`, usato anche da `ReportRenderers`); ogni
   formato è un modulo che produce un jar in `Core/plugins`: `ImporterGedcom` (`importer-gedcom.jar`, GEDCOM 5.5.x / 7.0, package
   `importer.gedcom`) e `ImporterGramps` (`importer-gramps.jar`, Gramps XML e pacchetti, package `importer.gramps`). Il menu File >
   Importa lo costruisce `FileToolRegistry.importTools()`: un `ImportFileTool` (lo stesso per tutti) per ogni importatore trovato, e
   per i formati noti senza jar (`FileImporters.KNOWN_IDS`) una voce `ImportUnavailableTool` disabilitata ("... (plugin not installed)"): l'import è possibile solo se il jar `importer-<id>.jar` c'è. Gramps usa gli
   helper del convertitore GEDCOM (identificatori, luoghi, deduplicazione), quindi ha bisogno anche di `importer-gedcom.jar` (stesso class
   loader). Per un nuovo formato: un modulo `Importer<Nome>` come questi, con `META-INF/services/...FileImporter` e un
   `importer-<id>.jar`. I test degli importatori stanno nei loro moduli; Core li ha come dipendenze di test (`--add-reads` nella
   configurazione di surefire, e in `surefire-opens.args` per l'IDE). `AuditStamp` (Domain) dà l'`audit` di un record creato a mano.
   TODO Esportare GEDCOM, quando si farà, sarà un plugin allo stesso modo (`FileExporter`).
1. PARZIALE Export dei report: `ReportRenderer` ha PDF, DOCX, HTML, Markdown e testo (antenati, discendenti, vita individuale,
   gruppi, fonti, ...); TODO Bassa priorità: export diretto del dossier (basta un report che lo usi).
2. TODO Utile: export immagine (PNG/SVG) di una vista (`ExportImageTool` è commentato in `FileToolRegistry` e la classe non
   esiste; per il PNG basta dipingere il pannello su un `BufferedImage`).
3. TODO Utile (alta priorità, interoperabilità): export GEDCOM (`ExportGedcom551Tool` / `ExportGedcom7Tool` sono commentati in
   `FileToolRegistry` e le classi non esistono); esiste solo l'import (GEDCOM, Gramps XML) e l'export FLEF. Va dichiarato cosa
   si perde (conclusioni, ricerca, contesto, privacy) e applicato `PrivacyExporter`.
4. TODO Bassa priorità: export CSV di sottoinsiemi (`ExportExcelTool` è commentato; fare solo CSV).


## Priorità

1. DONE Collegare `TemporalProjectionPanel` e `SocialNetworkPanel` allo switcher.
2. DONE Ricerca globale (Ctrl+F, "Find record") su tutti i tipi di record, riusando `components/searches`.
3. Export GEDCOM (poi PNG di una vista).
4. DONE Calcolo di F/R e report delle unioni consanguinee.
5. Filtri dell'ego network.
6. Viste di gruppo (Viste A, C e B DONE, con i TODO chiusi).

(Privacy enforcement e undo/redo applicativo, prima in cima, sono DONE.)


## Altri TODO emersi dalla revisione

- DONE Il dettaglio dell'assistente di ricerca (sezione "TODO Improvements", ~430 righe) è stato spostato in
  `ui/assistant/README.txt`; i suoi "Still to do" restano riassunti nella sezione "TODO aperti" in cima.
- DONE Lettura del modello da parte degli assistenti fuori dall'EDT mentre l'utente può modificarlo: un unico
  snapshot condiviso. Il modello viene modificato sul posto da molti punti, quindi un lock non si può imporre: `ModelHistory.readOnlyView()`
  dà una copia che nessuno cambia (i record non modificati sono quelli degli snapshot della cronologia, che non sono mai cambiati;
  costa quanto è cambiato, non quanto è grande, e finché il modello non cambia è la stessa copia per tutti; non registra passi della
  cronologia). `AssistantContext.forReading()` (da chiamare sull'EDT prima di uscirne) dà il contesto con la copia, usato per calcolare i
  risultati (`AssistantIndicator`, `ResearchAssistantPalette`, `ResearchAssistantService.reopenIfChanged`); le azioni ricevono sempre il
  contesto con il modello vero (un risultato dice i record con l'identificatore, che le azioni ritrovano nel modello vero). Il
  `PrivacyService` tiene fino a 3 resolver (il modello vero e le copie), altrimenti i due thread si butterebbero via la cache a ogni
  chiamata. Provato da `ModelHistoryReadOnlyViewTest` e `ReadingSnapshotTest` (una modifica dopo la copia non arriva a chi legge).
  Costo misurato (30000 individui): la prima copia 0.9 s (sull'EDT, una volta), quella dopo una modifica quanto è cambiato.
  Non cambia: gli assistenti sono lenti su file molto grandi (sulla stessa tabella: cronologia 40-65 s, relazioni 55-60 s,
  consanguineità 140 s, identità 30 s, gli altri sotto 5 s), ma stanno fuori dall'EDT con il ritmo dell'indicatore (4 volte la durata).
- DONE Un'unica definizione di "conclusione risolta": `io.model.readers.ConclusionResolution` (valutazione che risolve almeno due
  record in conflitto e prende posizione: preferred fra essi, oppure proof status `conflicting_evidence` / `disproven`). La usano
  `ConclusionChecker` (assistente) e `ProtocolCleanup` (merger); prima il merger accettava solo `preferred`, quindi avvisava
  di conflitti che l'assistente considerava risolti. `ConclusionResolutionTest`; `FLEFMergerTest` e `ConclusionCheckerTest`
  invariati.
- TODO Test di round-trip per l'export GEDCOM, una volta scritto.


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


## One validation: the grammar and the structure (Tools > Validate File)

- `CheckConsistencyTool` (never in the Tools menu until it was added by hand) was merged into `ValidateFileTool`: two controls for
  "is the file right" told the user to run both. `FileValidation.run(validator, model)` = the grammar (`FLEFValidator`, which stops at the
  first stage that fails) + the structural checks of `ConsistencyService`, that always run (they only read the records they
  understand; a failure of them is a warning, not a crash). `ConsistencyCheckDialog` and its texts are gone.
- `FLEFValidator.ValidationError` has a severity (`ERROR`, `WARNING`; the two-argument constructor is an error). The report dialog has
  a severity column, colors it, and says "1 error, 2 warnings"; a file with warnings only is valid.
- Improved checks: a circle of parentage is reported ONCE with all its members (strongly connected groups, Tarjan without recursion:
  60 000 ancestors in a line do not overflow the stack), where before every member got its own error (the `reported` set was never filled);
  a relationship with itself is found for every kind of record (it was individuals only) and for the place relationships; an
  individual is unnamed if its name is empty, not only if it has no `name` child; the texts are in the bundle. The grammar does NOT
  require a name (an individual without one passed the validation), so the warning is not a repeat.
  More than two biological parents is a check of the validation too (error, once for the individual, the parents named): that the
  Conclusion Assistant says it as well does not matter, the file is invalid either way. Two biological mothers or two biological fathers
  is an error too: the two parents of an individual both recorded with the same sex. A missing or `unknown` sex is not checked (it
  would be the opposite of the other parent, or it cannot be said); a wrong sex is not told from a wrong parentage, so the message
  says either may be wrong. With more than two parents only the number is reported. `FileValidationTest`. Not done: the cycles of the
  hierarchy of the groups: now done (below).
- More checks, done: a conclusion whose `preferred` is not among its `resolves` is an error (the protocol says MUST;
  the grammar did not enforce `require preferred member_of resolves`), a `disproven` conclusion with fewer than two `resolves`, or with
  a `preferred`, is a warning (SHOULD); a relationship type that is not valid between the kinds of its two records is an error (the
  generic `spouse` is valid between individuals although the editor does not offer it); circles in the hierarchy of the groups
  (`part_of`) and of the places (the five `*_part_of` types together) are errors, in the succession of the places a warning, each once;
  the same relationship twice is a warning (a couple or two associates from both sides are one; a child and a parent are not);
  `valid_to` certainly before `valid_from` is an error for relationships and place relationships. The last one used to compare the two
  dates as text, which was wrong for dates written in two ways: it normalizes them now and compares the days (`DateRange`, moved from
  the chronology assistant to `io.model.readers.date`, public). Each group of circles or of duplicates is one issue naming the
  records. `FileValidationTest`.

- Validate File, DONE: an event with no participants is a warning, and so is a birth, baptism, christening, death,
  burial or cremation whose participants all have a role and none is `principal` (a participant with no role says nothing: the role is
  optional; a marriage is not judged, the other spouse may be unknown); a media whose file is not found is a warning
  (`MediaFiles`, `FileValidation.run(validator, model, baseDirectory)`): a web address is not looked for, a relative path is looked for
  under the base directory of the user (Appearance > Media paths, or the folder of the open file) and with none it is not judged. It
  reads the filesystem, so it runs where the validation already runs, away from the EDT (the tool and the Re-run). Not done: a check
  that a file is of the kind the record says (an image that is a PDF), and the cost on a network drive is not measured.
  `FileValidationTest`.

- Validation report grouped by kind, not by severity: a file is corrected one kind of problem at a time, and the
  kinds that make hundreds of rows (the media not found) must not bury the others; the severity is a column, the order of the groups
  (the ones with errors first, then warnings only, the smaller before the larger in each) and a filter (errors only / warnings only).
  `FLEFValidator.ValidationError` has a `kind` (a stable key; empty if not known), `ConsistencyService.Issue` and `MediaFiles` set it
  (the end of the key of the message: `cycle`, `parents`, `media_missing`...), the relationship dates that end before they begin
  have `validity_dates`. What the grammar finds has no kind: the report groups it by the type of the record, or "the file as a
  whole" if it is about none (an approximation: the messages are free text). The line of a group closes or opens it (arrow, count).
  The text of each kind is `text.validate.kind.<kind>`; a kind with no text is shown by its key. `ValidationGroupingTest`.

- Validate File, the dates, `DateConsistency`: errors that hold for EVERY day the dates may stand for (`DateRange`:
  year 366 days, decade 3653, century 36525, an approximate date with its margin or two years, a bounded one between its days, an
  open end): a death before the birth; an event after the death or before the birth of a participant (a birth and a death of the
  individual as principal are the reference, not judged; burial, cremation, probate, deed... may follow the death); a child before a
  biological parent; born after the death of the mother, or more than 300 days after the death of the father (or of a parent of
  unknown sex); a union before a partner's birth or after a death. With several births the days they all cover are used; a date that
  cannot be read is not judged. The ages and the length of a life are conventions: warnings of the Chronology Assistant, not here.
  The rules of the provable subset are written twice (here and in `ChronologyChecker`, which makes Diagnostics with its own data
  records); only `DateRange` is shared. A year is judged conservatively: the last day of a year of 366 days overlaps the first of the
  next, so a death in 1849 is not 'certainly before' a birth in 1850 (the same limit as the assistant). The empty
  `FLEFValidator.validateIndividualDates` is removed. `DateConsistencyTest`.
- The groups of the report are CLOSED when it opens (the kinds and their sizes first); what the user opens stays open on Re-run.

- Validate File opens its report AT ONCE, with a progress bar (before, nothing was seen until the validation was done, and only then the
  dialog appeared). `ValidationReportDialog.validating(owner, model, viewSupplier, Validation)`: the dialog starts the validation in the
  background, shows the bar and `Validating...`, disables Re-run, and fills the groups when done; closing the dialog cancels the
  worker (interrupts the thread: the validation checks it and stops). Progress: `FileValidation.run(validator, model, base,
  DoubleConsumer)`: the grammar 85 % (the schema 70 % of it by records, the integrity 15 %, the business rules the rest), the structural
  checks and the files of the media the other 15 %; never goes back. `FLEFValidator.validateAllStructured(model, progress)`,
  `MediaFiles.missing(model, base, progress)`. Not done: the structural checks of the records (`ConsistencyService.check`) are one step,
  not by record (they are fast next to the grammar). `ValidationProgressTest`.

- Validate File, the dates that cannot be read: `DateNormalizer.problem` says why `normalize` gave null, which it did
  both for a date that is wrong (not valid in the notation of its calendar: `date_invalid`) and for one whose calendar needs the
  Ephemeris plugin, missing (`date_not_checked`, not a problem of the file). Both are warnings, in two groups of the report, for the
  dates of the events and of the relationships (and place relationships). A calendar code that does not exist is read as the
  Gregorian (`CalendarType.fromCode`), so it is not told. Not done: the dates of the attributes, and the same warnings in the
  Chronology Assistant. `DateConsistencyTest`.

## Progress of the long calculations

- Detect Duplicates and the Research Assistant tell how far they are (a progress bar and the percentage), so that the user knows when
  the data will come. `DuplicateFinderService.find(model, options, DoubleConsumer)`: the first 30 % is the description of each
  individual (by number), the rest the comparison of the pairs of the blocks (by number of pairs, known beforehand); never goes back;
  an interrupted search stops and gives nothing (a newer one replaces it). The dialog uses `SwingWorker.setProgress`.
  `ResearchAssistantService.diagnose(context, DoubleConsumer)`: every provider is an equal share, and a provider that can tell
  overrides `DiagnosticProvider.diagnose(context, progress)` (the identity of the individuals does, being the slowest); the palette
  has a determinate bar of 1000 steps. `ProgressTest`. Not covered: the other providers, that are done when they return, so the bar
  can wait on one that is slow (the others of the same size are fast); the indicator of the window, that recomputes in the
  background, has no bar.

## Consanguinity report: every reference is a link

- `ConsanguinityReportText.segments` writes the report as pieces of text, some standing for a record (the partners, the common
  ancestors, the unions, the children, the inbred individuals and their identifiers); `format` is their concatenation, unchanged.
  `ConsanguinityReportDialog` shows them in a `JTextPane` (blue, underlined, hand cursor) and a click opens the editor of the record
  (`ToolDialogs.openRecordEditor`), then the text is written again. A record that is not in the model is not a link. The names of the
  partners and of the inbred individuals are those of the report (computed once); those of the ancestors are read again.
  `ConsanguinityReportLinksTest` (a real click opens `IndividualRecordDialog`).

## Modal dialogs: centered, and not taller than the screen

- Every modal dialog opens centered over its owner (over the screen if it has none, or the owner is not on the screen), and is brought to
  85 % of the height and 95 % of the width of the usable area of the screen at most, so that the rest scrolls: `DialogPlacement`
  (installed once by the main frame, like `DialogEscapeSupport`), which acts when the dialog is opened. It does not depend on each
  constructor: a dialog that is packed, or whose minimum size is set, after `setLocationRelativeTo` ended with its corner where the
  center should be (the consanguinity report did, and a text area with all the text asks for the height of all of it). The pure
  calculation is tested (`DialogPlacementTest`), and so are two real dialogs, one of them the report with 300 couples.
- A dialog that wants a size of its own sets it (the report: a text of 760 x 380, the rest scrolls); the screen is only the limit.

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
