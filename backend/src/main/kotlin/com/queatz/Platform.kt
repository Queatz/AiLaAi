package com.queatz

import com.queatz.db.PlatformConfig
import com.queatz.db.platformConfig
import com.queatz.plugins.db


class Platform {
    val config get() = db.platformConfig ?: db.insert(PlatformConfig(hosts = emptyList(), inviteOnly = false))
}
