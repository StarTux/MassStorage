# MassStorage
Store simple items en masse in virtual chests.  The items are stored in a database where they can linger indefinitely.  Storage space is purchased via Vault economy.

![Mass Storage Menu](https://github.com/StarTux/MassStorage/MassStorageMenu.jpg)

## Description
This plugin provides a virtual chest interface to insert items which will then be stored in the database.  Retrieval can either happen with a search term or via the chest menu which is organized in various categories.  Items can be removed like with any vanilla chest, and the menu offers additional click gestures to retrieve stacks and whole inventories worth, assuming there is enough capacity.

Storage space is limited per player, with a configurable starting amount.  Additional space can be purchased by spending currency in the Vault economy, int amounts and for a price which is also configurable.

## Requirements
The plugin requires a few other plugins for economy and database as well as a properly configured **MySQL** database.
- [SQL](https://github.com/StarTux/SQL)
- [Custom](https://github.com/StarTux/Custom)
- [Vault](https://github.com/MilkBowl/Vault)

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
- **`/ms buy <amount>`** Buy additional storage space.

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
CommandHelp: Put all your spam items in the Mass Storage system. It can hold most simple, stackable items. Your storage space is limited, but you can buy more any time.
PermitNonStackingItems: true
DefaultCapacity: 1728
BuyCapacity:
  Amount: 1728
  DisplayName: Chest
  Price: 500
MaterialBlacklist:
- AIR
```

## Links
- [Source code](https://github.com/StarTux/MassStorage/) on Github