package com.watxaut.myjumpapp.domain.jump

enum class SurfaceType(
    val displayName: String,
    val description: String,
    val icon: String
) {
    HARD_FLOOR(
        displayName = "Hard Floor",
        description = "Indoor courts, gymnasium floors, concrete",
        icon = "🏀"
    ),
    SAND(
        displayName = "Sand",
        description = "Beach volleyball courts, sand surfaces",
        icon = "🏖️"
    );

    companion object {
        fun fromString(value: String): SurfaceType {
            return values().find { it.name == value } ?: HARD_FLOOR
        }
    }
}