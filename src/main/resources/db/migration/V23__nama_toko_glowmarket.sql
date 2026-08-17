-- Nama toko di header, email, dan invoice mengikuti pengaturan ini.
UPDATE public.app_settings
SET setting_value = 'GlowMarket',
    updated_at = NOW()
WHERE setting_key = 'store.name';
