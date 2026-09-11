# Prospettiva geografica e storico-territoriale (GIS / Spatial Map)

I record FLEF collegano eventi e individui a coordinate e luoghi storici. Manca una vista cartografica per visualizzare
spostamenti, rotte migratorie e concentrazioni territoriali di un ramo familiare.

Ecco l'analisi del punto 3.


## Cosa FLEF fornisce

Il protocollo è già strutturato per una vista geografica completa, su tre livelli.

**Coordinate.**
`PlaceRecord.map.coordinates` è un `Coord` in formato ISO 6709. La specifica ISO 6709 ammette tre forme:
- `±DD.DDDD±DDD.DDDD/` — gradi decimali
- `±DDMM.MMM±DDDMM.MMM/` — gradi e minuti
- `±DDMMSS.S±DDDMMSS.S/` — gradi, minuti, secondi
- con altitudine opzionale `+.../` finale.

Nessun datum è dichiarato: per convenzione ISO 6709 il datum implicito è WGS84. Per una genealogia è sufficiente.

**Gerarchia dei luoghi.**
`PlaceRelationshipRecord` collega i luoghi tra loro con cinque tipi (`administrative_part_of`, `geographic_part_of`, `ecclesiastical_part_of`, `judicial_part_of`, `cadastral_part_of`), ognuno con `valid_from` / `valid_to`. Questo significa che il protocollo supporta nativamente la **geografia storica**: un comune che nel 1800 apparteneva a una provincia e nel 1900 a un'altra è rappresentabile.

**Ancoraggio temporale.**
Ogni entità (individuo, gruppo, luogo) partecipa a `EventRecord` tramite `EventParticipationRecord`. Gli eventi hanno `date` e `place`. Gli attributi individuali e di gruppo hanno `place` e `valid_from` / `valid_to`. Le citazioni di luogo (`PlaceCitation`) portano anche `original_text`, cioè il nome del luogo come scritto nella fonte, che può differire dal nome normalizzato.


## Cosa manca

1. **Vista cartografica.**
   Nessuna delle viste attuali proietta le coordinate su un piano. L'utente non vede dove sono accaduti gli eventi.

2. **Rotte migratorie.**
   FLEF registra per ogni individuo una sequenza di eventi in luoghi diversi (nascita, matrimonio, immigrazione, morte). Nessuna vista li collega in una traiettoria.

3. **Geografia storica.**
   `PlaceRelationshipRecord` con le sue date permette di ricostruire "chi apparteneva a chi" in un dato anno. Nessuna vista espone questa informazione. Senza di essa, il luogo "Treviso" è solo un punto, non "Treviso, Lombardo-Veneto, Impero Austriaco nel 1840".

4. **Concentrazione territoriale.**
   Per un ramo familiare, dove si concentrano gli eventi? Quante nascite a Venezia tra il 1750 e il 1800? La risposta richiede un aggregatore geografico.

5. **Contesto storico-territoriale.**
   `HistoricEventRecord` ha a sua volta un `place`. Una guerra, un'epidemia, una riforma legale, hanno una collocazione geografica che incrocia la storia familiare.

6. **Parsing ISO 6709.**
   Il codebase non ha un parser per `Coord`. Va costruito, con tutte e tre le forme di rappresentazione.

7. **Dati cartografici.**
   Il progetto attualmente non ha mappe. Serve una sorgente di confini (paesi, regioni, province) da spedire come risorsa, oppure un approccio "senza mappa" con solo graticcio lat/long e punti.


## Architettura proposta

Due viste complementari più un pannello di contesto.

### Vista A — Spatial Map (mappa)

Una tela che proietta le coordinate su un piano, con:
- **Sfondo**: opzionale, confini vettoriali caricati da risorsa (Natural Earth, pubblico dominio). Se la risorsa manca, sfondo bianco con graticcio lat/long.
- **Marker**: un punto per ogni luogo del modello che ha coordinate. Il marker è cliccabile e apre la `PlaceRecord`.
- **Eventi**: ogni evento con luogo è un punto colorato per tipo.
- **Rotte**: per un individuo o un ramo familiare selezionato, una polyline che collega gli eventi in ordine cronologico.
- **Filtro temporale**: uno slider che aggiorna cosa è visibile per data.
- **Proiezione**: Mercatore (standard per mappe interattive) o equirettangolare (più semplice). Con zoom e pan.

### Vista B — Territorial Timeline

