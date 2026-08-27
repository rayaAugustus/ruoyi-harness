package com.ruoyi.harness.adapter.menu;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.harness.api.AppDescriptor;
import com.ruoyi.harness.core.port.AppLifecycleListener;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adapts Harness application changes to the host's navigation model. */
@Component
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class RuoYiAppLifecycleListener implements AppLifecycleListener {
    private final HarnessMenuSynchronizer menus;
    private final ISysUserService users;

    public RuoYiAppLifecycleListener(HarnessMenuSynchronizer menus, ISysUserService users) {
        this.menus = menus;
        this.users = users;
    }

    @Override
    public void onChanged(AppDescriptor app, Long actorId) {
        SysUser actor = actorId == null ? null : users.selectUserById(actorId);
        menus.synchronize(app, actor == null ? String.valueOf(actorId) : actor.getUserName());
    }
}
