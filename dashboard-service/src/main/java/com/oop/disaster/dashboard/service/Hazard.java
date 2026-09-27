package com.oop.disaster.dashboard.service;

/** The five hazard services: Eureka service id and the endpoint listing approved incidents. */
public enum Hazard {
    FLOOD("flood-service", "/api/floods/approved"),
    DROUGHT("drought-service", "/api/drought/incidents/approved"),
    FIRE("fire-service", "/api/fire-incidents/approved"),
    // The zoonotic list endpoint applies its own visibility rules; the dashboard
    // additionally keeps only APPROVED records.
    ZOONOTIC("zoonotic-disease-service", "/api/zoonotic-incidents"),
    MINING("mining-accident-service", "/api/mining-accidents/approved");

    private final String serviceId;
    private final String approvedPath;

    Hazard(String serviceId, String approvedPath) {
        this.serviceId = serviceId;
        this.approvedPath = approvedPath;
    }

    public String serviceId() { return serviceId; }
    public String approvedPath() { return approvedPath; }
}
