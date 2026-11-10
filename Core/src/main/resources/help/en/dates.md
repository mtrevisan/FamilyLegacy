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

- **Gregorian** and **Julian** are the calendars of the whole proleptic range: type the date as written in the source and choose the calendar of the source. Both are proleptic and continuous: the Gregorian calendar is also used before its reform, so 5 to 14 October 1582 are accepted as Gregorian days (they are the days the Julian calendar skipped), and the Julian calendar goes on after 1582. The two calendars differ by the days of the reform (4 October 1582 Julian is followed by 15 October 1582 Gregorian), so choose the one the source used.
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
- **Reformed Julian**: the Julian calendar with the reformed leap-year rule, written like the Gregorian one (`1 March 2800`).
- The other calendars are in the table below.

### The other calendars

Choose the calendar and type the date as in the second column; the third column is the Gregorian date the application gets from it (the examples were checked against the application). Month names are written in Latin letters unless the example shows another script; accents are optional where the examples show none.

| Calendar | You type | Gregorian date |
|---|---|---|
| Hebrew | `15 Nisan 5785` | 13 April 2025 |
| Islamic | `1 ramadan 1445` | 11 March 2024 |
| Persian | `1 Farvardin 1403` | 20 March 2024 |
| French Republican | `18 Brumaire 8` | 9 November 1799 |
| Coptic | `1 Thout 1741` | 11 September 2024 |
| Ethiopian | `1 Meskerem 2017` | 11 September 2024 |
| Armenian | `1 Navasard 1` | 13 July 552 |
| Egyptian (Nabonassar) | `1 Thoth 425` | 7 November 323 BC |
| Seleucid | `1 Nisan 1000`, `1 Tishrin Qdim 1000` | 4 April 689, 4 October 688 |
| Byzantine (years from the creation of the world; September year) | `29 May 6961`, `1 Σεπτέμβριος 6961` | 7 June 1453, 10 September 1452 |
| Rumi (Ottoman) | `1 mart 1333`, `15 subat 1332` | 1 March 1917, 28 February 1917 |
| Bahá'í | `1 Bahá 171`, `1 Ayyám-i-Há 182` | 21 March 2014, 25 February 2026 |
| Berber | `1 Yennayer 2976` | 14 January 2026 |
| Parsi (Zoroastrian) | `1 Fravardin 1370 Bastani`, `1 Aspandarmad 1394 Fasli`, `1 Fravardin 1 Kadmi` | 21 March 2000, 14 February 2025, 19 June 632 |
| Buddhist (Thai month names) | `1 มกราคม 2567` | 1 January 2024 |
| Indian national (Saka) | `1 Chaitra 1946`, `1 Pausa 1946` | 21 March 2024, 22 December 2024 |
| Bengali (West Bengal) | `1 Boishakh 1430` | 15 April 2023 |
| Bengali (Bangladesh, fixed calendar) | `1 Boishakh 1431`, `29 Falgun 1431` | 14 April 2024, 14 March 2025 |
| Tamil | `1 Chithirai 1946` | 14 April 2024 |
| Malayalam | `1 Chingam 1200` | 17 August 2024 |
| Nepali (Bikram Sambat) | `1 Baisakh 2080` | 14 April 2023 |
| Vikram Samvat (by tithi) | `VS 2081 Kartika Shukla 1`, `VS 2080 Adhika Shravana Shukla 1` | 2 November 2024, 18 July 2023 |
| Japanese (era years) | `Heisei 1 1 8`, `H1.1.8`, `平成元年1月8日`, `Ansei 7 3 3` | 8 January 1989 (the first three), 24 March 1860 |
| Minguo (Republic of China) | `民國50年3月1日`, `50.3.1`, `民前1年` | 1 March 1961 (the first two), 1 January 1911 |
| Dangi (Korean) | `Dangi 4294`, `단기 4290년 3월 1일` | 1 January 1961, 1 March 1957 |
| Juche | `Juche 113 2 15`, `주체112(2023)년 4월 15일` | 15 February 2024, 15 April 2023 |
| Chinese lunisolar | `2023年 癸卯年 五月 初五` | 22 June 2023 |
| Korean lunisolar | `음력 2024년 1월 1일`, `lunar 2023 1 1` | 10 February 2024, 22 January 2023 |
| Vietnamese lunisolar | `ngày 1 tháng Giêng năm 2024`, `mùng 1 tháng Giêng năm 1985`, `15/8/2023` | 10 February 2024, 21 January 1985, 29 September 2023 |
| Mayan (Long Count) | `13.0.0.0.0` | 21 December 2012 |
| Mayan (solar station of a year) | `Likin 2024`, `Xaman 2024`, `Chikin 2024`, `Nohol 2024` | 20 March 2024, 20 June 2024, 22 September 2024, 21 December 2024 |
| Soviet eternal calendar | `7 ноябрь 1929` | 7 November 1929 |

