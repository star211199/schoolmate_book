package com.schoolmate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schoolmate.common.ResultCode;
import com.schoolmate.context.UserContext;
import com.schoolmate.dto.album.AlbumCreateDTO;
import com.schoolmate.entity.Album;
import com.schoolmate.entity.Photo;
import com.schoolmate.entity.User;
import com.schoolmate.exception.BusinessException;
import com.schoolmate.mapper.AlbumMapper;
import com.schoolmate.mapper.PhotoMapper;
import com.schoolmate.service.AlbumService;
import com.schoolmate.service.ClassService;
import com.schoolmate.service.UserService;
import com.schoolmate.utils.FileUploadUtil;
import com.schoolmate.vo.album.AlbumVO;
import com.schoolmate.vo.album.PhotoVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 相册服务实现。
 *
 * @author Albot
 */
@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_PASSED = "PASSED";

    @Resource
    private PhotoMapper photoMapper;

    @Resource
    private ClassService classService;

    @Resource
    private UserService userService;

    @Resource
    private FileUploadUtil fileUploadUtil;

    @Value("${schoolmate.content.audit-enabled}")
    private boolean auditEnabled;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO createAlbum(Long classId, AlbumCreateDTO dto) {
        Long userId = UserContext.getUserId();
        classService.assertMember(classId, userId);

        Album album = new Album();
        album.setClassId(classId);
        album.setUserId(userId);
        album.setName(dto.getName());
        album.setDescription(dto.getDescription());
        album.setCoverUrl(dto.getCoverUrl());
        this.save(album);
        return convertToVO(album);
    }

    @Override
    public List<AlbumVO> listAlbums(Long classId) {
        List<Album> albums = this.list(new LambdaQueryWrapper<Album>()
            .eq(Album::getClassId, classId)
            .orderByDesc(Album::getCreateTime));
        return albums.stream().map(this::convertToVO).toList();
    }

    @Override
    public AlbumVO getAlbum(Long id) {
        Album album = this.getById(id);
        if (album == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册不存在");
        }
        return convertToVO(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long id) {
        Album album = this.getById(id);
        if (album == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册不存在");
        }
        if (!Objects.equals(album.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有相册创建者或管理员可删除相册");
        }
        this.removeById(id);
        photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PhotoVO uploadPhoto(Long albumId, MultipartFile file, String description) {
        Long userId = UserContext.getUserId();
        Album album = this.getById(albumId);
        if (album == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "相册不存在");
        }
        classService.assertMember(album.getClassId(), userId);

        String url = fileUploadUtil.upload(file);
        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setUserId(userId);
        photo.setUrl(url);
        photo.setDescription(description);
        photo.setAuditStatus(auditEnabled ? AUDIT_PENDING : AUDIT_PASSED);
        photoMapper.insert(photo);

        // 首张照片自动作为封面
        if (!StringUtils.hasText(album.getCoverUrl())) {
            album.setCoverUrl(url);
            this.updateById(album);
        }

        User user = userService.getById(userId);
        PhotoVO vo = convertToPhotoVO(photo);
        if (user != null) {
            vo.setNickname(user.getNickname());
        }
        return vo;
    }

    @Override
    public List<PhotoVO> listPhotos(Long albumId) {
        List<Photo> photos = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
            .eq(Photo::getAlbumId, albumId)
            .eq(Photo::getAuditStatus, AUDIT_PASSED)
            .orderByDesc(Photo::getCreateTime));
        if (photos.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = photos.stream().map(Photo::getUserId).distinct().toList();
        var userMap = userService.listByIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        return photos.stream().map(p -> {
            PhotoVO vo = convertToPhotoVO(p);
            User user = userMap.get(p.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "照片不存在");
        }
        if (!Objects.equals(photo.getUserId(), UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己上传的照片");
        }
        photoMapper.deleteById(id);
    }

    private AlbumVO convertToVO(Album album) {
        if (Objects.isNull(album)) {
            return null;
        }
        AlbumVO vo = new AlbumVO();
        vo.setId(album.getId());
        vo.setClassId(album.getClassId());
        vo.setUserId(album.getUserId());
        vo.setName(album.getName());
        vo.setCoverUrl(album.getCoverUrl());
        vo.setDescription(album.getDescription());
        vo.setCreateTime(album.getCreateTime());
        Long count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
            .eq(Photo::getAlbumId, album.getId()));
        vo.setPhotoCount(count == null ? 0L : count);
        return vo;
    }

    private PhotoVO convertToPhotoVO(Photo photo) {
        if (Objects.isNull(photo)) {
            return null;
        }
        PhotoVO vo = new PhotoVO();
        vo.setId(photo.getId());
        vo.setAlbumId(photo.getAlbumId());
        vo.setUserId(photo.getUserId());
        vo.setUrl(photo.getUrl());
        vo.setDescription(photo.getDescription());
        vo.setAuditStatus(photo.getAuditStatus());
        vo.setCreateTime(photo.getCreateTime());
        return vo;
    }
}
