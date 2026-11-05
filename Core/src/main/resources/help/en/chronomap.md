# The Chronomap

The Chronomap shows where and when the people of the file lived. Open it with the **Chronomap** button of the toolbar, with **View > Projection > Chronomap**, or with `Ctrl+4`.

It has three parts that move together: the **map** on the left, the **list** (called *Agorà*) on the right, and the **timeline** along the bottom. The divider between the map and the list can be moved (the default is about two thirds for the map), and has arrows to collapse either side.

The people shown are all the individuals visible under the current [privacy](privacy.md) policy. Groups are not drawn.

## What you need for a person to appear

The Chronomap does not guess places. A person appears at a moment if:

- they are **alive** at that moment: from the earliest birth event (even without a place) until the earliest death event; with no death, for 120 years after the birth. A person with no birth and no dated event is never shown;
- and they have at least one **placed anchor**: an event with a date and a place, or an attribute (such as a residence) with a *Valid From* or *Valid To* and a place. The place must have **coordinates**: its own, or inherited from the place that contains it (see below).

## The timeline

The timeline is a **playhead**: a red line over a year axis, with the year above it.

- **Click or drag** on the axis to move the playhead. Dragging away from the playhead moves the visible window.
- `Ctrl` + the wheel zooms the axis at the cursor. A double-click on the playhead resets the zoom.
- The keys **Left** and **Right** move by one month; with `Shift`, by one year. These keys are taken by the timeline while it is showing.
- A dashed guide follows the mouse and shows the year under it.

Moving the playhead updates the markers, the list and the historical maps. The axis covers all the dates in the file; the playhead starts at the latest one. There is no play button and no animation.

## The map

Where a person is at the playhead is worked out in this order: an event exactly at that date; an attribute whose interval contains it; otherwise a smooth movement along the great circle between the places before and after; before the first place, the first place; after the last, the last. During a journey the marker is elongated, with a trail and a dashed route.

**Markers.** Each person is a coloured dot with their name (the colour is always the same for the same person). The selected person has a thick orange border; a ring marks restricted or confidential people.

**The toolbar of the map**

| Control | What it does |
|---|---|
| **Reset view** | Fits all the live markers (a single marker is shown at a close zoom; none, the whole world), and resets the timeline zoom. Not available in 3D. |
| **Geolocate places** | Restarts the search of the coordinates of the places that are not yet resolved. |
| **2D** and **3D** | Flat map, or a pseudo-3D globe (drag to rotate, wheel to zoom). The centre is kept when you switch. |
| **Historical Map** | See below. |
| **Layers** | **Markers**, **Image overlays**, and one entry for each historical map shown. |
| **Events** | One check box for each event type in the data, to hide the anchors of that type, in the 2D map and in the 3D globe. Unchecking all of them hides every event. Attributes are never filtered. |
| **Show uncertainty** | On by default. Draws a translucent dashed orange circle for the uncertainty of the position of a place. |

On the map, drag to move and use the wheel to zoom; **a click on a marker selects that person**. There is no double-click and no context menu on the map.

The base map is OpenStreetMap, and it needs an **internet connection** (the tiles are kept in a cache on disk, in the `.familylegacy` folder of your home directory).

### Historical maps

**Historical Map** lets the Chronomap lay an old map over the modern one, taken from the public MapWarper collection. Only maps with a known year (the date depicted, or a year or period found in the title) are used.

- With **Auto Select Historical Map** (the default), all the maps whose period contains the date of the playhead **and** whose area contains the centre of the map are shown together, the largest below and the smaller on top, over the modern map.
- **None (modern map only)** turns the old maps off. Choosing one map from the list turns the automatic choice off and shows only that map; it disappears when the playhead leaves its period.
- The catalogue is searched in 4 by 4 degree cells, the first time an area is shown at a close zoom, and remembered for 30 days. Without a connection, only the maps that have already been downloaded are offered.

### Where the coordinates come from

The coordinates of a place are taken, in this order:

1. its own **coordinates** (the *Location* tab of the place);
2. the coordinates of the **place that contains it** (an administrative or geographic *part of* relationship), with a larger uncertainty;
3. a **search by name** on OpenStreetMap, using the full address of the place. This starts by itself whenever the Chronomap opens and with **Geolocate places**, at about one request per second, with a progress strip and a **Cancel** button; the map updates as places are found. Places whose address contains a question mark are not searched. The result, found or not, is remembered, so the next time the search is not repeated.

The circle of uncertainty is never drawn larger than 100 km. Places hidden by the privacy policy are not searched.

## The list (Agorà)

The list shows the people who are alive at the playhead and have a placed anchor: exactly the ones drawn on the map. At the top: the date of the playhead, the number of people alive, and a **Filter** box (any column, case-insensitive; **Clear** empties it).

The columns are *Name*, *Place*, *Reason* (the event or attribute at that date, or *last: ...* or *before first known place*) and *Age* (whole years since the birth). Click a header to sort.

- **One click** selects the person (the marker gets the orange border).
- **A double-click** opens the editor of the individual; the list is rebuilt after you save.
- **Right-click > Center on map** selects the person and centres the map on them (it zooms in if needed, never out).
