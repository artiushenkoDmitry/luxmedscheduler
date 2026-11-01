package com.artsiushenka.luxmedscheduler.model;

import java.time.LocalDateTime;

public record AppointmentSlot(
		LocalDateTime dateTimeFrom,
		LocalDateTime dateTimeTo,
		long doctorId,
		String doctorName,
		long clinicId,
		String clinic,
		long roomId,
		long serviceId,
		long scheduleId,
		boolean telemedicine) {
}
