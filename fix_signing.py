import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

old_signing = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystoreFile = file("${rootDir}/release.keystore")
      if (keystoreFile.exists()) {
          signingConfig = signingConfigs.getByName("release")
      }
    }"""
new_signing = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystoreFile = file("${rootDir}/release.keystore")
      if (keystoreFile.exists()) {
          signingConfig = signingConfigs.getByName("release")
      } else {
          signingConfig = signingConfigs.getByName("debug")
      }
    }"""
text = text.replace(old_signing, new_signing)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)
