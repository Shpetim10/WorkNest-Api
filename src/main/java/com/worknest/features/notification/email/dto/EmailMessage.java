package com.worknest.features.notification.email.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EmailMessage {
    private String to;
    private String subjectKey;
    private Object[] subjectArgs;
    private String templateName;
    private Map<String, Object> templateModel;
}
