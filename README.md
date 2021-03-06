# MassStorage
Store simple items en masse in virtual chests.  The items are stored in a database where they can linger indefinitely.  Storage space is purchased via GenericEvents economy.

![Mass Storage Menu](https://raw.githubusercontent.com/StarTux/MassStorage/master/MassStorageMenu.jpg)

## Description
This plugin provides a virtual chest interface to insert items which will then be stored in the database.  Retrieval can either happen with a search term or via the chest menu which is organized in various categories.  Items can be removed like with any vanilla chest, and the menu offers additional click gestures to retrieve stacks and whole inventories worth.

## Requirements
The plugin requires a few other plugins for economy and database as well as a properly configured **MySQL** database.
- [SQL](https://github.com/StarTux/SQL)
- [GenericEvents](https://github.com/StarTux/GenericEvents)

## Commands
Even with the focus on a custom chest menu, players may choose to use a set of simple commands to get additional info or search for items by name.
- **`/ms`** Open chest menu.
- **`/ms <item>`** Search items and offer in storage chest.
- **`/ms store`** Open storage chest.
- **`/ms help`** Get help.
- **`/ms info`** Get info about your mass storage.
- **`/ms dump`** Store your entire inventory.
- **`/ms auto`** Toggle automatic storage mode.
- **`/ms find <term>`** Search items and show info.

For admins, there is a special command to look at users' stored items or grant them more storage space, or view some debug information.
- **`/msadm debug`** Toggle debug mode.
- **`/msadm reload`** Reload configuration.
- **`/msadm info <player>`** Player info.
- **`/msadm grant <player> <amount>`** Grant player storage units.
- **`/msadm category <category>`** View category.

## Permissions
Permissions are kept simple.  There is one command for users and one for admins.  Lacking either will deny the use the relevant command or perform any pertaining actions.
- **`massstorage.ms`** Use /ms
- **`massstorage.admin`** Use /msadm

## Configuration
Most of what `MassStorage` has to offer can be configured in the main configuration file. Below is an excerpt with the long blacklist removed.

```yaml
CommandHelp: Put all your spam items in the Mass Storage system. It can hold most simple, stackable items.
PermitNonStackingItems: true
MaterialBlacklist:
- AIR
```

## Links
- [Source code](https://github.com/StarTux/MassStorage/) on Github