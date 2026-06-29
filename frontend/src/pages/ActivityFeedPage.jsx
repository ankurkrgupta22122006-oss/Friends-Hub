import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { RefreshCw, Image, UserPlus, Loader, Heart, MessageSquare } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getActivityFeed } from '../api/posts';

const PAGE_SIZE = 20;

function timeAgo(iso) {
    if (!iso) return '';
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
}

function Avatar({ picUrl, name, size = 10 }) {
    const initial = name?.charAt(0)?.toUpperCase() || '?';
    return (
        <div className={`avatar w-${size} h-${size} text-[11px] flex-shrink-0`}>
            {picUrl
                ? <img src={picUrl} alt={name} className="w-full h-full object-cover rounded-full" />
                : initial}
        </div>
    );
}

function PostCard({ item, idx }) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.04 }}
            className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl overflow-hidden"
        >
            {/* Header */}
            <div className="flex items-center gap-3 px-4 py-3">
                <Link to={`/profile/${item.actorId}`}>
                    <Avatar picUrl={item.actorPicUrl} name={item.actorName} size={10} />
                </Link>
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                        <Link to={`/profile/${item.actorId}`}>
                            <span className="text-[13px] font-bold text-[var(--text-primary)] hover:underline">
                                {item.actorName}
                            </span>
                        </Link>
                        <span className="text-[12px] text-[var(--text-muted)]">shared a post</span>
                    </div>
                    <p className="text-[11px] text-[var(--text-muted)]">{timeAgo(item.timestamp)}</p>
                </div>
                <div className="w-8 h-8 rounded-lg bg-[var(--accent-light)] flex items-center justify-center flex-shrink-0">
                    <Image size={14} className="text-[var(--accent)]" />
                </div>
            </div>

            {/* Image */}
            {item.postImageUrl && (
                <img
                    src={item.postImageUrl}
                    alt=""
                    className="w-full max-h-64 object-cover"
                />
            )}

            {/* Content */}
            {item.postContent && (
                <p className="px-4 py-2 text-[14px] text-[var(--text-secondary)] line-clamp-3">
                    {item.postContent}
                </p>
            )}

            {/* Footer */}
            <div className="flex items-center gap-4 px-4 py-2.5 border-t border-[var(--border-color)]/50">
                <span className="flex items-center gap-1 text-[12px] text-[var(--text-muted)]">
                    <Heart size={13} /> {item.likeCount}
                </span>
                <span className="flex items-center gap-1 text-[12px] text-[var(--text-muted)]">
                    <MessageSquare size={13} /> {item.commentCount}
                </span>
            </div>
        </motion.div>
    );
}

function NewFriendCard({ item, idx }) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.04 }}
            className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl px-4 py-3 flex items-center gap-3"
        >
            <Link to={`/profile/${item.actorId}`}>
                <Avatar picUrl={item.actorPicUrl} name={item.actorName} size={10} />
            </Link>
            <div className="flex-1 min-w-0">
                <p className="text-[13px] text-[var(--text-primary)]">
                    <Link to={`/profile/${item.actorId}`}>
                        <span className="font-bold hover:underline">{item.actorName}</span>
                    </Link>
                    <span className="text-[var(--text-muted)]"> is now friends with </span>
                    <Link to={`/profile/${item.targetId}`}>
                        <span className="font-bold hover:underline">{item.targetName}</span>
                    </Link>
                </p>
                <p className="text-[11px] text-[var(--text-muted)] mt-0.5">{timeAgo(item.timestamp)}</p>
            </div>
            <div className="flex -space-x-2 flex-shrink-0">
                <Avatar picUrl={item.actorPicUrl} name={item.actorName} size={8} />
                <Avatar picUrl={item.targetPicUrl} name={item.targetName} size={8} />
            </div>
            <div className="w-8 h-8 rounded-lg bg-[#34d39918] flex items-center justify-center flex-shrink-0">
                <UserPlus size={14} className="text-[#34d399]" />
            </div>
        </motion.div>
    );
}

