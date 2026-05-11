package com.quertimizer.monitoring.adapter.in.web.realtime.dto;

import lombok.Data;

@Data
public class MonitoringLogSocketReq {

    private String level;
    private String date;
    private Integer size;
}
