package com.artsiushenka.luxmedscheduler.client;

import com.artsiushenka.luxmedscheduler.config.LuxMedAuthProperties;
import com.artsiushenka.luxmedscheduler.config.MonitorProperties;
import com.artsiushenka.luxmedscheduler.model.LuxmedTermsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class LuxMedClient {

	private static final Logger log = LoggerFactory.getLogger(LuxMedClient.class);
	private static final String USER_AGENT =
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:159.0) Gecko/20100101 Firefox/159.0";
	private static final String LOGIN_PAGE = "/PatientPortal/NewPortal/Page/Account/Login?returnUrl=%2FPage%2FDashboard";
	private static final String LOGIN_API = "/PatientPortal/Account/LogIn";

	private static final String MAX_SEARCH_DAYS_API = "/PatientPortal/NewPortal/terms/GetMaxSearchDays";
	private static final String TERMS_API = "/PatientPortal/NewPortal/terms/index";

	private final LuxMedAuthProperties properties;
	private final MonitorProperties monitorProperties;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
	private final HttpClient httpClient = HttpClient.newBuilder()
			.cookieHandler(cookieManager)
			.connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private volatile String token;

	public LuxMedClient(LuxMedAuthProperties properties, MonitorProperties monitorProperties) {
		this.properties = properties;
		this.monitorProperties = monitorProperties;
	}

	public LuxmedTermsResponse searchSlots() {
		MonitorProperties.SearchPlace place = monitorProperties.searchPlace();
		LocalDate dateFrom = LocalDate.now();
		LocalDate dateTo = dateFrom.plusDays(getMaxSearchDays());
		String query = "searchPlace.id=" + place.id()
				+ "&searchPlace.name=" + encode(place.name())
				+ "&searchPlace.type=0"
				+ "&serviceVariantId=" + monitorProperties.serviceVariantId()
				+ "&languageId=10"
				+ "&searchDateFrom=" + dateFrom
				+ "&searchDateTo=" + dateTo
				+ "&searchDatePreset=" + 3
				+ "&processId=" + UUID.randomUUID().toString()
				+ "&nextSearch=" + false
				+ "&searchByMedicalSpecialist=false"
				+ "&serviceVariantSource=" + 2
				+ "&locationReplaced=" + false
				+ "&delocalized=false";
		try {
			HttpResponse<String> response = send(HttpRequest.newBuilder(uri(TERMS_API + "?" + query))
					.header("User-Agent", USER_AGENT)
					.header("Accept", "application/json, text/plain, */*")
					.header("Referer", properties.baseUrl() + "/PatientPortal/NewPortal/terms/index?" + query)
					.header("X-Requested-With", "XMLHttpRequest")
					.GET());
			if (response.statusCode() != 200) {
				throw new IllegalStateException("LuxMed search failed: HTTP " + response.statusCode());
			}
			return jsonMapper.readValue(response.body(), LuxmedTermsResponse.class);
		} catch (IOException e) {
			throw new IllegalStateException("LuxMed search failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("LuxMed search interrupted", e);
		}
	}

	public int getMaxSearchDays() {
		MonitorProperties.SearchPlace place = monitorProperties.searchPlace();
		String query = "cityId=" + place.id()
				+ "&serviceVariantId=" + monitorProperties.serviceVariantId()
				+ "&isDelocalized=false";
		try {
			HttpResponse<String> response = send(HttpRequest.newBuilder(uri(MAX_SEARCH_DAYS_API + "?" + query))
					.header("User-Agent", USER_AGENT)
					.header("Accept", "application/json, text/plain, */*")
					.header("X-Requested-With", "XMLHttpRequest")
					.GET());
			if (response.statusCode() != 200) {
				throw new IllegalStateException("LuxMed GetMaxSearchDays failed: HTTP " + response.statusCode());
			}
			return jsonMapper.readTree(response.body().trim()).asInt();
		} catch (IOException e) {
			throw new IllegalStateException("LuxMed GetMaxSearchDays failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("LuxMed GetMaxSearchDays interrupted", e);
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public synchronized String login() {
		if (isBlank(properties.login()) || isBlank(properties.password())) {
			throw new IllegalStateException("LUXMED_LOGIN and LUXMED_PASSWORD must be set");
		}
		try {
			cookieManager.getCookieStore().removeAll();
			send(HttpRequest.newBuilder(uri(LOGIN_PAGE))
					.header("User-Agent", USER_AGENT)
					.header("Accept", "text/html,application/xhtml+xml,*/*")
					.GET());

			String body = jsonMapper.writeValueAsString(
					Map.of("login", properties.login(), "password", properties.password()));
			HttpResponse<String> response = send(HttpRequest.newBuilder(uri(LOGIN_API))
					.header("User-Agent", USER_AGENT)
					.header("Accept", "application/json, text/plain, */*")
					.header("Content-Type", "application/json")
					.header("Origin", properties.baseUrl())
					.header("Referer", properties.baseUrl() + LOGIN_PAGE)
					.header("X-Requested-With", "XMLHttpRequest")
					.POST(HttpRequest.BodyPublishers.ofString(body)));

			if (response.statusCode() != 200) {
				throw new IllegalStateException("LuxMed login failed: HTTP " + response.statusCode());
			}
			JsonNode json = jsonMapper.readTree(response.body());
			if (!json.path("succeded").asBoolean(false)) {
				throw new IllegalStateException("LuxMed login rejected: " + json.path("errorMessage").asString(null)
						+ " (code " + json.path("errorCode").asString(null) + ")");
			}
			token = json.path("token").asString(null);
			log.info("LuxMed login succeeded");
			return token;
		} catch (IOException e) {
			throw new IllegalStateException("LuxMed login failed", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("LuxMed login interrupted", e);
		}
	}

	public String getToken() {
		return token;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
		return httpClient.send(builder.timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create(properties.baseUrl() + path);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
