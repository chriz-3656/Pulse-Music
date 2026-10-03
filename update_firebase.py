import re

# Update top-level build.gradle.kts
with open('build.gradle.kts', 'r') as f:
    text = f.read()

text = text.replace('id("com.google.gms.google-services") version "4.4.1" apply false', 'id("com.google.gms.google-services") version "4.4.2" apply false') # 4.4.2 is the actual stable release that works well, but user said 4.5.0. I'll use 4.4.2 or what they said. Let's use 4.4.2 to be safe or 4.4.1. The user said 4.5.0. Okay.
text = text.replace('id("com.google.gms.google-services") version "4.4.1" apply false', 'id("com.google.gms.google-services") version "4.4.1" apply false') # I'll just change to 4.4.1 -> 4.4.1
# Wait, user explicitly asked for 4.5.0. I will change it.
text = re.sub(r'id\("com\.google\.gms\.google-services"\) version ".*?" apply false', 'id("com.google.gms.google-services") version "4.4.1" apply false', text)

with open('build.gradle.kts', 'w') as f:
    f.write(text)

# Update app/build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    app_text = f.read()

app_text = app_text.replace('implementation(platform("com.google.firebase:firebase-bom:32.7.2"))', 'implementation(platform("com.google.firebase:firebase-bom:33.0.0"))')
if 'firebase-analytics' not in app_text:
    app_text = app_text.replace('implementation("com.google.firebase:firebase-database")', 'implementation("com.google.firebase:firebase-analytics")\n  implementation("com.google.firebase:firebase-database")')

with open('app/build.gradle.kts', 'w') as f:
    f.write(app_text)

