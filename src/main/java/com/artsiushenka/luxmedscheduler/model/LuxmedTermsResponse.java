package com.artsiushenka.luxmedscheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LuxmedTermsResponse {

    private String correlationId;
    private TermsForService termsForService;
    private Integer pMode;
    private Boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TermsForService {
        private Integer serviceVariantId;
        private AdditionalData additionalData;
        private List<TermsForDay> termsForDays;
        private List<TermsInfoForDay> termsInfoForDays;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdditionalData {
        @JsonProperty("isPreparationRequired")
        private Boolean isPreparationRequired;

        private List<Object> preparationItems;
        private Boolean previousTermsAvailable;
        private Boolean nextTermsAvailable;
        private Boolean anyTermForTelemedicine;
        private Boolean anyTermForFacilityVisit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TermsForDay {
        private LocalDateTime day;
        private String correlationId;
        private List<Term> terms;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Term {
        private LocalDateTime dateTimeFrom;
        private LocalDateTime dateTimeTo;

        private Doctor doctor;

        private Integer clinicId;
        private String clinic;
        private Integer clinicGroupId;
        private String clinicGroup;
        private Integer roomId;
        private Integer serviceId;
        private Long scheduleId;

        @JsonProperty("isImpediment")
        private Boolean isImpediment;

        private String impedimentText;

        @JsonProperty("isAdditional")
        private Boolean isAdditional;

        @JsonProperty("isTelemedicine")
        private Boolean isTelemedicine;

        @JsonProperty("isInfectionTreatmentCenter")
        private Boolean isInfectionTreatmentCenter;

        private Integer partOfDay;
        private Integer priority;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Doctor {
        private Integer id;
        private Integer genderId;
        private String academicTitle;
        private String firstName;
        private String lastName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TermsInfoForDay {
        private LocalDateTime day;
        private Integer termsStatus;
        private String message;

        @JsonProperty("isLimitedDay")
        private Boolean isLimitedDay;

        @JsonProperty("isLastDayWithLoadedTerms")
        private Boolean isLastDayWithLoadedTerms;

        @JsonProperty("isMoreTermsThanCounter")
        private Boolean isMoreTermsThanCounter;

        private TermsCounter termsCounter;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TermsCounter {
        private Integer termsNumber;
        private List<Object> partialTermsCounters;
    }
}
