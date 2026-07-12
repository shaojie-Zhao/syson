import json, urllib.request, uuid

EC_ID = "94c2ae91-5b6f-4621-899d-ba5f1520fa65"
ELEM_ID = "39f0d54d-35a7-428d-9582-d9b5c1c90683"
NAME = "TestCap_UnderPackage"

def graphql(query, variables=None):
    body = json.dumps({"query": query, "variables": variables or {}})
    req = urllib.request.Request("http://localhost:8080/api/graphql", 
        data=body.encode(), headers={"Content-Type":"application/json"})
    return json.loads(urllib.request.urlopen(req, timeout=10).read())

# Step 1: Find Package ancestor
print("Step 1: find-package-ancestor...")
req = urllib.request.Request(f"http://localhost:8080/api/matrix/default-matrix/find-package-ancestor?ctxId={EC_ID}&elementId={ELEM_ID}")
pkg = json.loads(urllib.request.urlopen(req, timeout=10).read())
print(f"Package ID: {pkg}")

# Step 2: Create under Package
pkg_id = pkg.get("packageId", ELEM_ID)
uid = str(uuid.uuid4())
desc = f"SysMLv2EditService-PartDefinition:{NAME}"
print(f"Step 2: createChild under Package {pkg_id[:8]}...")
r = graphql('mutation c($i:CreateChildInput!){createChild(input:$i){__typename ... on CreateChildSuccessPayload{object{id label}}... on ErrorPayload{message}}}',
    {"i": {"id": uid, "editingContextId": EC_ID, "objectId": pkg_id, "childCreationDescriptionId": desc}})
print(json.dumps(r, indent=2))

sid = r.get("data",{}).get("createChild",{}).get("object",{}).get("id","") if r.get("data") else ""
if sid:
    print(f"\nSUCCESS: Created '{NAME}' under Package")
    print("Now refresh the Explorer tree to see the new element at root level.")
else:
    print(f"\nFAILED: {r.get('errors',[{}])[0].get('message','?')}")
