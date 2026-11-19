FamilyLegacy

## TODO

- **Translations to generate (on request).** For now only English is maintained. When asked, generate:
  - the UI bundle `Domain/src/main/resources/i18n/ui/messages_<lang>.properties`;
  - the missing keys of the report bundles `Domain/src/main/resources/i18n/report/report_messages_<lang>.properties`
    (it, de, es, fr, pt, en_GB, en_US, ru, ar, bn, fa, hi, id, ja, ko, sw, th, tr, ur, vi, zh_CN). A missing key falls back to English.
  - the help pages (`help/<lang>`, only `en` exists).

- **PDF engine: evaluate alternatives to OpenPDF (spike pending).** Today `ReportPdf` (about 1500 lines, OpenPDF 3.0.5) lays out the
  `ReportDocument` sections in Java, with a hand-made `IndicShaper` (Devanagari/Bengali drawn as vector glyphs through `java.awt.TextLayout`);
  PDF is the only format that does not go through `ReportXhtml`, which EPUB and the site already use. Options:
  - **OpenHTMLToPDF** (or an actively maintained fork): the candidate if report **templates** are wanted. One XHTML + CSS pipeline for PDF, EPUB
    and site; `ReportLayout` becomes a generated `@page` (paper, margins, running header, footnote, page counter); the user could restyle with a CSS.
    Risks: unmaintained upstream (pick a fork), the layout is held in memory (check against the low-memory rules on a large tree), CSS support is partial
    (long tables split across pages, images in cells), Indic shaping still needs a workaround, LGPL (as OpenPDF, shaded in the plugin jar).
  - **Keep OpenPDF** if templates are not needed.
  - Spike (half a day): an experimental `HtmlPdfRenderer` fed by `ReportXhtml` + a CSS with `@page`; check long tables, contents with page numbers,
    Devanagari/Bengali and RTL text, memory and time on a real tree (`test.flef`); compare with `PdfReportRendererTest`. If all pass, migrate;
    if shaping or memory fail, stay on OpenPDF and add a lighter style layer.

## Reports: what to take from other applications

Reference screenshots: `Core/unused-images/gramps` (report selection list) and `Core/unused-images/tcgb` (the report preferences of a
commercial book generator, about 25 pages of options). Compared with what `ReportConfig`/`ReportOption` offer (language, privacy
policy and living-person policy, header, introduction, ancestry/descendants/relations, collateral scope, generations, order, notes, sources,
media, evidence, context, cultural norms, historic events, repositories, groups, coverage, timeline, indexes, date format, saved option sets,
page layout of PDF/Word, family group sheets; PDF/DOCX/EPUB/site/HTML/Markdown/text output), what is still worth doing:

### Not worth doing

