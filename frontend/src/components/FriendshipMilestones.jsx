import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Loader, CalendarDays, Clock, CheckCircle2, Lock } from 'lucide-react';
import { getMilestones } from '../api/users';

function Badge({ badge, delay }) {
    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.85 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay }}
            className={`flex flex-col items-center gap-1.5 p-3 rounded-xl border transition-all ${
                badge.unlocked
                    ? 'bg-[var(--bg-card)] border-[var(--accent)]/30 shadow-sm'
                    : 'bg-[var(--bg-elevated)]/40 border-[var(--border-color)] opacity-50'
            }`}
            title={badge.description}
        >
            <span className={`text-2xl ${badge.unlocked ? '' : 'grayscale'}`}>
                {badge.emoji}
            </span>
            <span className={`text-[11px] font-bold ${badge.unlocked ? 'text-[var(--text-primary)]' : 'text-[var(--text-muted)]'}`}>
                {badge.label}
            </span>
            {badge.unlocked
                ? <span className="text-[9px] text-[var(--accent)]">{badge.unlockedAt}</span>
                : <Lock size={9} className="text-[var(--text-muted)]" />
            }
        </motion.div>
    );
}

function TimelineItem({ event, isLast, delay }) {
    return (
        <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay }}
            className="flex gap-3"
        >
            {/* Spine */}
            <div className="flex flex-col items-center">
                <div className="w-7 h-7 rounded-full bg-[var(--accent-light)] border border-[var(--accent)]/30 flex items-center justify-center flex-shrink-0 text-sm">
                    {event.emoji}
                </div>
                {!isLast && <div className="w-px flex-1 mt-1 bg-[var(--border-color)]" style={{ minHeight: 20 }} />}
            </div>

            {/* Content */}
            <div className="pb-5 min-w-0">
                <p className="text-[13px] font-semibold text-[var(--text-primary)] leading-tight">
                    {event.label}
                </p>
                <p className="text-[11px] text-[var(--text-muted)] mt-0.5">
                    {new Date(event.date).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
                </p>
            </div>
        </motion.div>
    );
}

export default function FriendshipMilestones({ friendId }) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!friendId) return;
        setLoading(true);
        getMilestones(friendId)
            .then(res => setData(res.data))
            .catch(() => setError('Could not load milestone data.'))
            .finally(() => setLoading(false));
    }, [friendId]);

    if (loading) {
        return (
            <div className="flex items-center justify-center py-16">
                <Loader size={22} className="text-[var(--accent)] animate-spin" />
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className="flex items-center justify-center py-16">
                <p className="text-[13px] text-[var(--text-muted)]">{error || 'No milestone data.'}</p>
            </div>
        );
    }

    const unlockedCount = data.badges?.filter(b => b.unlocked).length ?? 0;

    return (
        <div className="p-4 space-y-5">

            {/* Summary card */}
            <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4 flex flex-wrap gap-6"
            >
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[var(--accent-light)] flex items-center justify-center">
                        <CalendarDays size={18} className="text-[var(--accent)]" />
                    </div>
                    <div>
                        <p className="text-[22px] font-bold text-[var(--text-primary)] leading-none">
                            {data.daysConnected}
                        </p>
                        <p className="text-[11px] text-[var(--text-muted)] font-medium">days connected</p>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[#34d39918] flex items-center justify-center">
                        <CheckCircle2 size={18} className="text-[#34d399]" />
                    </div>
                    <div>
                        <p className="text-[22px] font-bold text-[var(--text-primary)] leading-none">
                            {unlockedCount} / {data.badges?.length ?? 0}
                        </p>
                        <p className="text-[11px] text-[var(--text-muted)] font-medium">milestones reached</p>
                    </div>
                </div>

                {data.nextMilestone && (
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-[#a78bfa18] flex items-center justify-center">
                            <Clock size={18} className="text-[#a78bfa]" />
                        </div>
                        <div>
                            <p className="text-[13px] font-bold text-[var(--text-primary)] leading-tight">
                                {data.nextMilestone}
                            </p>
                            <p className="text-[11px] text-[var(--text-muted)] font-medium">
                                in {data.daysToNextMilestone} day{data.daysToNextMilestone !== 1 ? 's' : ''}
                            </p>
                        </div>
                    </div>
                )}

                <div className="w-full">
                    <p className="text-[11px] text-[var(--text-muted)] flex items-center gap-1">
                        <span>Friends since</span>
                        <span className="font-semibold text-[var(--text-secondary)]">
                            {new Date(data.friendsSince).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}
                        </span>
                        {data.mutual && <span className="ml-1 text-[var(--accent)]">· Mutual 🤝</span>}
                    </p>
                </div>
            </motion.div>

            {/* Badge grid */}
            <div>
                <p className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-3">
                    Milestone Badges
                </p>
                <div className="grid grid-cols-4 sm:grid-cols-8 gap-2">
                    {data.badges?.map((badge, i) => (
                        <Badge key={badge.id} badge={badge} delay={i * 0.04} />
                    ))}
                </div>
            </div>

            {/* Progress bar to next milestone */}
            {data.nextMilestone && data.badges && (() => {
                const lastUnlocked = [...data.badges].reverse().find(b => b.unlocked);
                const nextBadge    = data.badges.find(b => !b.unlocked);
                if (!nextBadge) return null;
                const prev = lastUnlocked ? Number(lastUnlocked.unlockedAt
                    ? Math.max(0, data.daysConnected - (data.daysToNextMilestone)) : 0) : 0;
                const range = data.daysToNextMilestone;
                const pct = range > 0 ? Math.round(((range - data.daysToNextMilestone) / range) * 100) : 100;
                const filledPct = 100 - Math.round((data.daysToNextMilestone / (data.daysConnected + data.daysToNextMilestone)) * 100);
                return (
                    <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                        <div className="flex items-center justify-between mb-2">
                            <p className="text-[12px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
                                Progress to {data.nextMilestone}
                            </p>
                            <span className="text-[11px] text-[var(--accent)] font-bold">{data.daysToNextMilestone}d left</span>
                        </div>
                        <div className="h-2 bg-[var(--bg-elevated)] rounded-full overflow-hidden">
                            <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${filledPct}%` }}
                                transition={{ duration: 0.8, ease: 'easeOut' }}
                                className="h-full rounded-full bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-3)]"
                            />
                        </div>
                    </div>
                );
            })()}

            {/* Timeline */}
            {data.timeline?.length > 0 && (
                <div className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl p-4">
                    <p className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">
                        Friendship Timeline
                    </p>
                    <div>
                        {data.timeline.map((event, i) => (
                            <TimelineItem
                                key={i}
                                event={event}
                                isLast={i === data.timeline.length - 1}
                                delay={i * 0.05}
                            />
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
