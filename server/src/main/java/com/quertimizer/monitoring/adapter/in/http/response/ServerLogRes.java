package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.monitoring.application.output.ServerLogOutput;
import lombok.Data;

import java.util.List;

@Data
public class ServerLogRes {

    private final String level;
    private final String date;
    private final boolean exists;
    private final List<String> lines;

    public static ServerLogRes from(ServerLogOutput output) {
        return new ServerLogRes(output.getLevel().getValue(), output.getDate().toString(), output.isExists(), output.getLines());
    }
}
