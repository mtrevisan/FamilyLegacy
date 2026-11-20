# Research Assistant (`ui/assistant`): state and backlog

**Two modules (November 2026).** The knowledge is in the Maven module `Assistant` (package `io.github.mtrevisan.familylegacy.assistant`, with the
same sub-packages as below: `identity`, `gap`, `chronology`...): the `*Checker`, `*Data`/`*DataLoader`, `*Finding`, `*Draft`, `Diagnostic` and its
keys, `DiagnosticProvider`, `AssistantContributor`, `AssistantTuning`, `IgnoredFindings`, `FindingsExport`... It knows nothing of Swing nor of
`ui` (`PackageDependenciesTest` and the compiler keep it so) and its context is `AssistantContext`, an interface with the model, the file and
the selected record (`PlainAssistantContext` for the tests and the command line). Core (`ui/assistant`) keeps the palette, the dialogs, the
contributors that offer actions that open editors and those actions (`Create*DraftAction`, `CompareRecordsAction`...): they use
`SwingAssistantContext` (window and focus owner) through `SwingAssistantContext.of(context)`. A checker never builds an action that opens a
dialog: it reports the `Diagnostic` with no action and the contributor in Core adds it with `Diagnostic.withAction`. The paths below that name
classes of the engine are now in `Assistant/src/main/java/.../assistant/`.

Moved from `ui/components/projections/README.txt`. The backlog of the views stays in that file; here is the detail of the
assistants. The "Still to do" lists of each assistant are summarised, by priority, in the "TODO aperti" section of the README of the
projections.

## Open TODO, grouped

The detail (and the reasoning) of each item is in the "Still to do" of its assistant below; this is the map. Within a group the
order is the priority. "NEW" marks items added in the review of November 2026.

### A. Calibration of the numbers
The collection is done (November 2026): see "The statistics of the assistant" in the notes of the framework. What is left is to use it:
1. Collect real counts from the users (they copy them from Settings > Research Assistant... > Statistics... and send them).
2. Read them by kind: a kind listed often and acted on never has a weight that is too high (or it is noise to be dropped); a kind acted on
   at once, and from far down the list, deserves a higher weight; a kind ignored by kind or by record is not wanted. Group the kinds (the
   gaps of birth, of death, of sources...) before judging: with a few users the rare kinds have no statistics.
3. Change the defaults of `AssistantTuning` (the counts say which numbers they were made with) and write what was learned here.

### B. Language
1. Languages: English only, on purpose for now (decided November 2026): the user will choose the languages at the end of the work. Then:
   `messages_<lang>.properties` for the ~700 keys of the assistants, `AssistantKindLabelsTest` for each language, and the help pages
   (`help/en` only today). Done meanwhile (November 2026), so that translating is only translating: the text of the assistant is no longer
   joined in the code (`Sentences`: the join of two sentences, the separators of a list, the status line; `context.why.join`,
   `identity.match.line`, `assistant.group.count`, `assistant.indicator.text*`), the "(s)" plurals of the assistant are ICU plurals, and
   `LanguageNeutralTextTest` keeps it so. Left: the "(s)" of 12 other messages (text.*, dialog.*, message.*) outside the assistant, the
   identifiers in square brackets and the locator labels of `ArchivalReferenceParser` (data, not language), and the grammar that the code
   cannot know (the gender of "high"/"medium"/"low" next to a noun).
2. Date language of a historical date: on hold, see the memory note on the date language priority.

### C. New checks and richer evidence, by assistant
- Chronology: a spouse older than the lifespan (warning only); overlapping incompatible events and attributes (residence, military
  service, prison, hospitalization; assistant warning only, never the validation).
- Relationship: more evidence for the type (evidence qualifiers of the sources, ages of the parents, surname of the father; foster and
  customary: a prompt only); other checks (spouse of a parent not linked to the child, half-siblings that contradict the parents, the
  associates; provable ones go to Validate File); drafts for a child of the spouse of a parent and for the parents named in a birth
  record.
- Conclusion: attributes that change in time (residence, occupation, citizenship, religion) need `valid_from` / `valid_to` to tell a
  change from a conflict; `children_count` and `marriages_count` against the tree; civil and religious marriages that differ in date;
  parentage by different couples in different sources; a stale hypothesis conclusion (evaluated before new evidence arrived); a
  hypothesis with a candidate that is not an individual.
- Place: places with no type (from the name), `successor_of` from names and dates, relationships outside the lifetime of a place, usage
  of places by records other than events, hierarchy from multi-part names, parent from nearby places.
- Gap: the places of the residences for "place known, birth record missing", residence histories incomplete between two censuses;
  baptisms and burials are not vital events here.
- Context: the date and the place of the draft of a historic event; migrations; naming conventions (patronymics); the age rules of the
  norms (minimum age of marriage); attributes and residences as events; a catalog of historic events and norms to import; norms that
  apply to a conclusion or a relationship.
- Planner: places and periods (which parish and archive keep which registers for the years: needs the repositories), the conclusions
  of a question, a due date, the order of the steps as a plan (dependencies: marriage before baptisms), questions about places and
  families.
- Citation: media metadata (EXIF, PDF properties, ISBN), whole repository citations from the reference, a locator per media type,
  learning the user's abbreviations, a draft of the whole SourceCitation.
- Dates: regnal years (they need tables from an authoritative source, see the rule on calendars).
- Identity: the pairs of the other kinds are shown without feature-by-feature evidence.
- NEW, across assistants: the drafts are plain (no suggestion of where to look, no due date, no link of a question to the tasks that
  would follow). One shared "draft enrichment" step (repositories of the place, sources of the relatives) would improve the Gap,
  Planner and Identity drafts at once instead of three times.

### D. Not tested (needs the interface)
The real click that opens the palette, the look of the indicator button (its colour is tested, its painting is not), the blinking of
the indicator on a new error (the timer is tested, the decision needs the button on screen), the check box "Only about ...", the
category box and its key Ctrl+L, the menu of Ctrl+Shift+D and its right click, Ctrl+G (the choice of the record is tested, the window
that shows it is not), the file choosers of the exports and of the ignored findings, the reopening of the palette with its notice and
selection, the keys Ctrl+D / Ctrl+I, the `AppPreferences` adapters of the ignored findings and of the settings (they would write the
preferences of whoever runs the tests), the two editors of `RecordDifferentAction` in a row. NEW: the Swing visual-check method (paint
the palette to a PNG from a test, see the memory note) would cover the first four without a real display.

### E. Decided not to do
- Names in another language (Giovanni, Johann, Jean) and patronymics via a table: too many names to keep up; `NameSimilarity` covers
  spelling variants without a table.
- "Proven with a single source" as a finding: it can be legitimate.
- The sex, and the type of a relationship between sources, as conflicts; events of groups and of participants that are not
  individuals (no birth or death to compare).
- "Banns mean religious, license means civil" as a rule: a convention that changes with country and period; it stays a weak hint.
- An icon of the assistant in every dialog (grey where it does not apply): a grey icon says "disabled".
- Brick walls as gap findings: they are the job of the Planner.
- The age of a death estimated from a known birth in the Date Assistant: arithmetic the user does alone.

## Assistants

### Implement a global "Research Assistant" system for the genealogy application.

