# Relazioni non parentali e network sociale/professionale

FLEF supporta ruoli formali e informali (padrini, testimoni, datori di lavoro, soci, mentori, esecutori testamentari).
L'albero genealogico li esclude; l'Ego Network li riduce a generici "associati" senza permettere di navigare la rete
sociale su più livelli.

Prima di elencare le classi, conviene chiarire **cosa manca esattamente** rispetto a quello che già esiste, perché il punto 2 della tua analisi copre tre cose diverse che vanno risolte separatamente.

## Cosa c'è già e cosa manca

**Già disponibile:**
- `EgoNetworkPanel` estrae relazioni di 1° grado, incluse `associate`, `group_member`, `part_of`.
- `EgoNode.RelationInfo(type, role, isInverse)` conserva tipo, ruolo e direzione.
- `IndividualPanel` / `GroupPanel` mostrano i nodi.
- Il tooltip di `EgoNetworkPanel` mostra già tipo, ruolo e `(Inverse)`.

**Cosa manca per coprire il punto 2:**

1. **Multi-livello.** L'Ego Network è a 1 grado. Non puoi chiedere "mostrami i padrini dei padrini" o "i soci d'affari dei miei testimoni di nozze". Serve un'esplorazione a N gradi con BFS dalla persona centrale.
2. **Navigazione per ruolo.** Oggi il ruolo è solo testo in un tooltip. Non puoi filtrare per ruolo ("mostrami tutti i testimoni"), non puoi raggruppare per categoria ("mostrami solo le relazioni professionali"), non puoi tracciare un cammino tra due persone attraverso i ruoli.
3. **Categorie semantiche.** Il protocollo lascia `role` come testo libero per `associate`. Non esiste una categorizzazione ufficiale. Serve una mappa euristica (ruolo → categoria) più la possibilità per l'utente di filtrare per ruolo grezzo.
4. **Rilevanza temporale.** `RelationshipRecord` ha `valid_from` / `valid_to` / `status`. Una rete sociale cambia nel tempo. Il punto 2 senza filtro temporale mostra relazioni che non sono mai coesistite.
5. **Path finding.** "Come è collegato A a B?" è la domanda centrale di una rete sociale. Un cammino minimo con evidenziazione dei passaggi è la risposta naturale.


## Architettura proposta

Tre viste complementari, da implementare in quest'ordine.

### Vista A — Social Network Explorer (multi-degree)

Estende l'Ego Network a N gradi con un layout a **anelli concentrici**: centro = ego, anello 1 = contatti diretti, anello 2 = contatti dei contatti, ecc. È implementabile con i pattern già esistenti (il layout è deterministico, il renderer riusa le linee ortogonali o curve di Bézier).

**Perché anelli concentrici e non force-directed:** il force-directed richiede un motore di simulazione, non è deterministico, e non si integra con i pattern di layout del codebase. Gli anelli sono immediati da leggere (la distanza dal centro è il grado) e banali da implementare.

### Vista B — Role Explorer

Un pannello affiancato che elenca i ruoli presenti nella rete corrente, raggruppati per categoria, con conteggio. Click su un ruolo → filtra la rete. Checkbox per categoria → mostra/nascondi intere classi di relazione.

### Vista C — Path Finder

Dialogo che prende due entità e trova il cammino minimo attraverso la rete sociale, mostrandolo evidenziato nella vista A. Serve a rispondere a "come sono collegato a questa persona".


## Elenco classi

Stesso schema dei layer della General Temporal Projection.

### Layer 1 — Enumerazioni

1. **`SocialRelationCategory`** — enum: `FAMILY`, `RELIGIOUS`, `PROFESSIONAL`, `LEGAL`, `COMMUNITY`, `POLITICAL`, `OTHER`.
2. **`SocialEdgeDirection`** — enum: `UNDIRECTED`, `DIRECTED`.
3. **`SocialLayoutMode`** — enum: `CONCENTRIC`, `RADIAL_TREE`. (Estendibile in futuro con `FORCE_DIRECTED`.)

