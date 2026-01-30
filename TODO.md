> [!NOTE] 
> * All entries that don't specify any add-ons name belong to the core project.
> * Make sure to also check the Discord server for suggestions bugs.

# Projects
- **[DiscordSRV Addon]** Read the [Discord Thread](https://discord.com/channels/429132410748141579/1082668620632563734) for more info.

# New
- Add a placeholder for total resource points used by a kingdom/nation (We can't track the total earned rp, because the data will be tampered when kingdoms transfer resouce points between each other by donations or transferring to their nation and vice versa) Would require a general resource point spend event with a custom namespace.
- Add variable assignments to math equations. With the format `let <variable-name> = <math-expression>;` The `MathCompiler` already has some classes ready for this called `Assignment` and `Block`
- Add an option to allow servers that disabled nexus to upgrade their kingdom level. (They might still allow `/k nexus open`) Perhaps `/k upgrade kingdom`?
- Add a way to not push an option and take up a slot and leave it empty for disabled paginated GUI options. (e.g. permissions)
- Make a GUI for log GUI filter by type
- Make GUI configs support math equations for `rows`
- Move shield prices to `invasions.yml` from the GUI configs.
- Make extractors GUI automatically update based on a condition when fuels are disabled.
- Make `death-messages` translatable for turrets (which is currently separate in each style's config)
- Add build duration speed up effect to all buildings. Right now building effects are only for turrets, but we can make
  some of them permanent by having them saving in the data.
- Make structure/turrets holograms support relational placeholders. (Right now it only gets a message context that
  doesn't include the player who's viewing the hologram)
- Option to disallow `/k tpa` to be instant for members of the same kingdom. Instead, only allow `/k tpa` to be sent to
  your own kingdom members or people you have `INSTANT_TELEPORT` permission with? There's an issue of people instantly
  teleporting to someone while they're pvping and combatlogged, making it unfair.
- Option to expand plunder invasion area without invading multiple lands
- Add `{$fn arg1="..." arg2="..."}` functional macros.
- Remove `AnnotationContainer` it's useless, use Java's built-in `AnnotatedElement`.
- Add full command annotation support for passing parameters using ASM. Also do the same thing for functional placeholders.
- Add negation for StringMatcher.
- Using color codes like &7 should not reset the whole formatting state and &r should only do that?
  E.g. `&2&lTest &7Test2`, "Test2" should be still bold but in different color?
- Make placeholder parser that assumes `kingdoms_` identifier (e.g. inside functional placeholders) to also work if the identifier is explicitly declared.
- Prevent outpost blocks from accessing the nexus if nexus isn't placed.
- Add a way to customize `/cancel` command used for inputs with a macro.
- Check if horses can be mounted in protected lands.
- Move `fill-cost` from turret's GUI settings (`guis/<language>/templates/turretgui.yml`) to the root `Turrets` folder configs.
- Introduce embedded GUI messages on the option's lore itself instead of sending raw messages to the player. (OptionHandler#error)
- Make Protection Signs and Chat into their own separate addons. (Needs data conversion)
- Add a `VIEW_ONLY` privilege for protection signs where the player cannot take/put any items.
- Add a `PUT_ONLY` privilege for protection signs where the player can only put items but not take out any.
- Make the JAR verifier work for Spigot.
- Make name/tag/lore regex case-insensitive by default. We should make a class similar to StringMatcher that supports multiple conditions for name restrictions in general.
- Make the `org.kingdoms.tasks.TaskRegistry` class support execution with an ignored list of tasks (useful for /k invade checks)
- Ability to use `fancy@` modifier inside the `type` parameter of `%kingdoms_nation_top%` and `%kingdoms_kingdom_top%` when the kingdom/nation at that position doesn't exist
- **[EngineHub]** Add support for WorldEdit v6 building pasting mechanism.
- **[Map Viewers]** Add icon overlays for repairing/upgrading/destroyed turrets/structures.
- **[Map Viewers]** Break relationships into multiple lines when there are too many ([Discord Suggestion Post](https://discord.com/channels/429132410748141579/1375544493805146233))
- **[Admin Tools]** Add a command to remove all existing types of a certain turrets/structures placed in the world.
- **[Admin Tools]** Add `/k admin resetNation` similar to `/k admin resetKingdom` 

- ~~Switch from **BCrypt** to **SCrypt**. Challenges include converting protection-sign passwords and defined CPU/RAM cost of SCrypt.~~ Not viable and unnedessary
- ~~Add an option to target the cloesest mob to the turret which causes more lag.~~ Not viable
- ~~Add schematics for claiming. Simply storing xz relative coordinates in a file that can be used to claim a certain shape. Should be saveable by targetting a whole kingdom too.~~ Added to admin tools addon

# Bugs
- There is a chance that a race condition might happen between Visualizer's start() and onPlayerAdd() which is called by StatefulVisiblityStrategy.
- Some commands like `/k admin backup restore` require `[Yes]` confirmation, but that only works in-game. Perhaps add a conditional message to show the command to execute rather than the text.
- The language files which are automatically generated (i.e. `en.yml`) seem to be inconsistent with some strings. For example if the text starts with `hover:{...` it'll serialize it in a weird way (maybe because it thinks that it's a YAML key?)
- **[EngineHub]** Make building demolition for schematics support blocks like ladders (if the support block is broken the ladder will drop due to block physics update)
- **[EngineHub]** Find a way to support FAWE. More information can be found [here](https://github.com/CryptoMorin/KingdomsX/wiki/EngineHub-Addon#worldedit-schematic-building-support).

# Others
- Add a `MessageObject#compile(MessageContextProvider)` that compiles a MessageObject for more efficient usage. It should convert `MessagePiece.Variable/Macro` to a custom variable messagepiece that contains a Supplier without fetching that variable from the placeholders list again. Additionally it should cache its Plain/Complex message pieces that are constant (Perhaps do the same for Minecraft's `Component` in the future?)
- `ClaimProcessor`'s `finalizeProcess()` doesn't actually claim lands (and doesn't handle overclaiming) and each call is forced to manually call the `kingdom.claim()` method, but this is party because an event is called there. We should probably add an `intent` similar to the ones in processors for those events.
- **[EngineHub]** Rebuild all structure schematics using MC_EDIT format of v1.9 WorldEdit. All turrets except the arrow and flame already use this format.
- **[All]** Edit commands to use the new annotation API to be more concise.
