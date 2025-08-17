package simple.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["simple.api", "traditional", "safe"])
class SimplePaymentApi

fun main(args: Array<String>) {
    runApplication<SimplePaymentApi>(*args)
}
