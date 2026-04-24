package com.team6.backend.area.application.service;

import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.area.domain.repository.AreaRepository;
import com.team6.backend.area.presentation.dto.request.AreaCreateRequest;
import com.team6.backend.area.presentation.dto.request.UpdateAreaRequest;
import com.team6.backend.area.presentation.dto.response.AreaResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.area.domain.exception.AreaErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public AreaResponse createArea(AreaCreateRequest request) {
        Area area = new Area(
                request.getName(),
                request.getCity(),
                request.getDistrict(),
                true
        );
        Area saved = areaRepository.save(area);
        return new AreaResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AreaResponse> getAreas(String keyword, int page, int size, String sortBy, boolean isAsc) {
        int normalizedSize = (size == 10 || size == 30 || size == 50) ? size : 10;

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
            isAsc = false;
        }

        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, normalizedSize, sort);

        Page<Area> areaList;
        if (keyword != null && !keyword.isBlank()) {
            areaList = areaRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            areaList = areaRepository.findAll(pageable);
        }

        return areaList.map(AreaResponse::new);
    }

    @Transactional(readOnly = true)
    public AreaResponse getAreaById(UUID areaId) {
        Area area = findAreaById(areaId);
        return new AreaResponse(area);
    }

    @Transactional
    public AreaResponse updateArea(UUID areaId, UpdateAreaRequest request) {
        Area area = findAreaById(areaId);
        area.update(
                request.getName(),
                request.getCity(),
                request.getDistrict(),
                request.getIs_active()
        );
        return new AreaResponse(area);
    }

    @Transactional
    public void deleteArea(UUID areaId) {
        Area area = findAreaById(areaId);
        area.markDeleted(securityUtils.getCurrentUserId().toString());
    }

    public Area findAreaById(UUID areaId) {
        return areaRepository.findById(areaId)
                .orElseThrow(() -> new ApplicationException(AreaErrorCode.AREA_NOT_FOUND));
    }
}