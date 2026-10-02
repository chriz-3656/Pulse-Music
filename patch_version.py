import re

# Update build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

text = re.sub(r'versionCode = \d+', 'versionCode = 11', text)
text = re.sub(r'versionName = ".*?"', 'versionName = "2.1.4"', text)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

# Update README.md
with open('README.md', 'r') as f:
    readme = f.read()
readme = readme.replace('v2.1.3', 'v2.1.4')
with open('README.md', 'w') as f:
    f.write(readme)

# Update CHANGELOG.md
with open('CHANGELOG.md', 'r') as f:
    changelog = f.read()
changelog = changelog.replace('[2.1.3]', '[2.1.4]')
with open('CHANGELOG.md', 'w') as f:
    f.write(changelog)

