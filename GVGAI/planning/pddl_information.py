import argparse
import os
import re

def check_pddl_extension(path):
    _, ext = os.path.splitext(path)

    if not ext.lower() == '.pddl':
        raise argparse.ArgumentTypeError('Expected domain file to have .pddl extension')

    return path


def check_yaml_extension(path):
    _, ext = os.path.splitext(path)

    if not ext == '.yaml':
        raise argparse.ArgumentTypeError('Expected output file to have .yaml extension')

    return path


if __name__ == '__main__':
    # Create parser object
    parser = argparse.ArgumentParser(description='Generate the game information file',
                                     allow_abbrev=False)

    # Add positional argument referred to the domain file
    # It is checked whether a PDDL file is passed or not
    parser.add_argument('-d',
                        '--domain',
                        action='store',
                        type=check_pddl_extension,
                        required=True)

    # Add positional argument referred to the GVGAI game file
    parser.add_argument('-g',
                        '--game',
                        action='store',
                        required=True)

    # Add optional argument referred to the output file path
    parser.add_argument('-o',
                        '--output',
                        type=check_yaml_extension,
                        action='store')


    # Parse arguments and get namespace
    args = parser.parse_args()

    # Read domain file
    with open(args.domain) as f:
        domain_content = f.read()

    print(domain_content)