Status: the framework and the first assistant (dates) are implemented, in `ui/assistant`.
- `ResearchAssistantService`: registry of the assistants, search, diagnostics, global shortcut.
- `ResearchAssistantPalette`: the palette (Ctrl+Shift+A, also Tools > Research Assistant...).
- `AssistantAction` / `AssistantContributor` / `DiagnosticProvider` / `Diagnostic`, `AssistantContext`, `AssistantCategory`.
- `FuzzyMatcher`, `DateAssistantContributor` (the Historical Date Assistant, see below).
- `DiagnosticAction`: what the user can do about a finding (Enter on it in the palette), e.g. start the draft of a conclusion.
- `DiagnosticActions`, `OpenRecordsAction`, `RecordChooserDialog`: every finding about records can open them. Enter runs the first
  action of the finding (the one it carries, e.g. the draft of a conclusion, otherwise the opening of its records), Ctrl+Enter
  (Cmd+Enter on a Mac) the second one; the keys are shown under the explanation. A finding about one record opens its usual editor
  (`HandlerRegistry`, `createEditDialog`); about several, a list that stays open while an editor is, to open them one after the
  other and compare them. ESC closes the list. Records that no longer exist are left out. Nothing changes unless the user edits a
  record and saves it in its editor.
- A finding can carry more than one action of its own (`Diagnostic.alternatives`, after the main one): the keys are Enter, Ctrl+Enter,
  Ctrl+Shift+Enter and Ctrl+Alt+Enter (at most four actions), and the opening of the records is always the last. The key belongs to the
  action, not to its position (`DiagnosticAction.keySlot`: 0 what the finding proposes, 1 compare, 2 record as different, 3 open the
  records), so that "Compare Side by Side" is Ctrl+Enter wherever it is offered; Enter also runs the first action when none has slot 0. `Diagnostic.rank` orders the findings of the same severity (the
  higher first); the severity still comes first.
- `AssistantIndicator`, `FindingCounts`: the indicator of the main window, a button at the right end of the toolbar: "Assistant" and
  the findings to look at by severity (`✖ 1  ⚠ 3  ℹ 12`, or a tick when there are none, `…` while it is checking); the tooltip says it
  in words and gives the shortcut; a click opens the palette. The ignored findings are not counted. It exists because the assistant is
  always available with Ctrl+Shift+A, and the way to learn it is to see that it has something to say; an icon in every dialog (grey
  where it does not apply) was not done: the assistant is available everywhere, what varies is the actions, and a grey icon says
  "disabled".
  - Up to date without getting in the way: the findings are computed away from the event dispatch thread, only when the model
    changes (`ModelHistory.addChangeListener`: a step recorded, an undo, a redo, another file), 2 s after the last change, never while a
    modal dialog is open, one computation at a time (a change that comes meanwhile asks for another afterward). A computation that took
    long is not repeated at once: the wait is 4 times how long it took, at least 5 s, so on a big file it does not keep the processor
    busy. Ignoring a finding (or showing it again) only recounts the findings already computed (`IgnoredFindings.addListener`).
- `IgnoredFindings`, `DiagnosticKeys`: Ctrl+D (Cmd+D on a Mac) on a finding ignores it, Ctrl+I shows the ignored findings (Ctrl+D
  there shows one again); the palette tells how many are ignored. The keys are not Delete (it edits the search field). Ignoring is a
  presentation choice of the user, like the order of the children: it lives in the user preferences (`AppPreferences`, one entry per
  file, by the path of the file) and NEVER in the records: nothing is created, modified, deleted or exported. A finding is ignored
  through its `Diagnostic.key()`, built from its kind, its records and the data it reports (names, dates as written, counts: never
  localized text), so it is the same in every language and it comes back by itself when the data changes (the date is corrected and the
  finding is still there, a record is added to the conflict), but not when only the suggestion changes. The preferences keep digests
  (16 hex characters of a SHA-256) of the path and of the keys, not names or dates. A file never saved has no identity: its ignored
  findings last for the session. The tests use an in-memory store, never the preferences of whoever runs them.
- `ResearchAssistantService.recalculate`, `reopenIfChanged`: after the user runs an action on a finding (the editors it opens are
  modal) and finishes with it, the findings are computed again away from the event dispatch thread and compared with the previous ones by
  `identity` (the key, or title and records for a finding without one). If they changed, the palette opens again where it was (same
  search, same view, the same finding selected or the one that took its place) with a notice "N resolved, M new"; if nothing changed,
  nothing opens and the user is left to their work. Ignoring (Ctrl+D) is not an edit and does not recalculate. What the user ignored
  stays ignored across the recalculation, because the ignored findings are told by their key.

Design decisions that refine the text below:
- The shortcut is a global KeyEventDispatcher, not a menu accelerator: the editors of the dates live in modal dialogs that have no
  menu bar, so an accelerator would not fire exactly where the assistant is most useful.
- The context is a snapshot (`AssistantContext`: window, focused component, model) captured BEFORE the palette takes the focus.
  An action finds the editor with `focusedAncestor(SomeInterface.class)`: the editor implements a small host interface (e.g.
  `HistoricalDateAssistantHost`), so the assistant never depends on the Swing panels.
- Three extension points: actions, context (through the host interfaces) and diagnostics. The built-in assistants are registered in
  `ResearchAssistantService.createDefault()` (one line each, like `MaintenanceToolRegistry`); the other ones are `ServiceLoader` providers
  (`uses AssistantContributor` in module-info), so a plugin never changes the code of the assistant.
- Diagnostics are computed away from the event dispatch thread (a provider can inspect the whole model, and must not touch Swing);
  a provider that fails is skipped.
- Search is fuzzy over title, category name and description, so typing a category name filters by category. Without a query the
  order is the relevance in the context, with a query it is the match score.
- Tools that are variants of one another are one action with a selector: the Venetan, Florentine and Pisan converters are the
  `Calendar Conversion` tool of the Historical Date Assistant (a calendar combo), not separate entries. The Roman calendar
  converter will come with the natural-language interpreter.
- Every action must respect the data safety rules below; a test cannot enforce this, so it is a review rule.

- Showing the record in the main window (done, November 2026): Ctrl+G on a finding about an individual closes the palette and makes that
  individual the root of the view (`ResearchAssistantPort.installRecordViewer`, `FamilyLegacyFrame.loadRoot`;
  `ResearchAssistantService.viewableRecord` picks the first individual of the finding). Only individuals: a finding about a place or a
  source has no view to go to. It makes the individual the root of the view; it does not select it in the view without moving the root.
- The selected record (done, November 2026): the main window gives the identifier of what is selected (`ResearchAssistantPort.install`,
  `FamilyLegacyFrame.currentSelectionId`), `AssistantContext.selectedRecordId` carries it (also in the copy for reading), and the palette
  puts the findings that name the record first (`ResearchAssistantService.aboutRecord`) and offers "Only about ...". Opened from an editor
  (a dialog) there is no selection. Not tested: the check box in the real palette (the ordering and the context are: `SelectedRecordContextTest`).
