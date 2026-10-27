package vn.autobot.webhook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.autobot.webhook.model.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
