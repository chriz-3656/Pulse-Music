import re

with open('.github/workflows/ci.yml', 'r') as f:
    text = f.read()

# Remove Spotify env vars
text = re.sub(r'\s*SPOTIFY_CLIENT_ID:.*?\n\s*SPOTIFY_CLIENT_SECRET:.*?\n', '\n', text)

with open('.github/workflows/ci.yml', 'w') as f:
    f.write(text)
