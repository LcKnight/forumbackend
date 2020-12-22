package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.Normal.ForumResource;
import com.example.forumbackend.Domain.Normal.Purchase;
import com.example.forumbackend.Domain.Normal.User_Info;
import com.example.forumbackend.Service.PurchaseService;
import com.example.forumbackend.Service.ResourceService;
import com.example.forumbackend.Service.UpFileService;
import com.example.forumbackend.Service.UserInfoService;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/purchase")
@Api(tags = "购买资源")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UpFileService upFileService;

    @Autowired
    private CookieUtil cookieUtil;

    @Value("${resourcetype.artical}")
    private Integer articaltype;

    @GetMapping("/purchase")
    @Transactional
    @ApiOperation(value = "购买资源")
    @ApiResponses({
            @ApiResponse(code = 112,message = "请求资源不存在"),
            @ApiResponse(code = 117,message = "此资源已购买"),
            @ApiResponse(code=116,message = "余额不足"),
            @ApiResponse(code = 125,message = "文章不需要购买"),
            @ApiResponse(code=102,message = "成功购买")
    })
    public ResponseResult<Purchase> pur(HttpServletRequest request, HttpServletResponse response,
                                        @RequestParam @ApiParam(value = "购买资源的RID") Integer rid){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ForumResource resource=resourceService.findresourceByrid(rid);
        if(resource==null)
            return Response.makeRsp(ResultCode.RESOURCE_NOT_EXIST.code, "请求资源不存在");
        if(resource.getType().equals(articaltype))
            return Response.makeRsp(ResultCode.ARTICAL_CANNOT_PURCHASE.code, "无法购买文章");
        User_Info info=userInfoService.findByUID(uid);
        if(purchaseService.findByUIDRID(uid,rid)!=null){
            return  Response.makeRsp(ResultCode.RESOURCE_PURCHASED.code, "此资源已购买");
        }
        if(info.getUserBalance()<resource.getPrice()){
            return Response.makeRsp(ResultCode.BALANCE_NOT_ENOUGH.code, "余额不足");
        }
        info.setUserBalance(info.getUserBalance()-resource.getPrice());
        userInfoService.update(info);//修改用户余额
        Purchase purchase=new Purchase();
        purchase.setUID(uid);
        purchase.setRID(rid);
        purchase.setPrice(resource.getPrice());
        purchase.setPurchaseTime(LocalDateTime.now());
        purchaseService.addpur(purchase);
        upFileService.add_purchasetime(resource.getRID());
        return Response.makeOKRsp(purchase);
    }

    @GetMapping("/countbyuid")
    @Transactional
    @ApiOperation("获取请求用户购买过的资源数量")
    public ResponseResult<Integer> getcountbyuid(HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uid=cookieUtil.getuid(request);
        return Response.makeOKRsp(purchaseService.getcountbyuid(uid));
    }

    @GetMapping("/getpurchasesbyuid")
    @Transactional
    @ApiOperation(value = "获取用户的购买记录")
    public ResponseResult<List<Purchase>> getpurchasesbyuid(
            HttpServletRequest request,HttpServletResponse response,
            @RequestParam @ApiParam(value = "页索引") Integer pageindex,
            @RequestParam @ApiParam(value = "页号码")Integer pagesize){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(purchaseService.getpurchasesbyuid(uid,pageindex,pagesize));
    }

    @GetMapping("/getpurchasesbyrid")
    @Transactional
    @ApiResponses({
            @ApiResponse(code = 115,message ="请求资源不存在" ),
            @ApiResponse(code=120,message ="请求资源不属于此用户上传" ),
            @ApiResponse(code = 102,message = "请求成功")
    })
    @ApiOperation(value = "获取用户发布的某资源的购买记录")
    public ResponseResult<List<Purchase>> getpurchasesbyrid(
            HttpServletRequest request,HttpServletResponse response,
            @RequestParam @ApiParam(value = "资源RID")Integer rid,
            @RequestParam @ApiParam(value = "页索引") Integer pageindex,
            @RequestParam @ApiParam(value = "页号码")Integer pagesize){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ForumResource resource=resourceService.findresourceByrid(rid);
        if(resource==null)
            return  Response.makeRsp(ResultCode.RESOURCE_NOT_EXIST.code, "请求资源不存在");
        else if(!resource.getUID().equals(uid))
            return Response.makeRsp(ResultCode.RESOURCE_NOT_BELONGS_TO_YOU.code, "请求资源不属于此用户上传");
        return Response.makeOKRsp(purchaseService.getpurchasesbyrid(uid,pageindex,pagesize));
    }
}
