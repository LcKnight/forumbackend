package com.example.forumbackend.Contoller;


import com.example.forumbackend.Domain.Normal.User;
import com.example.forumbackend.Domain.Normal.User_Info;
import com.example.forumbackend.Service.UserInfoService;
import com.example.forumbackend.Service.UserService;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/user")
@Api(tags = "用户信息(测试完成)")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private CookieUtil cookieUtil;

    @Transactional
    @GetMapping("/getuser")
    @ApiOperation(value = "获取自己的账户类(测试完成)")
    public ResponseResult<User> getuser(
            HttpServletRequest request,
            HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uidincookie= cookieUtil.getuid(request);
        User user=userService.findByUID(uidincookie);
        return Response.makeOKRsp(user);
    }

    @Transactional
    @GetMapping("/getinfo")
    @ApiOperation(value = "获取User_Info信息(测试完成)")
    public  ResponseResult<User_Info> getuserinfo(HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true"); response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uid=cookieUtil.getuid(request);
        User_Info userInfo=userInfoService.findByUID(uid);
        return Response.makeOKRsp(userInfo);
    }

    @Transactional
    @GetMapping("/getuserbyuid")
    @ApiOperation(value = "根据UID获取User,隐藏密码邮箱(测试完成)")
    public ResponseResult<User> getuserbyuid(
            Integer uid,HttpServletRequest request,HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        User user=userService.findByUID(uid);
        user.setPassword(null);
        user.setEmail(null);
        return Response.makeOKRsp(user);
    }

    @Transactional
    @GetMapping("/getinfobyuid")
    @ApiOperation(value = "根据uid获取User_Info信息，隐藏余额和infoID(测试完成)")
    public  ResponseResult<User_Info> getuserinfobyuid(Integer uid,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        User_Info userInfo=userInfoService.findByUID(uid);
        userInfo.setUserBalance(null);
        userInfo.setInfoID(null);
        return Response.makeOKRsp(userInfo);
    }


    @Transactional
    @GetMapping("/singup")
    @ApiOperation(value="注册账号(已测试)")
    @ApiResponses({
            @ApiResponse(code=108,message = "账号已被注册"),
            @ApiResponse(code=109,message = "邮箱已被注册"),
            @ApiResponse(code=102,message = "成功注册")
    })
    public ResponseResult<User> singup( User user,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));

        if(userService.findByAccount(user.getAccount())!=null){
            return Response.makeRsp(
                    ResultCode.ACCOUNT_DUPLICATED.code, user.getAccount()+"账号已被注册");
        }
        else if(userService.findByEmail(user.getEmail())!=null){
            return Response.makeRsp(ResultCode.EMAIL_DUPLICATED.code, user.getEmail()+"邮箱已被注册");
        }
        else{
            userService.insertUser(user);
            User_Info userInfo=new User_Info();
            userInfo.setUserID(user.getUID());
            userInfo.setUserIntro("");
            userInfo.setUserZan(0);
            userInfo.setUserBalance(0);
            userInfo.setUserPoint(0);
            userInfoService.insertinfo(userInfo);
            return Response.makeOKRsp(user);
        }
    }

    @Transactional
    @GetMapping("/changename")
    @ApiOperation(value = "修改姓名，具体账号从cookie中获取")
    public ResponseResult<User> changename(HttpServletResponse response,HttpServletRequest request,String name){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uidincookie= cookieUtil.getuid(request);;
        userService.changeName(uidincookie,name);
        return Response.makeOKRsp();
    }

    @Transactional
    @GetMapping("/changepassword")
    @ApiOperation(value = "修改密码，账号从cookie中获取")
    public ResponseResult<User> changepassword(HttpServletResponse response,HttpServletRequest request,String password){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uidincookie= cookieUtil.getuid(request);;
        userService.changePassword(uidincookie,password);
        return Response.makeOKRsp();
    }

    @Transactional
    @GetMapping("/changeintro")
    @ApiOperation(value = "修改个人简介，账号从cookie中获取")
    public ResponseResult<User_Info> changeintro(HttpServletResponse response,HttpServletRequest request,String intro){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uidincookie= cookieUtil.getuid(request);
        System.out.println("uid:"+ uidincookie.toString()+"\tintro:"+intro);
        userInfoService.addintro(uidincookie,intro);
        return Response.makeOKRsp("修改信息成功");
    }

    @Transactional
    @GetMapping("/getrankbyuid")
    @ApiResponses({
            @ApiResponse(code = 124,message = "查询用户不存在"),
            @ApiResponse(code = 102,message = "成功查询")
    })
    @ApiOperation(value = "获取某用户的排名")
    public ResponseResult<Integer> getrankbyuid(Integer uid,HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        User_Info tmp=userInfoService.findByUID(uid);
        if(tmp==null)
            return Response.makeRsp(ResultCode.USER_NOT_EXIST.code, "查询用户不存在");
        List<User_Info> userInfos=userInfoService.findAlldesc();
        int i=0;
        for(i=0;i< userInfos.size();i++){
            if(userInfos.get(i).getUserID().equals(uid))
                break;
        }
        return Response.makeOKRsp(i+1);

    }

    @Transactional
    @GetMapping("/getranks")
    @ApiOperation(value = "获取排行榜某页所有人的UID")
    public ResponseResult<List<Integer>> getranks(Integer pageindex,Integer pagesize,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        List<User_Info> userInfos=userInfoService.finddescbypage(pageindex,pagesize);
        List<Integer> integerList = new ArrayList<>();
        for(User_Info userInfo:userInfos){
            integerList.add(userInfo.getUserID());
        }
        return Response.makeOKRsp(integerList);
    }
}
