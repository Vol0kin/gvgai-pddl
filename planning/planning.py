import requests
import re
import time

ini = time.time()

with open("../domains/boulderdash-domain.pddl") as f:
    domain = f.read()

with open("problem.pddl") as f:
    problem = f.read()

data = {"domain": domain, "problem": problem}

resp = requests.post("http://127.0.0.1:5000/solve", json=data)
print(resp)
print(resp.json())
