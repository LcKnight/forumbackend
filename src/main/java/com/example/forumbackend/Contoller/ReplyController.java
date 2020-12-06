package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.ForumResource;
import com.example.forumbackend.Domain.Reply;
import com.example.forumbackend.Service.ReplyService;
import com.example.forumbackend.Service.ResourceService;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/reply")
@Api(tags = "回复API")
public class ReplyController {
    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private CookieUtil cookieUtil;

    @PostMapping("/replyresource")
    @Transactional
    public ResponseResult<Reply> replyresource(HttpServletRequest request,Integer rid,String content){
        ForumResource resource=resourceService.findresourceByrid(rid);
        System.out.println(resource);
        if(resource==null){
            return Response.makeRsp(ResultCode.RESOURCE_NOT_EXIST.code,"resource_id为"+rid+"的资源不存在");
        }
        replyService.addreply(content,rid,cookieUtil.getuid(request));
        return Response.makeOKRsp();
    }
}
