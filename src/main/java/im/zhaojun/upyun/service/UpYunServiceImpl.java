package im.zhaojun.upyun.service;

import cn.hutool.core.util.URLUtil;
import com.UpYun;
import im.zhaojun.common.model.StorageConfig;
import im.zhaojun.common.model.dto.FileItemDTO;
import im.zhaojun.common.model.enums.FileTypeEnum;
import im.zhaojun.common.model.enums.StorageTypeEnum;
import im.zhaojun.common.service.FileService;
import im.zhaojun.common.service.StorageConfigService;
import im.zhaojun.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpYunServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(UpYunServiceImpl.class);

    @Resource
    private StorageConfigService storageConfigService;

    private static final String BUCKET_NAME_KEY = "bucket-name";

    private static final String USERNAME_KEY = "username";

    private static final String PASSWORD_KEY = "password";

    private static final String DOMAIN_KEY = "domain";

    private String domain;

    private UpYun upYun;

    private boolean isInitialized;

    @Override
    public void init() {
        try {
            Map<String, StorageConfig> stringStorageConfigMap =
                    storageConfigService.selectStorageConfigMapByKey(StorageTypeEnum.UPYUN);
            String bucketName = stringStorageConfigMap.get(BUCKET_NAME_KEY).getValue();
            String username = stringStorageConfigMap.get(USERNAME_KEY).getValue();
            String password = stringStorageConfigMap.get(PASSWORD_KEY).getValue();
            domain = stringStorageConfigMap.get(DOMAIN_KEY).getValue();
            upYun = new UpYun(bucketName, username, password);
            isInitialized = true;
        } catch (Exception e) {
            log.debug(StorageTypeEnum.UPYUN.getDescription() + "初始化异常, 已跳过");
        }
    }

    @Override
    public List<FileItemDTO> fileList(String path) throws Exception {
        ArrayList<FileItemDTO> fileItemList = new ArrayList<>();
        String nextMark = null;

        do {
            HashMap<String, String> hashMap = new HashMap<>(24);
            hashMap.put("x-list-iter", nextMark);
            hashMap.put("x-list-limit", "100");
            UpYun.FolderItemIter folderItemIter = upYun.readDirIter(URLUtil.encode(path), hashMap);
            nextMark = folderItemIter.iter;
            ArrayList<UpYun.FolderItem> folderItems = folderItemIter.files;
            if (folderItems != null) {
                for (UpYun.FolderItem folderItem : folderItems) {
                    FileItemDTO fileItemDTO = new FileItemDTO();
                    fileItemDTO.setName(folderItem.name);
                    fileItemDTO.setSize(folderItem.size);
                    fileItemDTO.setTime(folderItem.date);
                    fileItemDTO.setPath(path);

                    if ("folder".equals(folderItem.type)) {
                        fileItemDTO.setType(FileTypeEnum.FOLDER);
                    } else {
                        fileItemDTO.setType(FileTypeEnum.FILE);
                        fileItemDTO.setUrl(getDownloadUrl(StringUtils.concatUrl(path, fileItemDTO.getName())));
                    }
                    fileItemList.add(fileItemDTO);
                }
            }
        } while (!"g2gCZAAEbmV4dGQAA2VvZg".equals(nextMark));
        return fileItemList;

    }

    @Override
    public String getDownloadUrl(String path) {
        return URLUtil.complateUrl(domain, path);
    }

    @Override
    public StorageTypeEnum getStorageTypeEnum() {
        return StorageTypeEnum.UPYUN;
    }


    @Override
    public boolean getIsInitialized() {
        return isInitialized;
    }


}