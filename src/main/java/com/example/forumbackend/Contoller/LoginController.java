package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.Utils.Role;
import com.example.forumbackend.Domain.Normal.User;
import com.example.forumbackend.Service.LoginService;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/login")
@Api(tags = "登录登出(测试完成)")
public class LoginController {
    @Autowired
    private LoginService loginService;


    @Value("${PasswordWrong}")
    private  String PasswordWrong;

    @Value("${AccountNotExist}")
    private String AccountNotExist;

    @Value("${cookie.timeout}")
    private Integer cookie_timeout;

    @Autowired
    private CookieUtil cookieUtil;


    @GetMapping("/in")
    @ApiOperation(value = "登录")
    @Transactional
    @ApiResponses({
            @ApiResponse(code = 104,message = "账号错误"),
            @ApiResponse(code=103,message = "密码错误"),
            @ApiResponse(code=102,message = "成功登录")
    })
    public ResponseResult<User> logintest(
            HttpServletResponse response,
            HttpServletRequest request,
            @ApiParam(value = "账号",example = "513317651") @RequestParam String account,
            @ApiParam(value = "密码",example = "zz123456") @RequestParam String password){
        Role role=new Role();
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        String token=loginService.login(account,password,role);
        System.out.println("token:\t"+token);
        System.out.println("test account"+AccountNotExist);
        if(AccountNotExist.equals(token)){
            return Response.makeRsp(ResultCode.ACCOUNTNOTEXIST.code,"账号错误");
        }
        else if(PasswordWrong.equals(token)){
            return Response.makeRsp(ResultCode.PASSWORDWRONG.code, "密码错误");
        }
        else{
            Cookie cookie=new Cookie("token",token);
            cookie.setPath("/");
            cookie.setMaxAge(cookie_timeout*60);
            response.addCookie(cookie);
            Cookie cookie1=new Cookie("UID",role.getUser().getUID().toString());
            cookie1.setMaxAge(cookie_timeout*60);
            cookie1.setPath("/");
            response.addCookie(cookie1);
            return Response.makeOKRsp(role.getUser());
        }
    }


    @GetMapping("/out")
    @ApiOperation("登出")
    @ApiResponses({
            @ApiResponse(code=102,message = "成功登出")
    })
    public ResponseResult<User> logout(HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uid=cookieUtil.getuid(request);
        loginService.logout(uid);
        return Response.makeRsp(ResultCode.LOGOUT_SUCCESS.code, "成功登出");
       }
}
