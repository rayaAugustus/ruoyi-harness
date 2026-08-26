package com.ruoyi.harness.adapter.menu;

import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.harness.api.AppDescriptor;
import com.ruoyi.harness.adapter.config.HarnessProperties;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.service.ISysMenuService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class HarnessMenuSynchronizer {
    private final ISysMenuService service; private final SysMenuMapper mapper; private final HarnessProperties properties;
    public HarnessMenuSynchronizer(ISysMenuService service,SysMenuMapper mapper,HarnessProperties properties){this.service=service;this.mapper=mapper;this.properties=properties;}
    public void synchronize(AppDescriptor app,String actor){String path="app/"+app.appKey();String route="HarnessApp"+camel(app.appKey());List<SysMenu> existing=mapper.selectMenusByPathOrRouteName(path,route);
        SysMenu menu=existing.isEmpty()?new SysMenu():existing.get(0);menu.setMenuName(app.routeTitle());menu.setParentId(properties.getMenuParentId());menu.setOrderNum(app.orderNum()==null?0:app.orderNum());
        menu.setPath(path);menu.setComponent("harness/runtime/index");menu.setRouteName(route);menu.setQuery(null);menu.setIsFrame("1");menu.setIsCache("1");menu.setMenuType("C");menu.setVisible("0");menu.setStatus(app.enabled()?"0":"1");menu.setPerms(app.requiredPermission());menu.setIcon(app.icon()==null?"component":app.icon());
        if(menu.getMenuId()==null){menu.setCreateBy(actor);service.insertMenu(menu);}else{menu.setUpdateBy(actor);service.updateMenu(menu);}}
    private static String camel(String key){StringBuilder b=new StringBuilder();for(String part:key.split("-"))if(!part.isEmpty())b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));return b.toString();}
}
