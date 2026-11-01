package com.artsiushenka.luxmedscheduler.scheduler;

import com.artsiushenka.luxmedscheduler.service.AppointmentMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "luxmed.run-on-startup", matchIfMissing = true)
public class AppointmentScheduler {

	private static final Logger log = LoggerFactory.getLogger(AppointmentScheduler.class);

	private final AppointmentMonitor monitor;

	public AppointmentScheduler(AppointmentMonitor monitor) {
		this.monitor = monitor;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		run();
	}

	@Scheduled(cron = "${luxmed.monitor.cron}")
	public void run() {
		try {
			monitor.check();
		} catch (RuntimeException e) {
			log.error("Appointment check failed", e);
		}
	}
}
