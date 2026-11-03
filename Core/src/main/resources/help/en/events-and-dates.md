# Events and dates

## Events

An event is something that happened: a birth, a marriage, a burial, a move. It has a **type**, a **date**, a **place**, a description and a confidence. The people who took part are linked to it through *participations*.

The **Event** menu groups the commands:

- **New Event...** creates an event.
- **Manage Events...** lists every event with its date, place and number of participants. You can search, create, edit and delete events. It is available only when the file has events.
- **Manage Participants...** lists every participation (event, participant, role) and lets you create, edit and delete them. It is the companion of the event editor: there you say what happened, here you say who took part and in which role.
- **Manage Event Types...** lists the event types, both the ones declared by the format and the custom ones, with the number of events using each. The list is read-only: it is useful to audit which types are really in use, and it can show all the events of a given type in one click.

## Dates

Historical dates are rarely a clean `2020-01-31`. A date field is meant for what the records actually say: partial dates (only a year, or a month and a year), approximate ones, and dates in other calendars. How to fill it, what you can type and what the messages mean are in [Entering dates](dates.md).

The **Historical Date Assistant** helps to interpret, convert and normalise a date. It suggests and never changes anything by itself; it is explained in [The Research Assistant](assistant.md#the-historical-date-assistant).

## Date tools in the Event menu

- **Calendar Converter...** converts a date between the Gregorian calendar and the other calendars it offers.
- **Date Calculator...** answers the questions most common in genealogy: the age at a date, the interval between two dates, and the estimated date of birth from a date and an age.
- **Timeline View...** lists every event in chronological order and can be filtered by date range, participant and event type. Events with a partial or missing date are placed at the end, so none of them is silently dropped.
- **Context Board...** shows the historical and cultural context as a tree: historic events and cultural norms, each with the impacts it had on the records it influences. Selecting a node shows its details; double-clicking it opens its editor; the filter field keeps only the matching branches.

## Calendars

Dates can be written in the calendar of the document. Among the calendars the application knows are the Julian and Gregorian ones (with the reform date that applies to the country), the Venetian, Florentine and Pisan year starts, the Swedish calendar, the French Republican, Hebrew, Islamic, Persian, Coptic, Byzantine and Chinese calendars, and regnal years. The [Historical Date Assistant](assistant.md#the-historical-date-assistant) converts between them.

## Tips

- Prefer entering a date as it is written in the source, in the calendar of the source, and let the application convert it, rather than converting by hand.
- A date you are not sure of is better entered as approximate or as a range than as a precise date you invented.
- The [Research Assistant](assistant.md) reports dates that cannot all be right: a death before a birth, a child born before its parent, an event after a death.
