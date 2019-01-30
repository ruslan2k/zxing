## Pharmacode support

How to download or build own lib and android app with [Pharmacode](https://en.wikipedia.org/wiki/Pharmacode) support

You should remove the original Barcode Scanner before install this

### Download only

[android-4.7.7.apk](http://bit.ly/2FWKTEV)

### Build own

Clone this repo

Checkout to [pharma](https://github.com/ruslan2k/zxing/tree/pharma) branch

```
git checkout pharma
```

Install Android SDK or build tools only.

Install Android API 22

```
${ANDROID_HOME}/tools/bin/sdkmanager "platforms;android-22"
```

Build with maven

```
mvn install \
        -Drat.numUnapprovedLicenses=100 \
        -Dcheckstyle.skip \
        -DskipTests
```

