[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![Java CI with Maven](https://github.com/Vol0kin/gvgai-pddl/workflows/Java%20CI%20with%20Maven/badge.svg)
[![Build Status](https://travis-ci.org/Vol0kin/gvgai-pddl.svg?branch=master)](https://travis-ci.org/Vol0kin/gvgai-pddl)

# gvgai-pddl

Implementation of a planning-based agent in GVGAI. This project is being developed as part
of my Bachelor's thesis.

The project makes use of the *PDDL Solver* from [planning.domains](http://planning.domains/).
If you want to have more information about this solver and how you can use it in
your own project, please check [the following page](http://solver.planning.domains/).

>This projects is being developed and partially funded under the framework of the Spanish MINECO
R&D Projects TIN2015-71618-R and RTI2018-098460-B-I00.

## Prerequisites

This project has the following dependencies:

- OpenJDK 1.8
- Maven
- Python 3.6+
- Python3 venv

To install them, run the following command:

```sh
$ sudo apt install openjdk-8-jdk maven python3 python3-venv
```

## Installation

First clone this repository:

```sh
$ git clone https://github.com/Vol0kin/gvgai-pddl.git
```

The best way to install the Python dependencies is by creating a virtual environment
and installing them in it. To do so, run the following commands

```sh
# Create virtual envirnoment
$ python3 -m venv env

# Activate virtual envirnment
$ source env/bin/activate

# Install dependencies
$ pip install -r requirements.txt
```

Now everything that's left is installing the main 

```sh
# Create executable file and run the tests
$ mvn package

# Create executable file without running the tests
$ mvn package -DskipTests=true
```
