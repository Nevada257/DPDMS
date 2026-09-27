package zoonotic_disease_service.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import zoonotic_disease_service.audit.AuditTrail;
import zoonotic_disease_service.audit.AuditTrailRepository;
import zoonotic_disease_service.entity.ZoonoticIncident;
import zoonotic_disease_service.enums.IncidentStatus;
import zoonotic_disease_service.enums.Role;
import zoonotic_disease_service.repository.ZoonoticIncidentRepository;
import zoonotic_disease_service.user.User;
import zoonotic_disease_service.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZoonoticIncidentService {

    private final ZoonoticIncidentRepository repository;
    private final AuditTrailRepository auditTrailRepository;
    private final UserRepository userRepository;

    public ZoonoticIncidentService(
            ZoonoticIncidentRepository repository,
            AuditTrailRepository auditTrailRepository,
            UserRepository userRepository) {

        this.repository = repository;
        this.auditTrailRepository = auditTrailRepository;
        this.userRepository = userRepository;
    }

    // ==========================================================
    // CREATE
    // ==========================================================

    public ZoonoticIncident createIncident(
            ZoonoticIncident incident) {

        User currentUser = getCurrentUser();

        checkCreateScope(currentUser, incident);

        incident.setStatus(IncidentStatus.PENDING);

        // Remember who created the incident
        incident.setCreatedByUsername(
                currentUser.getUsername()
        );

        ZoonoticIncident savedIncident =
                repository.save(incident);

        saveAudit(
                savedIncident.getId(),
                "CREATE",
                null,
                IncidentStatus.PENDING.name(),
                currentUser.getUsername()
        );

        return savedIncident;
    }

    // ==========================================================
    // GET ALL
    // ==========================================================

    public List<ZoonoticIncident> getAllIncidents() {

        User currentUser = getCurrentUser();

        // ------------------------------------------------------
        // NATIONAL USER
        // Can see APPROVED incidents across all provinces.
        // Pending records are hidden.
        // ------------------------------------------------------

        if (currentUser.getRole() ==
                Role.NATIONAL_USER) {

            return repository.findAll()
                    .stream()
                    .filter(incident ->
                            incident.getStatus() ==
                                    IncidentStatus.APPROVED)
                    .toList();
        }

        // ------------------------------------------------------
        // PROVINCIAL SUPERVISOR
        // Can see incidents within their province,
        // including pending incidents.
        // ------------------------------------------------------

        if (currentUser.getRole() ==
                Role.PROVINCIAL_SUPERVISOR
                || currentUser.getRole() ==
                Role.PROVINCIAL_ADMIN) {

            return repository.findByProvince(
                    currentUser.getProvince()
            );
        }

        // ------------------------------------------------------
        // WARD RECORDER
        //
        // Can see:
        // 1. APPROVED incidents in their ward
        // 2. Their own non-approved incidents
        //
        // They cannot see another recorder's pending record.
        // ------------------------------------------------------

        if (currentUser.getRole() ==
                Role.WARD_RECORDER) {

            List<ZoonoticIncident> results =
                    new ArrayList<>();

            // Approved incidents in the ward
            results.addAll(
                    repository.findByWardAndStatus(
                            currentUser.getWard(),
                            IncidentStatus.APPROVED
                    )
            );

            // The recorder's own non-approved incidents
            results.addAll(
                    repository
                            .findByWardAndCreatedByUsernameAndStatusNot(
                                    currentUser.getWard(),
                                    currentUser.getUsername(),
                                    IncidentStatus.APPROVED
                            )
            );

            return results;
        }

        throw new SecurityException(
                "User role is not authorized"
        );
    }

    // ==========================================================
    // GET ONE
    // ==========================================================

    public ZoonoticIncident getIncidentById(
            Long id) {

        User currentUser = getCurrentUser();

        ZoonoticIncident incident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkReadScope(
                currentUser,
                incident
        );

        return incident;
    }

    // ==========================================================
    // UPDATE
    // ==========================================================

    public ZoonoticIncident updateIncident(
            Long id,
            ZoonoticIncident updatedIncident) {

        User currentUser = getCurrentUser();

        ZoonoticIncident existingIncident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkReadScope(
                currentUser,
                existingIncident
        );

        checkCreateScope(
                currentUser,
                updatedIncident
        );

        existingIncident.setWard(
                updatedIncident.getWard()
        );

        existingIncident.setDistrict(
                updatedIncident.getDistrict()
        );

        existingIncident.setProvince(
                updatedIncident.getProvince()
        );

        existingIncident.setOccurrenceDateTime(
                updatedIncident.getOccurrenceDateTime()
        );

        existingIncident.setReporter(
                updatedIncident.getReporter()
        );

        existingIncident.setSeverity(
                updatedIncident.getSeverity()
        );

        existingIncident.setLatitude(
                updatedIncident.getLatitude()
        );

        existingIncident.setLongitude(
                updatedIncident.getLongitude()
        );

        existingIncident.setDiseaseName(
                updatedIncident.getDiseaseName()
        );

        existingIncident.setAnimalSpecies(
                updatedIncident.getAnimalSpecies()
        );

        existingIncident.setConfirmedHumanCases(
                updatedIncident.getConfirmedHumanCases()
        );

        existingIncident.setConfirmedAnimalCases(
                updatedIncident.getConfirmedAnimalCases()
        );

        existingIncident.setEventClassification(
                updatedIncident.getEventClassification()
        );

        return repository.save(
                existingIncident
        );
    }

    // ==========================================================
    // DELETE
    // ==========================================================

    public void deleteIncident(Long id) {

        User currentUser = getCurrentUser();

        ZoonoticIncident incident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkReadScope(
                currentUser,
                incident
        );

        repository.delete(incident);
    }

    // ==========================================================
    // APPROVE
    // ==========================================================

    public ZoonoticIncident approveIncident(
            Long id) {

        User currentUser = getCurrentUser();

        checkSupervisor(currentUser);

        ZoonoticIncident incident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkProvinceScope(
                currentUser,
                incident
        );

        if (incident.getStatus() !=
                IncidentStatus.PENDING) {

            throw new IllegalStateException(
                    "Only PENDING incidents can be approved"
            );
        }

        IncidentStatus oldStatus =
                incident.getStatus();

        incident.setStatus(
                IncidentStatus.APPROVED
        );

        ZoonoticIncident savedIncident =
                repository.save(incident);

        saveAudit(
                id,
                "APPROVE",
                oldStatus.name(),
                IncidentStatus.APPROVED.name(),
                currentUser.getUsername()
        );

        return savedIncident;
    }

    // ==========================================================
    // REJECT
    // ==========================================================

    public ZoonoticIncident rejectIncident(
            Long id) {

        User currentUser = getCurrentUser();

        checkSupervisor(currentUser);

        ZoonoticIncident incident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkProvinceScope(
                currentUser,
                incident
        );

        if (incident.getStatus() !=
                IncidentStatus.PENDING) {

            throw new IllegalStateException(
                    "Only PENDING incidents can be rejected"
            );
        }

        IncidentStatus oldStatus =
                incident.getStatus();

        incident.setStatus(
                IncidentStatus.REJECTED
        );

        ZoonoticIncident savedIncident =
                repository.save(incident);

        saveAudit(
                id,
                "REJECT",
                oldStatus.name(),
                IncidentStatus.REJECTED.name(),
                currentUser.getUsername()
        );

        return savedIncident;
    }

    // ==========================================================
    // REQUEST CORRECTION
    // ==========================================================

    public ZoonoticIncident requestCorrection(
            Long id) {

        User currentUser = getCurrentUser();

        checkSupervisor(currentUser);

        ZoonoticIncident incident =
                repository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Zoonotic incident not found with ID: "
                                                + id
                                )
                        );

        checkProvinceScope(
                currentUser,
                incident
        );

        if (incident.getStatus() !=
                IncidentStatus.PENDING) {

            throw new IllegalStateException(
                    "Only PENDING incidents can require correction"
            );
        }

        IncidentStatus oldStatus =
                incident.getStatus();

        incident.setStatus(
                IncidentStatus.CORRECTION_REQUIRED
        );

        ZoonoticIncident savedIncident =
                repository.save(incident);

        saveAudit(
                id,
                "REQUEST_CORRECTION",
                oldStatus.name(),
                IncidentStatus.CORRECTION_REQUIRED.name(),
                currentUser.getUsername()
        );

        return savedIncident;
    }

    // ==========================================================
    // GET CURRENT USER
    // ==========================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new SecurityException(
                    "User is not authenticated"
            );
        }

        String username =
                authentication.getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new SecurityException(
                                "Authenticated user not found"
                        )
                );
    }

    // ==========================================================
    // CREATE SCOPE
    // ==========================================================

    private void checkCreateScope(
            User user,
            ZoonoticIncident incident) {

        // National users and the provincial administrator are read-only
        if (user.getRole() ==
                Role.NATIONAL_USER
                || user.getRole() ==
                Role.PROVINCIAL_ADMIN) {

            throw new SecurityException(
                    user.getRole() + " users are read-only"
            );
        }

        // Ward recorder
        if (user.getRole() ==
                Role.WARD_RECORDER) {

            if (user.getWard() == null ||
                    incident.getWard() == null ||
                    !user.getWard().equalsIgnoreCase(
                            incident.getWard()
                    )) {

                throw new SecurityException(
                        "Ward recorder can only work with incidents in "
                                + user.getWard()
                );
            }

            if (incident.getProvince() != null &&
                    user.getProvince() != null &&
                    !user.getProvince().equalsIgnoreCase(
                            incident.getProvince()
                    )) {

                throw new SecurityException(
                        "Incident province does not match recorder province"
                );
            }
        }

        // Provincial supervisor
        if (user.getRole() ==
                Role.PROVINCIAL_SUPERVISOR) {

            if (incident.getProvince() == null ||
                    user.getProvince() == null ||
                    !user.getProvince().equalsIgnoreCase(
                            incident.getProvince()
                    )) {

                throw new SecurityException(
                        "Provincial supervisor can only work within "
                                + user.getProvince()
                );
            }
        }
    }

    // ==========================================================
    // READ SCOPE
    // ==========================================================

    private void checkReadScope(
            User user,
            ZoonoticIncident incident) {

        // ------------------------------------------------------
        // NATIONAL
        // Approved only
        // ------------------------------------------------------

        if (user.getRole() ==
                Role.NATIONAL_USER) {

            if (incident.getStatus() !=
                    IncidentStatus.APPROVED) {

                throw new SecurityException(
                        "National users can only view approved incidents"
                );
            }

            return;
        }

        // ------------------------------------------------------
        // WARD RECORDER
        // ------------------------------------------------------

        if (user.getRole() ==
                Role.WARD_RECORDER) {

            if (user.getWard() == null ||
                    incident.getWard() == null ||
                    !user.getWard().equalsIgnoreCase(
                            incident.getWard()
                    )) {

                throw new SecurityException(
                        "You are not authorized to access this ward"
                );
            }

            // Approved incidents in their ward are visible
            if (incident.getStatus() ==
                    IncidentStatus.APPROVED) {

                return;
            }

            // Non-approved records may only be seen
            // by the recorder who created them
            if (incident.getCreatedByUsername() == null ||
                    !user.getUsername().equalsIgnoreCase(
                            incident.getCreatedByUsername()
                    )) {

                throw new SecurityException(
                        "You are not authorized to access this pending incident"
                );
            }

            return;
        }

        // ------------------------------------------------------
        // PROVINCIAL SUPERVISOR
        // ------------------------------------------------------

        if (user.getRole() ==
                Role.PROVINCIAL_SUPERVISOR
                || user.getRole() ==
                Role.PROVINCIAL_ADMIN) {

            checkProvinceScope(
                    user,
                    incident
            );

            return;
        }

        throw new SecurityException(
                "User role is not authorized"
        );
    }

    // ==========================================================
    // PROVINCE SCOPE
    // ==========================================================

    private void checkProvinceScope(
            User user,
            ZoonoticIncident incident) {

        if (user.getProvince() == null ||
                incident.getProvince() == null ||
                !user.getProvince().equalsIgnoreCase(
                        incident.getProvince()
                )) {

            throw new SecurityException(
                    "You are not authorized to access this province"
            );
        }
    }

    // ==========================================================
    // SUPERVISOR CHECK
    // ==========================================================

    private void checkSupervisor(User user) {

        if (user.getRole() !=
                Role.PROVINCIAL_SUPERVISOR) {

            throw new SecurityException(
                    "Only provincial supervisors can perform this action"
            );
        }
    }

    // ==========================================================
    // AUDIT
    // ==========================================================

    private void saveAudit(
            Long incidentId,
            String action,
            String previousStatus,
            String newStatus,
            String performedBy) {

        AuditTrail auditTrail =
                new AuditTrail(
                        incidentId,
                        action,
                        previousStatus,
                        newStatus,
                        performedBy,
                        LocalDateTime.now()
                );

        auditTrailRepository.save(
                auditTrail
        );
    }
}