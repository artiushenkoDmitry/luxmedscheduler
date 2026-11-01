package com.artsiushenka.luxmedscheduler.client;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "luxmed.run-on-startup", matchIfMissing = true)
@Component
public class LoginOnStartup implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(LoginOnStartup.class);

	private final LuxMedClient client;

	public LoginOnStartup(LuxMedClient client) {
		this.client = client;
	}

	@Override
	public void run(ApplicationArguments args) {
		client.login();
		var terms = client.searchSlots();
		var days = terms.getTermsForService().getTermsForDays();
		int total = days.stream().mapToInt(day -> day.getTerms().size()).sum();
		log.info("Found {} terms in {} days", total, days.size());
		days.stream().flatMap(day -> day.getTerms().stream()).limit(10)
				.forEach(term -> log.info("{} {} {} {}", term.getDateTimeFrom(), term.getDoctor().getLastName(),
						term.getClinic(), term.getScheduleId()));
	}
}
