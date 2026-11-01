package com.artsiushenka.luxmedscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luxmed.monitor")
public record MonitorProperties(
		String cron,
		long serviceVariantId,
		SearchPlace searchPlace) {

	public record SearchPlace(long id, String name) {
	}
}
