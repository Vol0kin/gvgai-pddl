import requests

with open("domain.pddl") as f:
    domain = f.read()

with open("problem.pddl") as f:
    problem = f.read()

data = {"domain": domain, "problem": problem}

resp = requests.post("http://solver.planning.domains/solve", data=data)
print(resp)
print(resp.status_code)