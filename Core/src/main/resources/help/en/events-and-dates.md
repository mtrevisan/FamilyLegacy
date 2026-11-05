# Events and dates

## Events

An event is something that happened: a birth, a marriage, a burial, a move. It has a **type**, a **date**, a **place**, a description and a confidence. The people who took part are linked to it through *participations*.

The **Event** menu groups the commands:

- **New Event...** creates an event.
- **Manage Events...** lists the events, one per line: type, date, the complete place, the people who took part and the description, as in `birth (2 MAR 1850) – Borgo, Treviso, Veneto – Mario Rossi, Anna Bianchi +2 – Baptised at home [E12]`. The place is written with the places that contain it (*Borgo, Treviso, Veneto*), the principal participants come first, at most three are named and the others are counted (*+2*). You can search, create, edit and delete events; it is the same dialog as the other *Manage* commands, see [Manage dialogs](searching.md#manage-dialogs). It is available also in a file without events, so the first one can be created from it.
  - The text you type is matched against the whole line, so an event is found by its type, its date, its place or the name of a participant. The filters are *Event type*, *Description*, *Date* (with the calendar), *Place* and *Cause reason*.
  - The *Place* filter, like the text, also matches the places that contain the place of the event: *Veneto* finds an event held in Borgo, if Borgo is part of Treviso and Treviso of Veneto. An event with no place is not found when you filter by place.
- **Manage Participants...** lists every participation, one per line: the participant, the role and the event (type, date and place), such as `Mario Rossi (as principal) in birth (2 MAR 1850) – Borgo, Treviso, Veneto [E12] [EP3]`. You can search, create, edit and delete participations. The filters are *Role*, *Event type* and *Participant* (individual, group or place); the text finds the name of the participant and the data of the event. It is the companion of the event editor: there you say what happened, here you say who took part and in which role. It is available when the file has events.
- **Manage Event Types...** lists the event types, both the ones declared by the format and the custom ones, with the number of events using each and whether the type is declared or custom. *Birth* and *birth* are the same type: they are counted together. The list is read-only: it is useful to audit which types are really in use. Select a type and press **Show events** (or double-click it) to see its events, with the date and the complete place; double-click an event, or press **Open record**, to edit it.

## Dates

Historical dates are rarely a clean `2020-01-31`. A date field is meant for what the records actually say: partial dates (only a year, or a month and a year), approximate ones, and dates in other calendars. How to fill it, what you can type and what the messages mean are in [Entering dates](dates.md).

The **Historical Date Assistant** helps to interpret, convert and normalise a date. It suggests and never changes anything by itself; it is explained in [The Research Assistant](assistant.md#the-historical-date-assistant).

## Date tools in the Tools menu

The tools that work on dates, and not on events, are in the **Tools** menu, under the Research Assistant group. They need no file to be open.

- **Calendar Converter...** writes a date in every calendar of the converter at once. Choose the calendar of the date and type the year, month and day: the table shows the same day in the Gregorian, Julian, Hijrah (Islamic), Minguo, Thai Buddhist and Japanese calendars, with its Julian Day Number and the day of the week. The calendar you entered is marked with *. The dates are written as each calendar has them, without the Gregorian reform of 1582 (the Gregorian and the Julian calendars are the proleptic ones). A date that does not exist (30 February, 29 February 1900 in the Gregorian calendar) is said so. **Copy all** puts the table in the clipboard. **Other calendars...** opens the Historical Date Assistant, which reads the calendars of the sources (Venetan, Florentine, Pisan, the years that begin at the Nativity or at Easter, regnal, liturgical, ...).
- **Date Calculator...** answers the questions most common in genealogy, in three tabs: **Age at date** (from a birth date and a reference date, which starts as today), **Interval** (the time between two dates; with a minus sign if the second one is before the first) and **Estimated birth** (from a date and an age in years, months and days). The dates are Gregorian and are written as `YYYY-MM-DD`; the format is shown in the empty box. The result follows every change, and a text that is not a date, or a reference date before the birth, is said in red under the box instead of leaving the result empty. For a Julian date, or an age written as *in the 72nd year*, the button **Julian dates and other ages...** opens the Historical Date Assistant on its age tool.
- **Historical Date Assistant...** opens the assistant on its own, to read and copy its suggestions. Opened from here it has no **Apply to Date** button, because there is no date to apply to: to put a suggestion in a date, open the assistant from the date field of an editor (see [The Research Assistant](assistant.md#the-historical-date-assistant)).

## Views of the events in the Event menu

- **Timeline View...** lists the events in chronological order, one per line: date, type, complete place, the people who took part and the description. If an individual is selected in the view when you open it, the list is the one of that individual (the **Participant** box shows the name; **Clear** lists everyone, **Choose...** picks another one). The other filters are **From** and **To** and the **Type** of event.
  - **From** and **To** take a year (`1850`), a month (`1850-03`) or a day (`1850-03-02`): the start of the period for *From*, its end for *To*. A text that is not a date is shown in red and does not limit the list.
  - An event is placed by the first day its date can be, so an event of "1850" comes at the start of 1850 and not at the end of the list, and an interval (*between 1900 and 1910*) is placed at its beginning. An event is listed if some day of its date is in the interval you asked for.
  - Events with no date, or with a date that cannot be read, come last. When you ask for a date interval they cannot be placed and are not listed: the status line says how many they are.
  - Click a heading to sort by that column (the date column is sorted by the day, not as text). Double-click an event, or press **Open record**, to edit it; **Copy all** puts the list in the clipboard.
- **Context Board** is now in the **Research** menu: see [Research](research.md#the-context-board).

## Calendars

Dates can be written in the calendar of the document. Among the calendars the application knows are the Julian and Gregorian ones (with the reform date that applies to the country), the Venetian, Florentine and Pisan year starts, the Swedish calendar, the French Republican, Hebrew, Islamic, Persian, Coptic, Byzantine and Chinese calendars, and regnal years. The [Historical Date Assistant](assistant.md#the-historical-date-assistant) converts between them.

## Tips

- Prefer entering a date as it is written in the source, in the calendar of the source, and let the application convert it, rather than converting by hand.
- A date you are not sure of is better entered as approximate or as a range than as a precise date you invented.
- The [Research Assistant](assistant.md) reports dates that cannot all be right: a death before a birth, a child born before its parent, an event after a death.
