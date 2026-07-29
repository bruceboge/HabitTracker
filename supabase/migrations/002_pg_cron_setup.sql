-- pg_cron setup for nightly difficulty recalculation
-- Run this AFTER deploying the Edge Function

-- Enable pg_cron extension (must be done from Supabase Dashboard → Database → Extensions)
-- CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Enable pg_net for HTTP calls from cron
-- CREATE EXTENSION IF NOT EXISTS pg_net;

-- Schedule nightly recalculation at 3 AM UTC
-- Replace YOUR_PROJECT and YOUR_SERVICE_KEY with actual values
-- 
-- SELECT cron.schedule(
--     'recalculate-difficulty-nightly',
--     '0 3 * * *',
--     $$
--     SELECT net.http_post(
--         url := 'https://YOUR_PROJECT.supabase.co/functions/v1/recalculate-difficulty',
--         body := '{}',
--         headers := jsonb_build_object(
--             'Content-Type', 'application/json',
--             'Authorization', 'Bearer YOUR_SERVICE_ROLE_KEY'
--         )
--     );
--     $$
-- );

-- NOTE: The above is commented out because it requires project-specific values.
-- To set up:
-- 1. Enable pg_cron and pg_net in Supabase Dashboard → Database → Extensions
-- 2. Deploy the Edge Function (supabase functions deploy recalculate-difficulty)
-- 3. Replace YOUR_PROJECT with your project ref
-- 4. Replace YOUR_SERVICE_ROLE_KEY with your service role key
-- 5. Run the uncommented SQL in the Supabase SQL editor
