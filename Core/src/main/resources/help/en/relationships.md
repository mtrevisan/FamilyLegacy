# Relationships

Family links are not stored inside the people. Each link is a **relationship record** that joins two entities, the **subject** and the **target**, with a **type**. The tree, the views and the reports are built by reading these records.

## Subject and target

The subject is the entity whose role the type describes, and the target is the other one. For the parentage types, **the subject is the child and the target is the parent**: *biological child* with Alice as subject and John as target means that Alice is the biological child of John.

## The types of relationship

| Type | Joins | Meaning |
|---|---|---|
| **child** | individual to individual | Child of the target, nature of the parentage not stated. Replace it with a specific type when you know it. |
| **biological child** | individual to individual | Child through birth. |
| **adoptive child** | individual to individual | Legal adoption. |
| **foster child** | individual to individual | Temporary foster care. |
| **guarded child** | individual to individual | Court-appointed guardianship (the child is the subject, the guardian the target). |
| **step child** | individual to individual | Child through a marriage, not through descent. |
| **civil spouse** | individual to individual | Married in a civil ceremony. |
| **religious spouse** | individual to individual | Married in a religious ceremony. |
| **customary spouse** | individual to individual | Married according to custom. |
| **cohabiting partner** | individual to individual | Partners who live together. |
| **engaged partner** | individual to individual | Engaged to be married. |
| **associate** | any to any | Any other relationship; give it a role (a godparent, a servant, a business partner...). |
| **group member** | individual (subject) to group (target) | Membership of a group. |
| **part of** | group (subject) to group (target) | The subject group is part of the larger target group. |

The generic types **spouse** and **child** are for the ties whose nature the evidence does not state (an import writes them to every marriage or child that says nothing more). The editor offers both, so such a relationship can be saved again as it is; replace them with a specific type when you know it. The [Research Assistant](assistant.md) says when the evidence suggests one.

## The relationship editor

The editor opens when you add a relationship from a person or a group (see below). Its *Properties* tab has:

