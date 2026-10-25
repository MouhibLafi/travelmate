package com.travelmate.accommodation.service;

import com.travelmate.accommodation.dto.StayCostRequest;
import com.travelmate.accommodation.dto.StayCostResponse;

public interface StayCostService {

    StayCostResponse calculate(Long accommodationId, StayCostRequest request);
}
