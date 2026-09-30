import json, urllib.request, time, sys

print("Waiting for new CI run to start and complete...")
time.sleep(10)
while True:
    req = urllib.request.Request('https://api.github.com/repos/chriz-3656/Pulse-Music/actions/runs?branch=feature/spotify-oauth')
    try:
        data = json.load(urllib.request.urlopen(req))
        run = data['workflow_runs'][0]
        status = run['status']
        conclusion = run['conclusion']
        print(f"Current status: {status}, conclusion: {conclusion}")
        if status == 'completed' and run['head_sha'] == sys.argv[1]:
            print(f"CI Completed with conclusion: {conclusion}")
            if conclusion == 'success':
                sys.exit(0)
            else:
                sys.exit(1)
        elif status == 'completed':
            pass # Old run
    except Exception as e:
        print(f"Error checking CI: {e}")
    time.sleep(15)
