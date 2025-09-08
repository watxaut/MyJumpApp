package com.watxaut.myjumpapp.domain.jump

enum class JumpType(
    val displayName: String,
    val description: String,
    val icon: String
) {
    STATIC(
        displayName = "Static Jump",
        description = "Jump from a stationary position without approach",
        icon = "⬆️"
    ),
    DYNAMIC(
        displayName = "Dynamic Approach",
        description = "Approach from the side and jump with momentum",
        icon = "🏃‍♂️"
    );

    companion object {
        fun fromString(value: String): JumpType {
            return values().find { it.name == value } ?: STATIC
        }
    }
}