import sys
p = sys.argv[1] if len(sys.argv) > 1 else '.github/workflows/build.yml'
s = open(p, encoding='utf8').read()
done = []
KEY = '      - name: Decode signing keystore'
assert KEY in s, 'keystore step not found'

if 'native_plugin' not in s:
    inp = """      native_plugin:
        description: 'Native Kotlin plugin (open .enc from other apps, Argon2id). Untick if the build fails'
        type: boolean
        default: true
"""
    k = '    inputs:\n'
    assert k in s, 'inputs block not found'
    s = s.replace(k, k + inp, 1)
    step = r"""      - name: Apply native Kotlin plugin
        if: ${{ inputs.native_plugin }}
        run: |
          set -e
          D=android/app/src/main/java/com/cipherx/app
          test -d "$D"
          cp native/CipherXNativePlugin.kt "$D/CipherXNativePlugin.kt"
          cp native/MainActivity.java "$D/MainActivity.java"
          python3 native/patch_manifest.py android/app/src/main/AndroidManifest.xml
          sed -i "/classpath 'com.android.tools.build:gradle/a\        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21'" android/build.gradle
          sed -i "0,/apply plugin: 'com.android.application'/s//apply plugin: 'com.android.application'\napply plugin: 'kotlin-android'/" android/app/build.gradle
          grep -q "kotlin-gradle-plugin" android/build.gradle || { echo "::error::kotlin classpath was not added"; exit 1; }
          grep -q "kotlin-android" android/app/build.gradle || { echo "::error::kotlin-android was not applied"; exit 1; }
          cat >> android/app/build.gradle << 'EOF'
          tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
              compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(android.compileOptions.targetCompatibility.toString()) }
          }
          EOF

"""
    s = s.replace(KEY, step + KEY, 1)
    done.append('native')

if 'bcprov' not in s:
    bc = r"""      - name: Add Argon2 (BouncyCastle) dependency
        if: ${{ inputs.native_plugin }}
        run: |
          cat >> android/app/build.gradle << 'EOF'
          dependencies {
              implementation 'org.bouncycastle:bcprov-jdk18on:1.78.1'
          }
          EOF

"""
    s = s.replace(KEY, bc + KEY, 1)
    done.append('argon2')

open(p, 'w', encoding='utf8').write(s)
print('workflow patched:', done or 'nothing to do')
