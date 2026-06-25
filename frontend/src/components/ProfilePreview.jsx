import { useAuth } from '../context/AuthContext';
import { useEffect, useState, useCallback } from 'react';
import { getProfile, getRecommendations, followUser } from '../api/users';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';
import { RefreshCw, MapPin, Users, Sparkles } from 'lucide-react';

function MatchBadge({ score }) {
    const color = score >= 60 ? '#34d399' : score >= 30 ? 'var(--accent)' : '#a78bfa';
    return (
        <span
            className="text-[9px] font-bold px-1.5 py-0.5 rounded-full"
            style={{ background: `${color}20`, color }}
        >
            {score}% match
        </span>
    );
}

function ReasonPill({ reason }) {
    const icon = reason.includes('mutual') ? <Users size={9} className="flex-shrink-0" />
        : reason.includes('location') ? <MapPin size={9} className="flex-shrink-0" />
            : <Sparkles size={9} className="flex-shrink-0" />;
    return (
        <span className="flex items-center gap-0.5 text-[9px] text-[var(--text-muted)] bg-[var(--bg-elevated)] px-1.5 py-0.5 rounded-full truncate max-w-[120px]">
            {icon}{reason}
        </span>
    );
}

export default function ProfilePreview() {
    const { user } = useAuth();
    const [profile, setProfile] = useState(null);
    const [recommendations, setRecommendations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [followed, setFollowed] = useState(new Set());

    useEffect(() => {
        getProfile().then(res => setProfile(res.data)).catch(() => { });
    }, []);

    const loadRecommendations = useCallback(async (isRefresh = false) => {
        if (isRefresh) setRefreshing(true);
        else setLoading(true);
        try {
            const res = await getRecommendations();
            const data = Array.isArray(res.data) ? res.data : [];
            // On refresh, shuffle so users see fresh order
            if (isRefresh) {
                data.sort(() => Math.random() - 0.5);
            }
            setRecommendations(data.slice(0, 5));
        } catch {
            setRecommendations([]);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, []);

    useEffect(() => { loadRecommendations(); }, [loadRecommendations]);

    const handleFollow = async (userId) => {
        try {
            await followUser(userId);
            setFollowed(prev => new Set([...prev, userId]));
            // Remove card after short delay
            setTimeout(() => {
                setRecommendations(prev => prev.filter(r => r.id !== userId));
            }, 600);
        } catch { }
    };

    const initial = user?.email?.charAt(0)?.toUpperCase() || '?';
    const displayName = profile ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() : '';
    const profilePic = profile?.profilePicUrl;

    return (
        <div className="hidden xl:block w-[320px] h-screen sticky top-0 p-5 overflow-y-auto">
            <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 }}
                className="pt-4"
            >
                {/* Profile card */}
                <div className="flex items-center gap-3 mb-6">
                    <div className="avatar-story">
                        <div className="avatar w-[56px] h-[56px] text-[18px]">
                            {profilePic ? (
                                <img src={profilePic} alt={displayName} className="w-full h-full object-cover rounded-full" />
                            ) : initial}
                        </div>
                    </div>
                    <div className="min-w-0">
                        <p className="text-[14px] font-semibold text-[var(--text-primary)] truncate">{displayName}</p>
                        <p className="text-[13px] text-[var(--text-muted)] truncate">@{user?.email?.split('@')[0]}</p>
                    </div>
                </div>

                {/* Header */}
                <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-1.5">
                        <Sparkles size={13} className="text-[var(--accent)]" />
                        <span className="text-[13px] font-semibold text-[var(--text-muted)]">Recommended for you</span>
                    </div>
                    <button
                        onClick={() => loadRecommendations(true)}
                        disabled={refreshing}
                        className="p-1 rounded-lg hover:bg-[var(--bg-elevated)] transition-colors cursor-pointer disabled:opacity-40"
                        title="Refresh recommendations"
                    >
                        <RefreshCw size={13} className={`text-[var(--text-muted)] ${refreshing ? 'animate-spin' : ''}`} />
                    </button>
                </div>

                {/* Cards */}
                <div className="space-y-3">
                    {loading ? (
                        Array.from({ length: 3 }).map((_, i) => (
                            <div key={i} className="flex items-start gap-3 py-1">
                                <div className="w-9 h-9 rounded-full skeleton flex-shrink-0" />
                                <div className="flex-1 space-y-1.5 pt-0.5">
                                    <div className="h-3 w-24 skeleton" />
                                    <div className="h-2.5 w-16 skeleton" />
                                </div>
                            </div>
                        ))
                    ) : recommendations.length === 0 ? (
                        <p className="text-[12px] text-[var(--text-muted)] py-2">
                            Complete your profile to get better recommendations
                        </p>
                    ) : (
                        <AnimatePresence>
                            {recommendations.map((rec, idx) => (
                                <motion.div
                                    key={rec.id}
                                    initial={{ opacity: 0, x: 12 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -12, height: 0 }}
                                    transition={{ delay: idx * 0.05 }}
                                    className="flex items-start gap-3 py-1 group"
                                >
                                    <Link to={`/profile/${rec.id}`} className="flex-shrink-0">
                                        <div className="avatar w-9 h-9 text-[10px]">
                                            {rec.profilePicUrl ? (
                                                <img src={rec.profilePicUrl} alt={rec.name}
                                                    className="w-full h-full object-cover rounded-full" />
                                            ) : rec.name?.charAt(0)?.toUpperCase() || '?'}
                                        </div>
                                    </Link>

                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-1.5 flex-wrap">
                                            <Link to={`/profile/${rec.id}`}>
                                                <p className="text-[13px] font-semibold text-[var(--text-primary)] truncate hover:underline">
                                                    {rec.name || rec.email?.split('@')[0]}
                                                </p>
                                            </Link>
                                            <MatchBadge score={rec.matchScore} />
                                        </div>

                                        {/* Match reasons */}
                                        {rec.matchReasons?.length > 0 && (
                                            <div className="flex flex-wrap gap-1 mt-1">
                                                {rec.matchReasons.slice(0, 2).map((r, i) => (
                                                    <ReasonPill key={i} reason={r} />
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <button
                                        onClick={() => handleFollow(rec.id)}
                                        disabled={followed.has(rec.id)}
                                        className={`text-[12px] font-semibold transition-colors cursor-pointer flex-shrink-0 mt-0.5 ${
                                            followed.has(rec.id)
                                                ? 'text-[var(--text-muted)]'
                                                : 'text-[var(--accent)] hover:text-[var(--accent-hover)]'
                                        }`}
                                    >
                                        {followed.has(rec.id) ? '✓' : 'Follow'}
                                    </button>
                                </motion.div>
                            ))}
                        </AnimatePresence>
                    )}
                </div>

                {/* Footer */}
                <div className="mt-6 text-[11px] text-[var(--text-muted)]/50 leading-relaxed">
                    <p>
                        <span style={{ fontFamily: "'Grand Hotel', cursive", fontSize: '18px' }}>
                            <span style={{ color: 'var(--text-primary)' }}>Friends</span>
                            <span className="text-[var(--accent)]">Hub</span>
                        </span> © 2026
                    </p>
                </div>
            </motion.div>
        </div>
    );
}
