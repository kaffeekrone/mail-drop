package de.kaffeekrone.maildrop.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

public class CallbackDto {
    @Getter
    @Setter
    private boolean success;
    @Getter
    @Setter
    private ZonedDateTime sentDate;

}