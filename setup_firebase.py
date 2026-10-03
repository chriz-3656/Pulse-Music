import re

# Update top-level build.gradle.kts
with open('build.gradle.kts', 'r') as f:
    text = f.read()

text = text.replace('plugins {', 'plugins {\n  id("com.google.gms.google-services") version "4.4.1" apply false')
with open('build.gradle.kts', 'w') as f:
    f.write(text)

# Update app/build.gradle.kts
with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

text = text.replace('plugins {', 'plugins {\n  id("com.google.gms.google-services")')

deps = """  implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
  implementation("com.google.firebase:firebase-database")
  implementation("com.google.firebase:firebase-auth")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")"""

text = text.replace('dependencies {', 'dependencies {\n' + deps)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)
