import requests
import re
import time

ini = time.time()

with open("domain.pddl") as f:
    domain = f.read()

with open("problem.pddl") as f:
    problem = f.read()

data = {"domain": domain, "problem": problem}

resp = requests.post("http://solver.planning.domains/solve", data=data)
print(resp.json())
for i in resp.json()["result"]["plan"]:
    a = re.sub(" +", " ", i["action"].strip().replace("\n", ""))
    print(a)
    a = re.sub(" \)", ")", a)
    print(a)
#print(resp.json()["result"]["plan"])
b = time.time() - ini
print(b)
