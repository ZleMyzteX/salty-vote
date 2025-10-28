package er.codes.saltyvote.scrape

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
@EnableScheduling
class ScrapeAsyncConfig {
    @Bean("scrapeTaskExecutor")
    fun scrapeTaskExecutor(): Executor {
        val executor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = 4
                maxPoolSize = 10
                queueCapacity = 500
                initialize()
            }
        return executor
    }
}
