package com.artsiushenka.luxmedscheduler.notification;

import com.artsiushenka.luxmedscheduler.model.AppointmentSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogNotificationService implements NotificationService {

	private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);
	private static final int MAX_LOGGED = 20;

	@Override
	public void notifySlots(List<AppointmentSlot> slots) {
		log.info("{} slots available", slots.size());
		slots.stream().limit(MAX_LOGGED).forEach(slot -> log.info("{} {} {} scheduleId={}",
				slot.dateTimeFrom(), slot.doctorName(), slot.clinic(), slot.scheduleId()));
	}
}
