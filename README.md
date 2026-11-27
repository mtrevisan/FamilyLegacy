FamilyLegacy

Open work only, by priority. What is done lives in the code and in the help pages (`Core/src/main/resources/help/en`). The backlog of the
views, of the import/export and of the reports is in `Core/src/main/java/.../ui/components/projections/README.md`, the one of the Research
Assistant in `Core/src/main/java/.../ui/assistant/README.md`, the proposed roadmap in `.../projections/NEWFEATURES.md`.

# FamilyLegacy "WOW Features" Roadmap

Only what is still to do. Done so far (FAN table and findings, historical timeline with lifespan comparison and SVG, place-aware context, Book Builder, stories as a kind of note, Ahnentafel and d'Aboville numbering, Evidence Trace, planning findings and task generation) lives in the code and in the help pages.

The emphasis is not on storing more data, but on helping genealogists reason about data.

---

# Phase 2 — FAN Network Intelligence

## 1. FAN Network View

Today the FAN network is a table in the dossier and a list of findings. Still missing:

- a graph visualization of the people around an individual (witnesses, godparents, neighbours, employers, associates, shared events and places);
- a report section / report on the FAN network;
- the "Why are these two individuals connected?" answer: relationships, shared events, shared groups, places and sources, for any two selected individuals.

## 2. Associate Discovery

Recurring witnesses, neighbours and godparents are already found. Still missing: recurring migration companions (the same people moving to the same places in the same years).

---

# Phase 3 — Historical Context Engine

## 3. Historical Overlay Timeline and Lifespan Comparison: remaining outputs

- direct PDF rendering (today: SVG, and PDF by printing it);
- the lifespan comparison as a report section;
- entries in the individual popup menus (today only in the Event menu).

---

# Phase 4 — Intelligent Publishing

## 4. Book Builder: remaining

- save and reload a book definition;
- page numbers inside the indexes;
- true streaming (the document is fully in memory).

## 5. Story Integration: remaining

- tables, quotes and images in the story Markdown;
- an action / search "find stories citing this record" and backlinks in the reports.

---

# Phase 5 — AI-Powered Reasoning

Goal:
Use AI to assist research while keeping all reasoning transparent.

## 6. Historical Interpretation Assistant

User asks:

    What was happening here when this person lived?

Use:

- historic events;
- cultural norms;
- place history.

Generate contextual explanations linked to FLEF records.

---

# Ultimate Goal

FamilyLegacy should become a genealogy reasoning and research environment, not only a database or a report generator:

    "Show me the evidence."
    "Show me the conflicts."
    "Explain the conclusion."
    "Tell me what to research next."
    "Put this person into historical context."

Every major feature should reinforce those capabilities.


## TODO

### HIGH

Usability problems found by a second from-scratch walkthrough (2026-11-27, after the quick entry layer). Scenario: new empty file, enter a person with name, sex, birth, parents, a partner, a source and a note, and build a small tree. Method and limit: I read the code of the dialogs, the tools and the help pages, rendered the new windows to images, and probed the date reader with a test; I did not drive the GUI by hand, so items marked (verify) are suspicions to confirm. Ordered by how much they hurt a beginner.

1. Every list (names, attributes, sources, notes...) shows (no items) and no button: the way to add is a right-click menu or the Insert key, hinted only by a tooltip on hover. The short way to cite a source is in the menu, while Insert opens the long way (source editor, then citation editor). A beginner may not find any of it (verify with someone who has not seen the program). Proposal: a visible Add... row under every list (and Cite a new source... beside it for the sources), and Insert taking the short way for sources.
2. A note is a heavy dialog. Title, kind, text, MIME type, locale, a list of translations, and the tabs Sources, Privacy and Audit, to write a paragraph. Proposal: the text alone, with the rest behind a More... like the title of a source.
3. The name parser is explained only by a tooltip. Rossi Giovanni (family name first, usual in registers and lists) is read as given Rossi, family Giovanni; the preview shows it, but the way to fix it (/Rossi/ Giovanni, or the comma) is in the tooltip. Proposal: next to the preview, a swap link (family first) and the slashes written for the user.

Third walkthrough (2026-11-27), again by reading the code (two scenarios: media, places, events and citations; opening, finding and importing a big tree). Items marked (verify) are suspicions to confirm in the GUI.

18. Media paths are stored absolute (`MediaRecordDialog.setNewItem`), so the link breaks when the file or the folder moves. Proposal: a path relative to the FLEF file (the base is the folder of the file) and a "Copy into the project folder" box in the file chooser, one `MediaFiles.resolve` for every consumer of the uri (viewer, carousel, reports, site, backup), a "Make media paths relative" command for the paths already saved. (Done: the Mapping is behind a More... button, and the Confidence menus have tooltips.)

Resolved since the previous walkthrough (2026-11-26): typing a name takes three dialogs (now one line and a preview), the hidden sex (now a combo, locked when the link imposes it), the birth as event plus participation (now date and place rows, also on an existing person), the union as relationship plus event (one window, with date and place), and the long road to cite a source (plain title, supports by default, Cite a New Source...). Resolved since the second walkthrough: the dates of the quick entry are checked with their calendar (and the calendar can be chosen), the places can be typed as a chain (`Padova, Veneto, Italy`, each one a place of its own, part of the next), and the death is in the family window, and a quick entry says what it created (a line in the tool bar); it is one step of the history, taken back by Undo in one go (it was already so: the history records every action, the walkthrough had not checked), and the burial and the occupation are in the individual box. Resolved since: the parents have a part in the quick entry (father, mother, one type, biological by default) and Add Parent no longer asks the type (Shift asks it).

### MEDIUM

1. **Advanced search and saved groups** (high value).

Extend the existing search infrastructure with:

- AND / OR conditions;
- nested condition groups;
- date ranges;
- event-based criteria;
- relationship-based criteria;
- spouse and parent filters;
- existence/missing-data filters;
- free tags (the *Find record* dialog already filters by them; a saved search must keep the tags it asks for);

Allow users to save a search definition and reuse it later.

Introduce application-level Saved Groups (dynamic groups):

- a saved group is defined by a search query;
- membership is computed dynamically;
- groups update automatically when data changes.

Examples:

- Individuals born before 1800.
- Open research questions.
- Military ancestors.
- Possible duplicate individuals.
- Individuals without birth sources.

Saved groups should be usable in:

- reports;
- views;
- exports;
- statistics;
- Research Assistant workflows.

Avoid protocol changes unless long-term interchange of saved groups becomes a demonstrated requirement.

2. Evaluate later (niche or heavy): genogram and health history view (GenoPro, RootsMagic), DNA tests and Leeds Method report, cemeteries and
headstones, user-scriptable reports, queue of proposed edits for collaboration, globe/migration view, gazetteer with Soundex for place
standardisation.

From MyHeritage Family Tree Builder:

- **Report themes** (PDF/HTML/site): a small set of named themes instead of a colour picker or per-element fonts: Classic Genealogy,
  Academic, Family Book, Minimal, Modern. A theme controls fonts and sizes, heading styles, link colours, table and section styling,
  pedigree boxes tinted by sex, generation separators and page dividers, the cover. New themes must be addable without touching the
  report-generation logic.

- **Page layout panel, next options** (shown for every report, ignored by the formats without pages): cover image, watermark, fonts,
  numbering codes. Every new option follows the recipe: `ReportConfig` component + `ReportOption` constant + checkbox +
  `tooltip.report.<key>` + help page text; `ReportOptionStateTest` keeps hidden options from changing the output.

### LOW

- **Translations (on request only).** For now only English is maintained. When asked, generate: the UI bundle
  `Domain/src/main/resources/i18n/ui/messages_<lang>.properties`; the missing keys of the report bundles
  `Domain/src/main/resources/i18n/report/report_messages_<lang>.properties` (it, de, es, fr, pt, en_GB, en_US, ru, ar, bn, fa, hi, id, ja,
  ko, sw, th, tr, ur, vi, zh_CN; a missing key falls back to English); the help pages (`help/<lang>`, only `en` exists).

- **Privacy refinements** (only if users ask): "do not state still alive / no longer living"; per-flag exclusion of private, confidential or
  living-person details. `LivingPrivacy.NAME_AND_ROLE` (with `PrivacyPolicy`) already covers the common "report as Living" case.

- **Report indexes:** restrict the index of places/dates to births, deaths and marriages; add indexes of occupations and causes of death
  (attribute lookups like the ones of the Surnames report).

- **Editor di testo ricco:** si scrive Markdown in un'area di testo semplice, senza barra degli strumenti né anteprima. Il README dice "rich text", e il testo viene reso correttamente nei report, ma la scrittura è a mano.
- **Markdown a blocchi:** tabelle, citazioni e immagini dentro il testo non sono gestite.

- **Hand-editable charts** (idea from MyHeritage Family Tree Builder): in the chart view, drag the boxes and edit the card texts before
  exporting to PDF/JPEG/print. Build on the report themes, and keep the manual layout saved with the chart, not in the tree data.
