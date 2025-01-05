package com.queatz.ailaai.db

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique

@Entity
data class CacheDbModel(
    @Id var id: Long = 0,
    @Index @Unique(onConflict = ConflictStrategy.REPLACE) var key: String = "",
    var value: String = ""
)
