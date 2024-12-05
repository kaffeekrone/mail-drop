package de.kaffeekrone.maildrop.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Setter
@Getter
public class Callback {
    private boolean success;
    private ZonedDateTime sentDate;

    private String id;

}