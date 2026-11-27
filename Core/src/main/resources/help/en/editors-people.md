# People and groups

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md).

## Individual

Open it with **Individual > Edit Individual...** (or `F2`); **Individual > Add Individual...** creates a new one.

**Tabs:** Properties, Attributes, Relationships, Participations, Context, Research, Sources, Notes, Privacy, Audit. The **Notes** tab is always there. From the *standard* [interface level](preferences.md#interface-level) it has two lists: **Stories** above (the notes of kind *story*: narrative prose meant to be read and published, a biography, a family tradition, a migration) and **Notes** below (all the other notes); below that level it has the one list of all of them, each with its *Kind*. A new item of the Stories list starts as a story, and changing the *Kind* of an item moves it at once to the other list.

**Properties**

- **Quick entry** (above the rest): the most common facts in one place, without knowing which records they are made of. A new individual has the full name too.
  - **Full name**: type the whole name (*Maria de la Cruz*, *Rossi, Giovanni*, *Giovanni "Gianni" Rossi*, *Dr. John Smith Jr.*) and press `Enter` or **Add as a name**. It is split into the parts of a name (given, family prefix, family, nickname...) and added to the **Personal Names** below, where you see the result and can correct it with the [personal name editor](#personal-name). A name still typed when you press **OK** is added as well. The split is only a proposal: the family name is the last word together with the particles before it (*de*, *de la*, *van der*, *von*, *al*...; modern and historical forms, in many languages), and two family names joined by *y*, *e* or *i* (*Ortega y Gasset*) are one. When the family name is longer (*Garcia Marquez*), put it between slashes (*Maria /Garcia Marquez/*) or correct the part afterwards.
  - **Occupation**: what the person did for a living, as a text (the attribute of type *occupation*). One occupation: for several, or with a source, a confidence, dates or a place, use the **Attributes** tab (the note under the field is a link that opens it with the attribute selected; the same holds for an occupation that is one of several or has details of its own). For a person that exists the field shows its occupation and changes its text in place (emptying it removes the attribute), unless the person has more than one occupation or the one it has has a source, a confidence, dates or a place of its own: then the field is disabled, so that these are not left pointing at another occupation.
  - **Sex**: *male*, *female* or *unknown*. It writes the *sex* attribute (nothing if unknown). It is fixed when the person is created as a father or a mother, whose sex the link imposes. If you also add a *sex* attribute with another value in the **Attributes** tab, **OK** asks you to fix one of them.
  - **Birth**, **Death** and **Burial**: a **Date**, typed as in the [date quick entry](dates.md) (*12 March 1850*, *abt 1850*, *bet 1850 and 1855*, *1850s*; what it means is shown under the field), and a **Place**: a name, or pick a place of the file (right-click the field and choose **Set...**). A place can be typed with the places that contain it, from the nearest to the largest, separated by commas (*Padova, Veneto, Italy*): each name is a place of its own, part of the next one (an administrative jurisdiction), instead of one place with that whole text for a name. If the file already has places with that name you are asked which one to use, or whether to create a new place: you are asked about the largest place of the chain, and a name inside the place you chose is that place if the file has only one with the name there (two places that are called the same in different regions stay two places). Next to the date, a combo box chooses its **calendar**: the Gregorian one unless you choose another, and then the date is typed in the notation of that calendar, as in the [date editor](dates.md). The date is checked with its calendar, as the date editor does: under the field you see at once if it is not a date (*31 February 1850*), or can be read in more than one way (*est 1850* is the summer in Italian and August in Cornish), and on **OK** you are asked to choose the reading, or whether to keep a date that the calendar does not recognize (the answer that is proposed is to correct it). On **OK**, each fact that has a date or a place becomes an event (*birth*, *death*, *burial*), with the person as its *principal* participant: the same records as [Changing a date of birth](editing.md#changing-a-date-of-birth) describes. Nothing is written if you cancel.

  **An existing individual** has the box titled *Sex, birth and death*, without the full name. It shows what the file says (the first birth, death and burial event of which the person is the *principal*; if there are more, *+N more* is a link: click it to open the **Participations** tab with the next event selected) and changes **only what you edit**, in place, when you press **OK**; nothing changes if you cancel. A date or a place that is *cited* (a source or evidence on the date, a citation on the place) is shown but cannot be edited here: edit it in the event, so that the citation is not left pointing at a different value (a link under the row takes you to it; two sources that disagree are two assertions, not one overwritten). Emptying a date or a place removes it from the event, which stays. A birth or a death that the person does not have yet becomes a new event. The sex changes its *sex* attribute (*unknown* keeps the attribute, with that value); if the person has more than one *sex* attribute the field is disabled: use the **Attributes** tab. The audit of the event records the change.

  The tabs remain the full way to enter everything else.
- The **preferred image**: left-click to choose an image, right-click for more options such as *Edit Crop...*, which sets the part of the image shown in the boxes. In the crop dialog you draw the rectangle by dragging, and move or resize it with its edges and corners (`Ctrl` + drag pans the image, `Ctrl` + wheel zooms, right-click fits it again). With the **face detector** installed (see *Optional components* in the troubleshooting page) the faces are found when the dialog opens: each one gets a dashed, faded rectangle that is the whole head in the proportions of the portrait, and a **click** inside one of them (without dragging) makes it the crop, which you can then edit by hand as any other. A click where no rectangle is (a profile, someone seen from behind, a damaged photo) does nothing: draw the crop by hand. Without the detector the dialog is only the hand drawing.
- **Personal Names**: the list of the names of the person. Each opens the [personal name editor](#personal-name).

**Other tabs**

- **Attributes**: the *Individual Attributes*. The **sex** of the person is one of them: an attribute of type *sex* with the value *male*, *female* or *unknown* (see [the sex and the preferred assertion](#individual-attribute)). It matters: the views use the sex of the parents to put the father and the mother in their places. See [Relationships](relationships.md#how-the-views-read-the-relationships).
- **Relationships**: *Relationships* (this person is the subject) and *Inverse Relationships* (this person is the target). See [Relationships](relationships.md).
- **Participations**: the *Event Participations*. A birth date, for instance, is changed in the Quick entry of the **Properties** tab, or through the birth event: see [Editing people, groups and places](editing.md#changing-a-date-of-birth).
- **Context**: *Context Impacts*. **Research**: *Conclusions*, *Identity Hypotheses* and *Research Questions*.

The editor does not check anything on OK, not even that the person has a name.

## Individual attribute

A characteristic or status of a person that is not an event: an occupation, a residence, a title, a nationality, the number of children. When it started or ended is told by its dates. Open it from the **Attributes** tab of the individual (*Create New...* or *Edit...*).

**Tabs:** Properties, Context, Research, Sources, Notes, Privacy, Audit.

| Field | Meaning |
|---|---|
| **Type** | Editable. One of: sex, characteristic, residence, occupation, possession, military rank, caste, social class, ethnicity, citizenship, nationality, national ID, title, children count, marriages count, religion, language, literacy, education, apprenticeship. |
| **Value** | The value of the attribute (for an occupation, the occupation; for a sex, *male*, *female* or *unknown*: when the type is *sex* the box becomes a list of these three values). |
| **Validity Range** | **Valid From** and **Valid To**. |
| **Place** | Where it held (a place with its citations). |
| **Confidence** | See [The record editors](editors.md#confidence). |

On OK the type is required, and the attribute must belong to an individual; for a *sex* the value must be *male*, *female* or *unknown*, and it cannot be the same as the sex of someone who is a *biological child*'s other parent (the two biological parents of a child must be of different sexes; the generic *child* link says nothing about the parentage, so it never conflicts). Marriages and civil unions have no such limit: two people of the same sex can be spouses, just not biological parents of the same child.

**The sex and the preferred assertion.** A person can have several *sex* attributes, for instance one for each source that states it, and they may disagree. The application uses one of them, the **preferred** one, chosen in this order:

1. the preferred assertion of the [conclusion](editors-research.md) that resolves them (at least two of them, with the preferred one among them): the strongest proof status (*proven*, then *supported*), and among equals the most recent;
2. otherwise the only one, if there is only one;
3. otherwise the one with the strictly highest **Confidence**;
4. otherwise the sex is undetermined (shown as unknown).

An assertion whose value is *unknown* says nothing, so a *male* and an *unknown* give *male*. Nothing is ever deleted: the other assertions stay in the file. Files written before the sex became an attribute are converted when they are opened: the sex of the person becomes a *sex* attribute, dated as the person, with the note *Migrated from the sex of the individual* in its audit.

## Group

Open it with **Group > Edit Group...**; **Group > Add Group...** creates one.

**Tabs:** Properties, Attributes, Relationships, Participations, Context, Research, Sources, Notes, Privacy, Audit (from the standard level the Notes tab has a list of the stories above the other notes, as in the editor of an individual).

**Properties**

- The **preferred image**, as for an individual.
- **Name**: the list of the names of the group. Each opens the [name editor](#name).
- **Type**: editable. One of: family, household, neighborhood, fraternity, club, literary society, association, organization, tribe.

The **Relationships** tab shows *Group Memberships* (who belongs to the group) and *Inverse Relationships*. The **Attributes** tab lists the *Group Attributes*. Nothing is checked on OK.

## Group attribute

The same as an individual attribute, for a group (a family, a parish, a confraternity...). Open it from the **Attributes** tab of the group.

| Field | Meaning |
|---|---|
| **Type** | Editable. One of: residence, member count, children count, social class, ethnicity, religion, language, wealth, land holding, primary-income source. |
| **Value**, **Validity Range**, **Place**, **Confidence** | As for an individual attribute. |

## Personal name

Open it from the **Personal Names** list of an individual. **Tabs:** Properties, Context (the cultural norms that explain the name), Sources, Notes. The **Sources** tab also has a *Media* list: the media that attest *this form of the name* (a signature, a register entry, an inscription, a document that spells the name this way), each with its own caption, crop and page, as in [the media of a record](editors.md#media-of-a-record). It says nothing about the person in general, and it never changes the preferred image.

| Field | Meaning |
|---|---|
| **Type** | Editable. One of: official, religious, birth, married, maiden, divorce, adoption, fostering, legal, immigrant, adapted, alias, nickname, artistic, professional, pen name, regnal, slave name, placeholder (a name not known, so that the individual still has something to display). |
| **Parts** | The parts of the name, each in its own dialog. At least one is required. |
| **Locale** | The language of the name (a filterable list of language tags). |
| **Validity Range** | When the person bore this name. |

### Name part

| Field | Meaning |
|---|---|
| **Type** | Editable (required). One of: given, generation, patronymic, matronymic, kunya, family, family nickname, lineage, house, clan, tribal, caste, toponymic, title, occupational, prefix, suffix, nickname, regnal, religious, posthumous. |
| **Value** | The text of the part (required). |
| **Variant** | Alternative forms of the part, see [Name variants](#name-variants). |

## Name

Used for the names of **groups, places, sources** (the title) and **repositories**. **Tabs:** Properties, Sources, Notes.

| Field | Meaning |
|---|---|
| **Name** | The name itself (required). |
| **Type** | Editable. One of: official, legal, colonial, indigenous, traditional, translated, transcribed, historic, common, colloquial, abbreviated, acronym, religious, administrative, archival. |
| **Locale** | The language of the name. |
| **Variant** | Alternative forms, see below. |
| **Validity Range** | When the name was in use. |

## Name variants

A name or a part of a name can have variants. You choose the kind:

- **Phonetic**: a **System** (for example *ipa*, *rōmaji*, *pinyin*, *wadegiles*) and the **Value** in that system.
- **Transcription**: a **System** (a list: rōmaji, Hepburn, kunrei-shiki, nihon-shiki, pīnyīn, Wade-Giles, BGN/PCGN, ISO 9, ALA-LC, DMG, Buckwalter, ISO 233, ISO 259, IAST, ISO 15919, hunterian, McCune-Reischauer, revised korean, scientific; editable), a **Type** (romanized, latinized, anglicized, francized, germanized, italianized, hispanicized, lusitanized, cyrillized, arabized, hebraized, hellenized, gairaigized, kanaized, hangulized, modernized, normalized; editable) and the **Value**.

System and Value are required, for both kinds.
