package com.bbq.module.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crm_clue")
@Document(indexName = "crm_clue")
public class CrmClue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @Field(type = FieldType.Text)
    private String mobile;
    private String mobileMd5;
    private LocalDateTime createTime;
}
