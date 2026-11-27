# Building a tree: a walkthrough

A step-by-step example for when you start from nothing: a new file, and a family that grows from you towards the ancestors and the descendants. Every step says which command to use and what you will see. Nothing needs to be saved: every change is written to the file by itself, and `Ctrl+Z` takes back the last one (see [Undo, redo and recent changes](undo.md)).

The example follows Anna Rossi, born in Padova in 1976, whose parents are Giovanni Rossi and Maria Bianchi. Use your own people instead.

## 1. A new file

**File > New file...** (`Ctrl+N`) asks where to create the file and opens it empty. Choose a folder of its own: the file and the folder `.meta` that the application writes next to it belong together (see [Files](files.md#one-folder-for-each-project)). The view is empty because nobody exists yet.

## 2. A person and the two generations around them

Open **Individual > Quick Entry...** (`Ctrl+Shift+N`). It is one window for a whole small family; **fill only what you know**, because a person with nothing in it is not created.

1. In **Person**, type the full name (*Anna Rossi*), choose the sex, and type the birth: the date (*1976*, or *5 May 1976*) and the place (*Padova*). Under the name the window shows how it will be split into the given name and the family name; if a family name is written first (*Rossi Anna*), put it between slashes (*/Rossi/ Anna*).
2. In **Parents**, fill **Father** (*Giovanni Rossi*) and **Mother** (*Maria Bianchi*) in the same way, with the dates you know. Each parent has a **Relationship**, *biological child* by default: change it only for an adoptive, foster or step parent.
3. If the person has a partner or children, fill **Partner** (with the kind of union, the date and the place of the wedding) and **Children**: **Add a child** adds a block for each. See [Quick Entry of a family](editing.md#quick-entry-of-a-family) for every field.
4. Press **OK**. A line at the right end of the tool bar says what was created, and the tree shows Anna with her parents.

If the result is not what you wanted, **Edit > Undo** takes back everything the window created in one step.

## 3. Going up: the grandparents

Click the box of Giovanni to select him, and open **Individual > Quick Entry...** again: the window offers *The selected individual: Giovanni Rossi* as the person, so you only fill the **Parents** (his father and mother), and his partner and children if you want them. Do the same for Maria. This is the quick way to enter a tree from the bottom up, one generation after the other.

To add just one parent, select the person and use **Individual > Add Parent...** (also in the right-click menu of the box): you type the new person in the usual window and it is linked at once as a *biological* parent, without a second window for the type. To choose another type (adoptive, foster, step), choose the menu item holding `Shift`, or change the type afterwards in the relationship (see [Relationships](relationships.md)).

## 4. Going down and sideways

- **Partners.** Select a person and choose **Add Partner...**: after the new person you are asked the kind of relationship and, for the kinds that begin with an event (a marriage, an engagement), the date and the place; the event is made together with the relationship.
- **Children.** **Add Child...** creates the child already linked to the selected person. If that person has partners, the same window asks who the other parent is (the partner is preselected), so that the child has both parents.
- **Brothers and sisters.** Select one of the parents and add a child to them, or select a child and use **Add Parent...** only if the parents are not there yet.
- **A person that is already in the file.** Use **Connect Parent...**, **Connect Partner...** or **Connect Child...**: they work like the commands above but pick an existing person from a search instead of creating one. This is what you need when the same person turns up from two sides of the family.

Click a new person to make them the center of the view and repeat. **Jump To...** (`Ctrl+J`) takes you back to anybody by name; **Back** in the tool bar returns to the previous person (see [Navigating the tree](navigating.md)).

## 5. Dates and places

- A date is typed as the document writes it. In the quick boxes you can use short forms (*abt 1850*, *bef 1900*, *bet 1850 and 1860*, *1850s*); see [Entering dates](dates.md). The calendar is checked for you: a date that did not exist in that calendar is refused.
- A place may be typed with the places that contain it: *Padova, Veneto, Italy* makes the three places and links them. A place that the file already has is not duplicated silently: you are asked which one to use.
- Death, burial and occupation are in the window of the person (*Individual > Add Individual...* or *Edit Individual...*, in the **Quick entry** at the top), and the death is also in the Quick Entry of a family.
- For a person that exists, a date is an event: see [Changing a date of birth](editing.md#changing-a-date-of-birth).

## 6. Where you read it

When you start to have documents, record them: **Source > Cite a New Source...** is the short way (a plain title, and *supports* by default), and **Source > New Source...** the complete one. Cite the source on the fact it proves, not on the person. See [Sources and media](editors-sources.md), and [Worked examples](workflows.md) for a person recorded from a parish register.

## 7. Looking at what you have

- Switch the view with the buttons of the tool bar: the family tree, the graph, the ego network (see [The tree, the graph and the ego network](views.md)).
- **Tools > Validate File...** and the [Research Assistant](assistant.md) (`Ctrl+Shift+A`) point out what is missing or cannot be right: for instance a child born before its parent, or a death before the birth.
- **Reports** make a document of what you have (see [Reports](reports.md)).

## Habits that help

- Enter a generation at a time, and the dates later: a person with only a name is already useful, and everything can be completed by editing the person.
- Put the doubtful information in with a **Confidence** and a source rather than leaving it out (see [Research](research.md)).
- A mistake is cheap: `Ctrl+Z` takes it back, and **Edit > Recent Changes...** lists what changed.
