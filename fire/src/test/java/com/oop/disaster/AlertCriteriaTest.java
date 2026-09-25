package com.oop.disaster;

import com.oop.disaster.model.FireIncident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertCriteriaTest {

    @Test
    void activeFireIsAlertCandidate() {
        FireIncident incident = new FireIncident();
        incident.setActive(true);

        assertTrue(incident.isActive());
    }

    @Test
    void containedFireIsNotActive() {
        FireIncident incident = new FireIncident();
        incident.setActive(false);

        assertFalse(incident.isActive());
    }
}
