import argparse


if __name__ == "__main__":
    # Create parser object
    parser = argparse.ArgumentParser(description="Generate the game information file",
                                    allow_abbrev=False)

    # Add positional argument referred to the domain file
    parser.add_argument("-d",
                        "--domain",
                        action="store",
                        required=True)
    
    # Add positional argument referred to the goals file
    parser.add_argument("-g",
                        "--goals",
                        action="store",
                        required=True)
    
    # Add positional argument referred to the connections file
    parser.add_argument("-c",
                        "--connections",
                        action="store",
                        required=True)
    
    # Add optional argument referred to the orientation
    parser.add_argument("-o",
                        "--orientation",
                        action="store_true")

    args = parser.parse_args()

    print(args.domain)