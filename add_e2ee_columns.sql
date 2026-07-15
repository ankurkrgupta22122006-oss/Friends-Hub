-- =============================================================================
-- add_e2ee_columns.sql
-- FriendsHub — End-to-End Encryption (E2EE) Columns Migration
-- =============================================================================
-- HOW TO RUN:
--   Supabase Dashboard → SQL Editor → paste this entire file → Run
-- =============================================================================

-- Add public_key column to users table for ECDH P-256 public key (base64)
ALTER TABLE public.users 
ADD COLUMN IF NOT EXISTS public_key TEXT;

-- Add iv column to chat_messages table for AES-GCM IV (base64)
ALTER TABLE public.chat_messages 
ADD COLUMN IF NOT EXISTS iv TEXT;
