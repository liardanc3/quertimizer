package com.quertimizer.monitoring.adapter.in.websocket.dto;

import lombok.Data;

@Data
public class MonitoringLogSocketReq {

    private String level;
    private String date;
    private Integer size;
}
