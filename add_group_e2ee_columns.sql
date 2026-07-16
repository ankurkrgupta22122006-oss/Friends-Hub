-- =============================================================================
-- add_group_e2ee_columns.sql
-- FriendsHub — Group End-to-End Encryption (E2EE) Columns Migration
-- =============================================================================
-- HOW TO RUN:
--   Supabase Dashboard → SQL Editor → paste this entire file → Run
-- =============================================================================

-- Add group_keys TEXT column (JSON string mapping member userId -> encryptedKeyB64:ivB64)
ALTER TABLE public.chat_groups 
ADD COLUMN IF NOT EXISTS group_keys TEXT;

-- Add iv TEXT column to chat_group_messages table for AES-GCM IV (base64)
ALTER TABLE public.chat_group_messages 
ADD COLUMN IF NOT EXISTS iv TEXT;
