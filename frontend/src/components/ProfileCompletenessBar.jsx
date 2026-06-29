import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ChevronUp, CheckCircle2, Circle } from 'lucide-react';
import { useState } from 'react';

const STEPS = [
    { key: 'profilePicUrl',   label: 'Add a profile photo',      weight: 20, action: 'Upload a photo to help friends recognise you.' },
    { key: 'firstName',       label: 'Add your first name',       weight: 15, action: 'Let people know who you are.' },
    { key: 'lastName',        label: 'Add your last name',        weight: 10, action: 'Complete your display name.' },
    { key: 'bio',             label: 'Write a bio',               weight: 20, action: 'Tell others about yourself or your interests.' },
    { key: 'city',            label: 'Add your location',         weight: 10, action: 'Helps friends nearby discover you.' },
    { key: 'hasFollowers',    label: 'Get your first follower',   weight: 10, action: 'Share your profile to start building connections.' },
    { key: 'hasFollowing',    label: 'Follow someone',            weight: 5,  action: 'Follow at least one person to grow your network.' },
    { key: 'hasPost',         label: 'Share your first post',     weight: 10, action: 'Posts make your profile more engaging.' },
];

function getColor(pct) {
    if (pct >= 80) return '#34d399'; // green
    if (pct >= 50) return 'var(--accent)'; // orange
    return '#f87171'; // red
}

function getLabel(pct) {
    if (pct === 100) return 'Complete!';
    if (pct >= 80)   return 'Almost there';
    if (pct >= 50)   return 'Looking good';
    return 'Just getting started';
}

export function computeCompleteness(profile, postCount = 0) {
    if (!profile) return { score: 0, done: [], missing: [] };

    const checks = {
        profilePicUrl: !!profile.profilePicUrl,
        firstName:     !!profile.firstName,
        lastName:      !!profile.lastName,
        bio:           !!profile.bio,
        city:          !!profile.city,
        hasFollowers:  (profile.followerCount || 0) >= 1,
        hasFollowing:  (profile.followingCount || 0) >= 1,
        hasPost:       postCount >= 1,
    };

    const done    = STEPS.filter(s => checks[s.key]);
    const missing = STEPS.filter(s => !checks[s.key]);
    const score   = done.reduce((sum, s) => sum + s.weight, 0);

    return { score, done, missing };
}

export default function ProfileCompletenessBar({ profile, postCount = 0, onEditClick }) {
    const [expanded, setExpanded] = useState(false);
    const { score, done, missing } = computeCompleteness(profile, postCount);
    const color = getColor(score);

    if (score === 100) return null; // hide when fully complete

    return (
        <motion.div
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl px-4 py-3 mb-4"
        >
            {/* Header row */}
            <button
                onClick={() => setExpanded(e => !e)}
                className="w-full flex items-center gap-3 cursor-pointer"
            >
                {/* Circular score */}
                <svg width="36" height="36" viewBox="0 0 36 36" className="flex-shrink-0">
                    <circle cx="18" cy="18" r="15" fill="none" stroke="var(--bg-elevated)" strokeWidth="3" />
                    <circle
                        cx="18" cy="18" r="15"
                        fill="none"
                        stroke={color}
                        strokeWidth="3"
                        strokeDasharray={`${(score / 100) * 94.25} 94.25`}
                        strokeLinecap="round"
                        transform="rotate(-90 18 18)"
                        style={{ transition: 'stroke-dasharray 0.6s ease' }}
                    />
                    <text x="18" y="22" textAnchor="middle" fontSize="9" fontWeight="700" fill={color}>
                        {score}%
                    </text>
                </svg>

                <div className="flex-1 min-w-0 text-left">
                    <div className="flex items-center justify-between mb-1">
                        <span className="text-[13px] font-semibold text-[var(--text-primary)]">
                            Profile Completeness
                        </span>
                        <span className="text-[11px] font-medium" style={{ color }}>{getLabel(score)}</span>
                    </div>
                    {/* Progress bar */}
                    <div className="h-1.5 bg-[var(--bg-elevated)] rounded-full overflow-hidden">
                        <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${score}%` }}
                            transition={{ duration: 0.7, ease: 'easeOut' }}
                            className="h-full rounded-full"
                            style={{ background: color }}
                        />
                    </div>
                    {missing.length > 0 && (
                        <p className="text-[11px] text-[var(--text-muted)] mt-1">
                            {missing.length} step{missing.length > 1 ? 's' : ''} left to complete your profile
                        </p>
                    )}
                </div>

                <span className="text-[var(--text-muted)] flex-shrink-0">
                    {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                </span>
            </button>

            {/* Expandable step list */}
            <AnimatePresence>
                {expanded && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="overflow-hidden"
                    >
                        <div className="mt-3 space-y-1 border-t border-[var(--border-color)] pt-3">
                            {STEPS.map(step => {
                                const isDone = done.some(d => d.key === step.key);
                                return (
                                    <div key={step.key} className="flex items-start gap-2.5 py-1">
                                        {isDone
                                            ? <CheckCircle2 size={15} className="text-[#34d399] flex-shrink-0 mt-0.5" />
                                            : <Circle       size={15} className="text-[var(--text-muted)] flex-shrink-0 mt-0.5" />
                                        }
                                        <div className="flex-1 min-w-0">
                                            <p className={`text-[12px] font-medium ${isDone ? 'text-[var(--text-muted)] line-through' : 'text-[var(--text-primary)]'}`}>
                                                {step.label}
                                            </p>
                                            {!isDone && (
                                                <p className="text-[11px] text-[var(--text-muted)]">{step.action}</p>
                                            )}
                                        </div>
                                        {!isDone && onEditClick && (
                                            <button
                                                onClick={onEditClick}
                                                className="text-[11px] font-semibold text-[var(--accent)] hover:text-[var(--accent-hover)] flex-shrink-0 cursor-pointer"
                                            >
                                                Fix →
                                            </button>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}
