# FamilyLegacy - proposed features still open

Only what is not done yet. Already in the program (see the help pages): research questions/activities/tasks/conclusions with the Research
Board and the tasks panel, evidence qualifiers and conclusions with proof status, the proof summary and the Genealogical Proof (GPS) report, coverage as a report section and as
a dashboard (Source > Documentation Coverage), the source-centric workspace (Source > Source Workspace), the Research Assistant (rule-based, no AI vendor).

## General requirements for any of these

- Analyse the existing architecture (Domain, FLEF model, importers, reports, UI) first; reuse existing concepts; no duplicated functionality.
- Keep FLEF files backward compatible; prefer incremental evolution to rewrites.
- Keep domain concepts independent of any AI vendor.
- Tests for all new functionality; integrate with Sources, Citations, Media, Notes, Reports and Search.
- Before implementing a feature, write: affected modules/packages and refactorings; schema/entity changes and compatibility; migration
  (FLEF, reports, import/export); a risk estimate (complexity, maintenance cost, architectural impact, extensibility).

## MEDIUM

1. **CSV / spreadsheet import framework** (CSV, TSV, XLSX): column -> field mapping (e.g. `Name` -> individual name, `BirthDate` -> birth
   date, `Village` -> birth place, `Occupation` -> occupation), validation before the import, preview, saved profiles and reusable
   templates, a mapping wizard.
2. **Evidence package export:** `Package.zip` with `dataset.flef`, `sources/`, `media/`, `extracts/`, `citations/`, `reports/` and a
   `metadata.json` (creation date, FamilyLegacy version, author, counts of sources, citations, extracts, media and records). Uses: sharing
   with researchers, archival backups, collaboration, publication.

## LOW

3. **Research timeline** (the work, not the life): research task created, repository visited, source added, extract and
   citation created, conclusion updated, conflict resolved, task completed; filters by person, family, source, date range, repository.
4. **AI-ready abstractions** (`ExtractionProvider`, `InferenceProvider`, `ResearchAssistant`) without provider-specific
   integrations, and a UI that shows AI suggestions, proposed citations, possible record matches, confidence scores and explanation chains;
   final decisions always with the user. See also the optional AI layer in `README.md` of this folder.
5. **Association network view** beyond family links (witnesses, godparents, sponsors, associates, neighbors, shared sources, events and
   places; expand/collapse, shortest relationship path). Careful: the graph social network was deleted on 2026-11-17 (a list, the FAN
    panel, answers the genealogist's question better); revive only the parts a list cannot give.
