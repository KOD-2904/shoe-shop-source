package com.ttthinh.shoe_shop_basic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    @Value("${cloudinary.width:500}")
    private int imageWidth;

    @Value("${cloudinary.height:500}")
    private int imageHeight;

    public String upload(MultipartFile file, String folderName) {
        try {
            return upload(file.getBytes(), file.getOriginalFilename(), folderName);
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_TO_CLOUD_FAILED);
        }
    }

    public String upload(byte[] bytes, String originalFilename, String folderName) {
        try {
            String publicId = UUID.randomUUID().toString();

            Map<?, ?> options = ObjectUtils.asMap(
                    "folder", folderName,
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image"
            );
            Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, options);
            String url = (String) uploadResult.get("secure_url");
            log.info("Image {} uploaded to Cloudinary folder {}", originalFilename, folderName);
            return url;
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_TO_CLOUD_FAILED);
        }
    }

    public String uploadImageWithTransformation(MultipartFile file) {
        try {
            Map<String, String> options = new HashMap<>();
            options.put("transformation", "c_fill,w_" + imageWidth + ",h_" + imageHeight);

            Map<?, ?> uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), options);

            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_TO_CLOUD_FAILED);
        }
    }
}
