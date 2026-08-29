# Reproducibility

Every release APK is built on several providers, and published only if the two are byte-identical. This page is how you check that yourself.

Canonical builder: **Linux x86_64**.

## Pinned inputs

| Component                             | Where it's pinned                          |
| ------------------------------------- | ------------------------------------------ |
| JDK (Temurin 21.0.12.1+1)             | `ci/setup-android.sh`                      |
| Android cmdline-tools                 | `ci/setup-android.sh`                      |
| Android platform 37.1, build-tools 37 | `ci/setup-android.sh`                      |
| Gradle wrapper jar                    | `ci/setup-android.sh`                      |
| Gradle distribution 9.7.1             | `gradle/wrapper/gradle-wrapper.properties` |
| AGP, Kotlin, Compose BOM, androidx    | `gradle/libs.versions.toml`                |
| Every dependency, by SHA-256          | `gradle/verification-metadata.xml`         |
| compileSdk / minSdk / targetSdk       | `app/build.gradle.kts`                     |

- The JDK, cmdline-tools and Gradle distribution are checksum-verified on download
- The 445 components in the verification metadata are checked by Gradle on resolution
- Node.js is used by CI actions and pinned in `ci/setup-node.sh`
- `dependenciesInfo` and `vcsInfo` are disabled in `app/build.gradle.kts` because both embed build-time data

## Release pipeline

CI runs `.forgejo/workflows/release.yml` on a published release.

1. `build` step runs on ephemeral VMs from several providers, each producing `app-release-unsigned.apk`
2. `verify` step compares all artifacts by SHA-256 and fails unless they match
3. The artifact is downloaded, signed locally with offline key using `ci/sign-release.sh`
4. The resulting APK is tested, then uploaded to Forgejo releases
5. Finally, it's distributed via Play Store, F-Droid, etc

## Verify a published release

Debian 12 x86_64, as root:

```sh
# 1. Reproduce the app locally
TAG=v1.1
git checkout "$TAG"

export JAVA_HOME=/opt/jdk ANDROID_HOME=/opt/android-sdk
export TZ=UTC LC_ALL=C
./ci/setup-android.sh
./gradlew assembleRelease
LOCAL=app/build/outputs/apk/release/app-release-unsigned.apk

# 2. Fetch from https://git.hloth.dev/hloth/zaragoza-tarjeta-bus-android/releases
PUBLISHED=/path/to/zaragoza-tarjeta-bus-$TAG.apk

# 3. Compare locally built app with the release
apk_content_hash() {
  unzip -Z1 "$1" \
    | grep -vE '^META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|EC|DSA))$' \
    | while IFS= read -r entry; do
        printf '%s  %s\n' \
          "$(unzip -p "$1" "$entry" | sha256sum | cut -c1-64)" \
          "$entry"
      done
}

if diff <(apk_content_hash "$LOCAL") <(apk_content_hash "$PUBLISHED"); then
  echo "✓ APK hash checksum matches"
else
  echo "✗ APK hash checksum mismatch, local build does not match the published APK" >&2
  exit 1
fi
```
