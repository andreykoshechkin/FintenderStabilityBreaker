package com.example.fintenderstabilitybreaker.limiting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DepositDtoBuilder {

    public static List<DepositDto> getDepositDto() {

        List<DepositDto> data = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            data.add(new DepositDto(UUID.randomUUID()));
        }

        return data;

    }
}
