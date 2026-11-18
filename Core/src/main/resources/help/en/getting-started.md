# Getting started

## The window

The main window has five parts:

- The **menu bar** at the top. Every command of the application is in a menu.
- The **toolbar** under it, with the commands used most often: back and forward through the history, **Jump To...**, **Edit...**, the buttons that switch the view (**Tree**, **Sugiyama**, **Ego Net**, **Chronomap**) and the **Sidebar** toggle. At its right end the **Assistant** indicator shows how many findings the [Research Assistant](assistant.md) has.
- The **view** in the middle, which draws the family around the selected person. See [Navigating the tree](navigating.md).
- The **sidebar** on the right, with everything recorded about the selected individual: identity, events, sources.
- The **lifespans strip** at the bottom of the tree, which can be shown or hidden.

Menu entries that make no sense in the current situation are greyed out: for example, **Manage Sources** is available only when the file contains sources.

## Your first session

1. Choose **File > Open file...** (`Ctrl+O`) and pick a `.flef` file. To start from data kept in another program, use **File > Import** instead (see [Files](files.md)).
2. The tree shows the family around the root individual. Click a name to make that person the new root, or press `Ctrl+J` to jump to anybody by name.
3. Select a person and press `F2` (or double-click the box) to open the editor. See [Editing people, groups and places](editing.md).
4. There is nothing to save: every change is written to the file by itself. (A new document asks you where to create its file.)

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
- Your preferences (language, font, how media paths are shown) are in the **Appearance** menu. See [Preferences](preferences.md).
- The [shortcuts](shortcuts.md) can also be seen from **Help > Keyboard Shortcuts...**.
