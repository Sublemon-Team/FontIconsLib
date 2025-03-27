[![STARS](https://img.shields.io/github/stars/Sublemon-Team/FontIconsLib?style=for-the-badge&label=%E2%AD%90%EF%B8%8FSTAR%20FICO)](https://github.com/VuzZis/Subvoyage)
[![Download](https://img.shields.io/github/v/release/Sublemon-Team/FontIconsLib?color=6aa84f&include_prereleases&label=Latest%20version&logo=github&logoColor=white&style=for-the-badge)](https://github.com/VuzZis/Subvoyage/releases)[![Total Downloads](https://img.shields.io/github/downloads/Sublemon-Team/FontIconsLib/total?color=7289da&label&logo=docusign&logoColor=white&style=for-the-badge)](https://github.com/VuzZis/Subvoyage/releases)
[![Discord](https://img.shields.io/discord/1252898892882903082?style=for-the-badge&logo=discord&label=SUBLEMON-TEAM)](https://discord.gg/Gsbajms8CK)
# Font Icons Lib (FICO-Lib)
This is a library, for all Mindustry modders, 
featuring a way to add custom font icons without any coding.

## How To Use
1. Create a new file in `assets/icons`, named `<modid>.icons` (replace `<modid>` with your actual id)
2. Write your icon data as in example config
3. Install this mod and it will automatically read your file and add following icons

## [Example](https://github.com/Sublemon-Team/FontIconsLib/tree/master/assets/icons/yourmodid.icons)
```properties
# Example config for this library
# You should name your file <modid>.icons, and put in assets/icons/

# Your icon texture must be in assets/sprites/ui

# The format for icon info is this:
# <type> <id> <texture> [name]

#type:  the type of icon, either "icon" or "team-icon"
#id:    the numeric id of icon, hex or decimal. "#ABCD" is the same as "43981"
#       - But, be careful! Your id must be unique, if you use the same id as another mod or vanilla, it will override it!
#texture: the texture of the icon, you can put "@-" and it will replace it with "modid-"
#name:  optional field, the name of the icon, used for reference, if not set, it will be the same as your texture

# Example code:

icon 60113 @-laser-icon
icon 60114 subvoyage-laser-icon
icon #EAD3 @-laser-icon laser-icon

# For team icons, the name will be the team name
team-icon 60116 @-team-melius melius

# To use those icons, you can use \u tag in your text, and add your hex id, like this:
# somebundletext = This is a template \uEAD3 icon!
```