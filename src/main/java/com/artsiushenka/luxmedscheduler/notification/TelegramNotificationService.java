package com.artsiushenka.luxmedscheduler.notification;

import com.artsiushenka.luxmedscheduler.config.TelegramProperties;
import com.artsiushenka.luxmedscheduler.model.AppointmentSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("!'${telegram.bot-token:}'.isEmpty()")
public class TelegramNotificationService implements NotificationService {

	private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
	private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";
	private static final int MAX_LINES = 30;
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm");

	private final TelegramProperties properties;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	public TelegramNotificationService(TelegramProperties properties) {
		this.properties = properties;
	}

	@Override
	public void notifySlots(List<AppointmentSlot> slots) {
		try {
			String body = jsonMapper.writeValueAsString(
					Map.of("chat_id", properties.chatId(), "text", buildMessage(slots)));
			HttpResponse<String> response = httpClient.send(
					HttpRequest.newBuilder(URI.create(API_URL.formatted(properties.botToken())))
							.timeout(Duration.ofSeconds(15))
							.header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(body))
							.build(),
					HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				log.info("Telegram notification sent");
			} else {
				log.error("Telegram notification failed: HTTP {} {}", response.statusCode(), response.body());
			}
		} catch (IOException e) {
			log.error("Telegram notification failed: {}", e.getClass().getSimpleName());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static String buildMessage(List<AppointmentSlot> slots) {
		StringBuilder text = new StringBuilder("LuxMed: available slots: ").append(slots.size());
		slots.stream()
				.sorted(Comparator.comparing(AppointmentSlot::dateTimeFrom))
				.limit(MAX_LINES)
				.forEach(slot -> text.append('\n')
						.append(FORMAT.format(slot.dateTimeFrom())).append(' ')
						.append(slot.doctorName()).append(" - ")
						.append(slot.clinic()));
		if (slots.size() > MAX_LINES) {
			text.append("\n... and ").append(slots.size() - MAX_LINES).append(" more");
		}
		return text.toString();
	}
}
