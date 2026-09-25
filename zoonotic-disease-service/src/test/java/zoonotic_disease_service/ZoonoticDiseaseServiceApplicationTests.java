package zoonotic_disease_service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import zoonotic_disease_service.audit.AuditTrailRepository;
import zoonotic_disease_service.entity.ZoonoticIncident;
import zoonotic_disease_service.enums.EventClassification;
import zoonotic_disease_service.enums.IncidentStatus;
import zoonotic_disease_service.enums.Role;
import zoonotic_disease_service.enums.Severity;
import zoonotic_disease_service.repository.ZoonoticIncidentRepository;
import zoonotic_disease_service.service.ZoonoticIncidentService;
import zoonotic_disease_service.user.User;
import zoonotic_disease_service.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZoonoticIncidentServiceTest {

	@Mock
	private ZoonoticIncidentRepository repository;

	@Mock
	private AuditTrailRepository auditTrailRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ZoonoticIncidentService service;

	private User recorder;
	private User supervisor;
	private User nationalUser;

	@BeforeEach
	void setUp() {

		recorder = new User(
				"recorder1",
				"password123",
				Role.WARD_RECORDER,
				"Ward 5",
				"Harare"
		);

		supervisor = new User(
				"supervisor1",
				"password123",
				Role.PROVINCIAL_SUPERVISOR,
				null,
				"Harare"
		);

		nationalUser = new User(
				"national1",
				"password123",
				Role.NATIONAL_USER,
				null,
				null
		);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void authenticate(String username) {

		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
						username,
						null,
						Collections.singleton(
								new SimpleGrantedAuthority("ROLE_USER")
						)
				);

		SecurityContextHolder.getContext()
				.setAuthentication(authentication);
	}

	private ZoonoticIncident createTestIncident(
			String ward,
			String province,
			IncidentStatus status) {

		ZoonoticIncident incident = new ZoonoticIncident();

		incident.setWard(ward);
		incident.setDistrict("Harare");
		incident.setProvince(province);
		incident.setOccurrenceDateTime(LocalDateTime.now());
		incident.setReporter("Test Reporter");
		incident.setSeverity(Severity.MEDIUM);
		incident.setStatus(status);
		incident.setLatitude(-17.8252);
		incident.setLongitude(31.0335);
		incident.setDiseaseName("Anthrax");
		incident.setAnimalSpecies("Cattle");
		incident.setConfirmedHumanCases(2);
		incident.setConfirmedAnimalCases(5);
		incident.setEventClassification(EventClassification.CLUSTER);

		return incident;
	}

	@Test
	void wardRecorderCanCreateIncidentInOwnWard() {

		authenticate("recorder1");

		doReturn(Optional.of(recorder))
				.when(userRepository)
				.findByUsername("recorder1");

		doAnswer(invocation ->
				invocation.getArgument(0))
				.when(repository)
				.save(any(ZoonoticIncident.class));

		ZoonoticIncident incident = createTestIncident(
				"Ward 5",
				"Harare",
				null
		);

		ZoonoticIncident result =
				service.createIncident(incident);

		assertNotNull(result);

		assertEquals(
				IncidentStatus.PENDING,
				result.getStatus()
		);

		assertEquals(
				"recorder1",
				result.getCreatedByUsername()
		);

		verify(repository)
				.save(any(ZoonoticIncident.class));

		verify(auditTrailRepository)
				.save(any());
	}

	@Test
	void wardRecorderCannotCreateIncidentInAnotherWard() {

		authenticate("recorder1");

		doReturn(Optional.of(recorder))
				.when(userRepository)
				.findByUsername("recorder1");

		ZoonoticIncident incident = createTestIncident(
				"Ward 10",
				"Harare",
				null
		);

		assertThrows(
				SecurityException.class,
				() -> service.createIncident(incident)
		);

		verify(repository, never())
				.save(any(ZoonoticIncident.class));
	}

	@Test
	void nationalUserCannotCreateIncident() {

		authenticate("national1");

		doReturn(Optional.of(nationalUser))
				.when(userRepository)
				.findByUsername("national1");

		ZoonoticIncident incident = createTestIncident(
				"Ward 5",
				"Harare",
				null
		);

		assertThrows(
				SecurityException.class,
				() -> service.createIncident(incident)
		);

		verify(repository, never())
				.save(any(ZoonoticIncident.class));
	}

	@Test
	void supervisorCanApprovePendingIncident() {

		authenticate("supervisor1");

		doReturn(Optional.of(supervisor))
				.when(userRepository)
				.findByUsername("supervisor1");

		ZoonoticIncident incident = createTestIncident(
				"Ward 5",
				"Harare",
				IncidentStatus.PENDING
		);

		doReturn(Optional.of(incident))
				.when(repository)
				.findById(1L);

		doAnswer(invocation ->
				invocation.getArgument(0))
				.when(repository)
				.save(any(ZoonoticIncident.class));

		ZoonoticIncident result =
				service.approveIncident(1L);

		assertNotNull(result);

		assertEquals(
				IncidentStatus.APPROVED,
				result.getStatus()
		);

		verify(repository)
				.save(any(ZoonoticIncident.class));

		verify(auditTrailRepository)
				.save(any());
	}

	@Test
	void supervisorCannotApproveAlreadyApprovedIncident() {

		authenticate("supervisor1");

		doReturn(Optional.of(supervisor))
				.when(userRepository)
				.findByUsername("supervisor1");

		ZoonoticIncident incident = createTestIncident(
				"Ward 5",
				"Harare",
				IncidentStatus.APPROVED
		);

		doReturn(Optional.of(incident))
				.when(repository)
				.findById(1L);

		assertThrows(
				IllegalStateException.class,
				() -> service.approveIncident(1L)
		);

		verify(repository, never())
				.save(any(ZoonoticIncident.class));
	}

	@Test
	void supervisorCannotApproveIncidentFromAnotherProvince() {

		authenticate("supervisor1");

		doReturn(Optional.of(supervisor))
				.when(userRepository)
				.findByUsername("supervisor1");

		ZoonoticIncident incident = createTestIncident(
				"Ward 5",
				"Bulawayo",
				IncidentStatus.PENDING
		);

		doReturn(Optional.of(incident))
				.when(repository)
				.findById(1L);

		assertThrows(
				SecurityException.class,
				() -> service.approveIncident(1L)
		);

		verify(repository, never())
				.save(any(ZoonoticIncident.class));
	}
}