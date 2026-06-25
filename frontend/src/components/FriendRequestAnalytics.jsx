import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Send, Clock, CheckCheck, UserPlus, TrendingUp, Loader2 } from 'lucide-react';
import { getFriendRequestAnalytics } from '../api/users';

// ── Stat card ────────────────────────────────────────────────
function StatCard({ icon: Icon, label, value, color, delay }) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay }}
            className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4 flex flex-col gap-2"
        >
            <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: `${color}18` }}>
                <Icon size={15} style={{ color }} />
            </div>
            <span className="text-[22px] font-bold text-[var(--text-primary)] leading-none">{value}</span>
            <span className="text-[11px] text-[var(--text-muted)] font-medium">{label}</span>
        </motion.div>
    );
}

// ── SVG bar chart ────────────────────────────────────────────
function TrendChart({ sentData, receivedData }) {
    const allCounts = [...sentData, ...receivedData].map(d => d.count);
    const max = Math.max(...allCounts, 1);
    const labels = sentData.map(d => d.label);
    const n = labels.length;
    const W = 320, H = 90;
    const barW = 10, groupGap = (W - n * barW * 2 - (n - 1) * 8) / (n + 1);

    return (
        <div>
            {/* Legend */}
            <div className="flex items-center gap-4 mb-3">
                {[{ color: 'var(--accent)', label: 'Sent' }, { color: '#a78bfa', label: 'Received' }].map(l => (
                    <div key={l.label} className="flex items-center gap-1.5">
                        <div className="w-2.5 h-2.5 rounded-sm" style={{ background: l.color }} />
                        <span className="text-[11px] text-[var(--text-muted)]">{l.label}</span>
                    </div>
                ))}
            </div>
            <svg viewBox={`0 0 ${W} ${H + 18}`} width="100%" style={{ overflow: 'visible' }}>
                {labels.map((label, i) => {
                    const sentH    = Math.max(3, (sentData[i].count / max) * H);
                    const recvH    = Math.max(3, (receivedData[i].count / max) * H);
                    const groupX   = groupGap + i * (barW * 2 + 8 + groupGap);
                    return (
                        <g key={label}>
                            {/* sent bar */}
                            <rect x={groupX} y={H - sentH} width={barW} height={sentH}
                                rx={3} fill="var(--accent)" opacity="0.85" />
                            {/* received bar */}
                            <rect x={groupX + barW + 2} y={H - recvH} width={barW} height={recvH}
                                rx={3} fill="#a78bfa" opacity="0.85" />
                            {/* month label */}
                            <text x={groupX + barW} y={H + 13} textAnchor="middle"
                                fontSize="9" fill="var(--text-muted)">{label}</text>
                        </g>
                    );
                })}
            </svg>
        </div>
    );
}

// ── Acceptance rate ring ─────────────────────────────────────
function AcceptanceRing({ rate }) {
    const r = 28, circ = 2 * Math.PI * r;
    const dash = (rate / 100) * circ;
    const color = rate >= 70 ? '#34d399' : rate >= 40 ? 'var(--accent)' : '#f87171';
    return (
        <div className="flex flex-col items-center gap-1">
            <svg width="72" height="72" viewBox="0 0 72 72">
                <circle cx="36" cy="36" r={r} fill="none" stroke="var(--bg-elevated)" strokeWidth="5" />
                <circle cx="36" cy="36" r={r} fill="none" stroke={color} strokeWidth="5"
                    strokeDasharray={`${dash} ${circ}`} strokeLinecap="round"
                    transform="rotate(-90 36 36)"
                    style={{ transition: 'stroke-dasharray 0.8s ease' }} />
                <text x="36" y="39" textAnchor="middle" fontSize="13" fontWeight="700" fill={color}>
                    {rate}%
                </text>
            </svg>
            <span className="text-[11px] text-[var(--text-muted)] font-medium">Acceptance Rate</span>
        </div>
    );
}

// ── Main component ───────────────────────────────────────────
export default function FriendRequestAnalytics() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        getFriendRequestAnalytics()
            .then(res => setData(res.data))
            .catch(() => setError('Could not load analytics.'))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex items-center justify-center py-20">
                <Loader2 size={24} className="animate-spin text-[var(--accent)]" />
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className="flex items-center justify-center py-20">
                <p className="text-[13px] text-[var(--text-muted)]">{error || 'No data available.'}</p>
            </div>
        );
    }

    const sentCards = [
        { icon: Send,       label: 'Requests Sent',     value: data.totalSent,    color: 'var(--accent)', delay: 0 },
        { icon: CheckCheck, label: 'Accepted',           value: data.acceptedSent, color: '#34d399',       delay: 0.06 },
        { icon: Clock,      label: 'Pending',            value: data.pendingSent,  color: '#facc15',       delay: 0.12 },
    ];

    const receivedCards = [
        { icon: UserPlus,   label: 'Requests Received',  value: data.totalReceived,    color: '#a78bfa', delay: 0.18 },
        { icon: CheckCheck, label: 'Accepted',            value: data.acceptedReceived, color: '#34d399', delay: 0.24 },
        { icon: Clock,      label: 'Pending',             value: data.pendingReceived,  color: '#facc15', delay: 0.30 },
    ];

    return (
        <div className="space-y-6">
            <h2 className="text-[var(--text-primary)] text-lg font-semibold">Friend Request Analytics</h2>

            {/* Sent stats + acceptance ring */}
            <div>
                <p className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-3">
                    Requests Sent
                </p>
                <div className="flex items-start gap-4">
                    <div className="grid grid-cols-3 gap-3 flex-1">
                        {sentCards.map(c => <StatCard key={c.label} {...c} />)}
                    </div>
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.36 }}
                        className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4 flex-shrink-0"
                    >
                        <AcceptanceRing rate={data.acceptanceRate} />
                    </motion.div>
                </div>
            </div>

            {/* Received stats */}
            <div>
                <p className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-3">
                    Requests Received
                </p>
                <div className="grid grid-cols-3 gap-3">
                    {receivedCards.map(c => <StatCard key={c.label} {...c} />)}
                </div>
            </div>

            {/* Monthly trend chart */}
            <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.42 }}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4"
            >
                <div className="flex items-center gap-2 mb-4">
                    <TrendingUp size={14} className="text-[var(--accent)]" />
                    <p className="text-[12px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
                        6-Month Activity Trend
                    </p>
                </div>
                {data.monthlySent?.every(d => d.count === 0) && data.monthlyReceived?.every(d => d.count === 0) ? (
                    <p className="text-[12px] text-[var(--text-muted)] text-center py-6">
                        No activity in the last 6 months
                    </p>
                ) : (
                    <TrendChart sentData={data.monthlySent || []} receivedData={data.monthlyReceived || []} />
                )}
            </motion.div>
        </div>
    );
}
