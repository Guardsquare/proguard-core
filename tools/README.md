# ProGuardCORE command line tools

The command line tools provide convenient access to some functionalities of ProGuardCORE directly from the command line:

* Conversion of dex files (or APKs) into jar files
* Listing classes, methods & fields
* Printing a textual representation of classes

## Usage

```shell
$ proguard-core-tools --help
Usage: proguard-core-tools options_list
Subcommands: 
    dex2jar - Convert dex to jar
    list - List classes, methods & fields
    print - Print classes details

Options: 
    --help, -h -> Usage info 
```
## Execute via Gradle

```shell
$ /gradlew :proguard-core-tools:run --args='--help'
```

## Install distribution

```
$ ./gradlew :proguard-core-tools:installDist
$ cp -R proguard-core-tools/build/install/proguard-core-tools /path/to/install/dir
$ cd /path/to/install/dir
$ ./proguard-core-tools --help
```
