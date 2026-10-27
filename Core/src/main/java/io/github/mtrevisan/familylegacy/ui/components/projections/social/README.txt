# Relazioni non parentali e network sociale/professionale

FLEF supporta ruoli formali e informali (padrini, testimoni, datori di lavoro, soci, mentori, esecutori testamentari).
L'albero genealogico li esclude; l'Ego Network li riduce a generici "associati". Legenda: DONE = verificato nel codice,
TODO = non presente, PARZIALE = presente solo in parte.

Stato verificato sul codice (ottobre 2026).


## Stato

Il package `social` contiene quasi tutto il progetto originale. **Il pannello però non è collegato a nessuna
schermata**: `ProjectionType` espone solo `BIOLOGICAL_TREE`, `SUGIYAMA_GRAPH` ed `EGO_NETWORK`, e `SocialNetworkPanel` non
è istanziato da nessuna parte.

TODO collegare `SocialNetworkPanel` al `ProjectionSwitcherPanel` (nuovo `ProjectionType`, voce di menu, shortcut).

Già disponibile fuori dal package: `EgoNetworkPanel` (relazioni di 1° grado con `EgoNode.RelationInfo(type, role,
isInverse)`, tooltip con tipo, ruolo e `(Inverse)`).


## Cosa mancava rispetto al punto 2

1. DONE **Navigazione per categoria.** `SocialRelationCategory` + `SocialRoleClassifier` + `SocialCategorySelectionDialog`.
   Resta PARZIALE il filtro per ruolo grezzo: `SocialFilters` accetta un `Set<String>` di ruoli (`withRoles`) ma la toolbar
   offre solo le categorie e il flag "Empty role".
2. DONE **Categorie semantiche** (mappa euristica ruolo -> categoria).
3. DONE **Rilevanza temporale** (`SocialFilters.minDate` / `maxDate` / `includeInactive`).
4. DONE **Path finding** (`SocialPathFinder`, `SocialPath`, `SocialNetworkPathFinderDialog`, richiamato dal pannello).


## Classi (tutte presenti salvo l'ultima)

- Layer 1 enumerazioni: `SocialRelationCategory`, `SocialEdgeDirection`, `SocialLayoutMode`.
- Layer 2-3 dati: `SocialNodeRef`, `SocialEdgeRef`, `SocialPath`, `SocialGraph`, `SocialFilters`.
- Layer 4 servizi: `SocialRoleClassifier`, `SocialNetworkService`, `SocialPathFinder`.
- Layer 5 layout: `SocialLayout`.
- Layer 6 renderer: `SocialEdgeRenderer`, `SocialNodeRenderer`, `SocialNetworkRenderer`.
- Layer 7 UI: `SocialNetworkPanel`, `SocialNetworkToolbar`, `SocialNetworkInteractionHandler`,
  `SocialNetworkPathFinderDialog`, `SocialCategorySelectionDialog`.
- TODO `RoleExplorerPanel` (elenco dei ruoli della rete per categoria, con conteggio e checkbox di filtro): non esiste.


## Coerenza con l'esistente

- `TemporalEntityRef` e `TemporalSpan` (package `temporal`) per riferimenti e date.
- `SocialRoleClassifier` è l'unico punto di verità ruolo -> categoria; `SocialFilters` l'unico per i filtri;
  `SocialLayout` l'unico che calcola posizioni.


## Cosa NON fare

- Non generalizzare ulteriormente `EgoNetworkPanel` (1 grado, tutte le categorie): meglio la vista separata.
- Non usare un motore di graph layout esterno.
- Non mettere i ruoli in un enum chiuso: `role` è testo libero nel protocollo.


## Backlog (idee non ancora realizzate)

PARZIALE 1. Archi multipli tra la stessa coppia: `SocialGraph.edgesBetween()` li restituisce, ma non è verificato che il
   renderer li distingua.

TODO:
2. Selettore dei ruoli esatti nella toolbar (il filtro esiste in `SocialFilters`).
3. Percorso con i ruoli di ogni passaggio ("role path").
4. Analisi di reciprocità (due persone che si considerano a vicenda testimone, padrino, ...).
5. Vista bipartita individui-gruppi e layout bipartito.
6. Analisi di co-appartenenza ("chi condivide 3+ gruppi").
7. Storia dei ruoli nel tempo ("quando X è diventato presidente").
8. Archi "evento condiviso" per tutti i presenti allo stesso evento.
9. Clustering per profilo di ruoli.
10. Centralità (grado, betweenness) per trovare gli "hub".
11. Rilevamento dei ponti (archi la cui rimozione disconnette il grafo).
12. Inferenza di ruoli (due padrini dello stesso figlio potrebbero essere una coppia).
13. Evoluzione della rete nel tempo (snapshot o animazione).
14. Evidenziare tutte le istanze di un ruolo (colore invece di filtro).
15. Cammino vincolato a una categoria di relazioni.
16. Export della rete (CSV / GraphML / DOT).
17. Integrazione con la proiezione temporale (selezione sincronizzata nodo <-> timeline).
18. Matrice di co-occorrenza.
19. Frequenza dei ruoli nel dataset.
20. Filtro "relazioni familiari-sociali" (padrini).
21. Punteggio di forza della relazione (eventi condivisi, tipi multipli, sovrapposizione temporale).
22. Editor dedicato delle relazioni sociali (ruolo, date, fonti).
23. Vista multi-ego (due reti affiancate con contatti comuni evidenziati).
24. Pannello di statistiche del grafo (nodi, archi, grado medio, densità, componenti).
