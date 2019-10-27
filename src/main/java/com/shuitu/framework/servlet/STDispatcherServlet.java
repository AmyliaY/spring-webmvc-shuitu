package com.shuitu.framework.servlet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.shuitu.framework.annotation.STController;
import com.shuitu.framework.annotation.STRequestMapping;
import com.shuitu.framework.annotation.STRequestParam;
import com.shuitu.framework.context.STApplicationContext;

/**
 * @author 全恒
 */
public class STDispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = -883142951552854915L;

	private static final String LOCATION = "contextConfigLocation";

	/** URL 和 handler相对应，handler持有method和其对应的controller */
	private Map<String, Handler> handlerMapping = new HashMap<String, Handler>();

	private Map<Handler, HandlerAdapter> adapterMapping = new ConcurrentHashMap<Handler, HandlerAdapter>();

	/**
	 * 初始化IoC容器
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {

		// 先初始化IoC容器
		STApplicationContext context = new STApplicationContext(config.getInitParameter(LOCATION));

		// 请求解析
		initMultipartResolver(context);
		// 多语言，国际化
		initLocaleResolver(context);
		// 主题view层
		initThemeResolver(context);

		// --------SpringMVC核心实现部分------------
		// 解析url和Method的对应关系
		initHandlerMappings(context);
		// 适配器匹配
		initHandlerAdapters(context);
		// ----------------------------------------

		// 异常解析
		initHandlerExceptionResolvers(context);
		// 视图转发，根据视图名字匹配到一个具体模板
		initRequestToViewNameTranslator(context);
		// 解析模板中的内容
		initViewResolvers(context);

		initFlashMapManager(context);

		System.out.println("STSpringMVC 已完成初始化");
	}

	private void initFlashMapManager(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	private void initViewResolvers(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	private void initRequestToViewNameTranslator(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	private void initHandlerExceptionResolvers(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	// 适配器匹配，将request的请求参数自动赋值到要调用的method方法的参数上
	// （此参数必须加上RequestParam注解才能完成参数值的自动注入）
	private void initHandlerAdapters(STApplicationContext context) {

		if (handlerMapping.isEmpty()) {
			return;
		}

		// 方法中的参数是有序的，但通过反射无法拿到形参名，所以在Map中通过integer标识序号
		// 保存参数的序号是为了通过反射调用方法时能够为方法赋值
		Map<String, Integer> paramMapping = new HashMap<String, Integer>();

		// 获取方法的参数并缓存起来
		for (Entry<String, Handler> entry : handlerMapping.entrySet()) {

			// 获取此Handler的method中的所有参数类型
			Class<?>[] paramTypes = entry.getValue().method.getParameterTypes();

			// 装载request和response参数
			for (int i = 0; i < paramTypes.length; i++) {
				if (paramTypes[i] == HttpServletRequest.class || paramTypes[i] == HttpServletResponse.class) {
					paramMapping.put(paramTypes[i].getName(), i);
				}
			}

			// 装载加了RequestParam注解的参数
			Annotation[][] annotations = entry.getValue().method.getParameterAnnotations();
			for (int j = 0; j < annotations.length; j++) {
				for (Annotation anno : annotations[j]) {
					if (anno instanceof STRequestParam) {
						String paramName = ((STRequestParam) anno).value();
						if (!"".equals(paramName.trim())) {
							paramMapping.put(paramName, j);
						}
					}
				}
			}
			adapterMapping.put(entry.getValue(), new HandlerAdapter(paramMapping));
		}
	}

	/**
	 * 将请求的URL与要执行的Method方法相关联
	 * 
	 * @param context
	 */
	private void initHandlerMappings(STApplicationContext context) {

		Map<String, Object> ioc = context.getAll();
		if (ioc.isEmpty()) {
			return;
		}
		// 遍历出所有加了RequestMapping注解的bean
		for (Entry<String, Object> entry : ioc.entrySet()) {
			Object bean = entry.getValue();
			if (!bean.getClass().isAnnotationPresent(STController.class)) {
				return;
			}
			// 加在类上的URL前缀
			String prefixUrl = bean.getClass().getAnnotation(STRequestMapping.class).value();
			// 遍历bean中的method
			Method[] methods = bean.getClass().getDeclaredMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(STRequestMapping.class)) {
					continue;
				}
				// 加在方法上的URL后缀
				String suffixUrl = method.getAnnotation(STRequestMapping.class).value();
				// 将url 和Handler( bean（Controller），Method）相关联
				handlerMapping.put(prefixUrl + suffixUrl, new Handler(bean, method));
			}
		}
	}

	private void initThemeResolver(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	private void initLocaleResolver(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	private void initMultipartResolver(STApplicationContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	/**
	 * 调用自己的Controller中的方法
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Http请求过来了由doDispatcher方法进行分发
		// Tomcat的设计是 一个线程对应一个请求
		try {
			doDispatcher(req, resp);
		} catch (Exception e) {
			resp.getWriter().write("500 Exception，Msg：" + Arrays.toString(e.getStackTrace()));
		}
	}

	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		// 从handlerMapping中取出Handler
		Handler handler = getHandler(req);
		if (handler == null) {
			resp.getWriter().write("404 Not Found");
			return;
		}

		// 再根据handler获取adapter
		HandlerAdapter adapter = getHandlerAdapter(handler);

		// 最后由适配器调用具体的方法
		adapter.handle(req, resp, handler);
	}

	private HandlerAdapter getHandlerAdapter(Handler handler) {
		return adapterMapping.get(handler);
	}

	private Handler getHandler(HttpServletRequest req) {
		if (handlerMapping.isEmpty()) {
			return null;
		}
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");

		return handlerMapping.get(url);
	}

	private class Handler {

		protected Object controller;
		protected Method method;

		protected Handler(Object controller, Method method) {
			this.controller = controller;
			this.method = method;
		}

	}

	/**
	 * 方法适配器，为每一个method适配其参数类型，便于method的反射调用
	 * 
	 * @author 全恒
	 */
	private class HandlerAdapter {

		// 注解参数名及其对应的位置
		Map<String, Integer> paramMapping;

		public HandlerAdapter(Map<String, Integer> paramMapping) {
			this.paramMapping = paramMapping;
		}

		/**
		 * 用反射调用url对应的method方法 将request中请求的参数值自动注入到加了RequestParam注解的参数上
		 * 
		 * @param req
		 * @param resp
		 * @param handler
		 * @throws InvocationTargetException
		 * @throws IllegalArgumentException
		 * @throws IllegalAccessException
		 */
		public void handle(HttpServletRequest req, HttpServletResponse resp, Handler handler)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

			Class<?>[] paramTypes = handler.method.getParameterTypes();
			Object[] paramValues = new Object[paramTypes.length];

			// 该返回值记录着前端（如jsp页面）所提交请求中的请求参数和请求参数值的映射关系
			Map<String, String[]> params = req.getParameterMap();
			for (Entry<String, String[]> param : params.entrySet()) {
				// paramMapping中是否包含该请求参数
				if (!this.paramMapping.containsKey(param.getKey())) {
					continue;
				}
				String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
				int paramIndex = this.paramMapping.get(param.getKey());

				// request中获取到的参数值都是String类型的，
				// 这里根据method的参数类型对参数值进行转换
				paramValues[paramIndex] = castStringValue(value, paramTypes[paramIndex]);
			}

			// 注入request和response的值，如果方法中有这俩参数的话
			if (paramMapping.containsKey(HttpServletRequest.class.getName())) {
				int reqIndex = paramMapping.get(HttpServletRequest.class.getName());
				paramValues[reqIndex] = req;
			}
			if (paramMapping.containsKey(HttpServletResponse.class.getName())) {
				int respIndex = paramMapping.get(HttpServletResponse.class.getName());
				paramValues[respIndex] = resp;
			}

			handler.method.invoke(handler.controller, paramValues);
		}

		// 将value转化成给定类型
		private Object castStringValue(String value, Class<?> clazz) {
			if (clazz == String.class) {
				return value;
			} else if (clazz == Integer.class) {
				return Integer.parseInt(value);
			} else if (clazz == int.class) {
				return Integer.valueOf(value).intValue();
			} else {
				return null;
			}
		}

	}

}
