package com.schoolmate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schoolmate.dto.album.AlbumCreateDTO;
import com.schoolmate.entity.Album;
import com.schoolmate.vo.album.AlbumVO;
import com.schoolmate.vo.album.PhotoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 相册服务接口。
 *
 * @author Albot
 */
public interface AlbumService extends IService<Album> {

    /**
     * 创建相册（需为班级成员）。
     */
    AlbumVO createAlbum(Long classId, AlbumCreateDTO dto);

    /**
     * 查询班级相册列表。
     */
    List<AlbumVO> listAlbums(Long classId);

    /**
     * 查询相册详情。
     */
    AlbumVO getAlbum(Long id);

    /**
     * 删除相册（创建者或管理员）。
     */
    void deleteAlbum(Long id);

    /**
     * 上传照片到相册。
     */
    PhotoVO uploadPhoto(Long albumId, MultipartFile file, String description);

    /**
     * 查询相册照片列表。
     */
    List<PhotoVO> listPhotos(Long albumId);

    /**
     * 删除照片（上传者或管理员）。
     */
    void deletePhoto(Long id);
}
