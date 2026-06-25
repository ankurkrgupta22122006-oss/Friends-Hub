import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { MessageSquare, Heart, MessageCircle, Zap, Loader } from 'lucide-react';
import { getFriendStats } from '../api/users';

const STAT_CARDS = [
    { key: 'totalMessages',   label: 'Messages',         icon: MessageSquare, color: '#a78bfa' },
    { key: 'sharedActivity',  label: 'Total Interactions', icon: Zap,          color: '#E8824A' },
    { key: 'likesGiven',      label: 'Likes Given',      icon: Heart,         color: '#f43f5e' },
    { key: 'commentsGiven',   label: 'Comments Left',    icon: MessageCircle, color: '#34d399' },
];

function BarChart({ data }) {
    const max = Math.max(...data.map(d => d.count), 1);
    const W = 280, H = 80, barW = 24, gap = (W - data.length * barW) / (data.length + 1);

    return (
        <svg viewBox={`0 0 ${W} ${H + 20}`} width="100%" style={{ overflow: 'visible' }}>
            {data.map((d, i) => {
                const barH = Math.max(3, (d.count / max) * H);
                const x = gap + i * (barW + gap);
                const y = H - barH;
                return (
                    <g key={i}>
                        <rect
                            x={x} y={y} width={barW} height={barH}
                            rx={4}
                            fill={d.count > 0 ? 'var(--accent)' : 'var(--bg-hover)'}
                            opacity={d.count > 0 ? 0.85 : 0.4}
                        />
                        {d.count > 0 && (
                            <text
                                x={x + barW / 2} y={y - 4}
                                textAnchor="middle"
                                fontSize="8"
                                fill="var(--text-muted)"
                            >
                                {d.count}
                            </text>
                        )}
                        <text
                            x={x + barW / 2} y={H + 14}
                            textAnchor="middle"
                            fontSize="9"
                            fill="var(--text-muted)"
                        >
                            {d.day}
                        </text>
                    </g>
                );
            })}
        </svg>
    );
}

export default function FriendStats({ friendId }) {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!friendId) return;
        setLoading(true);
        getFriendStats(friendId)
            .then(res => setStats(res.data))
            .catch(() => setError('Could not load interaction stats.'))
            .finally(() => setLoading(false));
    }, [friendId]);

    if (loading) {
        return (
            <div className="flex items-center justify-center py-16">
                <Loader size={22} className="text-[var(--accent)] animate-spin" />
            </div>
        );
    }

    if (error || !stats) {
        return (
            <div className="flex items-center justify-center py-16">
                <p className="text-[13px] text-[var(--text-muted)]">{error || 'No data available.'}</p>
            </div>
        );
    }

    return (
        <div className="p-4 space-y-5">
            {/* Stat Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {STAT_CARDS.map(({ key, label, icon: Icon, color }, idx) => (
                    <motion.div
                        key={key}
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: idx * 0.06 }}
                        className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4 flex flex-col gap-2"
                    >
                        <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                            style={{ background: `${color}18` }}>
                            <Icon size={16} style={{ color }} />
                        </div>
                        <span className="text-[22px] font-bold text-[var(--text-primary)] leading-none">
                            {stats[key] ?? 0}
                        </span>
                        <span className="text-[11px] text-[var(--text-muted)] font-medium">{label}</span>
                    </motion.div>
                ))}
            </div>

            {/* Breakdown row */}
            <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.28 }}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4"
            >
                <p className="text-[12px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-3">
                    Interaction Breakdown
                </p>
                <div className="space-y-2.5">
                    {[
                        { label: 'Likes you gave them',     value: stats.likesGiven,      color: '#f43f5e' },
                        { label: 'Likes they gave you',     value: stats.likesReceived,   color: '#f43f5e', light: true },
                        { label: 'Comments you left',       value: stats.commentsGiven,   color: '#34d399' },
                        { label: 'Comments they left you',  value: stats.commentsReceived, color: '#34d399', light: true },
                    ].map(({ label, value, color, light }) => {
                        const total = Math.max(
                            stats.likesGiven + stats.likesReceived + stats.commentsGiven + stats.commentsReceived, 1
                        );
                        const pct = Math.round((value / total) * 100);
                        return (
                            <div key={label}>
                                <div className="flex items-center justify-between mb-1">
                                    <span className="text-[12px] text-[var(--text-secondary)]">{label}</span>
                                    <span className="text-[12px] font-bold text-[var(--text-primary)]">{value}</span>
                                </div>
                                <div className="h-1.5 bg-[var(--bg-elevated)] rounded-full overflow-hidden">
                                    <motion.div
                                        initial={{ width: 0 }}
                                        animate={{ width: `${pct}%` }}
                                        transition={{ duration: 0.6, delay: 0.3 }}
                                        className="h-full rounded-full"
                                        style={{ background: color, opacity: light ? 0.45 : 0.85 }}
                                    />
                                </div>
                            </div>
                        );
                    })}
                </div>
            </motion.div>

            {/* Weekly chart */}
            <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.36 }}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4"
            >
                <p className="text-[12px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">
                    Messages — Last 7 Days
                </p>
                {stats.weeklyMessages?.every(d => d.count === 0) ? (
                    <p className="text-[12px] text-[var(--text-muted)] text-center py-4">No messages in the last 7 days</p>
                ) : (
                    <BarChart data={stats.weeklyMessages || []} />
                )}
            </motion.div>
        </div>
    );
}
