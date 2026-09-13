package com.ceos22.cgv_clone.domain.store.service;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.store.dto.request.StoreCreateRequest;
import com.ceos22.cgv_clone.domain.store.dto.request.StoreStockCreateRequest;
import com.ceos22.cgv_clone.domain.store.dto.response.StoreProductResponse;
import com.ceos22.cgv_clone.domain.store.dto.response.StoreResponse;
import com.ceos22.cgv_clone.domain.store.entity.Product;
import com.ceos22.cgv_clone.domain.store.entity.ProductCategory;
import com.ceos22.cgv_clone.domain.store.entity.Store;
import com.ceos22.cgv_clone.domain.store.entity.StoreStock;
import com.ceos22.cgv_clone.domain.store.repository.ProductRepository;
import com.ceos22.cgv_clone.domain.store.repository.StoreRepository;
import com.ceos22.cgv_clone.domain.store.repository.StoreStockRepository;
import com.ceos22.cgv_clone.domain.theater.entity.Theater;
import com.ceos22.cgv_clone.domain.theater.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreStockRepository storeStockRepository;
    private final ProductRepository productRepository;
    private final TheaterRepository theaterRepository;
    private final com.ceos22.cgv_clone.common.metrics.PaymentMetrics paymentMetrics;

    public List<StoreResponse> findStoresByTheater(Long theaterId) {

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEATER_NOT_FOUND));

        List<Store> stores = storeRepository.findByTheater(theater);

        return stores.stream()
                .map(StoreResponse::from)
                .collect(Collectors.toList());
    }

    public List<StoreProductResponse> findProductsByStoreAndCategory(Long storeId, ProductCategory category) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        List<StoreStock> storeStocks = storeStockRepository.findByStoreAndProductCategory(store, category);

        return storeStocks.stream()
                .map(StoreProductResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {

        Theater theater = theaterRepository.findById(request.theaterId())
                .orElseThrow(() -> new CustomException(ErrorCode.THEATER_NOT_FOUND));

        Store store = Store.of(request.storeType(), theater);
        Store savedStore = storeRepository.save(store);

        return StoreResponse.from(savedStore);
    }

    @Transactional
    public void addProductToStore(StoreStockCreateRequest request) {

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (storeStockRepository.existsByStoreAndProduct(store, product)) {
            throw new CustomException(ErrorCode.DUPLICATED_RESOURCE);
        }

        StoreStock storeStock = StoreStock.of(store, product, request.initialStock());
        storeStockRepository.save(storeStock);
    }

    //=================================================================================================================

    // Both methods join the caller's transaction, so every product of one order commits or rolls back together.
    // Concurrent updates fail with OptimisticLockingFailureException at commit

    @Transactional
    public void increaseStocks(Long storeId, Map<Long, Integer> quantityByProductId) {

        for (StoreStock storeStock : findStocks(storeId, quantityByProductId)) {
            storeStock.increaseStock(quantityByProductId.get(productIdOf(storeStock)));
        }
    }

    @Transactional
    public void decreaseStocks(Long storeId, Map<Long, Integer> quantityByProductId) {

        int decreased = 0;

        for (StoreStock storeStock : findStocks(storeId, quantityByProductId)) {
            Long productId = productIdOf(storeStock);
            Integer quantity = quantityByProductId.get(productId);

            try {
                storeStock.decreaseStock(quantity);
            } catch (CustomException e) {
                // Callers know the order, not which product ran out
                throw new CustomException(e.getErrorCode(),
                        "storeId=%d, productId=%d, requested=%d, available=%d"
                                .formatted(storeId, productId, quantity, storeStock.getStock()));
            }
            decreased += quantity;
        }

        paymentMetrics.recordStockDecrease(decreased);
    }

    private List<StoreStock> findStocks(Long storeId, Map<Long, Integer> quantityByProductId) {

        if (quantityByProductId.isEmpty()) {
            return List.of();
        }

        List<StoreStock> storeStocks =
                storeStockRepository.findAllByStoreIdAndProductIdIn(storeId, quantityByProductId.keySet());

        if (storeStocks.size() != quantityByProductId.size()) {
            throw new CustomException(ErrorCode.STOCK_NOT_FOUND,
                    "storeId=%d, missingProductIds=%s".formatted(storeId, missingProductIds(storeStocks, quantityByProductId)));
        }
        return storeStocks;
    }

    private List<Long> missingProductIds(List<StoreStock> storeStocks, Map<Long, Integer> quantityByProductId) {

        List<Long> found = storeStocks.stream()
                .map(StoreService::productIdOf)
                .toList();

        return quantityByProductId.keySet().stream()
                .filter(productId -> !found.contains(productId))
                .toList();
    }

    private static Long productIdOf(StoreStock storeStock) {
        return storeStock.getProduct().getProductId();
    }

}