### Layer 2 — Dati di base

4. **`SocialEdgeRef`** — record: `source` (`TemporalEntityRef`), `target`, `relationshipType`, `role`, `category`, `direction`, `span` (`TemporalSpan`), `sourceRecord` (`FLEFRecord`). Si distingue da `TemporalConnection` perché ha categoria e direzione.
5. **`SocialNodeRef`** — record: `entity` (`TemporalEntityRef`), `degree`, `primaryCategory` (la categoria più rappresentata tra i suoi archi).
6. **`SocialPath`** — record: `edges` (`List<SocialEdgeRef>`), `length`.

### Layer 3 — Aggregati

7. **`SocialGraph`** — record: `center` (`SocialNodeRef`), `nodes` (`List<SocialNodeRef>`), `edges` (`List<SocialEdgeRef>`), `maxDegree`, `filters` (`SocialFilters`). Immutabile.
8. **`SocialFilters`** — record: `categories` (`Set<SocialRelationCategory>`), `roles` (`Set<String>`), `maxDegree`, `minDate` / `maxDate` (`NormalizedDate`), `includeInactive` (`boolean`). Immutabile; ha un metodo `accepts(SocialEdgeRef)`.

### Layer 4 — Servizi

9. **`SocialRoleClassifier`** — class. Mappa una stringa di ruolo a una `SocialRelationCategory` tramite una tabella interna con fallback euristico. Es:
   - `padrino`, `madrina`, `godfather`, `godmother`, `witness`, `testimone`, `officiant` → `RELIGIOUS`
   - `executor`, `esecutore`, `grantor`, `grantee`, `notary`, `judge`, `accused`, `power_of_attorney` → `LEGAL`
   - `employer`, `employee`, `partner`, `mentor`, `apprentice`, `colleague`, `business_partner`, `socio` → `PROFESSIONAL`
   - `neighbor`, `friend`, `clan_member`, `tribal_leader`, `elder` → `COMMUNITY`
   - `elector`, `elected`, `noble`, `subject`, `citizen` → `POLITICAL`
   - vuoto o sconosciuto → `OTHER`

10. **`SocialNetworkService`** — class. Orchestratore:
    - parte da un ego e fa BFS fino a `maxDegree`;
    - per ogni nodo, enumera tutte le relazioni sociali (usando `RelationshipHandler` per `associate` / `group_member`, `EventParticipationHandler` per i ruoli di evento come `witness`, `executor`);
    - esclude i tipi familiari puri (`biological_child`, `civil_spouse`, ecc.) perché già coperti da BiologicalTree e EgoNetwork;
    - applica `SocialFilters`;
    - costruisce `SocialGraph`;
    - indicizza per id per il path finder.

11. **`SocialPathFinder`** — class. Dato un `SocialGraph` e due id, restituisce un `SocialPath` con BFS. Gestisce anche il caso "nessun cammino".

### Layer 5 — Layout

12. **`SocialLayout`** — class. Assegna posizioni concentriche ai nodi:
    - calcola l'angolo di ogni nodo in base al grado e alla posizione del padre nel BFS;
    - distribuisce i nodi dello stesso anello in settori proporzionali al sottoalbero;
    - produce una mappa `SocialNodeRef → Point2D`;
    - produce anche un `Rectangle` di contenuto per lo scroll.

### Layer 6 — Renderer

13. **`SocialEdgeRenderer`** — class. Disegna gli archi con stile per categoria:
    - colore per categoria (`RELIGIOUS` blu tenue, `PROFESSIONAL` verde, `LEGAL` grigio scuro, `COMMUNITY` arancio, `POLITICAL` rosso scuro);
    - stroke solido/tratteggiato/puntinato in base a `RelationshipRecord.status`;
    - freccia solo per edge `DIRECTED` (es. `group_member`);
    - label di ruolo al centro dell'arco solo se selezionato o in hover.
