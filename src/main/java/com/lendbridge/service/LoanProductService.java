package com.lendbridge.service;

import com.lendbridge.dto.request.LoanProductRequest;
import com.lendbridge.dto.response.LoanProductResponse;
import com.lendbridge.entity.LoanProduct;
import com.lendbridge.enums.ProductStatus;
import com.lendbridge.exception.LendbridgeException;
import com.lendbridge.repository.LoanProductRepository;
import com.lendbridge.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository repo;
    private final EntityMapper mapper;

    public List<LoanProductResponse> getAll() {
        return repo.findByStatus(ProductStatus.ACTIVE).stream()
                .map(mapper::toLoanProductResponse).toList();
    }

    public LoanProductResponse getById(Long id) {
        return mapper.toLoanProductResponse(findById(id));
    }

    @Transactional
    public LoanProductResponse create(LoanProductRequest req) {
        validate(req);
        LoanProduct p = LoanProduct.builder()
                .name(req.getName())
                .minAmount(req.getMinAmount()).maxAmount(req.getMaxAmount())
                .minInterest(req.getMinInterest()).maxInterest(req.getMaxInterest())
                .minTenure(req.getMinTenure()).maxTenure(req.getMaxTenure()).build();
        return mapper.toLoanProductResponse(repo.save(p));
    }

    @Transactional
    public LoanProductResponse update(Long id, LoanProductRequest req) {
        validate(req);
        LoanProduct p = findById(id);
        p.setName(req.getName());
        p.setMinAmount(req.getMinAmount()); p.setMaxAmount(req.getMaxAmount());
        p.setMinInterest(req.getMinInterest()); p.setMaxInterest(req.getMaxInterest());
        p.setMinTenure(req.getMinTenure()); p.setMaxTenure(req.getMaxTenure());
        return mapper.toLoanProductResponse(repo.save(p));
    }

    @Transactional
    public void deactivate(Long id) {
        LoanProduct p = findById(id);
        p.setStatus(ProductStatus.INACTIVE);
        repo.save(p);
    }

    public LoanProduct findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> LendbridgeException.notFound("Loan product not found with ID: " + id));
    }

    private void validate(LoanProductRequest req) {
        if (req.getMinAmount().compareTo(req.getMaxAmount()) >= 0)
            throw LendbridgeException.badRequest("minAmount must be less than maxAmount");
        if (req.getMinInterest().compareTo(req.getMaxInterest()) >= 0)
            throw LendbridgeException.badRequest("minInterest must be less than maxInterest");
        if (req.getMinTenure() >= req.getMaxTenure())
            throw LendbridgeException.badRequest("minTenure must be less than maxTenure");
    }
}
