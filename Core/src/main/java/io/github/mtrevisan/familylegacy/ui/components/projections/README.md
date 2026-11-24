# Views, import/export, reports: open TODO

Only what is still to do, by priority. Done work is in the code and in the help pages (`Core/src/main/resources/help`). Decided-not-to-do
items are in git history (this file before the cleanup of 2026-11-23), to avoid proposing them again. The backlog of the Research Assistant
is in `../../assistant/README.md`.

## HIGH

- **Export GEDCOM 5.5.1 / 7.0.** Only the FLEF export exists (`ExportGedcom551Tool` / `ExportGedcom7Tool` are commented in
  `FileToolRegistry`, the classes do not exist). It will be a plugin like the importers (`FileExporter`; SPI `FileImporter` /
  `FileImporters` in Domain, jar `importer-<id>.jar` in `Core/plugins`). State what is lost (conclusions, research, context, privacy) and
  apply `PrivacyExporter`. Then a round-trip test.
- **Image export (PNG/SVG) of a view** (`ExportImageTool` is commented in `FileToolRegistry`, the class does not exist; for the PNG it is
  enough to paint the panel on a `BufferedImage`).

## MEDIUM

- **Ego network:** filter by relationship type and toggles for the groups (start from `SocialFilters`, Domain `social`, or from commit
  `eead2244cf42359aba65a1df47cf060f54e3570b`).
- **Tree:** filters and expansion of branches (useful only for big trees).
- **Pedigree collapse / consanguinity:** the ancestry with linked duplicates (the list at the top does not report it); highlight of the
  consanguinity rings on the Sugiyama graph (the layout has no hook to highlight a selection of ancestors).
- **Search:** decide whether boolean queries are worth it in `SearchService` / `SearchMatcher`, and whether a Trie helps. A global search
  that matches text in notes and citations does not say where the text is; places are not yet found through the names of the places that
  contain them (other records that point to a place neither).
- **AI-powered Research Assistant (optional layer).** The assistant today is rule-based and offline. A provider layer (`AIProvider` with
  Claude, OpenAI, Azure, ... behind it; no local model required; FamilyLegacy fully working offline) could add natural-language help:
  interpret historical date expressions, explain conflicts, draft biographies and plans. Constraints: never send the whole FLEF file, only
  the minimum context of the current task (current DateStructure/extract/place; individual + candidates + relatives + key events + sources;
  place hierarchy; conflicting records; question/activities/tasks/conclusions); never modify, create, merge or resolve anything by itself;
  every output marked as generated assistance; respect the Basic/Standard/Advanced levels; use `Ctrl+Shift+A` as the entry point.

## LOW

(only if a concrete need appears; most useful first)

- **Reports:** gaps per generation (known ancestors against possible 2^n, with the pedigree collapse via `PedigreeCollapseDetector`; partly
  in Research Progress); anniversaries calendar (births, marriages, deaths sorted by month and day, with year and age; global or per group;
  presumed-living persons are `restricted` so excluded); check whether `AncestorReportGenerator` numbers the ancestors with Sosa-Stradonitz
  (the Sugiyama `Layerer` already computes the numbers, the report does not).
- **Dossier:** filters (only event type and interval; the rest is in the sections); direct export (a report that uses it is enough); the
  provenance of an individual at dossier/report level; evidence as a graphic badge; ages at events with "~X years" for imprecise dates
  (also in the lifespans strip tooltips); duration of relationships.
- **Lifespans strip (`MultiLifespanStripPanel`):** birth and death distinct on the bar, uncertain death dashed; negative evidence (`evidence_type =
  negative`: faded body with a diagonal hatch, X on points, as `TemporalSpanRenderer` draws it in the group views).
- **Export:** CSV of subsets (`ExportExcelTool` is commented; CSV only).
- **DNA:** CSV import of segments (chromosome, start, end, cM, SNP) and a chromosome browser with paternal and maternal bars (a block of
  its own); a field for the Y-DNA label as a person attribute, in the format R-BY44535+ (the simple place to start).
- **Calendars:** inverse conversion JDN -> native date for all calendars (today only to Julian and proleptic Gregorian:
  `CalendarType.jdnToProlepticGregorianLocalDate`, `CivilDates` of the assistant).
- **Consanguinity:** the whole-file report and the assistant take 140 s in the extreme case (outside the EDT): replace the ancestor sets of
  `ConsanguinityCalculator.ancestorsOf` (`Set<String>`) with bit sets on integer indexes. Only for files closed on themselves, which a
  real file does not reach.
- **Identity assistant on a huge closed community, incremental by pair:** measured on `LargeFileBenchmark` (30,000 individuals, 355,000
  records, the worst case: everybody shares names) the identity assistant takes 26 s, consanguinity 7 s, the others under 3 s. Making
  identity incremental (only the pairs that touch an edited individual) was not done on purpose: a score depends on global things (the
  frequency of each name in the file, the trees that are connected, the ancestors within 15 generations), so a partial update would give
  other scores than a full one. Do it only if 26 s turns out to be too long on a real file; then keep the full run for when the
  frequencies or the connections changed.
- **Attributes:** a comparative matrix (only if a need appears).
- **Sources:** a source tree (only if needed).
- **Validate File:** the media type check reads the first bytes only of the formats it knows with certainty (Ogg and MP4 can be audio or
  video: not judged); the time of the media lookup is logged but a real network drive has not been measured yet; the progress of the
  structural checks is per check (a dozen steps), not per record.
