# Viste e funzionalità dell'interfaccia: backlog

Elenco di ciò che manca per sfruttare appieno il protocollo FLEF. Legenda: TODO = non presente, PARZIALE = presente solo in parte o
altrove, "Scelta" / "Da non fare" / SCARTATO = decisione di non fare, con il motivo. Ciò che è già fatto non è elencato (vedi il codice e
l'aiuto in `Core/src/main/resources/help`). Per le viste specializzate vedere anche `temporal/README.txt` (punto 1) e
`social/README.txt` (punto 2). Per ogni TODO è indicato se vale la pena farlo ("Utile", "Bassa priorità").


## TODO aperti, per priorità

Elenco di tutti e soli i TODO ancora aperti di questo file (le scelte "non TODO" sono nelle sezioni sotto). Priorità: ALTA,
MEDIA, BASSA. I TODO specifici delle viste temporale e social stanno in `temporal/README.txt` e `social/README.txt`.

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
Mantenuti, ma da fare solo se emerge un bisogno concreto (revisione del 2026-11-09), dal più utile al meno:
- Report: lacune per generazione; Calendario degli anniversari.
- Dossier: filtri (solo tipo di evento e intervallo; il resto è già nelle sezioni).
- Consanguineità: rapporto di tutto il file e assistente lenti nel caso estremo (140 s, fuori EDT): sostituire gli insiemi di antenati di
  `ConsanguinityCalculator.ancestorsOf` (oggi `Set<String>`) con insiemi di bit su indici interi. Serve solo con file chiusi su se
  stessi, che un file reale non raggiunge: farlo solo se qualcuno incontra davvero i 140 s.

SCARTATI (revisione del 2026-11-09; non sono più TODO, il motivo è scritto per non riproporli):
- Heatmap dell'endogamia: grafico da pubblicazione scientifica, non risponde a una domanda del genealogista; F per individuo c'è già
  (anelli di consanguineità, kinship); il costo pesa sui file grandi.
- Grafo DAG unificato con archi di unione: layout costoso (nodi di unione), il Sugiyama e il pedigree collapse coprono il caso.
- Soglie di consanguineità configurabili (1/256, 1/64, 1/16): sono soglie standard (cugini di terzo grado, di primo grado); renderle
  configurabili aggiunge confronti non omogenei fra file.
- Galleria documenti con crop e citation network: già serviti da Provenance, dai report e dal Research Board; la galleria ha il costo
  maggiore (selezione di regioni sull'immagine).
- Contesto, impact network (vista per bersaglio): vista di lusso, la Context Board mostra già gli impatti di ciascun contesto.
- Research Board, timeline delle attività: ha senso solo se le attività hanno date coerenti; vista di lusso.
- Viste temporale e social (nascoste), filtri per tipo di relazione di `SocialFilters` e strip/timeline globale: lavoro speso su viste
  che nessuno vede. Prima si decide se le due viste restano o spariscono; i filtri dell'ego network (MEDIA, sopra) sono un'altra cosa.

### Assistente di ricerca (`ui/assistant`): "Still to do" di ogni assistente (revisione del 2026-11-09)
Criterio: si tiene ciò che toglie falsi positivi, aggiunge un controllo che un genealogista userebbe davvero o costa poco; si scarta ciò
che richiede tabelle da mantenere, dati storici che non abbiamo, o aggiunge rumore. Il dettaglio per assistente è in
`ui/assistant/README.txt`; i punti scartati sono elencati sotto, con il motivo.

BASSA (in ordine di valore, il più utile per primo; revisione del 2026-11-10)
- Research Planner: luoghi e periodi (quale parrocchia, quale archivio, quali registri esistono per gli anni; servono repository e luoghi
  dell'individuo: è il passo che darebbe più valore, ma dipende dai dati, quindi si fa quando i controlli sui luoghi sono in uso e i file
  reali hanno repository); conclusioni della domanda (senza un'azione concreta che ne derivi, per ultimo).
- Source Citation: citazioni di repository intere (repository + locator) ricavate dal riferimento. Solo come proposta all'utente, mai
  applicata da sola: con testo libero l'euristica è fragile; prima si verifica quanti riferimenti reali hanno una forma regolare.
- Context: data e luogo della bozza di evento storico (se la bozza ha già un contesto, data e luogo si ricavano dall'individuo: farlo
  solo se l'utente se li ritrova a copiare a mano).
- Place: tipo del luogo dal nome ("Diocesi di", "Parrocchia di"): vale per una lingua sola e, con la regola sulle stringhe di
  grammatica, diventa una lista di prefissi per lingua da mantenere (via i18n); sbaglia in silenzio, il guadagno è piccolo.
- Date: anni di regno (servono tabelle da fonte autorevole, vedi la regola sui calendari: molto lavoro di ricerca per un caso d'uso raro).

LIMITI NOTI, non lavori pianificati
- Convenzioni dichiarate: età 110/90 per la morte mancante (chi non ha anno di nascita non è mai segnalato: senza dati non c'è
  nulla da dire), soglie 25 e 200 dell'aggregazione, pesi di utilità non calibrati (senza ricerche reali su cui tararli).
- Bozza della conclusione costruita all'apertura della palette (un record cambiato dopo è saltato) e modello letto intero a ogni
  apertura; la bozza di un'ipotesi di identità risolve un solo bersaglio; l'editor non è un wizard.
- Indicatore e ricalcolo: nessuna opzione per spegnere la riapertura della palette; i risultati ignorati sono per percorso del file e
  per utente; non testati: il clic reale che apre la palette, l'aspetto del pulsante, la riapertura con avviso, l'adattatore di
  `AppPreferences` e i tasti Ctrl+D / Ctrl+I.

SCARTATI (revisione del 2026-11-09; il motivo è scritto per non riproporli)
- Framework: filtro visibile per categoria (scrivere il nome della categoria nella ricerca già filtra); mostrare il record nella sua
  vista (gli editor bastano, e le viste non hanno un'API di selezione).
- Date: età con nascita nota per stimare la morte (è aritmetica che l'utente fa da solo; l'età alla morte che stima la nascita c'è già).
- Research Gap: storia delle residenze incompleta tra due censimenti (convenzione senza base, solo rumore); muri di mattoni (spetta al
  Planner, già fatto); battesimi e sepolture come eventi vitali (sono sostituti, non eventi); pesi configurabili e importanza
  dell'individuo, del periodo, degli archivi (non c'è dato per tararli); bozze con suggerimento di dove cercare, scadenza e
  collegamento ai compiti (è il lavoro del Planner); soglie di aggregazione configurabili.
- Source Citation: metadati dei media (EXIF, PDF, ISBN: librerie e manutenzione per poco valore); locator per tipo di fonte (tabella da
  mantenere); apprendere le abbreviazioni dell'utente (stato nascosto, comportamento imprevedibile); bozza dell'intera SourceCitation
  (extract e stance sono giudizio del ricercatore).
- Relationship: indizi dai qualificatori delle fonti, dall'età dei genitori e dal cognome del padre (deboli o già coperti da Chronology;
  il cognome richiede tabelle di nomi); matrimoni consuetudinari, affido e tutela (non deducibili da nulla nel file).
- Chronology: coniuge più vecchio della durata della vita (caso raro); eventi e attributi sovrapposti incompatibili (non dimostrabili,
  solo rumore); date non leggibili come finding (la validazione le segnala già); limiti di età e gestazione configurabili; cache;
  messaggi tradotti (vedi sotto).
- Place: suggerimenti `successor_of` da nomi e date (servono dati storici sulle giurisdizioni); luoghi senza coordinate (rumore: pochi
  luoghi storici le hanno); uso dei luoghi da record diversi dagli eventi; gerarchia dai nomi in più parti; genitore suggerito dai
  luoghi vicini (indovinare con le coordinate è fragile).
- Conclusion: attributi che cambiano nel tempo (residenza, occupazione, cittadinanza, religione) e `children_count`/`marriages_count`
  contro l'albero (distinguere un cambiamento da un conflitto non si decide da qui); matrimoni civile e religioso con date diverse e
  filiazione da coppie diverse in fonti diverse (casi legittimi); ipotesi di identità con conclusione obsoleta (le date di audit non sono
  un segnale affidabile) e candidato non individuo (già impedito dall'editor); `proven` con una sola fonte (può essere legittimo).
- Context: migrazioni e eventi storici del luogo di origine; convenzioni di nome contro i nomi dell'albero (patronimici: tabelle);
  regole di età delle norme contro le età dei coniugi; eventi dell'individuo diversi da quelli dell'albero; catalogo importabile di
  eventi storici e norme (dati da curare e licenze); norme che si applicano a una conclusione o relazione; pesi non calibrati.
- Research Planner: scadenza; ordine dei passi come piano con dipendenze (un piano da gestire, non un suggerimento); domande su luoghi e
  famiglie; pesi configurabili.
- Indicatore: "ignora tutti di questo tipo / di questo individuo" (nasconde problemi veri).
- Messaggi e bozze solo in inglese (Gap, Chronology, Conclusion, Identity): è una scelta di tutta l'applicazione finché il bundle della
  UI non ha file per lingua; non è un TODO dei singoli assistenti.


## Viste oggi raggiungibili

`ProjectionType` espone quattro proiezioni offerte all'utente: albero biologico (Ctrl+1), grafo Sugiyama (Ctrl+2), ego network (Ctrl+3),
chronomap (Ctrl+4). La proiezione temporale e il social network sono nello switcher (`ProjectionType.TEMPORAL` e
`SOCIAL_NETWORK`, `setProjection`, segnalibri) ma NASCOSTI per ora: niente bottone, voce di menu, Ctrl+5 / Ctrl+6 né aiuto
(su richiesta). Per riattivarli vedi il commento di `ProjectionType`.
Altre viste raggiungibili: dossier individuo/gruppo, Agorà, strip delle vite (`MultiLifespanStripPanel`),
timeline globale degli eventi (sotto l'ego network), gerarchia dei luoghi (`PlaceHierarchyPanel`), kinship dialog e
pedigree collapse (albero).


## Punto 4 - Viste di gruppo: scelte (non TODO)

Le viste di gruppo (Group Timeline, Group Career, Group Network, Jurisdiction Timeline) sono complete: qui restano solo le scelte.
- La Vista C (carriera) non propaga l'appartenenza lungo la gerarchia (chi è membro di un sottogruppo non risulta membro del gruppo che
  lo contiene): sarebbe un'inferenza non scritta nei dati; i gruppi sono quelli dei `group_member` espliciti.
- Nella carriera una fine mancante è la morte (la vista legge la vita della persona); nella Timeline di un gruppo e in quella delle
  giurisdizioni un'appartenenza senza fine, non marcata `ended`, dura fino a oggi.
- Vista B (rete): un gruppo parte di più gruppi sta un livello sotto il più basso dei suoi genitori; l'anno è gregoriano; il conteggio dei
  membri dei sottogruppi è un'opzione esplicita.
- Da non fare: JTree/JTable come vista di esplorazione, duplicare l'indicizzazione di `TemporalIndices`, un motore di layout generico di
  grafi, mostrare tutti i gruppi insieme.


## Punto 5 - Parentela estesa ed endogamia (Pedigree Collapse / Consanguinity)

PARZIALE (F, R, kinship, anelli, report e diagnostica fatti; restano le viste).

TODO aperti:
- MEDIA Ascendenza con duplicati collegati (la lista in cima non la riporta).
- Anelli di consanguineità: l'evidenziazione degli anelli sul Sugiyama non è fatta (il layout non ha un punto di aggancio per evidenziare
  una selezione di antenati).
- Solo se serve: nessuna distinzione fra parentela per sangue e per adozione/affido (esclusi).
- Il rapporto di tutto il file e l'assistente della consanguineità sono lenti nel caso estremo (BASSA, in cima).

Indicazioni di progetto (ancora valide):
- Per `F` e `R` contano solo i legami `biological_child`; adozione, affido, tutela e figliastri sono esclusi.
- Mai mostrare l'intero DAG: filtrare per individuo focale, profondità o coefficiente. Nessuna libreria esterna di graph drawing.


## Visualizzazione sintetica di eventi, attributi, fonti, luoghi, contesto (idee)

- Attributi: PARZIALE bande di validità nella strip; TODO carriera (Gantt: coperta dalla Vista C dei gruppi e dal
  `TemporalProjectionPanel` una volta collegato), TODO matrice comparativa (solo se emerge un bisogno: vedi BASSA in cima).
- Fonti: la copertura archivistica (es. un registro con i battesimi 1780-1820) sta nelle note, scelta del 2026-11-09: nessun campo
  `coverage` / `coverage_scope` nel protocollo FLEF. Dove scriverla: `SourceRecord.note`, `RepositoryRecord.note`,
  `RepositoryCitation.remark` ("Baptisms 1780-1820; volumes for 1794-1796 lost.", "Entries 1-56 missing."). Perché: la realtà
  archivistica non è un intervallo (anni mancanti dentro il periodo, un volume perso, solo una frazione, pagine che partono dalla
  voce 57, registri non indicizzati), e un modello strutturato (luogo, da, a, mancanti, inclusi, esclusi) diventa subito complicato e
  resta incompleto. Da riconsiderare solo se emerge un caso ricorrente che le note non esprimono e che trarrebbe vantaggio
  dall'elaborazione automatica (ricerca, filtri, analisi di completezza, Research Gap). TODO source tree (solo se serve), bassa
  priorità. TODO (bassa priorità): provenance di un individuo a livello di dossier/report; evidenza come badge grafico.
- Luoghi: scelta (non TODO): i `place_relationship` hanno `valid_from`/`valid_to` ma non `status` né `role`, quindi nessuna barra "ended".
- Meta-informazioni: scelta (non TODO): l'evidenza (`EvidenceQualifiers`) non è nei tooltip dei box (andrebbe messa per ogni evento e
  diventerebbe illeggibile); si vede nel dossier (badge testuale `orig/prim/dir`) e in Provenance.


## Privacy: scelte (non TODO)

- Il filtro di privacy non è una misura di sicurezza: il file resta in chiaro. Gli editor aperti esplicitamente mostrano sempre tutto,
  la consistenza e i menu di gestione restano non filtrati: per gestire i record nascosti si porta la policy su "Show All".


## Per usabilità generale

1. Ricerca globale: i luoghi si cercano anche dai nomi dei luoghi che li contengono; gli altri record che puntano a un luogo non ancora
   (solo il testo che le strategie già confrontano; gli eventi sì, dal nome completo). Con "Search in notes and citations" il risultato non
   dice dove c'è il testo (si apre l'editor e si guarda).
2. TODO Bassa priorità: filtri sul dossier (sezioni, tipo di evento, intervallo, presenza di fonte). Se si fa, solo tipo di evento e
   intervallo; il resto è già nelle sezioni.


## Per le viste grafiche

1. PARZIALE Albero: il collasso del pedigree c'è; TODO filtri/espansione di rami (Utile solo per alberi grandi).
2. TODO Utile: EgoNetwork, filtro per tipo di relazione e toggle gruppi (esiste `SocialFilters` nel social network: valutare
   se riusarlo).


## Import/export

0. TODO Esportare GEDCOM, quando si farà, sarà un plugin allo stesso modo degli importatori (`FileExporter`; SPI `FileImporter` /
   `FileImporters` in Domain, jar `importer-<id>.jar` in `Core/plugins`).
1. PARZIALE Export dei report: `ReportRenderer` ha PDF, DOCX, HTML, Markdown e testo (antenati, discendenti, vita individuale,
   gruppi, fonti, ...); TODO Bassa priorità: export diretto del dossier (basta un report che lo usi).
2. TODO Utile: export immagine (PNG/SVG) di una vista (`ExportImageTool` è commentato in `FileToolRegistry` e la classe non
   esiste; per il PNG basta dipingere il pannello su un `BufferedImage`).
3. TODO Utile (alta priorità, interoperabilità): export GEDCOM (`ExportGedcom551Tool` / `ExportGedcom7Tool` sono commentati in
   `FileToolRegistry` e le classi non esistono); esiste solo l'import (GEDCOM, Gramps XML) e l'export FLEF. Va dichiarato cosa
   si perde (conclusioni, ricerca, contesto, privacy) e applicato `PrivacyExporter`.
4. TODO Bassa priorità: export CSV di sottoinsiemi (`ExportExcelTool` è commentato; fare solo CSV).


## Report utili al genealogista (da aggiungere; revisione del 2026-11-09)

Esistono `ReportType`: ANCESTOR, DESCENDANT, GROUP, INDIVIDUAL, RELATIONSHIP, PLACE, SOURCE, RESEARCH_QUESTION, BIBLIOGRAPHY,
RESEARCH_PROGRESS, FULL_BIBLIOGRAPHY, FAMILY_BOOK, TO_VERIFY, SURNAMES (più il report delle unioni consanguinee, a parte, in Strumenti). Mancano, in
ordine di valore (si aggiungono come un nuovo `ReportType` con `ReportGenerator`, opzioni in `ReportOption`/`ReportOptionState`, voce
nel menu dei report, aiuto `reports.md` e test in `ReportOperationsTest`/`ReportGenerationTest`; i renderer PDF, DOCX, HTML, Markdown, testo,
EPUB e sito statico restano quelli):

1. To Verify, ancora aperto: i rilievi dell'assistente di ricerca (Research Gap, Chronology, Source Citation, Identity) stanno in
   `Core/ui/assistant`, il report è in Domain e non può dipendere da lì (il report lo dice in una riga): per includerli serve spostare nel
   Domain i provider (o un'interfaccia che il Core implementa e passa al report, come `ReportRenderer`). I messaggi dei problemi del file
   sono nella lingua dell'applicazione (`I18N`), non in quella del report. Matrimoni senza coniugi, individui senza genitori e altre
   lacune relazionali non sono ancora cercati (si aggiungono come un `Kind` di `ToVerifyFinder` e una chiave `TO_VERIFY_<KIND>` nei
   messaggi dei report; le lingue diverse da inglese e italiano ricadono sull'inglese).
2. BASSA Calendario degli anniversari: nascite, matrimoni e morti ordinati per mese e giorno, con l'anno e l'età. Globale, o per
   gruppo. Facile (gli eventi e le date normalizzate ci sono) e molto condiviso con i parenti; privacy applicata (i viventi presunti
   sono `restricted`: sono esclusi).
3. Surnames by Place, ancora aperto: raggruppare per un livello della gerarchia dei luoghi (per provincia o per regione: oggi il gruppo è
   il luogo dell'evento, anche se è una frazione); il periodo per secolo; i cognomi di tutti i nomi dell'individuo (il cognome da nubile di
   una donna sposata è un altro nome); un confronto fra grafie simili (`NameSimilarity` c'è già); i tipi `baptism` e `christening` sono
   costanti di `EventReader` (`CUSTOM_TYPE_*`), non dell'elenco del protocollo.
4. BASSA Lacune per generazione: antenati noti contro possibili (2^n) per ogni generazione, con il collasso del pedigree
   (`PedigreeCollapseDetector`). Dà un'idea dello stato della ricerca; in parte è nel Research Progress.
5. BASSA Numerazione Sosa-Stradonitz (ahnentafel) nel report degli antenati: da verificare se `AncestorReportGenerator` numera
   gli antenati (il `Layerer` del Sugiyama calcola già i numeri, il report no).
Da non fare: un report per ogni vista (la vista si esporta come immagine, vedi "Export immagine"); report sul solo coefficiente F
di un individuo (c'è già il dialogo degli anelli).


## Priorità

1. Export GEDCOM (poi PNG di una vista).
2. Filtri dell'ego network.


## Altri TODO emersi dalla revisione

- TODO Test di round-trip per l'export GEDCOM, una volta scritto.
- Misure su un file grande (30000 individui): gli assistenti sono lenti (cronologia 40-65 s, relazioni 55-60 s, consanguineità 140 s,
  identità 30 s, gli altri sotto 5 s), ma stanno fuori dall'EDT con il ritmo dell'indicatore (4 volte la durata). La prima copia del
  modello per la lettura fuori dall'EDT costa 0.9 s (sull'EDT, una volta), quella dopo una modifica quanto è cambiato.


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


## Limiti noti di Validate File, dei calcoli lunghi e dei dialoghi (non fatto)

- Validate File: non c'è un controllo che un file sia del tipo che il record dichiara (un'immagine che è un PDF); il costo della ricerca
  dei media su un'unità di rete non è misurato; i controlli strutturali dei record (`ConsistencyService.check`) sono un passo unico, non
  per record (sono rapidi rispetto alla grammatica); le date degli attributi non sono controllate, e gli stessi avvisi sulle date illeggibili
  non sono nell'assistente della cronologia. Le regole del sottoinsieme dimostrabile delle date sono scritte due volte
  (`DateConsistency` e `ChronologyChecker`, che fa Diagnostics con i suoi record dati): solo `DateRange` è condiviso. Un anno è giudicato
  con prudenza (l'ultimo giorno di un anno di 366 giorni si sovrappone al primo del successivo).
- Avanzamento dei calcoli lunghi (Detect Duplicates, assistente di ricerca): gli altri provider dell'assistente non danno avanzamento
  (la barra può aspettare su uno lento); l'indicatore della finestra, che ricalcola in background, non ha barra.
- Dialoghi: la chiusura con ESC dei dialoghi degli strumenti, uno per uno, non è provata da un test (servono argomenti: si prova solo
  l'helper che usano e una scansione controlla che lo usino), né un vero tasto su un vero focus (i test eseguono l'azione associata).
