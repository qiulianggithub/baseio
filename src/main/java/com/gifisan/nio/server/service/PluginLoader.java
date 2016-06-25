package com.gifisan.nio.server.service;

import java.util.List;

import com.gifisan.nio.AbstractLifeCycle;
import com.gifisan.nio.LifeCycle;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.common.LoggerUtil;
import com.gifisan.nio.component.ApplicationContext;
import com.gifisan.nio.component.Configuration;
import com.gifisan.nio.component.DynamicClassLoader;
import com.gifisan.nio.component.HotDeploy;
import com.gifisan.nio.component.PluginContext;
import com.gifisan.nio.server.configuration.PluginsConfiguration;

public class PluginLoader extends AbstractLifeCycle implements HotDeploy, LifeCycle {

	private ApplicationContext	context		= null;
	private DynamicClassLoader	classLoader	= null;
	private Logger				logger		= LoggerFactory.getLogger(PluginLoader.class);
	private PluginContext[]		pluginContexts	= new PluginContext[4];
	private PluginsConfiguration	configuration	= null;

	public PluginLoader(ApplicationContext context, DynamicClassLoader classLoader) {
		this.configuration = context.getConfiguration().getPluginsConfiguration();
		this.context = context;
		this.classLoader = classLoader;
	}

	protected void doStart() throws Exception {

		loadPlugins(context, this.classLoader, this.configuration);

		this.initializePlugins(pluginContexts);

		this.configPluginFilterAndServlet((ApplicationContext) context);
	}

	protected void doStop() throws Exception {

		for (PluginContext plugin : pluginContexts) {

			if (plugin == null) {
				continue;
			}

			try {

				plugin.destroy(context, plugin.getConfig());
				
				LoggerUtil.prettyNIOServerLog(logger, "卸载完成 [ {} ]",plugin);

			} catch (Throwable e) {

				logger.error(e.getMessage(), e);
			}
		}
	}

	public PluginContext[] getPluginContexts() {
		return pluginContexts;
	}

	private void initializePlugins(PluginContext[] plugins) throws Exception {

		for (PluginContext plugin : plugins) {

			if (plugin == null) {
				continue;
			}

			plugin.initialize(context, plugin.getConfig());

			LoggerUtil.prettyNIOServerLog(logger, "加载完成 [ {} ]", plugin);
		}
	}

	private void loadPlugins(ApplicationContext context, DynamicClassLoader classLoader,
			PluginsConfiguration configuration) throws Exception {

		List<Configuration> plugins = configuration.getPlugins();

		if (plugins.size() > 4) {
			throw new IllegalArgumentException("plugin size max to 4");
		}

		for (int i = 0; i < plugins.size(); i++) {

			Configuration config = plugins.get(i);

			String className = config.getParameter("class", "empty");

			Class<?> clazz = classLoader.forName(className);

			PluginContext plugin = (PluginContext) clazz.newInstance();

			pluginContexts[i] = plugin;

			plugin.setConfig(config);

		}
	}

	public void prepare(ApplicationContext context, Configuration config) throws Exception {

		LoggerUtil.prettyNIOServerLog(logger, "尝试加载新的Plugin配置......");

		loadPlugins(context, classLoader, this.configuration);

		LoggerUtil.prettyNIOServerLog(logger, "尝试启动新的Plugin配置......");

		this.prepare(context, pluginContexts);

		this.softStart();

	}

	private void prepare(ApplicationContext context, PluginContext[] plugins) throws Exception {

		for (PluginContext plugin : plugins) {

			if (plugin == null) {
				continue;
			}

			plugin.prepare(context, plugin.getConfig());

			LoggerUtil.prettyNIOServerLog(logger, "新的Plugin [ {} ] Prepare完成", plugin);

		}

		this.configPluginFilterAndServlet((ApplicationContext) context);
	}

	public void unload(ApplicationContext context, Configuration config) throws Exception {

		for (PluginContext plugin : pluginContexts) {

			if (plugin == null) {
				continue;
			}

			try {

				plugin.unload(context, plugin.getConfig());

				LoggerUtil.prettyNIOServerLog(logger, "旧的Plugin [ {} ] Unload完成", plugin);

			} catch (Throwable e) {

				LoggerUtil.prettyNIOServerLog(logger, "旧的Plugin [ {} ] Unload失败", plugin);

				logger.error(e.getMessage(), e);

			}

		}
	}

	private void configPluginFilterAndServlet(ApplicationContext context) {

		for (PluginContext pluginContext : pluginContexts) {

			if (pluginContext == null) {
				continue;
			}

			pluginContext.configFutureAcceptorFilter(context.getPluginFilters());
			pluginContext.configFutureAcceptor(context.getPluginServlets());
		}
	}

}