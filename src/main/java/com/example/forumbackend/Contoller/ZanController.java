package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.Normal.ForumResource;
import com.example.forumbackend.Domain.Normal.Zan;
import com.example.forumbackend.Service.ResourceService;
import com.example.forumbackend.Service.ZanService;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/zan")
@Api(tags = "点赞相关api(测试完成)")
public class ZanController {
    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ZanService zanService;

    @Autowired
    private CookieUtil cookieUtil;

    @GetMapping("/addzan")
    @ApiOperation(value = "点赞某资源")
    @Transactional
    public ResponseResult<Zan> Zan_Resource(HttpServletRequest request, HttpServletResponse response, Integer rid){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer uid=cookieUtil.getuid(request);
        ForumResource resource=resourceService.findresourceByrid(rid);
        if(resource==null)
            return Response.makeRsp(ResultCode.RESOURCE_NOT_EXIST.code, "点赞资源不存在");
        if(zanService.findByRID_UID(rid,uid)!=null){
            return Response.makeRsp(ResultCode.RESOURCE_ZAN_ALREADY.code, "此资源已被用户此用户点赞");
        }
        Zan zan=resourceService.Add_Zan(rid,uid);
        return Response.makeOKRsp(zan);
    }

    @GetMapping("/deletezan")
    @ApiOperation(value = "取消点赞")
    @Transactional
    public  ResponseResult<Zan> deletezan(HttpServletRequest request,HttpServletResponse response,Integer rid){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ForumResource resource=resourceService.findresourceByrid(rid);
        if(resource==null)
            return Response.makeRsp(ResultCode.RESOURCE_NOT_EXIST.code, "点赞资源不存在");
        if(zanService.findByRID_UID(rid,uid)==null){
            return Response.makeRsp(ResultCode.RESOURCE_NOT_ZAN_YET.code, "此资源用户尚未点赞");
        }
        resourceService.SUB_ZAN(rid,uid);
        return Response.makeOKRsp();
    }

    @GetMapping("/ifzan")
    @ApiOperation(value = "查询用户是否点赞了某资源")
    public ResponseResult<Boolean> if_zann(HttpServletRequest request,HttpServletResponse response,Integer rid){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Zan zan=zanService.findByRID_UID(rid,uid);
        return Response.makeOKRsp(zan != null);
    }

    @GetMapping("/getzancount")
    @ApiOperation(value = "获得资源的赞数量")
    public ResponseResult<Integer> getrzan(Integer rid,HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ForumResource forumResource=resourceService.findresourceByrid(rid);
        return Response.makeOKRsp(forumResource==null?0:forumResource.getZan());
    }
    @GetMapping("/getzans")
    @ApiOperation(value = "获得具体点赞信息")
    public ResponseResult<List<Zan>> getrzans(Integer rid,Integer pageindex,Integer pagesize,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true"); response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(zanService.getzansbyRID(rid,pageindex,pagesize));
    }
}
