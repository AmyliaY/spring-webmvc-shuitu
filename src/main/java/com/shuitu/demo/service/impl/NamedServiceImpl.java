package com.shuitu.demo.service.impl;

import com.shuitu.demo.service.INamedService;
import com.shuitu.demo.service.IService;
import com.shuitu.framework.annotation.STAutowired;
import com.shuitu.framework.annotation.STService;

@STService("myName")
public class NamedServiceImpl implements INamedService{

	@STAutowired IService service;
	
}
