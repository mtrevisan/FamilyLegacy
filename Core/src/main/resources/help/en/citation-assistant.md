# The Source Citation Assistant

The citation assistant turns an archival reference written as you find it, such as `vol. 3, p. 45, n. 12`, into the **locator** of a citation: `Volume: 3, Page: 45, Entry: 12`. It suggests; **nothing changes until you press Apply to Citation**, and even then only the *Locator* field of the open editor changes, until you save the citation.

## How to open it

It is an action of the [Research Assistant](assistant.md), and it is offered **only while you are inside a citation editor**:

1. Open the editor of a **source citation** (from the *Sources* tab of any record: *Create New...* or *Edit Citation...*), or of a **repository citation** (from the *Repositories with Citations* list of a source).
2. Click inside the editor, so that it has the focus.
3. Press `Ctrl+Shift+A` (or choose **Tools > Research Assistant...**) and pick **Source Citation Assistant** (it is listed first, in the category *Sources*).

It works from inside any open editor, even one that blocks the main window. It is not offered for place citations, and it does not appear when you are not in a citation editor.

## The window

- A reminder: *The locator is a draft built from your text: check it before applying it. What is not understood is shown, not guessed.*
- **Archival reference**: a text box, already filled with the current locator of the citation. Type or paste the reference as written in the source. It is interpreted at every keystroke.
- The **findings**, below it: one line for each part that was understood (`Label: value`), then, if there is anything left over, **Not understood**: the fragments that were not recognised, separated by semicolons, and notes that explain the doubtful readings.
- **Locator**: the text that will be written into the citation. You can edit it, but it is rewritten each time you change the reference box, and the buttons use whatever it contains at that moment.
- **Clues from the media of the source** and **Repositories named in the reference**: see below. They appear only when there is something to show.
- **Apply to Citation** and **Copy Locator**.

`Esc` closes the window from any field without applying anything.

## What it understands

It reads Italian, English, Latin, French, German and Spanish. A word is recognised only as a **whole word** (so *pagination* is not a page), in any case, with or without a final full stop or colon. A single letter (`p`, `f`, `c`, `n`) counts only when it is followed by a full stop, a colon or a number directly.

The locator always lists the parts in **this order**, whatever the order you typed them in:

| Part | Words it recognises |
|---|---|
| **Film** | film, microfilm, rullino, rotolo, roll, reel, DGS, mf |
| **Shelf Mark** | segnatura, collocazione, shelfmark, shelf mark, signature, cote, signatur, call number, inventario, inv; and *ms*, *mss*, *cod*, *codex* (which stay in the value: `Ms. 1234`) |
| **Box** | busta, buste, box, scatola, faldone, mazzo, cassa, carton, caja |
| **File** | fascicolo, fasc, filza, cartella, dossier, file, fascicule, legajo |
| **Register** | registro, registri, reg, register, registre, registers |
| **Year** | anno, year, jahr, année, annee (or a bare year from 1000 to 2099) |
| **Volume** | vol, volume, volumen, tomo, tome, tom, libro, band, bd |
| **Item** | item, pezzo, pezzi, unit, unità |
| **Page** | p, pp, pg, pag, pagg, pagina, pagine, page, pages, seite, seiten, pág, página |
| **Folio** | fol, folio, foglio, f, c, cc, carta, carte, blatt, bl, feuillet, hoja |
| **Frame** | frame, fotogramma, image, img, immagine, imm, foto, bild, vue |
| **Entry** | n, nr, no, num, numero, número, number, atto, atti, entry, acte, akte, eintrag |
| **Line** | line, lines, riga, rigo, ligne, zeile, linea |

**Values.** A number can have *recto* or *verso* (`12 verso` becomes `12v`), a letter (`12a`), or be a range (`45-47`, `45 – 47`, `12/1850`). Leading zeros are removed, except for *Film*, *Box* and *File*, which are codes (`busta 007` stays `007`). For *Shelf Mark* and *Register* the value is free text up to the next comma or semicolon; small words such as *dei*, *del*, *of*, *des*, *the* at its start are dropped (`Registro dei nati 1850` becomes `Register: nati 1850`).

**Prudence.** The assistant never guesses. Text before the first keyword, numbers with no label, archive names (*fondo*, *serie* and the like are not keywords) and unknown words are listed under **Not understood** and are **never** put in the locator. If the same part is written twice, the first is kept and the second is reported. `c.` and `f.` are read as *folio* and the note reminds you that they can also mean *circa* or a file; `c. 1850` is read as *circa 1850* and goes to *Not understood*, not as folio 1850. Empty input gives an empty locator.

**Examples**

| You type | Locator | Not understood |
|---|---|---|
| `vol. 3, p. 45, n. 12` | `Volume: 3, Page: 45, Entry: 12` | |
| `n. 12, p. 45, vol. 3` | `Volume: 3, Page: 45, Entry: 12` | |
| `Libro 2 pag. 143 atto 17` | `Volume: 2, Page: 143, Entry: 17` | |
| `Band 5, Seite 10, Nr. 3` | `Volume: 5, Page: 10, Entry: 3` | |
| `FHL film 1234567, frame 245` | `Film: 1234567, Frame: 245` | `FHL` |
| `ASVe, Notarile Testamenti, busta 123, fol. 12v` | `Box: 123, Folio: 12v` | `ASVe`; `Notarile Testamenti` |
| `Registro dei nati 1850, atto 12/1850` | `Register: nati 1850, Entry: 12/1850` | |
| `Ms. 1234, fol. 5` | `Shelf Mark: Ms. 1234, Folio: 5` | |
| `pp. 45-47` | `Page: 45-47` | |
| `1850, n. 3` | `Year: 1850, Entry: 3` | |

## Clues from the media of the source

If the citation has a source, and that source has media records, the assistant looks at each of them: at the **file name** (without folder and extension) and at the **description**. Each is cleaned (underscores become spaces, a space is put between letters and digits) and read like a reference. Those that give something are listed as `file name → locator`. For example, a file `film1234567_frame245.jpg` gives `Film: 1234567, Frame: 245`.

Select a clue and press **Use Clue**: its locator goes into the **Archival reference** box (not into the citation), where you can correct it; then press **Apply to Citation**. Nothing is offered if no source is chosen, if the source has no media, or if no name gives anything.

## Repositories named in the reference

This appears only in a **repository citation**. The fragments listed under *Not understood* are compared with the repositories of the file: a repository matches if a fragment is **exactly** one of its names, or its **acronym** (the initials of the words that start with a capital letter: *Archivio di Stato di Venezia* gives `ASV`; *Family History Library* gives `FHL`). A partial name such as *Archivio* matches nothing, and `ASVe` does not match `ASV`.

The matching repositories are listed, the first one chosen. **Use Repository** sets the repository of the citation and closes the assistant; it does not touch the locator.

## The buttons

- **Apply to Citation** is disabled while the locator is empty. If the citation already has a different locator, you are asked *The citation already has a locator. Replace it with the new one?* Answer *No* and nothing happens; answer *Yes* and the Locator field of the editor is replaced and the assistant closes. **Save the citation editor** to keep it.
- **Copy Locator** puts the text of the Locator field on the clipboard; the window stays open.
- **Use Clue** and **Use Repository**, as above.