Un pannello che, dato un luogo e un anno, mostra la gerarchia amministrativa in quel momento:
```
Treviso (city)
  → Veneto (region)          [1866-]
  → Lombardo-Veneto (kingdom) [1815-1866]
  → Impero Austriaco (empire) [1804-1867]
```
Costruita percorrendo `PlaceRelationshipRecord` con `valid_from <= anno <= valid_to`. È la vista che rende la geografia storica navigabile.

### Vista C — Migration Explorer

Un pannello che, dato un individuo o un gruppo, estrae la sua sequenza migratoria: tutti gli eventi con luogo, ordinati per data, con le distanze e le date intermedie. Mostra:
- una tabella cronologica (data, luogo, tipo di evento, distanza dal precedente);
- un rendering semplificato della rotta (linea con punti);
- click su una riga → centra la Vista A su quel luogo.


## Elenco classi

Stesso schema a layer.

### Layer 1 — Enumerazioni

1. **`GeoProjectionType`** — enum: `EQUIRECTANGULAR`, `MERCATOR`, `MERCATOR_WEB`.
2. **`GeoMarkerType`** — enum: `PLACE`, `EVENT`, `RESIDENCE`, `MIGRATION_STOP`, `HISTORIC_EVENT`.
3. **`GeoZoomLevel`** — enum: `WORLD`, `CONTINENT`, `COUNTRY`, `REGION`, `PROVINCE`, `MUNICIPALITY`, `LOCAL`.
4. **`MigrationEventKind`** — enum: `BIRTH`, `RESIDENCE`, `MARRIAGE`, `IMMIGRATION`, `EMIGRATION`, `DEATH`, `BURIAL`, `OTHER`.

### Layer 2 — Dati di base

5. **`GeoCoordinate`** — record: `latitude` (double), `longitude` (double), `altitude` (Double, nullable), `originalFormat` (String, la stringa ISO 6709 originale), `precision` (`GeoCoordinatePrecision`).
6. **`GeoCoordinatePrecision`** — enum: `DEGREES`, `MINUTES`, `SECONDS`. Determina quanti decimali mostrare.

### Layer 3 — Aggregati

7. **`GeoPlaceRef`** — record: `entity` (`TemporalEntityRef` per PlaceRecord), `coordinate` (`GeoCoordinate`, nullable), `displayName`, `historicalName` (da `original_text`), `parentRef` (`GeoPlaceRef`, nullable — la gerarchia risolta).
8. **`GeoEventRef`** — record: `eventRecord` (`FLEFRecord`), `placeRef` (`GeoPlaceRef`), `date` (`NormalizedDate`), `kind` (`MigrationEventKind`), `participants` (`List<TemporalEntityRef>`).
9. **`GeoMigrationRoute`** — record: `subject` (`TemporalEntityRef`), `stops` (`List<GeoEventRef>`), `totalDistanceKm` (double), `totalDurationDays` (long).
10. **`GeoTerritorialSnapshot`** — record: `placeRef` (`GeoPlaceRef`), `year` (int), `hierarchy` (`List<GeoTerritorialLevel>`). Ogni `GeoTerritorialLevel` è una coppia `(relationshipType, placeRef)`.
11. **`GeoMapModel`** — record: `places` (`List<GeoPlaceRef>`), `events` (`List<GeoEventRef>`), `routes` (`List<GeoMigrationRoute>`), `bounds` (`GeoBounds`), `snapshotYear` (Integer, nullable). `GeoBounds` è `(minLat, maxLat, minLon, maxLon)`.

### Layer 4 — Servizi

12. **`Iso6709Parser`** — class. Converte una stringa `Coord` in `GeoCoordinate`. Supporta le tre forme (gradi decimali, minuti, secondi), l'orientamento cardinale opzionale, l'altitudine opzionale. Lancia eccezione su formato invalido.
13. **`GeoDistanceCalculator`** — class. Distanza ortodromica tra due coordinate con la formula di Haversine. Usata per le rotte e per la concentrazione.
14. **`GeoTerritorialResolver`** — class. Dato un `PlaceRecord` e un anno, percorre `PlaceRelationshipRecord` all'indietro per costruire la gerarchia amministrativa in quell'anno. Cache per `(placeId, year)`.
15. **`GeoMigrationExtractor`** — class. Dato un `TemporalEntityRef` (individuo o gruppo), estrae la sequenza di eventi con luogo, li ordina per data, costruisce una `GeoMigrationRoute`.
16. **`GeoMapService`** — class. Orchestratore:
    - costruisce gli indici (`GeoIndices`, simile a `TemporalIndices`) per place-citations, eventi con place, attributi con place, historic events con place, place relationships;
    - costruisce il `GeoMapModel` filtrato per anno, tipo, entità;
    - espone `resolveTerritory(placeId, year)` delegando a `GeoTerritorialResolver`;
    - espone `migrationRouteFor(entityId)` delegando a `GeoMigrationExtractor`;
    - mantiene la cache.

