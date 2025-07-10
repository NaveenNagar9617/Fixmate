package com.fixmate.service;

import com.fixmate.exception.InvalidStateTransitionException;
import com.fixmate.model.enums.ComplaintStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ComplaintStateMachine {

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);

        ALLOWED_TRANSITIONS.put(ComplaintStatus.SUBMITTED, EnumSet.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.ESCALATED
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.ASSIGNED, EnumSet.of(
                ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.ESCALATED,
                ComplaintStatus.SUBMITTED
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, EnumSet.of(
                ComplaintStatus.RESOLVED,
                ComplaintStatus.ESCALATED
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.RESOLVED, EnumSet.of(
                ComplaintStatus.CLOSED,
                ComplaintStatus.REOPENED
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.CLOSED, EnumSet.of(
                ComplaintStatus.REOPENED
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.ESCALATED, EnumSet.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS
        ));

        ALLOWED_TRANSITIONS.put(ComplaintStatus.REOPENED, EnumSet.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS
        ));
    }

    public void validate(ComplaintStatus from, ComplaintStatus to) {
        Set<ComplaintStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        if (!allowed.contains(to)) {
            throw new InvalidStateTransitionException(from.name(), to.name());
        }
    }

    public Set<ComplaintStatus> getAllowedTransitions(ComplaintStatus current) {
        return ALLOWED_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }
}
