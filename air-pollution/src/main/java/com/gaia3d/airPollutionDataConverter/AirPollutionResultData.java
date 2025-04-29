package com.gaia3d.airPollutionDataConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AirPollutionResultData {
    @JsonProperty("DATE")
    private String date;
    @JsonProperty("MAXIMUM_VALUE")
    private double maximumValue;
    @JsonProperty("X")
    private double xPosition;
    @JsonProperty("Y")
    private double yPosition;
}
