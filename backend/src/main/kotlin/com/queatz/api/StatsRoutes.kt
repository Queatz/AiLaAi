package com.queatz.api

import com.queatz.db.AppStats
import com.queatz.db.StatsHealth
import com.queatz.db.activePeople
import com.queatz.db.newPeople
import com.queatz.db.recentFeedback
import com.queatz.db.totalClosedGroups
import com.queatz.db.totalDraftCards
import com.queatz.db.totalDraftStories
import com.queatz.db.totalItems
import com.queatz.db.totalOpenGroups
import com.queatz.db.totalPeople
import com.queatz.db.totalPublishedCards
import com.queatz.db.totalPublishedStories
import com.queatz.db.totalReminders
import com.queatz.plugins.db
import com.queatz.plugins.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.statsRoutes() {
    get("/stats") {
        respond {
            AppStats(
                activePeople30Days = db.activePeople(days = 30),
                activePeople7Days = db.activePeople(days = 7),
                activePeople24Hours = db.activePeople(days = 1),
                newPeople30Days = db.newPeople(days = 30),
                newPeople7Days = db.newPeople(days = 7),
                newPeople24Hours = db.newPeople(days = 1),
                totalPeople = db.totalPeople,
                totalDraftCards = db.totalDraftCards,
                totalPublishedCards = db.totalPublishedCards,
                totalDraftStories = db.totalDraftStories,
                totalPublishedStories = db.totalPublishedStories,
                totalClosedGroups = db.totalClosedGroups,
                totalOpenGroups = db.totalOpenGroups,
                totalReminders = db.totalReminders,
                totalItems = db.totalItems,
            )
        }
    }

    get("/stats/feedback") {
        respond {
            db.recentFeedback(call.parameters["limit"]?.toInt() ?: 20)
        }
    }

    get("/stats/health") {
        respond {
            StatsHealth(
                diskUsagePercent = getFreeSpacePercentage()
            )
        }
    }
}

private fun getFreeSpacePercentage(): Double = File("/").run {
    freeSpace.toDouble() / totalSpace.toDouble() * 100.0
}
