package com.ttthinh.shoe_shop_basic.service.image;

import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageUploadQueueService {
    private final ImageUploadWorker imageUploadWorker;

    public void enqueueProductImages(List<MultipartFile> files, String productId, Integer primaryIndex) {
        List<QueuedImageFile> queuedFiles = snapshot(files);
        if (queuedFiles.isEmpty()) {
            return;
        }
        afterCommit(() -> imageUploadWorker.uploadProductImages(queuedFiles, productId, primaryIndex));
    }

    public void enqueueVariantImages(List<MultipartFile> files, String productId, String variantId, Integer primaryIndex) {
        List<QueuedImageFile> queuedFiles = snapshot(files);
        if (queuedFiles.isEmpty()) {
            return;
        }
        afterCommit(() -> imageUploadWorker.uploadVariantImages(queuedFiles, productId, variantId, primaryIndex));
    }

    public void enqueueBrandLogo(MultipartFile file, String brandId) {
        List<QueuedImageFile> queuedFiles = snapshot(file == null ? List.of() : List.of(file));
        if (queuedFiles.isEmpty()) {
            return;
        }
        afterCommit(() -> imageUploadWorker.uploadBrandLogo(queuedFiles.get(0), brandId));
    }

    private List<QueuedImageFile> snapshot(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(this::snapshot)
                .toList();
    }

    private QueuedImageFile snapshot(MultipartFile file) {
        try {
            return new QueuedImageFile(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new AppException(ErrorCode.UPLOAD_IMAGE_TO_CLOUD_FAILED);
        }
    }

    private void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
