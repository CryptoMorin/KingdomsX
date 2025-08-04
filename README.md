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

Kingdoms uses a wide network of Gradle built systems. Everything from building the project to publishing it to platforms
and sharing data between addons and how addons are downloaded from is configured by the Gradle build itself, but however
unfortunately because the plugin itself is not open source, contributing to addons will be challenging as the build
files themselves reference certain settings and plugins that are not open source.

This guide helps to configure the `build.gradle` of the specific add-on in a way so you can build the project
successfully
and send Pull Requests in a proper way.

1. First you need to familiarize yourself with both [Gradle](https://gradle.org/) and [Kotlin](https://kotlinlang.org/)
   since the project doesn't only use Kotlin for Gradle,
   but it also uses Kotlin heavily in the add-on code as well.
2. You need to clone that specific add-on you want using a 3rd party website such
   as [download-directory.github.io](https://download-directory.github.io/?url=https%3A%2F%2Fgithub.com%2FCryptoMorin%2FKingdomsX%2Ftree%2Fmaster%2Fenginehub)
   or [downgit.github.io](https://downgit.github.io/#/home?url=https:%2F%2Fgithub.com%2FCryptoMorin%2FKingdomsX%2Ftree%2Fmaster%2Fenginehub).
   Because GitHub doesn't provide a way to download a specific folder only.
3. Lastly you need to download [Python (at least v3.6.0 or above)](https://www.python.org/downloads/)
   and run
   [KingdomsX Source Tool.py](https://github.com/CryptoMorin/KingdomsX/raw/refs/heads/master/KingdomsX%20Source%20Tool.py)
   which handles transforming the build files into usable local projects.
   You need internet access during the duration of the source tool's generation process to download the necessary files.

> [!WARNING]
> There are very rare cases where the Kotlin compiled code can have broken metadata
> which causes named parameters, default method members or static methods to stop
> working.
> Also, addons that require other add-ons like **Outposts** which requires **EngineHub** need to
> have their projects built separatedly first.
