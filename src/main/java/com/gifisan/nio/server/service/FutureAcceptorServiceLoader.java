package com.gifisan.nio.server.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gifisan.nio.AbstractLifeCycle;
import com.gifisan.nio.LifeCycle;
import com.gifisan.nio.common.Logger;
import com.gifisan.nio.common.LoggerFactory;
import com.gifisan.nio.common.LoggerUtil;
import com.gifisan.nio.component.ApplicationContext;
import com.gifisan.nio.component.Configuration;
import com.gifisan.nio.component.DynamicClassLoader;
import com.gifisan.nio.component.HotDeploy;
import com.gifisan.nio.server.configuration.ServletsConfiguration;

public class FutureAcceptorServiceLoader extends AbstractLifeCycle implements LifeCycle, HotDeploy {

	private ApplicationContext				context		= null;
	private DynamicClassLoader				classLoader	= null;
	private Logger							logger		= LoggerFactory
															.getLogger(FutureAcceptorServiceLoader.class);
	private Map<String, FutureAcceptorService>	services		= new LinkedHashMap<String, FutureAcceptorService>();
	private ServletsConfiguration				configuration	= null;

	public FutureAcceptorServiceLoader(ApplicationContext context, DynamicClassLoader classLoader) {
		this.configuration = context.getConfiguration().getServletsConfiguration();
		this.context = context;
		this.classLoader = classLoader;
	}

	protected void doStart() throws Exception {

		Map<String, FutureAcceptorService> servlets = loadServlets(configuration, classLoader);

		this.initializeServlets(servlets);

		this.services = servlets;

	}

	protected void doStop() throws Exception {

		Set<Entry<String, FutureAcceptorService>> entries = services.entrySet();

		for (Entry<String, FutureAcceptorService> entry : entries) {

			FutureAcceptorService servlet = entry.getValue();

			try {

				servlet.destroy(context, servlet.getConfig());

				LoggerUtil.prettyNIOServerLog(logger, "卸载完成 [ {} ]", servlet);

			} catch (Throwable e) {

				logger.error(e.getMessage(), e);
			}
		}
	}

	public FutureAcceptorService getFutureAcceptor(String serviceName) {
		return services.get(serviceName);
	}
	
	public void listen(String serviceName,FutureAcceptorService service){
		this.services.put(serviceName, service);
	}
	
	public void listen(Map<String, FutureAcceptorService> services){
		this.services.putAll(services);
	}

	private void initializeServlets(Map<String, FutureAcceptorService> servlets) throws Exception {

		Set<Entry<String, FutureAcceptorService>> entries = servlets.entrySet();

		for (Entry<String, FutureAcceptorService> entry : entries) {

			FutureAcceptorService servlet = entry.getValue();

			servlet.initialize(context, servlet.getConfig());

			LoggerUtil.prettyNIOServerLog(logger, "加载完成 [ {} ]", servlet);

		}
	}

	private Map<String, FutureAcceptorService> loadServlets(ServletsConfiguration configuration,
			DynamicClassLoader classLoader) throws Exception {

		List<Configuration> servletConfigurations = configuration.getServlets();

		Map<String, FutureAcceptorService> pluginServlets = context.getPluginServlets();

		Map<String, FutureAcceptorService> servlets = new LinkedHashMap<String, FutureAcceptorService>();

		servlets.putAll(pluginServlets);

		if (servletConfigurations.size() == 0) {

			if (servlets.size() == 0) {

				throw new Error("empty servlet config");
			}
			return servlets;
		}

		for (int i = 0; i < servletConfigurations.size(); i++) {

			Configuration config = servletConfigurations.get(i);

			String className = config.getParameter("class", "empty");

			Class<?> clazz = classLoader.forName(className);

			String serviceName = config.getParameter("serviceName", clazz.getSimpleName());

			if (servlets.containsKey(serviceName)) {
				throw new IllegalArgumentException("repeat servlet[ " + serviceName + "@" + className + " ]");
			}

			FutureAcceptorService servlet = (FutureAcceptorService) clazz.newInstance();

			servlets.put(serviceName, servlet);

			servlet.setConfig(config);

		}
		return servlets;
	}

	public void prepare(ApplicationContext context, Configuration config) throws Exception {

		LoggerUtil.prettyNIOServerLog(logger, "尝试加载新的Servlet配置......");

		this.services = loadServlets(configuration, classLoader);

		LoggerUtil.prettyNIOServerLog(logger, "尝试启动新的Servlet配置......");

		this.prepare(services);

		this.softStart();

	}

	private void prepare(Map<String, FutureAcceptorService> servlets) throws Exception {

		Set<Entry<String, FutureAcceptorService>> entries = servlets.entrySet();

		for (Entry<String, FutureAcceptorService> entry : entries) {

			FutureAcceptorService servlet = entry.getValue();

			servlet.prepare(context, servlet.getConfig());

			LoggerUtil.prettyNIOServerLog(logger, "新的Servlet [ {} ] Prepare完成", servlet);

		}
	}

	public void unload(ApplicationContext context, Configuration config) throws Exception {

		Set<Entry<String, FutureAcceptorService>> entries = services.entrySet();

		for (Entry<String, FutureAcceptorService> entry : entries) {

			FutureAcceptorService servlet = entry.getValue();

			try {

				servlet.unload(context, servlet.getConfig());

				LoggerUtil.prettyNIOServerLog(logger, "旧的Servlet [ {} ] Unload完成", servlet);

			} catch (Throwable e) {

				LoggerUtil.prettyNIOServerLog(logger, "旧的Servlet [ {} ] Unload失败", servlet);

				logger.error(e.getMessage(), e);

			}
		}
	}

}
