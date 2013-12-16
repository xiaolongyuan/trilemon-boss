package com.trilemon.boss.poster.template.dao;

import com.trilemon.boss.poster.template.client.request.PosterTemplateQueryRequest;
import com.trilemon.boss.poster.template.model.PosterTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosterTemplateDAO {
    int deleteByPrimaryKey(Long id);

    void insertSelective(PosterTemplate record);

    PosterTemplate selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PosterTemplate record);

    int countByQueryRequest(PosterTemplateQueryRequest request);

    List<PosterTemplate> paginateByQueryRequest(PosterTemplateQueryRequest request);

}