- **Subject** (required) and **Target** (required): the two entities. The target can only be a group when the subject is a group.
- **Subject Type** (required): the type. It is filled in only after the subject and the target are chosen, and it offers the types that fit that pair: for two individuals the child, spouse-like and associate types; for an individual and a group, *group member* and *associate*; for a group, *part of* and *associate*.
- **Subject Role**: free text. It is normally needed only for *associate* and *group member*. Examples for a group member: member, president, secretary, treasurer, resident, head of household, elder, custodian.
- **Valid From** and **Valid To** (the *Validity Range*): when the relationship held. For partners, the earliest *Valid From* is used to order the partners, see [Navigating the tree](navigating.md#order-of-children-and-partners).
- **Confidence**: low, medium or high.

The other tabs are *Context*, *Research*, *Sources*, *Notes*, *Privacy* and *Audit*.

### What the Research Assistant does with the relationships
The Relationships findings are suggestions: nothing is created or changed until you save the editor that the action opens.
- **Change type**: when the evidence points to another type (an adoption that both took part in, a parent married to the other after the child was born, the banns or the license of a marriage, a marriage in a parish or before an officiant who is of the clergy, which only hint at a religious marriage), the action opens the relationship with the suggested type already chosen. It is chosen only if the editor offers it for the two, and cancelling leaves the relationship as it was.
- **Create Relationship Draft**: for a marriage event with no relationship, the new relationship starts with the **date of the marriage** as *Valid from* and the **sources of the event**; for the partner of a parent who has no relationship with the child, and for the informant of a birth who has none with the child, it starts with the two individuals and no type (the partner may be a parent or a step-parent, the informant a parent, a relative or a neighbour: the choice is yours), the informant's draft with the sources of the birth.
- **Siblings** have no type: they are written as an *associate* relationship with a role such as *brother* or *half-brother*. The assistant checks that role against the biological parents (full siblings have both in common, half-siblings one) when both individuals have parents. A role that says a parent, a child or a spouse, for which there are types, and an association between two individuals already linked by a family relationship, are reported as information.

On OK the editor refuses a relationship with no subject, no target or no type, and one whose subject and target are the same entity. Everything else is not blocked: contradictions are reported by the [Research Assistant](assistant.md) as findings.

## How to record the usual cases

### The parents of an individual

1. Select the child and open **Individual > Edit Individual...** (or `F2`).
2. In the **Relationships** tab, add a relationship. The subject is already the child.
3. Choose the parent as **Target** and *biological child* (or another child type) as **Subject Type**, then save.
4. Repeat for the second parent.

In the tree there is a shortcut: right-click an **empty parent box** and choose **Connect Individual...**. You pick the person (an existing one, or you can create it), and for each child of that couple you confirm the type (*biological child* is the default). The empty box decides the sex assumed for the parent: left is male, right is female. Be careful: this command **replaces** the children's existing parent links of the same sex, if the new parent has a sex recorded.

**Add Individual...** on an empty box only creates a person and makes it the root: it does not link it to anybody.

### A marriage or a partnership

1. Edit one of the two people, **Relationships** tab, add a relationship.
2. Choose the other person as **Target**, and *civil spouse*, *religious spouse*, *customary spouse*, *cohabiting partner* or *engaged partner* as **Subject Type**.
3. Optionally set **Valid From** and **Valid To**, the confidence, sources and notes.

For partners the order of subject and target does not matter: the tree reads both directions. The marriage as an *event* (with its date and place) is a separate record, see [Events and dates](events-and-dates.md); the Research Assistant reports a marriage event that has no matching relationship.

### An adoption, a foster care or a guardianship

As for the parents, but with the type *adoptive child*, *foster child*, *guarded child* or *step child*. The subject is the child and the target is the adopter, the foster parent, the guardian or the step parent. Do not use *biological child* together with an adoption event for the same pair: the Research Assistant flags it.

### A member of a group, with a role

1. Choose **Group > Add Members...**. The group's editor opens on the **Relationships** tab.
2. In the **Group Memberships** panel add a relationship: the group is already the target, and you choose the member as subject.
3. Set **Subject Type** to *group member* and write the **Subject Role**.
4. Optionally set the validity range and the confidence, then save.

The same can be done from the editor of the individual: the subject is already that person, and you choose the group as target. A group inside another group uses *part of* (the smaller group is the subject) or *associate*.

## How the views read the relationships

- **Parents and children** come from the child types. The family views (the ones the application opens with) follow all six: *child*, *biological child*, *adoptive child*, *foster child*, *guarded child* and *step child*. Only links between two individuals count.
- **Father and mother** are told apart by the **sex** of the parent: a parent with no sex recorded (or whose sex assertions compete with nothing to settle them) may land in the wrong place. If the sexes do not decide, the first parent found is used.
- **Partners** come from the spouse-like and partner types, in either direction. *Associate* is not a partner. Two people who are both parents of the same child are also drawn as a couple, even without a partner relationship.
- A couple is drawn as **biological** only if both parents are biological parents. If any link is adoptive, foster, guarded or step, the couple is drawn with a dashed line.
- A person hidden by the current **privacy** setting is ignored together with all their links.
- **Children without a second parent** appear under a single-parent entry.
- The order of children and partners is a view preference, see [Navigating the tree](navigating.md#order-of-children-and-partners).

## Moving and removing links

- **Relocate** (`Ctrl+X`) only copies the selected individual or group to a clipboard. It changes nothing by itself.
- **Paste** is in the context menu of an **empty box** of the tree (the entry reads *Paste ... Here*). It **removes every link between the pasted person and other individuals** (parents, children, partners, associates; links to groups are not touched), without asking, and then creates the new links, asking for the type as **Connect Individual...** does. Moving a person is therefore: *detach from everyone, then reattach in the new place*. In the ego network, paste replaces all the relationships between the ego and the pasted record.
- **Unlink Relationships...** opens a dialog that lists what is linked to the selected individual, grouped as *Parents*, *Partners*, *Groups*, *Associates* and *Children* (for a group: *Members*, *Subgroups*, *Supergroups*), each marked as *Direct*, *Inverse* or *Bidirectional*. Tick what you want to remove and confirm: the relationship records are deleted, the people stay. The dialog may list other records that refer to the entity, with their own tabs; read them before confirming.
- **Delete** removes an individual together with every relationship that involves it, its event participations and its attributes.

Undo (`Ctrl+Z`) reverts these changes.

## Relationships between places

Places have their own relationships, edited the same way (subject, target, type, validity range, confidence). The subject is the place whose role is described, and the target is the place that contains it, or the earlier one.

| Kind | Types |
|---|---|
| **Part of** | administrative, geographic, ecclesiastical, judicial, cadastral |
| **Succession** | successor of, merged from, split from, received territory from |

For example, *administrative part of* with Treviso as subject and Veneto as target. For the succession types the subject is always the later place. The type is an editable list, so a type of your own can be typed. Subject, target and type are required, and both places must be places.
