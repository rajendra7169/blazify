package com.blazify.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BugReportTest {
    @Test
    fun opensTheBugFormWithVersionAndPhoneFilledIn() {
        val url = BugReport.issueUrl(
            version = "9.13.3 (156, gms)",
            device = "Google Pixel 8, Android 16 (API 36), language en_US",
        )

        assertEquals(
            "https://github.com/rajendra7169/blazify/issues/new?template=bug_report.yml" +
                "&version=9.13.3%20%28156%2C%20gms%29" +
                "&device=Google%20Pixel%208%2C%20Android%2016%20%28API%2036%29%2C%20language%20en_US",
            url,
        )
    }

    @Test
    fun symbolsInThePhoneNameDoNotBreakTheLink() {
        val url = BugReport.issueUrl(version = "1.0 & more", device = "A+B #1")

        assertTrue(url, "&version=1.0%20%26%20more&" in url)
        assertTrue(url, url.endsWith("&device=A%2BB%20%231"))
    }

    @Test
    fun theFormStillHasTheBoxesTheLinkFills() {
        val form = File("../.github/ISSUE_TEMPLATE/bug_report.yml").readText()

        assertTrue("id: version" in form)
        assertTrue("id: device" in form)
    }
}
