# Emote Downloader

Ever wanted to download all of your cute Discord emotes? I did too, so I made this nifty tool.
All you need is Java installed and your config file, then just double click the jar.

**NOTE:** This program does not interact with any REST API endpoints, there is no risk for using it.

You can find the latest pre-built JAR in the [releases](https://github.com/averen/emote-downloader/releases) tab.

### Making your config file
The config file is expected to be named `config.json` and exist in the same directory as the jar or working directory. Here's a template:

```
{
  "token": "Your token here", // The token is your CLIENT discord token. To find it, visit the link below.
  "mode": "ordered" // If the mode is set, the emotes will be organized by the guild they belong to. You'll probably want this.
}
```

Need help getting your token? [This should do the trick](https://camo.githubusercontent.com/d3d4ad5526143204a98db268d79eadadf0d03a87/687474703a2f2f692e696d6775722e636f6d2f5569416d4f714d2e706e67)!

If you need further help, contact me on Discord @ meister#7070
