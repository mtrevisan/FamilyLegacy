# Kinship and consanguinity

Two tools answer the question *how are these two people related?*, and a third finds the couples whose partners are related.

## Kinship Calculator

**Individual > Kinship Calculator...** compares two individuals. It needs at least two individuals in the file.

**The dialog**

- **Individual A** is filled with the selected individual; **Individual B** is empty. Right-click a field to **Set...**, **Edit...** or **Clear** it.
- **Swap** exchanges the two (enabled when both are set).
- There is no Calculate button: the result updates whenever you change a field. If one field is empty the result is cleared.
- The **Result** area is read-only text.

**What it follows.** The relationship is computed only through **shared ancestors**: the parents of each person, and their parents, and so on. Partners and spouses are not followed. The parent links used are the ones the tree uses, so they include adoptive, foster, guarded and step links when the tree does. People hidden by the current [privacy](privacy.md) policy are left out.

**The result**

1. The names of A and B and the **Relationship**, as a sentence (see below). If they are the same person, it says so.
2. If there is no common ancestor in the data: *No common ancestor found in the current data.*
3. The **chain** from each person up to the most recent common ancestor.
4. The **most recent common ancestor** and the furthest one, each with its distance in steps from A and from B. The most recent is the one with the smallest total distance.
5. **Statistics**: the total length of the path through the most recent common ancestor, the number of common ancestors, and **Wright's coefficient of relationship** (R). It is computed exactly over the whole pedigree, taking into account the inbreeding of the ancestors; an unknown parent is treated as unrelated.
6. **Kinship degrees**, according to several traditions, each with the limit that applies to it:

| Tradition | How the degree is counted |
|---|---|
| **Civil / Roman** (Italy, Germany, Japan) | In a direct line, the number of generations; in a collateral line, the steps up to the common ancestor and down again. The 6th degree is marked as the limit. |
| **Canonical** (Catholic Church) | In a direct line, the number of generations; in a collateral line, the number of steps of the **shorter** of the two sides. The 4th degree is the limit. |
| **Germanic knee number** | The same value as the canonical degree. |
| **Korean chon** | The same value as the civil degree. |
| **Chinese generation** (PRC Marriage Law) | One more than the longer side. For collateral relatives, three generations is the limit. |

7. **Marriage prohibitions** under the Italian civil law (prohibited in a direct line, and in a collateral line within the 4th degree), the Catholic canon law (prohibited in a direct line; a dispensation is required within the 4th canonical degree if the sexes are opposite; not applicable to the same sex) and the PRC Marriage Law (prohibited in a direct line, and in a collateral line within three generations). They are a reading aid, not legal advice.
8. **All common ancestors**, each with its distances and its share in the coefficient.

**How the relationship is worded.** Only the most recent common ancestor decides the wording: father, mother, grandfather, grandmother, great-grandfather (and so on) in a direct line; full or half brothers, sisters or siblings; uncle or aunt and nephew or niece; grand-uncle; and for the rest first, second, third cousins, with "once removed", "twice removed" for the difference of generations. When there are several common ancestors (double cousins, for example), the others appear in the list and in the coefficient.

**Examples**

- Select Anna, open the calculator and set B to her uncle Carlo: the result says Carlo is the uncle of Anna, with the shared grandfather at 2 steps from Anna and 1 from Carlo; the civil degree is 3 and the canonical degree is 1; under Italian civil law marriage is prohibited (collateral line within the 4th degree), and canon law requires a dispensation if the sexes are opposite.
- Two people whose parents are siblings are first cousins: the common ancestors are at 2 and 2 steps, the civil degree is 4, the canonical degree 2, and R is 0.125 if there is no other inbreeding.
- Unrelated people: *No common ancestor found in the current data.*

## Consanguinity Report

**Tools > Consanguinity Report...** lists the couples whose partners are related and the individuals who are the child of such couples. It needs at least two individuals and only reads the data. The header says how many couples and how many inbred individuals there are.

The report is computed only on the **biological** ancestry (*child* and *biological child* links; adoptive, foster, guarded and step links are left out). An unknown ancestor counts as unrelated, so the values are **lower bounds**, and a wrong link changes them. The report says so itself: it is a list of possibilities to look into, not a conclusion. Hidden people are excluded.

**Which couples.** Pairs joined by a spouse or partner relationship (civil, religious or customary spouse, cohabiting partner; not *engaged partner*), and also any two people who are the two biological parents of the same child, even if no union is recorded. A couple is reported when the kinship of the partners is at least 1/256 (about third cousins).

For each couple the report gives:

- **Related as**: the kinship, in words (siblings, first cousins, second cousins once removed, parent and child...).
- **Coefficient of relationship R**, and the inbreeding **F** that a child of theirs would have.
- The **closest common ancestors**, with the number of generations.
- **Recorded as**: the union, or *parents of the same child, no union recorded*.
- The biological **children together**.

**Inbred individuals**: every person with two known biological parents is checked, and is reported when their coefficient of inbreeding is at least 1/64 (1.56%, the child of second cousins). Each entry gives F, how the parents are related and their closest common ancestors.

Both lists are sorted from the strongest kinship down. The thresholds are fixed. Note that the calculator uses the links the tree uses, which can include adoptive links, while this report uses only biological ones, so the two can differ.
