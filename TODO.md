> [!NOTE] 
> * All entries that don't specify any add-ons name belong to the core project.
> * Make sure to also check the Discord server for suggestions bugs.

# Projects
- **[DiscordSRV Addon]** Read the [Discord Thread](https://discord.com/channels/429132410748141579/1082668620632563734) for more info.

# New
- Add a placeholder for total resource points used by a kingdom/nation (We can't track the total earned rp, because the data will be tampered when kingdoms transfer resouce points between each other by donations or transferring to their nation and vice versa)
- Add the tip system in CommandAdminTip and KingdomsTipSystem. Is it going to be useful?
- Add variable assignments to math equations.
- Add an option to allow servers that disabled nexus to upgrade their kingdom level. (They might still allow `/k nexus open`)
- onResourcePointsPenalty custom cause rp loss prevention.
- Add a way to not push an option and take up a slot and leave it empty for disabled paginated GUI options. (e.g.
  permissions)
- Make a GUI for log GUI filter by type
- ~~Add an option to target the cloesest mob to the turret which causes more lag.~~
- Make death-messages translatable for turrets.
- Add build duration speed up effect to all buildings. Right now building effects are only for turrets, but we can make
  some of them permanent by having them saving in the data.
- Make structure/turrets holograms support relational placeholders. (Right now it only gets a message context that
  doesnt include the player who's viewing the hologram)
- Make the GUI config support math equations for "rows"
- Option to disallow `/k tpa` to be instant for members of the same kingdom. Instead, only allow `/k tpa` to be sent to
  your own kingdom members or people you have `INSTANT_TELEPORT` permission with? There's an issue of people instantly
  teleporting to someone while they're pvping and combatlogged, making it unfair.
- Option to expand plunder invasion area without invading multiple lands
- Add `{$fn arg1="..." arg2="..."}` functional macros.
- Remove AnnotationContainer it's useless, use AnnotatedElement or whatever its called
- Add full command annotation support for passing parameters using ASM. Also do the same thing with placeholders.
- Make extractors GUI automatically update based on a condition when fuels are disabled.
- Add negation for StringMatcher.
- Using color codes like &7 should not reset the whole formatting state and &r should only do that?
  E.g. `&2&lTest &7Test2`, "Test2" should be still bold but in different color?
- Make placeholder parser that assumes `kingdoms_` identifier to also work if the identifier is explicitly declared.
- Prevent outpost blocks from accessing the nexus if nexus isn't placed.
- Add a way to customize `/cancel` command used for inputs with a macro.
- Check if horses can be mounted in protected lands.
- Move `fill-cost` from turret's GUI settings (`guis/<language>/templates/turretgui.yml`) to the root `Turrets` folder configs.
- Introduce embedded GUI messages on the option's lore itself instead of sending raw messages to the player. (OptionHandler#error)
- Add schematics for claiming. Simply storing xz relative coordinates in a file that can be used to claim a certain shape. Should be saveable by targetting a whole kingdom too.
- Make Protection Signs and Chat into their own separate addons. (Needs data conversion)
- Make the JAR verifier work for Spigot.
- Make name/tag/lore regex case-insensitive by default. We should make a class similar to StringMatcher that supports multiple conditions for name restrictions in general.
- Make the `org.kingdoms.tasks.TaskRegistry` class support execution with an ignored list of tasks (useful for /k invade checks)
- **[EngineHub]** Add support for WorldEdit v6 building pasting mechanism.
- **[Map Viewers]** Add icon overlays for repairing/upgrading/destroyed turrets/structures.
- **[Map Viewers]** Break relationships into multiple lines when there are too many ([Discord Suggestion Post](https://discord.com/channels/429132410748141579/1375544493805146233))
- **[Admin Tools]** Add a command to remove all existing types of a certain turrets/structures placed in the world.
- **[Admin Tools]** Add `/k admin resetNation` similar to `/k admin resetKingdom` 

# Bugs
- **[Map Viewers]** Fix the negative space bug. Image included in map-viewers folder.
- **[EngineHub]** Make building demolition for schematics support blocks like ladders (if the support block is broken the ladder will drop due to block physics update)
- **[EngineHub]** Find a way to support FAWE. More information can be found [here](https://github.com/CryptoMorin/KingdomsX/wiki/EngineHub-Addon#worldedit-schematic-building-support).
- **[EngineHub]** Currently the [NegativeSpaceOutliner](https://github.com/CryptoMorin/KingdomsX/blob/c4b0475b0dc10c573690d230ab9899691d02062b/map-viewers/commons/src/main/java/org/kingdoms/services/maps/abstraction/outliner/NegativeSpaceOutliner.java)
  will break in more complex land layouts. The bug can be seen in [this image](https://github.com/CryptoMorin/KingdomsX/blob/c4b0475b0dc10c573690d230ab9899691d02062b/map-viewers/Negative%20space%20bug%20with%20specific%20L%20shape%20layout.png) that contains a minimal reproduction claim layout
  (Happens to all map plugins).

# Others
- **[EngineHub]** Rebuild all structure schematics using MC_EDIT format of v1.9 WorldEdit. All turrets except the arrow and flame already use this format.
- **[All]** Edit commands to use the new annotation API to be more concise.