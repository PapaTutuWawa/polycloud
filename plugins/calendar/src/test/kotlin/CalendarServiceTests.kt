import me.polynom.polycloud.apps.calendar.persistence.repository.CalendarRepository
import me.polynom.polycloud.apps.calendar.service.CalendarService
import me.polynom.polycloud.apps.calendar.testutils.TestConfig
import me.polynom.polycloud.plugin.auth.UserContext
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [TestConfig::class],
)
class CalendarServiceTests {
    @InjectMocks
    private lateinit var calendarService: CalendarService

    @Mock
    private lateinit var userContext: UserContext

    @Mock
    private lateinit var repo: CalendarRepository

    @Test
    fun testGetCalendarById_unauthenticated_no_calendar() {
        val id = UUID.randomUUID()
        whenever(userContext.getUser()).thenReturn(null)
        whenever(repo.findById(any())).thenReturn(null)

        val response = calendarService.getCalendarById(id)

        assertEquals(response.statusCode, HttpStatus.UNAUTHORIZED)
    }
}