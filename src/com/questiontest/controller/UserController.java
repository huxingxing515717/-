package com.questiontest.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.questiontest.entity.ResponseMessage;
import com.questiontest.entity.ResponseObject;
import com.questiontest.entity.User;
import com.questiontest.service.UserDaoService;
import com.questiontest.util.RandomString;
import com.questiontest.util.SendMail;

@Controller
public class UserController {
	@Autowired
	private  UserDaoService service;
	
	private static ExecutorService s1 = Executors.newFixedThreadPool(100);
	private static ExecutorService s2 = Executors.newFixedThreadPool(100);
	
	/*
	 *用户登录 
	 */
	@RequestMapping(value = "login.action",method = RequestMethod.POST)
	public  @ResponseBody ResponseMessage login(@Param(value = "username")String username,@Param(value = "password")String password,HttpServletRequest request) {
		User user =service.getUser(username);
		ResponseMessage message = new ResponseMessage();
		if(user!=null&&user.getPassword().equals(password)) {
			message.setFlag(true);
			message.setMessage("登陆成功");
			request.getSession().setAttribute("user", user);
		}else if(user==null){
			message.setFlag(false);
			message.setMessage("用户名不存在");
		}else if(user.getPassword()==null||!user.getPassword().equals(password)){
			message.setMessage("密码错误");
			message.setFlag(false);
		}
		return message;
	}
	
	/*
	 * 用户注册
	 */
	@RequestMapping(value = "zhuce.action",method = RequestMethod.POST)
	public @ResponseBody ResponseMessage reginster(@Param(value="user") User user,HttpServletRequest request) {
		user.setRandomcode(RandomString.getRandomString(7));
		int flag=service.insertUser(user);
		ResponseMessage message = new ResponseMessage();
		if(flag==-1) {
			message.setFlag(false);
			message.setMessage("注册失败");
		}else {
			message.setFlag(true);
			message.setMessage("注册成功");
			user.setId(flag);
			request.getSession().setAttribute("user", user);
		}
		return message;
	}
	
	/*
	 * 判断用户是否存在
	 */
	@RequestMapping(value="isexist.action",method = RequestMethod.POST)
	public @ResponseBody ResponseMessage isexist(@Param(value="username") String username) {
		boolean flag =service.isextist(username);
		ResponseMessage message = new ResponseMessage();
		message.setFlag(!flag);
		if(flag) {
			message.setMessage("用户名存在");
		}else {
			message.setMessage("可以注册");
		}
		return message;
	}
	
	/*
	 * 更新用户信息
	 */
	@RequestMapping(value="updata.action",method = RequestMethod.POST)
	public @ResponseBody ResponseMessage updata(@Param("user") User user,HttpServletRequest request) {
		User user2 = (User) request.getSession().getAttribute("user");
		user.setId(user2.getId());
		user.setUsername(user2.getUsername());
		user.setPassword(user2.getPassword());
		System.err.println(user);
		boolean flag=service.updata(user,request);
		ResponseMessage message = new ResponseMessage();
		message.setFlag(flag);
		if(flag) {
			request.getSession().removeAttribute("user");
			request.getSession().setAttribute("user", user);
			message.setMessage("更新成功");
		}else {
			message.setMessage("更新失败");
		}
		return message;
	}
	
	/*
	 * 获取用户
	 */
	@RequestMapping(value="getuser.action",method = RequestMethod.POST)
	public @ResponseBody ResponseObject getUser(HttpServletRequest request) {
		Object user = request.getSession().getAttribute("user");
		ResponseObject obj = new ResponseObject();
		obj.setFlag(true);
		obj.setObj(user);
		return obj;
	}
	
	/*
	 * 用户登出
	 */
	@RequestMapping(value="dropuser.action",method =RequestMethod.POST)
	public @ResponseBody ResponseMessage dropUser(HttpServletRequest request) {
		ResponseMessage message = new ResponseMessage();
		request.getSession().removeAttribute("user");
		message.setFlag(true);
		message.setMessage("登出成功");
		return message;
	}
	
	/*
	 * 更新密码
	 */
	@RequestMapping(value="updatepassword.action",method = RequestMethod.POST)
	public @ResponseBody ResponseMessage updatapassword(String password,HttpServletRequest request) {
		ResponseMessage message = new ResponseMessage();
		User user = (User) request.getSession().getAttribute("user");
		user.setPassword(password);
		boolean flag=service.updata(user,request);
		message.setFlag(flag);
		if(flag) {
			request.getSession().removeAttribute("user");
			request.getSession().setAttribute("user", user);
			message.setMessage("更改成功");
		}else {
			message.setMessage("更改失败");
		}
		return message;
	}
	/*
	 * 发送验证邮件
	 */
	@RequestMapping(value="confireemail.action",method=RequestMethod.POST)
	public @ResponseBody ResponseMessage confireEmail(HttpServletRequest request) {
		String code = ((User)request.getSession().getAttribute("user")).getRandomcode();
		String email = ((User)request.getSession().getAttribute("user")).getEmail();
		String url2 = request.getContextPath().toString();
		String ur3 = request.getRealPath("./");
		String url = "<a href='http://47.93.61.90/QuestionTest/secendhtml/confire.html?code="+code+"'>点击验证</a>";
		s1.execute(new SendMail(email, url));
		ResponseMessage message = new ResponseMessage();
		message.setFlag(true);
		message.setMessage("我们已经向您的邮箱发送了验证消息，请及时处理");
		return message;
	}
	/*
	 * 邮件验证
	 */
	@RequestMapping(value="confire.action",method=RequestMethod.POST)
	public @ResponseBody ResponseMessage confire(String code,HttpServletRequest request) {
		boolean flag =service.confire(code);
		ResponseMessage message = new ResponseMessage();
		if(flag) {
			User user = (User) request.getSession().getAttribute("user");
			user.setConfire(1);
			request.getSession().removeAttribute("user");
			request.getSession().setAttribute("user", user);
		}			
		message.setFlag(flag);
		if(flag) {
			message.setMessage("验证成功");
		}else {
			message.setMessage("验证失败");
		}
		return message;
	}
	
	/*
	 * 重置密码发送邮箱
	 */
	@RequestMapping(value="forget.action",method =RequestMethod.POST)
	public @ResponseBody ResponseMessage forget(String username,HttpServletRequest request) {
		ResponseMessage message = new ResponseMessage();
		User user=service.getUser(username);
		if(user!=null) {
			int confire = user.getConfire();
			String email= user.getEmail();
			String password=RandomString.getRandomString(10);
			if(confire==1) {
				message.setFlag(true);
				user.setPassword(password);
				service.updata(user,request);
				s2.execute(new SendMail(email, password));
				message.setMessage("我们已经重置了您的密码，并发到您的邮箱上了");
			}else {
				message.setFlag(false);
				message.setMessage("您没有绑定邮箱，无法为您找回密码");
			}
		}else {
			message.setFlag(true);
			message.setMessage("用户名不存在");
		}
		return message;
	}
}
