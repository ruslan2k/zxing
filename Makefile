build:
	mvn install \
		-Drat.numUnapprovedLicenses=100 \
		-Dcheckstyle.skip \
		-DskipTests

clean:
	rm -v ~/Desktop/android-*.apk
	rm -v android/target/android-4.7.7.apk

mv:
	mv -v android/target/android-4.7.7.apk ~/Desktop/android-$$(date +%FT%H-%M-%S).apk

