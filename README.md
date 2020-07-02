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

- OpenJDK 8
- Maven
- Python3
- Python3 venv

To install them, run the following command:

```sh
$ sudo apt install openjdk-8-jdk maven python3 python3-venv
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
deactivate
```

To create the executable JAR file, run one of these two commands:

```sh
# Create executable file and run the tests
$ mvn package

# Create executable file without running the tests
$ mvn package -DskipTests=true
```

Either of these commands will generate the following JAR file: `target/GVGAI-PDDL-1.0.jar`. The `target/` directory
also contains external dependencies. Without them, the JAR file can't be executed.

## :computer: Usage

### Generation of configuration files templates

### Running the system

