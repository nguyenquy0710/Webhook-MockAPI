-- Key/value store for admin-editable settings (Admin > Settings)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(1000)
);
