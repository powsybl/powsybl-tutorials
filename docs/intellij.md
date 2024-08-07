---
layout: default
---

# Working a tutorial with IntelliJ IDEA
This guide walks you through using [IntelliJ IDEA](https://www.jetbrains.com/idea/) to build one of the tutorials.

* TOC
{:toc}

## What you will build?
You’ll pick a tutorial and import it into IntelliJ IDEA. Then you can read the guide, work on the code, and run the project.

## What you will need?
- About 15 minutes
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)

## Installing IntelliJ IDEA
If you don’t have IntelliJ IDEA installed yet, visit the link up above. From there, you can download a copy for your platform. To install it simply unpack the downloaded archive.

When you’re done, go ahead and launch IntelliJ IDEA.

## Importing a project
To import an existing project you need some code, so clone or copy one of the [tutorials](https://github.com/powsybl/powsybl-tutorials) repository.

```
$> git clone https://github.com/powsybl/powsybl-tutorials.git
```

With IntelliJ IDEA up and running, click `Import Project` on the Welcome Screen, or `File > Open` on the main menu:
**TODO:** inserer une image

In the pop-up dialog make sure to select Maven's pom.xml under the complete folder:
**TODO:** inserer une image

IntelliJ IDEA will create a project with all the code from the guide ready to run.

## Creating a project from Scratch
In case you’d like to start with an empty project and copy-and-paste your way through the tutorial, create a new Maven project in the `Project Wizard`:
**TODO:** add screenshot
