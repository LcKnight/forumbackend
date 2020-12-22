package com.example.forumbackend.Contoller;

import com.example.forumbackend.Domain.Normal.ForumResource;
import com.example.forumbackend.Domain.Normal.Section;
import com.example.forumbackend.Domain.Normal.Upfile;
import com.example.forumbackend.Service.*;
import com.example.forumbackend.Utils.CookieUtil;
import com.example.forumbackend.Utils.ResponseUitls.Response;
import com.example.forumbackend.Utils.ResponseUitls.ResponseResult;
import com.example.forumbackend.Utils.ResponseUitls.ResultCode;
import io.swagger.annotations.*;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/upfile")
@Api(tags = "文件API(完成测试)")
public class UpfileController {

    @Value("${web.upload-path}")
    private String basepath;

    @Value("${resourcetype.file}")
    private Integer filetype;

    @Autowired
    private SimpleDateFormat sdf;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UpFileService upFileService;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private UserInfoService userInfoService;

    @Value("${points.downloadfile}")
    private Integer pointsdownloadfile;

    @Value("${points.uploadfile}")
    private Integer pointsuploadfile;


    @PostMapping("/upload")
    @Transactional
    @ApiOperation(value = "上传文件(测试完成)")
    @ApiResponses({
            @ApiResponse(code = 121,message = "上传文件为空"),
            @ApiResponse(code = 110,message = "上传失败，请重传"),
            @ApiResponse(code=111,message = "板块不存在"),
            @ApiResponse(code = 102,message = "上传成功")
    })
    public ResponseResult<Upfile> upload(
            HttpServletRequest request,
            HttpServletResponse response,
            @ApiParam(value = "文件本身")@RequestParam MultipartFile file,
            @ApiParam(value = "价格，缺省时为0")@RequestParam(required = false) Integer price,
            @ApiParam(value = "资源所属板块")@RequestParam Integer sectionid,
            @ApiParam(value = "介绍")@RequestParam() String introduction,
            @ApiParam(value = "关键词")@RequestParam String keywords,
            @ApiParam(value = "标题")@RequestParam String title){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        if(file.isEmpty()){
            return Response.makeRsp(ResultCode.FILE_EMPTY.code, "上传文件为空");
        }
        Section section=sectionService.findByID(sectionid);
        if(section==null){
            return Response.makeRsp(ResultCode.SECTION_NOT_EXIST.code,"板块不存在");
        }
        ForumResource resource=new ForumResource();
        resource.setSectionID(sectionid);
        Integer uid=cookieUtil.getuid(request);
        resource.setUID(uid);
        resource.setZan(0);
        resource.setLastReplyUID(null);
        resource.setType(filetype);
        resource.setPrice(price!=null?price:0);
        resource.setCreatedtime(LocalDateTime.now());
        resource.setLastreplytime(null);
        resourceService.addresource(resource);

        //生成文件存储路径
        String format = sdf.format(new Date(System.currentTimeMillis()));
        String folder = basepath + format;
        String oldname=file.getOriginalFilename();
        String newfilename= UUID.randomUUID().toString()+oldname.substring(oldname.lastIndexOf("."), oldname.length());
        File newfile=new  File(folder,newfilename);

        Upfile upfile=new Upfile();

        upfile.setResourceid(resource.getRID());
        upfile.setFilename(file.getOriginalFilename());
        upfile.setPath(newfile.getPath());
        upfile.setIntro(introduction);
        upfile.setCreatedtime(resource.getCreatedtime());
        upfile.setTitle(title);
        upfile.setPurchasetime(0);
        upfile.setKeywords(keywords);
        try {
            File parent=newfile.getParentFile();
            if(!parent.exists())
                parent.mkdirs();
            file.transferTo(newfile);
            upFileService.addupfile(upfile);
        } catch (IOException e) {
            e.printStackTrace();
            return Response.makeRsp(ResultCode.UPLOAD_FAILED.code, "上传失败，请重传");
        }
        userInfoService.addpointbyrid(pointsuploadfile,upfile.getResourceid());
        return Response.makeOKRsp(upfile);
    }

    @PutMapping("/changeinfo")
    @Transactional
    @ApiOperation(value = "修改文件信息")
    public ResponseResult<Upfile> changeinfo(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam @ApiParam("文件fid") Integer fid,
            @RequestParam(required = false) String introduction,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String title
    ){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Upfile upfile=upFileService.findByID(fid);
        if(upfile==null){
            return Response.makeRsp(ResultCode.FILE_NOT_EXIST.code, "此文件不存在");
        }
        ForumResource forumResource=resourceService.findresourceByrid(upfile.getResourceid());
        if(!forumResource.getUID().equals(cookieUtil.getuid(request))){
            return Response.makeRsp(ResultCode.FILE_NOT_BELONGS_TO_YOU.code, "修改的资源不是由本账号上传");
        }
        upFileService.changeinfo(fid,introduction,keywords,title);
        return Response.makeOKRsp();
    }

