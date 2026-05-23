package com.campushub.trade.service;

import com.campushub.common.exception.BizException;
import com.campushub.common.response.ResponseCode;
import com.campushub.common.storage.ObjectStorage;
import com.campushub.common.util.ExifCleaner;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemImage;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.vo.TradeItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TradeItemServiceImpl implements TradeItemService {

    private final TradeItemRepository itemRepo;
    private final TradeItemImageRepository imageRepo;
    private final ObjectStorage objectStorage;

    public TradeItemServiceImpl(TradeItemRepository itemRepo,
                                TradeItemImageRepository imageRepo,
                                ObjectStorage objectStorage) {
        this.itemRepo = itemRepo;
        this.imageRepo = imageRepo;
        this.objectStorage = objectStorage;
    }

    @Override
    @Transactional
    public TradeItemVO createItem(long sellerId, TradeItemCreateDTO dto, List<ImageUpload> images) {
        TradeItem item = itemRepo.save(new TradeItem(
                sellerId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPricePoint(),
                dto.getPickupLocationType(),
                dto.getPickupLocationDetail()
        ));

        List<String> urls = new ArrayList<>();
        if (images != null) {
            int sort = 0;
            for (ImageUpload upload : images) {
                byte[] cleaned = ExifCleaner.clean(upload.bytes(), upload.contentType());
                ObjectStorage.PutResult put = objectStorage.put(cleaned, upload.contentType());
                imageRepo.save(new TradeItemImage(item.getId(), put.url(), sort++));
                urls.add(put.url());
            }
        }
        return toVo(item, urls);
    }

    @Override
    @Transactional
    public TradeItemVO updateStatus(long sellerId, long itemId, TradeItemStatusPatchDTO dto) {
        TradeItem item = itemRepo.findByIdAndSellerId(itemId, sellerId)
                .orElseThrow(() -> new BizException(ResponseCode.FORBIDDEN, "仅本人可修改商品状态"));

        TradeItemStatus current = item.getStatus();
        TradeItemStatus target = dto.getStatus();

        if (current == TradeItemStatus.IN_TRADE) {
            throw new BizException(TradeErrorCode.ITEM_STATUS_INVALID, "交易中商品不可上下架", 422);
        }
        if (current != TradeItemStatus.ON_SALE && current != TradeItemStatus.OFF_SALE) {
            throw new BizException(TradeErrorCode.ITEM_STATUS_INVALID, "当前状态不可变更", 422);
        }
        if (target != TradeItemStatus.ON_SALE && target != TradeItemStatus.OFF_SALE) {
            throw new BizException(TradeErrorCode.ITEM_STATUS_INVALID, "仅支持上架/下架切换", 422);
        }
        if (current == target) {
            return toVo(item, loadUrls(item.getId()));
        }

        item.setStatus(target);
        itemRepo.save(item);
        return toVo(item, loadUrls(item.getId()));
    }

    private List<String> loadUrls(Long itemId) {
        return imageRepo.findByItemIdOrderBySortOrderAsc(itemId).stream()
                .map(TradeItemImage::getUrl)
                .toList();
    }

    static TradeItemVO toVo(TradeItem item, List<String> urls) {
        return new TradeItemVO(
                item.getId(),
                item.getSellerId(),
                item.getTitle(),
                item.getDescription(),
                item.getPricePoint(),
                item.getPickupLocationType(),
                item.getPickupLocationDetail(),
                item.getStatus(),
                urls,
                item.getCreatedAt()
        );
    }
}
