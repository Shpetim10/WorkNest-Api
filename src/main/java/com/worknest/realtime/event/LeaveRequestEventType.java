package com.worknest.realtime.event;

public final class LeaveRequestEventType {
    public static final String LEAVE_REQUEST_SUBMITTED = "LEAVE_REQUEST_SUBMITTED";
    public static final String LEAVE_REQUEST_APPROVED = "LEAVE_REQUEST_APPROVED";
    public static final String LEAVE_REQUEST_REJECTED = "LEAVE_REQUEST_REJECTED";
    public static final String LEAVE_REQUEST_CANCELLED = "LEAVE_REQUEST_CANCELLED";

    private LeaveRequestEventType() {}
}
