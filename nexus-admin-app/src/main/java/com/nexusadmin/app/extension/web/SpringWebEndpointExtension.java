package com.nexusadmin.app.extension.web;

import com.nexusadmin.api.extension.web.WebControllerProvider;
import com.nexusadmin.api.extension.web.WebEndpointExtension;
import com.nexusadmin.core.extension.Extension;

/**
 * Spring MVC 下的 Web 接入扩展点实现。
 * <p>
 * 负责在插件激活时，将插件提供的控制器注册到 Spring MVC，
 * 并在插件停止前卸载所有已注册的端点。
 */
@Extension(priority = 50)
public class SpringWebEndpointExtension implements WebEndpointExtension {

    @Override
    public void registerEndpoints(Context ctx) {
        WebControllerProvider provider = ctx.controllerProvider();
        if (provider == null) {
            return;
        }

        for (Object controller : provider.getControllers()) {
            ctx.registrar().register(ctx.pluginId(), controller);
        }
    }

    @Override
    public void unregisterEndpoints(Context ctx) {
        ctx.registrar().unregister(ctx.pluginId());
    }
}
