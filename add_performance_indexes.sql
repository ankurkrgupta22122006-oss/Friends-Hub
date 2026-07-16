-- =============================================================================
-- add_performance_indexes.sql
-- FriendsHub — Database Latency & Query Performance Optimization Indexes
-- =============================================================================
-- HOW TO RUN:
--   Supabase Dashboard → SQL Editor → paste this entire file → Run
-- =============================================================================

-- 1. Fast 1-on-1 Chat Queries & Partner Lookup
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_receiver ON public.chat_messages (sender_id, receiver_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_chat_messages_receiver_sender ON public.chat_messages (receiver_id, sender_id, timestamp);

-- 2. Fast Group Chat Message & Last Message Lookups
CREATE INDEX IF NOT EXISTS idx_chat_group_messages_group_created ON public.chat_group_messages (group_id, created_at DESC);

-- 3. Fast Group Member Resolution
CREATE INDEX IF NOT EXISTS idx_chat_group_members_user_id ON public.chat_group_members (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_group_members_group_id ON public.chat_group_members (group_id);

-- 4. Fast User Lookup & Authentication
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON public.users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_public_key ON public.users (id) WHERE public_key IS NOT NULL;

-- 5. Fast Social Follow & Feed Lookups
CREATE INDEX IF NOT EXISTS idx_follows_follower_id ON public.follows (follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following_id ON public.follows (following_id);
