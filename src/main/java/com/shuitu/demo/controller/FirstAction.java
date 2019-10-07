package com.shuitu.demo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.shuitu.demo.service.INamedService;
import com.shuitu.demo.service.IService;
import com.shuitu.framework.annotation.STAutowired;
import com.shuitu.framework.annotation.STController;
import com.shuitu.framework.annotation.STRequestMapping;
import com.shuitu.framework.annotation.STRequestParam;
import com.shuitu.framework.annotation.STResponseBody;

@STController
@STRequestMapping("/web")
public class FirstAction {

	@STAutowired private IService service;
	
	@STAutowired("myName") private INamedService namedService;
	
	
	@STRequestMapping("/query/.*.json")
	@STResponseBody
	public String query(HttpServletRequest request,HttpServletResponse response, 
			@STRequestParam(value="name",required=false) String name,
			@STRequestParam("addr") String addr){
		out(response,"get params name = " + name);
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("name", name);
		model.put("addr", addr);
		return "";
	}
	
	
	@STRequestMapping("/add.json")
	public String add(HttpServletRequest request,HttpServletResponse response){
		out(response,"this is json string");
		return null;
	}
	
	
	
	public void out(HttpServletResponse response,String str){
		try {
			response.getWriter().write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
