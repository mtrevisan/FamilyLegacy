# Events, places and context

For what all the editors share (tabs, confidence, privacy, audit) see [The record editors](editors.md).

## Event

Open it with **Event > New Event...**, or from **Event > Manage Events...**, or from a participation. **Tabs:** Properties, Cause, Participations, Context, Research, Sources, Notes, Privacy, Audit.

**Properties**

| Field | Meaning |
|---|---|
| **Type** | Editable: the kind of event (see the list below). |
| **Description** | A free description of what happened (required). |
| **Date** | A [date field](dates.md). |
| **Place** | Where it happened (a place with its citations). |
| **Confidence** | See [The record editors](editors.md#confidence). |

**Cause** (for an event that has one, such as a death): **Reason** (text), the **Sources with Citations** that support the cause, and **Evidence**, a rating of low, medium or high for the cause. **Participations** lists the *Event Participations*; **Research** lists the conclusions and questions.

On OK, the type and the description are required.

**Event types**

| Group | Types |
|---|---|
| Life | birth, adoption, death, cremation, burial, coroner report, illness, hospitalization, medical procedure |
| Family | engagement, marriage bann, marriage contract, marriage license, marriage settlement, marriage, separation, divorce filed, divorce decree, divorce, annulment |
| Achievements and service | education, graduation, retirement, military induction, military muster roll, military service, military award, military release, military discharge, military resignation, military retirement, prison, pardon, jury duty, honor, bankruptcy |
| Nation and government | emigration, immigration, naturalization, deportation, internment, liberation, emancipation, relocation, census |
| Possessions and titles | deed, escrow, chancery, will, probate, guardianship |

## Event participation

It says that an individual, a group or a place took part in an event, and in which role. The same event can have several participants. Open it from the **Participations** tab of an individual, group, place or event, or with **Event > Manage Participants...**.

| Field | Meaning |
|---|---|
| **Participant** | An individual, a group or a place. |
| **Event** | The event. |
| **Role** | Editable; the list depends on the participant (below). |
| **Confidence** | How sure you are that the participation is true. |

**Roles.** For an individual or a group: principal, sponsor, power-of-attorney, prisoner, witness, officiant, informant, executor, grantor, grantee, landlord, tenant, soldier, commander, victim, survivor, accused, judge. For a place: *property*. Until you choose the participant, the whole list is shown.

On OK, the participant and the event are required.

## Place

Open it with **Place > New Place...** or **Place > Manage Places...**. **Tabs:** Properties, Location, Relationships, Participations, Context, Research, Sources, Notes, Privacy, Audit.

**Properties**

- **Names**: the list of the names of the place, each in a [name editor](editors-people.md#name). At least one is required.
- **Type**: editable. One of: address, building, street, hamlet, village, town, municipality, city, metropolitan area, county, province, department, district, region, macro region, country, empire, parish, diocese, archdiocese, cemetery.
- **Confidence**.

**Location**: the **Coordinates** (free text), the **Sources with Citations** that support the location, and a **Confidence** for the location.

**Relationships** lists the place relationships where this place is the subject and the ones where it is the container (*Members*). See [Relationships between places](relationships.md#relationships-between-places).

### Place relationship

Open it from the **Relationships** tab of a place. **Tabs:** Properties, Context, Research, Sources, Notes, Audit (no Privacy tab).

| Field | Meaning |
|---|---|
| **Subject** | The place being described. |
| **Target** | The place that contains it, or the earlier one. |
| **Part Type** | Editable. *Administrative part of*, *geographic part of*, *ecclesiastical part of* (a parish, a diocese, an archdiocese), *judicial part of*, *cadastral part of*; or the succession types *successor of* (it replaced the target other than by merger or division), *merged from* (it was created by merging the target; one record for each merged place), *split from* (it was created by dividing the target), *received territory from* (it received part or all of the territory of the target, which continued to exist). |
| **Validity Range**, **Confidence** | When it held, and how sure you are. |

All three of subject, target and type are required.

## Historic event

A broad event that shaped many lives (a war, an epidemic), not an event of one person. To say that someone took part in something, use an event with participations. Open it from the **Context Board** (*New... > Historic event*). **Tabs:** Properties, Context, Research, Sources, Notes, Audit. Nothing is required.

| Field | Meaning |
|---|---|
| **Type** | Editable. *war* (conscription, deaths, displacement, missing records), *epidemic* (a local outbreak), *pandemic* (an outbreak over several regions or countries), *famine*, *migration*, *legal reform*, *political change*, *territorial change*, *natural disaster*, *economic crisis*, *scientific discovery*, *religious reform*, *social movement*. |
| **Title**, **Date**, **Place**, **Confidence** | As for an event. |

## Cultural norm

A custom, law or practice of a time and place that helps to interpret records or to estimate missing data (an age of majority, a naming custom). Link it to what it explains with a context impact. Open it from the **Context Board** (*New... > Cultural norm*). **Tabs:** Properties, Context, Research, Sources, Notes, Audit. Nothing is required.

| Field | Meaning |
|---|---|
| **Title** | A short name, for example *Napoleonic Code age of majority*. |
| **Rule Type** | Editable; the broad category. The details and the parameters (ages, degrees) go in the notes and the citations. |
| **Place with Citation** | The place where the norm held, with its evidence. |
| **Validity Range** | The date from which the norm was in effect, and until which. |
| **Confidence** | How sure you are that the norm really held at that time and place. |

**Rule types:** age of majority, marriage minimum age, baptism age (useful to estimate a birth from a baptism), confirmation age, military service age, retirement age, naming convention, surname transmission, patronymic system, matronymic system, title usage, inheritance rule, succession rule, dowry practice, guardianship rule, adoption practice, marriage practice, marriage prohibited degree, widowhood rule, residence pattern, household structure, social classification, religious practice, burial practice, citizenship rule, legitimacy rule, age-difference convention, generational interval (the two last ones are used to estimate missing dates).

## Context impact

An interpretation of how a historic event or a cultural norm acted on a record. It is context, not proof that the target took part in it: say why in the explanation. Open it from the **Context Board** (*New... > Context impact*, or *Impact of the selected context*, which fills the context in), or from the **Context** tab of a record. **Tabs:** Properties, Sources, Audit.

| Field | Meaning |
|---|---|
| **Context** | The historic event or cultural norm that exerts the influence. |
| **Target** | What is affected: an individual, group, place, event, relationship, attribute, conclusion, participation, place relationship or identity hypothesis. Prefer the most specific record. |
| **Impact Type** | Editable. *explains* (gives context, without necessarily having caused it), *influences*, *constrains* (limits what was possible, for example a law that forbade a marriage), *motivates* (makes it more likely, for example a famine that pushed a family to emigrate), *causes*. |
| **Explanation** | How and why the context affects the target: the reasoning is what makes it useful. |
| **Confidence** | How sure you are that the context really had this effect. |

Context and target are required.
