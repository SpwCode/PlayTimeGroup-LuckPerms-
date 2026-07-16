# [DOWNLOAD](https://modrinth.com/plugin/playtime-group-luckperms)

## Functionality

The plugin tracks players' playtime and automatically assigns them roles via the LuckPerms permissions system based on
accumulated time. Roles are configured in the configuration file.

### Key features:

- __Playtime tracking:__ The plugin stores players' login/logout times and summarizes the total time in the
  `playtime.yml` file.
- __Automatic role assignment:__ The playtime of online players is checked every 5 minutes and the corresponding roles
  are assigned.
- __Role check upon login:__ When a player logs in, their playtime is immediately checked and roles are assigned.
- __`/playtime` command:__ Allows players to view their playtime or (with the `playtimeroles.admin` permission) the
  playtime of other players.

### Commands:

- `/playtime` - Show your playtime
- `/playtime <player>` - Show the playtime of the specified player (requires `playtimeroles.admin`)

### Permissions:

- `playtimeroles.playtime` - Access to the `/playtime` command
- `playtimeroles.admin` - View other players' playtimes

## Configuration

### config.yml

This file contains role settings. Each role has a name and a required time in the format:

- `h` - hours
- `m` - minutes
- `s` - seconds

Example configuration:

```yaml
roles:
  - role: "novice"
    time: "1m"
  - role: "experienced"
    time: "10h"
  - role: "veteran"
    time: "50h"
```

## How the plugin works

1. __When enabled:__ Loads the configuration, initializes the 'playtime.yml' file to store playtime data, and registers
   events and the command.
2. __Player tracking:__ Stores the login time upon login, and calculates and stores the session time upon logout.
3. __Role Checking:__ Periodically (every 5 minutes) and upon login, it checks the total playtime of each player and
   assigns roles via the LuckPerms API.
4. __Data Saving:__ Playtime is saved in `playtime.yml` in milliseconds by player UUID.

The plugin requires LuckPerms to be installed to work with roles and works on Paper/Spigot servers with API 1.21.
