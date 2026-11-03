# Entering dates

A date in Family Legacy is not a single line of text. You open the date editor from the date field of a record (for example the **Date** of an event) and describe the date with a few controls. This page explains the controls, what you can type in the *Full Date* box, and what the application tells you when it cannot understand a date.

## The date editor

The editor has three tabs, which say *what kind of statement* the date is:

| Tab | Use it when | Fields |
|---|---|---|
| **Point** | The fact happened on one date (a birth, a marriage). | One date. |
| **Bounded** | You do not know the date, but you know it fell within an interval. | **Not Before** and **Not After** (at least one is required). |
| **Spanning** | The fact itself lasts for a while (a residence, a marriage that lasted from...to). | **From** and **To** (at least one is required). |

Switching tab clears what you entered in the other tabs.

Each date (the *Point* date, or each of *Not Before*, *Not After*, *From*, *To*) is described by the same controls:

- **Type**: *Full Date*, *Decade* or *Century*.
  - *Full Date* is a text box where you type the date (see below). It can be a full date, a month and a year, or only a year.
  - *Decade* takes the first year of the decade: `1490` for the 1490s.
  - *Century* takes the ordinal: `15` for the 15th century. The optional **Part** narrows it: early, mid, late, first half, second half, or the first to the fourth quarter.
