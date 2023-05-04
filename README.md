# IDE Helper plugin for JetBrains IDE

A simple plugin for JetBrains IDE that only displays some configuration file paths.  
Useful when working under JetBrains Client.

## Build and Run
Create a JDK 17 as SDK in the project structure.

Using IDEA(either Community or Ultimate version) to open the project, 
then click the `Build Plugin` run configuration to build the plugin,
click the `Run Plugin` run configuration.

Before submit to the JetBrains Marketplace, better run `Run Verifications` locally first.


## Usage
There will be an IDE Helper tool window once a project is opened.

## TODO

- Fix icons issue for new UI according to https://plugins.jetbrains.com/docs/intellij/work-with-icons-and-images.html#mapping-new-ui-icons
- Display detailed configuration file list(with clickable links), some codes with taken from the open sourced https://github.com/beansoft/react-native-console