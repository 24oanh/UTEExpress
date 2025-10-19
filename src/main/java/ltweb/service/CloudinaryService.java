package ltweb.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:uteexpress}")
    private String folder;

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        
        String publicId = folder + "/" + UUID.randomUUID().toString() + fileExtension;

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "auto",
                        "folder", folder
                ));

        return (String) uploadResult.get("secure_url");
    }

    public String uploadProofImage(MultipartFile file, String shipmentCode) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String publicId = folder + "/proofs/" + shipmentCode + "_" + UUID.randomUUID().toString();

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "resource_type", "image",
                        "folder", folder + "/proofs",
                        "transformation", ObjectUtils.asMap(
                                "width", 800,
                                "height", 600,
                                "crop", "limit",
                                "quality", "auto"
                        )
                ));

        return (String) uploadResult.get("secure_url");
    }

    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    public String extractPublicIdFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        int startIndex = imageUrl.indexOf(folder);
        if (startIndex == -1) {
            return null;
        }
        
        int endIndex = imageUrl.lastIndexOf(".");
        if (endIndex == -1) {
            endIndex = imageUrl.length();
        }
        
        return imageUrl.substring(startIndex, endIndex);
    }

    public void deleteFileByUrl(String imageUrl) throws IOException {
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId != null) {
            deleteFile(publicId);
        }
    }
}