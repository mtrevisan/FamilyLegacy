# Getting started

## The window

The main window has five parts:

- The **menu bar** at the top. Every command of the application is in a menu.
- The **toolbar** under it, with the commands used most often: back and forward through the history, **Jump To...**, **Edit...**, the buttons that switch the view (**Family Tree**, **Sugiyama Graph**, **Ego Net**, **Chronomap**) and the **Sidebar** toggle. At its right end the **Assistant** indicator shows how many findings the [Research Assistant](assistant.md) has.
- The **view** in the middle, which draws the family around the selected person. See [Navigating the tree](navigating.md).
- The **sidebar** on the right, with everything recorded about the selected individual: identity, events, sources.
- The **lifespans strip** at the bottom of the tree, which can be shown or hidden.

Menu entries that make no sense in the current situation are greyed out: for example, **Manage Sources** is available only when the file contains sources.

## How to start

- **You already have a file** (a `.flef`, or data from another program): follow *Your first session* below.
- **You start from nothing**: follow *Your first tree*, further down.

## The guided tour

The first time the application starts without a file to open, a small **Guided Tour** window walks you through the first steps: the file, the people, the events and dates, the participants, the repositories, sources and citations, the views and the sidebar, and undo. Each step outlines on the real window the menu it is about; **Next** and **Back** move between the steps, and `Esc` (or **Skip tour**) closes it. It starts by itself only once: to see it again, choose **Help > Guided Tour...**.

## Your first session

1. Choose **File > Open file...** (`Ctrl+O`) and pick a `.flef` file. To start from data kept in another program, use **File > Import** instead (see [Files](files.md)).
2. The tree shows the family around the root individual. Click a name to make that person the new root, or press `Ctrl+J` to jump to anybody by name.
3. Select a person and press `F2` (or double-click the box) to open the editor. See [Editing people, groups and places](editing.md).
4. There is nothing to save: every change is written to the file by itself. (A new document asks you where to create its file.)

## Your first tree

For when you have no file yet and want to begin with yourself and your family.

1. **File > New file...** (`Ctrl+N`) asks where to create the file and opens it empty. The view is empty: nobody exists yet.
2. **Individual > Add Individual...** creates the first person. A name is a record with *parts*: add a name, and in it one part of type *given* (*Giovanni*) and one of type *family* (*Rossi*). Then set the sex (an attribute of type *sex*) and press **OK**. The views place father and mother by sex, so do not skip it. The new person appears in the tree as the root.
3. **Add the family around that person.** Select the person (a click on the box) and use **Individual > Add Parent...**, **Add Partner...** or **Add Child...** (also in the right-click menu of the box). Each creates a new individual already linked to the selected one and asks the type of the relationship (for example *biological child* for a parent). Click the new person to move on to them, and repeat.
4. **Add the dates.** A date of birth is not a field of the person but an event: see [Changing a date of birth](editing.md#changing-a-date-of-birth), or follow example 2 of the [worked examples](workflows.md).
5. **Say where you read it.** When you start to have documents, record them with **Source > New Source...** and cite them: see [Sources and media](editors-sources.md).
6. Nothing to save: every change is already in the file. If you make a mistake, `Ctrl+Z` undoes it.

Use [Jump To...](navigating.md) (`Ctrl+J`) to come back to anybody, and the [Research Assistant](assistant.md) (`Ctrl+Shift+A`) when the tree grows, to see what is missing or cannot be right.

## How the data is organised

FLEF does not store a birth date *inside* a person. It stores records, and links them:

- an **individual** is a person, with names, sex and attributes;
- an **event** (a birth, a marriage, a burial...) has a date, a place and a type;
- a **participation** says that an individual took part in an event, with a role (for example *principal*);
- a **source** says where a fact was read, and a **citation** says exactly where in the source;
- a **conclusion** is what the researcher decides after weighing the evidence.

This is why, to change a date of birth, you edit the birth *event* of the person. [Editing people, groups and places](editing.md#changing-a-date-of-birth) shows how.

## Things that are good to know early

- **Undo and Redo** (`Ctrl+Z`, `Ctrl+Y`) are in the **Edit** menu, together with **Recent Changes...**, which lists what changed.
- Every dialog closes with `Esc`, and only the dialog that has the focus.
- A word you do not know (participation, conclusion, extract...) is in the [glossary](glossary.md).
- Your preferences (language, font, how media paths are shown) are in the **Settings > Appearance** menu. See [Preferences](preferences.md).
- The [shortcuts](shortcuts.md) can also be seen from **Help > Keyboard Shortcuts...**.