- **Calendar**: the calendar the date is written in. It is required, and it decides how the Full Date text is read. The default is Gregorian. See [Calendars](#calendars).
- **Approximate**: tick it when the date is not exact. Then **Basis** is required (*stated*, *calculated*, *conventional* or *unspecified*) and **Margin** is optional (an ISO 8601 duration, for example `P2Y` for plus or minus two years). With the basis *conventional* you must also name the **cultural norm** the convention comes from.

Below the tabs:

- **Original Text** is a free field for the date as the source writes it. Nothing fills it in for you.
- **Sources with Citations** lists the citations that support the date.
- **Confidence** is the confidence you place in the date.

Typing words such as *about*, *before* or *between* in the Full Date box does not work: those ideas are expressed with the tabs (*Bounded*, *Spanning*) and with **Approximate**.

## What to type in the Full Date box

The text is read by the parser of the calendar you chose. For the Gregorian and Julian calendars (and the year-start styles below) these forms are accepted:

| You type | Meaning |
|---|---|
| `1953` | The year only (the date is taken as 1 January, and shown as a year). |
| `August 1953`, `Aug 1953`, `08 1953` | A month and a year. |
| `15 January 1953`, `15 Jan 1953`, `15 Sept. 1953` | A full date. |
| `15/01/1953`, `15.01.1953`, `15-01-1953`, `15 01 1953` | A full date, numeric. |
| `1953-01-15`, `1953-08` | Year first (ISO style). |
| `January 15, 1953` | The month name first, only when the month is written in letters. |

Rules worth knowing:

- **Numbers are always day, month, year.** There is no month-first reading: `05/03/1953` is 5 March, and `03/15/1953` is rejected because there is no month 15. A number first of four digits means year, month, day.
- **Month names** are accepted in almost any language (full and abbreviated, any case), and also in Latin (`Ianuarii`, `Martii`...) and in the old numeral forms (`7bris`, `8bre`, `9ber`, `10bris`, `Xbris`). Examples that work: `1 Gennaio 2000`, `15 août 1953`, `15 января 1953`, `15 Ianuarii 1953`, `15 7bris 1650`.
- **Days are checked.** `32 January 1953`, `0 January 1953`, `31 February 2023` and `29 February 2023` are rejected; `29 February` is accepted only in a leap year.
- **Before Christ.** Add `BC`, `B.C.`, `BCE` or `B.C.E.` after the year (or `a.C.` in Italian): `15 March 44 BC`. There is no year 0.
- **Filler words** found in old documents are ignored: *die*, *mensis*, *anno*, *domini*, *nativitatis*, *incarnationis*, *adi*, *the*, *of*, *de*, *del*, *di*, and the ordinal marks of the day (`1st`, `2nd`, `1°`). So `die 5 mensis martii anno Domini 1450` is read as 5 March 1450.
- **Indiction.** A phrase such as `indictione tertia decima` or `indictione XIV` is checked against the year and then ignored. If it does not match the year, the date is rejected.
- **Dual dating** (the English habit before 1752 of writing two years for January to March): `12 Feb 1704/5` is 12 February 1705, and `6 January 1705/6` is read in the same way. It applies only to January, February and March.
- **Seasons** can replace the month, when there is no day: `spr 2024`, `sum 2024`, `estate 2024`. The date is taken as the equinox or solstice of that year. **This needs the Ephemeris plugin**; without it, the date is accepted but cannot be placed on the timeline. With a day (`15 spring 2024`) a season is rejected.
- **Weekday names** are not read.

## When a word can be read in several ways

Some words are a month in one language and a season, or another month, in another language: `est` can be the Cornish August or the Italian *summer*, and `pri` is the Italian *spring* or the Albanian April. The application applies automatic rules (for example, a month wins over a season, and the Western languages win over the others), and when the text still has more than one reading it **asks you** when you press OK:

- The dialog **Ambiguous Date** lists the readings, one per line, each with its meaning; the first is the one the parser would have chosen.
- **Use Selected** replaces the text in the field with the chosen reading, which ends with the language in parentheses, for example `est 1850 (Cornish)`.
- **Edit Date** goes back to the field without saving.

You can avoid the question by writing the language yourself at the end of the date, in parentheses: `1 pay 2006 (asturiano)`. The language can be given by its name in any language or by its code (`Asturian`, `asturiano`, `ast`). A word that is a different season in different languages (such as `yaz`) is rejected unless you give the language. The language in parentheses is understood by the Gregorian-family parsers; for the other calendars, check the result in the display text after saving.

## Calendars

The **Calendar** box lists the calendars the application knows (it is editable, but a name that is not in the list is stored as text and not checked). Among them:

- **Gregorian** and **Julian** are the calendars of the whole proleptic range: type the date as written in the source and choose the calendar of the source. Dates between 5 and 14 October 1582 do not exist in the reformed calendar and are rejected.
- **Year-start styles** (the year number changes on a day that is not 1 January): you choose the style and type an ordinary date whose year is *numbered in that style*, and the application converts it.
  - *Florentine*: the year begins on 25 March, after 1 January. `10 gennaio 1600` is 10 January 1601 in the modern count.
  - *Pisan*: the year begins on 25 March, before 1 January. `26 marzo 1601` is 26 March 1600.
  - *Nativity*: the year begins on 25 December. `25 dicembre 1500` is 25 December 1499.
  - *Byzantine style*: the year begins on 1 September.
  - *Venetan* (*more veneto*): the year begins on 1 March, and the Venetian month names are understood (`15 febraro 1499`, with an optional `mv`).
  - *Easter style*: the year begins at Easter. A day that occurs twice in the year needs `avant Pâques` or `après Pâques` (also: *before/after Easter*, *prima/dopo Pasqua*, *ante/post Pascha*, *vor/nach Ostern*). `5 avril 1402 après Pâques` is 5 April 1402.
  - A dual year (`1499/1500`) is already counted from 1 January and is not shifted.
- **Swedish**: the Swedish calendar of 1700 to 1712, which accepts `30 February 1712`.
- **Quaker**: numbered days and months, for example `4th day 11th month 1702/3`, `4 da 11 mo 1702`, `the fourth of the eleventh month 1702`.
- **English regnal years** and **pontifical years**: the year of a ruler, for example `5 March 3 George II` or `the third year of the reign of George II`; for popes, `anno III del pontificato di Pio V` or `pontificatus domini Pii papae quinti anno tertio, die 5 martii`. When the ruler or the day is ambiguous, add the year of the Incarnation (`15 June 1753, 26 George II`).
- **Liturgical** calendars: dates relative to a feast, followed by the year: `Pasqua 1500`, `Easter 2024`, `Michaelmas 1700`, `terza domenica dopo Pentecoste 1700`, `3 days after Easter 1400`. A feast that is not known, or a missing year, is rejected.
- Others: Hebrew (`15 Nisan 5785`), Islamic (`1 ramadan 1445`), Persian, Chinese, French Republican (`1 Vendémiaire 1`), and many more.

For the calendars other than the Gregorian family, the examples above are the forms known to work. If you are not sure how to write a date, the [Historical Date Assistant](assistant.md#the-historical-date-assistant) converts between calendars and shows what it understood.

## What the application tells you

There is no live preview while you type. The checks happen when you press **OK**:

- A required value is missing: *Full Date is required*, *Calendar is required*, *At least one of Not Before or Not After is required*, *Basis is required*.
- The date can be read in several ways: the **Ambiguous Date** dialog above.
- The calendar's parser does not understand the date: the dialog **Unrecognized Date** shows the parser's message (for example *Month must be between 1 and 12: 13*, or *There is no year 0 before Christ*) and asks **Save it anyway?** If you answer yes, the text is stored as typed, but the date cannot be used by the timelines and by the Research Assistant.

After saving, the field shows the date as: the text you typed followed by the calendar, for example `15 Jan 1953 (gregorian)`. A decade is shown as `1490s`, a century as `15th century`, an approximate date ends with `[approx.]` (with the basis and the margin when given), a bounded date reads *between ... and ...*, *after ...* or *before ...*, and a spanning date reads *from ... to ...*, *from ...* or *until ...*.

## How the date is stored and used

The text you typed is stored **as you typed it**, together with the calendar. Nothing is reformatted.

When the application needs to compare or sort dates (the timelines, the chronomap, the [Research Assistant](assistant.md)), it converts each date into a day number that is the same for every calendar, with the precision that the text allows: day, month, year, decade or century. A partial date points to the start of its period: `1953` is 1 January 1953, and a century is the first day of its first year. A date that cannot be read in its calendar is left out of those views.

## Tips

- Write the date as the source writes it, choose the calendar of the source, and let the application convert it.
- Put the date as written in **Original Text** when you transcribe from a document, so that the source's wording is never lost.
- Prefer *Bounded* or **Approximate** to a precise date you had to invent.
- If a date is rejected, read the message: it names the problem (a day beyond the end of the month, a month that is not one, a word it does not know).
