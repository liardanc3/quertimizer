package com.quertimizer.judge.application.model;

import java.util.List;

public interface DatabaseSelector {

    int selectStartIndex(List<Database> candidates);
}