- The statistics of the assistant (November 2026, `AssistantStatistics`, `AssistantStatisticsDialog`, Settings > Research Assistant... >
  Keep counts..., Statistics...): off until the user turns it on (`AssistantSettings.isStatisticsEnabled`); for each kind of finding
  (`Diagnostic.kind()`) the counts of listed (once for each finding the palette lists when it opens), acted on (with the position in the list),
  records opened, shown in the window, ignored (alone, by kind, by record), put aside and shown again; nothing else: no name, date,
  identifier or text, and the text of the file is checked to hold only kinds and numbers. They are cumulative over the runs: the file
  `assistant-statistics.txt`, next to `preferences.properties`, is read the first time it is needed and written 30 s after a change and when
  the application ends (atomically; a file that is not ours or is damaged is started again), so two applications open together would
  overwrite one another. They are kept for each set of numbers of the assistants (a digest of `AssistantTuning`, written with the numbers
  that are not the defaults), because counts made with other weights say something else. Never sent: the user copies the text. The
  indicator does not count (it is not an action of the user). Not counted: the time before acting, the finding that was not looked at (the
  position of the listed ones is not kept, only of the ones acted on), the editing that follows an action (it is another record).
- The numbers of the assistants (November 2026, `AssistantTuning`, `AssistantSettings.tuning()`, Settings > Research Assistant..., one tab for
  each assistant): the age limits and the times of the chronology (`ChronologyChecker.Rules.of`), and for the gaps, the plan and the context
  the years after which an individual is taken to be dead, the generation, the limits of the levels (high, medium) and the weight of
  each kind of finding. Each parameter has its range, kept by `AssistantTuning.with` (and so by the dialog); the limits are kept in order (a
  medium level never above the high one, a probable death never after the certain one). The checkers take them like `IdentitySettings`: a
  `Supplier<AssistantTuning>` in the contributor (the preferences by default, the defaults in the tests) and an overload of `findings`
  that takes the numbers; the old overloads and constants are the defaults. The bonuses of a weight (+10 for children, +5 for events,
  +15 for a high priority...) and the caps (a weight + 25 for the couples, + 45 for the brick walls, + 30 for the clusters) are fixed. The
  aggregation thresholds (one finding for each individual up to 25, one for all beyond, 200 listed) that this file described for the
  Gap Assistant are not in the code any more: there is nothing to configure. Adding a number: a `Parameter`, its words in the messages
  (`assistant.tuning.<name>`), and its use in the checker.
- How long each assistant takes (November 2026): `ResearchAssistantService.lastDurationsMillis()` has the time of each one the last time
  it was asked (shown in Settings > Research Assistant..., column "Last time"), the log has them at debug level, and at info level
  when the whole computation took 2 s or more. Measured on the synthetic file of `LargeFileBenchmark` (20,000 individuals, 237,000
  records, the worst case: a closed community): Identity (individuals) 232 s, Consanguinity 101 s, Conclusion 2.4 s, Relationship
  1.4 s, Planner 0.8 s, Chronology 0.8 s, Gap 0.7 s, Context 0.2 s, Place 0.06 s, Identity (other kinds) 1.2 s: only the first two
  matter.
- The findings that are kept (`DiagnosticProvider.dependsOn()`, `environment()`; `ResearchAssistantService.keptFindings`): a provider that
  says which kinds of record its findings depend on is not asked again while the records of those kinds are the very same ones in the
  copy of the model (the copies that `ModelHistory.readOnlyView` gives share the records that did not change, and nobody changes
  them, so the same record is the same content; the same content in another record just computes again), the date is today and
  its environment (the thresholds of the user) is equal. Never kept: a context that is not such a copy, a provider that does not
  declare its kinds (the default: the other assistants cost seconds, and a wrong list gives stale findings), and anything while a
  privacy policy other than "show all" is in force (what is hidden depends on the relatives of a record). Declared today: Consanguinity
  (individuals, relationships) and the Identity individuals (individuals, relationships, participations, events, attributes, places,
  sources, hypotheses, conclusions). `DependencySoundnessTest` takes away, one at a time, each kind that is not declared and checks that
  the findings do not change: add every new declaration to it. An assistant that reads the record selected by the user, or the header,
  must not declare anything.
- The indicator computes the assistants it is limited to at each change, away from the event dispatch thread. Settings > Research Assistant... (`ResearchAssistantSettingsDialog`,
  `AssistantSettings`, kept in the preferences) can hide it (hidden, it computes nothing) and limit it, and the palette, to some assistants
  (`AssistantContributor.id()`; `ResearchAssistantService.Audience`: an assistant off for the palette is off for the indicator; the
  computation shared by the palette and the indicator is shared only if they ask for the same assistants). Switching an assistant off only
  saves its time: its tools stay in the palette. The model is read away from the event dispatch thread on a copy of it taken before leaving
  the thread (`AssistantContext.forReading`, `ModelHistory.readOnlyView`), so the user can go on editing, with or without a modal dialog.
  The text of the indicator is red with an error and amber with a warning (`AssistantIndicator.severityColor`, for a light and a dark
  theme), and it blinks for about four seconds when the errors go up after a computation (not at the first one; it can be turned off).
- Not tested: the real click that opens the palette, and the look of the button (the wiring in the window, the counting, the pacing, the
  ignoring and the model signal are: `AssistantIndicatorTest`, `AssistantIndicatorInFrameTest`, `ModelHistoryListenerTest`).
- The palette opens again whenever the findings changed after an action, even if the edit was not about the finding the user acted on
  (e.g. they fixed another record in the list); the user can turn it off in Settings > Research Assistant... (`reopenAfterAction`). The
  reopening itself (the notice, the selection) is not tested, it needs the interface; the comparison is (`RecalculationTest`).
- The findings are computed once for the palette and the indicator when they ask for the same assistants (the palette finds the ones of
  the indicator already done, and waits for them if the indicator is still computing); edits made while the palette is closed are seen by
  the indicator, so the palette opens on findings that are already there. With the indicator hidden or limited the palette computes at
  each opening.
- A finding of the kind `assistant.provider_failed` (a warning, category Diagnostics) tells that an assistant stopped with an error while
  it looked at the file; the cause is in the log (`ResearchAssistantService.compute`). On a recalculation it is a new finding like the
  others. It is a finding in the list, not a line of the footer: so the indicator counts it too.
- The category filter: a box under the search field (all the categories of `AssistantCategory`, Ctrl+L for the next one) that limits the
  actions and the findings; it is kept when the palette opens again after an action (`State.category`).
- Ignoring (`IgnoredFindings`): by finding (Ctrl+D), by kind (every finding with the same `Diagnostic.kind()`), by record (every finding
  whose records include the first record of the selected one: not an identity), and put aside until a date (a week, a month, three
  months, a year: `z:<date>:<digest>`); the menu is Ctrl+Shift+D or the right click. Showing a finding again lifts also the rules that
  cover it, which shows the others they hid. Entries are digests (`k:`, `r:`, `z:` prefixes); a file that is renamed or moved loses
  them (per path), but "Save the Ignored Findings..." and "Add Ignored Findings from a File..." (in the ignored view;
  `IgnoredFindings.export`, `importFrom`) carry them to the new path or another machine. The entries are still per user. Not done: a
  list of the rules in force to lift one without going through a finding it hides.
- The export of the findings (Ctrl+E) is a table (CSV, UTF-8 with a byte order mark) or a self-contained page (HTML; `FindingsExport`,
  chosen by the filter or the extension of the file).
- The adapter of `AppPreferences` (two methods) and the keys of the palette (Ctrl+D, Ctrl+I) are not tested: the first would write the
  preferences of whoever runs the tests, the second needs the interface. The logic is tested with an in-memory store.

Open the records of a finding: done for every assistant (see `OpenRecordsAction` above).
Still to do: what remains is in the "Open TODO, grouped" above and in the "Still to do" of each assistant. (The selected record in the
context is done, see above.)

Data Safety Rules
-----------------
The assistant must never:

