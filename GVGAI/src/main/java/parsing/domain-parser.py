import sys
import re

with open(sys.argv[1]) as f:
    domain = f.read()

actions = re.findall(r":PDDLAction\s+[a-zA-Z-]+", domain)
parameters = re.findall(r":parameters\s+\([^:]+\)", domain)
preconditions = re.findall(r":precondition\s+\([^:]+\)", domain)
effects = re.findall(r":effect\s+[a-zA-Z\s()?]+\)", domain)

# Get PDDLAction names
action_names = list(map(lambda x: x.split()[1], actions))

print(actions)
print(parameters)
print(preconditions)
print(effects)

print(action_names)