14. **`SocialNodeRenderer`** — class. Disegna i nodi. Riusa `IndividualPanel` / `GroupPanel` quando possibile; per i nodi troppo piccoli (grado ≥ 3), usa una mini-card con solo nome e iniziali.

### Layer 7 — UI

15. **`SocialNetworkPanel`** — class. Il `JPanel` principale. Possiede service, grafo, layout, renderer, toolbar. Gestisce scroll, resize, selezione, refresh.
16. **`SocialNetworkToolbar`** — class. Controlli: slider per `maxDegree`, checkbox per categoria, campo di ricerca per ruolo, filtro temporale (due date picker), toggle `includeInactive`, pulsante "Path finder".
17. **`SocialNetworkInteractionHandler`** — class. Hit-testing, tooltip (tipo + ruolo + categoria + span), doppio-click per aprire la dialog del record.
18. **`SocialPathFinderDialog`** — class. `JDialog` con due `RecordSelectionDialog` (sorgente e destinazione) e un'anteprima del cammino.
19. **`RoleExplorerPanel`** — class. Pannello laterale opzionale che elenca i ruoli della rete corrente raggruppati per categoria, con conteggio e checkbox di filtro.


## Coerenza con l'esistente

**Riuso diretto:**
- `TemporalEntityRef` del layer 2 (Temporal Projection) come riferimento ai nodi — è già pronto e coerente.
- `TemporalSpan` per le date delle relazioni sociali.
- `TemporalIndices` per l'indicizzazione dei record — le stesse mappe servono anche qui, solo filtrate per tipo di relazione.
- `EgoNetworkService` per l'estrazione di primo grado — il `SocialNetworkService` lo estende o lo riusa internamente.
- `IndividualPanel` / `GroupPanel` per il rendering dei nodi.

**Nuove responsabilità isolate:**
- `SocialRoleClassifier` è l'unico punto di verità per la mappatura ruolo → categoria. Se l'utente vuole ruoli custom, si modifica lì.
- `SocialFilters` è l'unico punto di verità per i filtri. Non si sparpagliano condizioni in giro.
- `SocialLayout` è l'unico punto che calcola posizioni. Il renderer legge solo.

**Nessuna modifica ai layer esistenti** (a parte l'estensione di `TemporalIndices` con un metodo `socialRelationshipsFor(id)` se vuoi unificare l'indicizzazione).


## Cosa NON fare

- **Non generalizzare ulteriormente `EgoNetworkPanel`.** È già complesso e la sua semantica è "1 grado con tutte le categorie, biologiche incluse". Aggiungere il multi-grado lo renderebbe illeggibile. Meglio una vista separata.
- **Non usare un motore di graph layout esterno.** Tutti i layout del codebase sono deterministici e scritti a mano. Aggiungere una dipendenza rompe lo stile.
- **Non mettere i ruoli in un enum chiuso.** Il protocollo lascia `role` come testo libero. La categoria è un layer di presentazione sopra il ruolo, non un sostituto.


## Ordine di implementazione consigliato

1. `SocialRelationCategory` + `SocialEdgeDirection` + `SocialLayoutMode`
2. `SocialRoleClassifier` (mappatura ruoli)
3. `SocialNodeRef` + `SocialEdgeRef` + `SocialFilters`
4. `SocialGraph`
5. `SocialNetworkService`
6. `SocialLayout` (versione concentrica)
7. `SocialNodeRenderer` + `SocialEdgeRenderer`
8. `SocialNetworkToolbar` + `SocialNetworkPanel`
9. `SocialNetworkInteractionHandler`
10. `SocialPathFinder` + `SocialPathFinderDialog`
11. `RoleExplorerPanel`

Le prime 5 classi sono la parte critica: se le strutture dati e il service sono corretti, il resto è UI.