- automatically merge records
- automatically create conclusions
- automatically modify assertions
- automatically resolve conflicts

The assistant may:
- generate drafts
- propose fixes
- create suggestions
- open dedicated workflows

All genealogical decisions remain under researcher control.

Implementation Notes
--------------------
- Create a reusable ResearchAssistantService.
- Context providers should be extensible.
- Action providers should be extensible.
- Diagnostics providers should be extensible.
- Future assistants must be registerable without modifying core assistant code.
- Add clear comments explaining that the assistant is a research aid and never an authority.

The long-term objective is to make the Research Assistant the primary entry point for advanced genealogy tools while keeping the main user interface simple and uncluttered.

### DONE Create a Historical Date Assistant accessible from every DateStructure editor.

Status: `HistoricalDateAssistantDialog` (Core, `ui/components`) over the engine in `io.github.mtrevisan.calendars.assistant`
(Calendars module). It opens from the Research Assistant (Ctrl+Shift+A in a date editor; category Dates, one action per
tool). Tools: calendar conversion, liturgical calendar (movable feasts, feast references, distance from Easter, Julian computus,
Gregorian reform by country), sunrise/sunset with equal and temporal hours and the readings of the day boundary, relative date,
date relative to an event of the tree, age to estimated birth (completed years or Nth year of life; range or exact), Roman date
(Kalends, Nones and Ides: `RomanDateTool`, "ante diem III Nonas Martias", "pridie Kalendas Ianuarias", "the Ides of March", in a year;
November 2026; in a leap year the classical reckoning with the doubled sixth day is given as well as the arithmetic one;
`RomanDateToolTest`).
Every result is a `DateSuggestion` (meaning, assumptions, calendar, steps, a date or a not-before/not-after range): nothing is
applied unless the user asks, the replacement of an existing date is confirmed, and the explanation can be copied.
Still to do: regnal years (they need tables from an authoritative source, see the rule on calendars). Not done, by choice: age with a
known birth to estimate the death date (arithmetic the user does alone); "more veneto" is already in the converters.

### DONE (core checks) Create an Identity Assistant for duplicate detection.

Status: `ui/assistant/identity`. A diagnostic provider of the Research Assistant (category Identity): `IdentityChecker` (pure: turns
the pairs into findings), `IdentityAssistantContributor`, `CreateIdentityHypothesisDraftAction`, `CompareRecordsAction`. It does not
have a comparison of its own: it uses `DuplicateFinderService`, the one of Tools > Detect Duplicates (names, parents, children, spouses,
siblings, years of birth and death with a tolerance, events, attributes, sources, places; blocking by name, sex as a hard filter).
Done:
- Proposes the pairs of individuals with a similarity score of 60 or more, the 100 best. A pair of 90 or more is a warning, the others
  information; the findings are listed by score (`Diagnostic.rank`). A pair that already has an identity hypothesis is not proposed
  again.
- Each finding gives the score, the confidence it suggests (90 and more high, 70 and more medium, else low: conventions), what makes
  the two look alike (feature by feature, with its weight) and what makes them differ, and the two records. It says that it is a
  possibility, not a probability, and that nothing is merged.
- Four actions (Enter, Ctrl+Enter, Ctrl+Shift+Enter, Ctrl+Alt+Enter; the third one, "Record as Different People", came later, see below): "Create Identity Hypothesis Draft" (the editor of a new IdentityHypothesisRecord
  with the two individuals, the reasons as the start of the rationale and the confidence; only a draft: nothing is saved unless the
  researcher confirms, cancelling leaves the model as it was; once saved the pair is not proposed again, and the palette, reopened by
  the recalculation, shows the finding as resolved), "Compare Side by Side" (the read-only comparison of the application), and the
  opening of the two records.
- Ignore the candidate: Ctrl+D, like every finding. The key is the pair of records, not the score: a pair the user ignored does not come
  back because a source raised its score. To say that two records are different people for good, the protocol's way is a conclusion that
  disproves the hypothesis (a pair disproven is never proposed again by the merger).
- Fixed on the way (a bug of the duplicate finder, not of the assistant): the years of birth and death were never read from the dates of
  the protocol (it looked for a field called `value` holding a string, but a date is value > point > value > full_date), so the finder
  scored the name only and ignored 25 points of dates, in the assistant and in Tools > Detect Duplicates. It now uses the date normalizer
  (the text reading stays as a fallback). `IdentityAssistantTest` has the regression.
Done since (November 2026, in this order):
- "Record as Different People" (Ctrl+Shift+Enter; "Different Records" for the other kinds): the way of the protocol to say that two records
  are not the same for good is a conclusion that disproves the hypothesis, so `RecordDifferentAction` opens the editor of the hypothesis
  (a draft, with the reasons and the shared sources) and, when the researcher has saved it, a conclusion that resolves it, with issue
  `identity`, proof status `disproven` and a narrative that lists, feature by feature, why the two looked alike and what tells them apart.
  Cancelling the first editor stops there, cancelling the second leaves the hypothesis alone; nothing is saved unless confirmed. A pair
  disproven this way is never proposed again (the matcher of the merger vetoes it). The palette has a fourth key, Ctrl+Alt+Enter
  (Cmd+Option+Enter on a Mac): `ResearchAssistantPalette`, `DiagnosticActions` (a finding has at most four actions).
- The draft carries the sources that both individuals cite, as supporting citations of the hypothesis (`IdentityHypothesisRecordDialog
  .withSources`; the ones that do not exist are left out), and the reasons one for each feature (`IdentityDraft.similarities`,
  `differences`, `sharedSourceIds`), used in the narrative of the conclusion. `IdentityDraftContentTest`.
- Other kinds: groups, sources, repositories, media, cultural norms and historic events are compared with the matcher of the
  automatic merge (`RecordAbsorber.duplicatePairs`: one index for the whole model, compared within the blocks they share, blocks over 300
  skipped, disproven pairs left out), with a second provider (`IdentityOtherKinds`): probability of 70 % or more, 25 pairs for each kind at
  most, 90 % and more a warning; the same four actions; the hypothesis editor now accepts all the kinds of candidate of the protocol.
  `IdentityOtherKindsTest`. The places are NOT in this list (November 2026): the Place Assistant owns the duplicate places (its own
  rules: name, type, hierarchy, distance), so the same pair is never proposed by two assistants (`IdentityOtherKinds.KINDS`,
  `thePlacesAreLeftToThePlaceAssistant`). The cost: a pair of places has the comparison and the opening of the records, but not the
  draft of an identity hypothesis nor "Record as Different Records" (a false pair is ignored with Ctrl+D).
- Names in another language (Giovanni, Johann, Jean) and patronymics: NOT done, by choice. It needs a table of equivalent
  names, and there are too many names to list and keep up to date; a first version with a short table was written and removed. The
  names agree if they are equal or spelling variants by the similarity of the letters, which needs no table: `NameSimilarity` (Jaro-Winkler,
  0.92 as in the merge; 0.85 if the two have the same consonant skeleton, which catches Catarina/Katarina; a name under 4 letters must be
  equal; an initial agrees weakly), used for the name of the individual itself (25 both exact, 20 if one agrees by similarity, 15 with an
  initial, 10 / 6 for a single name) and, as `TextSimilarity`, in the second pass; the blocking has the consonant skeleton too.
  `NameSimilarityTest`. Before this, the name of the individual was compared for equality only, unlike its relatives.
