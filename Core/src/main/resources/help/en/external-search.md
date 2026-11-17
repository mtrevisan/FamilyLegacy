# Searching external sites

Family Legacy can open, in your browser, a search already filled in with the names and the data of a person, on the genealogical sites and archives you choose: FamilySearch, Ancestry, MyHeritage, Geneanet, WikiTree, Open Archives, Antenati, GEDBAS and the metasearch of CompGen, FranceArchives, PARES, Find a Grave, Europeana, the US National Archives, Chronicling America, Trove, the Internet Archive, the Digital Public Library of America, and any site you add yourself. FamilySearch shows its results only to a logged-in user (the account is free); Ancestry and MyHeritage show the list of results, and ask for an account or a subscription for the details. Nothing is read back from the sites: you look at the results in the browser, where you may already be logged in. Each site takes only some of the values: Antenati and Geneanet the years of birth, Open Archives and the libraries only the name, WikiTree only the surname (it opens the index of the surname: its search page cannot be filled in from outside), Find a Grave the years with three years of margin. A site whose search is only a form (WieWasWie, for example) cannot be filled in from outside, and is not in the list: add it as a site of yours with its plain address if you want it at hand.

## Opening a search

Select an individual, then choose **Individual > Search External Sites...**, **Research > Search External Sites...**, or the same command in the menu of the right click on a box of the tree or of the ego network. Like the other research commands, they are shown from the *Standard* level (see [Interface level](preferences.md#interface-level)); the [Research Assistant](assistant.md) reaches them at every level. The window has three parts.

- **Names to search.** Every name of the individual is a row: the birth name, a married name, a religious name, an alias, and every transcription of a name (a Latin or a romanized form, written as a variant of its parts). The checked ones are searched: one search for each name on each site; when two names give the same address on a site (a site that takes only the surname, and two names with the same surname), it is opened once. The given names and the surname can be changed for this search only, and **Add a Name** adds a spelling the file does not have (a variant you met in a register). Changing a name here never changes the file.
- **Values of the search.** The other data the sites can use: sex, the years and the places of birth and death, the countries, the names of the parents and of a spouse. They are taken from the file and can be changed before searching; an empty value is simply left out of the search.
- **Sites.** The enabled sites, the most likely first: a site whose countries and years include the person is *Likely*, one that covers the whole world, or where the country or the years are not known, is *Possible*, one of another country or of other years is *Unlikely* (shown in grey, never hidden: a person may have emigrated). The **Holds** column says what the site has: records (indexes and images of registers), family trees (compiled by other people: a lead to check, not evidence), archive catalogues, newspapers, books, graves. A site that shows its results only to a logged-in user (FamilySearch) says *Ready (log in on the site)*: the first time, the site asks you to log in, and the browser then keeps you logged in. Double-click a site to open its search at once, or check several and press **Open in the Browser**. **Copy the Addresses** puts the addresses in the clipboard instead.

The sites you checked are remembered for the next search; the first time, the likely sites are checked. Opening more than 8 searches at once is confirmed first, because each one is a tab of the browser.

### Where the values come from

- The **birth** is the birth event of which the individual is the principal; without one, the baptism or the christening. The **death** is the death event; without one, the burial.
- The **years** are Gregorian. The *from* and *to* years are the window a site should search in: two years before and after an exact date (records often disagree by a year or two), five around an "about" date, the whole interval of a range or of a decade, ten years on the open side of "before" or "after".
- The **place** is the town: when the event happened in a building, a street, a parish or a cemetery, the place that contains it is taken. The *whole* place has all its containers. The **country** is found from the names of the place and of the places that contain it, in the main European languages and with some historical names (Prussia, Bohemia, Regno d'Italia); when it cannot be told, the sites are not ordered by country.
- The **parents** and the **spouse** are taken by their first name. A relative who may be living, or whose privacy is restricted, is left out.

## Privacy

A search sends the names and the values to the site. When the individual may be living, or the file restricts its privacy, the window says so, and nothing is opened until you check **Search anyway**. The names of living or restricted relatives are never filled in.

## Recording the search

Check **Record the search as a research activity** before opening: after the searches are opened, a new research activity is shown with type *search*, status *in progress*, the individual as target, and the sites, the names and the addresses in the action. Add what you found (the result, the observations, the sources) and save it, or close it to discard it: nothing is saved without you. A search with no result is worth recording too: it is negative evidence, and it saves you from repeating it. See [Research](research.md).

## Managing the sites

**Research > External Sites and Credentials...** (or **Sites and Credentials...** in the search window) lists every site.

- **Offered** says whether the site is shown in the search.
- **Add...** creates a site of yours, **Copy...** starts one from another site, **Edit...** changes the selected one. A built-in site can be changed (for example to use the address of the Italian version of a site), and **Restore Default** brings it back as it comes with the application. A site of yours can be removed.
- **Credentials...** keeps your keys and accounts for the selected site (see below).
- In the editor of a site, **The site shows its results only to a user logged in on it** marks the sites that need an account: the search window says so.

### The address of a search

A site is defined by the address of its results page, with the values of the person between braces:

`https://example.org/search?first={name_given}&last={surname}&born={birth_year}`

The values are: `{name_given}`, `{surname}`, `{name_full}`, `{sex}` (male or female), `{birth_year}`, `{birth_year_from}`, `{birth_year_to}`, `{birth_place}`, `{birth_place_full}`, `{birth_country}` (the ISO code, such as IT), the same six for the death (`{death_year}`, ..., `{death_country}`), `{father_given}`, `{father_surname}`, `{mother_given}`, `{mother_surname}`, `{spouse_given}`, `{spouse_surname}`. The **Insert** button writes the chosen one where the cursor is. Every value is encoded for an address (a space becomes `%20`); a value that is not known is empty. A brace that is part of the address itself is written twice: `{{` and `}}`.

To find the address of a site, search a name on the site by hand and copy the address of the results page; then replace the name with `{name_given}` and `{surname}`, and so on. The editor says at once whether the address is valid, and shows the search of an example person.

The **countries** (ISO codes such as `IT, SM, VA`, empty for the whole world) and the **years** of a site only order the list of the sites.

### Credentials

Every site has its own way of letting you in, and none of them needs Family Legacy for a search in the browser:

- **An account on the site** (FamilySearch, Ancestry, MyHeritage, Geneanet): log in on the site, in your browser; the browser remembers it, and the searches opened from Family Legacy find you logged in. Do not keep your passwords here: Family Legacy never sends them.
- **A key of a programming interface (API)**: the free key of Europeana, of the US National Archives, of Trove or of the Digital Public Library of America, the application identifier of WikiTree. Each site wants it in its own way (in the address, in a header of the request); they are needed only by a search made by Family Legacy itself, not by the browser.
- **An application authorized by the site** (FamilySearch): the site gives a key to the application after a review, and the user then signs in on the page of FamilySearch, so that the application never sees the password.

Some sites ask for a key or an account, mostly for their programming interfaces (an API key of Europeana, of the US National Archives, of Trove or of the Digital Public Library of America, the application key of FamilySearch, the application identifier of WikiTree). **Credentials...** keeps them, one per name: the built-in sites already list the ones they have; **Add** keeps any other one (for example a user name). An address can use a credential as `{credential:api_key}`, only for a site that wants it in the address; a search whose address needs a credential you have not given says so and is not opened. Remember that an address opened in the browser stays in its history, credential included: the built-in sites never put one there.

The credentials are kept in a file of their own in the folder of your preferences (`.familylegacy` in your home folder), readable only by your account and by the administrators of the computer. They are never written in a family file, in the preferences or in the log, and they are hidden in the address kept in a research activity. They are not encrypted: whoever can read the files of your account can read them. An empty value removes a credential.

The sites you added or changed are kept in the same folder (`external-sites.properties`), not in the family file, because they are a choice of yours and not genealogical data.
