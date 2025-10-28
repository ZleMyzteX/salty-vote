package er.codes.saltyvote.scrape

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.Path

@Component
@ConfigurationProperties(prefix = "scrape.storage")
class ScrapeStorageProperties {
    /**
     * Directory where scraped images and files will be stored.
     * Default: "uploads/"
     */
    var downloadFolder: String = "airbnbPics/"

    fun getDownloadPath(): Path = Path(downloadFolder)
}
