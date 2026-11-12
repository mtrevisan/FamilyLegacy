# People and groups

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md).

## Individual

Open it with **Individual > Edit Individual...** (or `F2`); **Individual > Add Individual...** creates a new one.

**Tabs:** Properties, Attributes, Relationships, Participations, Context, Research, Sources, Notes, Privacy, Audit.

**Properties**

- The **preferred image**: left-click to choose an image, right-click for more options such as *Edit Crop...*, which sets the part of the image shown in the boxes.
- **Personal Names**: the list of the names of the person. Each opens the [personal name editor](#personal-name).

**Other tabs**

- **Attributes**: the *Individual Attributes*. The **sex** of the person is one of them: an attribute of type *sex* with the value *male*, *female* or *unknown* (see [the sex and the preferred assertion](#individual-attribute)). It matters: the views use the sex of the parents to put the father and the mother in their places. See [Relationships](relationships.md#how-the-views-read-the-relationships).
- **Relationships**: *Relationships* (this person is the subject) and *Inverse Relationships* (this person is the target). See [Relationships](relationships.md).
- **Participations**: the *Event Participations*. A birth date, for instance, is changed through the birth event: see [Editing people, groups and places](editing.md#changing-a-date-of-birth).
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

On OK the type is required, and the attribute must belong to an individual; for a *sex* the value must be *male*, *female* or *unknown*.

**The sex and the preferred assertion.** A person can have several *sex* attributes, for instance one for each source that states it, and they may disagree. The application uses one of them, the **preferred** one, chosen in this order:

1. the preferred assertion of the [conclusion](editors-research.md) that resolves them (at least two of them, with the preferred one among them): the strongest proof status (*proven*, then *supported*), and among equals the most recent;
2. otherwise the only one, if there is only one;
3. otherwise the one with the strictly highest **Confidence**;
4. otherwise the sex is undetermined (shown as unknown).

An assertion whose value is *unknown* says nothing, so a *male* and an *unknown* give *male*. Nothing is ever deleted: the other assertions stay in the file. Files written before the sex became an attribute are converted when they are opened: the sex of the person becomes a *sex* attribute, dated as the person, with the note *Migrated from the sex of the individual* in its audit.

## Group

Open it with **Group > Edit Group...**; **Group > Add Group...** creates one.

**Tabs:** Properties, Attributes, Relationships, Participations, Context, Research, Sources, Notes, Privacy, Audit.

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

Open it from the **Personal Names** list of an individual. **Tabs:** Properties, Context (the cultural norms that explain the name), Sources, Notes.

| Field | Meaning |
|---|---|
| **Type** | Editable. One of: official, religious, birth, married, maiden, divorce, adoption, fostering, legal, immigrant, adapted, alias, nickname, artistic, professional, regnal, slave name. |
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
- **Transcription**: a **System** (a list: rōmaji, Hepburn, kunrei-shiki, nihon-shiki, pīnyīn, Wade-Giles, BGN/PCGN, ISO 9, ALA-LC, DMG, Buckwalter, ISO 233, ISO 259, IAST, ISO 15919, hunterian, McCune-Reischauer, revised korean, scientific; editable), a **Type** (romanized, latinized, anglicized, francized, germanized, italianized, hispanicized, lusitanized, cyrillized, arabized, hebraized, hellenized, gairaigized, modernized, normalized; editable) and the **Value**.

System and Value are required, for both kinds.
