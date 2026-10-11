package me.polynom.polycloud.apps.webdav.config

import me.polynom.polycloud.apps.webdav.api.WebDavConstants
import me.polynom.polycloud.apps.webdav.api.WebDavServlet
import me.polynom.polycloud.apps.webdav.autoconfigure.PluginEnabled
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@PluginEnabled
@Configuration
open class WebDavServletConfig {
    @Bean
    open fun webDavServletRegistration(servlet: WebDavServlet): ServletRegistrationBean<WebDavServlet> =
        ServletRegistrationBean(servlet, "${WebDavConstants.MOUNT}/*").apply {
            setName("webDavServlet")
        }
}
