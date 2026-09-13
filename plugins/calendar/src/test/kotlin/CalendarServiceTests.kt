import me.polynom.polycloud.apps.calendar.mappers.CalendarMapper
import me.polynom.polycloud.apps.calendar.mappers.EventMapper
import me.polynom.polycloud.apps.calendar.persistence.entities.Calendar
import me.polynom.polycloud.apps.calendar.persistence.repository.CalendarRepository
import me.polynom.polycloud.apps.calendar.persistence.repository.EventRepository
import me.polynom.polycloud.apps.calendar.service.CalendarService
import me.polynom.polycloud.plugin.auth.UserContext
import me.polynom.polycloud.plugin.auth.dto.AuthVerificationResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CalendarServiceTests {
    @Mock
    private lateinit var userContext: UserContext

    @Mock
    private lateinit var calendarRepo: CalendarRepository

    @Mock
    private lateinit var eventRepo: EventRepository

    @Mock
    private lateinit var calendarMapper: CalendarMapper

    @Mock
    private lateinit var eventMapper: EventMapper

    @Mock
    private lateinit var transactionTemplate: TransactionTemplate

    private val calendarService: CalendarService
        get() = CalendarService(
            calendarRepo,
            eventRepo,
            userContext,
            calendarMapper,
            eventMapper,
            transactionTemplate)

    @Test
    fun test_getCalenderByIdWithAccessCheck_unauthenticated_calendarExists_nonPublic() {
        val id = UUID.randomUUID()
        val calendar = Calendar(public = false)
        whenever(userContext.getUser()).thenReturn(null)
        whenever(calendarRepo.findById(id)).thenReturn(Optional.of(calendar))

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertNull(response.first)
        assertEquals(403, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_unauthenticated_calendarExists_public() {
        val id = UUID.randomUUID()
        val calendar = Calendar(public = true)
        whenever(userContext.getUser()).thenReturn(null)
        whenever(calendarRepo.findById(id)).thenReturn(Optional.of(calendar))

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertEquals(calendar, response.first)
        assertEquals(200, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_unauthenticated_calendarDoesNotExist() {
        val id = UUID.randomUUID()
        whenever(userContext.getUser()).thenReturn(null)
        whenever(calendarRepo.findById(id)).thenReturn(Optional.empty())

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertNull(response.first)
        assertEquals(403, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_authenticated_calendarExists_owner() {
        val id = UUID.randomUUID()
        val username = "user"
        val calendar = Calendar(owner = username, public = false)
        whenever(userContext.getUser()).thenReturn(
            AuthVerificationResult(username, emptyList()),
        )
        whenever(calendarRepo.findById(id)).thenReturn(Optional.of(calendar))

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertEquals(calendar, response.first)
        assertEquals(200, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_authenticated_calendarExists_notOwner_public() {
        val id = UUID.randomUUID()
        val username = "user"
        val calendar = Calendar(owner = "someone-else", public = true)
        whenever(userContext.getUser()).thenReturn(
            AuthVerificationResult(username, emptyList()),
        )
        whenever(calendarRepo.findById(id)).thenReturn(Optional.of(calendar))

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertEquals(calendar, response.first)
        assertEquals(200, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_authenticated_calendarExists_notOwner_notPublic() {
        val id = UUID.randomUUID()
        val username = "user"
        val calendar = Calendar(owner = "someone-else", public = false)
        whenever(userContext.getUser()).thenReturn(
            AuthVerificationResult(username, emptyList()),
        )
        whenever(calendarRepo.findById(id)).thenReturn(Optional.of(calendar))

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertNull(response.first)
        assertEquals(404, response.second)
    }

    @Test
    fun test_getCalenderByIdWithAccessCheck_authenticated_calendarExists_doesNotExist() {
        val id = UUID.randomUUID()
        val username = "user"
        whenever(userContext.getUser()).thenReturn(
            AuthVerificationResult(username, emptyList()),
        )
        whenever(calendarRepo.findById(id)).thenReturn(Optional.empty())

        val response = calendarService.getCalendarByIdWithAccessCheck(id)
        assertNull(response.first)
        assertEquals(404, response.second)
    }
}