- The settings: similarity from which a pair is proposed (60), most pairs (100), scores of the high (90) and medium (70) confidence, and
  probability for the other kinds (70 %) are in the preferences (`IdentitySettings`, `AppPreferences.intValue`) and in Appearance >
  Identity Assistant... (`IdentitySettingsDialog`); the tests give their own settings and never read the preferences of whoever runs them.
  `IdentitySettingsTest`, `IdentitySettingsDialogTest`.
- The second pass by name is complete: parents, spouses, children, places, occupations, the other attributes (residence, religion...) by
  text for the same type, and the titles of the sources (a title agrees only if equal or a spelling variant, never because it is part of
  another). Parents 12, spouses 8, children 8, places 4, occupations 4, attributes 3, source titles 3; only when the two individuals share no
  record of that kind. `IdentitySecondPassTest`, `IdentitySecondPassMoreTest`; the 65 of a pair with only names and dates is unchanged.
- "Compare Side by Side" is tested (`CompareRecordsActionTest`: the dialog is opened and closed by a timer; it changes nothing, and a record
  that is gone or a missing model open nothing).
Still to do:
- The same name in another language is not recognized (see above): two records of the same person written in two languages are told
  apart by their names, and found, if at all, by the dates, the relatives and the places.
- The pairs of the other kinds are not shown with the evidence feature by feature (the matcher gives one line for each, as text).
- The duplicate places have no identity-hypothesis draft and no "Record as Different Records" (see above): decide whether the Place
  Assistant should offer them.
- Measured (November 2026, a synthetic closed community of 20,000 individuals and 237,000 records, `LargeFileBenchmark`): the
  comparison of the individuals takes about 230 s (the other kinds, 1 s). It is the slowest assistant; it is kept while the kinds of
  record it reads do not change (see the notes of the framework). Making it incremental by pair (only the pairs that touch an edited
  individual) is the next step if 230 s after a change to an individual is too long.
- The narrative of the disproving conclusion is in English, and a hypothesis that is rejected is proposed with low confidence.
- Not tested: the real click on the hypothesis editor and then on the conclusion editor in a row (each editor is; `RecordDifferentAction`
  is tested up to the narrative).

### DONE (core checks) Create a Research Gap Assistant.

Status: `ui/assistant/gap`. A diagnostic provider of the Research Assistant (category Research): `GapChecker` (pure rules, with the
date of today as an input), `GapDataLoader` (reads the model, leaves out what privacy hides), `GapAssistantContributor`,
`CreateResearchDraftAction`. Tested on hand-built data, on a model converted from GEDCOM with research records added by hand, and on the
two real editors.
What the other assistants report is not repeated: an identity hypothesis with no conclusion is a finding of the Conclusion Assistant.
Done:
- Missing from the tree, with the draft of a research QUESTION (what to find out, about whom; the question is only started):
  - a birth missing for someone known at a place (an event other than the birth with a place, `GapData.Person.places`, the first three
    named): "Place known, birth record missing", usefulness 5 more than the same gap with no place, and the draft says to look first in
    the birth and baptism registers of that place and of the neighbouring ones (November 2026; `GapCheckerTest`, `GapAssistantTest`);
  - no death for an individual born 110 years ago or more (certainly dead, usefulness 85) or 90 years ago or more (probably, 55);
  - no birth for an individual with children or events (45, 55 with children, +5 with events);
  - children whose two parents are linked by no union and no marriage event (45 + 5 for each child, 70 at most).
- Where the research is thin, with the draft of a research TASK:
  - vital events (birth, death, marriage) that no source cites: one finding for all (70);
  - individuals in a census with no residence recorded: one finding for all (40);
  - an open question that no activity works on (75; 45 if on hold); an open question whose activities are all over, with no task
    open and no conclusion (65); a completed search that found nothing, was inconclusive or could not be done, with no follow-up and no
    activity that originated from it (50); the completed activities with no result: one finding for all (25, no draft);
  - a task not done and past its due date (a warning, 70, +15 high priority, +5 normal; no draft: the task is there already).
- Each finding has an estimated usefulness (0-100, high from 70, medium from 45), given in the explanation with the warning that it is
  by rules of thumb and not a judgement of the research, and used as the rank that orders the findings.
- The gaps that are about each individual (no death, no birth, no marriage) are one finding for each up to 25, and one finding for all
  beyond: an imported tree with hundreds of ancestors with no death would bury every other finding and fill the indicator. The aggregate
  has the usefulness of the most useful, lists up to 200 records and has a task for all.
- "Create Research Question Draft" / "Create Research Task Draft": the editor of a new ResearchQuestionRecord (title, question, the
  individuals it is about) or ResearchTaskRecord (description, priority, status open, the questions it supports), through
  `ResearchQuestionRecordDialog.withDraft` and `ResearchTaskRecordDialog.withDraft`. Only a draft: nothing is saved unless the researcher
  confirms, cancelling leaves the model as it was. The gap stays until the research fills it: a question is not an answer.
- Fixed on the way (a defect that the gap messages showed, not specific to them): a year given to a message as a number came out with
  the separator of the thousands ("born 1,900"). Years are given as text.
Still to do:
- DONE (November 2026): the sources of the vital events are looked for on the event, on the participation of its principals and on the
  principals themselves (`GapDataLoader`), so an event cited elsewhere is no longer "without a source" (`GapAssistantTest`).
- Not done: the places of the residences (attributes) are not used for "place known, birth record missing" (only the events are), and a
  residence history that is incomplete beyond having none at all (the years between two censuses).
- Parents unknown (the brick walls) are not reported: there are as many as ancestors at the edge of the tree; it is the job of the
  Research Planner Assistant to choose which to work on.
- The usefulness is a rule of thumb: the weights are not calibrated on real research (they are configurable: Settings > Research Assistant..., tab Gaps), and it does not know how
  important the individual is (a direct ancestor of the root, or a collateral), the period or the archives that exist.
- The age for a missing death (110 and 90 years) is a convention; living people could be taken for dead in a tree with no dates of
  death.