- **GEDCOM-specific switches** (ignore `ASSO/RELA`, `_TYPE DOCUMENT`, `OBJE`, `_SCBK`, "assume marriage occurred", unfound media, "combine identical
  NOTEs", "interpret font formatting characters"): they compensate for the poor GEDCOM data model; FLEF has typed records, and the importers already
  report what they discard.
- **Hebrew / French Republican conversion, "Quarter" format, B.C./B.C.E.**: the calendars module and the date grammar already cover them.
- **A page of "Loading" and "Analysis" options before the report**: the scope is already decided by the report type, the subject and `generations`.
- **Printer-oriented settings** (duplex offset, alternate binding margins, "reference the selected printer", PDF compress/protect, mixed fonts):
  low value, and the PDF renderer already embeds fonts and shapes RTL/Indic text.
- **A different engine per output** (Gramps lists about 25 reports: graphs, charts, calendar, web site): the section/renderer split
  (`ReportApi` + renderers) is the right shape; add reports, not engines.
- **Per-language kinship wording** (Dutch "nephew", Swedish cousins, Norwegian "second cousin"): a concern of the per-language labels bundle;
  revisit only when a translation is generated on request.

### Worth doing (roughly in order of value / cost)

1. **Leftovers of the first round** (small): a default reports folder, or "next to the file", as an alternative to the last folder used
   (the dialog proposes `<subject>-<report>` and the last folder already); `<` `~` `>` abbreviations instead of "before/about/after" in the
   date format; a cover image (or the subject's portrait) and a watermark in the page layout.
2. **Narrative style options** (medium): direct ancestors in bold, cross-references in brackets, Ahnentafel numbers (ancestor reports) and
   d'Aboville numbers (descendant reports), "all first names" vs first + initials, birth/death years in the index of individuals, unknown name
   as `?` or custom text. The numbering codes are the most useful. Look here, take the best numbering for each report, override previous
   numberings if a more suitable one is found: https://en.wikipedia.org/wiki/Genealogical_numbering_systems (if deemed necessary, let the user
   choose the numbering)
3. **Sources/notes placement** (medium): per kind choose *in narrative* or *as a section*, with a "long means more than N lines" threshold;
   today the layout is fixed. Also "principal sources only" and "hide source quality / page".
4. **Media options** (medium): image resolution cap (e.g. 2 Mpx) when embedding in PDF/DOCX/EPUB (file size), thumbnail side and width in the
   narrative, images per page in the media section, exclude originals of cropped portraits (crops are already stored), person name as the title
   of a crop.
5. **Indexes** (small): restrict the index of places/dates to births, deaths and marriages; add indexes of occupations, causes of death and
   anniversaries (birthdays). Occupations/causes need attribute lookups like the ones the Surnames report already does.
6. **Generation scope and order** (small): earlier / later generations only, peer generation, ascending/descending order, "start each
   generation on a new page", "title as Nth generation". Mostly rendering flags on `AncestorReportGenerator`/`DescendantReportGenerator`.
7. **Colours and fonts** (small, PDF/HTML/site): font and size, pedigree boxes tinted by sex, hyperlink colour. Prefer a few named themes to a colour table.
8. **Privacy refinements** (small, only if users ask): "do not state *still alive* / *no longer living*"; per-flag exclusion of private /
    confidential / living details. `LivingPrivacy.NAME_AND_ROLE` already covers "report as Living".

### New reports suggested by the Gramps list

Existing: ancestor, descendant, group, individual, relationship, place, source, research question, bibliography, research progress,
full bibliography, family book, to verify, surnames, family group sheets.

- **End of line report** (small): ancestors with no known parents (brick walls); fits the research assistant (FAN panel, to-verify finder).
- **Number of ancestors / pedigree collapse** (small): per generation, known vs possible; reuses the consanguinity tool data.
- **Birthday and anniversary report / calendar** (medium): by month, with ages; also exportable as iCalendar.
- **Records report** (small): oldest/youngest, longest marriage, most children, etc.
- **Database summary** (small): counts by record type, date range, completeness; partly overlaps the coverage report.
- **Kinship report** (small): all relatives of a given kind of a person; the relationship calculator already exists.
- **Fan chart, hourglass / descendant tree** (graphics): the tree views and the FAN panel cover the interactive side; for print, add a "drawing"
  section type (SVG/image) that PDF/DOCX/EPUB/site can embed. Statistics charts and timeline chart are lower priority.
- **Tag report, note link report**: skip unless tags become a first-class concept.

### Should the configuration be extended?

Yes, but selectively. The dialog shows only options that have an effect (`ReportBuilder.optionState`), which is the right principle;
the risk of the reference application is the opposite (dozens of pages of options, most irrelevant to a given report). So:

- the main dialog keeps the content options per report type; the **saved option sets** and the **page layout** (paper, margin, headings,
  footnote, text under the title) are in place: the next layout options (cover image, watermark, fonts, numbering codes, notes/sources
  placement) go in the page layout panel, shown for every report and ignored by the formats without pages;
- every new option follows the existing recipe: `ReportConfig` component + `ReportOption` constant + checkbox + `tooltip.report.<key>` + help
  page text; `ReportOptionStateTest` keeps hidden options from changing the output.

Suggested next step: the numbering codes (Ahnentafel, d'Aboville and the others of the Wikipedia list) and the notes/sources placement.
