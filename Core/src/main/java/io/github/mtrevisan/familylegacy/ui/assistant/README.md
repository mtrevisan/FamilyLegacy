# Research Assistant (`ui/assistant`, module `Assistant`): open TODO

Only what is still to do, by priority. How the assistant works (palette, indicator, ignoring, statistics, settings, each assistant's
checks and keys) is in the help page `Core/src/main/resources/help/en/assistant.md` and in the code. Items decided against are in git
history (this file before the cleanup of 2026-11-23), to avoid proposing them again.

Two modules: the knowledge (`*Checker`, `*Data`/`*DataLoader`, `*Finding`, `*Draft`, `Diagnostic`, `AssistantTuning`...) is in the Maven
module `Assistant`, Swing-free (`PackageDependenciesTest` keeps it so); Core keeps the palette, the dialogs and the actions that open
editors. A checker never builds an action that opens a dialog: the contributor in Core adds it with `Diagnostic.withAction`. Every
action respects the data-safety rule: the assistant analyses, explains, suggests and drafts, but never merges, creates conclusions,
modifies assertions or resolves conflicts by itself.

## MEDIUM

- **Calibrate the numbers.** The statistics collection is done (Settings > Research Assistant... > Statistics...). Collect real counts
  from users, read them by kind (a kind listed often and never acted on has too high a weight, or is noise; a kind acted on at once from
  far down the list deserves more; a kind ignored by kind or by record is not wanted; group the kinds before judging), then change the
  defaults of `AssistantTuning` and write what was learned.

## LOW

- **Source Citation:** whole repository citations (repository + locator) from the reference, only as a proposal, never applied by itself
  (first check how many real references have a regular form).
- **Context:** date and place of the historic-event draft (derive them from the individual only if users end up copying them by hand).
- **Place:** type from the name ("Diocesi di", "Parrocchia di": one language only, becomes a per-language prefix list via i18n, and fails
  silently; small gain).
- **Date:** regnal years (needs tables from an authoritative source, see the calendar rule; much research for a rare case).
- **Relationship:** drafts for other missing relationships (a child of the spouse of a parent; the parents named in a birth record: need
  a new kind of finding); provable checks to move to Validate File (`ConsistencyService`) as errors: a sibling that is also a parent or
  child of the same individual; a membership of a group outside the lifespan of the member (judged by `DateRange`).
- **Gap:** use the places of the residences (attributes) for "place known, birth record missing" (only events are used today).
- **Framework limits:** the conclusion draft is built when the palette opens (a record changed or deleted since is skipped, a record now
  settled is not detected) and the model is read whole at each opening; the editor of a draft is not a wizard; no option to turn off the
  reopening of the palette after an action per kind; no list of the ignore rules in force (to lift one without going through a finding it
  hides); ignored findings are per file path and per user.
- **Conventions to keep visible, not to change:** ages 110/90 for a missing death (relatives refine it; no data at all means no
  finding), aggregation thresholds 25 and 200, usefulness weights not calibrated (see MEDIUM).

## Languages

English only on purpose, until the user chooses the languages at the end of the work. Then: `messages_<lang>.properties` for the ~700
keys of the assistants, `AssistantKindLabelsTest` for each language, and the help pages (`help/en` only today). Already done so that
translating is only translating: the assistant text is not joined in code (`Sentences`), the "(s)" plurals of the assistant are ICU
plurals, `LanguageNeutralTextTest` keeps it so. Left: the "(s)" of 12 other messages (text.*, dialog.*, message.*) outside the assistant,
the identifiers in square brackets and the locator labels of `ArchivalReferenceParser` (data, not language), and the grammar the code
cannot know (the gender of "high"/"medium"/"low" next to a noun). The date language of a historical date is on hold (see the memory note
on the date-language priority).

## Not tested (needs the interface)

The real click that opens the palette; the look of the indicator button (its colour and timer are tested, the painting and the blinking
decision are not); the check box "Only about ..."; the category box and `Ctrl+L`; the menu of `Ctrl+Shift+D` and its right click; `Ctrl+G`
(the choice of the record is tested, the window is not); the file choosers of the exports and of the ignored findings; the reopening of
the palette with its notice and selection; the keys `Ctrl+D` / `Ctrl+I`; the `AppPreferences` adapters of the ignored findings and of the
settings (they would write the preferences of whoever runs the tests); the two editors of `RecordDifferentAction` in a row. The Swing
visual-check method (paint the palette to a PNG from a test, see the memory note) would cover the first four without a real display.
