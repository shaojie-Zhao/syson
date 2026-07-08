import json, urllib.request, sys

ctx = 'f7445a0d-5509-45c0-8f12-fe5edd235094'
target = '21f054d5-f48a-4f0e-9bc4-07d241771b8a'

# Step 1: createChild
uid = 'a1000000-0000-0000-0000-000000000001'
mutation = '''mutation {
  createChild(input: {
    id: "%s",
    editingContextId: "%s",
    objectId: "%s",
    childCreationDescriptionId: "SysMLv2EditService-PartUsage"
  }) {
    __typename
    ... on CreateChildSuccessPayload { object { id } }
    ... on ErrorPayload { message }
  }
}''' % (uid, ctx, target)
body = json.dumps({'query': mutation})
req = urllib.request.Request('http://192.168.3.111:8080/api/graphql', data=body.encode(), headers={'Content-Type':'application/json'})
resp = json.loads(urllib.request.urlopen(req, timeout=10).read())
sid = resp.get('data',{}).get('createChild',{}).get('object',{}).get('id','')
print('Step1 createChild siriusId:', sid)

# Step 2: element-id
if sid:
    req2 = urllib.request.Request('http://192.168.3.111:8080/api/matrix/default-matrix/element-id?ctxId=%s&objectId=%s' % (ctx, sid))
    eid = json.loads(urllib.request.urlopen(req2, timeout=10).read()).get('elementId','')
    print('Step2 elementId:', eid)

    # Step 3: rename
    if eid:
        body3 = json.dumps({'ctxId': ctx, 'siriusId': sid, 'newName': 'TEST-NAME'})
        req3 = urllib.request.Request('http://192.168.3.111:8080/api/matrix/default-matrix/rename-by-sirius', data=body3.encode(), headers={'Content-Type':'application/json'}, method='POST')
        resp3 = json.loads(urllib.request.urlopen(req3, timeout=10).read())
        print('Step3 rename:', resp3.get('result'))
