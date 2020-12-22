package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.ChatRoom.Chat;
import com.example.forumbackend.Domain.ChatRoom.ChatGroup;
import com.example.forumbackend.Domain.ChatRoom.GroupItem;
import com.example.forumbackend.Service.ChatGroupService;
import com.example.forumbackend.Service.ChatService;
import com.example.forumbackend.Service.UserService;
import com.example.forumbackend.Utils.ChatRoomWebSocketHandler;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatRoomWebSocketHandler handler;

    @Autowired
    private ChatGroupService chatGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${chatype.group}")
    private Integer GroupType;

    @Value("${chatype.private}")
    private Integer PrivateType;

    @GetMapping("/sendprivate")
    @Transactional
    @ApiOperation("发送私聊信息")
    @ApiResponses({
            @ApiResponse(code = 102,message = "成功发送"),
            @ApiResponse(code = 124,message = "发送对象不存在"),
            @ApiResponse(code=129,message = "不能自己给自己发")
    })
    public ResponseResult<Chat> sendprivate(@CookieValue("UID") Integer uid, HttpServletResponse response, HttpServletRequest request,
                                            @RequestParam @ApiParam("发送对象uid") Integer receivedid,
                                            @RequestParam @ApiParam("发送内容") String message) throws JsonProcessingException {
        if(uid.equals(receivedid))
            return Response.makeRsp(ResultCode.CANNOT_SEND_TO_YOURSELF.code, "不能自己给自己发");
        Chat chat=new Chat();
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        chat.setCreateTime(LocalDateTime.now());
        chat.setReceiveUID(receivedid);
        chat.setSendUID(uid);
        chat.setMessage(message);
        chat.setMtype(PrivateType);
        if(userService.findByUID(receivedid)==null)
            return Response.makeRsp(ResultCode.USER_NOT_EXIST.code, "用户不存在");
        chatService.insert(chat);
        handler.sendMessageToUser(receivedid,new TextMessage(jsonMapper.writeValueAsString(chat)));
        return Response.makeOKRsp(chat);
    }

    @GetMapping("/sendgroup")
    @Transactional
    @ApiOperation("发送群聊")
    @ApiResponses({
            @ApiResponse(code = 127,message = "未加群"),
            @ApiResponse(code=102,message = "成功发送"),
    })
    public ResponseResult<Chat> sendgroup(@CookieValue("UID") Integer uid,HttpServletRequest request,HttpServletResponse response,
                                         @RequestParam @ApiParam("群id") Integer GID,
                                          @RequestParam @ApiParam("发送内容") String message) throws JsonProcessingException {
        Chat chat=new Chat();
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        chat.setCreateTime(LocalDateTime.now());
        chat.setGroupID(GID);
        chat.setSendUID(uid);
        chat.setMessage(message);
        chat.setMtype(GroupType);
        if(!chatGroupService.verify(GID,uid))
            return Response.makeRsp(ResultCode.NOT_JOIN_GROUP.code, "未加群");
        chatService.insert(chat);
        List<GroupItem> list=chatGroupService.getlist(GID);
        for(GroupItem item:list) {
            if(item.getUID().equals(uid))
                continue;
            handler.sendMessageToUser(item.getUID(), new TextMessage(jsonMapper.writeValueAsString(chat)));
        }
        return Response.makeOKRsp(chat);
    }


    @GetMapping("/CreateGroup")
    @Transactional
    @ApiOperation("创建群聊")
    public ResponseResult<ChatGroup> CreateGroup(
            @RequestParam @ApiParam("群名称") String name,
            @RequestParam @ApiParam("群介绍") String introduce,
            HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        LocalDateTime now=LocalDateTime.now();
        ChatGroup group=new ChatGroup();
        group.setName(name);
        group.setIntroduce(introduce);
        group.setCreatetime(now);
        chatGroupService.CreateGroup(group);
        return Response.makeOKRsp(group);
    }

    @GetMapping("/addgroup")
    @Transactional
    @ApiOperation("加群")
    @ApiResponses({
            @ApiResponse(code = 126,message = "群不存在"),
            @ApiResponse(code = 102,message = "成功")
    })
    public ResponseResult<Boolean> AddGroup(@CookieValue("UID") Integer uid,
                                            @RequestParam @ApiParam("群id") Integer GID,
                                            HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ChatGroup group=chatGroupService.findByGID(GID);
        if(group==null){
            return Response.makeRsp(ResultCode.GROUP_NOT_EXIST.code, "群不存在",false);
        }
        chatGroupService.AddGroup(GID,uid);
        return Response.makeOKRsp(true);
    }

    @GetMapping("/getgroupitemlist")
    @Transactional
    @ApiOperation("获取论坛群总数量")
    public ResponseResult<List<GroupItem>> getlist(Integer GID,
                                                   HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatGroupService.getlist(GID));
    }

    @GetMapping("/getprivatecount")
    @Transactional
    @ApiOperation("获取某用户的存在的私聊的人的数量")
    public  ResponseResult<Integer> getprivatecount(@CookieValue("UID") Integer uid,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatService.getprivatecount(uid));
    }

    @GetMapping("/getprivatechatbytime")
    @Transactional
    @ApiOperation("获取与某用户存在的私聊的人的列表，按照最后发送时间来确定")
    public ResponseResult<List<Integer>> getprivatechatbytime(@CookieValue("UID")Integer uid,Integer pageindex,Integer pagesize,
                                                              HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatService.getprivatechatbytime(uid,pageindex,pagesize));
    }

    @GetMapping("/getgroupchatbytime")
    @Transactional
    @ApiOperation("获取与某用户存在聊天记录的群的列表，按照最后发送时间来确定")
    public ResponseResult<List<Integer>> getgroupchatbytime(@CookieValue("UID")Integer uid,Integer pageindex,Integer pagesize,
                                                            HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatService.getgroupchatbytime(uid,pageindex,pagesize));
    }

    @GetMapping("/getprivatechat")
    @Transactional
    @ApiOperation("获取与某用户最近的特定条数的聊天记录")
    public ResponseResult<List<Chat>> getprivatechat(
            @CookieValue("UID")Integer uid,
            Integer senduid,Integer pagesize,
            HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatService.getprivatechat(uid, senduid,  pagesize));
    }

    @GetMapping("/getprivatechatbefore")
    @Transactional
    @ApiOperation("分页获取与某用户的聊天记录中早于某条特定记录的记录")
    public ResponseResult<List<Chat>> getprivatechat2(
            @CookieValue("UID")Integer uid,
            Integer senduid,Integer CID,Integer size,
            HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatService.getprivatechat2(uid, senduid, CID,size));
    }

    @GetMapping("/getgroupchat")
    @Transactional
    @ApiOperation("获取某群中最近的特定数量的聊天记录")
    @ApiResponses({
            @ApiResponse(code = 127,message = "未加群"),
            @ApiResponse(code=102,message = "成功"),
    })
    public ResponseResult<List<Chat>> getgroupchat(@CookieValue("UID")Integer uid,Integer gid, Integer pagesize,HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        if(!chatGroupService.verify(gid,uid))
            return Response.makeRsp(ResultCode.NOT_JOIN_GROUP.code, "尚未加群");
        return Response.makeOKRsp(chatService.getgroupchat(gid,  pagesize));
    }

    @GetMapping("/getgroupchatbefore")
    @Transactional
    @ApiResponses({
            @ApiResponse(code = 127,message = "未加群"),
            @ApiResponse(code=102,message = "成功"),
    })
    @ApiOperation("分页获取某群的聊天记录中早于某条特定记录的记录")
    public ResponseResult<List<Chat>> getgroupchat2(@CookieValue("UID")Integer uid,Integer gid,Integer cid, Integer size,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        if(!chatGroupService.verify(gid,uid))
            return Response.makeRsp(ResultCode.NOT_JOIN_GROUP.code, "尚未加群");
        return Response.makeOKRsp(chatService.getgroupchat2(gid, cid,size));
    }

    @GetMapping("/searchgroupbyid")
    @Transactional
    @ApiOperation("通过GID搜索群")
    public  ResponseResult<ChatGroup> search(Integer gid,HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatGroupService.search(gid));
    }

    @GetMapping("/searchgroupbystring")
    @Transactional
    @ApiOperation("模糊搜索群")
    public ResponseResult<List<ChatGroup>> search(String str,Integer pageindex,Integer pagesize,HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(chatGroupService.search(str, pageindex, pagesize));
    }
}
