FamilyLegacy

## TODO

- **PDF report font.** The PDF renderer (`PdfReportRenderer` in the `ReportPdf` module, OpenPDF) uses the standard Helvetica and Courier fonts, which are not embedded and cover only Latin text. A report in Arabic, Hebrew, Persian, Cyrillic, Greek, an Indic script, Thai, Chinese, Japanese or Korean may show missing letters in the PDF (Word and HTML are not affected). Embed a Unicode font with a licence that allows it (for example a Noto family) and choose it by the language of the report, then check each language of `Domain/src/main/resources/i18n/report/`.

- **Enum values: files written with the old values.** The values of the enums follow `flef_0.2.0.gedg`, and every one
  has a constant in its `*Reader` (`ENUM_*`): the code does not repeat them as strings. Three values of the readers were
  aligned to the grammar and a file that already holds the old ones no longer matches them, and no migration exists:
  `neighbourhood` and `organisation` (group type, now `neighborhood` and `organization`), `closed` (research question
  status, now `abandoned`) and `unresearched` (proof status of a conclusion, removed: the status is required and a draft
  leaves it blank). Decide whether a file with the old values must be converted when it is opened. Strings that stay on
  purpose: `baptism` and `christening` in `EntityMatcher`, `ConsistencyService` and `DateConsistency` (they are not event
  types of the grammar), the keywords of `PlannerChecker`, and the words in the vocabularies of the importers.
- **Relationship Assistant: what is left.** The actions *Change type* and *Create Relationship Draft* (with the date and
  the sources of the event) are tested through the editor, not by clicking the palette. The siblings have no type in the
  grammar, so they are read from an *associate* relationship whose role says brother, sister or half-brother:
  `RoleKind` in `RelationshipChecker` knows only English and Italian words. The informant of a birth is asked about only
  when the participation has the role `informant`; a participant with no role counts as principal and is never asked
  about.

- **Menu audit: what is left to verify.** Every entry of the menu bar has been read item by item, except the ones listed
  under *Not audited*; the audit below says what was checked only by reading and compiling, or only through its logic
  (does the visible entry open something that works, is it translated through `I18N`, does the help page say what it
  does). The items below were checked only by reading and compiling, or only their logic is covered by a test, not the
  window itself:
  - **Place**: *Manage Places* (hierarchy view of `RecordSelectionDialog`), *New Place*, *Jurisdiction Timeline*. No
    test opens them.
  - **Source**: *New Source*, *New Repository*, *Manage Media* (the image preview was kept on purpose and is not
    covered), and the windows of *Add Citation* and *Citation Analysis* (only the logic, `AddCitationDialog.addCitation`
    and `CitationAnalysisTool.analyze`, is tested).
  - **Reports**: the dialog is never driven end to end (choose folder, overwrite prompt, progress, files written).
    *Bibliography* and *Research Progress* are about the selected record, not about the whole file (`ReportGenerator`
    builds every section from one root): a real whole-file bibliography needs its own section builders. The option panel
    shows every option for every report, including those the report ignores (for example the group options in the
    ancestor report), and the language list does not start from the language of the user.
  - **Research**: *Proof Summary* and the new *Without qualifiers* tab of *Evidence Qualifiers* have no test;
  *Duplicates*, *Merge* and *Statistics* were only translated. `MergeRecordsDialog` writes an English note into the file 
  ("Judged different by the researcher in the merge dialog"): decide whether it should follow the language of the user.
  - **Not audited yet** (the menu bar is covered, these are not):
    - **Tools**: *Merge Individuals* and *Consanguinity Report* (only *Detect Duplicates*, *Validate File*,
      *Statistics*, the date tools and the Research Assistant entry were looked at; *Detect Duplicates*, *Merge* and
      *Statistics* were only translated).
    - **Context (popup) menus**: the tree menu (`EntityTreePopupMenuFactory`, 13 items), the two ego-network menus
      (`EgoNetworkIndividualPopupMenuFactory`, `EgoNetworkGroupPopupMenuFactory`, 8 items each), the Agora and Chronomap
      panels, `PopupMenuHelper`, and the popups of the individual and group panels. Check, for each item, that it does
      what its name says, that it is enabled only when it can work, that it is translated, and that the help describes
      it.
  - **Everywhere**: only the English help (`help/en`) and the English UI bundle exist; the other languages of the report
  bundles are not the languages of the application.
- **Texts still in English in the code.** The user-visible texts of the windows and of the menus now go through `I18N`.
  What is left on purpose: the messages of the file validation (`FLEFValidator` and the grammar constraints in
  `io/grammar/contraints`, shown in *Validate File*), the notes the application writes into the file (the merge dialog
  and the place hierarchy: "Judged different ..."), the keywords of `ArchivalReferenceParser`, the test windows started
  by the `main` methods of the panels, and `ConsistencyService` (its tool is not in any menu). Texts that mix a language
  with the data (the event and attribute types in the dossier, written as `birth`, `marriage`, ...) still show the code
  of the file.