## So what's still missing? Let me think deeply about what the FLEF protocol allows for social relationships that we haven't covered:

1. Multi-edge between same pair - Two people can have multiple relationships (e.g., business partner AND witness at wedding AND neighbor). Currently SocialGraph deduplicates by record id, but the visual representation might not show all types clearly. We have edgesBetween() but the renderer might not show them distinctly.
2. Role-based filtering UI - We have SocialFilters.roles but no UI to pick specific roles (only categories). A "Role Explorer" would let the user see all roles in the current network with counts, and filter by exact role.
3. Transitive roles / path roles - "The witness of my godfather's marriage" - the path finder gives the path but doesn't expose the roles at each step. A "role path" view would show the sequence of roles connecting two entities.
4. Reciprocal vs asymmetric relationships - Some relationships are symmetric (spouse, cohabiting_partner) and some are not (godparent, employer). The current SocialEdgeDirection captures this, but there's no analysis of "reciprocal relationships" - e.g., "who considers X a witness vs who X considers a witness".
5. Bipartite analysis - Individuals ↔ Groups form a bipartite graph. Some patterns are only visible in the bipartite view (e.g., "these 5 people all belong to the same 3 groups").
6. Co-membership analysis - "Which people share 3+ groups?" - a group-based view of the social network, complementing the individual-based view.
7. Role transitions over time - "When did X become president of the confraternity?" - a per-role timeline showing transitions. This is partially covered by the temporal filter but not by an explicit "role history" view.
8. Cross-reference to event participation - Currently event-based edges are generated only when the role is in RELATIONAL_EVENT_ROLES. But other participants (e.g., all godparents of the same child) form an implicit group. "All people present at the same event" could be a "shared event" edge type.
9. Clustering by role profile - "People who have similar role patterns" - community detection by role similarity.
10. Betweenness centrality / degree centrality - Identify the "hubs" of the social network - the most connected people. Useful for finding influential ancestors.
11. Bridge detection - Identify edges whose removal disconnects the graph. Useful for finding "gatekeepers" - the person who connects two branches of the family via social relationships.
12. Social role inference - From the type of relationship and the roles of common associates, infer likely roles. E.g., if X and Y are both godparents of the same child, they might be a couple.
13. Timeline view of the social network - How does the network evolve over time? An animation or sequence of snapshots.
14. Named groups of people - "Confraternity of St. Anthony" as a group. This overlaps with point 4.
15. Filtering by specific role (not just category) - The current filter accepts Set<String> roles but no UI exists for it.
16. Highlighting all instances of a role - "Show me all witnesses in this network" - color highlight rather than filter.
17. Path through specific roles - "Find a path from X to Y where every edge is a religious relationship" - constrained path finding.
18. Export - Social network to CSV/GraphML/DOT for analysis in external tools.
19. Bipartite layout - Instead of concentric/radial, a bipartite layout showing individuals on one side and groups on the other, with edges between them. Useful for group-membership analysis.
20. Timeline-integrated view - Cross-reference with temporal projection: when you select a node in social network, show its timeline; when you select a person in timeline, show their social network.
21. Co-occurrence matrix - For a set of individuals, a matrix showing how many relationships they share (same groups, same events). Reveals clusters.
22. Reciprocal relationship detection - Two people who both consider each other a witness, godparent etc.
23. Role frequency analysis - Which roles are most common in the dataset? Rare roles might indicate special relationships.
24. Family-like social relationships - "Godparent" is technically family-like. The FAMILY category exists but no dedicated UI. Maybe a "familial-social" filter view.
25. Relationship strength - A heuristic score for the strength of a relationship (based on shared events, multiple relationship types, temporal overlap).
26. Editor for social relationships - A dedicated dialog to create/edit social relationships with role, dates, sources.
27. Multi-ego view - Show the social networks of two people side by side, with shared contacts highlighted.
28. Graph statistics panel - Number of nodes, edges, average degree, density, components.
