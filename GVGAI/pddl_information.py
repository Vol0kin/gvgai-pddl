import argparse
import os
import re
import yaml

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


def get_domain_name(domain_content):
    domain_name = re.findall(r'domain\s+[a-zA-Z]+', domain_content)[0]
    domain_name = re.sub(r'\s+', ' ', domain_name).split()[-1]

    return domain_name


def get_pddl_actions(domain_content):
    # Get all the actions
    actions = re.findall(r'action\s+[a-zA-Z\-_]+' , domain_content)

    # Get action names
    actions = list(map(lambda x: re.sub(r'\s+', ' ', x).split()[-1].upper(), actions))

    # Create a dictionary which will map PDDL actions to GVGAI actions
    actions_dict = {act: None for act in actions}

    return actions_dict


def get_game_elements(game_file):
    game_elements = []

    with open(game_file) as f:
        for line in f:
            if 'img' in line:
                element  = re.sub(r'\s+', ' ', line).split()[0]
                game_elements.append(element)


    game_elements_dict = {element: [None] for element in game_elements}

    return game_elements_dict


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

    # Add optional argument referred to the use of orientations
    parser.add_argument('--orientations',
                        action='store_true')

    # Parse arguments and get namespace
    args = parser.parse_args()

    # Read domain file
    with open(args.domain) as f:
        domain_content = f.read()

    ############################################################################
    # Create game information data structure
    game_information = {}

    # Add domain file name
    game_information['domainFile'] = args.domain

    # Add domain name
    game_information['domainName'] = get_domain_name(domain_content)

    # Add a template of the correspondence between the game elements and the
    # PDDL predicates
    game_elements_correspondence = get_game_elements(args.game)
    game_information['gameElementsCorrespondence'] = game_elements_correspondence

    # Add a template of the PDDL types of the variables found in the predicates
    variables_types = {'?variable': 'Type'}
    game_information['variablesTypes'] = variables_types

    # Add cell variable template
    game_information['cellVariable'] = None

    # Add avatar variable template
    game_information['avatarVariable'] = None

    # Add a template of the orientations correspondence to PDDL predicates
    if args.orientations:
        orientation_correspondence = {'UP': None, 'DOWN': None, 'LEFT': None, 'RIGHT': None}
        game_information['orientationCorrespondence'] = orientation_correspondence

    # Add a template of the connections between cells
    connections = {'UP': None, 'DOWN': None, 'LEFT': None, 'RIGHT': None}
    game_information['connections'] = connections

    # Add a template of the actions correspondence
    actions_correspondence = get_pddl_actions(domain_content)
    game_information['actionsCorrespondence'] = actions_correspondence

    # Add goals template
    goals = [{'goalPredicate': None, 'priority': 0}]
    game_information['goals'] = goals


    # Save information
    output = args.output if args.output is not None else 'template.yaml'
    with open(output, 'w') as out:
        yaml.dump(game_information, out, default_flow_style=False, sort_keys=False)





