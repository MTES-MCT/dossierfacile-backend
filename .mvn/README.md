This directory marks the Maven project root, including when Maven is launched
from a module directory without its own `.mvn` directory. The parent POM uses
`maven.multiModuleProjectDirectory` to set `dossierfacile.configDirectory`, which
locates the shared Checkstyle rules and Eclipse formatting profile.

The owner API has its own `.mvn` wrapper directory, so its POM overrides
`dossierfacile.configDirectory` with `../config` relative to the module.
