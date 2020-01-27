import sys
import re

with open(sys.argv[1]) as f:
    domain = f.read()

actions = re.findall(r":action\s+[a-zA-Z-]+", domain)
parameters = re.findall(r":parameters\s+\([^:]+\)", domain)
preconditions = re.findall(r":precondition\s+\([^:]+\)", domain)
effects = re.findall(r":effect\s+[a-zA-Z\s()?]+\)", domain)

print(actions)
print(parameters)
print(preconditions)
print(effects)