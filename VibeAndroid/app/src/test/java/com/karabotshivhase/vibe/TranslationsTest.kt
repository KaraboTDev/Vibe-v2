package com.karabotshivhase.vibe

import com.karabotshivhase.vibe.data.settings.LanguageCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TranslationsTest {

    private fun findResDir(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Could not find res directory")
    }

    private fun parseXmlFile(file: File): Element {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        return doc.documentElement
    }

    private fun extractStrings(root: Element): Pair<Map<String, String>, Map<String, String>> {
        val translatable = mutableMapOf<String, String>()
        val untranslatable = mutableMapOf<String, String>()

        val stringNodes = root.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i) as Element
            val name = node.getAttribute("name")
            val isTranslatable = node.getAttribute("translatable") != "false"
            val text = node.textContent

            if (isTranslatable) {
                translatable[name] = text
            } else {
                untranslatable[name] = text
            }
        }
        return Pair(translatable, untranslatable)
    }

    private fun extractPlurals(root: Element): Map<String, Map<String, String>> {
        val pluralsMap = mutableMapOf<String, Map<String, String>>()
        val pluralNodes = root.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val element = pluralNodes.item(i) as Element
            val name = element.getAttribute("name")
            val itemsMap = mutableMapOf<String, String>()
            val itemNodes = element.getElementsByTagName("item")
            for (j in 0 until itemNodes.length) {
                val itemEl = itemNodes.item(j) as Element
                val quantity = itemEl.getAttribute("quantity")
                itemsMap[quantity] = itemEl.textContent
            }
            pluralsMap[name] = itemsMap
        }
        return pluralsMap
    }

    private fun extractPlaceholders(text: String): List<String> {
        val regex = Regex("%(\\d+\\\$)?[-#+ 0,(<]*[0-9]*(\\.[0-9]+)?[a-zA-Z]")
        return regex.findAll(text).map { it.value }.sorted().toList()
    }

    @Test
    fun zu_and_af_contain_every_translatable_english_string() {
        val resDir = findResDir()
        val enFile = File(resDir, "values/strings.xml")
        val (enStrings, _) = extractStrings(parseXmlFile(enFile))

        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            assertTrue("File values-$locale/strings.xml exists", locFile.exists())
            val (locStrings, _) = extractStrings(parseXmlFile(locFile))

            val missing = enStrings.keys.filter { it !in locStrings }
            assertTrue(
                "Locale '$locale' is missing translatable strings: $missing",
                missing.isEmpty()
            )
        }
    }

    @Test
    fun translations_have_no_extra_strings_that_english_lacks() {
        val resDir = findResDir()
        val enFile = File(resDir, "values/strings.xml")
        val (enStrings, enUntranslatable) = extractStrings(parseXmlFile(enFile))
        val enAllKeys = enStrings.keys + enUntranslatable.keys

        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            val (locStrings, locUntranslatable) = extractStrings(parseXmlFile(locFile))
            val locAllKeys = locStrings.keys + locUntranslatable.keys

            val extra = locAllKeys.filter { it !in enAllKeys }
            assertTrue(
                "Locale '$locale' has extra keys not present in English: $extra",
                extra.isEmpty()
            )
        }
    }

    @Test
    fun no_translation_is_blank() {
        val resDir = findResDir()
        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            val (locStrings, _) = extractStrings(parseXmlFile(locFile))

            for ((key, value) in locStrings) {
                assertFalse("Translation for '$key' in locale '$locale' is blank", value.isBlank())
            }
        }
    }

    @Test
    fun format_placeholders_match_english() {
        val resDir = findResDir()
        val enFile = File(resDir, "values/strings.xml")
        val (enStrings, _) = extractStrings(parseXmlFile(enFile))

        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            val (locStrings, _) = extractStrings(parseXmlFile(locFile))

            for ((key, enText) in enStrings) {
                val locText = locStrings[key] ?: continue
                val enPlaceholders = extractPlaceholders(enText)
                val locPlaceholders = extractPlaceholders(locText)
                assertEquals(
                    "Placeholder mismatch for '$key' in locale '$locale'",
                    enPlaceholders,
                    locPlaceholders
                )
            }
        }
    }

    @Test
    fun plurals_define_one_and_other_in_each_locale() {
        val resDir = findResDir()
        val enFile = File(resDir, "values/strings.xml")
        val enPlurals = extractPlurals(parseXmlFile(enFile))

        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            val locPlurals = extractPlurals(parseXmlFile(locFile))

            for ((pluralName, _) in enPlurals) {
                val locMap = locPlurals[pluralName]
                assertNotNull("Plural '$pluralName' missing in locale '$locale'", locMap)
                assertTrue(
                    "Plural '$pluralName' in locale '$locale' missing 'one'",
                    locMap!!.containsKey("one")
                )
                assertTrue(
                    "Plural '$pluralName' in locale '$locale' missing 'other'",
                    locMap.containsKey("other")
                )
            }
        }
    }

    @Test
    fun untranslatable_strings_are_not_repeated_in_translations() {
        val resDir = findResDir()
        val enFile = File(resDir, "values/strings.xml")
        val (_, enUntranslatable) = extractStrings(parseXmlFile(enFile))

        val locales = listOf("zu", "af")
        for (locale in locales) {
            val locFile = File(resDir, "values-$locale/strings.xml")
            val (locStrings, locUntranslatable) = extractStrings(parseXmlFile(locFile))

            for (untranslatableKey in enUntranslatable.keys) {
                assertFalse(
                    "Untranslatable key '$untranslatableKey' should not be in values-$locale/strings.xml",
                    locStrings.containsKey(untranslatableKey) || locUntranslatable.containsKey(untranslatableKey)
                )
            }
        }
    }

    @Test
    fun locales_config_lists_exactly_en_zu_af_and_matches_LanguageCodes_SUPPORTED() {
        val resDir = findResDir()
        val localesFile = File(resDir, "xml/locales_config.xml")
        assertTrue("locales_config.xml exists", localesFile.exists())

        val root = parseXmlFile(localesFile)
        val localeNodes = root.getElementsByTagName("locale")
        val foundLocales = mutableListOf<String>()

        for (i in 0 until localeNodes.length) {
            val el = localeNodes.item(i) as Element
            val name = el.getAttribute("android:name")
            foundLocales.add(name)
        }

        val expected = listOf("en", "zu", "af")
        assertEquals(expected, foundLocales)
        assertEquals(LanguageCodes.SUPPORTED, foundLocales)
    }
}