- DONE (November 2026): a person with no year of birth is no longer skipped when the relatives say something (`GapChecker.checkDeaths`):
  (1) the latest year of birth that the descendants and ancestors allow: a parent is born no later than the birth or the death of a child
  (any descendant, down the generations), a child no later than the year after the death of a parent; with that year the same 110 and 90
  years apply (finding "... from relatives"); (2) failing that, the birth is estimated at 25 years (`GENERATION_YEARS`) after the birth
  of the youngest parent with a year, and that is only ever "probably dead" (90 years, never "certainly"; finding "... from the
  parents"). The year of the death of the relatives is read by `GapDataLoader` (`Person.deathYear`). Still never reported: a person
  whose relatives have no dates either.
- Baptisms and burials are not vital events here.
- The drafts are in English and plain: no suggestion of where to look (repositories, sources, places), no due date, no link of a
  question to the tasks and activities that would follow.

### DONE (core) Create a Source Citation Assistant.

Done (package `ui.assistant.citation`):
- It is a contextual action (Ctrl+Shift+A in the editor of a source or repository citation, `CitationHost`), not a diagnostic provider.
- `ArchivalReferenceParser` turns free text (vol. 3, p. 45, n. 12 / Libro 2 pag. 143 atto 17 / FHL film 1234567, frame 245 / busta 123, fol. 12v /
  Registro dei nati 1850, n. 12 / segnatura A.3.12 / pp. 45-47) into the locator "Label: value, ..." in a canonical order. Keywords in
  Italian, English, French, German, Spanish and Latin; recto/verso normalized; leading zeros dropped except for film, box and file;
  "c. 1850" is read as circa; duplicate labels keep the first and say so; what is not understood is shown, never guessed.
- `CitationHints`: the file names and descriptions of the media of the cited source are read as references (film1234567_frame245.jpg);
  repositories named in the reference (by name or acronym) are offered to a repository citation.
- `CitationAssistantDialog`: reference text (starting from the current locator), editable locator draft, clues, repositories; it is
  written in the editor only with Apply (asks before replacing a locator), ESC closes it and only it; tests in `CitationAssistantTest`.
Still to do: media metadata (EXIF, PDF properties, ISBN), suggesting whole repository citations (repository + locator) from the
reference, a locator per kind of source (tuned on the media_type), learning the abbreviations of the user, a draft of the whole
SourceCitation (extracts, stance).

### DONE (core checks) Create a Relationship Assistant.

Status: `ui/assistant/relationship`. A diagnostic provider of the Research Assistant (category Relationships): `RelationshipChecker`
(pure rules), `RelationshipDataLoader` (reads the model, leaves out what privacy hides), `RelationshipAssistantContributor`,
`CreateRelationshipDraftAction`. Tested on hand-built data, on a model converted from GEDCOM with relationships added by hand, and on
the real editor.
What the other assistants already report is not repeated here: conflicting parentage, more than two biological parents and descent
cycles are findings of the Conclusion Assistant, the ages of the parents (and the dates of the events) of the Chronology Assistant.
Done:
- Suggests the type from the evidence, says what the evidence is, and leaves the choice to the researcher (the suggested type is only
  text in the explanation; the type is changed in the usual editor, opened with Enter or Ctrl+Enter):
  - a generic `child` whose parent took part, with the child, in an `adoption` event: adoptive_child; recorded as `biological_child`
    in the same case: a conflict (warning);
  - a generic `child` whose parent is married to another (biological) parent of the child, with a date of the marriage certainly after
    the birth of the child: step_child;
  - the generic `spouse` (what an import from GEDCOM gives a marriage without a type): religious_spouse if the couple has marriage
    banns and no license, civil_spouse if it has a license and no banns; both or none do not decide;
  - the generic types that the evidence does not help to specify are ONE finding each (spouse, child), not one for each, with the
    first 200 relationships listed.
- Missing: a `marriage` event of exactly two individuals (the principals; a witness does not count) that no relationship of marriage
  (spouse, civil, religious, customary) links, one finding per couple, with "Create Relationship Draft": the editor of a new
  relationship with the two individuals already in it (and the type, only if the banns or the license point to one and it is valid for
  the participants). Only a draft: nothing is saved unless the researcher confirms, cancelling leaves the model as it was
  (`RelationshipRecordDialog.withParticipants`).
- Inconsistent: a relationship of an individual with itself (error); a type that does not fit what it links (a spouse that is a group,
  a membership between individuals, a group that is part of an individual); the same relationship more than once (a couple written
  from both sides is the same relationship; a civil and a religious marriage of the same couple are not); a relationship that ends
  before it begins (error); a role on a relationship of the family, where the type says it all; an engagement with no end although
  the couple married.
- Every finding explains why, lists its records, can be opened, ignored and is recalculated like the others.
Still to do, in order of priority (problem, then possible solutions):
- DONE: the generic `spouse` is valid for two individuals in `RelationshipReader.getValidTypes` (the one place that says what is valid),
  so an imported marriage can be saved again from the editor (`RelationshipReaderTest`); "Change the type" (`ChangeRelationshipTypeAction`)
  opens the editor with the suggested type already chosen, if the editor offers it (`RelationshipRecordDialog.withType`,
  `RelationshipDraftDialogTest`). Nothing is saved unless confirmed.
- DONE (November 2026): the officiant (an individual whose occupation holds a word of `IndividualAttributeReader.CLERGY_OCCUPATIONS`, in
  the role officiant of the marriage or banns event) and the place (a parish or a diocese, or a place that is part of another by an
  ecclesiastical relationship) point to religious_spouse, only as a hint in the explanation; together with a license they do not decide
  (`RelationshipCheckerTest`). The draft of the missing marriage uses the same hint.
- MEDIUM, more evidence for the type (the first of the solutions, the officiant and the place, is done). Problem: when the evidence does not decide, a generic `spouse` or `child` stays one generic
  finding, although the file often says more. Solutions, one at a time, each with its weight and its explanation, never a rule of the
  validation (they are indications, not proofs): the officiant (a priest) and the place (a church) of the marriage for
  religious_spouse, the most reliable one, to start with; the evidence qualifiers of the sources (a parish register, a civil registry);
  the ages of the parents (too young or too old to be biological, with the limits of the Chronology Assistant); the surname of the
  father (weak, and there are no name tables, so only an exact match); customary marriages and foster and guarded children: nothing in
  the research tells them, so only a prompt to the researcher, not a suggestion.
- MEDIUM, other checks. Problem: contradictions of the relationships that nothing reports. Solutions, by where they belong: PROVABLE ones
  go to Validate File (`ConsistencyService`) as errors: a sibling that is also a parent or a child of the same individual; a membership
  of a group outside the lifespan of the member (judged by `DateRange`, as the date checks, so approximate dates, decades and
  centuries do not raise false alarms). CONVENTIONS go here as warnings: the spouse of a parent that is not linked to the child (it may
  be a step-parent, so a question, not an error); half-siblings that contradict the parents; the associates.
- DONE: the draft of the missing marriage copies the date of the event into `valid_from` and its sources (`withEvidenceOf`,
  `RelationshipDraftDialogTest`).
- LOW, drafts for other missing relationships. Problem: only the missing marriage has a draft (`Create Relationship Draft`). Solution: the
  drafts for a child of the spouse of a parent and for the parents named in a birth record (these need a new kind of finding).
- NOT TO DO as a rule: the banns mean religious and the license civil. Problem: it is a convention that changes with country and
  period (a license can be an ecclesiastical one), so it cannot be a rule, and it is wrong more often than the tuning would be worth.
  The two clues stay as they are, a weak hint that decides only when only one of them is there and is never applied by itself; a
  setting by country and period is not worth the cost.

### DONE (core checks) Create a Chronology Assistant.

Status: `ui/assistant/chronology`. It is a diagnostic provider of the Research Assistant (category Diagnostics): `ChronologyChecker`
(pure rules, tested without a model), `ChronologyDataLoader` (reads the model, leaves out the individuals hidden by privacy),
`DateRange` (the days a date may stand for), `ChronologyAssistantContributor`. Tested on hand-built data and on a model converted
from GEDCOM.
Done:
- Child born before the parent (biological links only: an adopted, foster, guarded or step child can be older).
- Parent too young (mother 12, father 14) and too old (mother 55, father 90): warnings, the limits are conventions (`Rules`).
- Child born after the death of the mother, or more than 300 days after the death of the father.
- Event after the death (posthumous types are exempt: death, burial, cremation, coroner report, probate, honor, pardon, deed,
  escrow, chancery) and event before the birth, marriage events reported as "marriage before birth".
- Union (relationship `valid_from`) before the birth, or after the death, of a partner.
- Death before birth, lifespan over 120 years.
- Overlapping marriages (information, not error: polygamy exists), one marriage per couple, an end known without a date is not a
  claim.
- A finding is reported only when it holds for every day the dates may stand for (year precision, approximate margin, bounded
  and open dates): an uncertain date never raises a false alarm. Every finding explains its reasoning and lists the records.
Done: the rules that are PROVABLE for every day the dates may stand for are also checks of Validate File, as
errors (`DateConsistency`, in Domain; the rules are written twice, only `DateRange` is shared): a death before the birth, an event after
the death or before the birth, a child before the parent or after the death of the mother or 300 days after the death of the father, a
union before the birth or after the death of a partner, and a relationship that ends before it begins (this one only there). "A mother
who gives birth after her death" is the same rule as the child born after the death of the mother: done, here and in the validation.
The validation also warns of the dates that cannot be read (`date_invalid`: not valid in the notation of the calendar) and of the ones it
did not check because the calendar needs the Ephemeris plugin (`date_not_checked`: not a problem of the file), told apart by
`DateNormalizer.problem`.
Done (November 2026): the memberships of a group outside the lifespan of the member (`membership_before_birth`, `membership_after_death`,
`membership_ended_before_birth`) are errors here too; `relationship.invalid_for_participants` is an error, as in the validation. The two
lists are kept in step by `ValidationAssistantParityTest`: a new kind of Validate File must be classified there (same check here, only
there, or to do). `child_married_to_parent`, `sibling_and_parent` (relationship assistant) and `date_invalid` / `date_not_checked` (chronology
assistant) are findings here too; the last is information, not a warning, because it is not a problem of the file. Two differences are on purpose: `parents` and `same_sex_parents` are warnings here, and nothing once a conclusion resolves them.
Done since (November 2026): children of the same mother born less than 270 days apart (`births_too_close`; certainly less, and certainly
more than 7 days apart, so twins are never reported) and a spouse married younger than the age of a parent of the same sex
(`marriage_too_young`, from the marriage event or, if none, the union), as warnings; both limits are in `Rules`.
Still to do, and where it belongs:
- A spouse older than the lifespan: a convention, so for this assistant as a warning, never for the validation.
- Other overlapping incompatible events (residence, military service, prison, hospitalization) and overlapping attributes: not provable
  (two homes, a soldier who lives somewhere), so only an assistant warning, with weights that are a matter of judgement; not for the
  validation.
- DONE (November 2026): the age limits and the gestation limit are configurable (Settings > Research Assistant..., tab Chronology).
- DONE (November 2026): "Check Chronology" for a selected individual is the "Only about ..." check box of the palette, for every assistant
  (the selected record is in `AssistantContext.selectedRecordId`, see the Framework notes).
- Not worth doing: events of groups and events whose participant is not an individual (they have no birth or death to compare to).
- The model is read away from the event dispatch thread on a copy of it (`AssistantContext.forReading`), so a half-edited model is never
  seen. No cache of the findings: a large file is read again at each opening.
- Messages are English only (the UI bundle has no per-locale files yet).

### DONE (core checks) Create a Place Assistant.

Status: `ui/assistant/place`. A diagnostic provider of the Research Assistant (category Places): `PlaceChecker` (pure rules),
`PlaceDataLoader`, `PlaceAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM.
Done:
- Possible duplicate places: same name (ignoring case, accents and punctuation), same type (a missing type makes it only an
  information; a city and a province with the same name are different places), not already related, not in different places (same
  name in different parents, or coordinates more than 50 km apart, is not a duplicate). Ranked by how much the places are used.
  The action is the side-by-side comparison; nothing is ever merged.
- A place that is part of nothing: if its name says where it is (Treviso, Veneto) and one larger place has that name, the draft of the
  relationship names it; with no candidate it is reported only if events happen there (the draft leaves the target to choose).
- A parish that is part of no diocese (draft of an ecclesiastical_part_of; the diocese is proposed only if it is the only one).
- Territorial relationships (administrative, geographic) where the subject is larger than the target (a province in a city), by a
  scale of the types; the ones of ecclesiastical, judicial and cadastral kind and the types off the scale are not judged.
- A place related to itself (error); places that are part of each other in a circle (error) or that succeed each other in a circle
  (warning), reported once per circle.
- More than 25 findings of a kind: the most used are listed and one finding sums up the others.
- "Create Place Relationship Draft": Enter on a finding opens `PlaceRelationshipRecordDialog.withDraft`; saved only if confirmed.
- Places with no source (November 2026): a place where events happen that neither a citation of the place nor one of its coordinates
  backs (`PlaceData.Place.sourceCount`); one finding for each, ranked by the events, the most used first (25 at most, the others
  summed up); a place no event happens in is not reported (an imported tree is full of them). No draft. `PlaceCheckerTest`.
- A relationship of places whose `valid_from` is after its `valid_to` is not a finding of the assistant: it is provable, so it is an
  error of Validate File (`FLEFValidator`, `validity_dates`, already there; `FileValidationTest`).
Still to do: places with no type (suggest the type from the name: Diocesi di, Parrocchia di; low priority, one language only), successor_of
suggestions from the names and dates of places (historical jurisdiction changes), relationships outside the lifetime of a place, the
usage of places by other records than events, hierarchy by the names written with several parts, suggestions of the parent from the
places near it.

### DONE (core checks) Create a Conclusion Assistant.

Status: `ui/assistant/conclusion`. A diagnostic provider of the Research Assistant (category Conclusions): `ConclusionChecker` (pure
rules), `ConclusionDataLoader` (reads the model, leaves out the individuals hidden by privacy), `ConclusionAssistantContributor`.
Tested on hand-built data and on a model converted from GEDCOM, with a ConclusionRecord added to it.
Done:
- Several birth or death events of an individual: a warning if their dates cannot all be right (no common day), an information if
  they agree (corroboration or duplicates).
- Competing parentage: more than two biological parents, two biological mothers (or fathers); adoptive, foster, guarded and step
  links do not count.
- Conflicting relationships: the same child-parent pair both biological and adoptive/foster/guarded/step.
- An individual that is their own ancestor through the relationships of descent (an error; reported once per cycle).
- Conflicting attributes: more than one value of an attribute that can have only one (`national_id`), compared ignoring case,
  spaces and hyphens.
- Unresolved identity hypotheses between individuals (no conclusion evaluates them, whatever its outcome).
- A conflict is settled when a ConclusionRecord evaluates it: RESOLVES contains at least two of the competing records (events, or
  the participations of the individual in them, or relationships, or attributes) and it prefers one of them, or its proof status
  says that none prevails (conflicting_evidence) or that all are false (disproven). The assertions are never changed.
- Each finding lists the records of the conflict, explains why, and suggests the draft of a ConclusionRecord; for events it names the
  candidate preferred assertion (higher confidence, then more sources) only when one is better supported than all the others, and
  otherwise says there is none. It is a suggestion with its reason: the researcher judges the sources.
- "Create Conclusion Draft": Enter on a finding of the Conclusion Assistant (the palette shows the action in the detail line) opens
  the editor of a new ConclusionRecord, through `ConclusionRecordDialog.createNew(...).withDraft(...).withResolvedTarget(...)`: the
  competing records are already in RESOLVES (and the research questions that target them are linked), the issue is filled
  (`birth_date`, `parentage`, `relationship_type`, `national_id`, `identity`, ...), the narrative starts with the description of the
  conflict. It is only a draft: proof status `unresearched`, NO preferred assertion (the candidate is only named in the narrative),
  nothing saved unless the researcher completes it and confirms, cancelling leaves the model untouched. A draft saved as it is does not
  settle the conflict (a conclusion needs a preferred assertion, or conflicting_evidence or disproven). The mechanism is generic:
  `Diagnostic.action()` (`DiagnosticAction`) is what the user can do about any finding, run only when chosen.
Still to do:
- The draft is built from the model as it was when the palette opened: a record changed or deleted since is skipped, a record that is
  now settled is not detected. The narrative is English and is not translated.
- The editor is not a wizard: the preferred assertion, the proof status and the reasoning are chosen in the usual tabs. A draft for
  an identity hypothesis resolves one target only (a disproven conclusion normally has two).
- DONE (November 2026): the candidate preferred assertion weighs the evidence qualifiers: the confidence of the researcher first, then the
  weight of the citations (`SourceCitationReader.supportWeight`: one for each supporting citation, plus one for an original source, one
  for primary information and one for direct evidence; minus one for a contradicting or negative citation). It is given for every
  single-valued event (birth, baptism, death, burial), for the conflicting attributes and for the conflicting relationships, and the
  explanation shows the weight. A rule of thumb, never a judgement of the sources (`ConclusionCheckerTest`, `SupportWeightTest`).
- DONE (November 2026): baptisms and burials, like births and deaths, are single-valued, and a different place is a conflict when
  neither place contains the other (a town and its province agree: `PlaceAncestors`, shared with the Context Assistant); the finding
  lists the places (`ConclusionCheckerTest`). Not done, and not planned: the sex (one field of the individual, nothing to compare) and
  the type of a relationship between sources (the parent-child one is already a finding; civil and religious marriages differ legitimately).
- Attributes that change in time (residence, occupation, citizenship, religion) need a comparison aware of `valid_from` and
  `valid_to`, to tell a change from a conflict; the reported `children_count` and `marriages_count` against the tree.
- DONE (November 2026): a biological child-parent pair that is also a pair of spouses (marriage, civil, religious or customary; an engagement
  or an adoptive link does not count) is an error of the validation, not of the assistant (`ConsistencyService`, `child_married_to_parent`).
- Relationships: civil and religious marriages of the same couple that differ in date, parentage of a child by different couples in
  different sources.
- Identity hypotheses: only "no conclusion" is checked; a hypothesis evaluated before new evidence arrived (a stale conclusion, by the
  audit dates), a candidate that is not an individual (place, source, repository).
- DONE (November 2026): the consistency of the conclusions is a check of the validation, not of the assistant: a `preferred` that is not
  in RESOLVES was already an error, and now RESOLVES or `preferred` that point to records that no longer exist are one too
  (`ConsistencyService`, `resolves_missing`). `proven` with a single source: not done (it can be legitimate).
- DONE "Settled" has one definition, `ConclusionResolution`, shared with the merger (`ProtocolCleanup`).
- Same limits as the Chronology Assistant: the whole model is read at each opening, away from the event dispatch thread while the
  user may be editing; English messages only.


### DONE (core checks) Create a Context Assistant.

Status: `ui/assistant/context`. A diagnostic provider of the Research Assistant (category Context): `ContextChecker` (pure rules),
`ContextDataLoader`, `ContextAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM with a historic event
and a cultural norm added by hand. The catalog of historic events and norms is the one of the model: the assistant does not bring any.
Done:
- A historic event or a cultural norm of the model that may have influenced an event of the tree: the dates overlap, the place is the
  same or contains the place of the event (by the territorial relationships, at any depth), the kind of context can influence that type
  of event (a war and a death or a conscription, an epidemic and a death, a marriage law and a marriage, a naming custom and a birth,
  an inheritance rule and a death or a will...). Usefulness 55 with the place, 35 without, +10 if the event is entirely inside the
  period. Events with no date, no bounded date or a date wider than 5 years are not used; a historic event needs a date, a norm a date
  limit or a place; an impact that exists already is not proposed again; 3 events at most for each context, 25 in all.
- Every finding says why (the period, the place) and how the context may matter (a hint for each kind), with the usefulness, and the
  draft of a ContextImpactRecord (the context, the event, the type of impact: explains, influences, constrains or motivates, and the start
  of the explanation) in the usual editor, through `ContextImpactRecordDialog.withDraft`. The sources are for the researcher to give.
- Several deaths (or burials) of different individuals in the same place within two years (3 or more), that no historic event of the
  model of the kind of a crisis (epidemic, pandemic, famine, war, natural disaster) covers in that place and period: a finding with the
  draft of a HistoricEventRecord (type epidemic as a first guess, title with the place and the years), through
  `HistoricEventRecordDialog.withDraft`.
Still to do: the date and the place of the draft of a historic event; migrations (an individual in two distant places) and the
historic events of the place of origin; naming conventions checked against the names of the tree (patronymics); the age rules of the
norms (minimum age of marriage against the ages of the spouses); events of the individual other than the events of the tree (attributes
and residences); a catalog of historic events and norms to import; the cultural norms that apply to a conclusion or a relationship;
the weights are not calibrated.

### DONE (core checks) Create a Research Planner Assistant.

Status: `ui/assistant/planner`. A diagnostic provider of the Research Assistant (category Research): `PlannerChecker` (pure rules, on the
data of the Gap Assistant: `GapData`, `GapDataLoader`, `GapDraft` and `CreateResearchDraftAction` are now public, and a question carries
its targets), `PlannerAssistantContributor`. Tested on hand-built data and on a model converted from GEDCOM with a question added.
Done:
- Next steps of an open question with no conclusion (the individuals it targets, or the principals of the events it targets, up to 3):
  the birth or baptism record (no birth: 80; no source cites it: 65), the marriage record that names the parents (parents unknown: 75;
  marriage unrecorded or uncited: 60), the baptisms of the years around the birth (parents unknown, year known: 55), the death or burial
  record (born 80 years ago or more, death missing or uncited: 55), the census (born 1700-1950, in none: 40), contacting the archive (two
  searches of the question completed with no positive result: 50). +5 if nobody started the question.
- A step is left out if a task or an activity of the question (not abandoned) already says it does it (by words, in several languages);
  at most 3 steps for each question and 25 in all, the others summed up in one finding.
- Brick walls: individuals with no known parents and at least one descendant, ranked by how many descendants depend on them (40 + 3 for
  each, at most 30; +10 year of birth, +5 events; 85 at most); not proposed if an open question already targets them; 10 at most, the
  others summed up in one finding. The draft is a research QUESTION ("Who were the parents of X, born Y?").
- Every suggestion says why and gives its usefulness (high, medium, low; a rule of thumb, as for the gaps); a step is the draft of a research
  TASK (description, priority from the usefulness, supports the question) in the usual editor, saved only if the researcher confirms.
Done since (November 2026): the sources already in the tree count: the sources of the event, of the participation of its principals and of
the individual (`GapDataLoader`), and the ones that cite the child-parent relationship (a source of the birth) or the marriage relationship
(a source of the marriage), so the step to find a record is not proposed for what the tree already cites (`GapData.ParentLink` and
`Union` carry `sourceCount`; `PlannerCheckerTest`).
Still to do: the places and the periods (which parish, which archive keeps the records, which registers exist for the years: needs
the repositories and the places of the individual), the conclusions of the question, a due date, the order of the steps as a plan (dependencies: the marriage
before the baptisms), questions about places and families, the weights are not calibrated (they are configurable: Settings > Research Assistant..., tab Plan).
