import me.polynom.polycloud.apps.calendar.mappers.CalendarMapper
import me.polynom.polycloud.apps.calendar.persistence.repository.CalendarRepository
import me.polynom.polycloud.apps.calendar.service.CalendarService
import me.polynom.polycloud.plugin.auth.UserContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CalendarServiceTests {
    @Mock
    private lateinit var userContext: UserContext

    @Mock
    private lateinit var repo: CalendarRepository

    @Mock
    private lateinit var mapper: CalendarMapper

    private val calendarService: CalendarService
        get() = CalendarService(repo, userContext, mapper)

    @Test
    fun testGetCalendarById_unauthenticated_no_calendar() {
        val id = UUID.randomUUID()
        whenever(userContext.getUser()).thenReturn(null)
        whenever(repo.findById(id)).thenReturn(Optional.empty())

        val response = calendarService.getCalendarById(id)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }
}