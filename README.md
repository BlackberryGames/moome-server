# About

This package is a server written in Java for the [Moome](https://github.com/BlackberryCode/moome) game.

Please excuse how poorly written this is; I wrote it when in middle school.

# Installation

## Manual

Be sure you have packages installed for your distribution that provide `javac` (to compile) and `java`.

```
$ git clone git@github.com:BlackberryCode/moome-server.git && cd moome-server
$ ./compile.sh
$ ./run.sh
```

## AUR

Install the package `moome-server` from the AUR:

```
$ trizen -S moome-server
$ moome-server
```

## `makepkg`

```
$ git clone git@github.com:BlackberryCode/moome-server.git && cd moome-server
# makepkg -sif .
$ moome-server
```
