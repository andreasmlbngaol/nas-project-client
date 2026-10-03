package id.andreasmlbngaol.nas_project.data

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The one non-obvious piece: the session cookie must survive a restart. This
 * checks the jvm cookie storage persists `id` on addCookie and replays it from disk.
 *
 * The session file is redirected to a temp dir so the test never touches the
 * real ~/.nas-project/session (doing so would log the developer out).
 */
class CookiePersistenceTest {

    private val url = Url("http://localhost:3000")

    @BeforeTest
    fun useTempSessionFile() {
        val tmp = createTempDirectory("nas-test").resolve("session").toFile()
        System.setProperty("nas.sessionFile", tmp.absolutePath)
    }

    @AfterTest
    fun reset() {
        System.clearProperty("nas.sessionFile")
    }

    @Test
    fun `session cookie survives a new storage instance`() = runBlocking {
        val first = persistentCookiesStorage()
        first.addCookie(url, Cookie("id", "abc123"))
        assertEquals("abc123", loadSession())

        // A fresh instance simulates an app restart.
        val afterRestart = persistentCookiesStorage()
        assertEquals("abc123", afterRestart.get(url).first { it.name == "id" }.value)

        clearSession()
        assertNull(loadSession())
    }

    @Test
    fun `non-session cookies are ignored`() = runBlocking {
        persistentCookiesStorage().addCookie(url, Cookie("tracking", "xyz"))
        assertNull(loadSession())
    }
}