### Layer 5 — Proiezione e layout

17. **`GeoProjection`** — class. Converte `(lat, lon)` in `(x, y)` pixel dato un `GeoBounds` e una viewport. Implementa le tre proiezioni di `GeoProjectionType`. Ha anche l'inversa `(x, y) → (lat, lon)` per il click e l'hover.
18. **`GeoMapViewport`** — class. Finestra visibile in coordinate geografiche (`GeoBounds`), con metodi di pan, zoom, fit. Concettualmente identica a `TemporalAxis`, ma per due dimensioni.
19. **`GeoLayout`** — class. Calcola la posizione dei marker in base alla viewport corrente, gestisce la sovrapposizione (cluster per punti troppo vicini), produce una mappa `GeoPlaceRef → Rectangle`.

### Layer 6 — Renderer

20. **`GeoGraticuleRenderer`** — class. Disegna il graticcio lat/long (meridiani e paralleli) con etichette in gradi. Si adatta al livello di zoom.
21. **`GeoBorderRenderer`** — class. Disegna i confini da una risorsa vettoriale (GeoJSON semplificato o SVG). Se la risorsa manca, non disegna nulla e la mappa è bianca.
22. **`GeoMarkerRenderer`** — class. Disegna i marker: punto pieno per luoghi con coordinate, punto vuoto per luoghi la cui coordinata è ereditata dal parent. Colore per tipo di evento. Etichetta solo sopra una certa soglia di zoom.
23. **`GeoRouteRenderer`** — class. Disegna le rotte migratorie come polyline con frecce direzionali, punti numerati sull'ordine cronologico, e stile variabile in base al numero di tappe.
24. **`GeoConcentrationRenderer`** — class. Disegna la densità territoriale come heatmap (kernel density) o come cluster aggregati. Usata quando si vuole "dove si concentrano gli eventi di questo ramo familiare".
25. **`GeoMapRenderer`** — class. Orchestratore: graticcio, confini, rotte, marker, concentrazione, overlay di selezione.

### Layer 7 — UI

26. **`GeoMapController`** — class. Pan (drag), zoom (rotella, pulsanti), fit-to-bounds, conversione schermo↔coordinate.
27. **`GeoMapInteractionHandler`** — class. Hit-testing dei marker, tooltip (nome, coordinate, eventi associati, gerarchia storica), doppio-click per aprire `PlaceRecord`.
28. **`GeoMapToolbar`** — class. Controlli: zoom, proiezione, slider temporale, toggle per rotte/eventi/luoghi/confini/heatmap, combo per entità centrale, pulsante "Migration Explorer".
29. **`GeoMapPanel`** — class. Il `JPanel` principale. Compone toolbar, mappa, eventuale pannello laterale di dettaglio.
30. **`MigrationExplorerPanel`** — class. Vista C. Tabella cronologica + rendering semplificato della rotta + statistiche (distanza totale, durata, numero di tappe).
31. **`TerritorialTimelinePanel`** — class. Vista B. Gerarchia storica di un luogo, navigabile anno per anno con uno slider.

### Risorse

32. **`GeoBorderData`** — file di risorsa. Confini vettoriali semplificati (Natural Earth 1:110m per il mondo, 1:50m per l'Europa, opzionale 1:10m per l'Italia). Formato: GeoJSON minimale o un formato binario custom compatto. Caricato pigramente.


## Note su dati e proiezioni

**Sorgente dei confini.** Natural Earth è pubblico dominio e fornisce shapefile pronti per la conversione. Il livello 1:110m pesa poche centinaia di KB, quello 1:50m pochi MB. Per un'applicazione desktop è accettabile spedirli come risorse. In alternativa, il progetto può partire senza confini e mostrare solo il graticcio, lasciando la mappa "vuota" ma funzionale.

