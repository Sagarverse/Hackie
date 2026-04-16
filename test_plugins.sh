#!/bin/bash
sed -i '' 's/alias(libs.plugins.kotlin.android)//' app/build.gradle.kts
./gradlew :app:help
