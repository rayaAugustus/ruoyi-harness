package com.ruoyi.harness.adapter.security;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.harness.api.RuntimeIdentity;

public class RuoYiIdentityAdapter {
    public RuntimeIdentity current(){LoginUser user=SecurityUtils.getLoginUser();return new RuntimeIdentity(user.getUserId(),user.getUsername(),user.getPermissions());}
}
