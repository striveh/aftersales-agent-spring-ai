package dev.striveh.aftersales.agent.trace;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TraceStore {

    private static final Logger log = LoggerFactory.getLogger("agent.trace");

    private final Clock clock;
    private final ConcurrentMap<String, List<TraceEvent>> traces = new ConcurrentHashMap<>();

    public TraceStore(Clock clock) {
        this.clock = clock;
    }

    public TraceEvent add(String conversationId, String type, String action, String status, String detail) {
        TraceEvent event = new TraceEvent(type, action == null ? "" : action, status == null ? "" : status,
                detail == null ? "" : detail, clock.instant());
        traces.computeIfAbsent(conversationId, id -> new ArrayList<>()).add(event);
        log.info("trace conversationId={} type={} action={} status={} detail={}", conversationId, type, action, status, detail);
        return event;
    }

    public List<TraceEvent> get(String conversationId) {
        return List.copyOf(traces.getOrDefault(conversationId, List.of()));
    }
}
