FamilyLegacy

Open work only, by priority. What is done lives in the code and in the help pages (`Core/src/main/resources/help/en`). The backlog of the
views, of the import/export and of the reports is in `Core/src/main/java/.../ui/components/projections/README.md`, the one of the Research
Assistant in `Core/src/main/java/.../ui/assistant/README.md`, the proposed roadmap in `.../projections/NEWFEATURES.md`.

# FamilyLegacy "WOW Features" Roadmap

This roadmap focuses on capabilities that significantly differentiate FamilyLegacy from traditional genealogy applications by leveraging the unique strengths of FLEF: evidence, conclusions, hypotheses, historical context, and research workflow.

The emphasis is not on storing more data, but on helping genealogists reason about data.

---

# Phase 1 — Research Workflow Engine

Goal:
Guide research rather than simply recording it.

## 1. Research Gap Analysis

Automatic identification of:

- missing births;
- missing deaths;
- missing sources;
- incomplete chronologies;
- unresolved identity problems.

Generate:

- suggested tasks;
- suggested searches;
- suggested repositories.

---

## 2. GPS Workflow Support

Implement Genealogical Proof Standard-oriented workflows.

Support:

- research question;
- evidence collection;
- conflict analysis;
- conclusion creation;
- proof statement generation.

Provide:

    What is still missing before this conclusion can be considered reasonably proven?

This is one of the most distinctive features FamilyLegacy can offer.

---

# Phase 2 — FAN Network Intelligence

Goal:
Reveal connections beyond direct family relationships.

## 3. FAN Network View

Use:

- witnesses;
- godparents;
- neighbours;
- employers;
- employees;
- associates;
- shared events;
- shared places.

Provide graph visualization and reports.

Example:

    Why are these two individuals connected?

Answer using:

- relationships;
- shared events;
- shared groups;
- places;
- sources.

---

## 4. Associate Discovery

Allow identification of recurring associates.

Examples:

- witness appears in many family events;
- repeated neighbours;
- repeated godparents;
- recurring migration companions.

Highlight possible research leads.

---

# Phase 3 — Historical Context Engine

Goal:
Place people inside history.

## 5. Historical Overlay Timeline

Combine:

- personal events;
- family events;
- local historical events;
- national historical events;
- world historical events.

Use:

    HistoricEventRecord
    ContextImpactRecord

Provide:

- interactive timeline;
- report section;
- PDF rendering.

---

## 6. Place-Aware Historical Context

When viewing a person, event, or place, automatically surface relevant context.

Examples:

    Napoleonic administration
    Austrian rule
    Civil registration introduction
    Military conscription periods

Prioritize context connected to associated places.

---

## 7. Lifespan Comparison Charts

Display:

- multiple individuals;
- historical events;
- family milestones.

Allow comparisons across generations.

Export:

- SVG
- PDF
- report sections

---

# Phase 4 — Intelligent Publishing

Goal:
Generate publication-quality genealogical works.

## 8. Book Builder

Combine multiple reports into:

- PDF
- DOCX
- EPUB
- HTML
- Markdown

Support:

- cover page;
- introduction;
- table of contents;
- combined indexes;
- continuous pagination.

Reuse existing report generators.

---

## 9. Story Integration

Support narrative stories within:

- individual reports;
- family books;
- biographies.

Stories should:

- render as narrative sections;
- support Markdown;
- be searchable;
- be optionally included in reports.

Implement via:

    Note + classification

before considering a dedicated StoryRecord.

---

## 10. Narrative Numbering Systems

Provide:

- Ahnentafel
- d'Aboville

and other genealogical numbering systems where appropriate.

Use sensible defaults per report type.

---

# Phase 5 — AI-Powered Reasoning

Goal:
Use AI to assist research while keeping all reasoning transparent.

## 11. Explain My Conclusion

User asks:

    Why was this conclusion generated?

Provide:

- supporting evidence;
- contradictory evidence;
- confidence;
- research gaps.

AI must never hide the evidence chain.

---

## 12. Historical Interpretation Assistant

User asks:

    What was happening here when this person lived?

Use:

- historic events;
- cultural norms;
- place history.

Generate contextual explanations linked to FLEF records.

---

## 134. Research Advisor

Provide recommendations such as:

- which source type to search next;
- which repository may help;
- what evidence is missing;
- which identity hypothesis deserves attention.

All recommendations must be grounded in existing data and accompanied by explanations.

---

# Ultimate Goal

FamilyLegacy should become:

    Not only a genealogy database.

    Not only a genealogy report generator.

    But a genealogy reasoning and research environment.

The defining experience should be:

    "Show me the evidence."
    "Show me the conflicts."
    "Explain the conclusion."
    "Tell me what to research next."
    "Put this person into historical context."

Every major feature should reinforce those capabilities.


## TODO

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

3. Evaluate later (niche or heavy): genogram and health history view (GenoPro, RootsMagic), DNA tests and Leeds Method report, cemeteries and
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
