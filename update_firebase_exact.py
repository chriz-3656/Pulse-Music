import re

# Update top-level build.gradle.kts
with open('build.gradle.kts', 'r') as f:
    text = f.read()

text = re.sub(r'id\("com\.google\.gms\.google-services"\) version ".*?" apply false', 'id("com.google.gms.google-services") version "4.4.1" apply false', text)

with open('build.gradle.kts', 'w') as f:
    f.write(text)

# Update app/build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    app_text = f.read()

app_text = re.sub(r'implementation\(platform\("com\.google\.firebase:firebase-bom:.*?"\)\)', 'implementation(platform("com.google.firebase:firebase-bom:32.8.1"))', app_text)

with open('app/build.gradle.kts', 'w') as f:
    f.write(app_text)
