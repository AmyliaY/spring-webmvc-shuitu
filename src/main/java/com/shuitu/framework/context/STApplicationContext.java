package com.shuitu.framework.context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.shuitu.framework.annotation.STAutowired;
import com.shuitu.framework.annotation.STController;
import com.shuitu.framework.annotation.STService;

/**
 * @author 全恒 简化版IoC容器 不从配置文件中扫描加载bean 从注解中实例化bean并完成依赖注入
 */
public class STApplicationContext {

	/** IoC容器 */
	private Map<String, Object> ioc = new ConcurrentHashMap<String, Object>();

	/** 缓存从指定目录扫描出的 “包名.类名” */
	private List<String> clazzCache = new ArrayList<String>();

	public STApplicationContext(String location) {
		// 1、资源定位(加载配置文件)
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);

		// 2、载入配置文件
		Properties config = new Properties();
		try {
			config.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 3、注册，扫描指定包路径下的所有类（.class）并缓存
		String packageName = config.getProperty("scanPackage");
		doRegister(packageName);

		// 4、初始化，将缓存的 特定类（加了Controller和Service注解）进行实例化
		doCreateBean();

		// 5、依赖注入（根据Autowired注解来判断）
		populate();
	}

	// 把packageName下的符合条件的类全部扫描出来 放到缓存中
	private void doRegister(String packageName) {
		// 获取指定文件路径
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));

		File dir = new File(url.getFile());
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				doRegister(packageName + "." + file.getName());
			} else {
				clazzCache.add(packageName + "." + file.getName().replace(".class", ""));
			}
		}
	}

	// 将doRegister扫描出来的bean进行实例化
	// 只实例化加了Controller和Service注解的类
	private void doCreateBean() {
		if (ioc.isEmpty()) {
			return;
		}
		try {
			for (String clazzName : clazzCache) {
				Class clazz = Class.forName(clazzName);
				if (clazz.isAnnotationPresent(STController.class) || clazz.isAnnotationPresent(STService.class)) {
					// beanId默认为类名首字母小写
					String beanId = lowerFirstChar(clazz.getName());
					ioc.put(beanId, clazz.newInstance());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将类名首字母小写
	 * @param name
	 * @return
	 */
	private String lowerFirstChar(String name) {
		char[] chars = name.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	/**
	 * 依赖注入
	 * 扫描IOC容器中的bean，将加了Autowired注解的属性注入值
	 */
	private void populate() {

		if (ioc.isEmpty()) { return; }

		for (Entry<String, Object> entry : ioc.entrySet()) {
			// 获取所有属性
			Object bean = entry.getValue();
			Field[] fields = bean.getClass().getDeclaredFields();
			// 属性是否加了Autowired注解，对加了注解的注入值
			for (Field field : fields) {
				if (!field.isAnnotationPresent(STAutowired.class)) {
					continue;
				}
				// 注入属性值
				String beanId = lowerFirstChar(field.getType().getName());
				// 开放访问权限
				field.setAccessible(true);
				try {
					field.set(bean, ioc.get(beanId));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
	}

	public Object getBean(String name) {
		return ioc.get(name);
	}

	public Map<String, Object> getAll() {
		return ioc;
	}

}