Notes on the table:

- **Japanese**: from 1 January 1873 (Meiji 6) the months and days are Gregorian; before, they are the lunisolar ones, leap months included (`天保十四年閏九月十日`). An era year beyond the end of the era (`Meiji 46`) is rejected. The first year of an era can be written `元年` or `gannen`.
- **Minguo, Dangi, Juche** count the year from an era; the months and days are Gregorian.
- **Chinese, Korean and Vietnamese** lunisolar dates are computed astronomically, each at the time of its own country, so the new year can fall on different days (Korean 1997: 8 February, Chinese: 7 February). The leap month is marked `閏`/`闰`, `윤` or `nhuận`. For the Korean and Vietnamese ones the year is the Gregorian year in which it mostly falls.
- **Nepali** works only for the years 1970 to 2100. **Vikram Samvat** counts the days of a fortnight from 1 to 15 (`Shukla` the bright half, `Krishna` the dark one), and a leap month is marked `Adhika` before the month name, only in a year that has one (`Adhika Shravana` in 2080, `Adhika Jyeshtha` in 2083; elsewhere the date is rejected).
- **Parsi**: after the year you can name the reckoning (*Kadmi*, *Fasli*, *Bastani*); without it the Shahenshahi one is used.
- **Mayan**: the Long Count has five numbers separated by dots. A station is the equinox or solstice of a Gregorian year: *Likin* (also `Lik'in`, `Lakin`, `Chac`) the March equinox, *Xaman* (also `Yax`, `Sac`) the June solstice, *Chikin* (also `Chik'in`, `Ek`) the September equinox, *Nohol* (also `Kan`) the December solstice. Like the seasons of the Gregorian calendar, the stations need the **Ephemeris plugin**. The station of a year is the one that falls in that Gregorian year, so `Nohol 2024` is the December solstice of 2024, as the *winter 2024* of the Gregorian calendar.
- **Bangladesh** has its own fixed Bengali calendar (the year starts on 14 April, and the year number is the Gregorian one minus 593); the Bengali digits and month names (`১ বৈশাখ ১৪৩১`) are read.
- **Soviet eternal calendar** was in force from 1929 to 1940, with the days and leap years of the Gregorian calendar, so the date is the Gregorian one written with Russian month names. The month is written in the nominative (`январь`, not `января`: the second is rejected), a season (`лето 1930`) needs the Ephemeris plugin, and a year outside 1929-1940 is not rejected.
- **Buddhist** accepts the Thai month names, not the English ones.
- A day that does not exist in the month of that calendar (`30 Falgun 1431` in the Bangladesh variant, which has 29 days) is rejected with a message that says so.

For the calendars other than the Gregorian family, the examples above and in the table are the forms known to work. If you are not sure how to write a date, the [Historical Date Assistant](assistant.md#the-historical-date-assistant) converts between calendars and shows what it understood.

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
