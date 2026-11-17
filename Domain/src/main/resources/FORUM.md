# FamilyLegacy - Ricerca di beta tester

Ciao a tutti,

negli ultimi anni ho sviluppato *FamilyLegacy*, un'applicazione genealogica che sto utilizzando e migliorando progressivamente.

L'obiettivo non è soltanto costruire alberi genealogici, ma aiutare a documentare l'*intero processo di ricerca genealogica*:

- persone, eventi, luoghi e relazioni
- fonti e citazioni
- ipotesi e possibili identità duplicate
- attività di ricerca e domande aperte
- conclusioni motivate e gestione delle evidenze in conflitto
- contesto storico e culturale
- luoghi storici e cambi di giurisdizione nel tempo
- calendari storici e date complesse

## Cosa cerca di fare FamilyLegacy

FamilyLegacy non nasce per sostituire gli strumenti esistenti, ma per affrontare alcuni aspetti della ricerca genealogica che spesso vengono gestiti in modo esterno tramite note, fogli Excel o documenti separati.

### Rispetto a GEDCOM

GEDCOM è eccellente come formato di scambio e per rappresentare l'albero genealogico risultante.

FamilyLegacy cerca di aggiungere una rappresentazione strutturata di:

- evidenze e citazioni
- conflitti tra fonti
- ipotesi genealogiche
- conclusioni documentate
- attività di ricerca
- task e ricerche future
- contesto storico e culturale

In altre parole, GEDCOM rappresenta molto bene il risultato della ricerca; FamilyLegacy cerca di rappresentare anche il percorso che ha portato a quel risultato.

### Rispetto a Gramps

Gramps è probabilmente uno dei software genealogici più completi oggi disponibili e molte idee di FamilyLegacy nascono proprio da esigenze emerse utilizzandolo.

FamilyLegacy pone però un'enfasi particolare su:

- gestione esplicita delle conclusioni
- tracciamento delle ipotesi
- documentazione dei conflitti
- workflow di ricerca
- modellazione dei luoghi storici e delle giurisdizioni mutevoli
- supporto ai calendari storici

L'obiettivo non è avere semplicemente più funzioni, ma rendere esplicito il ragionamento genealogico.

### Rispetto a Excel e note personali

Molti genealogisti utilizzano fogli Excel, documenti Word, taccuini o file di testo per conservare:

- ricerche effettuate
- fonti già consultate
- ricerche negative
- ipotesi
- conflitti
- cose da verificare

FamilyLegacy cerca di integrare queste informazioni direttamente nel modello genealogico, mantenendo insieme:

- dati
- fonti
- ricerca
- conclusioni

In questo modo la documentazione del lavoro genealogico rimane collegata alle persone, ai luoghi, e agli eventi a cui si riferisce.

## Alcune caratteristiche

- gestione avanzata di luoghi storici e cambi di giurisdizione
- supporto a calendari storici
- gestione delle fonti con estratti e riferimenti ai documenti
- tracciamento delle attività di ricerca
- supporto alla documentazione del ragionamento genealogico
- strumenti di assistenza alla ricerca
- workflow esplicito per ipotesi, conflitti e conclusioni
- gestione della privacy (persone viventi, livelli di riservatezza) per viste, report ed esportazione
- interfaccia a tre livelli (Basic, Standard, Advanced) con suggerimenti contestuali: si usa come un normale programma genealogico e le funzioni di ricerca emergono quando servono

## Semplice o completo, a scelta

Non è necessario usare tutto: l'interfaccia ha tre livelli, che si cambiano da *Settings > Interface Level*.

- **Basic** (predefinito): un programma genealogico tradizionale (persone, famiglie, eventi, luoghi, fonti, media); i concetti di ricerca non compaiono.
- **Standard**: aggiunge il flusso di ricerca (domande, attività, task, ipotesi di identità).
- **Advanced**: tutto, comprese conclusioni, norme culturali, eventi storici e impatti di contesto.

Il file è lo stesso a tutti i livelli: cambia solo ciò che l'interfaccia mostra, e nulla viene perso passando da un livello all'altro.

I concetti più avanzati vengono inoltre *suggeriti quando servono*, con una riga discreta in fondo all'editor del record, mai con finestre che interrompono il lavoro. Per esempio: fonti che si contraddicono, un possibile duplicato, una data stimata per consuetudine senza la norma che la giustifica, un luogo che ha cambiato giurisdizione o nome nel tempo. Le parole si adattano al livello (a Basic: "Possible duplicate person detected"; a Standard e Advanced: "Create Identity Hypothesis..."). I suggerimenti si possono nascondere, ignorare o disattivare del tutto da *Settings > Suggestions in the Editors*.

## Importazione e report

- **Importazione** da GEDCOM (5.5, 5.5.1 e 7) e da Gramps (XML), così è possibile provarlo con i propri dati senza ricominciare da zero. L'originale non viene mai modificato e, al termine, un report elenca ciò che non è stato possibile convertire.
- **14 tipi di report** (antenati, discendenti, libro di famiglia, bibliografia, ricerche ancora da verificare, ...) in testo, Markdown, HTML, Word, PDF, EPUB e sito web statico.
- I dati sono salvati in un formato proprio, testuale e aperto: *FLEF* (Family LEgacy Format).

## Requisiti e licenza

- Software open source, licenza MIT.
- Esiste un installer per Windows, con controllo degli aggiornamenti integrato.
- **L'interfaccia e la guida in linea sono attualmente solo in inglese.**

## Documentazione

https://github.com/mtrevisan/FamilyLegacy/wiki

## Cerco feedback

Sto cercando genealogisti interessati a provarlo e a fornire feedback sinceri:

- cosa funziona bene
- cosa è poco intuitivo
- cosa manca
- cosa potrebbe essere migliorato
- quali problemi reali della ricerca genealogica non sono ancora coperti

Non è necessario essere esperti di informatica: mi interessa soprattutto il punto di vista di chi fa ricerca genealogica concreta su documenti e fonti.

Se qualcuno fosse interessato a provarlo, sarò felice di fornire maggiori informazioni e raccogliere suggerimenti.

Grazie!