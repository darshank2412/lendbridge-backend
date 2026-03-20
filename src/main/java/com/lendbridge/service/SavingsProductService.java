package com.lendbridge.service;

import com.lendbridge.dto.request.SavingsProductRequest;
import com.lendbridge.dto.response.SavingsProductResponse;
import com.lendbridge.entity.SavingsProduct;
import com.lendbridge.enums.ProductStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.SavingsProductRepository;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsProductService {

    private final SavingsProductRepository repo;
    private final EntityMapper mapper;

    public List<SavingsProductResponse> getAll() {
        return repo.findByStatus(ProductStatus.ACTIVE).stream()
                .map(mapper::toSavingsProductResponse).toList();
    }

    public SavingsProductResponse getById(Long id) {
        return mapper.toSavingsProductResponse(findById(id));
    }

    @Transactional
    public SavingsProductResponse create(SavingsProductRequest req) {
        validateMinMax(req.getMinBalance().doubleValue(), req.getMaxBalance().doubleValue(), "balance");
        SavingsProduct p = SavingsProduct.builder()
                .name(req.getName()).minBalance(req.getMinBalance())
                .maxBalance(req.getMaxBalance()).interestRate(req.getInterestRate()).build();
        return mapper.toSavingsProductResponse(repo.save(p));
    }

    @Transactional
    public SavingsProductResponse update(Long id, SavingsProductRequest req) {
        validateMinMax(req.getMinBalance().doubleValue(), req.getMaxBalance().doubleValue(), "balance");
        SavingsProduct p = findById(id);
        p.setName(req.getName());
        p.setMinBalance(req.getMinBalance());
        p.setMaxBalance(req.getMaxBalance());
        p.setInterestRate(req.getInterestRate());
        return mapper.toSavingsProductResponse(repo.save(p));
    }

    @Transactional
    public void deactivate(Long id) {
        SavingsProduct p = findById(id);
        p.setStatus(ProductStatus.INACTIVE);
        repo.save(p);
    }

    public SavingsProduct findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Savings product not found"));
    }

    private void validateMinMax(double min, double max, String field) {
        if (min >= max) throw LendbridgeException.badRequest("min" + field + " must be less than max" + field);
    }
}
