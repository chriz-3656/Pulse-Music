import re

with open('app/build.gradle.kts', 'r') as f:
    text = f.read()

# Remove Spotify BuildConfig fields and manifest placeholders
spotify_config_regex = r'\s*buildConfigField\("String", "SPOTIFY_CLIENT_ID".*?\n\s*buildConfigField\("String", "SPOTIFY_CLIENT_SECRET".*?\n\s*manifestPlaceholders\["redirectSchemeName"\].*?\n\s*manifestPlaceholders\["redirectHostName"\].*?\n'
text = re.sub(spotify_config_regex, '\n', text)

# Fix signing configs fallback
old_signing = """  signingConfigs {
    create("release") {
      storeFile = file("${rootDir}/release.keystore")
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
  }"""
new_signing = """  signingConfigs {
    create("release") {
      val keystoreFile = file("${rootDir}/release.keystore")
      if (keystoreFile.exists()) {
          storeFile = keystoreFile
          storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
          keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
          keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      }
    }
  }"""
text = text.replace(old_signing, new_signing)

old_build_type = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }"""
new_build_type = """    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystoreFile = file("${rootDir}/release.keystore")
      if (keystoreFile.exists()) {
          signingConfig = signingConfigs.getByName("release")
      }
    }"""
text = text.replace(old_build_type, new_build_type)

# Remove Spotify Auth dependency
text = re.sub(r'\s*implementation\("com\.spotify\.android:auth:2\.1\.1"\)', '', text)

with open('app/build.gradle.kts', 'w') as f:
    f.write(text)

