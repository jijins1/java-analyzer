package com.ruokki.parser.log

import mu.KLogger
import mu.KotlinLogging

interface Loggable {
    public fun logger(): KLogger {
        return KotlinLogging.logger(this.javaClass.name)
    }
}