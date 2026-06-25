package com.example.socialmedia.dto;

import java.util.List;

public class FriendRequestAnalyticsResponse {

    private long totalSent;
    private long pendingSent;
    private long acceptedSent;
    private long totalReceived;
    private long pendingReceived;
    private long acceptedReceived;
    private double acceptanceRate;   // accepted / total sent * 100
    private List<MonthlyPoint> monthlySent;
    private List<MonthlyPoint> monthlyReceived;

    public FriendRequestAnalyticsResponse(long totalSent, long pendingSent, long acceptedSent,
                                          long totalReceived, long pendingReceived, long acceptedReceived,
                                          List<MonthlyPoint> monthlySent, List<MonthlyPoint> monthlyReceived) {
        this.totalSent = totalSent;
        this.pendingSent = pendingSent;
        this.acceptedSent = acceptedSent;
        this.totalReceived = totalReceived;
        this.pendingReceived = pendingReceived;
        this.acceptedReceived = acceptedReceived;
        this.acceptanceRate = totalSent > 0 ? Math.round((acceptedSent * 100.0 / totalSent) * 10) / 10.0 : 0;
        this.monthlySent = monthlySent;
        this.monthlyReceived = monthlyReceived;
    }

    public long getTotalSent() { return totalSent; }
    public long getPendingSent() { return pendingSent; }
    public long getAcceptedSent() { return acceptedSent; }
    public long getTotalReceived() { return totalReceived; }
    public long getPendingReceived() { return pendingReceived; }
    public long getAcceptedReceived() { return acceptedReceived; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public List<MonthlyPoint> getMonthlySent() { return monthlySent; }
    public List<MonthlyPoint> getMonthlyReceived() { return monthlyReceived; }

    public static class MonthlyPoint {
        private String label;  // e.g. "Jan", "Feb"
        private long count;

        public MonthlyPoint(String label, long count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() { return label; }
        public long getCount() { return count; }
    }
}
