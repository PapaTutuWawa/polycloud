package me.polynom.polycloud.apps.webdav.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.polynom.polycloud.apps.webdav.lock.IfCheckResult
import me.polynom.polycloud.apps.webdav.lock.LockManager
import me.polynom.polycloud.apps.webdav.store.DavCollection
import me.polynom.polycloud.apps.webdav.store.DavNode
import me.polynom.polycloud.apps.webdav.store.DavPath
import org.springframework.http.HttpStatus
import org.xml.sax.SAXException

/** Reads the full request body as a UTF-8 string. */
fun readBody(request: HttpServletRequest): String = request.inputStream.readBytes().decodeToString()

/** Runs [block] (an XML body parse), turning a malformed-XML failure into a 400 instead of a 500. */
fun <T> parseXmlBody(block: () -> T): T =
    try {
        block()
    } catch (e: SAXException) {
        throw DavException(HttpStatus.BAD_REQUEST.value(), "Malformed XML request body: ${e.message}", e)
    }

/** Sets [status] and, if [message] is given, writes it as a plain-text body. */
fun fail(
    response: HttpServletResponse,
    status: Int,
    message: String? = null,
) {
    response.status = status
    if (message != null) {
        response.contentType = "text/plain; charset=UTF-8"
        response.writer.write(message)
    }
}

/** Sets [status] and writes [xml] as an `application/xml` body. */
fun writeXml(
    response: HttpServletResponse,
    status: Int,
    xml: String,
) {
    response.status = status
    response.contentType = "application/xml; charset=UTF-8"
    response.writer.write(xml)
}

/**
 * Full `If` header precondition check (RFC 4918 section 10.4) for a state-changing request
 * against [path], given its [currentEtag] (null if it doesn't exist yet, e.g. a PUT of a new
 * file). Throws the corresponding [DavException] (423/412) if the precondition fails.
 */
fun checkPrecondition(
    lockManager: LockManager,
    request: HttpServletRequest,
    path: String,
    currentEtag: String?,
) {
    when (lockManager.checkIfHeader(path, request.getHeader(DavHeaders.IF), currentEtag)) {
        IfCheckResult.LOCKED -> throw DavException(HttpStatus.LOCKED.value(), "Resource is locked")
        IfCheckResult.PRECONDITION_FAILED -> throw DavException(HttpStatus.PRECONDITION_FAILED.value())
        IfCheckResult.SATISFIED -> Unit
    }
}

/** The `D:href` for [path], with a trailing slash when [node] is a collection. */
fun hrefFor(
    path: String,
    node: DavNode,
): String {
    val base = if (path == DavPath.ROOT) "${WebDavConstants.MOUNT}/" else "${WebDavConstants.MOUNT}$path"
    return if (node is DavCollection && !base.endsWith("/")) "$base/" else base
}