**Proiezione.** Mercatore è ciò che l'utente si aspetta da una mappa interattiva, ma distorce le latitudini alte. Per una genealogia italiana o europea è irrilevante. Per una genealogia che copre le Americhe o l'Australia, è ancora accettabile. Mercatore Web (quello di OpenStreetMap) ha il vantaggio di essere lo standard di fatto: se un domani si volesse aggiungere una mappa a tile, le coordinate coinciderebbero.

**Coordinate mancanti.** Molti `PlaceRecord` non avranno coordinate. Due strategie:
1. **Ereditarietà**: risali la gerarchia `PlaceRelationshipRecord` finché trovi un antenato con coordinate. Il marker è disegnato vuoto per indicare che la posizione è approssimata.
2. **Geocoding offline**: nessuna fonte locale è affidabile per nomi storici. Meglio non tentare.

**Nomi storici.** `PlaceCitation.original_text` contiene il nome come scritto nella fonte. Va mostrato nel tooltip, non usato per il posizionamento (che si basa sull'id normalizzato).

**Geografia storica.** `PlaceRelationshipRecord` con `valid_from` / `valid_to` va risolto **al volo** per l'anno di interesse. Non c'è una gerarchia unica: la stessa "Treviso" ha tre gerarchie diverse nel 1800, 1850, 1900. Il `GeoTerritorialResolver` è l'unico punto che fa questa risoluzione.

**Prestazioni.** Stessa lezione della Temporal Projection: costruire gli indici una volta sola. `GeoIndices` con mappe `placeId → eventi`, `placeId → attributi`, `placeId → historic events`, `placeId → figli gerarchici`. Niente scansioni ripetute.


## Ordine di implementazione consigliato

1. `GeoCoordinatePrecision` + `GeoCoordinate` + `GeoProjectionType`
2. `Iso6709Parser`
3. `GeoDistanceCalculator`
4. `GeoPlaceRef` + `GeoEventRef` + `GeoBounds`
5. `GeoMigrationExtractor` (dipende solo dagli eventi, testabile subito)
6. `GeoTerritorialResolver` (dipende da `PlaceRelationshipRecord`)
7. `GeoIndices` + `GeoMapService`
8. `GeoMapModel` + `GeoTerritorialSnapshot` + `GeoMigrationRoute`
9. `GeoProjection` + `GeoMapViewport` + `GeoLayout`
10. `GeoGraticuleRenderer` + `GeoMarkerRenderer` + `GeoRouteRenderer`
11. `GeoMapController` + `GeoMapInteractionHandler`
12. `GeoMapToolbar` + `GeoMapPanel`
13. `MigrationExplorerPanel` + `TerritorialTimelinePanel`
14. `GeoBorderRenderer` + risorsa `GeoBorderData`

Le prime 8 classi sono pura logica, testabili senza Swing. La parte grafica parte dal punto 9.


## Coerenza con l'esistente

**Riuso:**
- `TemporalEntityRef` per riferimenti a luoghi e individui.
- `TemporalSpan` per le date.
- `NormalizedDate` per l'ordinamento cronologico.
- `TemporalIndices` come modello di indicizzazione (estendibile con `placeCitationsByPlaceId`, ecc.).
- Il pattern service + renderer + panel è identico a quello della Temporal Projection.
- `IndividualPanel` / `GroupPanel` per i pannelli di dettaglio laterali.

**Nuovo:**
- Parsing ISO 6709 (unico punto di verità per le coordinate).
- Proiezione geografica (unico punto di verità per la conversione coordinate↔pixel).
- Risoluzione storica della gerarchia territoriale (unico punto di verità per "chi apparteneva a chi").

**Cosa NON fare:**
- Non aggiungere dipendenze cartografiche pesanti (GeoTools, JTS, ecc.): il codebase non ne ha, e per una genealogia non servono.
- Non usare tile online: rompe l'offline e la privacy.
- Non duplicare la logica di risoluzione della gerarchia: `GeoTerritorialResolver` è l'unico posto.
- Non caricare i confini all'avvio: vanno caricati pigramente, solo quando la vista mappa è aperta e solo al livello di zoom necessario.


Il punto 2 e il punto 3 sono indipendenti ma condividono `TemporalEntityRef`, `NormalizedDate`, `TemporalSpan` e il pattern service+renderer+panel. Se vuoi, possiamo partire dal punto 2 o dal punto 3 a tua scelta, oppure costruire prima le due classi condivise che servono a entrambi (`SocialFilters` e `GeoMapViewport` hanno entrambe bisogno di un modello di "filtro temporale" che non esiste ancora). Dimmi tu.