    @GetMapping("/allcount")
    @Transactional
    @ApiOperation(value = "获得论坛所有文件数量")
    public ResponseResult<Integer> getallcount(HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true"); response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(upFileService.getcount());
    }

    @GetMapping("/files")
    @Transactional
    @ApiOperation(value = "分页查询文件的资源信息（Resource）")
    public ResponseResult<List<ForumResource>> getfiles(@RequestParam @ApiParam(value = "页码号") Integer pageindex,@RequestParam @ApiParam(value = "页大小")Integer pagesize,
                                                        HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(resourceService.getfiles(pageindex,pagesize));
    }

    @GetMapping("/download")
    @ApiOperation(value = "下载文件(完成测试)")
    @ApiResponses({
            @ApiResponse(code = 112,message = "下载文件不存在"),
            @ApiResponse(code=118,message = "还未购买资源"),
            @ApiResponse(code = 102,message = "成功下载")
    })
    @Transactional
    public ResponseResult<Upfile> download(HttpServletRequest request, HttpServletResponse response,
                                           @RequestParam @ApiParam(value = "要下载文件的FID") Integer fid){
        Integer uid=cookieUtil.getuid(request);
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        ResponseResult<Upfile> result=upFileService.download(fid,uid);
        if(result.getData()==null)
            return  result;
        Upfile upfile=result.getData();
        try (InputStream inputStream=new FileInputStream(new File(upfile.getPath()));
             OutputStream outputStream=response.getOutputStream();){
            response.setContentType("application/x-download");
            response.addHeader("Content-Disposition","attachment;filename="+upfile.getFilename());
            IOUtils.copy(inputStream,outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        userInfoService.addpointbyrid(pointsdownloadfile,upfile.getResourceid());
        return  result;
    }

    @GetMapping("/getfilecountbyuid")
    @ApiOperation(value = "获取某一用户上传的文件数量")
    @Transactional
    public ResponseResult<Integer> getfilecountbyuid(Integer uid,HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(resourceService.getfilecountbyuid(uid));
    }

    @GetMapping("/getfilesbyuid")
    @ApiOperation(value = "获取某一用户上传的文件")
    @Transactional
    public ResponseResult<List<ForumResource>> getfilesbyuid(@RequestParam @ApiParam(value = "所查询用户的UID") Integer uid,
                                                             @RequestParam @ApiParam(value = "页码号") Integer pageindex,
                                                             @RequestParam @ApiParam(value = "页大小")Integer pagesize,
                                                             HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(resourceService.getfilesbyuid(uid,pageindex,pagesize));
    }


    @GetMapping("/getbyrid")
    @ApiOperation(value = "获取文件信息")
    @Transactional
    public ResponseResult<Upfile> getbyrid(@RequestParam @ApiParam(value = "所查询文件的RID") Integer rid,
                                           HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        return Response.makeOKRsp(upFileService.findByRID(rid));
    }

    @GetMapping("/search")
    @Transactional
    public ResponseResult<List<ForumResource>> search(
            @RequestParam @ApiParam(value = "页码号") Integer pageindex,
            @RequestParam @ApiParam(value = "页大小")Integer pagesize,
            @RequestParam(required = false) @ApiParam(value = "是否为最新文件" ) Boolean latest,
            @RequestParam(required = false) @ApiParam(value = "是否为最火文件") Boolean hottest,
            @RequestBody(required = false) @ApiParam(value = "搜索关键词")List<String> strings,
            HttpServletRequest request,HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        List<Upfile> upfiles=upFileService.search(pageindex,pagesize,latest,hottest,strings);
        List<Integer> rids=new ArrayList<>();
        for(Upfile upfile:upfiles)
            rids.add(upfile.getResourceid());
        return Response.makeOKRsp(resourceService.getbyrids(rids));
    }

    @GetMapping("/searchcount")
    @Transactional
    public ResponseResult<Integer> searchcount(
            @RequestParam(required = false) @ApiParam(value = "是否为最新文件" ) Boolean latest,
            @RequestParam(required = false) @ApiParam(value = "是否为最火文件") Boolean hottest,
            @RequestBody(required = false) @ApiParam(value = "搜索关键词")List<String> strings,
            HttpServletResponse response,HttpServletRequest request){
        response.addHeader("Access-Control-Allow-Credentials","true");
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("origin"));
        Integer integer=upFileService.searchcount(latest,hottest,strings);
        return Response.makeOKRsp(integer);
    }


}
