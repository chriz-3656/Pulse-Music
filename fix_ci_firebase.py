import re

with open('.github/workflows/ci.yml', 'r') as f:
    text = f.read()

dummy_json = """      - name: Inject Dummy Google Services JSON
        run: |
          if [ -z "${{ secrets.GOOGLE_SERVICES_JSON }}" ]; then
            echo '{"project_info":{"project_number":"1","project_id":"dummy","storage_bucket":"dummy.appspot.com"},"client":[{"client_info":{"mobilesdk_app_id":"1:1:android:1","android_client_info":{"package_name":"com.aistudio.pulsemusic.kzvpmx"}},"oauth_client":[],"api_key":[{"current_key":"dummy"}],"services":{"appinvite_service":{"other_platform_oauth_client":[]}}}],"configuration_version":"1"}' > app/google-services.json
          else
            echo "${{ secrets.GOOGLE_SERVICES_JSON }}" > app/google-services.json
          fi
"""

text = text.replace('      - name: Grant Execute Permission for Gradle Wrapper', dummy_json + '\n      - name: Grant Execute Permission for Gradle Wrapper')

with open('.github/workflows/ci.yml', 'w') as f:
    f.write(text)
