[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![Java CI with Maven](https://github.com/Vol0kin/gvgai-pddl/workflows/Java%20CI%20with%20Maven/badge.svg)
[![Build Status](https://travis-ci.org/Vol0kin/gvgai-pddl.svg?branch=master)](https://travis-ci.org/Vol0kin/gvgai-pddl)

# :robot: GVGAI-PDDL

Implementation of a planning-based agent in GVGAI. This project is being developed as part
of my Bachelor's thesis.

The project makes use of the *PDDL Solver* from [planning.domains](http://planning.domains/).
If you want to have more information about this solver and how you can use it in
your own project, please check [the following page](http://solver.planning.domains/).

>This projects is being developed and partially funded under the framework of the Spanish MINECO
R&D Projects TIN2015-71618-R and RTI2018-098460-B-I00.

## :unlock: Requirements

This project has the following dependencies:

- git
- OpenJDK 8
- Maven
- Python3
- Python3 venv

To install them, run the following command:

```sh
$ sudo apt install git openjdk-8-jdk maven python3 python3-venv
```

**NOTE**: If you have more than one version of `java` installed in your device, you will have to select
the `openjdk-8-jdk` version by running `sudo update-alternatives --config java`.

## :wrench: Installation

First clone this repository:

```sh
$ git clone https://github.com/Vol0kin/gvgai-pddl.git
```

After the repository has been cloned, change the working directory to the directory that contains
the cloned files. To do that, run the following command:

```sh
$ cd gvgai-pddl
```

To install the required Python dependencies, you can create a virtual environment and install them in there.
You can do this by running the following commands:

```sh
# Create virtual envirnoment
$ python3 -m venv env

# Activate virtual envirnment
$ source env/bin/activate

# Install dependencies
$ pip install -r requirements.txt
```

This way, you will be able to run the Python script without any kind of issue whenever the virtual
environment is active. If you want to exit the virtual environment, run the following command:

```sh
$ deactivate
```

To create the executable JAR file, run either of these two commands:

```sh
# Create executable file and run the tests
$ mvn package

# Create executable file without running the tests
$ mvn package -DskipTests=true
```

Either of them will generate the following JAR file: `target/GVGAI-PDDL-1.0.jar`. The `target/` directory
also contains external dependencies. Without them, the JAR file can't be executed.

## :computer: Usage

### :pencil: Generation of template configuration files

The `gen_config_file.py` script allows the generation of template configuration files.

```
usage: gen_config_file.py [-h] -d DOMAIN -g GAME [-o OUTPUT] [--orientations]

Generate the game's configuration file.

optional arguments:
  -h, --help            show this help message and exit
  -d DOMAIN, --domain DOMAIN
                        Domain file.
  -g GAME, --game GAME  Game description file.
  -o OUTPUT, --output OUTPUT
                        Output file.
  --orientations        Use orientation predicates.
```

The game description files can be found in the `examples/gridphysic/` directory.
Their names follow the following pattern: `gamename.txt`.

### :running: Running the system

To run the system, execute the following command:

```sh
$ java -jar target/GVGAI-PDDL-1.0.jar [ options ]
```

The detailed list of options can be found here:

```
Usage: GVGAI-PDDL [-dhsV] [--localhost] [-c=<configurationFile>] -g=<gameIdx>
                  -l=<levelIdx>
Launches a new GVGAI game played by a planning agent or by a human.
  -c, --config=<configurationFile>
                           YAML configuration file that will be used by the
                             agent.
  -d, --debug              Debug mode.
  -g, --game=<gameIdx>     Game to be played.
  -h, --help               Show this help message and exit.
  -l, --level=<levelIdx>   Level to be played.
      --localhost          Call planner running on localhost.
  -s, --save               Save runtime information (problems, plans and log).
  -V, --version            Print version information and exit.
```

The `levelIdx` parameter ranges from 0 to 4. To find out what the `gameIdx` of a given
game is, please check [this file](https://github.com/Vol0kin/gvgai-pddl/blob/master/examples/all_games_sp.csv).

#### :joystick: Human player

If you want to play a level of a given game to mess with it and understand it better, just run the system with the following options:

```sh
$ java -jar target/GVGAI-PDDL-1.0.jar -g [gameIdx] -l [lvlIdx]
```

#### :space_invader: Run system using configuration file

To run the system using the implemented planning agent, you must specify the path of a configuration file. To do so, run the
system with the following options:

```sh
$ java -jar target/GVGAI-PDDL-1.0.jar -g [gameIdx] -l [lvlIdx] -c [configurationFile]
```

#### :bug: Debugging mode

The system also has a debugging mode, which can be very handy if you want to get more information during execution time. To enable it,
run the system with the following options:

```sh
$ java -jar target/GVGAI-PDDL-1.0.jar -g [gameIdx] -l [lvlIdx] -c [configurationFile] -d
```

> **Note**: In order to run the debugging mode, you have to use the implemented planning agent. Thus, you must specify a configuration
file. Otherwise, the `-d` flag won't do anything.

#### :floppy_disk: Save runtime information

You can also save runtime information such as the generated problems, the response plans and a log file which contains
information about everything that is going on in the system. To enable this option, run the system as follows:

```sh
$ java -jar target/GVGAI-PDDL-1.0.jar -g [gameIdx] -l [lvlIdx] -c [configurationFile] -s
```

> **Note**: This option is only available if you are running the implemented planning agent. Thus, you must specify a configuration
file. Otherwise, the `-s` flag won't do anything.

## :cloud: Running the planner on localhost

Sometimes you might experience some issues while trying to run the system because the cloud solver is busy.
To solve this problems and get faster execution times, consider running it on localhost. This is especially advisable if you
are going to do some heavy testing. However, in order to run it you will need to install NodeJS beforehand. Please referr to
the [official web page](https://nodejs.org/en/) to find the latests releases and the installation process.

After you have installed it, go to the [solver's repository](https://github.com/AI-Planning/cloud-solver) and
clone it. Once you have cloned it, go to the directory where the source files are found and run the
following commands:

```sh
# Install the project's dependencies
$ npm install .

# Run the server
$ node web.js
```

This will create a new server running on `localhost:5000`. By running the system with the `--localhost` option,
the HTTP requests will be automatically sent to the server running on localhost.

## :books: Source code documentation

The source code's documentation is available [here](https://vol0kin.github.io/gvgai-pddl/src-docs/). There you can
find the generated JavaDoc for the whole project (including GVGAI's source files). However, if you are more interested
in the documentation of this particular agent, you can check the
[controller package documentation](https://vol0kin.github.io/gvgai-pddl/src-docs/controller/package-summary.html).

## :mortar_board: Other documentation

In the following links you can find out more documentation about this project:

- [My Bachelor's Thesis (Spanish)](https://vol0kin.github.io/TFG/pdfs/memoriaTFG.pdf)
- [User's manual (Spanish)](https://vol0kin.github.io/TFG/pdfs/manual_usuario.pdf)

## :handshake: Contributing

If you want to contribute to this project or think that some feature is broken/missing, please consider
opening a PR. All contributions are welcome! :smile:
