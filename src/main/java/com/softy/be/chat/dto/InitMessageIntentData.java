package com.softy.be.chat.dto;

public record InitMessageIntentData(
        String intentLabel,
        boolean isInWorkingHours
) {
}
