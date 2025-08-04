# KingdomsX

[![Spigot Version](https://img.shields.io/badge/Spigot-1.21-dark_green.svg)](https://shields.io/)
[![Crowdin](https://badges.crowdin.net/kingdomsx/localized.svg)](https://crowdin.com/project/kingdomsx)
[![CodeFactor](https://www.codefactor.io/repository/github/cryptomorin/kingdomsx/badge/master)](https://www.codefactor.io/repository/github/cryptomorin/kingdomsx/overview/master)
[![Discord](https://discordapp.com/api/guilds/429132410748141579/widget.png?style=shield)](https://discord.gg/cKsSwtt)
<!-- Another unofficial Discord badge style: https://img.shields.io/discord/429132410748141579?logo=discord -->

<img src="https://i.imgur.com/Mz7cbAV.png" alt="The official banner of KingdomsX plugin">

Battles for might, land and glory.

KingdomsX is plugin similar to Factions which provides more advanced core features and introduces new mechanics such as
turrets, structures and invasions to make the game more fun.
Kingdomsx is heavily optimized and easy to use.

### Links:

- [SpigotMC](https://www.spigotmc.org/resources/77670/): The first original page of KingdomsX plugin.
- [Modrinth](https://modrinth.com/plugin/kingdomsx/): The official modrinth page of KingdomsX plugin. It's easier to
  keep track of plugin versions for people who still wish to use outdated server patches here.
- [Discord](https://discord.gg/cKsSwtt): Official Kingdoms Discord server. Ask questions, suggest features and report
  bugs.
- [Polymart](https://polymart.org/product/492/kingdomsx): Official Kingdoms Polymart page. (Currently really inactive
  since kingdoms publications are automated but Polymart's API is broken right now and is being worked on.)
- [Wiki](https://github.com/CryptoMorin/KingdomsX/wiki): Learn how Kingdoms works.

### Contributing

Currently, the plugin is not open source. See [this FAQ](https://github.com/CryptoMorin/KingdomsX/wiki/FAQ#source-code)
for more information.
However, the configs, GUIs, language files and addons are open source and any contribution is welcome.

* For contributing to translations and GUIs see [here](https://github.com/CryptoMorin/KingdomsX/wiki/Languages).
* For contributing to [addons](https://github.com/CryptoMorin/KingdomsX/wiki/Addons) continue reading below.

#### Contributing to add-ons

Kingdoms relies on a complex network of systems built with Gradle. This includes everything from building and publishing
the project to various platforms to managing data sharing between add-ons and configuring how add-ons are downloaded.
However, since the core plugin is not open source, contributing to add-ons can be challenging because the build files
reference settings and plugins that are not publicly available.

This guide explains how to configure the `build.gradle.kts` file for add-ons, enabling you to build the project
without issues and submit pull requests effectively.

1. Familiarize yourself with both [Gradle](https://gradle.org/) and [Kotlin](https://kotlinlang.org/)
   since the project doesn't only use Kotlin for Gradle,
   but it also uses Kotlin extensively in the add-on code.
2. Download [Python (at least v3.6.0 or above)](https://www.python.org/downloads/)
3. Download and place
   [KingdomsX Source Tool.py](https://github.com/CryptoMorin/KingdomsX/raw/refs/heads/master/KingdomsX%20Source%20Tool.py)
   in a separate folder, this will be the add-ons root folder.
   (Download it using **Ctrl+S** when on the page) which handles transforming the build files into usable local
   projects.
   You need internet access during the duration of the source tool's generation process to download the necessary files.
4. Open the folder your favourite IDE (IntelliJ is preferred)

> [!WARNING]
> There are very rare cases where the Kotlin compiled code can have broken metadata
> which causes named parameters, default method members or static methods to stop
> working.
>
> Also, addons that require other add-ons like **Outposts** which requires **EngineHub** need to
> have their projects built separatedly first.
>
> To submit pull requests, you may need to add additional rules to your `.gitignore` since the other
> add-ons are most likely not preset in your root folder.