export default function ActivityFeedPage() {
    const [items, setItems] = useState([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [refreshing, setRefreshing] = useState(false);

    const load = useCallback(async (pageNum, append = false) => {
        if (append) setLoadingMore(true);
        else if (pageNum === 0) setLoading(true);

        try {
            const res = await getActivityFeed(pageNum, PAGE_SIZE);
            const data = Array.isArray(res.data) ? res.data : [];
            setItems(prev => append ? [...prev, ...data] : data);
            setHasMore(data.length === PAGE_SIZE);
            setPage(pageNum);
        } catch {
            setHasMore(false);
        } finally {
            setLoading(false);
            setLoadingMore(false);
            setRefreshing(false);
        }
    }, []);

    useEffect(() => { load(0); }, [load]);

    const handleRefresh = () => {
        setRefreshing(true);
        load(0);
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="max-w-[600px] mx-auto pt-4 md:pt-8 px-4 pb-24 lg:pb-8"
        >
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
                <h1 className="text-[20px] font-bold text-[var(--text-primary)]">Friend Activity</h1>
                <button
                    onClick={handleRefresh}
                    disabled={refreshing}
                    className="btn-icon border border-[var(--border-color)] w-9 h-9 disabled:opacity-40"
                    title="Refresh"
                >
                    <RefreshCw size={15} className={refreshing ? 'animate-spin' : ''} />
                </button>
            </div>

            {/* Loading skeleton */}
            {loading && (
                <div className="space-y-3">
                    {Array.from({ length: 4 }).map((_, i) => (
                        <div key={i} className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-2xl p-4">
                            <div className="flex items-center gap-3 mb-3">
                                <div className="w-10 h-10 rounded-full skeleton" />
                                <div className="flex-1 space-y-1.5">
                                    <div className="h-3 w-32 skeleton" />
                                    <div className="h-2.5 w-20 skeleton" />
                                </div>
                            </div>
                            <div className="h-3 w-full skeleton mb-1.5" />
                            <div className="h-3 w-3/4 skeleton" />
                        </div>
                    ))}
                </div>
            )}

            {/* Empty state */}
            {!loading && items.length === 0 && (
                <div className="flex flex-col items-center justify-center py-24 text-center">
                    <div className="w-16 h-16 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center mb-4">
                        <UserPlus size={28} className="text-[var(--text-muted)]" />
                    </div>
                    <p className="text-[15px] font-medium text-[var(--text-primary)] mb-1">No activity yet</p>
                    <p className="text-[12px] text-[var(--text-muted)] max-w-[220px]">
                        Follow more people to see their posts and new friendships here.
                    </p>
                </div>
            )}

            {/* Feed */}
            {!loading && (
                <div className="space-y-3">
                    <AnimatePresence>
                        {items.map((item, idx) =>
                            item.type === 'POST'
                                ? <PostCard key={`${item.type}-${item.postId}-${idx}`} item={item} idx={idx} />
                                : <NewFriendCard key={`${item.type}-${item.actorId}-${item.targetId}-${idx}`} item={item} idx={idx} />
                        )}
                    </AnimatePresence>
                </div>
            )}

            {/* Load more */}
            {!loading && hasMore && (
                <div className="flex justify-center mt-6">
                    <button
                        onClick={() => load(page + 1, true)}
                        disabled={loadingMore}
                        className="btn-ghost px-6 py-2.5 text-[13px] font-semibold flex items-center gap-2"
                    >
                        {loadingMore ? <Loader size={14} className="animate-spin" /> : null}
                        {loadingMore ? 'Loading...' : 'Load more'}
                    </button>
                </div>
            )}

            {/* End */}
            {!loading && !hasMore && items.length > 0 && (
                <div className="text-center py-8 border-t border-[var(--border-color)] mt-4">
                    <div className="inline-flex items-center gap-2 text-[var(--text-muted)]">
                        <div className="w-8 h-[1px] bg-[var(--border-color)]" />
                        <span className="text-[12px]">You're all caught up</span>
                        <div className="w-8 h-[1px] bg-[var(--border-color)]" />
                    </div>
                </div>
            )}
        </motion.div>
    );
}
