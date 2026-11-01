package com.artsiushenka.luxmedscheduler.service;

import com.artsiushenka.luxmedscheduler.client.LuxMedClient;
import com.artsiushenka.luxmedscheduler.model.AppointmentSlot;
import com.artsiushenka.luxmedscheduler.model.LuxmedTermsResponse;
import com.artsiushenka.luxmedscheduler.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AppointmentMonitor {

	private static final Logger log = LoggerFactory.getLogger(AppointmentMonitor.class);

	private final LuxMedClient client;
	private final NotificationService notificationService;

	public AppointmentMonitor(LuxMedClient client, NotificationService notificationService) {
		this.client = client;
		this.notificationService = notificationService;
	}

	public synchronized void check() {
		List<AppointmentSlot> slots = toSlots(search());
		log.info("Found {} slots", slots.size());
		if (!slots.isEmpty()) {
			notificationService.notifySlots(slots);
		}
	}

	private LuxmedTermsResponse search() {
		if (client.getToken() == null) {
			client.login();
		}
		try {
			return client.searchSlots();
		} catch (IllegalStateException e) {
			log.warn("Search failed ({}), re-logging in and retrying", e.getMessage());
			client.login();
			return client.searchSlots();
		}
	}

	private static List<AppointmentSlot> toSlots(LuxmedTermsResponse response) {
		if (response == null || response.getTermsForService() == null
				|| response.getTermsForService().getTermsForDays() == null) {
			return List.of();
		}
		return response.getTermsForService().getTermsForDays().stream()
				.filter(day -> day.getTerms() != null)
				.flatMap(day -> day.getTerms().stream())
				.map(term -> {
					LuxmedTermsResponse.Doctor doctor = term.getDoctor();
					String doctorName = doctor == null ? null : String.join(" ",
							Objects.toString(doctor.getAcademicTitle(), ""),
							Objects.toString(doctor.getFirstName(), ""),
							Objects.toString(doctor.getLastName(), "")).trim();
					return new AppointmentSlot(
							term.getDateTimeFrom(),
							term.getDateTimeTo(),
							doctor == null || doctor.getId() == null ? 0 : doctor.getId(),
							doctorName,
							term.getClinicId() == null ? 0 : term.getClinicId(),
							term.getClinic(),
							term.getRoomId() == null ? 0 : term.getRoomId(),
							term.getServiceId() == null ? 0 : term.getServiceId(),
							term.getScheduleId() == null ? 0 : term.getScheduleId(),
							Boolean.TRUE.equals(term.getIsTelemedicine()));
				})
				.toList();
	}
}
