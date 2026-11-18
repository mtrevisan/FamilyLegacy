# Pannello delle timeline (package `temporal`)

Il package contiene il motore di disegno delle timeline usato dalle viste di gruppo, tutte nel package `grouptimeline`: Group Timeline
(`GroupTimelinePanel`), carriera dell'individuo (`IndividualCareerPanel`), timeline delle giurisdizioni (`JurisdictionTimelinePanel`).
`TemporalAxis` serve anche alla strip delle vite (`MultiLifespanStripPanel`) e alla timeline globale degli eventi
(`GlobalEventTimelinePanel`).

Stato verificato sul codice (novembre 2026).


## Storia

La proiezione temporale generale (ogni entità del file su un asse comune, `ProjectionType.TEMPORAL`, mai offerta all'utente) è stata
cancellata il 2026-11-17: motivi, backlog scartato e TODO rimasti sono in `projections/README.txt`. Il codice è al commit
`80b4ad3d2e8960ae341131fe0d1d01fec1e2914c`. Con lei sono stati tolti:
- `TemporalProjectionService`, che costruiva il modello di tutto il file (le bande di contesto sono rimaste in `TemporalContextBands`);
- le connessioni fra le righe (`TemporalConnection`, `TemporalConnectionType`, `TemporalConnectionRenderer`) e gli impact link
  (`ContextImpactLink`): le viste di gruppo non li producevano;
- il filtro per tipo di entità e i toggle dei layer della toolbar, che le viste di gruppo nascondevano.


## Classi

Domain (`io.github.mtrevisan.familylegacy.temporal`):
- modello: `TemporalProjectionModel` (righe, bande, dominio), `TemporalRow`, `TemporalTrack`, `TemporalTrackType`, `TemporalEntry`,
  `TemporalContextBand`, `TemporalEntityRef`, `TemporalEntityType`, `TemporalProjectionRef`;
- estrattore e indici usati dai builder del package `groups`: `TemporalExtractor`, `TemporalIndices`;
- bande di contesto: `TemporalContextBands`;
- layout verticale: `TemporalProjectionLayout`.

Core (questo package):
- `TemporalProjectionPanel`: mostra il modello dato da `withModelSource`, richiesto di nuovo a ogni `loadProjection`;
- `TemporalProjectionToolbar`: solo lo zoom (le viste di gruppo hanno i loro filtri);
- asse e zoom: `TemporalAxis`, `TemporalZoomController`;
- renderer: `TemporalAxisRenderer`, `TemporalRowRenderer`, `TemporalSpanRenderer`, `TemporalContextRenderer`, `TemporalRoleColors`,
  costanti comuni in `TemporalProjectionRenderer`;
- `TemporalInteractionHandler`: hit test e tooltip.

Cosa disegna il pannello: righe con tre track (EVENT, ATTRIBUTE, CONTEXT); bande di contesto (eventi storici e norme culturali) con
tooltip; date normalizzate (point, bounded, spanning) con precisione ridotta e approssimazione; stato active/ended/unknown; evidenza
negativa (corpo attenuato con tratteggio diagonale sulle barre, X sui punti, segnalata nel tooltip); zoom e pan; doppio clic per
modificare il record.


## Cosa NON fare

- Non aggiungere motori di visualizzazione esterni (JFreeChart, TimelineJS, ...).
- Non moltiplicare le lane: tre track (EVENT, ATTRIBUTE, CONTEXT), il colore per distinguere.
- Non rifare una proiezione di tutto il file: vedi SCARTATI in `projections/README.txt`.
