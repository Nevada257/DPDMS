package com.oop.disaster.alert.security;

/**
 * The authenticated caller, built from the signed JWT issued by auth-service.
 *
 * @param username    subject of the token
 * @param role        RECORDER, SUPERVISOR, ADMIN or NATIONAL
 * @param hazardScope FLOOD, DROUGHT, FIRE, ZOONOTIC, MINING, or ALL (admin / national)
 * @param ward        ward of a recorder, otherwise null
 */
public record AuthUser(String username, String role, String hazardScope, String ward) {

    public boolean hasRole(String r) {
        return r.equalsIgnoreCase(role);
    }

    /** True if this user may act on / see data for the given hazard. */
    public boolean coversHazard(String hazard) {
        return "ALL".equalsIgnoreCase(hazardScope) || (hazard != null && hazard.equalsIgnoreCase(hazardScope));
    }
}
