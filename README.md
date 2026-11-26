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

**Usability problems found by a from-scratch walkthrough (2026-11-26).** Scenario: new empty file, enter a person with name, sex, birth,
parents, a partner, a source and a note, and build a small tree. *Method and limit:* I followed the real flow by reading the dialogs
(`IndividualRecordDialog`, `PersonalNameStructureDialog`, `EventRecordDialog`, `EventParticipationRecordDialog`, `EntityListPanel`,
`BaseRecordDialog`) and the help pages; I did **not** drive the GUI, so the dialog counts come from the code and items marked *(verify)* are
suspicions to confirm with a test or by hand. Ordered by how much they hurt a beginner.

1. **Typing a name takes three nested dialogs.** *Add Individual* shows only a *Personal Names* list. Each name opens its own dialog, whose
   *Parts* list opens one more dialog per part (type + value). "Giovanni Rossi" = individual > name > part *given* > part *family*, with no
   field to type the full name. Proposal: a one-line "Full name" in the new-individual dialog that is split (last word = family, with the
   usual particles), creating the name and its parts, editable afterwards with the full dialogs.
2. **The sex is hidden.** It is not in the new-individual dialog: it is an *attribute of type sex* in another tab (create, choose the type,
   then the value). Only *Add Parent/Child/Partner* from the tree preselect it. But the views place father and mother by sex, so the first
   person of a file, left with no sex, gives a silently wrong tree. Proposal: a *Sex* combo (male/female/unknown) in the Properties tab that
   writes the attribute.
3. **There is no birth date for a person.** It is an *event* plus a *participation*: *Event > New Event...* (type, date, place), then on the
   person *Participations > Create New...* (event, role *principal*), then save both. At least four dialogs for the most common fact, and
   the help needs a section ("Changing a date of birth") to explain it. Proposal: "Birth" and "Death" rows (date, place) in the new-individual
   dialog and in the dossier, creating the event and the participation behind the scenes. *(verify)* what the views do with a participation
   whose role is empty: the role is optional, but the birth is read as the *principal* one.
4. **A marriage is two records for one fact.** The relationship (*civil spouse*, with *Valid From*) and the marriage *event* (date, place,
   witnesses) are separate; the Assistant reports an event with no relationship, and the relationship's *Valid From* repeats the date of the
   event. The generic *spouse* and *child* types exist only because the importers have nothing better, and the editor still offers them.
   Proposal: create the union from one dialog (type, date, place) that writes both records.
5. **Citing a source is a long road.** *Sources > Create New...* opens the source (its **title is a name structure**: another nested dialog
    just to type a title), then its repository (another name dialog), then the citation (source and *Stance* required; stance is almost
    always *supports*), the *evidence* qualifiers, the *extract*. Proposal: *Title* as a plain text box when there is one name, *Stance*
    defaulting to *supports*, and a "Cite a new source" shortcut that asks only for the title and the locator.

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
