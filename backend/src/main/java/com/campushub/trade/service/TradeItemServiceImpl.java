package com.campushub.trade.service;

import com.campushub.common.PublicUserVO;
import com.campushub.common.exception.BizException;
import com.campushub.common.exception.NotFoundException;
import com.campushub.common.response.PageResponse;
import com.campushub.common.response.ResponseCode;
import com.campushub.trade.dto.TradeItemCreateDTO;
import com.campushub.trade.dto.TradeItemQueryDTO;
import com.campushub.trade.dto.TradeItemStatusPatchDTO;
import com.campushub.trade.entity.TradeItem;
import com.campushub.trade.entity.TradeItemImage;
import com.campushub.trade.entity.TradeItemStatus;
import com.campushub.trade.exception.TradeErrorCode;
import com.campushub.trade.repository.TradeItemImageRepository;
import com.campushub.trade.repository.TradeItemRepository;
import com.campushub.trade.vo.TradeItemVO;
import com.campushub.user.api.UserApi;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TradeItemServiceImpl implements TradeItemService {

    private final TradeItemRepository itemRepo;
    private final TradeItemImageRepository imageRepo;
    private final UserApi userApi;

    public TradeItemServiceImpl(TradeItemRepository itemRepo,
                                TradeItemImageRepository imageRepo,
                                UserApi userApi) {
        this.itemRepo = itemRepo;
        this.imageRepo = imageRepo;
        this.userApi = userApi;
    }

    @Override
    @Transactional
    public TradeItemVO createItem(long sellerId, TradeItemCreateDTO dto) {
        TradeItem item = itemRepo.save(new TradeItem(
                sellerId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPricePoint(),
                dto.getPickupLocationType(),
                dto.getPickupLocationDetail()
        ));

        List<String> urls = new ArrayList<>();
        if (dto.getImageUrls() != null) {
            int sort = 0;
            for (String url : dto.getImageUrls()) {
                if (url == null || url.isBlank()) continue;
                imageRepo.save(new TradeItemImage(item.getId(), url, sort++));
                urls.add(url);
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TradeItemVO> listItems(TradeItemQueryDTO query) {
        Specification<TradeItem> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.isNull(root.get("deletedAt")));
            if (query.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getQ() != null && !query.getQ().isBlank()) {
                String like = "%" + query.getQ() + "%";
                preds.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("description"), like)
                ));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        var pageable = PageRequest.of(
                query.getPage() - 1,
                query.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var page = itemRepo.findAll(spec, pageable);
        List<TradeItemVO> vos = page.getContent().stream()
                .map(item -> toVo(item, loadUrls(item.getId())))
                .toList();
        return PageResponse.of(vos, page.getTotalElements(), query.getPage(), query.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public TradeItemVO getItem(long itemId) {
        TradeItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new NotFoundException("商品不存在: " + itemId));
        if (item.getDeletedAt() != null) {
            throw new NotFoundException("商品已下架/删除");
        }
        return toVo(item, loadUrls(item.getId()));
    }

    private List<String> loadUrls(Long itemId) {
        return imageRepo.findByItemIdOrderBySortOrderAsc(itemId).stream()
                .map(TradeItemImage::getUrl)
                .toList();
    }

    private TradeItemVO toVo(TradeItem item, List<String> urls) {
        // 卖家账号若被注销或测试数据用了不存在的 sellerId（如单测里直接 createItem(100L, ...)），
        // 不应让商品 VO 渲染失败 —— 用 Optional 版本避免抛异常污染事务，找不到时降级占位。
        PublicUserVO seller = userApi.findPublicUser(item.getSellerId())
                .orElseGet(() -> new PublicUserVO(item.getSellerId(), "已注销用户", null, null));
        return new TradeItemVO(
                item.getId(),
                seller,
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
