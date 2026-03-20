# Setting Up Unit Tests and JaCoCo Coverage

This guide walks through adding a unit test and configuring JaCoCo code coverage for this Android project.

## 1. Add the JaCoCo Plugin

In `app/build.gradle.kts`, add the `jacoco` plugin:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    jacoco
}
```

## 2. Configure JaCoCo

Add the following blocks **after** the `android { }` block and **before** the `dependencies { }` block in `app/build.gradle.kts`:

### Enable coverage on test tasks

```kotlin
tasks.withType<Test> {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
```

### Register the coverage report task

```kotlin
tasks.register<JacocoReport>("JacocoDebugCodeCoverage") {
    dependsOn("testDebugUnitTest")
    group = "Reporting"
    description = "Generate JaCoCo coverage reports for debug unit tests"

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(
            "**/R.class",
            "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/ComposableSingletons*",
            "**/*_Factory*",
            "**/*_MembersInjector*",
            "**/ui/theme/**"
        )
    }

    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom("${project.projectDir}/src/main/java")
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
        }
    )
}
```

**What this does:**

- `classDirectories` — points to compiled Kotlin debug classes, excluding generated/framework files (R, BuildConfig, Compose singletons, DI factories, theme files)
- `sourceDirectories` — points to source code so the HTML report can show line-by-line coverage
- `executionData` — reads the `.exec` file produced by JaCoCo during test execution
- Reports are generated in both XML (for CI tools like SonarQube) and HTML (for local viewing)

## 3. Create a Utility Class to Test

Create `app/src/main/java/dev/elainedb/ytdash_android_claude/utils/StringHelper.kt`:

```kotlin
package dev.elainedb.ytdash_android_claude.utils

object StringHelper {

    fun isPalindrome(text: String): Boolean {
        val cleaned = text.lowercase().filter { it.isLetterOrDigit() }
        return cleaned == cleaned.reversed()
    }

    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun reverseWords(text: String): String {
        return text.trim().split("\\s+".toRegex()).reversed().joinToString(" ")
    }

    fun capitalizeWords(text: String): String {
        return text.split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }

    fun removeVowels(text: String): String {
        return text.filter { it.lowercaseChar() !in "aeiou" }
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
```

## 4. Create the Unit Test

Create `app/src/test/java/dev/elainedb/ytdash_android_claude/utils/StringHelperTest.kt`:

```kotlin
package dev.elainedb.ytdash_android_claude.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringHelperTest {

    @Test
    fun `isPalindrome returns true for simple palindrome`() {
        assertTrue(StringHelper.isPalindrome("racecar"))
    }

    @Test
    fun `isPalindrome ignores case and spaces`() {
        assertTrue(StringHelper.isPalindrome("A man a plan a canal Panama"))
    }

    @Test
    fun `isPalindrome returns false for non-palindrome`() {
        assertFalse(StringHelper.isPalindrome("hello"))
    }

    @Test
    fun `isPalindrome handles empty string`() {
        assertTrue(StringHelper.isPalindrome(""))
    }

    @Test
    fun `wordCount returns correct count`() {
        assertEquals(3, StringHelper.wordCount("hello world test"))
    }

    @Test
    fun `wordCount handles multiple spaces`() {
        assertEquals(2, StringHelper.wordCount("hello   world"))
    }

    @Test
    fun `wordCount returns zero for blank string`() {
        assertEquals(0, StringHelper.wordCount(""))
        assertEquals(0, StringHelper.wordCount("   "))
    }

    @Test
    fun `reverseWords reverses word order`() {
        assertEquals("world hello", StringHelper.reverseWords("hello world"))
    }

    @Test
    fun `capitalizeWords capitalizes first letter of each word`() {
        assertEquals("Hello World", StringHelper.capitalizeWords("hello world"))
    }

    @Test
    fun `removeVowels removes all vowels`() {
        assertEquals("hll wrld", StringHelper.removeVowels("hello world"))
    }

    @Test
    fun `removeVowels handles uppercase vowels`() {
        assertEquals("HLL", StringHelper.removeVowels("HAELLO"))
    }

    @Test
    fun `isValidEmail accepts valid email`() {
        assertTrue(StringHelper.isValidEmail("user@example.com"))
    }

    @Test
    fun `isValidEmail rejects missing at sign`() {
        assertFalse(StringHelper.isValidEmail("userexample.com"))
    }

    @Test
    fun `isValidEmail rejects missing domain`() {
        assertFalse(StringHelper.isValidEmail("user@"))
    }

    @Test
    fun `isValidEmail rejects missing TLD`() {
        assertFalse(StringHelper.isValidEmail("user@example"))
    }
}
```

## 5. Run Tests and Generate Coverage

### Run unit tests only

```bash
./gradlew :app:testDebugUnitTest
```

### Run tests and generate coverage report

```bash
./gradlew :app:testDebugUnitTest :app:JacocoDebugCodeCoverage
```

### View the coverage report

Open the HTML report in a browser:

```bash
open app/build/reports/jacoco/JacocoDebugCodeCoverage/html/index.html
```

The XML report (used by CI tools like SonarQube) is at:

```
app/build/reports/jacoco/JacocoDebugCodeCoverage/JacocoDebugCodeCoverage.xml
```

## 6. CI Integration

The GitHub Actions workflow (`.github/workflows/build.yml`) runs both tasks:

```yaml
- name: JaCoCo
  run: ./gradlew :app:testDebugUnitTest :app:JacocoDebugCodeCoverage
```

The XML coverage report is then consumed by the SonarCloud analysis step that follows.

## Directory Structure

After setup, the relevant files are:

```
app/
├── build.gradle.kts                          # JaCoCo plugin + task config
├── src/
│   ├── main/java/dev/elainedb/ytdash_android_claude/
│   │   └── utils/
│   │       └── StringHelper.kt               # Utility class under test
│   └── test/java/dev/elainedb/ytdash_android_claude/
│       └── utils/
│           └── StringHelperTest.kt           # Unit tests
```
