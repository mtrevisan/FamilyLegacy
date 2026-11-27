# Historical timeline and lifespan comparison

**Event > Historical Timeline...** puts the life of people against the history of their time and places. It opens in a modal dialog (`Esc` closes it) and is for looking: it needs at least one individual. If an individual is selected when you open it, it starts from them.

The historical context is made of the **historic events** and the **cultural norms** of the file (see [Editors for events, places and context](editors-events-places.md)). They are only context: a historic event does not say that a person took part in it. What the file says about a person is the **context impact** that links a historic event or a norm to the person, to one of their events or to one of their attributes.

## Life and history

The first tab shows one individual (**right-click** the name and choose **Set...**, or double-click it, to pick another one). The top part is a strip with the bar of their life, from the birth to the death, their events, and, behind it, the bands of the historic events and norms of the places where they lived. It zooms and moves like the other timelines (`Ctrl` + the wheel zooms at the cursor, a drag moves along the axis, a double-click opens the record).

Under the strip is **one list**, in chronological order, with the events and attributes of the person and the historic events and norms of their time. The columns are:

- **Date** (sorted by the day, not as text) and **Age**: the age of the person on the first day of the item, in whole years. There is no age if the birth is not known, or if the item is before the birth or after the death.
- **Kind**: *Life event*, *Attribute*, *Historic event* or *Cultural norm*; the last two are shaded, and in bold if a context impact links them to the person.
- **Title**, **Description** and **Place**; **Scope** and **Impact** for the historic events and norms.

Double-click a line to edit it. **Copy all** puts the list in the clipboard; **Save as SVG...** saves the strip (see below).

**Which historic events are listed.** A historic event or a norm is listed if its dates overlap the **lifetime** of the person, from the birth to the death. With no birth, the lifetime begins at the earliest date of the person; with no death, it ends at the latest one: the status line says *estimated* when it is so, and no age is computed from an estimated birth. What a **context impact** links to the person (or to one of their events or attributes) is always listed, even before the birth or far away: it is in the file because it explains something.

**How far from the person.** A historic event is placed by its place and the places of the person (the places of their events), following the place hierarchy (*part of*, see [Places](editors-events-places.md)):

| Scope | What it is |
|---|---|
| **Local** | In one of their places, or inside one (a village, a parish, a city). |
| **Regional** | In an area that contains one of their places (a province, a region, a diocese). |
| **National** | In a country, a state or an empire that contains one of their places. |
| **World** | With no place at all, or in a continent. |
| **Elsewhere** | In a place that has no relation to their places. |

The level is the one the **type** of the place says; a place with no type is taken as local if it is inside a place of the person, and regional, then national, as the places that contain it are further away. The dates of the place relationships are not considered. The check boxes above the list choose the scopes (**Elsewhere** is off at first), and add or remove the **Cultural norms** and the **Attributes**. The local events come first among those of the same day. The strip follows the same choice: with **Elsewhere** off it shows only the context of the places of the person.

## Lifespan comparison

The second tab puts **several individuals** on a shared axis. Add them with **Add...** (a person already there is not added twice) and take them away with **Remove**. Each has a row, the earliest born first, with the bar of their life (labeled with the birth and death dates, a *?* for the one that is not known), their events and the historical context that a context impact links to them. A person with no dated event is left out.

The options are:

- **Main events only**: birth, marriage, divorce, death, burial, emigration and immigration, instead of all the events;
- **Attributes**: draws the attributes, with their dates;
- **Historic context**: the bands of the historic events and norms behind the rows;
- **Only the places of the individuals**: leaves out the bands of the places that have no relation to the places of any of the people (as the **Elsewhere** scope of the first tab). The events with no place stay.

## Saving as SVG

**Save as SVG...**, in both tabs, writes what you see as an SVG image: the rows, the bars with their labels, the bands and a year axis. The file has no script nor external reference, so it opens in a browser or in a vector editor, and a PDF is made by printing it from there. The bars have a tooltip with the label and the dates. The image is as wide as 1200 pixels and as high as the rows need.

## Related

- The events of everyone, in a table with filters: **Event > Timeline View...** ([Events and dates](events-and-dates.md)).
- The same chronological table as a report section: the **Chronological timeline** of [Reports](reports.md).
- How a place changed jurisdiction in time, and the group windows: [Group timeline, group network and memberships over time](group-views.md).
- Dates that cannot be read are left out of the timelines: see [Dates](dates.md).
